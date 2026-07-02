# Migration de ListTableBDController vers les Handlers

## Vue d'ensemble

`ListTableBDController` a été migré pour utiliser des **handlers fonctionnels** (`Consumer`, `BiConsumer`, `Function`) au lieu de dépendre exclusivement de `MaitreFX`. 

Cette approche offre :
- ✅ **Plus de flexibilité** : vous choisissez comment gérer chaque opération
- ✅ **Découplage** : moins de dépendances sur l'interface `MaitreFX`
- ✅ **Testabilité** : facile de mocker des lambdas
- ✅ **Rétrocompatibilité** : l'ancien système avec `MaitreFX` fonctionne toujours

---

## Handlers disponibles

### 1. **saveHandler** (Sauvegarde)
```java
BiConsumer<ObjetCSA, Consumer<List<Object>>> saveHandler
```

**Rôle** : Gère la création ou modification d'un objet.

**Exemple** :
```java
listTable.setSaveHandler((patient, callback) -> {
    CompletableFuture.runAsync(() -> {
        List<Object> resultat = new ArrayList<>();
        try {
            boolean ok = patientService.save(patient);
            resultat.add(ok ? 1 : 0); // code succès/échec
        } catch (Exception e) {
            resultat.add(0); // échec
            resultat.add(e.getMessage());
        }
        callback.accept(resultat); // prévenir le ListTable
    });
});
```

---

### 2. **deleteHandler** (Suppression)
```java
BiConsumer<ObjetCSA, Consumer<List<Object>>> deleteHandler
```

**Rôle** : Gère la suppression d'un objet.

**Exemple** :
```java
listTable.setDeleteHandler((patient, callback) -> {
    CompletableFuture.runAsync(() -> {
        List<Object> resultat = new ArrayList<>();
        try {
            boolean ok = patientService.delete(patient);
            resultat.add(ok ? 3 : 0); // code 3 = suppression réussie
        } catch (Exception e) {
            resultat.add(0);
            resultat.add(e.getMessage());
        }
        callback.accept(resultat);
    });
});
```

---

### 3. **refreshDetailsHandler** (Récupération des détails)
```java
BiConsumer<ObjetCSA, Consumer<ObjetCSA>> refreshDetailsHandler
```

**Rôle** : Récupère un objet complet depuis la BD (pour édition).

**Exemple** :
```java
listTable.setRefreshDetailsHandler((patientMinimal, callback) -> {
    CompletableFuture.runAsync(() -> {
        Patient complet = patientService.getById(patientMinimal.getId());
        Platform.runLater(() -> callback.accept(complet));
    });
});
```

---

### 4. **refreshListHandler** (Rafraîchissement de la liste)
```java
Runnable refreshListHandler
```

**Rôle** : Recharge la liste complète depuis la source de données.

**Exemple** :
```java
listTable.setRefreshListHandler(() -> {
    CompletableFuture.runAsync(() -> {
        List<Patient> patients = patientService.getAll();
        ArrayList<Object> objets = new ArrayList<>(patients);
        Platform.runLater(() -> {
            listTable.charger_liste(objets, new Patient());
        });
    });
});
```

---

### 5. **typeResolver** (Résolveur de types)
```java
Function<String, ObjetCSA> typeResolver
```

**Rôle** : Retourne une instance vide d'un type donné (remplace `getElementFromType`).

**Exemple** :
```java
listTable.setTypeResolver(nomTable -> {
    return switch (nomTable) {
        case "Patient" -> new Patient();
        case "Medecin" -> new Medecin();
        case "Consultation" -> new Consultation();
        default -> null;
    };
});
```

---

### 6. **userActionHandler** (Actions utilisateur)
```java
BiConsumer<Object, String> userActionHandler
```

**Rôle** : Gère les actions personnalisées (clic, sélection, etc.).

**Exemple** :
```java
listTable.setUserActionHandler((selection, action) -> {
    System.out.println("Action : " + action + " sur " + selection);
    
    if ("2 clics".equals(action) && selection instanceof Patient) {
        // Ouvrir une fenêtre de détails
        afficherDetailsPatient((Patient) selection);
    }
});
```

---

### 7. **messageHandler** (Affichage de messages)
```java
Consumer<String> messageHandler
```

**Rôle** : Affiche des messages à l'utilisateur (remplace `affiche_console`).

**Exemple** :
```java
listTable.setMessageHandler(message -> {
    Platform.runLater(() -> {
        labelStatus.setText(message);
        // ou : showNotification(message);
    });
});
```

---

## Exemple complet d'utilisation

```java
public class GestionPatients extends BorderPane {
    
    private ListTableBDController listTable;
    private PatientService patientService;
    
    public void initialize() {
        listTable = new ListTableBDController();
        
        // Configuration des handlers
        configurerHandlers();
        
        // Charger les données initiales
        chargerPatients();
        
        setCenter(listTable);
    }
    
    private void configurerHandlers() {
        // 1. Sauvegarde
        listTable.setSaveHandler((patient, callback) -> {
            CompletableFuture.runAsync(() -> {
                List<Object> result = new ArrayList<>();
                try {
                    Patient sauve = patientService.save((Patient) patient);
                    result.add(sauve.getId() > 0 ? 2 : 1); // 1=création, 2=modification
                } catch (Exception e) {
                    result.add(0);
                    result.add("Erreur : " + e.getMessage());
                }
                callback.accept(result);
            });
        });
        
        // 2. Suppression
        listTable.setDeleteHandler((patient, callback) -> {
            CompletableFuture.runAsync(() -> {
                List<Object> result = new ArrayList<>();
                try {
                    patientService.delete((Patient) patient);
                    result.add(3); // code succès
                } catch (Exception e) {
                    result.add(0);
                    result.add("Impossible de supprimer : " + e.getMessage());
                }
                callback.accept(result);
            });
        });
        
        // 3. Détails
        listTable.setRefreshDetailsHandler((patientMin, callback) -> {
            CompletableFuture.runAsync(() -> {
                Patient complet = patientService.getById(patientMin.getId());
                Platform.runLater(() -> callback.accept(complet));
            });
        });
        
        // 4. Rafraîchissement liste
        listTable.setRefreshListHandler(this::chargerPatients);
        
        // 5. Résolveur de types
        listTable.setTypeResolver(type -> new Patient());
        
        // 6. Actions utilisateur
        listTable.setUserActionHandler((obj, action) -> {
            System.out.println("Action : " + action);
        });
        
        // 7. Messages
        listTable.setMessageHandler(msg -> 
            Platform.runLater(() -> lblStatus.setText(msg))
        );
        
        // 8. Callback post-création
        listTable.setPostCreateCallback(patient -> {
            // Initialiser des valeurs par défaut
            patient.inserer_valeur("dateCreation", LocalDate.now());
        });
    }
    
    private void chargerPatients() {
        CompletableFuture.runAsync(() -> {
            List<Patient> patients = patientService.getAll();
            ArrayList<Object> objets = new ArrayList<>(patients);
            Platform.runLater(() -> {
                listTable.charger_liste(objets, new Patient());
            });
        });
    }
}
```

---

## Migration progressive

Vous pouvez migrer progressivement :

### Étape 1 : Garder `MaitreFX` (ancien système)
```java
listTable.setMaitre(this); // fonctionne toujours
```

### Étape 2 : Ajouter quelques handlers
```java
listTable.setMaitre(this);
listTable.setSaveHandler(...);  // override juste la sauvegarde
listTable.setMessageHandler(...); // override juste les messages
```

### Étape 3 : Tout migrer vers les handlers
```java
// Ne plus utiliser setMaitre()
listTable.setSaveHandler(...);
listTable.setDeleteHandler(...);
// ... tous les handlers
```

---

## Avantages de cette approche

1. **Flexibilité maximale** : chaque opération a son propre handler
2. **Asynchrone natif** : facile d'utiliser `CompletableFuture`
3. **Testabilité** : mocker des lambdas est trivial
4. **Séparation des préoccupations** : la logique métier n'est plus dans l'UI
5. **Rétrocompatibilité** : l'ancien système fonctionne toujours

---

## Notes importantes

- Les handlers ont **priorité** sur `MaitreFX` s'ils sont configurés
- Le `saveHandler` est **automatiquement propagé** à l'`EditorPane` créé
- Format de retour pour `saveHandler`/`deleteHandler` : `List<Object>` avec :
  - `list.get(0)` = code entier (> 0 = succès, 0 = échec)
  - `list.get(1)` (optionnel) = message d'erreur

---

## Voir aussi

- `EditorPaneController` utilise le même pattern avec `setSaveHandler()`
- `EXEMPLE_UTILISATION_CALLBACKS.md` pour des exemples détaillés

