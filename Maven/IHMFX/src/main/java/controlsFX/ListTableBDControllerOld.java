package controlsFX;

import general.GestionnaireRetour;
import general.Monostable;
import general.ObjetCSA;
import generalFX.CustomEditor;
import generalFX.GenerateurColonnesTableView;
import generalFX.ObjetIHM;
import generalFX.TransformeurObjets;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.URL;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class ListTableBDController extends BorderPane implements Initializable, MaitreFX, GestionnaireRetour {
    private final static int TAILLE_CARACTERE = 10;
    private static final int SEUIL_GRANDE_LISTE = 200;
    private static final int MIN_CARACTERES_FILTRE = 3;
    private final HashMap<String, Object> informationsAdditionnelles = new HashMap<>();
    @FXML
    public TableView<ObjetIHM> tableau;
    @FXML
    public Label lblEntete, lblSousTitre;
    @FXML
    public Button btnNouveau, btnClearFiltre, btnRafraichir, btnModifier,
            btnSupprimer, btnDupliquer, btnSpecial, btnLister;
    @FXML
    public Separator separateur;
    @FXML
    public VBox vboxTop;
    @FXML
    public HBox hBoxBoutons;
    @FXML
    public TextField tfFiltre;
    private EditorPaneController editorPaneController = new EditorPaneController();
    private ArrayList<ObjetIHM> listeOriginale = new ArrayList<>();
    private ObjetCSA selection = null;
    private boolean activeSuppression = true;
    private boolean activeDuplication = true;
    private boolean activeModification = true;
    private boolean activeAjout = true;
    private MaitreFX maitreFX;
    private ObjetCSA elementDeReference = null;
    private Monostable activeurIHM;
    //private boolean demandeDetails = false;
    private GenerateurColonnesTableView generateurColonnes;
    private Consumer<ObjetCSA> postCreateCallback;

    public ListTableBDController() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ListTableBD.fxml"));

        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        editorPaneController.setMaitre(this, null);
        activeurIHM = new Monostable(true, false);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void activer_topPanel(boolean activer) {
        Platform.runLater(() -> {
            if (activer) {
                setTop(vboxTop);
            } else {
                setTop(null);
            }
        });
    }

    public void activer_buttonPanel(boolean activer) {
        Platform.runLater(() -> {
            if (activer) {
                if (!vboxTop.getChildren().contains(hBoxBoutons)) {
                    vboxTop.getChildren().add(hBoxBoutons);
                }
            } else {
                vboxTop.getChildren().remove(hBoxBoutons);
            }
        });
    }


    public void activer_bouton_special(boolean on) {
        separateur.setVisible(on);
        btnSpecial.setVisible(on);
    }

    public Button getBoutonSpecial() {
        return btnSpecial;
    }

    /**
     * Rajoute des informations additionnelles à tenir compte lors de la manipulation
     * de l'élémentBD ou pour toute autre fonction jugée nécessaire
     *
     * @param nom    L'identification de la variable
     * @param donnee La variable dont il faut tenir compte
     * @return vrai si la précédente valeur à ce nom n'existe par ou est NULL
     */
    public boolean add_info_additionnelle(String nom, Object donnee) {
        return informationsAdditionnelles.put(nom, donnee) == null;
    }

    public void add_valeur_dans_reference(String nom, Object donnee) {
        if (elementDeReference != null) {
            elementDeReference.inserer_valeur(nom, donnee);
        }
    }

    public void add_fk_dans_reference(String nom, ObjetCSA donnee) {
        if (elementDeReference != null) {
            elementDeReference.inserer_fk(nom, donnee);
        }
    }

    public void add_fki_dans_reference(String nomTable, String nomVariable, ObjetCSA donnee) {
        if (elementDeReference != null) {
            elementDeReference.inserer_valeur_intermediaire(nomTable, nomVariable, donnee);
        }
    }

    public MaitreFX getMaitre() {
        return maitreFX;
    }

    public void setMaitre(MaitreFX coeur) {
        maitreFX = coeur;
    }

    public EditorPaneController getEditorPaneController() {
        return editorPaneController;
    }

    public void rafraichir_listes_editeur() {
        editorPaneController.rafraichir_listes();
    }

    /**
     * Supprime l'info additionnelle portant le nom rentré
     *
     * @param nom le nom de l'info additionnele à supprimer
     * @return vrai si la suppression est réussie car l'objet existe
     */
    public boolean remove_info_additionnelle(String nom) {
        return informationsAdditionnelles.remove(nom) != null;
    }

    public void clear_info_additionnelles() {
        informationsAdditionnelles.clear();
    }

    public HashMap<String, Object> getCopyOfInfoAdditionnelles() {
        return new HashMap<>(informationsAdditionnelles);
    }

    public void setEditeurPerso(CustomEditor customEditor) {
        if (customEditor != null) {
            editorPaneController.setCustomEditor(customEditor);
        }
    }

    public ObjetCSA getElementDeReference() {
        return elementDeReference;
    }

    public void charger_liste(ArrayList<Object> donnees, String nomTable) {
        charger_liste(donnees, maitreFX.getElementFromType(nomTable));
    }

    /**
     * Permet de charger les données dans la listeTable
     * Les données doivent être sous forme de liste
     *
     * @param donnees liste à insérer dans le tableau
     * @param ref     le type des données affichées (nomTable)
     *                si vide, alors ce sera le type du premier élément de la liste
     */
    public void charger_liste(ArrayList<Object> donnees, ObjetCSA ref) {
        elementDeReference = ref.getNewInstance();
        // permet de faire des modifs à l'objet de référence
        if (postCreateCallback != null) postCreateCallback.accept(elementDeReference);

        ArrayList<ObjetIHM> objetIHMS;
        ObjetIHM elementReferenceIHM;
        String nomEntete;
        if (elementDeReference == null) {
            elementReferenceIHM = null;
            objetIHMS = new ArrayList<>();
            nomEntete = "-";
        } else {
            nomEntete = elementDeReference.getPublicName();
            objetIHMS = TransformeurObjets.transformer_en_objetsIHM(donnees, elementDeReference, maitreFX);
            elementReferenceIHM = new ObjetIHM(elementDeReference);
        }

        generateurColonnes.setColumn(elementReferenceIHM);

        Platform.runLater(() -> {
            lblEntete.setText(nomEntete);
            ObservableList<ObjetIHM> listeTriee = FXCollections.observableArrayList(objetIHMS);
            FXCollections.sort(listeTriee); // ou tri personnalisé
            tableau.getItems().setAll(listeTriee);
        });
        activer_selection(false);
        selection = null;

        listeOriginale = new ArrayList<>(objetIHMS);

        if (maitreFX == null) {
            System.err.println("!!! La méthode 'setMaitre(MaitreFX)' n'a pas été appelée pour " + this);
            return;
        }
        maitreFX.action_utilisateur(null, this, "liste reçue");
    }

    public void setPostCreateCallback(Consumer<ObjetCSA> callback) {
        this.postCreateCallback = callback;
    }

    public ObjetCSA getSelection() {
        ObjetIHM selection = tableau.getSelectionModel().getSelectedItem();
        if (selection != null) {
            return selection.getObjetCSA();
        }
        return null;
    }

    public ArrayList<ObjetCSA> getSelections() {
        ArrayList<ObjetIHM> objetDiIHMs = new ArrayList<>(tableau.getSelectionModel().getSelectedItems());
        ArrayList<ObjetCSA> terug = new ArrayList<>();
        objetDiIHMs.forEach(elt -> {
            terug.add(elt.getObjetCSA());
        });
        return terug;
    }


    @FXML
    public void action_clic_liste(MouseEvent actionEvent) {
        Platform.runLater(() -> {
            ObjetIHM objetDiIHM = tableau.getSelectionModel().getSelectedItem();

            if (objetDiIHM != null) {
                activeSuppression = activeSuppression && objetDiIHM.getObjetCSA().estSupprimable();
                selection = objetDiIHM.getObjetCSA();
                activer_selection(true);
                lblSousTitre.setText(String.valueOf(selection));
                lblEntete.setText(selection.getPublicName());
                int nbreClics = actionEvent.getClickCount();
                String message = nbreClics == 2 ? "2 clics" : "1 clic";
                boolean modificationAutorisee = !btnModifier.isDisabled() && btnModifier.isVisible();
                if (nbreClics == 2 && activeurIHM.isStable() && modificationAutorisee) {
                    action_modifier(null);
                }
                maitreFX.action_utilisateur(selection, this, message);
                return;
            }
            lblEntete.setText("");
        });
    }

    private void activer_selection(boolean activer) {
        Platform.runLater(() -> {
            btnModifier.setDisable(!(activer && activeModification));
            btnDupliquer.setDisable(!(activer && activeDuplication));
            btnSupprimer.setDisable(!(activer && activeSuppression));
            btnLister.setDisable(!activer || selection == null || !selection.isParent());
            lblSousTitre.setText(activer ? String.valueOf(selection) : "");
        });
    }

    @FXML
    public void action_effacer_filtre(ActionEvent actionEvent) {
        Platform.runLater(() -> {
            tfFiltre.setText("");
            selection = null;
            activer_selection(false);
            filtrer("");
        });
    }

    @FXML
    public void action_nouveau(ActionEvent actionEvent) {
        if (maitreFX != null) {
            if (!activeurIHM.isStable()) {
                System.out.println("* ListTableBD/action_nouveau : doublon détecté. Ignoré !");
                return;
            }

            activeurIHM.go();

            if (elementDeReference != null) {
                affiche_utilisateur("Fenêtre d'édition pour création d'un nouvel élément");
                charger_donnees_vers_enfant(elementDeReference);
                return;
            }
            System.err.println("Didier :  ListTableBDController/action_nouveau : elementDeReference vaut null ! " + "=> ne peut pas être transformé en elementBD !");
            affiche_utilisateur("Impossible de déterminer le type d'objet à créer");
            return;
        }

        System.err.println("Le coeur du listeTable n'a pas été initialisé !");
    }

    @FXML
    public void action_modifier(ActionEvent actionEvent) {
        if (!activeurIHM.isStable()) {
            System.out.println("* ListTableBD/action_modifier : doublon détecté. Ignoré !");
            return;
        }

        activeurIHM.go();
        if (selection == null) {
            affiche_utilisateur("Aucun élément sélectionné !");
            return;
        }
        if (maitreFX == null) {
            //action_open_dialog(selection);
            System.err.println("*** La listeTable ne trouve pas son conteneur. " + "Ceci est un problème !");
            return;
        }

        if (selection.getId() > 0) {
            if (maitreFX != null) {
                affiche_utilisateur("Recherche des détails de " + selection);
                maitreFX.envoyer_vers_parent(4, selection, new RecepteurDetails());
                return;
            }
            System.err.println("*** Ordre de modification reçue mais la listeBD ne " + "connait ni contrôleur, ni conteneurFX");
            return;
        }
        charger_donnees_vers_enfant(selection);
    }


    public void ajouter(ArrayList<ObjetCSA> liste) {
        if (maitreFX != null) {
            maitreFX.envoyer_vers_parent(1, liste, this);
            return;
        }

        affiche_console("Didier : Le listeBDTable n'a pas son Gestionnaire !");
    }

    public void setSousTitre(String texte) {
        Platform.runLater(() -> {
            lblSousTitre.setText(texte);
        });
    }

    public void supprimer(ArrayList<ObjetCSA> selections) {
        if (selections.isEmpty()) {
            affiche_utilisateur("Aucune sélection détectée. Suppresion annulée !");
            return;
        }
        Object objetAEnvoyer;
        if (selections.size() == 1) {
            objetAEnvoyer = selections.get(0);
        } else {
            objetAEnvoyer = selections;
        }
        if (maitreFX != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Suppression de : " +
                    extraire_smart_text(objetAEnvoyer) +
                    "\nVeuillez confirmer, svp");
            alert.setWidth(780);
            alert.setHeight(520);
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    maitreFX.envoyer_vers_parent(3, objetAEnvoyer, this);
                } else {
                    affiche_utilisateur("Suppression annulée par l'utilisateur");
                }
            });
            return;
        }

        affiche_utilisateur("Suppression impossible");
        affiche_console("Didier : Le listeBDTable n'a pas son Gestionnaire !");
    }

    private String extraire_smart_text(Object objetAEnvoyer) {
        if (objetAEnvoyer instanceof ArrayList<?>) {
            ArrayList<?> liste = (ArrayList<?>) objetAEnvoyer;
            int nombreElts = liste.size();
            if (nombreElts > 1) {
                return nombreElts + " éléments";
            }
            return liste.get(0).toString();
        }
        return objetAEnvoyer.toString();
    }

    public void affiche_utilisateur(String s) {
        if (maitreFX != null) {
            maitreFX.affiche_console(s);
            return;
        }
        System.err.println("*** Didier : Le listeBDTable n'a pas son Gestionnaire !");
    }

    @Override
    public void envoyer_vers_parent(int code, Object objet, GestionnaireRetour appelant) {
        if (appelant != null) {
            maitreFX.envoyer_vers_parent(code, objet, appelant);
            return;
        }
        if (objet instanceof ObjetCSA) {
            ObjetCSA elementBD = (ObjetCSA) objet;
            if (elementBD.getId() > 0 && code < 2) {
                System.err.println("*** Didier : ListTableBD ne peut créer un nouvel élément avec id !");
                return;
            }
            if (maitreFX != null) {
                maitreFX.envoyer_vers_parent(code, elementBD, this);
                return;
            }
            affiche_console("Didier : Le listeBDTable n'a pas son MaitreFX ! L'envoi vers le parent est " +
                    "ignoré.");
            return;
        }

        System.out.println("*** ListTableBD/requete_BD ne peut envoyer l'objet : " + objet);
    }


    public void affiche_console(String s) {
        if (maitreFX == null) {
            System.err.println("** Didier : Le listeBDTable n'a pas son MaitreFX !" + "\n => " + s);
            return;
        }
        maitreFX.affiche_console(s);
    }

    @Override
    public ObjetCSA getElementFromType(String typeAffiche) {
        if (maitreFX != null) {
            return maitreFX.getElementFromType(typeAffiche);
        }
        return null;
    }

    @Override
    public void action_utilisateur(Object selectedItem, Object source, String extraInfo) {
        // un de mes enfants me sollicite. Si je ne sais pas quoi faire, je fais remonter la patate chaude
        if (maitreFX == null) {
            affiche_console("ListTableBD " + this + " : je reçois un appel de " + source + " pour " +
                    selectedItem + ", mais " +
                    "je ne sais pas quoi en faire, donc j'ignore puisque je n'ai pas de maitreFX !");
            return;
        }
        maitreFX.action_utilisateur(selectedItem, source, extraInfo);
    }

    @Override
    public void affiche_details(Object selectedItem) {
        throw new UnsupportedOperationException("ListTableBD n'affiche pas les détails");
    }

    @Override
    public void nettoyer_champs() {
        action_nouveau(null);
    }

    @Override
    public void charger_donnees_vers_enfant(ObjetCSA objetDi) {
        CustomEditor customEditor = editorPaneController.getEditeurPersonnel();
        editorPaneController = EditorPaneController.openEditor(objetDi.getCopy(), this, customEditor, "");
    }

    @Override
    public void rafraichir() {
        action_rafraichir(null);
    }


    @Override
    public boolean isTest() {
        if (maitreFX == null) {
            return false;
        }
        return maitreFX.isTest();
    }

    public boolean isEditorOpened() {
        try {
            return editorPaneController.getEditeurPersonnel().getScene().getWindow().isShowing();
        } catch (Exception e) {
            return false;
        }
    }


    @FXML
    public void action_supprimer(ActionEvent actionEvent) {
        if (!activeurIHM.isStable()) {
            return;
        }
        activeurIHM.go();

        ObservableList<ObjetIHM> selections = tableau.getSelectionModel().getSelectedItems();

        ArrayList<ObjetCSA> objetDis = new ArrayList<>();
        selections.forEach(elt -> {
            objetDis.add(elt.getObjetCSA());
        });
        ArrayList<ObjetCSA> aSupprimer = new ArrayList<>(objetDis);
        supprimer(aSupprimer);
    }

    @FXML
    public void action_dupliquer(ActionEvent actionEvent) {
        if (!activeurIHM.isStable()) {
            System.out.println("* ListTableBD/action_dupliquer : doublon détecté. Ignoré !");
            return;
        }
        activeurIHM.go();

        ObjetIHM coquille = tableau.getSelectionModel().getSelectedItem();
        if (coquille != null && !activeurIHM.isStable()) {
            coquille.setId(0);
            coquille.setNom("");
            charger_donnees_vers_enfant(coquille.getObjetCSA());
            activeurIHM.go();
        } else {
            affiche_utilisateur("Aucun élément sélectionné !");
        }
    }

    @FXML
    public void action_rafraichir(ActionEvent actionEvent) {
        if (maitreFX == null) {
            System.err.println("ListeTableBD : Impossible de rafraichir : je ne connais pas le type de données que "
                    + "je suis supposé afficher. Je n'ai pas de MaitreFX.");
            return;
        }

        tableau.getItems().clear();
        maitreFX.rafraichir();
    }

    public void select(ObjetCSA o) {
        if (o == null) {
            clearSelection();
        } else {
            tableau.getSelectionModel().select(new ObjetIHM(o));
        }
    }

    public void clearSelection() {
        tableau.getSelectionModel().clearSelection();
        selection = null;
        activer_selection(false);
    }

    public void clear() {
        listeOriginale.clear();
        selection = null;
        elementDeReference = null;
        Platform.runLater(() -> {
            tableau.getItems().clear();
        });

    }

    public ArrayList<ObjetIHM> getListeObjetIHMsComplete() {
        return new ArrayList<>(listeOriginale);
    }


    public ArrayList<ObjetCSA> getListeObjetsCSAComplete() {
        return TransformeurObjets.transformer_en_objetsDi(listeOriginale);
    }

    /**
     * Renvoie une copie de la liste affichée
     *
     * @return une copie de la liste affichée !
     */
    public ArrayList<ObjetCSA> getListeAffichee() {
        return TransformeurObjets.transformer_en_objetsDi(tableau.getItems());
    }

    public ArrayList<ObjetCSA> getElementsSelectionnes() {
        return TransformeurObjets.transformer_en_objetsDi(tableau.getSelectionModel().getSelectedItems());
    }

    @FXML
    public void action_filtrer(KeyEvent keyEvent) {
        Platform.runLater(() -> {
            String texteFiltre = tfFiltre.getText();

            if (texteFiltre.isEmpty()) {
                tableau.getItems().setAll(listeOriginale);
                if (selection == null) {
                    clearSelection();
                } else {
                    tableau.getSelectionModel().select(new ObjetIHM(selection));
                }
                tableau.refresh();
            } else {
                // Si grande liste, exiger minimum de caractères
                if (listeOriginale.size() > SEUIL_GRANDE_LISTE &&
                        texteFiltre.length() < MIN_CARACTERES_FILTRE) {
                    return; // Ne pas filtrer
                }

                if (listeOriginale.isEmpty()) {
                    ObjetCSA nouvelElt = elementDeReference.getCopy();
                    nouvelElt.inserer_valeur("filtre", texteFiltre);
                    envoyer_vers_parent(4, nouvelElt, this);
                } else {
                    filtrer(texteFiltre);
                }
            }
        });
    }

    public void filtrer(String texteFiltre) {
        Platform.runLater(() -> {
            String texte = Normalizer.normalize(texteFiltre.trim().toUpperCase(), Normalizer.Form.NFD).replaceAll("[\u0300-\u036F]", "");
            if (texte.isEmpty()) {
                tableau.getItems().setAll(listeOriginale);
            } else {
                ArrayList<ObjetIHM> inclus = new ArrayList<>();
                listeOriginale.forEach((elt) -> {
                    String selection = Normalizer.normalize(elt.getObjetCSA().getNomAFiltrer().trim().toUpperCase(),
                            Normalizer.Form.NFD).replaceAll("[\u0300-\u036F]", "");

                    if (selection.startsWith(texte)) {
                        inclus.add(elt);
                    }
                });

                tableau.getItems().setAll(inclus);
            }

            if (selection != null) {
                tableau.getSelectionModel().select(new ObjetIHM(selection));
            } else {
                clearSelection();
            }
        });
    }

    /**
     * Permet de n'afficher que les objetsDi NON proposés dans la liste fournie
     * La liste complète est conservée en mémoire
     *
     * @param objetDis la liste des objetDis à cacher
     */
    public void filtrer(ArrayList<? extends ObjetCSA> objetDis) {
        Platform.runLater(() -> {
            if (objetDis.isEmpty()) {
                tableau.getItems().setAll(listeOriginale);
            } else {
                ArrayList<ObjetIHM> inclus = new ArrayList<>();
                listeOriginale.forEach((elt) -> {
                    if (!objetDis.contains(elt.getObjetCSA())) {
                        inclus.add(elt);
                    }
                });

                tableau.getItems().setAll(inclus);
            }

            if (selection != null) {
                tableau.getSelectionModel().select(new ObjetIHM(selection));
            } else {
                clearSelection();
            }
        });
    }


    @FXML
    public void action_chercher_sousListe(ActionEvent event) {
        ObjetIHM selection = tableau.getSelectionModel().getSelectedItem();
        if (selection != null) {
            maitreFX.action_utilisateur(selection.getObjetCSA(), this, "chercher_sousListe");
            return;
        }
        affiche_utilisateur("Aucune sélection détectée !");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        generateurColonnes = new GenerateurColonnesTableView(tableau, maitreFX);
        btnNouveau.setTooltip(new Tooltip("Ajouter un nouvel élément"));
        btnClearFiltre.setTooltip(new Tooltip("Effacer le filtre"));
        btnRafraichir.setTooltip(new Tooltip("Rafraichir la liste"));
        btnModifier.setTooltip(new Tooltip("Éditer la sélection"));
        btnDupliquer.setTooltip(new Tooltip("Créer un nouveau à partir de la sélection"));
        btnLister.setTooltip(new Tooltip("Ouvrir une sous-liste liée à l'élément sélectionné"));
        btnSupprimer.setTooltip(new Tooltip("Supprimer l'élément sélectionné"));
        tableau.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableau.setPlaceholder(new Label("Aucun élément dans la liste"));
        //tableau.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Add listener to adjust column widths when TableView's width changes
        tableau.widthProperty().addListener(
                (obs, oldWidth, newWidth) -> adjustColumnWidths(tableau));

        // Add listener to adjust column widths when columns are added or removed
        tableau.getColumns().addListener((ListChangeListener<TableColumn<ObjetIHM, ?>>)
                change -> adjustColumnWidths(tableau));

        if (listeOriginale.size() > SEUIL_GRANDE_LISTE) {
            tfFiltre.setPromptText("Min. " + MIN_CARACTERES_FILTRE + " caractères");
        }
    }

    private void adjustColumnWidths(TableView<ObjetIHM> tableView) {
        double tableWidth = tableView.getWidth();
        int columnCount = tableView.getColumns().size();
        if (columnCount > 0) {
            double colWidth = tableWidth / columnCount;
            for (TableColumn<ObjetIHM, ?> column : tableView.getColumns()) {
                column.setPrefWidth(colWidth);
            }
        }
    }

    public void addListeANePasAfficher(String nom) {
        generateurColonnes.getListeEltsANePasAfficher().add(nom);
    }

    public void clearListeANePasAfficher() {
        generateurColonnes.getListeEltsANePasAfficher().clear();
    }

    private void debug(String texte) {
        System.out.println("===== DEBUG =====");
        System.out.println(texte);
        System.out.println("===== DEBUG =====");
    }


    private String affiche_couleur(Color couleur) {
        int opacite = (int) (couleur.getOpacity() * 255);
        int rouge = (int) (couleur.getRed() * 255);
        int vert = (int) (couleur.getGreen() * 255);
        int bleu = (int) (couleur.getBlue() * 255);

        return Integer.toHexString(opacite) + Integer.toHexString(rouge) + Integer.toHexString(vert) + Integer.toHexString(bleu) + " (" + (opacite + rouge + vert + bleu) + ")";
    }

    private String firstCapital(String nom) {
        if (nom.isEmpty()) {
            return "";
        }
        if (nom.length() < 2) {
            return nom.toUpperCase();
        }
        Character initiale = nom.toCharArray()[0];
        return (String.valueOf(initiale)).toUpperCase() + nom.substring(1);
    }

    public boolean isActiveSuppression() {
        return activeSuppression;
    }

    public void setActiveSuppression(boolean activeSuppression) {
        this.activeSuppression = activeSuppression;
    }

    public boolean isActiveDuplication() {
        return activeDuplication;
    }

    public void setActiveDuplication(boolean activeDuplication) {
        this.activeDuplication = activeDuplication;
    }

    public boolean isActiveModification() {
        return activeModification;
    }

    public void setActiveModification(boolean activeModification) {
        this.activeModification = activeModification;
    }

    public boolean isActiveAjout() {
        return activeAjout;
    }

    public void setActiveAjout(boolean activeAjout) {
        this.activeAjout = activeAjout;
    }

    @Override
    public void traitementRetour(ArrayList list) {
        if (list == null) {
            System.err.println("La listeTable reçoit une liste NULL !");
            return;
        }
        if (list.isEmpty()) {
            System.err.println("La listeTable reçoit une liste vide de la BD !");
            return;
        }
        // on doit recevoir une AL => (int, nomTable, (liste d'objetDiIHM)) ou juste une AL => (int)
        if (list.get(0) instanceof Integer) {
            int code = (int) list.get(0);
            affiche_console("Info : la listeTable " + (elementDeReference == null ? "NULL" : elementDeReference.getType()) + " reçoit un code " + code);
            switch (code) {
                case 1:
                case 2:
                    affiche_utilisateur("Enregistrement réussi !");
                    action_rafraichir(null);
                    return;
                case 3:
                    affiche_utilisateur("Suppression réussie !");
                    action_rafraichir(null);
                    return;
                case 4:
                    if (list.size() < 3) {
                        System.err.println("*** paquet reçu incorrect dans ListTableBD : " + list + "\n...mais j'attendais une liste de 3 éléments : (int, String, liste)");
                        break;
                    }
                    String nomTable = String.valueOf(list.get(1));
                    Object liste = list.get(2);
                    if (liste instanceof ArrayList) {
                        ArrayList<Object> listeRecue = (ArrayList) liste;
                        int taille = listeRecue.size();
                        affiche_console("* la listeTable reçoit (en direct) une liste de taille " + taille);
                        charger_liste(listeRecue, maitreFX.getElementFromType(nomTable));
                        return;
                    }
                    System.err.println("*** Retour de envoi détail incorrect dans ListTableBD : " + list);
                    return;
                default:
                    affiche_utilisateur("Échec de l'opération (code " + code + ")");
                    System.err.println("*** La listeTable reçoit un retour de requête inattendu " + list + "\n...je ne fais rien !");
                    return;
            }
            return;
        }
        System.err.println("La listeTable reçoit une requête non conforme : " + list);
    }


    /*
     ************  CLASSES INTERNES *****************
     */
    private class RecepteurDetails implements GestionnaireRetour {
        @Override
        public void traitementRetour(ArrayList p) {
            if (p.isEmpty()) {
                System.err.println("La listeTable reçoit une liste vide de la BD !");
                return;
            }
            if (p.get(0) instanceof Integer) {
                int code = (int) p.get(0);
                affiche_console("Info : la listeTable " + (elementDeReference == null ? "NULL" : elementDeReference.getClass().getSimpleName()) + " reçoit un code " + code);
                if (code == 4) {
                    if (p.size() < 3) {
                        System.err.println("*** paquet reçu incorrect dans ListTableBD (recepteur détail) : " + p + "\n...mais j'attendais une liste de 3 éléments : (int, String, liste)");
                        return;
                    }
                    String nomTable = String.valueOf(p.get(1));
                    Object element = p.get(2);
                    if (element instanceof ArrayList) {
                        ArrayList<?> listeRecue = (ArrayList) element;
                        int taille = listeRecue.size();
                        if (taille == 1 && listeRecue.get(0) instanceof ObjetCSA) {
                            ObjetCSA elementBD = (ObjetCSA) listeRecue.get(0);
                            elementBD.clearModifications();
                            charger_donnees_vers_enfant(elementBD);
                            affiche_console("* : Détail de " + elementBD + " reçu sans liste");
                            return;
                        }
                        affiche_console("** la listeTable reçoit (dans le récepteur détails), " + "une liste de taille " + taille + "\n... alors qu'un seul élément est attendu !" + "\n...=> " + listeRecue);
                        return;
                    }
                    System.err.println("*** Retour de envoi détail incorrect dans ListTableBD : " + p);
                    return;
                }
                System.err.println("*** La listeTable reçoit un retour de requête inattendu " + p + "\nJe m'attends à soit une liste, soit un objetDiIHM !");
                return;
            }
            System.err.println("La listeTable reçoit une requête non conforme : " + p);
        }
    }
}
