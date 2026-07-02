package generalFX;

import javafx.scene.control.TreeItem;

import java.util.ArrayList;

/**
 * Permet d'afficher l'arborescence d'un treeview
 */
public class AfficheurTreeView {
    /**
     * Affiche l'arborescence du treeview sous forme de String avec des symboles
     * Unicode.
     *
     * @param root   la racine à afficher
     * @param indent le caractère d'indentation pour les sous-éléments
     * @return le contenu formaté avec de jolis symboles
     */
    public static String getArborescence(TreeItem root, String indent) {
        StringBuilder builder = new StringBuilder();

        // Choix du symbole : Case vide (☐) pour les feuilles, puce (•) pour les
        // dossiers
        // Vous pouvez aussi utiliser "☑" si vous voulez simuler un état coché par
        // défaut
        String bullet = root.isLeaf() ? "[ ] " : "+ ";

        builder.append(indent)
                .append(bullet)
                .append(root.getValue())
                .append("\n");

        for (Object child : root.getChildren()) {
            if (child instanceof TreeItem) {
                // On utilise un caractère de liaison visuel pour les enfants (│ ou └───)
                // Ici on garde votre indentation simple mais on pourrait l'enrichir
                builder.append(getArborescence((TreeItem) child, indent + "    "));
            }
        }

        return builder.toString();
    }

    /**
     * Affiche l'arborescence du treeview sous forme d'arraylist de String
     *
     * @param root   la racine à afficher
     * @param indent le caractère d'indentation pour les sous-éléments
     * @return le contenu formaté : chaque élément est un des éléments de la racine
     *         selon getArborescence
     */
    public static ArrayList<String> getArborescences(TreeItem root, String indent) {
        ArrayList<String> retour = new ArrayList<>();
        for (Object treeItem : root.getChildren()) {
            if (treeItem instanceof TreeItem) {
                StringBuilder builder = new StringBuilder();
                String bullet = ((TreeItem) treeItem).isLeaf() ? "[  ] " : "• ";
                builder.append(indent).append(bullet).append(((TreeItem) treeItem).getValue()).append("\n");

                for (Object child : ((TreeItem) treeItem).getChildren()) {
                    if (child instanceof TreeItem) {
                        builder.append(getArborescence((TreeItem) child, indent + "    "));
                    }
                }
                retour.add(builder.toString());
            } else {
                System.err.println("** Ceci ne devrait pas arriver : j'attends un treeItem et je reçois : " + treeItem);
                retour.add(treeItem.toString());
            }
        }

        return retour;
    }
}
