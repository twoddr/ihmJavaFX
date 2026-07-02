package generalFX;

import controlsFX.*;
import general.DetecteurModification;
import general.GestionnaireImbrications;
import general.ObjetCSA;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LecteurChamps {
    private static ObjetCSA objetCSA;
    private static HashMap<String, Node> hashNoeuds;

    public static ObjetCSA enregistrer_donnees(ObjetCSA elementBD,
                                               HashMap<String, Node> pairesChamps) {
        objetCSA = elementBD;
        hashNoeuds = pairesChamps;
        return lire_champs();
    }

    private static ObjetCSA lire_champs() {
        for (Map.Entry<String, Node> paire : hashNoeuds.entrySet()) {
            String cle = paire.getKey();
            Node noeud = paire.getValue();
            if (noeud instanceof TextField) {
                objetCSA.inserer_valeur(cle, ((TextField) noeud).getText());
            } else if (noeud instanceof TextArea) {
                objetCSA.inserer_valeur(cle, ((TextArea) noeud).getText().trim());
            } else if (noeud instanceof Label) {
                objetCSA.inserer_valeur(cle, ((Label) noeud).getText());
            } else if (noeud instanceof ComboFiltrableController) {
                Object objet = ((ComboFiltrableController) noeud).getSelection();
                if (objet instanceof ObjetCSA) {
                    if (!objetCSA.inserer_fk(cle, (ObjetCSA) objet)) {
                        objetCSA.inserer_dans_gestionnaireImbrication(cle, (ObjetCSA) objet);
                    }
                }
            } else if (noeud instanceof CombosImbriques) {
                GestionnaireImbrications objet = ((CombosImbriques) noeud).getGestionnaire();
                objetCSA.inserer_gestionnaireImbrications(cle, objet);
            } else if (noeud instanceof ComboEditableController) {
                Object objet = ((ComboEditableController) noeud).getSelection();
                if (objet instanceof ObjetCSA) {
                    if (!objetCSA.inserer_fk(cle, (ObjetCSA) objet)) {
                        objetCSA.inserer_dans_gestionnaireImbrication(cle, (ObjetCSA) objet);
                    }
                } else {
                    objetCSA.inserer_valeur(cle, objet);
                }
            } else if (noeud instanceof ComboBox) {
                if (objetCSA.getSimpleData(cle) instanceof Integer) {
                    objetCSA.inserer_valeur(cle, ((ComboBox) noeud).getSelectionModel().getSelectedIndex());
                } else {
                    objetCSA.inserer_valeur(cle, ((ComboBox) noeud).getSelectionModel().getSelectedItem());
                }
            } else if (noeud instanceof ListeBDController) {
                ListeBDController listeBDController = (ListeBDController) noeud;
                objetCSA.inserer_fk(cle, (ObjetCSA) listeBDController.getSelection());
            } else if (noeud instanceof ListeFiltrableController) {
                objetCSA.inserer_fk(cle, (ObjetCSA) ((ListeFiltrableController) noeud).getListe()
                        .getSelectionModel().getSelectedItem());
            } else if (noeud instanceof ListView) {
                objetCSA.inserer_fk(cle, (ObjetCSA) ((ListView<Object>) noeud).getSelectionModel().getSelectedItem());
            } else if (noeud instanceof TableView) {
                objetCSA.inserer_liste(cle, new ArrayList<>(((TableView) noeud).getItems()));
            } else if (noeud instanceof DoubleListeController) {
                objetCSA.inserer_liste(cle, new ArrayList<>(((DoubleListeController) noeud).getListeCible().getItems()));
            } else if (noeud instanceof CheckBox) {
                objetCSA.inserer_valeur(cle, ((CheckBox) noeud).isSelected());
            } else if (noeud instanceof ColorPicker) {
                objetCSA.inserer_valeur(cle, FX_RGBConverter.convert_color_2_rgb(((ColorPicker) noeud).getValue()));
            } else if (noeud instanceof DatePicker) {
                objetCSA.inserer_valeur(cle, ((DatePicker) noeud).getValue());
            } else if (noeud instanceof RadioButton) {
                objetCSA.inserer_valeur(cle, ((RadioButton) noeud).isSelected());
            } else if (noeud instanceof ToggleButton) {
                objetCSA.inserer_valeur(cle, ((ToggleButton) noeud).isSelected());
            } else if (noeud instanceof DateBoxController) {
                objetCSA.inserer_valeur(cle, ((DateBoxController) noeud).getDate());
            } else if (noeud instanceof TableViewCRUD) {
                TableViewCRUD tableViewCRUD = (TableViewCRUD) noeud;
                ObjetCSA reference = tableViewCRUD.getElementDeReference();
                if (reference == null) {
                    System.err.println("LecteurChamps/lire_champs : La TableViewCRUD ne connait pas le type des élts" +
                            " qu'elle est supposée contenir. La liste ne sera pas enregistrée !");
                } else {
                    String nomTable = reference.getType();
                    ArrayList<Object> listeFinale = tableViewCRUD.getListe();
                    objetCSA.inserer_liste(nomTable, listeFinale);
                    ArrayList<Object> listeOriginale = tableViewCRUD.getListeOriginale();
                    DetecteurModification detecteurModification = new DetecteurModification(listeOriginale, listeFinale);
                    objetCSA.inserer_liste(cle + "_ajouts", detecteurModification.getListeAAjouter());
                    objetCSA.inserer_liste(cle + "_suppressions", detecteurModification.getListeASupprimer());
                }
            } else {
                System.err.println("LecteurChamps : je dois lire un champ que je ne gère pas : " + noeud +
                        ", donc je l'ignore !");
            }
        }
        return objetCSA;
    }

    private static Object extraire_valeur(Node noeud, Class classe) {
        if (noeud instanceof TextArea) {
            return ((TextArea) noeud).getText();
        }
        if (noeud instanceof PasswordField) {
            return ((PasswordField) noeud).getText();
        }
        if (noeud instanceof TextField) {
            String texte = ((TextField) noeud).getText().trim();
            if (classe != null) {
                if (classe.equals(Integer.class)) {
                    try {
                        return Integer.parseInt(texte);
                    } catch (NumberFormatException ex) {
                        return texte;
                    }
                }
                if (classe.equals(Float.class)) {
                    try {
                        return Float.parseFloat(texte.replace(',', '.'));
                    } catch (NumberFormatException e) {
                        return texte;
                    }
                }
            }
            return texte;
        }
        if (noeud instanceof ComboBox) {
            Object selection = ((ComboBox) noeud).getSelectionModel().getSelectedItem();
            if (selection instanceof ObjetCSA) {
                return ((ObjetCSA) selection).getId();
            }
            if (selection instanceof ObjetCSA) {
                return ((ObjetCSA) selection).getId();
            }
            if (classe.equals(Integer.class) &&
                    !(selection instanceof Integer)) {
                return ((ComboBox<?>) noeud).getSelectionModel().getSelectedIndex();
            }
            return selection;
        }
        if (noeud instanceof ComboFiltrableController) {
            Object selection = ((ComboFiltrableController) noeud).getSelection();
            if (selection instanceof ObjetCSA) {
                return ((ObjetCSA) selection).getId();
            }
            if (selection instanceof ObjetCSA) {
                return ((ObjetCSA) selection).getId();
            }
            if (classe.equals(Integer.class) &&
                    !(selection instanceof Integer)) {
                return ((ComboFiltrableController) noeud).getIndiceSelectionne();
            }
            return selection;
        }
        if (noeud instanceof ComboEditableController) {
            Object selection = ((ComboEditableController) noeud).getSelection();
            if (selection instanceof ObjetCSA) {
                return ((ObjetCSA) selection).getId();
            }
            if (selection instanceof ObjetCSA) {
                return ((ObjetCSA) selection).getId();
            }
            if (classe.equals(Integer.class) &&
                    !(selection instanceof Integer)) {
                return ((ComboEditableController) noeud).getIndiceSelectionne();
            }
            return selection;
        }
        if (noeud instanceof ListViewStringItems) {
            return ((ListViewStringItems) noeud).getData();
        }
        if (noeud instanceof ListView) {
            Object selection = ((ListView) noeud).getSelectionModel().getSelectedItem();
            if (selection instanceof ObjetCSA && classe.equals(Integer.class)) {
                return ((ObjetCSA) selection).getId();
            }
            if (classe.equals(Integer.class) &&
                    !(selection instanceof Integer)) {
                return ((ListView<Object>) noeud).getSelectionModel().getSelectedIndex();
            }

            return selection;
        }
        if (noeud instanceof DoubleListeController) {
            DoubleListeController doubleListe = (DoubleListeController) noeud;
            ArrayList<Object> retourArrayList = new ArrayList<>();
            doubleListe.getListeCible().getItems().forEach((elt) -> {
                if (elt instanceof ObjetCSA) {
                    retourArrayList.add(((ObjetCSA) elt).getId());
                } else if (elt instanceof Integer) {
                    retourArrayList.add(elt);
                }
            });
            return retourArrayList;
        }
        if (noeud instanceof CheckBox) {
            return ((CheckBox) noeud).isSelected();
        }
        if (noeud instanceof RadioButton) {
            return ((RadioButton) noeud).isSelected();
        }
        if (noeud instanceof DatePicker) {
            DatePicker datePicker = (DatePicker) noeud;
            LocalDate date = datePicker.getValue();
            return ((date != null)
                    ? datePicker.getChronology().date(date)
                    : null);
        }
        if (noeud instanceof ColorPicker) {
            if (classe.equals(Integer.class)) {
                return RGBConverter.convertRGB2Int(((ColorPicker) noeud).getValue());
            }
            if (classe.equals(java.awt.Color.class)) {
                int intColor = RGBConverter.convertRGB2Int(((ColorPicker) noeud).getValue());
                return (new java.awt.Color(intColor));
            }
            return ((ColorPicker) noeud).getValue();
        }
        System.err.println("Attention : le noeud " + noeud + " n'a pas trouvé sa variable !");
        return null;
    }

    /*
     ************************************************
     */
    private static class RGBConverter {
        public static Integer convertRGB2Int(Color color) {
            int rouge = ((int) (color.getRed() * 255)) << 24;
            int vert = ((int) (color.getGreen() * 255)) << 16;
            int bleu = ((int) (color.getBlue() * 255)) << 8;
            int opacite = ((int) (color.getOpacity() * 255));

            return rouge + vert + bleu + opacite;
        }

        public Color convertInt2RGB(Integer couleur) {
            double red = (couleur >> 24) & 0xFF;
            double green = (couleur >> 16) & 0xFF;
            double blue = (couleur >> 8) & 0xFF;
            double opacity = couleur & 0xFF;

            Color color = new Color(red / 255f, green / 255f, blue / 255f, opacity / 255f);
            return color;
        }
    }
}
