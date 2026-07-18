package controlsFX;

import general.GestionnaireRetour;
import general.ObjetCSA;
import generalFX.ObjetIHM;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class MonTreeViewLight extends TreeView<ObjetIHM> implements MaitreFX {
    private final String STRING_VIDE = ConstantesFX.STRING_VIDE;

    Map<ObjetCSA, TreeItem<ObjetIHM>> itemMap = new HashMap<>();
    private MaitreFX maitre;
    private boolean isExpansionBusy = false;
    private Class<ObjetCSA> type;

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
    public MonTreeViewLight() {
        this.type = null;
        TreeItem<ObjetIHM> root = new TreeItem<>();
        setRoot(root);
        setShowRoot(false);
        configureCellFactory();
        configureSelectionListener();
        configureContextMenu();
    }

    public MonTreeViewLight(Class<ObjetCSA> typeRacine) {
        type = Objects.requireNonNull(typeRacine,
                "Le type de MonTreeViewLight ne peut être nul !");
        TreeItem<ObjetIHM> root = new TreeItem<>();
        setRoot(root);
        setShowRoot(false);

        configureCellFactory();
        configureSelectionListener();
        configureContextMenu();
    }

    /**
     * Retourne le nom complet de la classe racine, ou {@code null} si non définie.
     */
    public String getTypeClassName() {
        return type != null ? type.getName() : null;
    }

    /**
     * Définit la classe racine à partir de son nom complet.
     * Utilisé pour l'instanciation depuis FXML : {@code <MonTreeViewLight typeClassName="com.example.MaClasse"/>}
     *
     * @param className nom complet de la classe (ex: {@code "com.example.MaCategorie"})
     */
    @SuppressWarnings("unchecked")
    public void setTypeClassName(String className) {
        try {
            this.type = (Class<ObjetCSA>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MonTreeViewLight : classe non trouvée : " + className, e);
        }
    }

    private void configureCellFactory() {
        setCellFactory(tv -> new TreeCell<ObjetIHM>() {
            @Override
            protected void updateItem(ObjetIHM item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.toString());
                    setStyle(getTreeItem().isLeaf() ?
                            "-fx-font-weight: normal;" :
                            "-fx-font-weight: bold;");
                }
            }
        });
    }

    private void configureSelectionListener() {
        setOnMouseClicked(event -> {
            if (!isExpansionBusy) {
                TreeItem<ObjetIHM> treeItem = getSelectionModel().getSelectedItem();
                if (treeItem != null) {
                    prevenir_ecouteur(treeItem.getValue().getObjetCSA(), this, "clic");
                }
            }
        });
    }

    private void configureContextMenu() {
        setOnContextMenuRequested(event -> {
            TreeItem<ObjetIHM> selectedItem = getSelectionModel().getSelectedItem();

            if (selectedItem != null && !selectedItem.getValue().toString().equals(STRING_VIDE)) {
                ObjetCSA objet = selectedItem.getValue().getObjetCSA();

                ContextMenu contextMenu = new ContextMenu();

                // Menu pour les objets pouvant avoir des enfants
                if (objet.getTypeEnfant() != null) {
                    MenuItem ajouterItem = new MenuItem("Ajouter un enfant");
                    ajouterItem.setGraphic(createIcon("/images/add.png"));
                    ajouterItem.setOnAction(e -> action_ajouter_enfant(selectedItem));

                    MenuItem rafraichirItem = new MenuItem("Rafraîchir les enfants");
                    rafraichirItem.setGraphic(createIcon("/images/refresh.png"));
                    rafraichirItem.setOnAction(e -> action_rafraichir_enfants(selectedItem));

                    contextMenu.getItems().addAll(ajouterItem, rafraichirItem, new SeparatorMenuItem());
                }

                // Menu commun
                MenuItem editerItem = new MenuItem("Éditer");
                editerItem.setOnAction(e -> charger_donnees_vers_enfant(objet));

                MenuItem supprimerItem = new MenuItem("Supprimer");
                supprimerItem.setGraphic(createIcon("/images/delete.png"));
                supprimerItem.setOnAction(e -> confirmer_suppression(objet));

                contextMenu.getItems().addAll(editerItem, supprimerItem);

                contextMenu.show(this, event.getScreenX(), event.getScreenY());
            }
        });
    }

    public Map<ObjetCSA, TreeItem<ObjetIHM>> getItemMap() {
        return itemMap;
    }

    private void action_ajouter_enfant(TreeItem<ObjetIHM> parentItem) {
        ObjetCSA parent = parentItem.getValue().getObjetCSA();

        String typeEnfant = parent.getTypeEnfant();
        if (typeEnfant == null) {
            affiche_console("** Type d'enfant non défini pour " + parent);
            affiche_utilisateur("Cet élément ne peut pas avoir d'enfants");
            return;
        }

        if (bdHandler == null && maitre == null) {
            affiche_console("** MaitreFX non défini pour " + this);
            affiche_utilisateur("Impossible d'ajouter un enfant");
            return;
        }

        ObjetCSA reference = getElementFromType(typeEnfant);

        if (reference != null) {
            reference.setParent(parent);
            charger_donnees_vers_enfant(reference);
            return;
        }

        affiche_console("MonTreeViewLight/action_ajouter_enfant : Échec création instance de " + typeEnfant);
        affiche_utilisateur("Impossible de créer un enfant");
    }

    private void action_rafraichir_enfants(TreeItem<ObjetIHM> parentItem) {
        ObjetCSA parent = parentItem.getValue().getObjetCSA();

        if (parent.getTypeEnfant() == null) {
            affiche_console("** Type d'enfant non défini pour " + parent);
            return;
        }

        if (bdHandler == null && maitre == null) {
            affiche_console("** MaitreFX non défini pour " + this);
            return;
        }

        affiche_console("Rafraîchissement des enfants de " + parent);
        affiche_utilisateur("Rafraîchissement en cours...");

        // Code 4 = demande de liste des enfants
        appeler_bd(4, parent, new RafraichisseurEnfants(parentItem));
    }

    private ImageView createIcon(String path) {
        try {
            ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream(path)));
            imageView.setFitHeight(16);
            imageView.setFitWidth(16);
            imageView.setPreserveRatio(true);
            return imageView;
        } catch (Exception e) {
            return new ImageView();
        }
    }

    private void confirmer_suppression(ObjetCSA objet) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer " + objet.getPublicName() + " ?");
        alert.setContentText("Cette action est irréversible.");
        generalFX.StylesheetsFX.apply(alert, this);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Code 3 = Suppression
                appeler_bd(3, objet, new GestionnaireRetour() {
                    @Override
                    public void traitementRetour(ArrayList arrayList) {
                        if (!arrayList.isEmpty() && arrayList.get(0) instanceof Integer) {
                            int code = (int) arrayList.get(0);
                            if (code == 3) {
                                affiche_utilisateur("Suppression réussie");
                                rafraichir();
                            }
                        }
                    }
                });
            }
        });
    }

    private TreeItem<ObjetIHM> creer_treeItem(ObjetCSA objetDi) {
        return new TreeItem<>(new ObjetIHM(objetDi));
    }

    private void prevenir_ecouteur(ObjetCSA selectedItem, MonTreeViewLight monTreeView, String info) {
        if (userActionHandler != null) {
            userActionHandler.accept(selectedItem, info);
            return;
        }
        if (maitre == null) {
            System.err.println("\n!!!\nMaitreFX non spécifié pour le TreeView " + this);
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

    public void setData(List<? extends ObjetCSA> objetDis) {
        getRoot().getChildren().clear();
        itemMap.clear();
        addDataList(objetDis);
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
    }

    private void addDescendant(ObjetCSA objetDi) {
        TreeItem<ObjetIHM> treeItem = create_node(objetDi);
        ObjetCSA leParent = objetDi.getParent();

        if (leParent == null) {
            System.err.println("MonTreeViewLight/addDescendant : " + objetDi +
                    " n'a pas de parent. Ajout ignoré !");
            return;
        }

        for (Map.Entry<ObjetCSA, TreeItem<ObjetIHM>> noeud : itemMap.entrySet()) {
            if (insertion_reussie(objetDi, noeud.getValue())) {
                return;
            }
        }

        System.err.println("MonTreeViewLight/addDescendant : " + objetDi +
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
    }

    public void clear_children() {
        for (TreeItem<ObjetIHM> parent : getRoot().getChildren()) {
            parent.getChildren().clear();
            parent.getChildren().add(creer_treeItem(creer_objet_vide()));
        }
        itemMap.clear();
    }

    private TreeItem<ObjetIHM> create_node(ObjetCSA objetDi) {
        TreeItem<ObjetIHM> treeItem = creer_treeItem(objetDi);
        treeItem.getChildren().add(creer_treeItem(creer_objet_vide()));

        treeItem.expandedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable,
                                Boolean oldValue, Boolean newValue) {
                isExpansionBusy = true;
                String action = newValue ? "expanded" : "collapsed";
                System.out.println(objetDi + " " + action);
                prevenir_ecouteur(objetDi, MonTreeViewLight.this, action);
                Platform.runLater(() -> isExpansionBusy = false);
            }
        });

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
            bdHandler.execute(code, objetCSA, liste -> gestionnaire.traitementRetour(new ArrayList<>(liste)));
            return;
        }
        if (maitre != null) {
            maitre.appeler_bd(code, objet, gestionnaire);
            return;
        }
        affiche_console("** Attention : MonTreeViewLight n'a pas de parent ni de bdHandler pour code=" + code);
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
            maitre.affiche_console("MonTreeViewLight : " + texte);
        } else {
            System.out.println("* MonTreeViewLight : " + texte);
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
        if (maitre instanceof EditorPaneController) {
            String heritedPath = ((EditorPaneController) maitre).getEditorPath();
            EditorPaneController.openEditor(objetCSA, this, null,
                    heritedPath.isEmpty() ? objetCSA.getType() : heritedPath + "." + objetCSA.getType());
        } else {
            EditorPaneController.openEditor(objetCSA, this, null, objetCSA.getType());
        }
    }

    @Override
    public void rafraichir() {
        if (bdHandler == null && maitre == null) {
            affiche_utilisateur("Rafraîchissement impossible pour cet élément");
            affiche_console("Aucun accès à la bd pour cet objet. Il faut initialiser son maitreFX ou bdHandler !");
            return;
        }

        if (type == null) {
            affiche_console("Rafraîchissement impossible : typeClassName non défini. " +
                    "Appeler setTypeClassName(...) avant rafraichir().");
            affiche_utilisateur("Type non configuré, rafraîchissement impossible");
            return;
        }

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

    // ============ Classe interne pour gérer le rafraîchissement ============

    private class RafraichisseurEnfants implements GestionnaireRetour {
        private final TreeItem<ObjetIHM> parentItem;

        public RafraichisseurEnfants(TreeItem<ObjetIHM> parentItem) {
            this.parentItem = parentItem;
        }

        @Override
        public void traitementRetour(ArrayList arrayList) {
            if (arrayList == null || arrayList.isEmpty()) {
                affiche_console("Liste vide reçue pour rafraîchissement");
                Platform.runLater(() -> {
                    parentItem.getChildren().clear();
                    parentItem.getChildren().add(creer_treeItem(creer_objet_vide()));
                    affiche_utilisateur("Aucun enfant");
                });
                return;
            }

            Platform.runLater(() -> {
                parentItem.getChildren().removeIf(item ->
                        item.getValue().toString().equals(STRING_VIDE));

                parentItem.getChildren().clear();

                for (Object obj : arrayList) {
                    if (obj instanceof ObjetCSA) {
                        ObjetCSA enfant = (ObjetCSA) obj;
                        TreeItem<ObjetIHM> nouveauItem = create_node(enfant);
                        parentItem.getChildren().add(nouveauItem);
                        itemMap.put(enfant, nouveauItem);
                    }
                }

                if (parentItem.getChildren().isEmpty()) {
                    parentItem.getChildren().add(creer_treeItem(creer_objet_vide()));
                }

                affiche_utilisateur("Enfants rafraîchis");
            });
        }
    }
}

