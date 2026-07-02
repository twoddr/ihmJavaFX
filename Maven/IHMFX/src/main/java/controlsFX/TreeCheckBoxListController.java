package controlsFX;

import general.ObjetCSA;
import generalFX.ObjetIHM;
import generalFX.TransformeurObjets;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class TreeCheckBoxListController extends BorderPane {

    private final ArrayList<TreeItem<ObjetIHM>> arborescence = new ArrayList<>();
    @FXML
    private TreeView<ObjetIHM> treeView;
    private EcouteurFX ecouteur;

    public TreeCheckBoxListController() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/TreeCheckBoxList.fxml"));

        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
            initialiser();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void initialiser() {
        TreeItem root = new TreeItem(new ObjetCSA("ElementBD", "id", "nom") {
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
                setNom("Racine");
            }

            @Override
            public String toString() {
                return "Racine";
            }
        });
        root.setExpanded(true);
        treeView.setRoot(root);
        treeView.setShowRoot(false);
        treeView.setCellFactory(p -> {
            return new TreeCell() {
                private final Label label;
                private final CheckBox checkBox;

                {
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    label = new Label();
                    checkBox = new CheckBox();
                }

                protected void updateItem(ObjetIHM item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item == null || empty) {
                        setGraphic(null);
                        setBorder(new Border(new BorderStroke(Color.WHITE,
                                BorderStrokeStyle.SOLID,
                                new CornerRadii(4),
                                new BorderWidths(2))));
                    } else {
                        if (item.getObjetCSA().isParent()) {
                            label.setText(item.toString());
                            label.setTextFill(Color.BLUE);

                            setBorder(new Border(new BorderStroke(Color.LIGHTGRAY,
                                    BorderStrokeStyle.SOLID,
                                    new CornerRadii(4),
                                    new BorderWidths(2))));

                            setGraphic(label);
                            label.setOnMousePressed(mouseEvent -> {
                                action_clic_noeud(item.getObjetCSA());
                            });
                        } else {
                            checkBox.setText(item.toString());
                            setStyle(getTreeItem().isLeaf() ?
                                    "-fx-font-weight: normal;" :
                                    "-fx-font-weight: bold;");
                            setGraphic(checkBox);
                        }
                    }
                }
            };
        });
    }

    public void update_ui() {
        Platform.runLater(() -> {
            treeView.refresh();
        });
    }

    private ArrayList<TreeItem<ObjetIHM>> trouver_enfants(ArrayList<ObjetIHM> objetIHMS, int idParent) {
        ArrayList<TreeItem<ObjetIHM>> sortie = new ArrayList<>();
        ArrayList<ObjetIHM> aRetirer = new ArrayList<>();
        for (ObjetIHM objetDiIHM : objetIHMS) {
            if (objetDiIHM.getObjetCSA().getIdParent() == idParent) {
                TreeItem noeud = new TreeItem(objetDiIHM);
                sortie.add(noeud);
                aRetirer.add(objetDiIHM);
            }
        }
        // réduction de la liste de départ !
        objetIHMS.removeAll(aRetirer);

        return sortie;
    }

    public void setEcouteurFX(EcouteurFX ecouteurFX) {
        ecouteur = ecouteurFX;
    }

    public void clear() {
        treeView.getRoot().getChildren().clear();
    }

    public void setListe(ArrayList<? extends ObjetCSA> liste) {
        ArrayList<ObjetIHM> objetIHMS = TransformeurObjets.transformer_en_objetsIHM(liste);
        ArrayList<TreeItem<ObjetIHM>> arbre = creer_arborescence(objetIHMS, ObjetCSA.creer_objet_vide());
        Platform.runLater(() -> {
            treeView.getRoot().getChildren().setAll(arbre);
        });
    }

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
     * Rajoute la liste reçue dans la liste déjà affichée sous un noeud
     *
     * @param objetDis la liste des nouveaux
     * @param objetDi  le parent qui devrait se trouver dans le treeView
     */
    public void ajouter_liste(ArrayList<? extends ObjetCSA> objetDis, ObjetCSA objetDi) {
        ArrayList<ObjetIHM> objetIHMS = TransformeurObjets.transformer_en_objetsIHM(objetDis);
        if (objetDi == null) {
            rafraichir_liste(treeView.getRoot(), creer_arborescence(objetIHMS, ObjetCSA.creer_objet_vide()));
            return;
        }
        Platform.runLater(() -> {
            TreeItem branche = chercher_dans_arborescence(treeView.getRoot().getChildren(), objetDi);
            if (branche != null) {
                objetIHMS.forEach(objetDiIHM -> {
                    objetDiIHM.getObjetCSA().setParent(objetDi);
                });
                rafraichir_liste(branche, creer_arborescence(objetIHMS, objetDi));
                return;
            }
            System.err.println("TreeViewController/ajouter_liste : la liste de type " + objetDi.getType() + " n'a pas trouvé de " +
                    "parent d'id = " + objetDi.getId());
        });
    }

    private synchronized void rafraichir_liste(TreeItem<ObjetIHM> treeItem, ArrayList<TreeItem<ObjetIHM>> items) {
        ArrayList<TreeItem<ObjetIHM>> nouveaux = new ArrayList<>();
        ArrayList<TreeItem<ObjetIHM>> anciens = new ArrayList<>(treeItem.getChildren());
        ArrayList<TreeItem<ObjetIHM>> anciensRescapes = new ArrayList<>(anciens);
        for (TreeItem<ObjetIHM> iterTreeNew : items) {
            ObjetIHM objetIHMNew = iterTreeNew.getValue();
            boolean continuer = true;
            Iterator iter = anciens.iterator();
            while (iter.hasNext() && continuer) {
                TreeItem<ObjetIHM> treeItemOld = (TreeItem) iter.next();
                ObjetIHM objetIHMOld = treeItemOld.getValue();
                if (objetIHMOld.equals(objetIHMNew)) {
                    nouveaux.add(treeItemOld);
                    anciensRescapes.remove(treeItemOld);
                    rafraichir_liste(treeItemOld, new ArrayList<>(iterTreeNew.getChildren()));
                    continuer = false;
                }
            }
            // pas trouvé donc c'est un nouveau à rajouter
            if (continuer) {
                nouveaux.add(iterTreeNew);
            }
        }

        ArrayList<TreeItem<ObjetIHM>> list = new ArrayList<>(anciensRescapes);
        list.addAll(nouveaux);
        treeItem.getChildren().setAll(list);
        update_ui();
    }


    /**
     * Remplace la liste affichée par la liste reçue sous un noeud
     *
     * @param objetDis la liste des nouveaux
     * @param parent   le parent qui devrait se trouver dans le treeView
     */
    public void remplacer_liste(ArrayList<? extends ObjetCSA> objetDis, ObjetCSA parent) {
        ArrayList<ObjetIHM> objetIHMS = TransformeurObjets.transformer_en_objetsIHM(objetDis);
        if (parent == null) {
            treeView.getRoot().getChildren().setAll(creer_arborescence(objetIHMS, ObjetCSA.creer_objet_vide()));
            return;
        }
        int idParent = parent.getId();
        TreeItem branche = chercher_dans_arborescence(treeView.getRoot().getChildren(), parent);
        if (branche != null) {
            branche.getChildren().setAll(creer_arborescence(objetIHMS, parent));
            return;
        }
        System.err.println("TreeViewController/ajouter_liste : la liste de type " + parent.getType() + " n'a pas trouvé de " +
                "parent d'id = " + idParent);
    }

    private ArrayList<TreeItem<ObjetIHM>> creer_arborescence(ArrayList<ObjetIHM> objetIHMS, ObjetCSA parent) {
        ArrayList<ObjetIHM> reducedList = new ArrayList<>(objetIHMS);
        // trouver la liste des enfants
        ArrayList<TreeItem<ObjetIHM>> enfants = trouver_enfants(reducedList, parent.getId());
        // y intégrer la liste des petits-enfants
        for (TreeItem<ObjetIHM> enfant : enfants) {
            if (enfant.getValue() != null) {
                ObjetIHM objetIHM = enfant.getValue();
                // la liste de départ a été réduite dans trouver_enfants
                ArrayList<TreeItem<ObjetIHM>> petitsEnfants = creer_arborescence(reducedList, objetIHM.getObjetCSA());
                enfant.getChildren().setAll(petitsEnfants);
                enfant.setExpanded(!petitsEnfants.isEmpty());
            }
        }
        return enfants;
    }

    private void action_clic_noeud(ObjetCSA objetDi) {
        ecouteur.action_retour_component(objetDi, this, "clic");
        System.out.println("clic");
    }

    public ArrayList<? extends ObjetCSA> getSelectedObjects() {
        Collection<TreeItem<ObjetIHM>> items = treeView.getRoot().getChildren();
        return new ArrayList<ObjetCSA>(getSelectedObjects(items));
    }

    public void setSelectedObjects(ArrayList<? extends ObjetCSA> selectedObjects) {
        setSelectedObjects(selectedObjects, new ArrayList<>(treeView.getRoot().getChildren()));
    }

    public void setSelectedObjects(ArrayList<? extends ObjetCSA> selectedObjects, Collection<TreeItem> items) {
        for (TreeItem<?> item : treeView.getRoot().getChildren()) {
            if (item != null) {
                TreeItem<?> treeItem = item;
                if (treeItem.getGraphic() instanceof CheckBox) {
                    CheckBox checkBox = (CheckBox) treeItem.getGraphic();
                    for (ObjetCSA aSelectionner : selectedObjects) {
                        checkBox.setSelected(treeItem.getValue().equals(aSelectionner));
                    }
                } else {
                    setSelectedObjects(selectedObjects, new ArrayList<>(treeItem.getChildren()));
                }
            }
        }
    }

    public ArrayList<? extends ObjetCSA> getSelectedObjects(Collection<TreeItem<ObjetIHM>> items) {
        ArrayList<ObjetCSA> sortie = new ArrayList<>();
        items.forEach(treeItem -> {
            if (treeItem.getGraphic() instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) treeItem.getGraphic();
                if (checkBox.isSelected()) {
                    sortie.add(treeItem.getValue().getObjetCSA());
                }
            } else {
                sortie.addAll(getSelectedObjects(new ArrayList<>(treeItem.getChildren())));
            }
        });
        return sortie;
    }

    /*private static class CheckBoxElement extends CheckBox {
        ObjetIHM objetIHM;

        public CheckBoxElement() {
            setPadding(new Insets(5, 5, 5, 5));
            objetIHM = null;
            Platform.runLater(() -> {
                setText(ConstantesFX.STRING_VIDE);
            });
        }

        public CheckBoxElement(ObjetIHM objetDiIHM) {
            setPadding(new Insets(5, 5, 5, 5));
            objetIHM = objetDiIHM;
            Platform.runLater(() -> {
                setText(objetIHM.toString());
            });
        }

        public ObjetCSA getObjet() {
            if (objetIHM == null) {
                return null;
            }
            return objetIHM.getObjetCSA();
        }

        @Override
        public boolean equals(Object obj) {
            if (objetIHM == null) {
                System.err.println("** TreeCheckBox : objetIHM non initialisé !");
                return false;
            }
            if (obj instanceof ObjetIHM) {
                return objetIHM.equals(obj);
            }
            if (obj instanceof ObjetCSA) {
                return objetIHM.getObjetCSA().equals(obj);
            }
            if (obj instanceof CheckBoxElement) {
                return objetIHM.equals(((CheckBoxElement) obj).objetIHM);
            }
            return false;
        }

        public void setObjetIHM(ObjetIHM item) {
            objetIHM = item;
        }
    }*/
}
