package controlsFX;

import general.ObjetCSA;
import generalFX.ObjetIHM;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonTreeView extends TreeView<ObjetIHM> {
    private final String STRING_VIDE = "Vide !";
    /**
     * Contient la liste des parents uniquement !
     */
    Map<ObjetCSA, TreeItem<ObjetIHM>> itemMap = new HashMap<>();
    EcouteurFX ecouteurFX;
    private boolean isExpansionBusy = false;

    public MonTreeView() {
        TreeItem<ObjetIHM> root = new TreeItem<>();
        setRoot(root);

        setOnMouseClicked(event -> {
            if (!isExpansionBusy) {
                TreeItem<ObjetIHM> treeItem = getSelectionModel().getSelectedItem();
                if (treeItem != null) {
                    prevenir_ecouteur(treeItem.getValue().getObjetCSA(), this, "clic");
                }
            }
        });

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

    private TreeItem<ObjetIHM> creer_treeItem(ObjetCSA objetDi) {
        return new TreeItem<>(new ObjetIHM(objetDi));
    }

    private void prevenir_ecouteur(ObjetCSA selectedItem, MonTreeView monTreeView, String clic) {
        if (ecouteurFX == null) {
            System.err.println("\n!!!\nEcouteurFX non spécifié pour le Treeview " + this);
            return;
        }
        ecouteurFX.action_retour_component(selectedItem, monTreeView, clic);
    }

    public void setEcouteurFX(EcouteurFX ecouteurFX) {
        this.ecouteurFX = ecouteurFX;
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
                TreeItem<ObjetIHM> nouveauParent = objetDi.isParent() ? create_node(objetDi) : creer_treeItem(objetDi);
                ajout_special(getRoot().getChildren(), nouveauParent);
                itemMap.putIfAbsent(objetDi, nouveauParent);
            } else {
                descendants.add(objetDi);
            }
        }
        descendants.forEach(this::addDescendant);
    }

    /**
     * Attention : n'ajoute rien sous la racine. Il est sous-entendu que l'objet
     * à ajouter est un descendant des éléments de la racine
     *
     * @param objetDi le descendant à insérer
     */
    private void addDescendant(ObjetCSA objetDi) {
        TreeItem<ObjetIHM> treeItem = objetDi.isParent() ?
                create_node(objetDi) :
                creer_treeItem(objetDi);
        ObjetCSA leParent = objetDi.getParent();
        if (leParent == null) {
            System.err.println("VueArborescence/addDescendant : " + objetDi + " n'est pas un descendant." +
                    " Ajout ignoré !");
            return;
        }
        for (Map.Entry<ObjetCSA, TreeItem<ObjetIHM>> noeud : itemMap.entrySet()) {
            if (insertion_reussie(objetDi, noeud.getValue())) {
                return;
            }
        }
        System.err.println("VueArborescence/addDescendant : " + objetDi + " n'a pas trouvé son parent dans : " +
                itemMap + "\nAjout ignoré !");
    }

    private boolean insertion_reussie(ObjetCSA objetDi, TreeItem<ObjetIHM> treeItem) {
        ObjetCSA parent = objetDi.getParent();
        TreeItem<ObjetIHM> nouvelElement = objetDi.isParent() ? create_node(objetDi) : creer_treeItem(objetDi);
        // a-t-on déjà trouvé ?
        if (parent.equals(treeItem.getValue().getObjetCSA())) {
            ajout_special(treeItem.getChildren(), nouvelElement);
            return true;
        }
        // parcours des descendants
        for (TreeItem<ObjetIHM> enfant : treeItem.getChildren()) {
            if (insertion_reussie(objetDi, enfant)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Insère l'élément actuel dans la liste actuelle et pas dans les sous-listes !
     *
     * @param treeItems la liste actuelle
     * @param treeItem  l'item à insérer
     */
    private void ajout_special(ObservableList<TreeItem<ObjetIHM>> treeItems,
                               TreeItem<ObjetIHM> treeItem) {
        if (treeItems.size() == 1) {
            TreeItem<ObjetIHM> ancienItem = treeItems.get(0);
            if (ancienItem.getValue().toString().equals(STRING_VIDE)) {// c'est le premier elt qui arrive
                treeItems.clear();
                treeItems.add(treeItem);
                return;
            }
        }
        ObjetIHM itemIHM = treeItem.getValue();
        // vérifier qu'il n'est pas déjà présent dans la liste !
        for (TreeItem<ObjetIHM> iter : treeItems) {
            ObjetIHM objetListe = iter.getValue();
            if (objetListe.equals(itemIHM)) {
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
        ArrayList<TreeItem<ObjetIHM>> parents = new ArrayList<>();
        for (TreeItem<ObjetIHM> parent : getRoot().getChildren()) {
            parent.getChildren().clear();
            if (parent.getValue().getObjetCSA().isParent()) {
                parent.getChildren().add(creer_treeItem(creer_objet_vide()));
            }
        }
        itemMap.clear();
    }

    private TreeItem<ObjetIHM> create_node(ObjetCSA objetDi) {
        TreeItem<ObjetIHM> treeItem = creer_treeItem(objetDi);
        ObjetCSA objetVide = creer_objet_vide();
        treeItem.getChildren().add(creer_treeItem(objetVide));
        treeItem.expandedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable,
                                Boolean oldValue,
                                Boolean newValue) {
                isExpansionBusy = true;
                String string = "collapsed";
                if (newValue) {
                    string = "expanded";
                    // Your custom code for expansion
                }
                System.out.println(objetDi + " " + string);
                prevenir_ecouteur(objetDi, MonTreeView.this, string);
                Platform.runLater(() -> {
                    isExpansionBusy = false;
                });
            }
        });
        return treeItem;
    }

    private ObjetCSA creer_objet_vide() {
        return new ObjetCSA("Object", "id", "Vide !") {
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

}
