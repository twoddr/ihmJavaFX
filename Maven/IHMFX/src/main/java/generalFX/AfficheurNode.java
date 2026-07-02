package generalFX;

import controlsFX.ListTableBDController;
import controlsFX.MaitreFX;
import general.GestionnaireRetour;
import general.ObjetCSA;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;

import javax.swing.*;
import java.util.ArrayList;

/**
 * S'occupe d'effectuer la recherche et d'insérer la liste reçue dans le combo
 */
public class AfficheurNode implements GestionnaireRetour {
    private Node noeud;
    private String nomTable;
    private MaitreFX mom;

    /**
     * S'occupe d'effectuer la recherche et d'insérer la liste reçue dans le combo
     *
     * @param nom       le type de l'objet de la recherche
     * @param composant le combo dans lequel les résultats de la recherche sont attendus
     * @param maitreFX  la fenêtre parente
     */
    public AfficheurNode(String nom, Node composant, MaitreFX maitreFX) {
        nomTable = nom;
        noeud = composant;
        mom = maitreFX;
        ObjetCSA objetIHM = mom.getElementFromType(nomTable);
        go(objetIHM);
    }

    /**
     * S'occupe d'effectuer la recherche et d'insérer la liste reçue dans le combo
     *
     * @param objetIHM l'objet de la recherche (qui contient des informations utiles
     *                 pour la BD, sinon le nom aurait suffi !)
     * @param node     le combo dans lequel les résultats de la recherche sont attendus
     * @param maitreFX la fenêtre parente
     */
    public AfficheurNode(ObjetCSA objetIHM, Node node, MaitreFX maitreFX) {
        nomTable = objetIHM.getType();
        noeud = node;
        mom = maitreFX;

        go(objetIHM);
    }

    private void go(ObjetCSA oihm) {
        SwingUtilities.invokeLater(() -> {
            if (noeud instanceof ComboBox) {
                ((ComboBox<?>) noeud).getItems().clear();
            } else if (noeud instanceof ListView) {
                ((ListView<?>) noeud).getItems().clear();
            } else if (noeud instanceof ListTableBDController) {
                ((ListTableBDController) noeud).clear();
            } else {
                System.err.println("*** AfficheurNode : attention : noeud non géré => " + noeud);
            }
        });

        mom.appeler_bd(4, oihm, this);
    }

    @Override
    public void traitementRetour(ArrayList arrayList) {
        if (arrayList.size() > 0 &&
                arrayList.get(0) instanceof Integer) {
            int code = (int) arrayList.get(0);
            if (code == 4) {
                if (arrayList.size() > 2 && arrayList.get(1) instanceof String) {
                    Object o = arrayList.get(2);
                    String type = String.valueOf(arrayList.get(1));
                    if (o instanceof ArrayList) {
                        SwingUtilities.invokeLater(() -> {
                            ArrayList<ObjetCSA> objetIHMS = (ArrayList<ObjetCSA>) o;
                            if (noeud instanceof ComboBox) {
                                ((ComboBox<ObjetCSA>) noeud).getItems().setAll(objetIHMS);
                            } else if (noeud instanceof ListView) {
                                ((ListView<ObjetCSA>) noeud).getItems().setAll(objetIHMS);
                            } else if (noeud instanceof ListTableBDController) {
                                ((ListTableBDController) noeud).charger_liste(new ArrayList<>(objetIHMS),
                                        nomTable);
                            }
                        });
                        return;
                    }
                    mom.affiche_console("*** AfficheurNoeud : je m'attends à une AL, mais je reçois : " + o);
                    return;
                }
                mom.affiche_console("*** AfficheurNoeud : j'attends (int,string,liste), " +
                        "mais j'obtiens : " + arrayList);
                return;
            }
            String message = "Échec de la consultation " + nomTable;
            mom.affiche_utilisateur(message);
            mom.affiche_console("** AfficheurNoeud : " + message + "\n" +
                    "code reçu : " + code);
            return;
        }
        String message = "Échec de la consultation " + nomTable;
        mom.affiche_utilisateur(message);
        mom.affiche_console("** AfficheurNoeud : " + message + "\n" +
                "liste reçue : " + arrayList);
    }
}
