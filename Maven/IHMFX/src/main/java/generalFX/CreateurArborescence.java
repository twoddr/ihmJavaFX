package generalFX;

import general.ObjetCSA;
import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.function.Predicate;

public class CreateurArborescence {


    public static ArrayList<TreeItem<ObjetIHM>> creer_arborescence(ArrayList<? extends ObjetCSA> objetDis,
                                                                   ObjetCSA parent) {
        ArrayList<TreeItem<ObjetIHM>> treeItemsParents = new ArrayList<>();
        if (objetDis.isEmpty()) {
            return treeItemsParents;
        }
        Predicate<ObjetCSA> predicat = new Predicate<ObjetCSA>() {
            @Override
            public boolean test(ObjetCSA objetDi) {
                // si pas de parent, alors racine !
                if (parent == null || parent.getId() == 0) {
                    return objetDi.getIdParent() == 0;
                }
                // Si parent existe, il faut le trouver selon l'id ET le type
                return parent.getId() == objetDi.getIdParent() &&
                        parent.getType().equals(objetDi.getParent().getType());
            }
        };
        splitList(objetDis, predicat, treeItemsParents);

        // on parcourt la liste de ceux qui ont réussi le prédicat
        for (TreeItem<ObjetIHM> treeItemParent : treeItemsParents) {
            // bout de liste pour terminer la récurrence
            if (objetDis.isEmpty()) break;

            if (treeItemParent.getValue() != null) {
                ObjetIHM noeudIHM = treeItemParent.getValue();
                ArrayList<TreeItem<ObjetIHM>> petits_enfants = creer_arborescence(objetDis, noeudIHM.getObjetCSA());
                treeItemParent.getChildren().setAll(petits_enfants);
                boolean nonvide = !petits_enfants.isEmpty();
                treeItemParent.setExpanded(nonvide);
            }
        }

        return treeItemsParents;
    }

    private static void splitList(ArrayList<? extends ObjetCSA> objetDis,
                                  Predicate<ObjetCSA> postulat,
                                  ArrayList<TreeItem<ObjetIHM>> treeItemParents) {
        ArrayList<ObjetCSA> utilises = new ArrayList<>();
        for (ObjetCSA objetDi : objetDis) {
            //System.out.println("***\nDEBUG : Test de postulat entre " + objetDi + " et son parent + haut >>> ");
            if (postulat.test(objetDi)) {
                //System.out.println("VRAI");
                treeItemParents.add(new TreeItem<>(new ObjetIHM(objetDi)));
                utilises.add(objetDi);
            }
            //System.out.println("FAUX");
        }
        objetDis.removeAll(utilises);

        // à la fin, treeItemParents contient la liste de tous ceux qui ont réussi le prédicat
    }


}
