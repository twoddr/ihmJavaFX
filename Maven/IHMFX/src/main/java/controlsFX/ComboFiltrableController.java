package controlsFX;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import general.ObjetCSA;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.TreeSet;

/**
 * FXML Controller class
 *
 * @author didiertowe
 */
public class ComboFiltrableController extends AnchorPane implements Initializable {

    private static final int SEUIL_GRANDE_LISTE = 60;
    private static final int MIN_CARACTERES_FILTRE = 2;
    private static final int MAX_RESULTATS_COMBO = 30;
    @FXML
    private TextField tfTexte;
    @FXML
    private ComboBox<Object> combo;
    @FXML
    private Button btnEffacer;
    private ArrayList<Object> listeOriginale = new ArrayList<>();
    private ArrayList<Object> listeComplete = new ArrayList<>(); // backup complet
    private Object selection = null;
    private EcouteurFX ecouteurFX = null;

    public ComboFiltrableController() {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/fxml/ComboFiltrable.fxml"));

        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        combo.setCellFactory(new Callback<ListView<Object>, ListCell<Object>>() {
            @Override
            public ListCell<Object> call(ListView<Object> list) {
                return new CelluleListe();
            }
        });

        btnEffacer.setTooltip(new Tooltip("Permet de réinitialiser la sélection"));

        tfTexte.textProperty().addListener((obs, oldVal, newVal) -> {
            if (listeComplete.size() > SEUIL_GRANDE_LISTE) {
                if (newVal.length() < MIN_CARACTERES_FILTRE) {
                    tfTexte.setPromptText(String.format("Min. %d car. (%d élts)",
                            MIN_CARACTERES_FILTRE, listeComplete.size()));
                }
            }
        });
    }

    public void setEcouteurFX(EcouteurFX ecouteurFX) {
        this.ecouteurFX = ecouteurFX;
    }

    @FXML
    public void action_vider_textField(ActionEvent action) {
        tfTexte.setText("");
        action_filtrer(null);
    }

    public void fireModification() {
        if (ecouteurFX == null) {
            System.err.println("** ComboFiltrable/fireModification : Je n'ai pas d'écouteurFX :-(" +
                    ". Je laisse tomber !");
            return;
        }
        ecouteurFX.action_retour_component(selection, this, null);
    }

    @FXML
    public void action_filtrer(KeyEvent event) {
        String texte = Normalizer.normalize(tfTexte.getText().trim().toUpperCase(),
                Normalizer.Form.NFD).replaceAll("[\u0300-\u036F]", "");

        // Si grande liste et filtre trop court
        if (listeComplete.size() > SEUIL_GRANDE_LISTE &&
                texte.length() < MIN_CARACTERES_FILTRE) {
            Platform.runLater(() -> {
                combo.getItems().clear();
                combo.getSelectionModel().clearSelection();
            });
            if (ecouteurFX != null) {
                ecouteurFX.action_retour_component(null, this, event);
            }
            return;
        }

        if (texte.isEmpty()) {
            Platform.runLater(() -> {
                if (listeComplete.size() <= SEUIL_GRANDE_LISTE) {
                    combo.getItems().setAll(listeComplete);
                } else {
                    combo.getItems().clear();
                }
                combo.getSelectionModel().clearSelection();
            });
            if (ecouteurFX != null) {
                ecouteurFX.action_retour_component(null, this, event);
            }
        } else {
            TreeSet<Object> inclus = new TreeSet<>();
            int compteur = 0;

            for (Object elt : listeComplete) {
                if (compteur >= MAX_RESULTATS_COMBO) break;

                String selectionString = Normalizer.normalize(elt.toString().trim().toUpperCase(),
                        Normalizer.Form.NFD).replaceAll("[\u0300-\u036F]", "");

                if (selectionString.toUpperCase().startsWith(texte.toUpperCase())) {
                    inclus.add(elt);
                    compteur++;
                }
            }

            Platform.runLater(() -> {
                if (inclus.isEmpty()) {
                    combo.getItems().clear();
                    if (ecouteurFX != null) {
                        ecouteurFX.action_retour_component(null, this, event);
                    }
                } else {
                    combo.getItems().setAll(inclus);
                    selection = inclus.first();
                    combo.getSelectionModel().select(selection);
                    if (ecouteurFX != null) {
                        ecouteurFX.action_retour_component(selection, this, event);
                    }
                    if (event != null && event.getCode() == KeyCode.ENTER) {
                        tfTexte.setText(selection.toString());
                    } else {
                        tfTexte.requestFocus();
                    }
                }
            });
        }
    }

    @FXML
    private void action_selection_combo(Event event) {
        Platform.runLater(() -> {
            selection = combo.getSelectionModel().getSelectedItem();
            if (ecouteurFX == null) {
                tfTexte.setText(selection == null ? "" : selection.toString());
                System.out.println("Attention : le comboFiltrable ne connait pas son ecouteurFX !");
            } else {
                tfTexte.setText(selection == null ? "" : selection.toString());
                ecouteurFX.action_retour_component(selection, this, event);
            }
        });
    }

    public void setListe(Collection liste) {
        Platform.runLater(() -> {
            listeComplete.clear();
            listeComplete.addAll(liste);

            if (listeComplete.size() > SEUIL_GRANDE_LISTE) {
                combo.getItems().clear();
                listeOriginale.clear();
                tfTexte.setPromptText(String.format("Tapez min. %d caractères (%d éléments)",
                        MIN_CARACTERES_FILTRE, listeComplete.size()));
            } else {
                combo.getItems().setAll(liste);
                listeOriginale.clear();
                listeOriginale.addAll(liste);
            }

            clearSelection();
        });
    }

    public void setListeWithSelection(Collection liste, Object aSelectionner) {
        Platform.runLater(() -> {
            combo.getItems().setAll(liste);
            combo.getSelectionModel().select(aSelectionner);
            if (liste.contains(aSelectionner)) {
                selection = aSelectionner;
            }
            listeOriginale.clear();
            listeOriginale.addAll(liste);
        });
    }

    public void setListeKeepSelection(Collection liste) {
        Platform.runLater(() -> {
            combo.getItems().setAll(liste);
            if (selection != null) {
                combo.getSelectionModel().select(selection);
            } else {
                clearSelection();
            }
            listeOriginale.clear();
            listeOriginale.addAll(liste);
        });
    }

    ArrayList<Object> getListeComplete() {
        return new ArrayList<>(listeOriginale);
    }

    /**
     * Renvoie une copie de la liste affichée
     *
     * @return une copie de la liste affichée
     */
    public ArrayList<Object> getListeAffichee() {
        return new ArrayList<>(combo.getItems());
    }

    public void clearSelection() {
        selection = null;
        Platform.runLater(() -> {
            tfTexte.setText("");
            combo.getSelectionModel().clearSelection();
        });
    }

    public void clear() {
        setListe(new ArrayList());
        select(null);
        Platform.runLater(() -> {
            tfTexte.setText("");
        });
    }

    private ComboBox getCombo() {
        return combo;
    }

    private TextField getTextField() {
        return tfTexte;
    }

    public void setTexte(String texte) {
        Platform.runLater(() -> {
            tfTexte.setText(texte);
        });

    }

    public Object getSelection() {
        return combo.getSelectionModel().getSelectedItem();
    }

    /**
     * Sélectionne l'objet rentré
     *
     * @param object c'est l'élément à sélectionner s'il existe dans la liste,
     *               au sens de 'equals'
     * @return vrai si l'élément est trouvé (et sélectionné); faux sinon. Auquel
     * cas, on a un clearSelection qui est opéré
     */
    public boolean select(Object object) {
        if (getCombo().getItems().contains(object)) {
            Platform.runLater(() -> {
                tfTexte.setText(object.toString());
                getCombo().getSelectionModel().select(object);
            });

            selection = object;
            return true;
        }
        clearSelection();
        return false;
    }

    /**
     * Sélectionne l'objet rentré
     *
     * @param elementBD c'est l'élément à sélectionner s'il existe dans la liste, avec le même id
     * @return vrai si l'élément est trouvé (et sélectionné); faux sinon. Auquel
     * cas, on a un clearSelection qui est opéré
     */
    public boolean select_with_id(ObjetCSA elementBD) {
        for (Object x : getCombo().getItems()) {
            if (x instanceof ObjetCSA) {
                if (((ObjetCSA) x).getId() == elementBD.getId() &&
                        x.getClass().equals(elementBD.getClass())) {
                    Platform.runLater(() -> {
                        tfTexte.setText(x.toString());
                        getCombo().getSelectionModel().select(x);
                        selection = x;
                    });
                    return true;
                }
            }
        }

        clearSelection();
        return false;
    }

    public Integer getIndiceSelectionne() {
        return getCombo().getSelectionModel().getSelectedIndex();
    }

    public void selectIndex(int i) {
        Platform.runLater(() -> {
            combo.getSelectionModel().select(i);
        });
    }
}
