package generalFX;

import controlsFX.*;
import general.DateString;
import general.GestionnaireImbrications;
import general.ObjetCSA;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import util.SimpleLogger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EcriveurChamps {
    static SimpleLogger logger = SimpleLogger.getLogger(EcriveurChamps.class);

    private static ObjetCSA getObjet(ObjetCSA objetDi, String cle) {
        ObjetCSA retour = objetDi.getFK(cle);
        if (retour != null) {
            return retour;
        }
        retour = objetDi.getObjetIntermediaire(cle);
        if (retour != null) {
            return retour;
        }
        retour = objetDi.getObjetDependant(cle);
        if (retour == null) {
            logger.error("Je n'ai pas trouvé d'objet correspondant à la clé " + cle + " dans l'objet " + objetDi);
        }
        return retour;
    }

    /**
     * Permet de charger les données des champs dans un objet représenté par une
     * hashmap
     *
     * @param pairesChamps hashmap(nom, champ)
     *                     les noms peuvent aussi bien être des noms de variables (directes ou internes)
     *                     ou même des noms de fk, fki ou dépendants
     *                     D'abord je checke les données simples, ensuite les fks et enfin l'intérieur des fks
     * @param elementBD    hashmap(nom, objet) de type ElementBD
     */
    public static void charger_champs(HashMap<String, Node> pairesChamps,
                                      ObjetCSA elementBD) {
        for (Map.Entry<String, Node> paire : pairesChamps.entrySet()) {
            String cle = paire.getKey();
            Node noeud = paire.getValue();
            // Gestion des champs vides:
            if (cle.startsWith("#")) {
                if (noeud instanceof Labeled labeled) {
                    Platform.runLater(() -> {
                        labeled.setText("");
                    });
                }
            } else {
                Object finalObjet = elementBD.chercher_valeur(cle);
                if (noeud instanceof TextInputControl) {
                    TextInputControl textInput = (TextInputControl) noeud;
                    Platform.runLater(() -> {
                        textInput.setText(finalObjet instanceof ObjetCSA ?
                                ((ObjetCSA) finalObjet).toExport() :
                                String.valueOf(finalObjet));
                    });
                } else if (noeud instanceof ComboFiltrableController) {
                    ObjetCSA objetFK = getObjet(elementBD, cle);
                    if (objetFK == null) {
                        GestionnaireImbrications gestionnaireImbrications = elementBD.getGestionnairesImbrications().get(cle);
                        if (gestionnaireImbrications != null) {
                            ObjetCSA quartier = gestionnaireImbrications.getQuartier();
                            if (quartier == null) {
                                ((ComboFiltrableController) noeud).setListeWithSelection(gestionnaireImbrications.getListeVilles(),
                                        gestionnaireImbrications.getVille());
                            } else {
                                ((ComboFiltrableController) noeud).setListeWithSelection(gestionnaireImbrications.getListeQuartiers(),
                                        quartier);
                            }
                        }
                    } else {
                        ((ComboFiltrableController) noeud).select(objetFK);
                    }
                } else if (noeud instanceof ComboEditableController) {
                    ObjetCSA objetFK = getObjet(elementBD, cle);
                    if (objetFK == null) {
                        GestionnaireImbrications gestionnaireImbrications = elementBD.getGestionnairesImbrications().get(cle);
                        if (gestionnaireImbrications != null) {
                            ObjetCSA quartier = gestionnaireImbrications.getQuartier();
                            if (quartier == null) {
                                ((ComboEditableController) noeud).setListeWithSilentSelection(gestionnaireImbrications.getListeVilles(),
                                        gestionnaireImbrications.getVille());
                            } else {
                                ((ComboEditableController) noeud).setListeWithSilentSelection(gestionnaireImbrications.getListeQuartiers(),
                                        quartier);
                            }
                        }
                    } else {
                        ((ComboEditableController) noeud).selectSilently(objetFK);
                    }
                } else if (noeud instanceof ComboBox) {
                    Platform.runLater(() -> {
                        Object valeur = elementBD.getSimpleData(cle);
                        if (valeur instanceof Integer) {
                            ((ComboBox<Object>) noeud).getSelectionModel().select((int) valeur);
                        } else {
                            ((ComboBox<Object>) noeud).getSelectionModel().select(valeur);
                        }
                    });
                } else if (noeud instanceof TableView) {
                    Platform.runLater(() -> {
                        ObservableList<ObjetIHM> newList = FXCollections.observableArrayList(
                                TransformeurObjetsIHM.transformer_en_objetsIHM(elementBD.getListe(cle)));
                        TableView<ObjetIHM> tableView = (TableView<ObjetIHM>) noeud;
                        tableView.setItems(newList);
                        tableView.refresh(); // Force immediate refresh
                    });

                } else if (noeud instanceof ListeBDController) {
                    ((ListeBDController) noeud).chargerListe(new ArrayList<>(elementBD.getListe(cle)));
                } else if (noeud instanceof ListeFiltrableController) {
                    ((ListeFiltrableController) noeud).chargerListe(new ArrayList<>(elementBD.getListe(cle)));
                } else if (noeud instanceof ListView) {
                    ((ListView) noeud).getItems().setAll(new ArrayList<>(elementBD.getListe(cle)));
                } else if (noeud instanceof TableViewCRUD) {
                    ((TableViewCRUD) noeud).charger_liste(new ArrayList<>(elementBD.getListe(cle)),
                            elementBD.getObjetFromType(cle));
                } else if (noeud instanceof DoubleListeController) {
                    ((DoubleListeController) noeud).envoyer_vers_cible(new ArrayList<>(elementBD.getListe(cle)));
                } else if (noeud instanceof CheckBox) {
                    Platform.runLater(() -> {
                        if (elementBD.getSimpleData(cle) instanceof Boolean) {
                            ((CheckBox) noeud).setSelected((Boolean) elementBD.getSimpleData(cle));
                        }
                    });
                } else if (noeud instanceof RadioButton) {
                    Platform.runLater(() -> {
                        if (elementBD.getSimpleData(cle) instanceof Boolean) {
                            ((RadioButton) noeud).setSelected((Boolean) elementBD.getSimpleData(cle));
                        }
                    });
                } else if (noeud instanceof ToggleButton) {
                    Platform.runLater(() -> {
                        if (elementBD.getSimpleData(cle) instanceof Boolean) {
                            ((ToggleButton) noeud).setSelected((Boolean) elementBD.getSimpleData(cle));
                        }
                    });
                } else if (noeud instanceof ColorPicker) {
                    Platform.runLater(() -> {
                        if (elementBD.getSimpleData(cle) instanceof Integer) {
                            ((ColorPicker) noeud).setValue(FX_RGBConverter.convert_rgb_2_color((int) elementBD.getSimpleData(cle)));
                        }
                    });
                } else if (noeud instanceof DatePicker) {
                    Platform.runLater(() -> {
                        Object date = elementBD.getSimpleData(cle);
                        if (date instanceof String) {
                            ((DatePicker) noeud).setValue(new DateString(date).getLocalDate());
                        } else if (date instanceof LocalDate) {
                            ((DatePicker) noeud).setValue((LocalDate) date);
                        } else if (date instanceof LocalDateTime) {
                            ((DatePicker) noeud).setValue(((LocalDateTime) date).toLocalDate());
                        } else {
                            ((DatePicker) noeud).setValue(null);
                        }
                    });
                } else if (noeud instanceof DateBoxController) {
                    Platform.runLater(() -> {
                        Object date = elementBD.getSimpleData(cle);
                        if (date instanceof String) {
                            ((DateBoxController) noeud).setDate(new DateString(date).getLocalDate());
                        } else if (date instanceof LocalDate) {
                            ((DateBoxController) noeud).setDate((LocalDate) date);
                        } else if (date instanceof LocalDateTime) {
                            ((DateBoxController) noeud).setDate(((LocalDateTime) date).toLocalDate());
                        } else {
                            ((DateBoxController) noeud).setDate(null);
                        }
                    });
                } else if (noeud instanceof CombosImbriques) {
                    CombosImbriques afficheurImbrications = (CombosImbriques) noeud;
                    if (finalObjet instanceof GestionnaireImbrications) {
                        afficheurImbrications.charger_donnees((GestionnaireImbrications) finalObjet);
                    }
                } else if (noeud instanceof Labeled) {
                    Labeled labeled = (Labeled) noeud;
                    Platform.runLater(() -> {
                        labeled.setText(finalObjet instanceof ObjetCSA ?
                                ((ObjetCSA) finalObjet).toExport() :
                                String.valueOf(finalObjet));
                    });
                }
            }

        }
    }


}
