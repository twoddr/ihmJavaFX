package generalFX;

import controlsFX.*;
import general.GestionnaireRetour;
import general.ObjetCSA;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GestionnaireListesFX implements GestionnaireRetour {
    protected HashMap<Node, Object> composant_selections;

    public GestionnaireListesFX(ArrayList<Node> composants) {
        composant_selections = new HashMap<>();
        if (composants != null) {
            composants.forEach(jcomponent -> {
                composant_selections.put(jcomponent, null);
            });
        }
    }

    public GestionnaireListesFX(Node node) {
        composant_selections = new HashMap<>();
        if (node != null) {
            composant_selections = new HashMap<>();
            composant_selections.put(node, null);
        }
    }

    public GestionnaireListesFX(Node node, Object objetASelectionner) {
        composant_selections = new HashMap<>();
        if (node != null) {
            composant_selections = new HashMap<>();
            composant_selections.put(node, objetASelectionner);
        }
    }

    public GestionnaireListesFX(HashMap<Node, Object> nodeObjectHashMap) {
        if (nodeObjectHashMap == null) {
            composant_selections = new HashMap<>();
        } else {
            composant_selections = nodeObjectHashMap;
        }
    }

    @Override
    public void traitementRetour(ArrayList arrayList) {
        int code = (int) arrayList.get(0);

        // Codes 1-3 : opérations d'écriture
        if (code == 1) {
            // Protocole optimisé : (1, nomTable, nouvelID)
            // GestionnaireListesFX ne dispose pas de l'objet complet pour l'injecter directement.
            // Les composants individuels (ListTableBDController, ComboEditable...) gèrent eux-mêmes l'ajout direct.
            // Ici on accepte le retour proprement sans erreur.
            if (arrayList.size() >= 3 && arrayList.get(2) instanceof ObjetCSA objetAjoute) {
                // Cas futur : si la BD renvoie l'objet complet, on l'ajoute aux composants
                for (Map.Entry<Node, Object> couple : composant_selections.entrySet()) {
                    Node component = couple.getKey();
                    if (component instanceof ComboEditableController combo) {
                        combo.ajouter_element_direct(objetAjoute);
                    } else if (component instanceof ListTableBDController liste) {
                        liste.ajouter_element(objetAjoute);
                    }
                }
            }
            // Quelle que soit la forme (ID ou objet), on ne génère pas d'erreur
            return;
        }
        if (code == 2 || code == 3) {
            // Modification ou suppression : acknowledged, pas de traitement automatique ici
            return;
        }

        String nomTable = String.valueOf(arrayList.get(1));
        Object obj = arrayList.get(2);

        if (code == 4) {
            if (obj instanceof ArrayList al) {

                for (Map.Entry<Node, Object> couple : composant_selections.entrySet()) {
                    Node component = couple.getKey();
                    Object selection = couple.getValue();

                    if (component instanceof ComboEditableController comboBox) {

                        if (selection instanceof ObjetCSA) {
                            // ✅ Si selection a un ID invalide, chercher par nom
                            Object aSelectionner = trouver_objet_equivalent(selection, al);
                            comboBox.setListeWithSelection(al, aSelectionner);
                        } else {
                            comboBox.setListe(al);
                        }

                    } else if (component instanceof ComboFiltrableController) {
                        ComboFiltrableController combo = (ComboFiltrableController) component;
                        combo.setListe(al);

                        if (selection != null) {
                            Object aSelectionner = trouver_objet_equivalent(selection, al);
                            combo.select(aSelectionner);
                        }

                    } else if (component instanceof ComboBox comboBox) {
                        Platform.runLater(() -> {
                            comboBox.getItems().setAll(al);

                            if (selection != null) {
                                Object aSelectionner = trouver_objet_equivalent(selection, al);
                                comboBox.getSelectionModel().select(aSelectionner);
                            }
                        });

                    } else if (component instanceof ListeBDController) {
                        ListeBDController jList = (ListeBDController) component;
                        Object x = selection == null ? jList.getElementsSelectionnes() : selection;
                        jList.chargerListe(al);

                        if (x instanceof ObservableList && !((ObservableList<?>) x).isEmpty()) {
                            Object premier = ((ObservableList<?>) x).get(0);
                            Object equivalent = trouver_objet_equivalent(premier, al);
                            if (equivalent != null) {
                                jList.select(equivalent);
                            }
                        } else {
                            Object equivalent = trouver_objet_equivalent(x, al);
                            if (equivalent != null) {
                                jList.select(equivalent);
                            }
                        }

                    } else if (component instanceof ListTableBDController) {
                        ListTableBDController jList = (ListTableBDController) component;
                        Object x = selection == null ? jList.getElementsSelectionnes() : selection;
                        jList.charger_liste(al, nomTable);

                        if (x instanceof ObservableList && !((ObservableList<?>) x).isEmpty()) {
                            Object premier = ((ObservableList<?>) x).get(0);
                            Object equivalent = trouver_objet_equivalent(premier, al);
                            if (equivalent instanceof ObjetCSA) {
                                jList.select((ObjetCSA) equivalent);
                            }
                        } else if (x instanceof ObjetCSA) {
                            Object equivalent = trouver_objet_equivalent(x, al);
                            if (equivalent instanceof ObjetCSA) {
                                jList.select((ObjetCSA) equivalent);
                            }
                        }

                    } else if (component instanceof TreeViewBDController) {
                        TreeViewBDController jList = (TreeViewBDController) component;
                        ObjetCSA x = jList.getSelection();
                        jList.charger_liste(al, nomTable);

                        if (x != null) {
                            Object equivalent = trouver_objet_equivalent(x, al);
                            if (equivalent instanceof ObjetCSA) {
                                jList.select((ObjetCSA) equivalent);
                            }
                        }

                    } else if (component instanceof DoubleListeController) {
                        DoubleListeController doubleListe = (DoubleListeController) component;
                        doubleListe.setListeComplete(al);
                        if (selection instanceof Collection) {
                            doubleListe.envoyer_vers_cible((Collection) selection);
                        }

                    } else if (component instanceof MonTreeView) {
                        MonTreeView treeView = (MonTreeView) component;
                        if (selection instanceof ObjetCSA) {
                            ObjetCSA objetDi = (ObjetCSA) selection;
                            al.forEach(elt -> {
                                if (elt instanceof ObjetCSA) {
                                    ObjetCSA element = (ObjetCSA) elt;
                                    element.setParent(objetDi);
                                }
                            });
                        }
                        treeView.addData(al);

                    } else if (component instanceof BoutonListe) {
                        BoutonListe bts = (BoutonListe) component;
                        ObservableList<Object> selections = bts.getSelectionnes();
                        bts.setListe(al);
                        if (selection instanceof Collection) {
                            bts.setSelectionnes((ObservableList) selection);
                        } else {
                            bts.setSelectionnes(selections);
                        }

                    } else if (component instanceof ListeFiltrableController) {
                        ListeFiltrableController panneauListeFiltree = (ListeFiltrableController) component;
                        Object x = selection == null ? panneauListeFiltree.getElementsSelectionnes() : selection;
                        panneauListeFiltree.chargerListe(al);

                        Object equivalent = trouver_objet_equivalent(x, al);
                        if (equivalent != null) {
                            panneauListeFiltree.getListe().getSelectionModel().select(equivalent);
                            panneauListeFiltree.action_filtrer(null);
                        }

                    } else if (component instanceof ListView) {
                        ListView listView = (ListView) component;
                        Object x = selection == null ? listView.getSelectionModel().getSelectedItem() : selection;
                        Platform.runLater(() -> {
                            listView.getItems().setAll(al);
                            Object equivalent = trouver_objet_equivalent(x, al);
                            if (equivalent != null) {
                                listView.getSelectionModel().select(equivalent);
                            }
                        });

                    } else if (component instanceof CheckBoxListController) {
                        CheckBoxListController checkList = (CheckBoxListController) component;
                        checkList.setListe(al);

                    } else if (component instanceof TreeCheckBoxListController) {
                        TreeCheckBoxListController checkList = (TreeCheckBoxListController) component;
                        checkList.setListe(al);
                    }
                }
                return;
            }
            System.err.println("** GestionnaireListes/traitementRetour : " +
                    "je reçois une non liste ! " + obj);
            return;
        }
        System.err.println("** GestionnaireListes/traitementRetour : " +
                "je reçois une code non prévu " + code + " -> " + obj);
    }

    /**
     * Trouve un objet équivalent dans la liste, même si l'ID a changé
     */
    private Object trouver_objet_equivalent(Object cible, ArrayList liste) {
        if (cible == null || liste == null || liste.isEmpty()) {
            return null;
        }

        for (Object elt : liste) {
            if (isEquivalent(cible, elt)) {
                return elt;
            }
        }

        return null;
    }

    /**
     * Méthode utilitaire pour comparer deux objets de manière robuste
     */
    private boolean isEquivalent(Object obj1, Object obj2) {
        if (obj1 == null || obj2 == null) {
            return false;
        }

        // Comparaison standard
        if (obj1.equals(obj2)) {
            return true;
        }

        // Comparaison spéciale pour ObjetCSA (par ID et type)
        if (obj1 instanceof ObjetCSA && obj2 instanceof ObjetCSA) {
            ObjetCSA cibleCSA = (ObjetCSA) obj1;
            ObjetCSA eltCSA = (ObjetCSA) obj2;

            // Même type requis
            if (!cibleCSA.getType().equals(eltCSA.getType())) {
                return false;
            }

            // Si les deux ont un ID valide, comparer par ID
            if (cibleCSA.getId() > 0 && eltCSA.getId() > 0) {
                return cibleCSA.getId() == eltCSA.getId();
            } else {
                // Sinon, comparer par nom (toString)
                return cibleCSA.toString().equals(eltCSA.toString());
            }
        }

        return false;
    }
}
