package controlsFX;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import general.GestionnaireImbrications;
import general.GestionnaireRetour;
import general.Monostable;
import general.ObjetCSA;
import generalFX.CustomEditor;
import generalFX.EcriveurChamps;
import generalFX.FX_RGBConverter;
import generalFX.LecteurChamps;
import generalFX.StylesheetsFX;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class EditorPaneController extends BorderPane
        implements Initializable, MaitreFX {
    /**
     * Contexte partagé (propagé automatiquement aux ComboEditable enfants).
     */
    private static ContexteFX contexte;
    /**
     * contient les paires (nomBD,champ)
     */
    protected final LinkedHashMap<String, Node> pairesNomChamp = new LinkedHashMap<>();
    private final Monostable activeurIHM;
    protected ObjetCSA monObjet = null;
    protected CustomEditor editeurPersonnel = null;
    @FXML
    protected Label lblEntete, lblCommentaire;
    @FXML
    protected BorderPane centerPane;
    protected MaitreFX mom;
    @FXML
    private Button btnEnregistrer;
    @FXML
    private Button btnEffacer, btnRafraichir;
    @FXML
    private Button btnAnnuler;
    // Chemin hiérarchique de cet éditeur
    private String editorPath = "", prefixePath = "";
    private Runnable onCloseHandler;
    private Consumer<ObjetCSA> postCreateCallback;
    /*
     * private ContexteFX.HandlerBD bdHandler;
     * // Nouveaux callbacks pour remplacer MaitreFX (approche moderne)
     * private Consumer<String> messageHandler; // Pour afficher des messages à
     * l'utilisateur parent
     * private java.util.function.Function<String, ObjetCSA> typeResolver; // Pour
     * résoudre les types
     * private Runnable refreshHandler; // Pour demander un rafraîchissement
     */
    private java.util.function.BiConsumer<Integer, ObjetCSA> parentNotifier; // Pour notifier le parent (code, objet)

    private Stage editorStage; // Stage de cette fenêtre modale, pour fermeture synchrone

    /**
     * C'est le pannneau qui contient les boutons enregistrer, effacer et autres...
     * Il peut disposer ou non d'un customeditor et est le customeditor d'un
     * maitreFX
     */
    public EditorPaneController() {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/fxml/EditorPane.fxml"));

        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
            activeurIHM = new Monostable(true, false);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Ouvrir un EditorPane depuis n'importe où (méthode statique).
     *
     * @param objetCSA            L'objet à éditer
     * @param maitre              le maitreFX parent
     * @param customEditorFactory factory qui crée le CustomEditor en recevant le
     *                            pane parent, ou null
     * @param parentPath          La clé du parent
     * @return Référence à l'EditorPane créé
     */
    public static EditorPaneController openEditor(ObjetCSA objetCSA,
            MaitreFX maitre,
            Function<EditorPaneController, CustomEditor> customEditorFactory,
            String parentPath) {
        EditorPaneController editor = new EditorPaneController();
        editor.setMaitre(maitre, customEditorFactory);
        editor.charger_donnees(parentPath, objetCSA);
        editor.show();
        return editor;
    }

    /**
     * Ouvrir un EditorPane depuis n'importe où (méthode statique).
     *
     * @param objetCSA            L'objet à éditer
     * @param maitre              le maitreFX parent
     * @param customEditorFactory factory qui crée le CustomEditor en recevant le
     *                            pane parent, ou null
     * @param parentPath          La clé du parent
     * @param callback            code exécuté à la fermeture
     * @return Référence à l'EditorPane créé
     */
    public static EditorPaneController openEditor(ObjetCSA objetCSA,
            MaitreFX maitre,
            Function<EditorPaneController, CustomEditor> customEditorFactory,
            String parentPath,
            Runnable callback) {
        EditorPaneController editor = openEditor(objetCSA, maitre, customEditorFactory, parentPath);
        editor.setOnClose(() -> {
            if (callback != null)
                callback.run();
        });
        return editor;
    }

    /**
     * Ouvrir un EditorPane sans MaitreFX (approche moderne avec callbacks).
     *
     * @param objetCSA            L'objet à éditer
     * @param customEditorFactory factory qui crée le CustomEditor en recevant le
     *                            pane parent, ou null
     * @param parentPath          La clé du parent
     * @return Référence à l'EditorPane créé
     */
    public static EditorPaneController openEditor(ObjetCSA objetCSA,
            ContexteFX contexteFX,
            Function<EditorPaneController, CustomEditor> customEditorFactory,
            String parentPath) {
        contexte = contexteFX;
        EditorPaneController editor = new EditorPaneController();
        if (customEditorFactory != null) {
            editor.setCustomEditor(customEditorFactory);
        }
        editor.charger_donnees(parentPath, objetCSA);
        editor.show();
        return editor;
    }

    public ContexteFX getContexte() {
        if (contexte == null) {
            System.err.println("Attention : EditorPane " + this + " n'a pas de contexte implémenté !\n" +
                    "Ses enfants pourraient ne pas pouvoir communiquer...");
        }
        return contexte;
    }

    /**
     * Configure tous les handlers en une seule fois depuis un {@link ContexteFX}.
     * Remplace les appels individuels à {@code setSaveHandler},
     * {@code setTypeResolver}, etc.
     * Le contexte est mémorisé et propagé automatiquement aux
     * {@link ComboEditableController} enfants.
     *
     * @param ctx le contexte à appliquer (les champs null dans ctx ne remplacent
     *            pas les handlers existants)
     */
    public void setContexte(ContexteFX ctx) {
        this.contexte = ctx;
        /*
         * if (ctx == null) return;
         * if (ctx.getBdHandler() != null) this.bdHandler = ctx.getBdHandler();
         * if (ctx.getTypeResolver() != null) this.typeResolver = ctx.getTypeResolver();
         * if (ctx.getMessageHandler() != null) this.messageHandler =
         * ctx.getMessageHandler();
         */
    }

    public ObjetCSA getObjetManipule() {
        return monObjet;
    }

    public void setOnClose(Runnable handler) {
        onCloseHandler = handler;
    }

    public void show() {
        Scene scene = new Scene(this);
        // Hérite du thème de l'application hôte ; fallback CSS IHMFX si standalone
        StylesheetsFX.apply(scene, this);

        editorStage = new Stage();
        editorStage.initModality(Modality.APPLICATION_MODAL);
        Window owner = StylesheetsFX.findOwnerWindow(this);
        if (owner != null) {
            editorStage.initOwner(owner);
        }
        editorStage.setScene(scene);

        editorStage.setOnCloseRequest(event -> {
            scene.getStylesheets().clear();
            RegistreDesNoeuds.getInstance().unregisterPrefix(editorPath);
        });
        // On diffère sizeToScene et l'affichage après que le contenu asynchrone
        // (Platform.runLater dans charger_donnees) ait eu le temps de s'exécuter.
        // Sinon la fenêtre s'ouvre "rikiki" car le centerPane est encore vide
        // au moment du sizeToScene().
        Platform.runLater(() -> {
            editorStage.sizeToScene();
            editorStage.showAndWait();
        });

        rafraichir_listes();
    }

    @FXML
    public void initialize() {
        btnAnnuler.setTooltip(new Tooltip("Annuler et fermer cette fenêtre"));
        btnEnregistrer.setTooltip(new Tooltip("Enregistrer et fermer cette fenêtre"));
        btnEffacer.setTooltip(new Tooltip("Vider les champs"));
        rafraichir_listes();
    }

    @FXML
    public void action_annuler(ActionEvent event) {
        fermer_proprement_enfant(event);
    }

    private void fermer_proprement_enfant(ActionEvent event) {
        // Appeler le handler de fermeture s'il existe (pour nettoyage/notification)
        if (onCloseHandler != null) {
            onCloseHandler.run();
        }

        // Nettoyer le registre avant de fermer
        RegistreDesNoeuds.getInstance().unregisterPrefix(editorPath);

        if (editorStage != null) {
            editorStage.close();
        }
    }

    @FXML
    private void action_effacer(ActionEvent event) {
        if (hasEditeurPersonnel()) {
            editeurPersonnel.nettoyer_champs();
        } else {
            monObjet = monObjet.getCopy();
            EcriveurChamps.charger_champs(pairesNomChamp, monObjet);
        }
    }

    /**
     * Devrait permettre de rafraichir la fenêtre !
     */
    public void rafraichir_ihm() {
        Platform.runLater(() -> {
            setCenter(centerPane);
        });
    }

    public void rafraichir_listes() {
        if (editeurPersonnel != null) {
            // prévenir l'éditeur personnel. Un code spécifique a dû être écrit puisqu'il
            // est customizé
            editeurPersonnel.rafraichir_listes();
            return;
        }
        // Publier uniquement pour ce panneau et ses enfants
        // Rafraîchir tout ce qui est sous ce panneau (garder le '.'!!)
        String prefix = editorPath.isEmpty() ? "" : editorPath + ".";
        RegistreDesNoeuds.getInstance().publishWithPrefix(prefix, null);
    }

    @FXML
    private void action_rafraichir(ActionEvent event) {
        if (!activeurIHM.isStable()) {
            System.out.println("* EditorPaneController/action_rafraichir : doublon détecté. Ignoré !");
            return;
        }
        activeurIHM.go();

        // Approche moderne : utiliser le callback d'abord
        if (contexte != null && monObjet != null) {
            contexte.getBdHandler().execute(4, monObjet.getCopy(), arraylist -> {
                new Rafraichisseur().traitementRetour(new ArrayList<>(arraylist));
            });
        }

        // Fallback : comportement historique avec MaitreFX
        if (mom != null) {
            mom.appeler_bd(4, monObjet, new Rafraichisseur());
            return;
        }

        affiche_console("Attention: l'editorPane " + this + " n'a pas de maître donc ne peut rafraichir " +
                "l'objet " + monObjet + " donc réinitialise tout simplement les données");
        reinitialiser_affichage(monObjet);
    }

    private void reinitialiser_affichage(ObjetCSA objetCSA) {
        if (editeurPersonnel == null) {
            charger_donnees(prefixePath, objetCSA);
            return;
        }
        monObjet = objetCSA;
        editeurPersonnel.charger_donnees(objetCSA);
    }

    @FXML
    protected void action_enregistrer(ActionEvent event) {
        if (!activeurIHM.isStable()) {
            System.out.println("* EditorPaneController/action_enregistrer : doublon détecté. Ignoré !");
            return;
        }
        activeurIHM.go();

        // on aspire les données des champs de l'objet
        if (enregistrer_champs()) {
            int code = 1;
            if (monObjet.getId() > 0) {
                code = 2;
                if (monObjet.getHashModifications().isEmpty()) {
                    affiche_utilisateur("Aucune modification détectée. Pas d'enregistrement possible !");
                    return;
                }
            }

            if (contexte != null) {
                ContexteFX.HandlerBD bdHandler = contexte.getBdHandler();
                if (bdHandler != null) {
                    bdHandler.execute(code, monObjet, list -> Platform.runLater(() -> {
                        if (list != null && !list.isEmpty() &&
                                list.get(0) instanceof Integer kode && kode > 0) {
                            if (list.size() > 2 && list.get(2) instanceof Integer nouvelID) {
                                monObjet.setId(nouvelID);
                            }
                            fermer_proprement_enfant(event);
                            if (parentNotifier != null) {
                                parentNotifier.accept(kode, monObjet);
                            }
                        } else {
                            String messageAdditionnel = (list != null && list.size() > 1) ? " : " + list.get(1) : ".";
                            affiche_utilisateur_local(
                                    "Échec de l'enregistrement distant de l'objet" + messageAdditionnel);
                        }
                    }));
                }
            } else if (parentNotifier != null) {
                // Approche moderne : utiliser le callback
                parentNotifier.accept(code, monObjet);
                fermer_proprement_enfant(event);
            } else if (mom != null) {
                // Fallback : comportement historique avec MaitreFX
                mom.appeler_bd(code, monObjet, null);
                fermer_proprement_enfant(event);
            } else {
                affiche_utilisateur(
                        "Aucun handler d'enregistrement disponible (ni saveHandler ni parentNotifier ni mom).\n" +
                                "Impossible d'enregistrer l'objet.");
            }

            return;
        }
        affiche_console("Échec de l'enregistrement des champs !" +
                "\nIl faut soit implémenter aspirer_donnees, soit charger le LecteurChamps de l'éditeur");
    }

    public MaitreFX getMaitre() {
        return mom;
    }

    public void setPostCreateCallback(Consumer<ObjetCSA> callback) {
        this.postCreateCallback = callback;
    }

    /**
     * Définit le callback pour notifier le parent d'événements (création,
     * modification).
     * Remplace l'utilisation de mom.envoyer_vers_parent().
     *
     * @param notifier BiConsumer qui reçoit (code, objet) où code = 1 (création) ou
     *                 2 (modification)
     */
    public void setParentNotifier(java.util.function.BiConsumer<Integer, ObjetCSA> notifier) {
        this.parentNotifier = notifier;
    }

    /**
     * Permet de définir la fenêtre parente et éventuellement, le customEditor.
     *
     * @param mom                 le parent
     * @param customEditorFactory factory {@code pane -> new MonEditeur(pane)}, ou
     *                            null
     */
    public void setMaitre(MaitreFX mom, Function<EditorPaneController, CustomEditor> customEditorFactory) {
        this.mom = mom;
        if (customEditorFactory != null) {
            editeurPersonnel = customEditorFactory.apply(this);
            integrate_personalEditor();
        }
    }

    private void integrate_personalEditor() {
        Platform.runLater(() -> {
            centerPane.setCenter(editeurPersonnel);
            setWidth(editeurPersonnel.getWidth() * 1.1);
            setHeight(editeurPersonnel.getHeight() + 120);
        });
    }

    /**
     * Définit le CustomEditor via une factory.
     * La factory reçoit ce pane en paramètre, ce qui garantit que le CustomEditor
     * est toujours associé à son EditorPane parent dès sa construction.
     *
     * @param factory {@code pane -> new MonEditeur(pane)}
     */
    public void setCustomEditor(Function<EditorPaneController, CustomEditor> factory) {
        editeurPersonnel = factory.apply(this);
        integrate_personalEditor();
    }

    private boolean enregistrer_champs() {
        if (hasEditeurPersonnel()) {
            // on travaille avec l'éditeur perso
            monObjet = editeurPersonnel.aspirer_donnees();
            return monObjet != null;
        }

        monObjet = LecteurChamps.enregistrer_donnees(monObjet, pairesNomChamp);
        return monObjet != null;
    }

    private LinkedHashMap<String, LinkedHashMap<String, Node>> classer_paires_nom_champs(
            LinkedHashMap<String, Node> paires,
            ObjetCSA objetCSA) {
        LinkedHashMap<String, LinkedHashMap<String, Node>> terug = new LinkedHashMap<>();

        terug.put("base", createSubPairesNomChamps(paires, objetCSA.getExpectedSimpleData().keySet()));
        terug.put("intermédiaires", createSubPairesNomChamps(paires, objetCSA.getFKIs().keySet()));
        terug.put("fks", createSubPairesNomChamps(paires, objetCSA.getFKs().keySet()));
        terug.put("dépendants", createSubPairesNomChamps(paires, objetCSA.getHashmapDependants().keySet()));
        terug.put("listes", createSubPairesNomChamps(paires, objetCSA.getHashListes().keySet()));
        terug.put("imbrications", createSubPairesNomChamps(paires, objetCSA.getGestionnairesImbrications().keySet()));

        return terug;
    }

    LinkedHashMap<String, Node> createSubPairesNomChamps(LinkedHashMap<String, Node> original,
            Set<String> keys) {
        LinkedHashMap<String, Node> subMap = new LinkedHashMap<>();

        for (Map.Entry<String, Node> entry : original.entrySet()) {
            if (keys.contains(entry.getKey())) {
                subMap.put(entry.getKey(), entry.getValue());
            }
        }
        return subMap;
    }

    public ObjetCSA aspirer_donnees() {
        if (hasEditeurPersonnel()) {
            return editeurPersonnel.aspirer_donnees();
        }
        return LecteurChamps.enregistrer_donnees(monObjet, pairesNomChamp);
    }

    /**
     * Permet de charger les informations nécessaires à l'édition de l'élémentBD
     * À UTILISER LORSQU'ON N'A PAS D'ÉDITEUR CUSTOMIZÉ ; LES DONNÉES
     * supplémentaires À AFFICHER SONT
     * ALORS DANS LA HASHMAP paires
     * Evolution :
     * Si la hashmap est null ou vide, les données de base de l'objet sont
     * automatiquement créées
     *
     * @param elementBD  c'est l'objet à éditer ; ne peut être null
     * @param parentPath C'est le nom unique du parent (qui donne naissance à cet
     *                   EditorPane)
     */
    public void charger_donnees(String parentPath, ObjetCSA elementBD) {
        if (elementBD == null && monObjet == null) {
            affiche_console("EditorPane/charger_donnees : je refuse de charger NULL alors que je" +
                    " ne connais déjà pas mon type à afficher !");
            return;
        }
        if (hasEditeurPersonnel()) {
            monObjet = elementBD;

            lblEntete.setText(monObjet == null ? "Non défini" : monObjet.getPublicName());
            editeurPersonnel.charger_donnees(monObjet);
            return;
        }
        monObjet = elementBD == null ? monObjet.getNewInstance() : elementBD;
        // permet de faire des modifs à l'objet de référence
        if (postCreateCallback != null)
            postCreateCallback.accept(monObjet);

        prefixePath = parentPath.isEmpty() ? "" : parentPath + ".";
        editorPath = prefixePath + monObjet.getType();
        pairesNomChamp.clear();
        pairesNomChamp.putAll(creer_paires_de_base(monObjet));

        EcriveurChamps.charger_champs(pairesNomChamp, monObjet);
        editeurPersonnel = null;

        LinkedHashMap<String, LinkedHashMap<String, Node>> pairesFractionnees = classer_paires_nom_champs(
                pairesNomChamp,
                monObjet);

        Platform.runLater(() -> {
            lblEntete.setText(monObjet.getPublicName());
            VBox content = new VBox();
            // panneau 1
            LinkedHashMap<String, Node> pairesBase = pairesFractionnees.get("base");
            if (!pairesBase.isEmpty()) {
                VBox vBox = new VBox(10);

                vBox.getChildren().add(creer_grille(pairesBase));
                LinkedHashMap<String, Node> pairesIntermediaires = pairesFractionnees.get("intermédiaires");
                if (!pairesIntermediaires.isEmpty()) {
                    vBox.getChildren().add(new Separator(Orientation.HORIZONTAL));
                    vBox.getChildren().add(creer_grille(pairesIntermediaires));
                }
                content.getChildren().add(new TitledPane("Données de base", vBox));
            }
            // panneau 2
            LinkedHashMap<String, Node> pairesImbrications = pairesFractionnees.get("imbrications");
            if (!pairesImbrications.isEmpty()) {
                content.getChildren().add(creer_zone_imbrications(pairesImbrications));
            }
            // panneau 3
            LinkedHashMap<String, Node> pairesFKs = pairesFractionnees.get("fks");
            if (!pairesFKs.isEmpty()) {
                VBox vBox = new VBox(10);
                vBox.getChildren().add(creer_grille(pairesFKs));
                LinkedHashMap<String, Node> pairesDependants = pairesFractionnees.get("dépendants");
                if (!pairesDependants.isEmpty()) {
                    vBox.getChildren().add(new Separator(Orientation.HORIZONTAL));
                    vBox.getChildren().add(creer_grille(pairesDependants));
                }
                content.getChildren().add(new TitledPane("Éléments liés", vBox));
            }
            // panneau listes
            LinkedHashMap<String, Node> pairesListes = pairesFractionnees.get("listes");
            if (!pairesListes.isEmpty()) {
                Accordion accordeon = new Accordion();
                accordeon.getPanes().addAll(creer_titledPanes(pairesListes));
                if (!accordeon.getPanes().isEmpty()) {
                    accordeon.setExpandedPane(accordeon.getPanes().get(0));
                }
                content.getChildren().add(accordeon);
            }
            ScrollPane scrollPane = new ScrollPane(content);
            scrollPane.setFitToWidth(true); // Crucial for VBox content
            centerPane.setCenter(scrollPane);
        });
    }

    private List<TitledPane> creer_titledPanes(LinkedHashMap<String, Node> pairesListes) {
        List<TitledPane> listeRetour = new ArrayList<>();
        for (Map.Entry<String, Node> paire : pairesListes.entrySet()) {
            String nomTable = paire.getKey();
            ObjetCSA objetCSA = getElementFromType(nomTable);
            if (objetCSA != null) {
                // on peut insérer des éléments qui ne sont pas des nomsTable (p. ex : ajout,
                // suppression, etc
                String nomPublic = objetCSA.getPublicName();
                TitledPane titledPane = new TitledPane("Liste de " +
                        nomPublic + (nomPublic.matches(".*[sx]$") ? "" : "s"),
                        paire.getValue());
                listeRetour.add(titledPane);
            }
        }
        return listeRetour;
    }

    private Accordion creer_zone_imbrications(LinkedHashMap<String, Node> pairesImbrications) {
        Accordion terug = new Accordion();
        for (Map.Entry<String, Node> paire : pairesImbrications.entrySet()) {
            Node titledpane = paire.getValue();
            if (titledpane instanceof CombosImbriques) {
                terug.getPanes().add((CombosImbriques) titledpane);
            } else {
                System.err.println("++ EditorPane/creer_zone_imbrications : Je devrais avoir un titledpane" +
                        ", mais j'ai : " + titledpane);
            }
        }
        if (!terug.getPanes().isEmpty()) {
            terug.setExpandedPane(terug.getPanes().get(0));
        }
        return terug;
    }

    private GridPane creer_grille(LinkedHashMap<String, Node> paires) {
        GridPane gridPane = new GridPane();
        gridPane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        gridPane.setPadding(new Insets(10));
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        // Column constraints - adjust for full-width columns
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(15);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(35);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(15);
        ColumnConstraints col4 = new ColumnConstraints();
        col4.setPercentWidth(35);
        gridPane.getColumnConstraints().addAll(col1, col2, col3, col4);

        int ligne = 0;
        boolean gauche = true;

        for (Map.Entry<String, Node> paire : paires.entrySet()) {
            String cle = paire.getKey();
            // retirer complètement les paires avec le nom commençant par '£' ou 'id'
            if (cle != null && !cle.matches("^(£|id).*")) {
                // retirer les labels des paires dont le nom commence par #
                String nomExterne = (cle.isEmpty() || cle.startsWith("#")) ? "" : monObjet.getNomPublic(cle);

                Label label = new Label(nomExterne.isEmpty() ? "" : nomExterne + " :");
                label.setMinWidth(Region.USE_PREF_SIZE);
                label.setPrefWidth(Region.USE_COMPUTED_SIZE);
                label.setMaxWidth(Double.MAX_VALUE);
                label.setWrapText(true);

                Node noeud = paire.getValue();

                // Apply minimum width only if the Node is a Region
                if (noeud instanceof Region) {
                    ((Region) noeud).setMinWidth(120);
                }

                // Check if this field should span full width (customized/bigger ones)
                boolean isFullWidth = shouldBeFullWidth(cle, noeud);

                if (isFullWidth) {
                    if (!gauche) {
                        ligne++;
                    }
                    // Full width field - span all 4 columns
                    gridPane.add(label, 0, ligne); // label spans all columns
                    gridPane.add(noeud, 1, ligne, 3, 1); // node spans all columns
                    ligne++;
                    gauche = true; // Reset to left side for next field
                } else {
                    // Regular field - use 2-column layout
                    if (gauche) {
                        gridPane.add(label, 0, ligne);
                        gridPane.add(noeud, 1, ligne);
                        gauche = false;
                    } else {
                        gridPane.add(label, 2, ligne);
                        gridPane.add(noeud, 3, ligne);
                        gauche = true;
                        ligne++;
                    }
                }
            }
        }

        gridPane.setHgap(20);
        gridPane.setVgap(10);

        return gridPane;
    }

    private boolean shouldBeFullWidth(String cle, Node noeud) {
        boolean pleineLigne = !(noeud instanceof TextField);
        pleineLigne &= !(noeud instanceof Labeled);
        pleineLigne &= !(noeud instanceof DatePicker);
        pleineLigne &= !(noeud instanceof DateBoxController);
        return pleineLigne;
    }

    private HashMap<String, Node> creer_paires_de_base(ObjetCSA objetCSA) {
        HashMap<String, Node> terug = new LinkedHashMap<>();
        // parcourir les données de base et les fk intermédaires
        for (Map.Entry<String, Object> paire : objetCSA.getExpectedSimpleData().entrySet()) {
            String cle = paire.getKey();
            Object valeur = paire.getValue();
            String fullPath = editorPath.isEmpty() ? cle : editorPath + "." + cle;
            // 1. Chercher une customisation globale
            Node customNode = RegistreDesNoeuds.getInstance().getCustomNode(objetCSA.getType(), fullPath, objetCSA);
            // 2. Sinon, créer le node par défaut
            Node node = (customNode != null) ? customNode : creer_node_par_defaut(valeur, fullPath, objetCSA);

            terug.put(cle, node);

            // Enregistrer dans le registre
            inscrire_dans_registre(fullPath, node);
        }
        // les gestionnaires d'imbrication
        for (Map.Entry<String, GestionnaireImbrications> paire : objetCSA.getGestionnairesImbrications().entrySet()) {
            String cle = paire.getKey();
            String fullPath = editorPath.isEmpty() ? cle : editorPath + "." + cle;
            // 1. Chercher une customisation globale
            Node customNode = RegistreDesNoeuds.getInstance().getCustomNode(objetCSA.getType(), fullPath, objetCSA);
            // 2. Sinon, créer le node par défaut
            Node node = (customNode != null) ? customNode : new CombosImbriques();
            terug.put(cle, node);
            // IMPORTANT : Enregistrer dans le registre global
            inscrire_dans_registre(fullPath, node);
        }
        // les fks
        for (Map.Entry<String, ObjetCSA> paire : objetCSA.getFKs().entrySet()) {
            String cle = paire.getKey();
            ObjetCSA valeur = paire.getValue();
            // 1. Chercher une customisation globale
            Node customNode = RegistreDesNoeuds.getInstance().getCustomNode(objetCSA.getType(),
                    editorPath + "." + cle,
                    objetCSA);
            // 2. Sinon, créer le node par défaut
            Node node;
            if (customNode != null) {
                node = customNode;
            } else {
                ComboEditableController<?> cmbe = new ComboEditableController<>(valeur.getClass());
                cmbe.setMaitre(this);
                // Propager le contexte si disponible (les handlers individuels ont priorité)
                if (contexte != null)
                    cmbe.setContexte(contexte);
                node = cmbe;
            }

            terug.put(cle, node);

            String fullPath = editorPath.isEmpty() ? cle : editorPath + "." + cle;
            // inscrire dans le registre
            inscrire_dans_registre(fullPath, node);
            // S'abonner aux mises à jour de liste
            final Node finalNode = node;
            RegistreDesNoeuds.getInstance().subscribe(fullPath, data -> {
                if (data == null) {
                    if (finalNode instanceof ComboEditableController) {
                        ((ComboEditableController<?>) finalNode).rafraichir_liste();
                    } else if (finalNode instanceof ListeBDController) {
                        // todo : ce serait mieux de lui mettre un rafraichir_liste
                        ((ListeBDController) finalNode).clearSelection();
                    }
                } else if (data instanceof List<?>) {
                    if (finalNode instanceof ComboEditableController) {
                        ((ComboEditableController<?>) finalNode).setListe((Collection<?>) data);
                    } else if (finalNode instanceof ListeBDController) {
                        // todo : ce serait mieux de lui mettre un rafraichir_liste
                        ((ListeBDController) finalNode).chargerListe((new ArrayList<>((List<?>) data)));
                    }
                }
            });

        }
        // les listes
        for (Map.Entry<String, Object> paire : objetCSA.getHashListes().entrySet()) {
            String cle = paire.getKey();
            String fullPath = editorPath.isEmpty() ? cle : editorPath + "." + cle;
            // 1. Chercher une customisation globale
            Node customNode = RegistreDesNoeuds.getInstance().getCustomNode(objetCSA.getType(), fullPath, objetCSA);
            // 2. Sinon, créer le node par défaut
            Node node;
            if (customNode != null) {
                node = customNode;
            } else {
                TableViewCRUD tableCRUD = new TableViewCRUD();
                if (contexte != null)
                    tableCRUD.setContexte(contexte);
                node = tableCRUD;
            }

            // IMPORTANT : Enregistrer dans le registre global
            inscrire_dans_registre(fullPath, node);
            terug.put(cle, node);
        }

        return terug;
    }

    private void inscrire_dans_registre(String fullPath, Node node) {
        affiche_console("* Inscription de " + node + " à l'adresse : " + fullPath);
        RegistreDesNoeuds.getInstance().register(fullPath, node);
    }

    private Node creer_node_par_defaut(Object valeur, String cle, ObjetCSA objetCSA) {
        Node terug;
        if (valeur instanceof Boolean) {
            CheckBox checkBox = new CheckBox();
            checkBox.setOnMouseClicked(clicEvent -> {
                action_utilisateur(objetCSA, checkBox, "clic sur checkbox : " + cle + " -> " + valeur);
            });
            terug = checkBox;
        } else if (valeur instanceof LocalDate) {
            terug = new DateBoxController();
        } else if (valeur instanceof LocalDateTime) {
            terug = new DatePicker();
        } else if (cle.toLowerCase().contains("couleur") &&
                valeur instanceof Integer) {
            // Create ColorPicker
            ColorPicker colorPicker = new ColorPicker();
            colorPicker.setValue(FX_RGBConverter.convert_rgb_2_color(objetCSA.getCouleur()));
            terug = colorPicker;
        } else {
            TextField textField = new TextField();
            textField.setOnKeyPressed(event -> {
                action_utilisateur(objetCSA, textField, "tape sur textField : " + cle + " -> " + valeur);
            });
            terug = textField;
        }
        return terug;
    }

    @Override
    public String toString() {
        return "EditorPane " + this.hashCode() + " - contient des " +
                (monObjet == null ? "NULL" : monObjet.getPublicName() + " et identifié par " + editorPath);
    }

    @Override
    public void appeler_bd(int i, Object o, GestionnaireRetour gestionnaireRetour) {
        if (contexte != null && o instanceof ObjetCSA objetCSA) {
            contexte.getBdHandler().execute(i, objetCSA, list -> Platform.runLater(() -> {
                if (list != null && !list.isEmpty()) {
                    affiche_utilisateur_local(list.get(0) instanceof Integer k && k > 0
                            ? "Opération réussie."
                            : "Échec : " + (list.size() > 1 ? list.get(1) : "sans détail"));
                }
                retour_appelant(list, gestionnaireRetour);
            }));
            return;
        }
        if (parentNotifier != null) {
            parentNotifier.accept(i, (ObjetCSA) o);
            affiche_console("Notification envoyée au parent via parentNotifier : code " + i + ", objet " + o +
                    ", mais le gestionnaire retour ne reçoit pas l'info : bug probable !");
            return;
        }
        if (mom != null) {
            System.out.println("**DEBUG**\nEnvoi de " + o + " vers parent de " + this + " vers " + mom + "\n");
            mom.appeler_bd(i, o, gestionnaireRetour);
            return;
        }
        System.err.println("** EditorPaneController : Mon manager ne connait pas son maitreFX !");
    }

    private void retour_appelant(List<Object> list, GestionnaireRetour gestionnaireRetour) {
        Platform.runLater(() -> {
            if (gestionnaireRetour != null) {
                gestionnaireRetour.traitementRetour(new ArrayList<>(list));
            } else {
                affiche_console("Aucun gestionnaire de retour fourni pour traiter la réponse du serveur !");
            }
        });
    }

    public void affiche_utilisateur(String texte) {
        Platform.runLater(() -> {
            String tronque = tronquerTexte(texte, 120);
            lblCommentaire.setText(tronque);
            lblCommentaire.setTooltip(new Tooltip(texte));
        });

        // Approche moderne : utiliser le callback d'abord
        if (contexte != null) {
            contexte.getMessageHandler().accept(texte);
            return;
        }

        // Fallback : comportement historique avec MaitreFX
        if (mom != null) {
            mom.affiche_utilisateur(texte);
        }
    }

    /**
     * Affiche un message SEULEMENT dans cette fenêtre modale (ne remonte pas vers
     * le parent).
     * Utile pour les messages d'erreur qui concernent uniquement cette fenêtre.
     */
    private void affiche_utilisateur_local(String texte) {
        Platform.runLater(() -> {
            String tronque = tronquerTexte(texte, 120);
            lblCommentaire.setText(tronque);
            lblCommentaire.setTooltip(new Tooltip(texte));
        });
    }

    /**
     * Tronque un texte à une longueur maximale en ajoutant "…" si nÃ©cessaire.
     */
    private static String tronquerTexte(String texte, int maxLength) {
        if (texte == null)
            return "";
        if (texte.length() <= maxLength)
            return texte;
        return texte.substring(0, maxLength) + "…";
    }

    @Override
    public void affiche_console(String texte) {
        if (mom == null) {
            System.out.println("* EditorPaneController >> " + texte);
            return;
        }
        if (mom.isTest()) {
            mom.affiche_console("* EditorPaneController >> " + texte);
        }
    }

    @Override
    public ObjetCSA getElementFromType(String typeAffiche) {
        // Approche moderne : utiliser le callback d'abord
        if (contexte != null) {
            return contexte.getTypeResolver().apply(typeAffiche);
        }

        // Fallback : comportement historique avec MaitreFX
        if (mom != null) {
            return mom.getElementFromType(typeAffiche);
        }

        System.err.println("** EditorPaneController : Ni typeResolver ni mom défini !");
        return null;
    }

    @Override
    public void action_utilisateur(Object selectedItem, Object source, String nom) {
        // tous les potentiels descendants passent par ici. Si je sais quoi faire, ok
        // sinon, je remonte l'info
        if (mom != null) {
            mom.action_utilisateur(selectedItem, source, nom);
            return;
        }
        System.err.println("** EditorPaneController : Mon manager ne connait pas son maitreFX !" +
                " Par conséquent, l'action utilisateur de " + source + " n'est pas répercutée!");

    }

    @Override
    public void affiche_details(Object selectedItem) {
        mom.affiche_details(selectedItem);
    }

    @Override
    public void nettoyer_champs() {
        // je préviens ma mère de mon nettoyage
        mom.action_utilisateur(null, this, "j'ai nettoyé mes champs");
        // je me nettoie
        charger_donnees(prefixePath, monObjet == null ? null : monObjet.getNewInstance());
        // je nettoie mes descendants

    }

    @Override
    public void charger_donnees_vers_enfant(ObjetCSA hashObjet) {
        charger_donnees(prefixePath, hashObjet);
    }

    @Override
    public void rafraichir() {
        action_rafraichir(null);
    }

    @Override
    public boolean isTest() {
        if (mom == null) {
            return false;
        }
        return mom.isTest();
    }

    public CustomEditor getEditeurPersonnel() {
        return editeurPersonnel;
    }

    public boolean hasEditeurPersonnel() {
        return editeurPersonnel != null;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void rafraichir_liste(ArrayList<?> objets, String type) {
        if (editeurPersonnel != null) {
            editeurPersonnel.rafraichir_liste(objets, type);
        }
    }

    /**
     * Permet de remplacer un node par défaut par un node personnalisé
     * DOIT être appelé AVANT charger_donnees()
     *
     * @param customNode le nouveau Node
     * @param nomChamp   son nom unique
     */
    public void overrideNode(String nomChamp, Node customNode) {
        pairesNomChamp.put(nomChamp, customNode);

        // Mettre à jour aussi dans le registre
        String fullPath = editorPath.isEmpty() ? nomChamp : editorPath + "." + nomChamp;
        inscrire_dans_registre(fullPath, customNode);
    }

    /**
     * Remplacer plusieurs nodes en une fois
     *
     * @param customNodes la liste de paires (nom, Node) qui doivent écraser les
     *                    existantes
     */
    public void overrideNodes(LinkedHashMap<String, Node> customNodes) {
        customNodes.forEach(this::overrideNode);
    }

    public String getEditorPath() {
        return editorPath;
    }

    /*
     ************** CLASSES INTERNES ********************************
     */
    private class Rafraichisseur implements GestionnaireRetour {
        @Override
        public void traitementRetour(ArrayList<Object> arrayList) {
            if (!arrayList.isEmpty() && arrayList.get(0) instanceof Integer) {
                int k = (int) arrayList.get(0);
                if (k == 4) {
                    if (arrayList.size() > 2) {
                        String nomTable = String.valueOf(arrayList.get(1));
                        Object objet = arrayList.get(2);
                        if (objet instanceof ObjetCSA) {
                            reinitialiser_affichage((ObjetCSA) objet);
                            return;
                        }
                        // ça peut aussi être une monoliste ! (cas d'une requête "détail" par id)
                        if (monObjet != null && monObjet.getId() > 0
                                && objet instanceof ArrayList
                                && ((ArrayList<?>) objet).size() == 1
                                && ((ArrayList<?>) objet).get(0) instanceof ObjetCSA) {
                            reinitialiser_affichage((ObjetCSA) ((ArrayList<?>) objet).get(0));
                            return;
                        }
                        affiche_console("Rafraichisseur de EditorPane s'attend à recevoir un objetCSA" +
                                ", mais reçoit " + objet + " de type " + nomTable + "\nRetour ignoré !");
                        return;
                    }
                    affiche_console("Rafraichisseur de EditorPane s'attend à recevoir au moins " +
                            "3 éléments, mais reçoit " + arrayList + "\nRetour ignoré !");
                    return;
                }
                affiche_console("Le rafraichisseur de EditorPane n'attend pas autre chose qu'un code 4, mais " +
                        "reçoit : " + k);
            }
        }
    }
}
