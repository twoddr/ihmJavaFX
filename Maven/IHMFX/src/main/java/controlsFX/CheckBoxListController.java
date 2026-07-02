package controlsFX;

import general.GestionnaireRetour;
import general.ObjetCSA;
import generalFX.CustomEditor;
import generalFX.GestionnaireListesFX;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;

public class CheckBoxListController extends BorderPane implements MaitreFX {

    private final HashMap<Integer, ObjetCSA> hash_objet = new HashMap<>();
    private final LinkedHashMap<Integer, CheckBox> hash_checkbox = new LinkedHashMap<>();
    private final Class<? extends ObjetCSA> type;
    @FXML
    private VBox box;
    private Button btnAjouter, btnRafraichir;
    private MaitreFX maitre;
    private java.util.function.Function<EditorPaneController, CustomEditor> editeurPersonnel = null;
    private Consumer<ObjetCSA> postCreateCallback;

    public CheckBoxListController(Class<? extends ObjetCSA> typeAffiche) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/CheckBoxList.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
            type = typeAffiche;
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        initializeButtons();
    }

    private void initializeButtons() {
        // Création des boutons
        Image addImage = new Image("images/add.png");
        ImageView addIconView = new ImageView(addImage);
        addIconView.setFitWidth(20);
        addIconView.setFitHeight(20);
        addIconView.setPreserveRatio(true);

        btnAjouter = new Button("", addIconView);
        btnAjouter.setTooltip(new Tooltip("Permet de créer un nouvel item"));
        btnAjouter.setOnAction(e -> action_ajouter());

        Image refreshImage = new Image("images/refresh.png");
        ImageView refreshIconView = new ImageView(refreshImage);
        refreshIconView.setFitWidth(20);
        refreshIconView.setFitHeight(20);
        refreshIconView.setPreserveRatio(true);

        btnRafraichir = new Button("", refreshIconView);
        btnRafraichir.setTooltip(new Tooltip("Permet de rafraichir le contenu de la liste"));
        btnRafraichir.setOnAction(e -> rafraichir_liste());

        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10));
        buttonBox.getChildren().addAll(btnAjouter, btnRafraichir);

        setTop(buttonBox);
    }

    public Class<? extends ObjetCSA> getType() {
        return type;
    }

    public MaitreFX getMaitre() {
        return maitre;
    }

    public void setMaitre(MaitreFX maitre) {
        this.maitre = maitre;
    }

    public void setEditeurPersonnel(java.util.function.Function<EditorPaneController, CustomEditor> factory) {
        this.editeurPersonnel = factory;
    }

    public void setPostCreateCallback(Consumer<ObjetCSA> callback) {
        this.postCreateCallback = callback;
    }


    public void enlever_bouton_ajout() {
        if (getTop() instanceof HBox) {
            ((HBox) getTop()).getChildren().remove(btnAjouter);
        }
    }

    public void enlever_bouton_refresh() {
        if (getTop() instanceof HBox) {
            ((HBox) getTop()).getChildren().remove(btnRafraichir);
        }
    }

    private void action_ajouter() {
        if (type == null) {
            affiche_console("** Element de référence inconnu dans " + this);
            affiche_utilisateur("Impossible d'ajouter un nouvel élément dans cette liste");
            return;
        }
        if (maitre == null) {
            affiche_console("** MaitreFX non défini pour " + this);
            affiche_utilisateur("Impossible d'ajouter un nouvel élément dans cette liste");
            return;
        }

        // Instanciation par défaut
        try {
            ObjetCSA instance = type.getDeclaredConstructor().newInstance();
            if (postCreateCallback != null) postCreateCallback.accept(instance);
            charger_donnees_vers_enfant(instance);
        } catch (InstantiationException | IllegalAccessException |
                 NoSuchMethodException | InvocationTargetException e) {
            affiche_console("CheckBoxList/action_ajouter : Failed to create an instance of "
                    + type.getName());
            affiche_utilisateur("Impossible d'ajouter un nouvel élément dans cette liste");
        }
    }

    public void clear() {
        hash_objet.clear();
        hash_checkbox.clear();
        Platform.runLater(() -> box.getChildren().clear());
    }

    public void setListe(ArrayList<? extends ObjetCSA> liste) {
        clear();
        for (ObjetCSA objetDi : liste) {
            CheckBox coche = new CheckBox(objetDi.toString());
            hash_objet.put(objetDi.getId(), objetDi);
            hash_checkbox.put(objetDi.getId(), coche);
            coche.setOnMouseClicked(event -> {
                if (maitre == null) {
                    System.err.println("Le checkBoxList ne connait pas son maitre. " +
                            "Les cases à cocher ne seront pas enregistrées !");
                    return;
                }
                maitre.action_utilisateur(objetDi, this, String.valueOf(coche.isSelected()));
            });
        }
        Platform.runLater(() -> box.getChildren().setAll(hash_checkbox.values()));
    }

    /**
     * Ajoute ou met à jour des éléments dans la liste
     * Si un objet existe déjà (même ID), il est mis à jour
     * Sinon, il est ajouté
     * L'état de sélection des checkboxes existantes est préservé
     *
     * @param liste liste des objets à ajouter/mettre à jour
     */
    public void addData(ArrayList<? extends ObjetCSA> liste) {
        for (ObjetCSA objetDi : liste) {
            int id = objetDi.getId();

            // Vérifier si l'objet existe déjà
            if (hash_objet.containsKey(id)) {
                // Mettre à jour l'objet existant
                hash_objet.put(id, objetDi);

                // Mettre à jour le label de la checkbox (préserve l'état de sélection)
                CheckBox existingCheckBox = hash_checkbox.get(id);
                if (existingCheckBox != null) {
                    boolean wasSelected = existingCheckBox.isSelected();
                    existingCheckBox.setText(objetDi.toString());
                    existingCheckBox.setSelected(wasSelected);  // Restaure l'état
                }
            } else {
                // Ajouter comme nouveau
                CheckBox coche = new CheckBox(objetDi.toString());
                hash_objet.put(id, objetDi);
                hash_checkbox.put(id, coche);

                coche.setOnMouseClicked(event -> {
                    if (maitre == null) {
                        System.err.println("Le checkBoxList ne connait pas son maitre. " +
                                "Les cases à cocher ne seront pas enregistrées !");
                        return;
                    }
                    maitre.action_utilisateur(objetDi, this, String.valueOf(coche.isSelected()));
                });
            }
        }

        // Mettre à jour l'UI avec toutes les checkboxes (anciennes + nouvelles)
        Platform.runLater(() -> box.getChildren().setAll(hash_checkbox.values()));
    }

    public ArrayList<? extends ObjetCSA> getSelectedObjects() {
        ArrayList<ObjetCSA> sortie = new ArrayList<>();
        box.getChildren().forEach(element -> {
            if (element instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) element;
                if (checkBox.isSelected()) {
                    for (Map.Entry<Integer, CheckBox> entry : hash_checkbox.entrySet()) {
                        if (checkBox.equals(entry.getValue())) {
                            sortie.add(hash_objet.get(entry.getKey()));
                            break;
                        }
                    }
                }
            }
        });
        return sortie;
    }

    public void setSelectedObjects(ArrayList<? extends ObjetCSA> selectedObjects) {
        Platform.runLater(() -> {
            for (Map.Entry<Integer, CheckBox> paire : hash_checkbox.entrySet()) {
                CheckBox checkBox = paire.getValue();
                int id = paire.getKey();
                checkBox.setSelected(false);
                for (ObjetCSA aSelectionner : selectedObjects) {
                    if (id == aSelectionner.getId()) {
                        checkBox.setSelected(true);
                        break;
                    }
                }
            }
        });
    }

    public void setSelectedObjectsByName(ArrayList<? extends ObjetCSA> selectedObjects) {
        Platform.runLater(() -> {
            for (Map.Entry<Integer, CheckBox> paire : hash_checkbox.entrySet()) {
                CheckBox checkBox = paire.getValue();
                int id = paire.getKey();
                boolean trouve = false;
                Iterator<? extends ObjetCSA> iterator = selectedObjects.iterator();
                while (iterator.hasNext() && !trouve) {
                    String localNom = iterator.next().getNom();
                    ObjetCSA objetDi = hash_objet.get(id);
                    if (objetDi != null) {
                        String intrusNom = objetDi.getNom();
                        if (intrusNom.equals(localNom)) {
                            trouve = true;
                        }
                    }
                }
                checkBox.setSelected(trouve);
            }
        });
    }

    public void selectAll() {
        Platform.runLater(() -> {
            for (CheckBox checkBox : hash_checkbox.values()) {
                checkBox.setSelected(true);
            }
        });
    }

    // ========== Implémentation de MaitreFX ==========

    @Override
    public void appeler_bd(int code, Object objet, GestionnaireRetour gestionnaireRetour) {
        if (maitre == null) {
            affiche_console("Attention : le checkBoxList " + this + " ne connait pas son maitre !");
            return;
        }

        boolean pourMoi = objet instanceof ObjetCSA &&
                type != null &&
                ((ObjetCSA) objet).getType().equals(type.getSimpleName()) &&
                code == 1;

        affiche_console("Demande d'ajout de : " + objet);
        maitre.appeler_bd(code, objet,
                new MonGestionnaireRetour(objet, pourMoi ? null : gestionnaireRetour));
    }

    @Override
    public void affiche_utilisateur(String texte) {
        if (maitre != null) {
            maitre.affiche_utilisateur(texte);
        } else {
            System.err.println("** CheckBoxList " + this + " : " + texte);
        }
    }

    @Override
    public void affiche_console(String texte) {
        if (maitre != null) {
            maitre.affiche_console("CheckBoxList : " + texte);
        } else {
            System.err.println("** CheckBoxList " + this + " : " + texte);
        }
    }

    @Override
    public ObjetCSA getElementFromType(String type) {
        if (maitre != null) {
            return maitre.getElementFromType(type);
        }
        System.err.println("** CheckBoxList " + this + " ne connait pas son maitre !");
        return null;
    }

    @Override
    public void action_utilisateur(Object selectedItem, Object source, String extraInfo) {
        if (maitre != null) {
            maitre.action_utilisateur(selectedItem, source, extraInfo);
        }
    }

    @Override
    public void affiche_details(Object selectedItem) {
        // Pas d'affichage de détails dans une liste de checkboxes
    }

    @Override
    public void nettoyer_champs() {
        clear();
    }

    @Override
    public void charger_donnees_vers_enfant(ObjetCSA objetCSA) {
        String heritedPath = (maitre == null || !(maitre instanceof EditorPaneController)) ?
                "" :
                ((EditorPaneController) maitre).getEditorPath();

        ObjetCSA instance = getObjetReference();
        if (instance == null) {
            affiche_utilisateur("Aucune possibilité de gérer les éléments affichés ici");
            return;
        }

        EditorPaneController.openEditor(objetCSA,
                this,
                editeurPersonnel,
                heritedPath);
    }

    @Override
    public void rafraichir() {
        rafraichir_liste();
    }

    @Override
    public boolean isTest() {
        return maitre != null && maitre.isTest();
    }

    private ObjetCSA getObjetReference() {
        if (type == null) {
            return null;
        }
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            affiche_console("CheckBoxList : Impossible de créer une instance de " + type);
            return null;
        }
    }

    public void rafraichir_liste() {
        if (maitre == null) {
            affiche_console("** " + this + " n'a pas de maitreFX, donc ne peut rafraichir son contenu");
            return;
        }
        if (type == null) {
            affiche_console("** Element de référence inconnu dans " + this);
            affiche_utilisateur("Impossible de rafraichir cette liste");
            return;
        }

        affiche_console("Rafraichissement de la liste de " + type.getSimpleName());
        try {
            ObjetCSA instance = type.getDeclaredConstructor().newInstance();
            if (postCreateCallback != null) postCreateCallback.accept(instance);

            affiche_utilisateur("Rafraichissement de la liste de " + instance.getPublicName());
            maitre.appeler_bd(4, instance, new GestionnaireListesFX(this));
        } catch (Exception e) {
            affiche_console("CheckBoxList : Impossible d'instancier " + type.getName());
            affiche_utilisateur("Impossible de rafraichir cette liste");
        }
    }

    private class MonGestionnaireRetour implements GestionnaireRetour {
        private final GestionnaireRetour autreGestionnaire;
        private final Object nouveau;

        protected MonGestionnaireRetour(Object selection, GestionnaireRetour gestionnaireRetour) {
            nouveau = selection;
            autreGestionnaire = gestionnaireRetour;
        }

        @Override
        public void traitementRetour(ArrayList arrayList) {
            if (arrayList.isEmpty()) {
                System.err.println("Le CheckBoxList reçoit une liste vide de la BD !");
                return;
            }

            if (arrayList.get(0) instanceof Integer) {
                int code = (int) arrayList.get(0);

                if (autreGestionnaire != null) {
                    autreGestionnaire.traitementRetour(arrayList);
                    return;
                }

                if (code == 1) {
                    affiche_utilisateur("Ajout réussi dans la base de données !");
                    if (type != null) {
                        if (nouveau instanceof ObjetCSA) {
                            ObjetCSA objetCSA = maitre.getElementFromType(((ObjetCSA) nouveau).getType());
                            if (postCreateCallback != null) postCreateCallback.accept(objetCSA);
                            maitre.appeler_bd(4, objetCSA,
                                    new GestionnaireListesFX(CheckBoxListController.this, nouveau));
                            return;
                        }
                        try {
                            ObjetCSA newInstance = type.getDeclaredConstructor().newInstance();
                            if (postCreateCallback != null) postCreateCallback.accept(newInstance);
                            maitre.appeler_bd(4, newInstance,
                                    new GestionnaireListesFX(CheckBoxListController.this, newInstance));
                            return;
                        } catch (Exception x) {
                            affiche_console(type + " non-instantiable ! " +
                                    "Impossible de rafraichir la liste !");
                        }
                    }
                    affiche_console("** Ajout réussi mais elt de référence inconnu !");
                    return;
                }
                System.err.println("*** Le CheckBoxList reçoit un retour inattendu " + arrayList);
                return;
            }
            System.err.println("Le CheckBoxList reçoit une requête non conforme : " + arrayList);
        }
    }
}