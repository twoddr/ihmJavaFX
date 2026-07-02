# Exemple pratique : Édition de patients avec Callbacks

## Scénario

Vous avez une application de gestion d'hôpital avec :
- Une liste de patients
- Un service REST pour sauvegarder vers le serveur
- Un éditeur pour modifier les données d'un patient

## Implémentation

### 1. Votre contrôleur principal

```java
public class HopitalController {
    
    @FXML
    private TableView<Patient> tablePatients;
    @FXML
    private Label lblStatus;
    
    private PatientService patientService = new PatientService();
    
    /**
     * Appelé quand l'utilisateur double-clique sur un patient
     */
    @FXML
    public void onPatientDoubleClick() {
        Patient patientSelectionne = tablePatients.getSelectionModel().getSelectedItem();
        if (patientSelectionne == null) return;
        
        // Récupérer les données complètes depuis le serveur
        Patient patientComplet = patientService.getById(patientSelectionne.getId());
        
        // Ouvrir l'éditeur avec l'approche moderne (callbacks)
        ouvrirEditeurAvecCallbacks(patientComplet);
    }
    
    /**
     * Approche MODERNE avec callbacks (recommandée)
     */
    private void ouvrirEditeurAvecCallbacks(Patient patient) {
        // 1. Créer l'éditeur (sans MaitreFX !)
        EditorPaneController editor = EditorPaneController.openEditorWithCallbacks(
            patient,              // L'objet à éditer
            null,                 // Pas de CustomEditor (ou votre EditeurPatient personnalisé)
            "hopital"             // Chemin parent
        );
        
        // 2. Configurer le handler d'enregistrement (ASYNCHRONE)
        editor.setSaveHandler((patientModifie, callback) -> {
            // Afficher un indicateur de chargement
            Platform.runLater(() -> lblStatus.setText("Enregistrement en cours..."));
            
            // Enregistrer de manière asynchrone (ne bloque pas l'UI)
            CompletableFuture.runAsync(() -> {
                try {
                    // Appel REST vers le serveur
                    int id = patientService.save(patientModifie);
                    
                    // Succès : notifier l'EditorPane
                    List<Object> resultat = Arrays.asList(id, "Patient enregistré avec succès");
                    callback.accept(resultat);
                    
                    // Rafraîchir la liste (sur le thread JavaFX)
                    Platform.runLater(() -> {
                        rafraichirListePatients();
                        lblStatus.setText("Patient enregistré : " + patientModifie.getNom());
                    });
                    
                } catch (Exception e) {
                    // Erreur : notifier l'EditorPane
                    List<Object> resultat = Arrays.asList(-1, "Erreur : " + e.getMessage());
                    callback.accept(resultat);
                    
                    Platform.runLater(() -> 
                        lblStatus.setText("Échec de l'enregistrement : " + e.getMessage())
                    );
                }
            });
        });
        
        // 3. Configurer le résolveur de types (pour les listes déroulantes, etc.)
        editor.setTypeResolver(typeName -> {
            switch (typeName) {
                case "Patient":
                    return new Patient();
                case "Medecin":
                    return new Medecin();
                case "Service":
                    return new Service();
                default:
                    return null;
            }
        });
        
        // 4. Configurer le handler de fermeture
        editor.setOnClose(() -> {
            System.out.println("Éditeur fermé");
            // Vous pouvez faire du nettoyage ici
        });
        
        // 5. (Optionnel) Handler pour afficher des messages dans l'IHM parent
        editor.setMessageHandler(message -> {
            Platform.runLater(() -> lblStatus.setText(message));
        });
        
        // 6. (Optionnel) Handler de rafraîchissement
        editor.setRefreshHandler(() -> {
            // Re-charger depuis le serveur
            Patient patientFrais = patientService.getById(patient.getId());
            editor.charger_donnees("hopital", patientFrais);
        });
    }
    
    /**
     * ANCIENNE approche avec MaitreFX (toujours supportée)
     */
    private void ouvrirEditeurAvecMaitreFX(Patient patient) {
        // Nécessite que HopitalController implémente MaitreFX
        EditorPaneController.openEditor(patient, this, null, "hopital");
    }
    
    private void rafraichirListePatients() {
        List<Patient> patients = patientService.getAll();
        tablePatients.getItems().setAll(patients);
    }
}
```

### 2. Votre service REST

```java
public class PatientService {
    
    private static final String BASE_URL = "http://localhost:8080/api/patients";
    
    /**
     * Enregistre un patient (création ou modification)
     * @return l'ID du patient enregistré
     */
    public int save(Patient patient) throws IOException {
        // Simulation d'un appel REST
        HttpClient client = HttpClient.newHttpClient();
        
        String json = new Gson().toJson(patient);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        
        HttpResponse<String> response = client.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            // Parser la réponse pour extraire l'ID
            JsonObject jsonResponse = JsonParser.parseString(response.body())
                .getAsJsonObject();
            return jsonResponse.get("id").getAsInt();
        } else {
            throw new IOException("Erreur HTTP : " + response.statusCode());
        }
    }
    
    public Patient getById(int id) {
        // Appel GET vers le serveur
        // ...
    }
    
    public List<Patient> getAll() {
        // Appel GET vers le serveur
        // ...
    }
}
```

## Avantages de cette approche

### ✅ Séparation des responsabilités
- `HopitalController` gère l'interface principale
- `EditorPaneController` gère l'édition
- `PatientService` gère la communication réseau
- **Aucun couplage entre eux** via MaitreFX

### ✅ Testabilité
```java
@Test
public void testEnregistrementPatient() {
    EditorPaneController editor = new EditorPaneController();
    
    // Mock du service
    AtomicBoolean saved = new AtomicBoolean(false);
    
    editor.setSaveHandler((patient, callback) -> {
        saved.set(true);
        callback.accept(Arrays.asList(1, "OK"));
    });
    
    // Simuler l'enregistrement
    editor.action_enregistrer(mockEvent);
    
    assertTrue(saved.get());
}
```

### ✅ Réutilisabilité
Le même `EditorPaneController` peut être utilisé dans différents contextes :
- Application desktop JavaFX
- Module web avec JavaFX embarqué
- Application de test/démo

### ✅ Flexibilité
Chaque projet peut injecter son propre comportement sans modifier `EditorPaneController`.

## Migration progressive

Vous pouvez migrer étape par étape :

```java
// Étape 1 : Commencer par le saveHandler
editor.setSaveHandler((obj, cb) -> { /* ... */ });
// Le reste continue avec MaitreFX

// Étape 2 : Ajouter le typeResolver
editor.setTypeResolver(type -> { /* ... */ });

// Étape 3 : Ajouter les autres callbacks au besoin
// ...

// Étape finale : Supprimer l'implémentation de MaitreFX
// et utiliser openEditorWithCallbacks()
```

## Combinaison avec CustomEditor

Si vous avez un éditeur personnalisé :

```java
// Créer votre CustomEditor
EditeurPatientPersonnalise customEditor = new EditeurPatientPersonnalise();

// L'ouvrir avec callbacks
EditorPaneController editor = EditorPaneController.openEditorWithCallbacks(
    patient,
    customEditor,  // <-- Votre éditeur personnalisé
    "hopital"
);

// Configurer les callbacks comme d'habitude
editor.setSaveHandler((p, cb) -> { /* ... */ });
```

## Conclusion

L'approche par callbacks est **plus moderne, plus flexible et plus testable** que l'ancienne approche avec `MaitreFX`. 

Elle est particulièrement adaptée pour :
- Les applications avec architecture en couches
- Les projets nécessitant des tests unitaires
- Les équipes travaillant avec des patterns modernes (DI, SOLID, etc.)

