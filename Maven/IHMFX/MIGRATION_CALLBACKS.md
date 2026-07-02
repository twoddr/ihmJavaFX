# Migration vers l'approche par Callbacks

## Vue d'ensemble

`EditorPaneController` supporte maintenant deux approches :

1. **Ancienne approche** : via l'interface `MaitreFX` (toujours supportée pour compatibilité)
2. **Nouvelle approche** : via des callbacks `Consumer`/`Function` (recommandée)

## Pourquoi migrer ?

✅ **Plus flexible** : Injection de comportements spécifiques sans héritage  
✅ **Moins de couplage** : Pas besoin d'implémenter toute l'interface `MaitreFX`  
✅ **Plus moderne** : Utilisation des lambdas Java 8+  
✅ **Testable** : Facilite les tests unitaires avec des mocks simples

## Callbacks disponibles

### 1. `setSaveHandler(BiConsumer<ObjetCSA, Consumer<List<Object>>>)`
Pour gérer l'enregistrement asynchrone vers la base de données.

```java
editor.setSaveHandler((patient, callback) -> {
    CompletableFuture.runAsync(() -> {
        try {
            int id = patientService.save(patient);
            List<Object> result = Arrays.asList(id, "Succès");
            callback.accept(result);
        } catch (Exception e) {
            List<Object> result = Arrays.asList(-1, e.getMessage());
            callback.accept(result);
        }
    });
});
```

### 2. `setMessageHandler(Consumer<String>)`
Pour afficher des messages à l'utilisateur parent.

```java
editor.setMessageHandler(message -> {
    parentLabel.setText(message);
});
```

### 3. `setTypeResolver(Function<String, ObjetCSA>)`
Pour résoudre les types d'objets (utilisé dans `getElementFromType`).

```java
editor.setTypeResolver(typeName -> {
    return ObjetFactory.createInstance(typeName);
});
```

### 4. `setRefreshHandler(Runnable)`
Pour demander un rafraîchissement au parent.

```java
editor.setRefreshHandler(() -> {
    rechargerDepuisServeur();
    rafraichirAffichage();
});
```

### 5. `setParentNotifier(BiConsumer<Integer, ObjetCSA>)`
Pour notifier le parent d'événements (création/modification).

```java
editor.setParentNotifier((code, objet) -> {
    if (code == 1) {
        System.out.println("Création de : " + objet);
    } else if (code == 2) {
        System.out.println("Modification de : " + objet);
    }
});
```

### 6. `setOnClose(Runnable)`
Pour exécuter du code lors de la fermeture.

```java
editor.setOnClose(() -> {
    System.out.println("EditorPane fermé");
});
```

### 7. `setPostCreateCallback(Consumer<ObjetCSA>)`
Pour modifier l'objet après sa création.

```java
editor.setPostCreateCallback(objet -> {
    objet.setCreatedBy("CurrentUser");
});
```

## Exemple complet de migration

### Avant (avec MaitreFX)

```java
public class MonControleur implements MaitreFX {
    private PatientService service;
    
    @Override
    public void envoyer_vers_parent(int code, Object objet, GestionnaireRetour retour) {
        if (code == 1 || code == 2) {
            Patient p = (Patient) objet;
            service.save(p);
        }
    }
    
    @Override
    public ObjetCSA getElementFromType(String type) {
        return ObjetFactory.createInstance(type);
    }
    
    // ... autres méthodes MaitreFX à implémenter
    
    public void ouvrirEditeur(Patient patient) {
        EditorPaneController.openEditor(patient, this, null, "monControleur");
    }
}
```

### Après (avec Callbacks)

```java
public class MonControleur {
    private PatientService service;
    
    public void ouvrirEditeur(Patient patient) {
        EditorPaneController editor = 
            EditorPaneController.openEditorWithCallbacks(patient, null, "monControleur");
        
        // Configurer uniquement ce dont on a besoin
        editor.setSaveHandler((p, callback) -> {
            CompletableFuture.runAsync(() -> {
                try {
                    int id = service.save(p);
                    callback.accept(Arrays.asList(id, "Enregistré"));
                } catch (Exception e) {
                    callback.accept(Arrays.asList(-1, e.getMessage()));
                }
            });
        });
        
        editor.setTypeResolver(type -> ObjetFactory.createInstance(type));
        
        editor.setOnClose(() -> rafraichirListe());
    }
    
    private void rafraichirListe() {
        // Rafraîchir la liste des patients
    }
}
```

## Compatibilité

L'ancienne approche avec `MaitreFX` continue de fonctionner. Les callbacks sont vérifiés en **premier**, puis l'`EditorPane` fait un fallback vers `MaitreFX` si aucun callback n'est défini.

Cela permet une migration progressive :
1. Vous pouvez commencer par remplacer un seul callback
2. Les autres continueront à utiliser `MaitreFX`
3. Une fois tous les callbacks en place, vous pouvez supprimer l'implémentation de `MaitreFX`

## Bonnes pratiques

1. **Préférer les callbacks** pour tout nouveau code
2. **Gérer les erreurs** dans vos callbacks (try-catch)
3. **Utiliser CompletableFuture** pour les opérations asynchrones
4. **Ne pas bloquer le thread JavaFX** dans les callbacks
5. **Utiliser Platform.runLater()** si besoin de modifier l'UI depuis un thread différent

