package controlsFX;

import java.io.IOException;
import java.net.URL;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import general.GestionnaireRetour;
import general.Monostable;
import general.ObjetCSA;
import generalFX.CustomEditor;
import generalFX.FenetreGlissante;
import generalFX.GenerateurColonnesTableView;
import generalFX.ObjetIHM;
import generalFX.TransformeurObjets;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Contrôleur pour une table de données BD avec pagination, filtrage et édition.
 * Affiche les données sous forme de tableau avec colonnes dynamiques.
 * <p>
 * Utiliser les handlers fonctionnels plutôt que {@code setMaitre(MaitreFX)} :
 * voir {@code setSaveHandler}, {@code setDeleteHandler},
 * {@code setRefreshDetailsHandler},
 * {@code setRefreshListHandler}, {@code setTypeResolver},
 * {@code setUserActionHandler},
 * {@code setMessageHandler}. Consulter MIGRATION_LISTTABLE_HANDLERS.md.
 */
public class ListTableBDController extends BorderPane
        implements Initializable, GestionnaireRetour {
    private final static int TAILLE_CARACTERE = 10;
    private static final int SEUIL_GRANDE_LISTE = 200;
    private static final int NOMBRE_CARACTERES = 2;
    private static final int SCROLL_THRESHOLD = 5; // lignes avant la fin
    private final HashMap<String, Object> informationsAdditionnelles = new HashMap<>();
    @FXML
    public TableView<ObjetIHM> tableau;
    @FXML
    public Label lblEntete, lblSousTitre;
    @FXML
    public Button btnNouveau, btnClearFiltre, btnRafraichir, btnModifier, btnSupprimer, btnDupliquer, btnSpecial,
            btnLister;
    @FXML
    public Separator separateur;
    @FXML
    public VBox vboxTop;
    @FXML
    public HBox hBoxBoutons;
    @FXML
    public TextField tfFiltre;
    private EditorPaneController editorPaneController = new EditorPaneController();
    private FenetreGlissante<ObjetIHM> fenetreGlissante = new FenetreGlissante<>();
    private ArrayList<ObjetIHM> listeOriginale = new ArrayList<>();
    private HashSet<ObjetCSA> selectionsMemoire = new HashSet<>(); // pour sélections multiples
    private ObjetCSA selection = null;
    private boolean activeSuppression = true;
    private boolean activeDuplication = true;
    private boolean activeModification = true;
    private boolean activeAjout = true;
    /**
     * Parent MaitreFX (système déprécié). Utilisé comme fallback quand les handlers
     * ne sont pas configurés.
     *
     * @deprecated Utiliser les handlers fonctionnels via les méthodes
     *             set*Handler().
     */
    @Deprecated
    private MaitreFX maitreFX;
    private ObjetCSA elementDeReference = null;
    private Monostable activeurIHM;
    private GenerateurColonnesTableView generateurColonnes;
    private Consumer<ObjetCSA> postCreateCallback;
    /**
     * Mode test, utilisé en remplacement de maitreFX.isTest().
     */
    private boolean testMode = false;
    /**
     * Chemin de l'éditeur parent (quand ce composant est embarqué dans un
     * EditorPane).
     */
    private String parentEditorPath = "";
    /**
     * Objet à sélectionner automatiquement au prochain rechargement de la liste
     * (utilisé après création d'un nouvel élément).
     */
    private ObjetCSA pendingSelection = null;

    /**
     * Objet en cours de création, en attente du retour BD (code=1, maitreFX path).
     * Permet d'ajouter directement l'élément à la liste sans rechargement.
     */
    private ObjetCSA pendingAjout = null;

    // Handler BD unifié (remplace les 4 handlers individuels)
    private ContexteFX.HandlerBD bdHandler;

    /**
     * Factory pour créer le CustomEditor associé à chaque EditorPane ouvert.
     */
    private java.util.function.Function<EditorPaneController, CustomEditor> customEditorFactory = null;

    private Function<String, ObjetCSA> typeResolver;
    private BiConsumer<Object, String> userActionHandler;
    private Consumer<String> messageHandler;
    /**
     * Contexte partagé (propagé automatiquement aux composants enfants).
     */
    private ContexteFX contexte;
    private BiConsumer<ObjetCSA, String> parentNotifier;

    /**
     * Construit un ListTableBDController et charge le fichier FXML associé.
     */
    public ListTableBDController() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ListTableBD.fxml"));

        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        activeurIHM = new Monostable(true, false);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Active ou désactive l'affichage du panneau supérieur.
     *
     * @param activer true pour afficher, false pour masquer
     */
    public void activer_topPanel(boolean activer) {
        Platform.runLater(() -> {
            if (activer) {
                setTop(vboxTop);
            } else {
                setTop(null);
            }
        });
    }

    public BiConsumer<ObjetCSA, String> getParentNotifier() {
        return parentNotifier;
    }

    public void setParentNotifier(BiConsumer<ObjetCSA, String> parentNotifier) {
        this.parentNotifier = parentNotifier;
    }

    private ScrollBar getVerticalScrollBar(TableView<?> table) {
        for (Node node : table.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar) {
                ScrollBar bar = (ScrollBar) node;
                if (bar.getOrientation() == Orientation.VERTICAL) {
                    return bar;
                }
            }
        }
        return null;
    }

    /**
     * Active ou désactive l'affichage du panneau de boutons.
     *
     * @param activer true pour afficher, false pour masquer
     */
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

    /**
     * Active ou désactive l'affichage du bouton spécial.
     *
     * @param on true pour afficher, false pour masquer
     */
    public void activer_bouton_special(boolean on) {
        separateur.setVisible(on);
        btnSpecial.setVisible(on);
    }

    /**
     * Retourne le bouton spécial pour personnalisation.
     *
     * @return le bouton spécial
     */
    public Button getBoutonSpecial() {
        return btnSpecial;
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

    /**
     * Ajoute une valeur dans l'élément de référence.
     *
     * @param nom    le nom de la valeur
     * @param donnee la valeur à insérer
     */
    public void add_valeur_dans_reference(String nom, Object donnee) {
        if (elementDeReference != null) {
            elementDeReference.inserer_valeur(nom, donnee);
        }
    }

    /**
     * Ajoute une clé étrangère dans l'élément de référence.
     *
     * @param nom    le nom de la FK
     * @param donnee l'objet CSA à insérer
     */
    public void add_fk_dans_reference(String nom, ObjetCSA donnee) {
        if (elementDeReference != null) {
            elementDeReference.inserer_fk(nom, donnee);
        }
    }

    /**
     * Ajoute une valeur intermédiaire dans l'élément de référence.
     *
     * @param nomTable    le nom de la table
     * @param nomVariable le nom de la variable
     * @param donnee      l'objet CSA à insérer
     */
    public void add_fki_dans_reference(String nomTable, String nomVariable, ObjetCSA donnee) {
        if (elementDeReference != null) {
            elementDeReference.inserer_valeur_intermediaire(nomTable, nomVariable, donnee);
        }
    }

    /**
     * Retourne le maître parent (système déprécié).
     *
     * @deprecated Utiliser les handlers fonctionnels.
     */
    @Deprecated
    public MaitreFX getMaitre() {
        return maitreFX;
    }

    /**
     * Définit le maître parent. Déprécié — utiliser les handlers fonctionnels.
     *
     * @deprecated Utiliser {@code setSaveHandler}, {@code setDeleteHandler}, etc. à
     *             la place.
     *             Voir MIGRATION_LISTTABLE_HANDLERS.md.
     */
    @Deprecated
    public void setMaitre(MaitreFX coeur) {
        maitreFX = coeur;
    }

    /**
     * Définit le mode test (remplace {@code maitreFX.isTest()}).
     */
    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    /**
     * Retourne true si ce composant est en mode test.
     */
    public boolean isTest() {
        return testMode || (maitreFX != null && maitreFX.isTest());
    }

    /**
     * Définit le chemin de l'éditeur parent quand ce composant est embarqué dans un
     * EditorPane.
     * Permet à l'éditeur enfant d'hériter du chemin hiérarchique.
     */
    public void setParentEditorPath(String path) {
        this.parentEditorPath = path != null ? path : "";
    }

    /**
     * Retourne le contrôleur du panneau d'édition.
     *
     * @return l'EditorPaneController associé
     */
    public EditorPaneController getEditorPaneController() {
        return editorPaneController;
    }

    /**
     * Rafraîchit les listes de l'éditeur.
     */
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

    /**
     * Efface toutes les informations additionnelles.
     */
    public void clear_info_additionnelles() {
        informationsAdditionnelles.clear();
    }

    /**
     * Retourne une copie des informations additionnelles.
     *
     * @return une copie de la map des informations additionnelles
     */
    public HashMap<String, Object> getCopyOfInfoAdditionnelles() {
        return new HashMap<>(informationsAdditionnelles);
    }

    /**
     * Définit l'éditeur personnalisé via une factory.
     * La factory reçoit le pane parent et retourne un nouveau CustomEditor.
     * Exemple : {@code listTable.setEditeurPerso(pane -> new MonEditeur(pane))}
     *
     * @param factory la factory, ou null pour utiliser l'éditeur générique
     */
    public void setEditeurPerso(java.util.function.Function<EditorPaneController, CustomEditor> factory) {
        this.customEditorFactory = factory;
        if (factory != null) {
            editorPaneController.setCustomEditor(factory);
        }
    }

    /**
     * Retourne l'élément de référence utilisé pour créer de nouveaux éléments.
     *
     * @return l'ObjetCSA de référence
     */
    public ObjetCSA getElementDeReference() {
        return elementDeReference;
    }

    /**
     * Charge une liste de données dans le tableau.
     *
     * @param donnees  la liste d'objets à charger
     * @param nomTable le nom de la table (type) des données
     */
    public void charger_liste(ArrayList<Object> donnees, String nomTable) {
        ObjetCSA ref = null;
        if (typeResolver != null) {
            ref = typeResolver.apply(nomTable);
        } else if (maitreFX != null) {
            ref = maitreFX.getElementFromType(nomTable);
        }
        if (ref == null) {
            System.err.println("ListTableBD/charger_liste : impossible de résoudre le type '" + nomTable +
                    "'. Configurer setTypeResolver() ou setMaitre().");
            return;
        }
        charger_liste(donnees, ref);
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
        long startTime = System.currentTimeMillis();

        elementDeReference = ref.getCopy(); // éviter les modifications directes sur le référentiel
        if (postCreateCallback != null)
            postCreateCallback.accept(elementDeReference);

        ArrayList<ObjetIHM> objetIHMS;
        ObjetIHM elementReferenceIHM;
        String nomEntete;

        if (elementDeReference == null) {
            elementReferenceIHM = null;
            objetIHMS = new ArrayList<>();
            nomEntete = "-";
        } else {
            nomEntete = elementDeReference.getPublicName();
            objetIHMS = TransformeurObjets.transformer_en_objetsIHM(donnees, elementDeReference);
            elementReferenceIHM = new ObjetIHM(elementDeReference);
        }

        generateurColonnes.setColumn(elementReferenceIHM);

        listeOriginale = new ArrayList<>(objetIHMS);
        fenetreGlissante.chargerTout(objetIHMS);

        long loadTime = System.currentTimeMillis() - startTime;

        Platform.runLater(() -> {
            lblEntete.setText(nomEntete);

            // Info utilisateur si pagination
            if (fenetreGlissante.needsPagination()) {
                lblSousTitre.setText(String.format("Affichage %d-%d sur %d éléments",
                        fenetreGlissante.getIndexDebut() + 1,
                        fenetreGlissante.getIndexFin(),
                        fenetreGlissante.getTailleTotale()));

                if (donnees.size() > 500) {
                    lblSousTitre.setStyle("-fx-text-fill: orange;");
                }
            }

            // Charger fenêtre initiale
            ObservableList<ObjetIHM> listeTriee = FXCollections.observableArrayList(
                    fenetreGlissante.getFenetreActuelle());
            FXCollections.sort(listeTriee);
            tableau.getItems().setAll(listeTriee);

            // Sélectionner automatiquement l'élément créé/modifié si en attente
            if (pendingSelection != null) {
                ObjetCSA toSelect = pendingSelection;
                pendingSelection = null;
                tableau.getSelectionModel().select(new ObjetIHM(toSelect));
            }

            // Debug performance
            if (loadTime > 500) {
                affiche_console(String.format("⚠️ Chargement lent: %dms pour %d éléments",
                        loadTime, donnees.size()));
            }
        });

        activer_selection(false);
        selection = null;

        if (userActionHandler != null) {
            userActionHandler.accept(null, "liste reçue");
        } else if (maitreFX != null) {
            maitreFX.action_utilisateur(null, this, "liste reçue");
        }
    }

    private void charger_batch_suivant() {
        sauvegarder_selections();

        if (fenetreGlissante.chargerSuivants()) {
            Platform.runLater(() -> {
                ObservableList<ObjetIHM> listeTriee = FXCollections.observableArrayList(
                        fenetreGlissante.getFenetreActuelle());
                FXCollections.sort(listeTriee);
                tableau.getItems().setAll(listeTriee);

                maj_compteur();
                restaurer_selections();

                // Debug
                affiche_console("Batch suivant: " + fenetreGlissante.getDebugInfo());
            });
        }
    }

    private void charger_batch_precedent() {
        sauvegarder_selections();

        if (fenetreGlissante.chargerPrecedents()) {
            Platform.runLater(() -> {
                ObservableList<ObjetIHM> listeTriee = FXCollections.observableArrayList(
                        fenetreGlissante.getFenetreActuelle());
                FXCollections.sort(listeTriee);
                tableau.getItems().setAll(listeTriee);

                maj_compteur();
                restaurer_selections();

                // Debug
                affiche_console("Batch précédent: " + fenetreGlissante.getDebugInfo());
            });
        }
    }

    private void maj_compteur() {
        if (fenetreGlissante.needsPagination()) {
            lblSousTitre.setText(String.format("Affichage %d-%d sur %d éléments",
                    fenetreGlissante.getIndexDebut() + 1,
                    fenetreGlissante.getIndexFin(),
                    fenetreGlissante.getTailleTotale()));
        }
    }

    private void sauvegarder_selections() {
        selectionsMemoire.clear();
        getSelections().forEach(elt -> selectionsMemoire.add(elt));
    }

    private void restaurer_selections() {
        if (!selectionsMemoire.isEmpty()) {
            tableau.getSelectionModel().clearSelection();
            for (ObjetIHM item : tableau.getItems()) {
                if (selectionsMemoire.contains(item.getObjetCSA())) {
                    tableau.getSelectionModel().select(item);
                }
            }
        }
    }

    public void setPostCreateCallback(Consumer<ObjetCSA> callback) {
        this.postCreateCallback = callback;
    }

    /**
     * Configure le résolveur de type (remplace getElementFromType).
     */
    public void setTypeResolver(Function<String, ObjetCSA> resolver) {
        this.typeResolver = resolver;
    }

    /**
     * Configure le handler d'actions utilisateur.
     */
    public void setUserActionHandler(BiConsumer<Object, String> handler) {
        this.userActionHandler = handler;
    }

    /**
     * Configure le handler de messages utilisateur.
     */
    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    public ContexteFX getContexteFX() {
        return contexte;
    }

    /**
     * Configure le handler BD unifié (remplace setSaveHandler, setDeleteHandler,
     * etc.).
     * Codes : 1=ajout, 2=modification, 3=suppression, 4=consultation.
     */
    public void setBdHandler(ContexteFX.HandlerBD handler) {
        this.bdHandler = handler;
    }

    public boolean isEditorOpened() {
        try {
            return editorPaneController.getEditeurPersonnel().getScene().getWindow().isShowing();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Configure tous les handlers en une seule fois depuis un {@link ContexteFX}.
     * Remplace les appels individuels à {@code setSaveHandler},
     * {@code setDeleteHandler}, etc.
     * Le contexte est mémorisé et propagé automatiquement aux composants enfants
     * (EditorPane, ComboEditable) lors de leur ouverture.
     *
     * @param ctx le contexte à appliquer (les champs null dans ctx ne remplacent
     *            pas les handlers existants)
     */
    public void setContexte(ContexteFX ctx) {
        this.contexte = ctx;
        if (ctx == null)
            return;
        if (ctx.getBdHandler() != null)
            this.bdHandler = ctx.getBdHandler();
        if (ctx.getTypeResolver() != null)
            this.typeResolver = ctx.getTypeResolver();
        if (ctx.getMessageHandler() != null)
            this.messageHandler = ctx.getMessageHandler();
        if (ctx.getUserActionHandler() != null)
            this.userActionHandler = ctx.getUserActionHandler();
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
                if (userActionHandler != null) {
                    userActionHandler.accept(selection, message);
                } else if (maitreFX != null) {
                    maitreFX.action_utilisateur(selection, this, message);
                }
                return;
            }
            lblEntete.setText("");
        });
    }

    public void action_clic_liste(ObjetCSA selection, String nomColonne) {
        Platform.runLater(() -> {
            ObjetIHM objetDiIHM = tableau.getSelectionModel().getSelectedItem();

            if (selection != null) {
                activeSuppression = activeSuppression && objetDiIHM.getObjetCSA().estSupprimable();
                activer_selection(true);
                lblSousTitre.setText(String.valueOf(selection));
                lblEntete.setText(selection.getPublicName());
                boolean modificationAutorisee = !btnModifier.isDisabled() && btnModifier.isVisible();
                if (activeurIHM.isStable() && modificationAutorisee) {
                    action_modifier(null);
                }
                if (userActionHandler != null) {
                    userActionHandler.accept(selection, "Clic sur " + nomColonne);
                } else if (maitreFX != null) {
                    maitreFX.action_utilisateur(selection, this, "Clic sur " + nomColonne);
                }
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
        System.err.println("Didier : ListTableBDController/action_nouveau : elementDeReference vaut null ! " +
                "=> ne peut pas être transformé en elementBD !");
        affiche_utilisateur("Impossible de déterminer le type d'objet à créer");
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
        if (bdHandler == null && maitreFX == null) {
            System.err.println(
                    "*** La listeTable ne trouve pas son conteneur (ni refreshDetailsHandler ni maitreFX configuré). " +
                            "Ceci est un problème !");
            return;
        }

        if (selection.getId() > 0) {
            if (bdHandler != null) {
                affiche_utilisateur("Recherche des détails de " + selection);
                bdHandler.execute(4, selection, list -> Platform.runLater(() -> {
                    if (list != null && list.size() > 2) {
                        if (list.get(2) instanceof ObjetCSA detailedObj) {
                            detailedObj.clearModifications();
                            charger_donnees_vers_enfant(detailedObj);
                            return;
                        }
                        if (list.get(2) instanceof ArrayList<?> monoliste && !monoliste.isEmpty()) {
                            if (monoliste.get(0) instanceof ObjetCSA detailedObj) {
                                detailedObj.clearModifications();
                                charger_donnees_vers_enfant(detailedObj);
                                return;
                            }
                            affiche_console(
                                    "ListTableBD : le code 4 (détails) de bdHandler a retourné une liste d'objets inattendue : "
                                            + monoliste.get(0));
                            return;
                        }
                        affiche_console(
                                "ListTableBD : le code 4 (détails) de bdHandler a retourné un résultat inattendu : "
                                        + list.get(2));
                        return;
                    }
                    affiche_utilisateur("Impossible de récupérer les détails de " + selection);
                }));
                return;
            }

            if (maitreFX != null) {
                affiche_utilisateur("Recherche des détails de " + selection);
                maitreFX.appeler_bd(4, selection, new RecepteurDetails());
                return;
            }
            System.err.println(
                    "*** Ordre de modification reçue mais la listeBD ne " + "connait ni contrôleur, ni conteneurFX");
            return;
        }
        charger_donnees_vers_enfant(selection);
    }

    public void ajouter(ArrayList<ObjetCSA> liste) {
        if (bdHandler != null) {
            if (!liste.isEmpty()) {
                bdHandler.execute(1, liste.get(0), resultList -> Platform.runLater(() -> {
                    if (resultList != null && !resultList.isEmpty()
                            && resultList.get(0) instanceof Integer kode && kode > 0) {
                        affiche_utilisateur("Enregistrement réussi !");
                        action_rafraichir(null);
                    } else {
                        affiche_utilisateur("Échec de l'enregistrement");
                    }
                }));
            }
            return;
        }

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
        Object objetAEnvoyer;
        if (selections.size() == 1) {
            objetAEnvoyer = selections.get(0);
        } else {
            objetAEnvoyer = selections;
        }
        if (bdHandler != null && objetAEnvoyer instanceof ObjetCSA) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Suppression de : " +
                    extraire_smart_text(objetAEnvoyer) + "\nVeuillez confirmer, svp");
            alert.setWidth(780);
            alert.setHeight(520);
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    bdHandler.execute(3, (ObjetCSA) objetAEnvoyer, resultList -> Platform.runLater(() -> {
                        if (resultList != null && !resultList.isEmpty()
                                && resultList.get(0) instanceof Integer kode && kode > 0) {
                            affiche_utilisateur("Suppression réussie !");
                            action_rafraichir(null);
                        } else {
                            affiche_utilisateur("Échec de la suppression");
                        }
                    }));
                } else {
                    affiche_utilisateur("Suppression annulée par l'utilisateur");
                }
            });
            return;
        }

        if (maitreFX != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Suppression de : " + extraire_smart_text(objetAEnvoyer) + "\nVeuillez confirmer, svp");
            alert.setWidth(780);
            alert.setHeight(520);
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    maitreFX.appeler_bd(3, objetAEnvoyer, this);
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
        if (messageHandler != null) {
            messageHandler.accept(s);
            return;
        }
        if (maitreFX != null) {
            maitreFX.affiche_console(s);
            return;
        }
        System.err.println("ListTableBD: " + s);
    }

    /**
     * Envoie une requête au parent via MaitreFX (fallback déprécié).
     * Utilisé pour la compatibilité ascendante. Préférer les handlers fonctionnels.
     */
    private void envoyer_vers_parent_compat(int code, Object objet, GestionnaireRetour appelant) {
        if (appelant != null && maitreFX != null) {
            maitreFX.appeler_bd(code, objet, appelant);
            return;
        }
        if (objet instanceof ObjetCSA) {
            ObjetCSA elementBD = (ObjetCSA) objet;
            if (elementBD.getId() > 0 && code < 2) {
                System.err.println("*** Didier : ListTableBD ne peut créer un nouvel élément avec id !");
                return;
            }
            if (maitreFX != null) {
                maitreFX.appeler_bd(code, elementBD, this);
                return;
            }
            affiche_console("Didier : Le listeBDTable n'a pas son MaitreFX ! L'envoi vers le parent est ignoré.");
            return;
        }
        System.out.println("*** ListTableBD/envoyer_vers_parent_compat ne peut envoyer l'objet : " + objet);
    }

    public void affiche_console(String s) {
        if (messageHandler != null) {
            messageHandler.accept(s);
            return;
        }
        if (maitreFX != null) {
            maitreFX.affiche_console(s);
            return;
        }
        System.err.println("ListTableBD: " + s);
    }

    public ObjetCSA getElementFromType(String typeAffiche) {
        if (typeResolver != null) {
            return typeResolver.apply(typeAffiche);
        }
        if (maitreFX != null) {
            return maitreFX.getElementFromType(typeAffiche);
        }
        return null;
    }

    // Ancienne méthode MaitreFX.action_utilisateur — supprimée car ce composant
    // n'implémente plus MaitreFX.
    // Utilisée en interne pour propager vers userActionHandler ou maitreFX parent.
    private void propager_action_utilisateur(Object selectedItem, String extraInfo) {
        if (userActionHandler != null) {
            userActionHandler.accept(selectedItem, extraInfo);
        } else if (maitreFX != null) {
            maitreFX.action_utilisateur(selectedItem, this, extraInfo);
        }
    }

    // Ancienne méthode MaitreFX.affiche_details — non supportée.
    // private void affiche_details(Object selectedItem) { throw new
    // UnsupportedOperationException(); }

    /**
     * Nettoie les champs en demandant la création d'un nouvel élément.
     */
    public void nettoyer_champs() {
        action_nouveau(null);
    }

    /**
     * Ouvre l'éditeur pour créer ou modifier un objet.
     * Si l'éditeur est déjà ouvert, on le réutilise en lui passant les nouvelles
     * données.
     * Utilise les callbacks configurés (saveHandler, typeResolver,
     * messageHandler…).
     */
    public void charger_donnees_vers_enfant(ObjetCSA objetDi) {
        if (isEditorShowing()) {
            // L'éditeur existe déjà : on lui passe simplement le nouvel objet
            editorPaneController.charger_donnees(parentEditorPath, objetDi.getCopy());
            return;
        }

        editorPaneController = EditorPaneController.openEditor(objetDi.getCopy(),
                contexte,
                customEditorFactory,
                parentEditorPath);

        // parentNotifier : ajout direct sans rechargement (code=1) ou rafraîchissement
        // (code=2)
        editorPaneController.setParentNotifier((code, obj) -> {
            // pendingSelection = obj;
            if (code == 1) {
                ajouter_element(obj);
            } else if (code == 2) {
                modifier_element(obj);
            }
        });
    }

    public void modifier_element(ObjetCSA obj) {
        if (obj == null)
            return;
        ObjetIHM objetIHM = new ObjetIHM(obj);
        // Remplacer dans listeOriginale
        for (int i = 0; i < listeOriginale.size(); i++) {
            if (listeOriginale.get(i).getObjetCSA().getId() == obj.getId()) {
                listeOriginale.set(i, objetIHM);
                break;
            }
        }
        Platform.runLater(() -> {
            // Remplacer dans le tableau visible
            for (int i = 0; i < tableau.getItems().size(); i++) {
                if (tableau.getItems().get(i).getObjetCSA().getId() == obj.getId()) {
                    tableau.getItems().set(i, objetIHM);
                    break;
                }
            }
            tableau.getSelectionModel().select(objetIHM);
            tableau.scrollTo(objetIHM);
            affiche_utilisateur("Modification enregistrée !");
        });
    }

    /**
     * Indique si la fenêtre d'édition enfant est actuellement ouverte.
     */
    public boolean isEditorShowing() {
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

    public void rafraichir() {
        action_rafraichir(null);
    }

    @FXML
    public void action_rafraichir(ActionEvent actionEvent) {
        tableau.getItems().clear();

        // Handler BD unifié (priorité)
        if (bdHandler != null) {
            bdHandler.execute(4, elementDeReference, list -> {
                if (list != null && list.size() >= 3 && list.get(2) instanceof ArrayList<?> listeRecue) {
                    String nomTable = String.valueOf(list.get(1));
                    charger_liste((ArrayList<Object>) listeRecue, nomTable);
                    return;
                }
                affiche_utilisateur("Impossible de rafraîchir la liste");
                affiche_console(
                        "ListTableBD : le code 4 (consultation) de bdHandler a retourné un résultat inattendu : "
                                + list);
            });
            return;
        }

        // Fallback ancien système
        if (maitreFX != null) {
            maitreFX.rafraichir();
            return;
        }

        System.err.println("ListeTableBD : Impossible de rafraîchir (aucun handler configuré)");
    }

    public void select(ObjetCSA o) {
        if (o == null) {
            clearSelection();
        } else {
            tableau.getSelectionModel().select(new ObjetIHM(o));
        }
    }

    /**
     * Ajoute directement un élément à la liste sans rechargement depuis la BD.
     * L'élément doit posséder un ID valide (setId() déjà appelé par
     * EditorPaneController).
     *
     * @param obj l'objet à ajouter (avec son ID final)
     */
    public void ajouter_element(ObjetCSA obj) {
        if (obj == null || elementDeReference == null)
            return;
        ObjetIHM objetIHM = new ObjetIHM(obj);
        listeOriginale.add(objetIHM);
        fenetreGlissante.ajouterElement(objetIHM);
        Platform.runLater(() -> {
            // Ajouter au tableau seulement si l'élément est dans la fenêtre visible
            if (!fenetreGlissante.needsPagination() || fenetreGlissante.isAtEnd()) {
                tableau.getItems().add(objetIHM);
            }
            tableau.getSelectionModel().clearSelection();
            tableau.getSelectionModel().select(objetIHM);
            tableau.scrollTo(objetIHM);
            affiche_utilisateur("Enregistrement réussi !");
        });
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

            // Si grande liste, exiger minimum de caractères
            if (listeOriginale.size() > SEUIL_GRANDE_LISTE &&
                    texteFiltre.length() > 0 && texteFiltre.length() < NOMBRE_CARACTERES) {
                return; // Ne pas filtrer
            }

            if (listeOriginale.isEmpty() && !texteFiltre.isEmpty()) {
                ObjetCSA nouvelElt = elementDeReference.getCopy();
                nouvelElt.inserer_valeur("filtre", texteFiltre);
                if (bdHandler != null) {
                    bdHandler.execute(4, nouvelElt, resultList -> {
                        if (resultList.size() < 3) {
                            System.err.println("*** paquet reçu incorrect dans ListTableBD : " + resultList
                                    + "\n...mais j'attendais une liste de 3 éléments : (int, String, liste)");
                            return;
                        }
                        String nomTable = String.valueOf(resultList.get(1));
                        Object liste = resultList.get(2);
                        if (liste instanceof ArrayList) {
                            ArrayList<Object> listeRecue = (ArrayList) liste;
                            int taille = listeRecue.size();
                            affiche_console("* la listeTable reçoit (en direct) une liste de taille " + taille);
                            charger_liste(listeRecue, getElementFromType(nomTable));
                            return;
                        }
                        System.err.println("*** Retour de envoi détail incorrect dans ListTableBD : " + resultList);
                    });
                } else if (maitreFX != null) {
                    maitreFX.appeler_bd(4, nouvelElt, this);
                }
            } else {
                filtrer(texteFiltre);
            }
        });
    }

    public void filtrer(String texteFiltre) {
        Platform.runLater(() -> {
            String texte = Normalizer.normalize(texteFiltre.trim().toUpperCase(),
                    Normalizer.Form.NFD).replaceAll("[\u0300-\u036F]", "");

            if (texte.isEmpty()) {
                fenetreGlissante.chargerTout(listeOriginale);
            } else {
                ArrayList<ObjetIHM> inclus = new ArrayList<>();
                listeOriginale.forEach((elt) -> {
                    String nom = Normalizer.normalize(
                            elt.getObjetCSA().getNomAFiltrer().trim().toUpperCase(),
                            Normalizer.Form.NFD).replaceAll("[\u0300-\u036F]", "");

                    if (nom.startsWith(texte)) {
                        inclus.add(elt);
                    }
                });
                fenetreGlissante.chargerTout(inclus);
            }

            ObservableList<ObjetIHM> listeTriee = FXCollections.observableArrayList(
                    fenetreGlissante.getFenetreActuelle());
            FXCollections.sort(listeTriee);
            tableau.getItems().setAll(listeTriee);

            maj_compteur();

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
            if (parentNotifier != null) {
                parentNotifier.accept(selection.getObjetCSA(), "chercher_sousListe");
                return;
            }
            affiche_console("Attention : pas de notification possible vers le parent de ce listeTable.\n" +
                    "Il faut absolument faire un setParentNotifier et lui dire ce qu'on y fait !\n" +
                    "Je regarde si l'ancien système avec maitreFX peut passer...");
            if (maitreFX != null) {
                maitreFX.action_utilisateur(selection.getObjetCSA(), this, "chercher_sousListe");
            }
            affiche_console(
                    "Si vous voyez ce message, c'est que le maitreFX n'est pas configuré pour recevoir les actions utilisateur de ce composant.\n"
                            +
                            "Il faut absolument faire un setUserActionHandler ou setContexte pour que ce composant puisse communiquer avec son environnement !");
            return;
        }
        affiche_utilisateur("Aucune sélection détectée !");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configure la politique de redimensionnement pour éviter le scroll horizontal
        tableau.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // Force le TableView à prendre tout l'espace disponible
        tableau.setPrefHeight(Control.USE_COMPUTED_SIZE);
        tableau.setMinHeight(Region.USE_PREF_SIZE);

        generateurColonnes = new GenerateurColonnesTableView(tableau,
                string -> {
                    if (messageHandler != null) {
                        messageHandler.accept(string);
                    }
                },
                (selection, nomColonne) -> {
                    action_clic_liste(selection, nomColonne);
                });
        btnNouveau.setTooltip(new Tooltip("Ajouter un nouvel élément"));
        btnClearFiltre.setTooltip(new Tooltip("Effacer le filtre"));
        btnRafraichir.setTooltip(new Tooltip("Rafraichir la liste"));
        btnModifier.setTooltip(new Tooltip("Éditer la sélection"));
        btnDupliquer.setTooltip(new Tooltip("Créer un nouveau à partir de la sélection"));
        btnLister.setTooltip(new Tooltip("Ouvrir une sous-liste liée à l'élément sélectionné"));
        btnSupprimer.setTooltip(new Tooltip("Supprimer l'élément sélectionné"));
        tableau.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableau.setPlaceholder(new Label("Aucun élément dans la liste"));
        // tableau.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Détection du scroll pour pagination
        tableau.setOnScroll(event -> {
            if (fenetreGlissante.needsPagination()) {
                int derniereVisible = tableau.getSelectionModel().getSelectedIndex();
                int totalVisible = tableau.getItems().size();

                if (event.getDeltaY() < 0 && derniereVisible > totalVisible - SCROLL_THRESHOLD) {
                    // Scroll vers le bas
                    charger_batch_suivant();
                } else if (event.getDeltaY() > 0 && derniereVisible < SCROLL_THRESHOLD) {
                    // Scroll vers le haut
                    charger_batch_precedent();
                }
            }
        });
        // Détection du scroll via ScrollBar
        tableau.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                ScrollBar scrollBar = getVerticalScrollBar(tableau);
                if (scrollBar != null) {
                    scrollBar.valueProperty().addListener((o, oldVal, newVal) -> {
                        if (fenetreGlissante.needsPagination()) {
                            double position = newVal.doubleValue();

                            // Scroll vers le bas
                            if (position > FenetreGlissante.SCROLL_POSITION_BASSE &&
                                    !fenetreGlissante.isAtEnd()) {
                                charger_batch_suivant();
                            }
                            // Scroll vers le haut
                            else if (position < FenetreGlissante.SCROLL_POSITION_HAUTE &&
                                    !fenetreGlissante.isAtStart()) {
                                charger_batch_precedent();
                            }
                        }
                    });
                }
            }
        });

        // Alternative: détection via index de sélection
        tableau.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            if (fenetreGlissante.needsPagination() && newVal != null) {
                int selectedIndex = newVal.intValue();
                int size = tableau.getItems().size();

                // Proche de la fin
                if (selectedIndex > size - FenetreGlissante.SCROLL_THRESHOLD &&
                        !fenetreGlissante.isAtEnd()) {
                    charger_batch_suivant();
                }
                // Proche du début
                else if (selectedIndex < FenetreGlissante.SCROLL_THRESHOLD &&
                        !fenetreGlissante.isAtStart()) {
                    charger_batch_precedent();
                }
            }
        });
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
        // on doit recevoir une AL => (int, nomTable, (liste d'objetDiIHM)) ou juste une
        // AL => (int)
        if (list.get(0) instanceof Integer) {
            int code = (int) list.get(0);
            affiche_console("Info : la listeTable "
                    + (elementDeReference == null ? "NULL" : elementDeReference.getType()) + " reçoit un code " + code);
            switch (code) {
                case 1:
                    if (list.size() >= 3 && list.get(2) instanceof Integer nouvelID && pendingAjout != null) {
                        // Protocole optimisé : BD retourne (1, nomTable, nouvelID)
                        // → on insère directement l'objet dans la liste, sans rechargement
                        ObjetCSA obj = pendingAjout;
                        pendingAjout = null;
                        obj.setId(nouvelID);
                        Platform.runLater(() -> ajouter_element(obj));
                    } else {
                        // Fallback : pas d'objet en attente ou format inattendu → rechargement complet
                        pendingAjout = null;
                        affiche_utilisateur("Enregistrement réussi !");
                        action_rafraichir(null);
                    }
                    return;
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
                        charger_liste(listeRecue, getElementFromType(nomTable));
                        return;
                    }
                    System.err.println("*** Retour de envoi détail incorrect dans ListTableBD : " + list);
                    return;
                default:
                    affiche_utilisateur("Échec de l'opération (code " + code + ")");
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
        public void traitementRetour(ArrayList list) {
            if (list.isEmpty()) {
                System.err.println("La listeTable reçoit une liste vide de la BD !");
                return;
            }
            if (list.get(0) instanceof Integer) {
                int code = (int) list.get(0);
                affiche_console("Info : la listeTable "
                        + (elementDeReference == null ? "NULL" : elementDeReference.getClass().getSimpleName())
                        + " reçoit un code " + code);
                if (code == 4) {
                    if (list.size() < 3) {
                        System.err.println("*** paquet reçu incorrect dans ListTableBD (recepteur détail) : " + list
                                + "\n...mais j'attendais une liste de 3 éléments : (int, String, liste)");
                        return;
                    }
                    String nomTable = String.valueOf(list.get(1));
                    Object element = list.get(2);
                    if (element instanceof ArrayList<?> listeRecue) {
                        int taille = listeRecue.size();
                        if (taille == 1 && listeRecue.get(0) instanceof ObjetCSA elementBD) {
                            elementBD.clearModifications();
                            charger_donnees_vers_enfant(elementBD);
                            affiche_console("* : Détail de " + elementBD + " reçu dans une liste" +
                                    " de taille " + listeRecue.size());
                            return;
                        }
                        affiche_console("** la listeTable reçoit (dans le récepteur détails), " + "une liste de taille "
                                + taille + "\n... alors qu'un seul élément est attendu !" + "\n...=> " + listeRecue);
                        return;
                    }
                    if (element instanceof ObjetCSA objetCSA) {
                        objetCSA.clearModifications();
                        charger_donnees_vers_enfant(objetCSA);
                        affiche_console("* : Détail de " + objetCSA + " reçu sans liste, mais ok, c'est géré !");
                        return;
                    }
                    System.err.println("*** Retour de envoi détail incorrect dans ListTableBD : " + list);
                    return;
                }
                System.err.println("*** La listeTable reçoit un retour de requête inattendu " + list
                        + "\nJe m'attends à soit une liste, soit un objetDiIHM !");
                return;
            }
            System.err.println("La listeTable reçoit une requête non conforme : " + list);
        }
    }
}
