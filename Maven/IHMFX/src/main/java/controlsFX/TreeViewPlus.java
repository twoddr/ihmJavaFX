package controlsFX;

import general.ObjetCSA;
import generalFX.CreateurArborescence;
import generalFX.ObjetIHM;
import generalFX.TransformeurObjets;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TreeViewPlus extends TreeView<ObjetIHM> {

    private ArrayList<String> variablesEditables = new ArrayList<>();
    private HashMap<String, Node> noeudAffiche = new HashMap<>();
    private ArrayList<String> listeEltsANePasAfficher = new ArrayList<>();
    private MaitreFX mom;

    public TreeViewPlus() {
        setCellFactory(new Callback<TreeView<ObjetIHM>, TreeCell<ObjetIHM>>() {
            @Override
            public TreeCell<ObjetIHM> call(TreeView<ObjetIHM> treeView) {
                return new CustomTreeCell();
            }
        });
    }

    public HashMap<String, Node> getNoeudAffiche() {
        return noeudAffiche;
    }

    public void setNoeudAffiche(HashMap<String, Node> noeudAffiche) {
        this.noeudAffiche = noeudAffiche;
    }

    public ArrayList<String> getVariablesEditables() {
        return variablesEditables;
    }

    public void setVariablesEditables(ArrayList<String> variablesEditables) {
        this.variablesEditables = variablesEditables;
    }

    /**
     * Permet de charger les données dans la listeTable
     * Les données doivent être sous forme de liste d'ElementBD
     *
     * @param donnees liste à insérer dans le tableau
     * @param type    le type des données affichées (nomTable)
     */
    public void charger_liste(ArrayList<Object> donnees, String type) {

        ArrayList<ObjetCSA> objetDis = TransformeurObjets.transformer_en_objetsDi(donnees, type, null, mom);
        Platform.runLater(() -> {
            TreeItem root = new TreeItem();
            ArrayList<TreeItem<ObjetIHM>> items = CreateurArborescence.creer_arborescence(objetDis, ObjetCSA.creer_objet_vide());
            root.getChildren().setAll(items);
            setRoot(root);
            root.setExpanded(true);
            setShowRoot(false);
        });
    }

    private boolean isEditable(String nomVariable) {
        return variablesEditables.contains(nomVariable);
    }

    public void addListeANePasAfficher(String nom) {
        listeEltsANePasAfficher.add(nom);
    }

    public void clearListeANePasAfficher() {
        listeEltsANePasAfficher.clear();
    }

    private Node extract_node_from_name(String nomVariable, Object value) {
        Node noeud = noeudAffiche.get(nomVariable);
        if (noeud == null) noeud = new Label();
        noeud.setDisable(!isEditable(nomVariable));
        insert_value_in_node(value, noeud);
        return noeud;
    }

    private void insert_value_in_node(Object value, Node noeud) {
        if (noeud instanceof CheckBox) {
            if (value instanceof Boolean) {
                ((CheckBox) noeud).setSelected((boolean) value);
                return;
            }
            if (value instanceof Integer) {
                ((CheckBox) noeud).setSelected((int) value != 0);
                return;
            }
            ((CheckBox) noeud).setText(value.toString());
            return;
        }
        if (noeud instanceof TextField) {
            ((TextField) noeud).setText(value.toString());
            return;
        }
        if (noeud instanceof Label) {
            ((Label) noeud).setText(value.toString());
        }
    }

    public void setMaitre(MaitreFX maitreFX) {
        mom = maitreFX;
    }

    // Custom TreeCell class to display controls in each leaf
    private class CustomTreeCell extends TreeCell<ObjetIHM> {
        private final HBox hbox = new HBox(10);

        @Override
        protected void updateItem(ObjetIHM objetIHM, boolean empty) {
            super.updateItem(objetIHM, empty);

            if (empty || objetIHM == null) {
                setText(null);
                setGraphic(null);
            } else {
                hbox.getChildren().clear(); // Clear previous items
                ArrayList<String> variablesVisibles = objetIHM.getVariablesVisibles();
                variablesVisibles.removeAll(listeEltsANePasAfficher);
                LinkedHashMap<String, Object> data = objetIHM.getObjetCSA().getData(variablesVisibles);

                AtomicInteger count = new AtomicInteger(data.size());
                // Iterate through the HashMap and create controls based on the value type
                data.forEach((key, value) -> {
                    hbox.getChildren().add(extract_node_from_name(key, value));
                    count.getAndDecrement();
                    if (count.get() > 0) hbox.getChildren().add(new Separator(Orientation.VERTICAL));
                });

                setGraphic(hbox);
                setText(null); // Clear text to show only the custom graphic
            }
        }
    }
}
