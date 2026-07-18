package controlsFX;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

import general.GestionnaireRetour;
import general.Monostable;
import general.ObjetCSA;
import general.TransformeurObjets;
import generalFX.CreateurArborescence;
import generalFX.CustomEditor;
import generalFX.FX_RGBConverter;
import generalFX.ObjetIHM;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class TreeViewBDController extends BorderPane implements Initializable, MaitreFX, GestionnaireRetour {
    private final static int TAILLE_CARACTERE = 10;
    private final Scene scene;
    private final HashMap<String, Object> informationsAdditionnelles = new HashMap<>();
    @FXML
    public TreeTableView<ObjetIHM> tableau;
    @FXML
    public Label lblEntete, lblSousTitre;
    @FXML
    public Button btnNouveau, btnRafraichir, btnModifier,
            btnSupprimer, btnDupliquer, btnSpecial;
    @FXML
    public Separator separateur;
    private EditorPaneController editorPaneController = new EditorPaneController();
    private ObjetCSA selection = null;
    private boolean activeSuppression = true;
    private boolean activeDuplication = true;
    private boolean activeModification = true;
    private String titreExterne = "";
    private MaitreFX maitreFX;
    private ObjetCSA elementDeReference = null;
    private Monostable activeurIHM;
    private ArrayList<String> listeEltsANePasAfficher = new ArrayList<>();
    /** Factory pour créer le CustomEditor associé à chaque EditorPane ouvert. */
    private java.util.function.Function<EditorPaneController, CustomEditor> customEditorFactory = null;

    public TreeViewBDController() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/TreeViewBD.fxml"));

        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        editorPaneController.setMaitre(this, null);
        scene = new Scene(editorPaneController);
        activeurIHM = new Monostable(true, false);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private TreeTableColumn<ObjetIHM, ObservableList> getObjetCSAIHMObservableListTableColumn(String nomVariable,
            String titre) {
        TreeTableColumn<ObjetIHM, ObservableList> colonne = new TreeTableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<ObservableList>) oihm.getValue().getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Float)");
                return null;
            }
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private TreeTableColumn<ObjetIHM, Color> getObjetCSAIHMColorTableColumn(String nomVariable, String titre) {
        TreeTableColumn<ObjetIHM, Color> colonne = new TreeTableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<Color>) oihm.getValue().getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Float)");
                return null;
            }
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    public void activer_bouton_special(boolean on) {
        separateur.setVisible(on);
        btnSpecial.setVisible(on);
    }

    public Button getBoutonSpecial() {
        return btnSpecial;
    }

    private TreeTableColumn<ObjetIHM, LocalDate> getObjetCSAIHMLocalDateTableColumn(String nomVariable, String titre) {
        TreeTableColumn<ObjetIHM, LocalDate> colonne = new TreeTableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<LocalDate>) oihm.getValue().getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "LocalDate)");
                return null;
            }
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private TreeTableColumn<ObjetIHM, LocalDateTime> getObjetCSAIHMLocalDateTimeTableColumn(String nomVariable,
            String titre) {
        TreeTableColumn<ObjetIHM, LocalDateTime> colonne = new TreeTableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<LocalDateTime>) oihm.getValue().getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "LocalDateTime)");
                return null;
            }
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private TreeTableColumn<ObjetIHM, Float> getObjetCSAIHMFloatTableColumn(String nomVariable, String titre) {
        TreeTableColumn<ObjetIHM, Float> colonne = new TreeTableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<Float>) oihm.getValue().getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Float)");
                return null;
            }
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private TreeTableColumn<ObjetIHM, ObjetCSA> getObjetCSAIHMObjetCSATableColumn(String nomVariable, String titre) {
        TreeTableColumn<ObjetIHM, ObjetCSA> colonne = new TreeTableColumn<>(titre);
        colonne.setCellValueFactory(cellDataFeatures -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<ObjetCSA>) cellDataFeatures.getValue().getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "String)");
                return null;
            }
        });

        colonne.setCellFactory((tableColumn) -> {
            return new TreeTableCell<ObjetIHM, ObjetCSA>() {
                @Override
                protected void updateItem(ObjetCSA item, boolean empty) {
                    super.updateItem(item, empty);

                    this.setText(null);
                    this.setGraphic(null);

                    if (item != null && !empty) {
                        this.setText(item.getNom().toUpperCase());
                    }
                }
            };
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private TreeTableColumn<ObjetIHM, String> getObjetCSAIHMStringTableColumn(String nomVariable, String titre) {
        TreeTableColumn<ObjetIHM, String> colonne = new TreeTableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                TreeItem<ObjetIHM> treeItem = oihm.getValue();
                return (ObservableValue<String>) treeItem.getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "String)");
                return null;
            }
        });

        colonne.setCellFactory((tableColumn) -> {
            return new TreeTableCell<ObjetIHM, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    this.setText(null);
                    this.setGraphic(null);

                    if (item != null && !empty) {
                        this.setText(item.toUpperCase());
                    }
                }
            };
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private TreeTableColumn<ObjetIHM, Object> getObjetCSAIHMGeneralTableColumn(String nomVariable, String titre) {
        TreeTableColumn<ObjetIHM, Object> colonne = new TreeTableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<Object>) oihm.getValue().getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Object)");
                return null;
            }
        });

        colonne.setCellFactory((tableColumn) -> {
            return new TreeTableCell<ObjetIHM, Object>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);

                    this.setText(null);
                    this.setGraphic(null);

                    if (item != null && !empty) {
                        this.setText(String.valueOf(item).toUpperCase());
                    }
                }
            };
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private TreeTableColumn<ObjetIHM, Integer> getObjetCSAIHMIntegerTableColumn(String nomVariable, String titre) {
        TreeTableColumn<ObjetIHM, Integer> colonne = new TreeTableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<Integer>) oihm.getValue().getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Integer)");
                return null;
            }
        });
        if (nomVariable.toLowerCase().contains("ouleur")) {
            colonne.setCellFactory((tableColumn) -> {
                return new TreeTableCell<ObjetIHM, Integer>() {
                    @Override
                    protected void updateItem(Integer item, boolean empty) {
                        super.updateItem(item, empty);

                        if (!empty) {
                            Rectangle rectangle = new Rectangle(50, 30);
                            rectangle.setArcHeight(5);
                            rectangle.setArcWidth(5);
                            if (item == null) {
                                this.setText("ERROR !");
                                Color couleur = Color.RED;
                                rectangle.setFill(couleur);
                                System.err.println("*** ListeTable : je n'arrive pas à afficher "
                                        + "une couleur écrite sous forme d'integer !");
                            } else {
                                Color couleur = FX_RGBConverter.convert_rgb_2_color(item);
                                rectangle.setFill(couleur);
                                // System.out.println("\nDEBUG : listTable affiche une couleur " +
                                // affiche_couleur(couleur)
                                // + "de valeur entière : " + item);
                            }
                            this.setGraphic(rectangle);
                        }
                    }
                };
            });
        }
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private TreeTableColumn<ObjetIHM, java.awt.Color> getDiIHMColorTableColumn(String nomVariable, String titre) {
        TreeTableColumn<ObjetIHM, java.awt.Color> colonne = new TreeTableColumn<>(titre);
        colonne.setCellFactory((tableColumn) -> {
            TreeTableCell<ObjetIHM, java.awt.Color> tableCell = new TreeTableCell<ObjetIHM, java.awt.Color>() {
                @Override
                protected void updateItem(java.awt.Color item, boolean empty) {
                    super.updateItem(item, empty);

                    this.setText(null);
                    this.setGraphic(null);

                    if (!empty && item != null) {
                        Rectangle rectangle = new Rectangle(50, 30);
                        rectangle.setArcHeight(5);
                        rectangle.setArcWidth(5);
                        rectangle.setFill(FX_RGBConverter.convert_rgb_2_color(item.getRGB()));
                        this.setGraphic(rectangle);
                    }
                }
            };
            return tableCell;
        });
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<java.awt.Color>) oihm.getValue().getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Color)");
                return null;
            }
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    /**
     * Rajoute des informations additionnelles à tenir compte lors de la
     * manipulation
     * de l'élémentBD ou pour toute autre fonction jugée nécessaire
     *
     * @param nom    L'identification de la variable
     * @param donnee La variable dont il faut tenir compte
     * @return vrai si la précédente valeur à ce nom n'existe par ou est NULL
     */
    public boolean add_info_additionnelle(String nom, Object donnee) {
        return informationsAdditionnelles.put(nom, donnee) == null;
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

    public void setEditeurPerso(java.util.function.Function<EditorPaneController, CustomEditor> factory) {
        this.customEditorFactory = factory;
        if (factory != null) {
            editorPaneController.setCustomEditor(factory);
        }
    }

    public ObjetCSA getElementDeReference() {
        return elementDeReference;
    }

    /*
     * public void ajouter_liste(ArrayList<Object> donnees, String type, ObjetCSA
     * parent) {
     * ArrayList<ObjetCSA> objetIHMS =
     * TransformeurObjets.transformer_en_objetsDi(donnees,type,parent);
     * Platform.runLater(() -> {
     * TreeItem branche =
     * chercher_dans_arborescence(tableau.getRoot().getChildren(), parent);
     * if (branche != null) {
     * objetIHMS.forEach(objetDiIHM -> {
     * objetDiIHM.getObjetCSA().setParent(parent);
     * });
     * branche.getChildren().addAll(CreateurArborescence.creer_arborescence(
     * objetIHMS, parent));
     * return;
     * }
     * System.err.println("TreeViewController/ajouter_liste : la liste de type " +
     * type + " n'a pas trouvé de " +
     * "parent d'id = " + parent.getId());
     * });
     * }
     */

    private TreeItem<ObjetIHM> chercher_dans_arborescence(List<TreeItem<ObjetIHM>> parents, ObjetCSA parent) {
        for (TreeItem<ObjetIHM> treeItem : parents) {
            if (treeItem.getValue().getId() == parent.getId()) {
                return treeItem;
            }
            TreeItem<ObjetIHM> rechercher_sous_elements = chercher_dans_arborescence(treeItem.getChildren(), parent);
            if (rechercher_sous_elements != null) {
                return rechercher_sous_elements;
            }
        }
        return null;
    }

    /**
     * Permet de charger les données dans la listeTable
     * Les données doivent être sous forme de liste d'ElementBD
     *
     * @param donnees liste à insérer dans le tableau
     * @param type    le type des données affichées (nomTable)
     */
    public void charger_liste(ArrayList<Object> donnees, String type) {
        elementDeReference = maitreFX.getElementFromType(type);
        String nomEntete;
        if (elementDeReference == null) {
            System.err.println("*** La liste chargée dans la listeTable est "
                    + "de type non géré par son propre appelant => " + type);
            nomEntete = type;
        } else {
            nomEntete = elementDeReference.getPublicName();
        }

        boolean readonly = editorPaneController.getEditeurPersonnel() == null;
        btnNouveau.setDisable(readonly);
        activeDuplication = !readonly;
        activeModification = !readonly;
        activeSuppression = !readonly;

        ArrayList<ObjetCSA> objetDis = TransformeurObjets.transformer_en_objetsCSA(donnees, type, null, maitreFX);
        ObjetIHM elementReferenceIHM = new ObjetIHM(elementDeReference);
        Platform.runLater(() -> {
            setColumn(elementReferenceIHM);
            lblEntete.setText(elementDeReference.getPublicName());
            TreeItem root = new TreeItem(elementReferenceIHM);
            ArrayList<TreeItem<ObjetIHM>> items = CreateurArborescence.creer_arborescence(objetDis,
                    ObjetCSA.creer_objet_vide());
            root.getChildren().setAll(items);
            tableau.setRoot(root);
            root.setExpanded(true);
            tableau.setShowRoot(false);
            activer_selection(false);
            selection = null;
        });
    }

    public ObjetCSA getSelection() {
        try {
            return tableau.getSelectionModel().getSelectedItem().getValue().getObjetCSA();
        } catch (Exception e) {
            return null;
        }
    }

    public ArrayList<ObjetCSA> getSelections() {
        ArrayList<TreeItem<ObjetIHM>> items = new ArrayList<>(tableau.getSelectionModel().getSelectedItems());
        ArrayList<ObjetCSA> terug = new ArrayList<>();
        items.forEach(elt -> {
            terug.add(elt.getValue().getObjetCSA());
        });
        return terug;
    }

    @FXML
    public void action_clic_liste(MouseEvent actionEvent) {
        TreeItem<ObjetIHM> treeItem = tableau.getSelectionModel().getSelectedItem();
        if (treeItem == null) {
            affiche_utilisateur("Aucune sélection");
            return;
        }
        ObjetIHM objetIHM = treeItem.getValue();

        if (objetIHM != null) {
            selection = objetIHM.getObjetCSA();
            activer_selection(true);
            lblEntete.setText(String.valueOf(selection));
            lblSousTitre.setText(selection.getPublicName());
            int nbreClics = actionEvent.getClickCount();
            String message = nbreClics == 2 ? "2 clics" : "1 clic";
            if (nbreClics == 2 && activeurIHM.isStable()) {
                action_modifier(null);
            }
            maitreFX.action_utilisateur(selection, this, message);
            return;
        }
        lblEntete.setText("");
    }

    private void activer_selection(boolean sel) {
        btnModifier.setDisable(!(sel && activeModification));
        btnDupliquer.setDisable(!(sel && activeDuplication));
        btnSupprimer.setDisable(!(sel && activeSuppression));
        lblEntete.setText(sel ? String.valueOf(selection) : titreExterne);
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
            System.err.println("Didier :  ListTableBDController/action_nouveau : elementDeReference vaut null ! "
                    + "=> ne peut pas être transformé en elementBD !");
            affiche_utilisateur("Impossible de déterminer le type d'objet à créer");
            return;
        }

        System.err.println("Le coeur du listeTable n'a pas été initialisé !");
    }

    @FXML
    public void action_modifier(ActionEvent actionEvent) {
        if (!activeurIHM.isStable()) {
            System.out.println("* TreeViewBD/action_modifier : doublon détecté. Ignoré !");
            return;
        }
        activeurIHM.go();
        if (selection == null) {
            affiche_utilisateur("Aucun élément sélectionné !");
            return;
        }
        if (maitreFX == null) {
            // action_open_dialog(selection);
            System.err.println("*** La listeTable ne trouve pas son conteneur. " + "Ceci est un problème !");
            return;
        }
        if (selection.getId() > 0) {
            if (maitreFX != null) {
                maitreFX.appeler_bd(4, selection, new RecepteurDetails());
                return;
            }
            System.err.println(
                    "*** Ordre de modification reçue mais la listeBD ne " + "connait ni contrôleur, ni conteneurFX");
            return;
        }
        charger_donnees_vers_enfant(selection);
    }

    public void action_retour_demande_details(ObjetCSA eltDetaille) {
        eltDetaille.clearModifications();
        charger_donnees_vers_enfant(eltDetaille);
    }

    public void ajouter(ArrayList<ObjetCSA> liste) {
        if (maitreFX != null) {
            maitreFX.appeler_bd(1, liste, this);
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
        ObjetCSA coquille = selections.get(0);
        if (selections.size() > 1) {
            affiche_utilisateur("Opération de suppression autorisée sur un seul élément à"
                    + " la fois. Suppression du premier de la liste ...");
        }
        if (maitreFX != null) {
            if (coquille.getId() > 0) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Suppression de l'objet : " + coquille.getNom() + "\nVeuillez confirmer, svp");
                alert.setWidth(780);
                alert.setHeight(520);
                generalFX.StylesheetsFX.apply(alert, this);
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        maitreFX.appeler_bd(3, selections, this);
                    } else {
                        affiche_utilisateur("Suppression annulée par l'utilisateur");
                    }
                });
            }
            return;
        }

        affiche_utilisateur("Suppression impossible");
        affiche_console("Didier : Le listeBDTable n'a pas son Gestionnaire !");
    }

    public void affiche_utilisateur(String s) {
        if (maitreFX != null) {
            maitreFX.affiche_console(s);
            return;
        }
        System.err.println("*** Didier : Le listeBDTable n'a pas son Gestionnaire !");
    }

    @Override
    public void appeler_bd(int code, Object objet, GestionnaireRetour appelant) {
        if (appelant != null) {
            maitreFX.appeler_bd(code, objet, appelant);
            return;
        }
        if (objet instanceof ObjetCSA) {
            ObjetCSA elementBD = (ObjetCSA) objet;
            if (elementBD.getId() > 0 && code < 2) {
                System.err.println("*** Didier : ListTableBD ne peut créer un nouvel élément avec id !");
                return;
            }
            envoyer_requete(code, elementBD);
            return;
        }

        System.out.println("*** ListTableBD/requete_BD ne peut envoyer l'objet : " + objet);
    }

    private void envoyer_requete(int code, Object o) {

        if (maitreFX != null) {
            maitreFX.appeler_bd(code, o, this);
            return;
        }

        affiche_console("Didier : Le listeBDTable n'a pas son Gestionnaire !");
    }

    public void affiche_console(String s) {
        if (maitreFX != null) {
            maitreFX.affiche_console(s);
            return;
        }
        System.err.println("*** Didier : Le listeBDTable n'a pas son Gestionnaire !" + "\n => " + s);
    }

    @Override
    public ObjetCSA getElementFromType(String typeAffiche) {
        if (maitreFX != null) {
            return maitreFX.getElementFromType(typeAffiche);
        }
        return null;
    }

    @Override
    public void action_utilisateur(Object selectedItem, Object source, String nomColonne) {
        System.err.println("La ListTableBDController ne gère pas les actions" + " utilisateur de ses enfants !");
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
        editorPaneController = EditorPaneController.openEditor(objetDi.getCopy(), this, customEditorFactory, "");
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

    public void afficher_entete(String titre) {
        Platform.runLater(() -> {
            lblEntete.setText(titre);
        });
        titreExterne = titre;
    }

    @FXML
    public void action_supprimer(ActionEvent actionEvent) {
        if (!activeurIHM.isStable()) {
            return;
        }
        activeurIHM.go();

        ObservableList<TreeItem<ObjetIHM>> selections = tableau.getSelectionModel().getSelectedItems();

        ArrayList<ObjetCSA> objetDis = new ArrayList<>();
        selections.forEach(elt -> {
            objetDis.add(elt.getValue().getObjetCSA());
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

        ObjetIHM coquille = tableau.getSelectionModel().getSelectedItem().getValue();
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
        Platform.runLater(() -> {
            tableau.setRoot(null);
        });

        maitreFX.rafraichir();
        elementDeReference = null;
    }

    public void select(ObjetCSA o) {
        if (o == null) {
            clearSelection();
        } else {
            tableau.getSelectionModel().select(new TreeItem<>(new ObjetIHM(o)));
        }
    }

    public void clearSelection() {
        tableau.getSelectionModel().clearSelection();
        selection = null;
        activer_selection(false);
    }

    public void clear() {
        selection = null;
        elementDeReference = null;
        Platform.runLater(() -> {
            tableau.setRoot(null);
        });

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        btnNouveau.setTooltip(new Tooltip("Ajouter un nouvel élément"));
        btnRafraichir.setTooltip(new Tooltip("Rafraichir la liste"));
        btnModifier.setTooltip(new Tooltip("Éditer la sélection"));
        btnDupliquer.setTooltip(new Tooltip("Créer un nouveau à partir de la sélection"));
        btnSupprimer.setTooltip(new Tooltip("Supprimer l'élément sélectionné"));
        tableau.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableau.setPlaceholder(new Label("Aucun élément dans la liste"));
        tableau.setColumnResizePolicy(TreeTableView.UNCONSTRAINED_RESIZE_POLICY);
        tableau.setRowFactory(ttv -> new TreeTableRow<ObjetIHM>() {
            @Override
            protected void updateItem(ObjetIHM item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    setStyle(getTreeItem().isLeaf() ? "-fx-font-weight: normal;" : "-fx-font-weight: bold;");
                }
            }
        });
    }

    public void addListeANePasAfficher(String nom) {
        listeEltsANePasAfficher.add(nom);
    }

    public void clearListeANePasAfficher() {
        listeEltsANePasAfficher.clear();
    }

    private void debug(String texte) {
        System.out.println("===== DEBUG =====");
        System.out.println(texte);
        System.out.println("===== DEBUG =====");
    }

    /**
     * Permet de charger le tableau de données vierges
     *
     * @param objetIHM chaque colonne doit avoir un titre et un type qui doivent
     *                 se trouver dans un ElementBD
     */
    public void setColumn(ObjetIHM objetIHM) {
        if (objetIHM == null) {
            affiche_console("* ListeTableBD/setColumn : Aucune donnée à charger, je reçois NULL !");
            return;
        }
        Platform.runLater(() -> {
            tableau.getColumns().clear();
            ArrayList<String> variablesVisibles = objetIHM.getVariablesVisibles();
            variablesVisibles.removeAll(listeEltsANePasAfficher);
            for (String nomVariable : variablesVisibles) {
                String titre = objetIHM.getNomPublic(nomVariable);
                Object variable = objetIHM.getObjetCSA().getSimpleData(nomVariable);
                Class classeVariable = objetIHM.getTypeOfVariable(nomVariable);
                if (classeVariable != null) {
                    if (variable == null) {
                        TreeTableColumn<ObjetIHM, Object> colonne = getObjetCSAIHMGeneralTableColumn(nomVariable,
                                titre);
                        tableau.getColumns().add(colonne);
                    } else if (variable instanceof String || classeVariable.equals(String.class)) {
                        TreeTableColumn<ObjetIHM, String> colonne = getObjetCSAIHMStringTableColumn(nomVariable, titre);
                        tableau.getColumns().add(colonne);
                    } else if (variable instanceof ObjetCSA) {
                        TreeTableColumn<ObjetIHM, ObjetCSA> colonne = getObjetCSAIHMObjetCSATableColumn(nomVariable,
                                titre);
                        tableau.getColumns().add(colonne);
                    } else if (variable instanceof Integer || classeVariable.equals(Integer.class)) {
                        TreeTableColumn<ObjetIHM, Integer> colonne = getObjetCSAIHMIntegerTableColumn(nomVariable,
                                titre);
                        tableau.getColumns().add(colonne);
                    } else if (variable instanceof java.awt.Color || classeVariable.equals(java.awt.Color.class)) {
                        TreeTableColumn<ObjetIHM, java.awt.Color> colonne = getDiIHMColorTableColumn(nomVariable,
                                titre);
                        tableau.getColumns().add(colonne);
                    } else if (variable instanceof Float || classeVariable.equals(Float.class)) {
                        TreeTableColumn<ObjetIHM, Float> colonne = getObjetCSAIHMFloatTableColumn(nomVariable, titre);
                        tableau.getColumns().add(colonne);
                    } else if (variable instanceof LocalDateTime || classeVariable.equals(LocalDateTime.class)) {
                        TreeTableColumn<ObjetIHM, LocalDateTime> colonne = getObjetCSAIHMLocalDateTimeTableColumn(
                                nomVariable, titre);
                        tableau.getColumns().add(colonne);
                    } else if (variable instanceof LocalDate || classeVariable.equals(LocalDate.class)) {
                        TreeTableColumn<ObjetIHM, LocalDate> colonne = getObjetCSAIHMLocalDateTableColumn(nomVariable,
                                titre);
                        tableau.getColumns().add(colonne);
                    } else if (variable instanceof Boolean || classeVariable.equals(Boolean.class)) {
                        TreeTableColumn<ObjetIHM, Boolean> colonne = getObjetCSAIHMBooleanTableColumn(nomVariable,
                                titre);
                        tableau.getColumns().add(colonne);
                    } else if (variable instanceof Color || classeVariable.equals(Color.class)) {
                        TreeTableColumn<ObjetIHM, Color> colonne = getObjetCSAIHMColorTableColumn(nomVariable, titre);
                        tableau.getColumns().add(colonne);
                    } else if (variable instanceof List) {
                        TreeTableColumn<ObjetIHM, ObservableList> colonne = getObjetCSAIHMObservableListTableColumn(
                                nomVariable, titre);
                        tableau.getColumns().add(colonne);
                    } else {
                        System.err.println("** : type non géré par le tableau pour la variable : " + variable);
                    }
                }
            }
        });
    }

    private TreeTableColumn<ObjetIHM, Boolean> getObjetCSAIHMBooleanTableColumn(String nomVariable, String titre) {
        TreeTableColumn<ObjetIHM, Boolean> colonne = new TreeTableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<Boolean>) oihm.getValue().getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Boolean)");
                return null;
            }
        });
        colonne.setCellFactory(p -> {
            CheckBoxTreeTableCell<ObjetIHM, Boolean> cell = new CheckBoxTreeTableCell<>();
            cell.setAlignment(Pos.CENTER);
            cell.setDisable(false);
            cell.setOnMousePressed(event -> {
                action_clic_checkBox(cell);
            });
            return cell;
        });
        return colonne;
    }

    private void action_clic_checkBox(Object source) {
        if (source instanceof CheckBoxTreeTableCell) {
            ObservableList<TreeTablePosition<ObjetIHM, ?>> cellules = tableau.getSelectionModel().getSelectedCells();
            String nomColonne = null;
            if (!cellules.isEmpty()) {
                TreeTablePosition celluleSelectionnee = cellules.get(0);
                nomColonne = celluleSelectionnee.getTableColumn().getText();
            }

            maitreFX.action_utilisateur(tableau.getSelectionModel().getSelectedItem().getValue().getObjetCSA(),
                    source,
                    nomColonne);
        }
    }

    private String affiche_couleur(Color couleur) {
        int opacite = (int) (couleur.getOpacity() * 255);
        int rouge = (int) (couleur.getRed() * 255);
        int vert = (int) (couleur.getGreen() * 255);
        int bleu = (int) (couleur.getBlue() * 255);

        return Integer.toHexString(opacite) + Integer.toHexString(rouge) + Integer.toHexString(vert)
                + Integer.toHexString(bleu) + " (" + (opacite + rouge + vert + bleu) + ")";
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

    @Override
    public void traitementRetour(ArrayList list) {
        if (list.isEmpty()) {
            System.err.println("La listeTable reçoit une liste vide de la BD !");
            return;
        }
        // on doit recevoir une AL => (int, nomTable, (liste d'objetDiIHM)) ou juste une
        // AL => (int)
        if (list.get(0) instanceof Integer) {
            int code = (int) list.get(0);
            affiche_console("Info : la listeTable "
                    + (elementDeReference == null ? "NULL" : elementDeReference.getType()) + " reçoit un code " + code);
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
                        System.err.println("*** paquet reçu incorrect dans ListTableBD : " + list
                                + "\n...mais j'attendais une liste de 3 éléments : (int, String, liste)");
                        break;
                    }
                    String nomTable = String.valueOf(list.get(1));
                    Object liste = list.get(2);
                    if (liste instanceof ArrayList) {
                        ArrayList<Object> listeRecue = (ArrayList) liste;
                        int taille = listeRecue.size();
                        affiche_console("* la listeTable reçoit (en direct) une liste de taille " + taille);
                        charger_liste(listeRecue, nomTable);
                        return;
                    }
                    System.err.println("*** Retour de envoi détail incorrect dans ListTableBD : " + list);
                    return;
                default:
                    System.err.println("*** La listeTable reçoit un retour de requête inattendu " + list
                            + "\n...je ne fais rien !");
                    return;
            }
            return;
        }
        System.err.println("La listeTable reçoit une requête non conforme : " + list);
    }

    /*
     ************ CLASSES INTERNES *****************
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
                affiche_console("Info : la listeTable "
                        + (elementDeReference == null ? "NULL" : elementDeReference.getClass().getSimpleName())
                        + " reçoit un code " + code);
                if (code == 4) {
                    if (p.size() < 3) {
                        System.err.println("*** paquet reçu incorrect dans ListTableBD (recepteur détail) : " + p
                                + "\n...mais j'attendais une liste de 3 éléments : (int, String, liste)");
                        return;
                    }
                    String nomTable = String.valueOf(p.get(1));
                    Object element = p.get(2);
                    if (element instanceof ArrayList) {
                        ArrayList<?> listeRecue = (ArrayList) element;
                        int taille = listeRecue.size();
                        if (taille == 1 && listeRecue.get(0) instanceof ObjetCSA) {
                            ObjetCSA elementBD = (ObjetCSA) listeRecue.get(0);
                            action_retour_demande_details(elementBD);
                            affiche_console("* : Détail de " + elementBD + " reçu sans liste");
                            return;
                        }
                        affiche_console("** la listeTable reçoit (dans le récepteur détails), " + "une liste de taille "
                                + taille + "\n... alors qu'un seul élément est attendu !" + "\n...=> " + listeRecue);
                        return;
                    }
                    System.err.println("*** Retour de envoi détail incorrect dans ListTableBD : " + p);
                    return;
                }
                System.err.println("*** La listeTable reçoit un retour de requête inattendu " + p
                        + "\nJe m'attends à soit une liste, soit un objetDiIHM !");
                return;
            }
            System.err.println("La listeTable reçoit une requête non conforme : " + p);
        }
    }

    /*
     **************************************
     */

}
