package controlsFX;

import general.GestionnaireRetour;
import general.ObjetCSA;
import generalFX.ObjetIHM;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class MonTreeView extends TreeView<ObjetIHM> implements MaitreFX {
    // Utiliser la constante centralisée
    private final String STRING_VIDE = ConstantesFX.STRING_VIDE;
    Map<ObjetCSA, TreeItem<ObjetIHM>> itemMap = new HashMap<>();
    private Class<? extends ObjetCSA> type;
    private MaitreFX maitre;
    private boolean isExpansionBusy = false;
    private TreeItem<ObjetIHM> noeudSelectionne = null;
    private ObjetCSA pendingSelection = null;
    private volatile boolean isProgrammaticChange = false;

    // ---- Handlers fonctionnels (alternative à MaitreFX) ----
    private ContexteFX contexte;
    private ContexteFX.HandlerBD bdHandler;
    private Function<String, ObjetCSA> typeResolver;
    private BiConsumer<Object, String> userActionHandler;
    private Consumer<String> messageHandler;


    /**
     * Constructeur sans argument requis pour l'instanciation depuis FXML.
     * Après instanciation, appeler {@link #setTypeClassName(String)} pour définir le type racine.
     */
    public MonTreeView() {
        this.type = null;
        TreeItem<ObjetIHM> root = new TreeItem<>();
        setRoot(root);
        setShowRoot(false);
        configureCellFactory();
        configureSelectionListener();
    }

    public MonTreeView(Class<? extends ObjetCSA> typeRacine) {
        type = Objects.requireNonNull(typeRacine,
                "Le type de MonTreeView ne peut être nul !");
        TreeItem<ObjetIHM> root = new TreeItem<>();
        setRoot(root);
        setShowRoot(false);

        configureCellFactory();
        configureSelectionListener();
    }

    /**
     * Retourne le nom complet de la classe racine, ou {@code null} si non définie.
     */
    public String getTypeClassName() {
        return type != null ? type.getName() : null;
    }

    /**
     * Définit la classe racine à partir de son nom complet.
     * Utilisé pour l'instanciation depuis FXML : {@code <MonTreeView typeClassName="com.example.MaClasse"/>}
     *
     * @param className nom complet de la classe (ex: {@code "com.example.MaCategorie"})
     */
    @SuppressWarnings("unchecked")
    public void setTypeClassName(String className) {
        try {
            this.type = (Class<? extends ObjetCSA>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MonTreeView : classe non trouvée : " + className, e);
        }
    }

    private void configureCellFactory() {
        setCellFactory(tv -> {
            TreeCellAvecBoutons cell = new TreeCellAvecBoutons();

            // Handler pour l'ajout d'une sous-catégorie
            cell.setOnAjouter(treeItem -> {
                if (treeItem != null && treeItem.getValue() != null) {
                    action_ajouter_sous_categorie(treeItem);
                }
            });

            // Handler pour le rafraîchissement
            cell.setOnRafraichir(treeItem -> {
                if (treeItem != null && treeItem.getValue() != null) {
                    action_rafraichir_sous_categories(treeItem);
                }
            });

            // Handler pour la suppression (visible seulement sur les feuilles)
            cell.setOnSupprimer(treeItem -> {
                if (treeItem != null && treeItem.getValue() != null) {
                    action_supprimer(treeItem);
                }
            });

            return cell;
        });
    }


    private void configureSelectionListener() {
        getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (!isExpansionBusy && !isProgrammaticChange && newValue != null && newValue.getValue() != null) {
                prevenir_ecouteur(newValue.getValue().getObjetCSA(), this, "clic");
            }
        });
    }

    private void action_ajouter_sous_categorie(TreeItem<ObjetIHM> parentItem) {
        noeudSelectionne = parentItem;
        ObjetCSA parent = parentItem.getValue().getObjetCSA();

        if (parent.getTypeEnfant() == null) {
            affiche_console("** Type de sous-catégorie non défini pour " + parentItem);
            affiche_utilisateur("Impossible d'ajouter une sous-catégorie");
            return;
        }

        if (bdHandler == null && maitre == null) {
            affiche_console("** MaitreFX non défini pour " + this);
            affiche_utilisateur("Impossible d'ajouter une sous-catégorie");
            return;
        }

        creer_sous_categorie(parent);
    }

    private void creer_sous_categorie(ObjetCSA parent) {
        ObjetCSA reference = getElementFromType(parent.getTypeEnfant());

        if (reference != null) {
            reference.setParent(parent);
            charger_donnees_vers_enfant(reference);
            return;
        }

        affiche_console("MonTreeView/creer_sous_categorie : Échec création instance de " +
                parent.getTypeEnfant());
        affiche_utilisateur("Impossible de créer une sous-catégorie pour " + parent);
    }

    private void action_rafraichir_sous_categories(TreeItem<ObjetIHM> parentItem) {
        ObjetCSA parent = parentItem.getValue().getObjetCSA();

        if (parent.getTypeEnfant() == null) {
            affiche_console("** Type de sous-catégorie non défini pour " + parentItem);
            affiche_utilisateur("Impossible de rafraichir les sous-catégories de " +
                    parent);
            return;
        }

        if (bdHandler == null && maitre == null) {
            affiche_console("** MaitreFX non défini pour " + this);
            return;
        }
        ObjetCSA enfant = getElementFromType(parent.getTypeEnfant());
        enfant.setParent(parent);

        affiche_console("Rafraîchissement des sous-catégories de " + parent);
        affiche_utilisateur("Rafraîchissement en cours...");

        // Code 4 = demande de liste des sous-catégories
        appeler_bd(4, enfant, new FacteurRafraichissement(parentItem));
    }

    public Map<ObjetCSA, TreeItem<ObjetIHM>> getItemMap() {
        return itemMap;
    }

    private void action_supprimer(TreeItem<ObjetIHM> item) {
        if (item == null || item.getValue() == null) return;

        ObjetIHM ihm = item.getValue();
        if (STRING_VIDE.equals(ihm.toString())) {
            affiche_utilisateur("Élément non supprimable");
            return;
        }

        ObjetCSA cible = ihm.getObjetCSA();
        if (cible == null) return;

        // Ne supprimer que les feuilles
        if (!item.isLeaf()) {
            affiche_utilisateur("Seules les feuilles peuvent être supprimées");
            return;
        }

        if (maitre == null) {
            affiche_console("** MaitreFX non défini pour la suppression");
            affiche_utilisateur("Suppression impossible");
            return;
        }

        ArrayList<ObjetCSA> selections = new ArrayList<>();
        selections.add(cible);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Suppression de l'objet : " + cible.getNom() + "\nVeuillez confirmer, svp");
        alert.setWidth(600);
        alert.setHeight(400);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Code 3 = suppression
                appeler_bd(3, cible, new GestionnaireRetour() {
                    @Override
                    public void traitementRetour(ArrayList arrayList) {
                        if (arrayList != null && !arrayList.isEmpty() && arrayList.get(0) instanceof Integer) {
                            int code = (int) arrayList.get(0);
                            if (code == 3) {
                                Platform.runLater(() -> {
                                    // Suppression locale : retirer le TreeItem et nettoyer itemMap
                                    TreeItem<ObjetIHM> parent = item.getParent();
                                    if (parent != null) {
                                        parent.getChildren().remove(item);

                                        // Si le parent n'a plus d'enfants, ajouter le placeholder si le parent peut en avoir
                                        if (parent.getChildren().isEmpty()) {
                                            ObjetIHM parentIhm = parent.getValue();
                                            if (parentIhm != null) {
                                                ObjetCSA parentCSA = parentIhm.getObjetCSA();
                                                if (parentCSA != null && parentCSA.getTypeEnfant() != null) {
                                                    parent.getChildren().add(creer_treeItem(creer_objet_vide()));
                                                }
                                            }
                                        }
                                    } else {
                                        // Si pas de parent (cas rare), retirer de la racine
                                        getRoot().getChildren().remove(item);
                                    }

                                    // Retirer toutes les entrées itemMap pointant vers ce TreeItem
                                    ArrayList<ObjetCSA> keysToRemove = new ArrayList<>();
                                    for (Map.Entry<ObjetCSA, TreeItem<ObjetIHM>> entry : itemMap.entrySet()) {
                                        if (entry.getValue() == item) {
                                            keysToRemove.add(entry.getKey());
                                        }
                                    }
                                    for (ObjetCSA k : keysToRemove) {
                                        itemMap.remove(k);
                                    }

                                    affiche_utilisateur("Suppression réussie");
                                });
                            }
                        }
                    }
                });
            } else {
                affiche_utilisateur("Suppression annulée par l'utilisateur");
            }
        });
    }

    private TreeItem<ObjetIHM> creer_treeItem(ObjetCSA objetDi) {
        return new TreeItem<>(new ObjetIHM(objetDi));
    }

    private void prevenir_ecouteur(ObjetCSA selectedItem, MonTreeView monTreeView, String info) {
        if (userActionHandler != null) {
            userActionHandler.accept(selectedItem, info);
            return;
        }
        if (maitre == null) {
            System.err.println("\n!!!\nEcouteurFX non spécifié pour le TreeView " + this);
            return;
        }
        maitre.action_utilisateur(selectedItem, monTreeView, info);
    }


    public void setMaitre(MaitreFX maitre) {
        this.maitre = maitre;
    }

    // ============ Handlers fonctionnels (ContexteFX) ============

    /**
     * Configure tous les handlers en une seule fois depuis un {@link ContexteFX}.
     *
     * @param ctx le contexte à appliquer
     */
    public void setContexte(ContexteFX ctx) {
        this.contexte = ctx;
        if (ctx == null) return;
        if (ctx.getBdHandler() != null) this.bdHandler = ctx.getBdHandler();
        if (ctx.getTypeResolver() != null) this.typeResolver = ctx.getTypeResolver();
        if (ctx.getMessageHandler() != null) this.messageHandler = ctx.getMessageHandler();
        if (ctx.getUserActionHandler() != null) this.userActionHandler = ctx.getUserActionHandler();
    }

    public ContexteFX getContexteFX() {
        return contexte;
    }

    /**
     * Configure le handler BD unifié. Codes : 1=ajout, 2=modif, 3=suppression, 4=consultation.
     */
    public void setBdHandler(ContexteFX.HandlerBD handler) {
        this.bdHandler = handler;
    }

    /**
     * Configure le résolveur de type (remplace {@code maitre.getElementFromType}).
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

    /**
     * Sélectionne un élément dans le TreeView
     *
     * @param objetCSA l'objet à sélectionner
     * @return true si la sélection a réussi, false si l'élément n'est pas trouvé (mis en attente)
     */
    public boolean select(ObjetCSA objetCSA) {
        if (objetCSA == null) {
            clearSelection();
            return true;
        }

        TreeItem<ObjetIHM> item = trouver_item_existant(objetCSA);
        if (item != null) {
            // L'élément existe, on peut le sélectionner immédiatement
            selectItem(item);
            pendingSelection = null;
            return true;
        } else {
            // L'élément n'existe pas encore, on mémorise la sélection
            pendingSelection = objetCSA;
            affiche_console("Sélection différée mémorisée pour: " + objetCSA);
            return false;
        }
    }

    /**
     * Sélectionne un élément sans déclencher d'événement
     *
     * @param objetCSA l'objet à sélectionner
     * @return true si la sélection a réussi, false si l'élément n'est pas trouvé (mis en attente)
     */
    public boolean selectSilently(ObjetCSA objetCSA) {
        if (objetCSA == null) {
            clearSelectionSilently();
            return true;
        }

        TreeItem<ObjetIHM> item = trouver_item_existant(objetCSA);
        if (item != null) {
            selectItemSilently(item);
            pendingSelection = null;
            return true;
        } else {
            pendingSelection = objetCSA;
            affiche_console("Sélection différée silencieuse mémorisée pour: " + objetCSA);
            return false;
        }
    }

    /**
     * Efface la sélection
     */
    public void clearSelection() {
        Platform.runLater(() -> {
            isProgrammaticChange = true;
            try {
                getSelectionModel().clearSelection();
                pendingSelection = null;
            } finally {
                isProgrammaticChange = false;
            }
        });
    }

    /**
     * Efface la sélection sans déclencher d'événement
     */
    public void clearSelectionSilently() {
        Platform.runLater(() -> {
            isProgrammaticChange = true;
            try {
                getSelectionModel().clearSelection();
                pendingSelection = null;
            } finally {
                isProgrammaticChange = false;
            }
        });
    }

    /**
     * Retourne l'élément actuellement sélectionné
     */
    public ObjetCSA getSelectedObject() {
        TreeItem<ObjetIHM> selectedItem = getSelectionModel().getSelectedItem();
        return (selectedItem != null && selectedItem.getValue() != null)
                ? selectedItem.getValue().getObjetCSA()
                : null;
    }

    /**
     * Retourne la sélection en attente
     */
    public ObjetCSA getPendingSelection() {
        return pendingSelection;
    }

    private void selectItem(TreeItem<ObjetIHM> item) {
        Platform.runLater(() -> {
            isProgrammaticChange = true;
            try {
                getSelectionModel().select(item);
                // Développer les parents pour rendre l'élément visible
                expandParents(item);
            } finally {
                isProgrammaticChange = false;
            }
            // Déclencher l'événement après avoir réinitialisé le flag
            if (item != null && item.getValue() != null) {
                if (userActionHandler != null) {
                    userActionHandler.accept(item.getValue().getObjetCSA(), "sélection programmatique");
                } else if (maitre != null) {
                    maitre.action_utilisateur(item.getValue().getObjetCSA(), this, "sélection programmatique");
                }
            }
        });
    }

    private void selectItemSilently(TreeItem<ObjetIHM> item) {
        Platform.runLater(() -> {
            isProgrammaticChange = true;
            try {
                getSelectionModel().select(item);
                expandParents(item);
            } finally {
                isProgrammaticChange = false;
            }
        });
    }

    private void expandParents(TreeItem<ObjetIHM> item) {
        TreeItem<ObjetIHM> parent = item.getParent();
        while (parent != null && parent != getRoot()) {
            parent.setExpanded(true);
            parent = parent.getParent();
        }
    }


    public void setData(List<? extends ObjetCSA> objetDis) {
        getRoot().getChildren().clear();
        itemMap.clear();
        addData(objetDis);
        // Tenter d'appliquer la sélection en attente après le remplissage
        applyPendingSelection();
    }

    public void addDataList(List<? extends ObjetCSA> liste) {
        ArrayList<ObjetCSA> descendants = new ArrayList<>();
        for (ObjetCSA objetDi : liste) {
            ObjetCSA parent = objetDi.getParent();
            if (parent == null || parent.getId() == 0) {
                TreeItem<ObjetIHM> nouveauParent = create_node(objetDi);
                ajout_special(getRoot().getChildren(), nouveauParent);
                itemMap.putIfAbsent(objetDi, nouveauParent);
            } else {
                descendants.add(objetDi);
            }
        }
        descendants.forEach(this::addDescendant);
        // Tenter d'appliquer la sélection en attente après l'ajout
        applyPendingSelection();
    }

    private void addDescendant(ObjetCSA objetDi) {
        TreeItem<ObjetIHM> treeItem = create_node(objetDi);
        ObjetCSA leParent = objetDi.getParent();

        if (leParent == null) {
            System.err.println("MonTreeView/addDescendant : " + objetDi +
                    " n'a pas de parent. Ajout ignoré !");
            return;
        }

        for (Map.Entry<ObjetCSA, TreeItem<ObjetIHM>> noeud : itemMap.entrySet()) {
            if (insertion_reussie(objetDi, noeud.getValue())) {
                return;
            }
        }

        System.err.println("MonTreeView/addDescendant : " + objetDi +
                " n'a pas trouvé son parent. Ajout ignoré !");
    }

    private boolean insertion_reussie(ObjetCSA objetDi, TreeItem<ObjetIHM> treeItem) {
        ObjetCSA parent = objetDi.getParent();
        TreeItem<ObjetIHM> nouvelElement = create_node(objetDi);

        if (parent.equals(treeItem.getValue().getObjetCSA())) {
            ajout_special(treeItem.getChildren(), nouvelElement);
            return true;
        }

        for (TreeItem<ObjetIHM> enfant : treeItem.getChildren()) {
            if (insertion_reussie(objetDi, enfant)) {
                return true;
            }
        }
        return false;
    }

    private void ajout_special(ObservableList<TreeItem<ObjetIHM>> treeItems,
                               TreeItem<ObjetIHM> treeItem) {
        if (treeItems.size() == 1) {
            TreeItem<ObjetIHM> ancienItem = treeItems.get(0);
            if (ancienItem.getValue().toString().equals(STRING_VIDE)) {
                treeItems.clear();
                treeItems.add(treeItem);
                return;
            }
        }

        ObjetIHM itemIHM = treeItem.getValue();
        for (TreeItem<ObjetIHM> iter : treeItems) {
            if (iter.getValue().equals(itemIHM)) {
                return;
            }
        }
        treeItems.add(treeItem);
    }

    public void clear() {
        getRoot().getChildren().clear();
        itemMap.clear();
        pendingSelection = null;
    }

    public void clear_children() {
        for (TreeItem<ObjetIHM> parent : getRoot().getChildren()) {
            parent.getChildren().clear();
            parent.getChildren().add(creer_treeItem(creer_objet_vide()));
        }
        itemMap.clear();
        pendingSelection = null;
    }

    private TreeItem<ObjetIHM> create_node(ObjetCSA objetDi) {
        TreeItem<ObjetIHM> treeItem = creer_treeItem(objetDi);

        // N'ajouter un placeholder QUE si l'objet peut avoir des enfants
        if (objetDi.getTypeEnfant() != null) {
            treeItem.getChildren().add(creer_treeItem(creer_objet_vide()));

            treeItem.expandedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable,
                                    Boolean oldValue, Boolean newValue) {
                    isExpansionBusy = true;
                    String action = newValue ? "expanded" : "collapsed";
                    System.out.println(objetDi + " " + action);
                    prevenir_ecouteur(objetDi, MonTreeView.this, action);
                    Platform.runLater(() -> isExpansionBusy = false);
                }
            });
        }
        // Sinon, c'est une feuille → pas d'enfants, pas de listener, pas expandable

        return treeItem;
    }

    private ObjetCSA creer_objet_vide() {
        return new ObjetCSA("Object", "id", STRING_VIDE) {
            @Override
            public ObjetCSA getParent() {
                return null;
            }

            @Override
            public void setParent(ObjetCSA objetDi) {
            }

            @Override
            public ObjetCSA getObjetFromType(String s) {
                return null;
            }

            @Override
            public void reinitialiser() {
            }

            @Override
            public String toString() {
                return STRING_VIDE;
            }
        };
    }

    // ============ Implémentation MaitreFX ============

    @Override
    public void appeler_bd(int code, Object objet, GestionnaireRetour gestionnaire) {
        if (bdHandler != null && objet instanceof ObjetCSA objetCSA) {
            bdHandler.execute(code, objetCSA, liste -> {
                if (gestionnaire != null) {
                    gestionnaire.traitementRetour(new ArrayList<>(liste));
                    return;
                }
                 /*la liste retournée est pour moi. Si le code vaut 1, alors l'élément en position 2 de la liste
                 est un integer qui contient l'id de objetCSA. Après lui avoir appliqué setId, il faut l'insérer
                 dans l'arbre à la place de l'instance temporaire qui a été créée pour l'envoi.
                 C'est le seul cas où je dois traiter la liste moi-même.*/
                if (code == 1 && liste != null && liste.size() > 2 && liste.get(2) instanceof Integer) {
                    int newId = (Integer) liste.get(2);
                    objetCSA.setId(newId);
                    ArrayList<ObjetCSA> monoList = new ArrayList<>();
                    monoList.add(objetCSA);
                    addData(monoList);
                    return;
                }
                affiche_console("MonTreeView : Réponse BD reçue ET IGNORÉE pour code " + code + " avec liste = " + liste);
            });
            return;
        }
        if (maitre != null) {
            maitre.appeler_bd(code, objet, new FacteurTreeView(objet, gestionnaire));
            return;
        }
        affiche_console("** Attention : MonTreeView ne fait pas suivre l'info car il n'a pas de parent !\n" +
                "L'info est : code = " + code + ", avec pour objet : " + objet);
    }

    @Override
    public void affiche_utilisateur(String texte) {
        if (messageHandler != null) {
            messageHandler.accept(texte);
        } else if (maitre != null) {
            maitre.affiche_utilisateur(texte);
        }
    }

    @Override
    public void affiche_console(String texte) {
        if (maitre != null) {
            maitre.affiche_console("MonTreeView : " + texte);
        } else {
            System.out.println("* MonTreeView : " + texte);
        }
    }

    @Override
    public ObjetCSA getElementFromType(String type) {
        if (typeResolver != null) {
            return typeResolver.apply(type);
        }
        if (maitre != null) {
            return maitre.getElementFromType(type);
        }
        return null;
    }

    @Override
    public void action_utilisateur(Object selectedItem, Object source, String extraInfo) {
        if (userActionHandler != null) {
            userActionHandler.accept(selectedItem, extraInfo);
        } else if (maitre != null) {
            maitre.action_utilisateur(selectedItem, source, extraInfo);
        }
    }

    @Override
    public void affiche_details(Object selectedItem) {
        if (maitre != null) {
            maitre.affiche_details(selectedItem);
        }
    }

    @Override
    public void nettoyer_champs() {
        clear();
    }

    @Override
    public void charger_donnees_vers_enfant(ObjetCSA objetCSA) {
        affiche_console("Ouverture d'une EditorPane pour éditer : " + objetCSA);
        if (maitre instanceof EditorPaneController) {
            String heritedPath = ((EditorPaneController) maitre).getEditorPath();
            EditorPaneController.openEditor(objetCSA,
                    this,
                    null,
                    heritedPath);
            return;
        }
        EditorPaneController.openEditor(objetCSA, this, null, "");
    }

    @Override
    public void rafraichir() {
        if (bdHandler == null && maitre == null) {
            affiche_utilisateur("rafraîchissement impossible pour cet élément");
            affiche_console("Aucun accès à la bd pour cet objet. Il faut initialiser " +
                    "son maitreFX ou bdHandler !");
            return;
        }
        if (type == null) {
            affiche_console("Rafraîchissement impossible : typeClassName non défini. " +
                    "Appeler setTypeClassName(...) avant rafraichir().");
            affiche_utilisateur("Type non configuré, rafraîchissement impossible");
            return;
        }
        // Rafraîchir la vue complète
        try {
            ObjetCSA instance = type.getDeclaredConstructor().newInstance();
            GestionnaireRetour gestionnaire = new GestionnaireRetour() {
                @Override
                public void traitementRetour(ArrayList arrayList) {
                    Platform.runLater(() -> {
                        List<ObjetCSA> categories = new ArrayList<>();
                        for (Object obj : arrayList) {
                            if (obj instanceof ObjetCSA) {
                                categories.add((ObjetCSA) obj);
                            }
                        }
                        setData(categories);
                    });
                }
            };
            if (bdHandler != null) {
                bdHandler.execute(4, instance, liste -> gestionnaire.traitementRetour(new ArrayList<>(liste)));
            } else {
                maitre.appeler_bd(4, instance, gestionnaire);
            }
        } catch (Exception e) {
            affiche_console("Erreur lors du rafraîchissement : " + e.getMessage());
        }
    }

    @Override
    public boolean isTest() {
        return maitre != null && maitre.isTest();
    }

    /**
     * Applique la sélection en attente si l'élément est maintenant disponible
     */
    private void applyPendingSelection() {
        if (pendingSelection != null) {
            TreeItem<ObjetIHM> item = trouver_item_existant(pendingSelection);
            if (item != null) {
                affiche_console("Application de la sélection différée pour: " + pendingSelection);
                selectItemSilently(item);
                pendingSelection = null;
            }
        }
    }

    /**
     * Ajoute ou met à jour des données dans le TreeView
     * Si un objet existe déjà (même ID et type), il est écrasé
     * Sinon, il est ajouté
     *
     * @param objetDis liste des objets à ajouter/mettre à jour
     */
    public void addData(List<? extends ObjetCSA> objetDis) {
        ArrayList<ObjetCSA> descendants = new ArrayList<>();

        for (ObjetCSA objetDi : objetDis) {
            ObjetCSA parent = objetDi.getParent();

            // Vérifier si l'objet existe déjà
            TreeItem<ObjetIHM> existingItem = trouver_item_existant(objetDi);

            if (parent == null || parent.getId() == 0) {
                // C'est un élément racine
                if (existingItem != null) {
                    // Mettre à jour l'objet existant
                    existingItem.getValue().setObjetCSA(objetDi);
                    itemMap.put(objetDi, existingItem);
                } else {
                    // Ajouter comme nouveau
                    TreeItem<ObjetIHM> nouveauParent = create_node(objetDi);
                    ajout_special(getRoot().getChildren(), nouveauParent);
                    itemMap.put(objetDi, nouveauParent);
                }
            } else {
                // C'est un descendant, traiter après
                descendants.add(objetDi);
            }
        }

        // Traiter les descendants
        descendants.forEach(this::addOrUpdateDescendant);
        // Tenter d'appliquer la sélection en attente après l'ajout
        applyPendingSelection();
    }

    /**
     * Trouve un TreeItem existant correspondant à l'objetCSA
     * La comparaison se fait sur l'ID et le type
     */
    private TreeItem<ObjetIHM> trouver_item_existant(ObjetCSA objetDi) {
        for (Map.Entry<ObjetCSA, TreeItem<ObjetIHM>> entry : itemMap.entrySet()) {
            ObjetCSA existing = entry.getKey();

            // Comparaison par ID et type
            if (existing.getId() == objetDi.getId() &&
                    existing.getType().equals(objetDi.getType())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Ajoute ou met à jour un descendant dans l'arbre
     */
    private void addOrUpdateDescendant(ObjetCSA objetDi) {
        ObjetCSA leParent = objetDi.getParent();

        if (leParent == null) {
            System.err.println("MonTreeView/addOrUpdateDescendant : " + objetDi +
                    " n'a pas de parent. Ajout ignoré !");
            return;
        }

        // Vérifier si l'objet existe déjà
        TreeItem<ObjetIHM> existingItem = trouver_item_existant(objetDi);

        if (existingItem != null) {
            // Mettre à jour l'objet existant
            existingItem.getValue().setObjetCSA(objetDi);
            itemMap.put(objetDi, existingItem);
            return;
        }

        // Sinon, ajouter comme nouveau
        TreeItem<ObjetIHM> treeItem = create_node(objetDi);

        for (Map.Entry<ObjetCSA, TreeItem<ObjetIHM>> noeud : itemMap.entrySet()) {
            if (insertion_reussie(objetDi, noeud.getValue())) {
                return;
            }
        }

        System.err.println("MonTreeView/addOrUpdateDescendant : " + objetDi +
                " n'a pas trouvé son parent. Ajout ignoré !");
    }

    private class FacteurTreeView implements GestionnaireRetour {
        private final ObjetCSA objetCSA;
        private final GestionnaireRetour autreGestionnaire;


        public FacteurTreeView(Object objet, GestionnaireRetour gestionnaire) {
            autreGestionnaire = gestionnaire;
            if (objet instanceof ObjetCSA) {
                objetCSA = (ObjetCSA) objet;
            } else {
                objetCSA = null;
            }
        }

        @Override
        public void traitementRetour(ArrayList arrayList) {
            if (arrayList == null || arrayList.size() < 3) {
                affiche_console("Liste vide reçue pour rafraîchissement devrait contenir 3 elts mais est = " + arrayList);
                return;
            }

            if (arrayList.get(0) instanceof Integer code &&
                    arrayList.get(2) instanceof ArrayList<?> listeRecue) {
                String nomTable = String.valueOf(arrayList.get(1));
                if (code == 1) {
                    action_rafraichir_sous_categories(noeudSelectionne);
                }
                affiche_console("*** Erreur dans MonTreeView : Je n'attends que le code 1, mais je reçois " + code +
                        "\nRequête ignorée pour " + nomTable);
                return;
            }
            affiche_console("Paquet reçu mal formaté. J'attends (code,nomTable, liste), mais j'obtiens : " + arrayList);
        }
    }

    private class FacteurRafraichissement implements GestionnaireRetour {
        private final TreeItem<ObjetIHM> parentItem;

        public FacteurRafraichissement(TreeItem<ObjetIHM> parentItem) {
            this.parentItem = parentItem;
        }


        @Override
        public void traitementRetour(ArrayList arrayList) {
            if (arrayList == null || arrayList.size() < 3) {
                affiche_console("Liste vide reçue pour rafraîchissement devrait contenir 3 elts mais est = " + arrayList);
                Platform.runLater(() -> {
                    parentItem.getChildren().clear();
                    parentItem.getChildren().add(creer_treeItem(creer_objet_vide()));
                    affiche_utilisateur("Aucune sous-catégorie");
                });
                return;
            }

            if (arrayList.get(0) instanceof Integer code &&
                    arrayList.get(2) instanceof ArrayList<?> listeRecue) {
                String nomTable = String.valueOf(arrayList.get(1));
                Platform.runLater(() -> {
                    // Supprimer le placeholder "Vide !"
                    parentItem.getChildren().removeIf(item ->
                            item.getValue().toString().equals(ConstantesFX.STRING_VIDE));

                    // Vider les enfants existants
                    parentItem.getChildren().clear();

                    // Ajouter les nouvelles sous-catégories
                    for (Object obj : listeRecue) {
                        if (obj instanceof ObjetCSA) {
                            ObjetCSA categorie = (ObjetCSA) obj;
                            TreeItem<ObjetIHM> nouveauItem = create_node(categorie);
                            parentItem.getChildren().add(nouveauItem);
                            itemMap.put(categorie, nouveauItem);
                        }
                    }

                    // Si toujours vide, remettre le placeholder
                    if (parentItem.getChildren().isEmpty()) {
                        parentItem.getChildren().add(creer_treeItem(creer_objet_vide()));
                    }

                    affiche_utilisateur("Sous-catégories rafraîchies");
                });
                return;
            }
            affiche_console("Paquet reçu mal formaté. J'attends (code,nomTable, liste), mais j'obtiens : " + arrayList);
        }
    }
}

