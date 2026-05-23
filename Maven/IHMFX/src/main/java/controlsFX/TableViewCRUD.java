package controlsFX;

import general.GestionnaireRetour;
import general.ObjetCSA;
import generalFX.GenerateurColonnesTableView;
import generalFX.ObjetIHM;
import generalFX.TransformeurObjets;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Composant tableau CRUD embarqué dans un EditorPane, gérant une liste de sous-objets en mémoire.
 * <p>
 * Supporte deux modes de configuration :
 * <ul>
 *   <li><b>Ancien mode</b> : {@code new TableViewCRUD(maitreFX)} — propagation via l'interface MaitreFX</li>
 *   <li><b>Nouveau mode</b> : {@code new TableViewCRUD()} + {@link #setContexte(ContexteFX)} — handlers fonctionnels</li>
 * </ul>
 * Les handlers fonctionnels ont toujours la priorité sur le MaitreFX.
 */
public class TableViewCRUD extends BorderPane implements MaitreFX {

    private final TreeSet<ObjetIHM> masterSet = new TreeSet<>();
    private final ObservableList<ObjetIHM> tableItems = FXCollections.observableArrayList();
    private final ArrayList<Object> listeInitiale = new ArrayList<>();
    private final TableView<ObjetIHM> tableView = new TableView<>();

    /**
     * Parent MaitreFX (système déprécié). Utilisé comme fallback quand les handlers ne sont pas configurés.
     *
     * @deprecated Utiliser {@link #setContexte(ContexteFX)} ou les setters individuels.
     */
    @Deprecated
    private final MaitreFX maitreFX;

    GenerateurColonnesTableView generateurColonnes;
    private ObjetCSA elementDeReference = null;
    private boolean isContentFree = false;
    private EditorPaneController editorPaneController = null;

    // Handlers fonctionnels (remplacent MaitreFX)
    private Consumer<String> messageHandler;
    private Consumer<String> consoleHandler;
    private Function<String, ObjetCSA> typeResolver;
    private BiConsumer<Object, String> userActionHandler;
    /**
     * Contexte partagé (propagé automatiquement à l'EditorPane enfant).
     */
    private ContexteFX contexte;

    // -------------------------------------------------------------------------
    // Constructeurs
    // -------------------------------------------------------------------------

    /**
     * Constructeur pour l'utilisation avec {@link ContexteFX}.
     * Appeler {@link #setContexte(ContexteFX)} après construction.
     */
    public TableViewCRUD() {
        this(null);
        initialiser_champs();
    }

    /**
     * Constructeur historique avec MaitreFX.
     *
     * @param maitre le MaitreFX parent (peut être null si on utilise setContexte())
     * @deprecated Préférer {@link #TableViewCRUD()} + {@link #setContexte(ContexteFX)}.
     */
    @Deprecated
    public TableViewCRUD(MaitreFX maitre) {
        super();
        this.maitreFX = maitre;

        initialiser_champs();
    }

    private void initialiser_champs() {
        Image addImage = new Image("images/add.png");
        Image suppImage = new Image("images/trash.png");
        Image modImage = new Image("images/edit.png");

        ImageView addIconView = new ImageView(addImage);
        addIconView.setFitWidth(20);
        addIconView.setFitHeight(20);
        addIconView.setPreserveRatio(true);
        Button addButton = new Button("", addIconView);
        addButton.setOnAction(e -> showDialog(elementDeReference.getNewInstance()));

        ImageView suppImageView = new ImageView(suppImage);
        suppImageView.setFitWidth(20);
        suppImageView.setFitHeight(20);
        suppImageView.setPreserveRatio(true);
        Button deleteButton = new Button("", suppImageView);
        deleteButton.setOnAction(e -> deleteSelectedItem());

        ImageView modImageView = new ImageView(modImage);
        modImageView.setFitWidth(20);
        modImageView.setFitHeight(20);
        modImageView.setPreserveRatio(true);
        Button modButton = new Button("", modImageView);
        modButton.setOnAction(e -> modifySelectedItem());

        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10));
        buttonBox.getChildren().addAll(addButton, modButton, deleteButton);

        tableView.setRowFactory(tv -> {
            TableRow<ObjetIHM> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    showDialog(row.getItem().getObjetCSA());
                }
            });
            return row;
        });
        generateurColonnes = new GenerateurColonnesTableView(tableView, this);
        setCenter(tableView);
        setTop(buttonBox);
    }

    // -------------------------------------------------------------------------
    // Configuration par handlers / ContexteFX
    // -------------------------------------------------------------------------

    /**
     * Définit le callback pour afficher des messages à l'utilisateur.
     */
    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    /**
     * Définit le callback pour afficher les messages console/debug.
     */
    public void setConsoleHandler(Consumer<String> handler) {
        this.consoleHandler = handler;
    }

    /**
     * Définit le callback de résolution de type (remplace {@code getElementFromType}).
     */
    public void setTypeResolver(Function<String, ObjetCSA> resolver) {
        this.typeResolver = resolver;
    }

    /**
     * Définit le callback de notification d'actions utilisateur.
     */
    public void setUserActionHandler(BiConsumer<Object, String> handler) {
        this.userActionHandler = handler;
    }

    /**
     * Configure tous les handlers en une seule fois depuis un {@link ContexteFX}.
     * Le contexte est mémorisé et propagé automatiquement à l'EditorPane enfant ouvert via "Ajouter/Modifier".
     *
     * @param ctx le contexte à appliquer (les champs null ne remplacent pas les handlers existants)
     */
    public void setContexte(ContexteFX ctx) {
        this.contexte = ctx;
        if (ctx == null) return;
        if (ctx.getMessageHandler() != null) this.messageHandler = ctx.getMessageHandler();
        if (ctx.getConsoleHandler() != null) this.consoleHandler = ctx.getConsoleHandler();
        if (ctx.getTypeResolver() != null) this.typeResolver = ctx.getTypeResolver();
        if (ctx.getUserActionHandler() != null) this.userActionHandler = ctx.getUserActionHandler();
    }

    // -------------------------------------------------------------------------
    // Logique interne
    // -------------------------------------------------------------------------

    public boolean isContentFree() {
        return isContentFree;
    }

    public void setContentFree(boolean contentFree) {
        isContentFree = contentFree;
    }

    private void updateTableViewFromSet() {
        tableItems.setAll(masterSet);
    }

    private void modifySelectedItem() {
        ObjetIHM selection = tableView.getSelectionModel().getSelectedItem();
        if (selection == null) {
            affiche_utilisateur("Veuillez sélectionner un item svp");
            return;
        }
        showDialog(selection.getObjetCSA());
    }

    /**
     * Charge les données dans le tableau.
     *
     * @param donnees liste d'objets à afficher
     * @param ref     objet de référence indiquant le type (peut être null)
     */
    public void charger_liste(ArrayList<Object> donnees, ObjetCSA ref) {
        listeInitiale.clear();
        listeInitiale.addAll(donnees);
        if (ref != null) {
            elementDeReference = ref.getNewInstance();
        } else {
            if (donnees.isEmpty() || !(donnees.get(0) instanceof ObjetCSA)) {
                Platform.runLater(() -> {
                    masterSet.clear();
                    updateTableViewFromSet();
                    tableView.setItems(tableItems);
                });
                return;
            }
            elementDeReference = ((ObjetCSA) donnees.get(0)).getNewInstance();
        }
        ArrayList<ObjetIHM> objetIHMS = TransformeurObjets.transformer_en_objetsIHM(donnees, elementDeReference, this);
        generateurColonnes.setColumn(new ObjetIHM(elementDeReference));
        Platform.runLater(() -> {
            masterSet.clear();
            masterSet.addAll(objetIHMS);
            updateTableViewFromSet();
            tableView.setItems(tableItems);
        });
    }

    @Override
    public String toString() {
        return "[" + super.toString() + " ( " + (isContentFree ? "libre" : "contraint à ids") + ")]";
    }

    private void showDialog(ObjetCSA objetCSA) {
        affiche_console("$$DEBUG : le tablecrud " + this + " est " + (isContentFree ? "libre" : "verrouillé => liste"));
        if (elementDeReference == null) {
            affiche_utilisateur("Aucune possibilité de gérer les éléments affichés ici");
            return;
        }
        String heritedPath = (maitreFX instanceof EditorPaneController) ?
                ((EditorPaneController) maitreFX).getEditorPath() : "";
        String myObject = elementDeReference.getType();
        String fullPath = heritedPath.isEmpty() ? myObject : heritedPath + "." + myObject;

        editorPaneController = EditorPaneController.openEditor(
                objetCSA.getObjetRacine(), this, null, fullPath);

        // Propager le contexte vers l'EditorPane enfant
        if (contexte != null) editorPaneController.setContexte(contexte);

        // L'EditorPane notifie via envoyer_vers_parent (MaitreFX) → géré dans notre implémentation ci-dessous
    }

    private void deleteSelectedItem() {
        ObjetIHM selectedItem = tableView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert("Information", "Veuillez sélectionner un élément à supprimer");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de suppression");
        confirmation.setHeaderText("Supprimer l'élément ");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer '" + selectedItem.getNom() + "' ?");
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                tableView.getItems().remove(selectedItem);
                affiche_utilisateur("Item " + selectedItem + " supprimé avec succès");
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // -------------------------------------------------------------------------
    // Implémentation MaitreFX
    // -------------------------------------------------------------------------

    /**
     * Reçoit les notifications des enfants (EditorPane). Gère localement les objets
     * du même type que {@code elementDeReference}, remonte les autres vers le parent.
     */
    @Override
    public void appeler_bd(int code, Object objet, GestionnaireRetour gestionnaireRetour) {
        if (objet instanceof ObjetCSA objetCSA) {
            if (elementDeReference != null && objetCSA.getType().equals(elementDeReference.getType())) {
                remplacer_masterSet(objetCSA);
                updateTableViewFromSet();
            }
            // Notifier le parent (au cas où il voudrait aussi réagir)
            if (userActionHandler != null) {
                userActionHandler.accept(objet, "appel de tableCRUD");
            } else if (maitreFX != null) {
                maitreFX.action_utilisateur(objet, this, "appel de tableCRUD");
            }
            return;
        }
        if (objet instanceof List<?> liste) {
            if (isListePourMoi(liste)) {
                liste.forEach(item -> {
                    if (item instanceof ObjetCSA) {
                        remplacer_masterSet((ObjetCSA) item);
                    } else {
                        affiche_console("TableViewCRUD/requete_BD : " + item +
                                " aurait dû être de type ObjetCSA. L'insertion sera ignorée !");
                    }
                });
                updateTableViewFromSet();
                return;
            }
        }
        affiche_console("* TableViewCRUD ne gère pas " + objet + " donc fait remonter la requête");
        remonter_vers_parent(code, objet, gestionnaireRetour);
    }

    private boolean isListePourMoi(List<?> liste) {
        for (Object item : liste) {
            if (item instanceof ObjetCSA && elementDeReference != null &&
                    ((ObjetCSA) item).getType().equals(elementDeReference.getType())) {
                return true;
            }
        }
        return false;
    }

    private void remonter_vers_parent(int code, Object objet, GestionnaireRetour gestionnaireRetour) {
        if (maitreFX == null) {
            affiche_console("TableViewCRUD/remonter_vers_parent : pas de maitreFX, appel ignoré : " + code + " - " + objet);
            return;
        }
        affiche_console("* Le tableviewCRUD envoie l'info " + code + " - " + objet + " vers son parent " + maitreFX);
        maitreFX.appeler_bd(code, objet, gestionnaireRetour);
    }

    private void remplacer_masterSet(ObjetCSA objet) {
        ObjetIHM nouvelItem = new ObjetIHM(remplacer_racine(objet));
        masterSet.remove(nouvelItem);
        masterSet.add(nouvelItem);
    }

    private ObjetCSA remplacer_racine(ObjetCSA etranger) {
        for (ObjetIHM element : masterSet) {
            if (element.getObjetCSA().getObjetRacine().equals(etranger)) {
                ObjetCSA elu = element.getObjetCSA();
                elu.setObjetRacine(etranger);
                return elu;
            }
        }
        return etranger;
    }

    @Override
    public void affiche_utilisateur(String texte) {
        if (messageHandler != null) {
            messageHandler.accept(texte);
        } else if (maitreFX != null) {
            maitreFX.affiche_utilisateur(texte);
        } else {
            System.out.println("* TableViewCRUD : " + texte);
        }
    }

    @Override
    public void affiche_console(String texte) {
        if (consoleHandler != null) {
            consoleHandler.accept("TableViewCRUD : " + texte);
            return;
        }
        if (maitreFX != null) {
            maitreFX.affiche_console("TableViewCRUD : " + texte);
            return;
        }
        System.err.println("*** TableViewCRUD : " + texte);
    }

    @Override
    public ObjetCSA getElementFromType(String type) {
        if (typeResolver != null) {
            return typeResolver.apply(type);
        }
        if (maitreFX != null) {
            return maitreFX.getElementFromType(type);
        }
        System.err.println("*** TableViewCRUD : impossible de résoudre le type '" + type +
                "' (ni typeResolver ni maitreFX configuré)");
        return null;
    }

    @Override
    public void action_utilisateur(Object selectedItem, Object source, String extraInfo) {
        if (userActionHandler != null) {
            userActionHandler.accept(selectedItem, extraInfo);
        } else if (maitreFX != null) {
            maitreFX.action_utilisateur(selectedItem, source, extraInfo);
        } else {
            affiche_console("TableViewCRUD : action utilisateur ignorée (pas de handler) — " + selectedItem);
        }
    }

    @Override
    public void affiche_details(Object selectedItem) {
    }

    @Override
    public void nettoyer_champs() {
        charger_liste(new ArrayList<>(), elementDeReference);
    }

    @Override
    public void charger_donnees_vers_enfant(ObjetCSA objetCSA) {
    }

    @Override
    public void rafraichir() {
    }

    @Override
    public boolean isTest() {
        return maitreFX != null && maitreFX.isTest();
    }

    // -------------------------------------------------------------------------
    // Accesseurs
    // -------------------------------------------------------------------------

    /**
     * @deprecated Utiliser les handlers fonctionnels.
     */
    @Deprecated
    public MaitreFX getMaitreFX() {
        return maitreFX;
    }

    public void rafraichir_listes() {
        if (editorPaneController == null) {
            affiche_console("Le tableCRUD ne peut rafraichir les listes internes car pas d'enfant initializé !");
            return;
        }
        editorPaneController.rafraichir_listes();
    }


    public ObjetCSA getElementDeReference() {
        if (elementDeReference == null && !tableItems.isEmpty()) {
            ObjetIHM objetUniforme = tableItems.get(0);
            for (int index = 1; index < tableItems.size(); index++) {
                if (!objetUniforme.getType().equals(tableItems.get(index).getType())) {
                    return null;
                }
            }
            return objetUniforme.getObjetCSA().getNewInstance();
        }
        return elementDeReference;
    }

    public ArrayList getListe() {
        return new ArrayList<>(TransformeurObjets.transformer_en_objetsDi(tableItems));
    }

    public ArrayList<Object> getListeOriginale() {
        return listeInitiale;
    }

}
