package controlsFX;

import general.GestionnaireImbrications;
import general.ObjetCSA;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class CombosImbriques extends TitledPane {
    GestionnaireImbrications gestionnaireImbrications = null;
    private ComboEditableController<ObjetCSA> cmbePays = new ComboEditableController<>(ObjetCSA.class);
    private ComboEditableController<ObjetCSA> cmbeVille = new ComboEditableController<>(ObjetCSA.class);
    private ComboEditableController<ObjetCSA> cmbeQuartier = new ComboEditableController<>(ObjetCSA.class);
    private Label lblPays = new Label("Pays");
    private Label lblVille = new Label("Ville");
    private Label lblQuartier = new Label("Quartier");
    private TextField textField = new TextField();
    private Label lblAdresse = new Label(" --- ");

    public CombosImbriques() {
        setText("Non initialisé");
        setPadding(new Insets(5, 5, 5, 5));

        VBox box = new VBox(10);
        // Add this to ensure content has proper size
        box.setMinHeight(Region.USE_PREF_SIZE);
        box.setPrefHeight(Region.USE_COMPUTED_SIZE);

        HBox boxAdresse = new HBox(10);
        boxAdresse.getChildren().add(lblAdresse);
        boxAdresse.getChildren().add(textField);

        HBox boxCombos = new HBox(10);
        VBox boxPays = new VBox();
        boxPays.getChildren().add(cmbePays);
        boxPays.getChildren().add(lblPays);
        boxCombos.getChildren().add(boxPays);

        VBox boxVille = new VBox();
        boxVille.getChildren().add(cmbeVille);
        boxVille.getChildren().add(lblVille);
        boxCombos.getChildren().addAll(boxVille);

        VBox boxQuartier = new VBox();
        boxQuartier.getChildren().add(cmbeQuartier);
        boxQuartier.getChildren().add(lblQuartier);
        boxCombos.getChildren().add(boxQuartier);

        box.getChildren().add(boxAdresse);
        box.getChildren().add(boxCombos);

        setContent(box);
    }

    public void setLabelPays(String texte) {
        Platform.runLater(() -> {
            lblPays.setText(texte);
        });
    }

    public void setLabelAdresse(String texte) {
        Platform.runLater(() -> {
            lblAdresse.setText(texte);
        });
    }

    public void setTextFieldAdresse(TextField textFieldAdresse) {
        textField = textFieldAdresse;
    }

    public void setLabelVille(String texte) {
        Platform.runLater(() -> {
            lblVille.setText(texte);
        });
    }

    public void setLabelQuartier(String texte) {
        Platform.runLater(() -> {
            lblQuartier.setText(texte);
        });
    }

    public void charger_donnees(GestionnaireImbrications gestionnaire) {
        gestionnaireImbrications = gestionnaire;
        String nomCle = gestionnaire.getNomPublic();
        ObjetCSA pays = gestionnaire.getPays();
        ObjetCSA ville = gestionnaire.getVille();
        ObjetCSA quartier = gestionnaire.getQuartier();

        Platform.runLater(() -> {
            CombosImbriques.this.setText(nomCle);
        });
        cmbePays.setListeWithSelection(gestionnaire.getListePays(), pays);
        if (ville == null) {
            Platform.runLater(() -> {
                cmbeQuartier.setVisible(false);
                cmbeVille.setVisible(false);
            });
        } else {
            cmbeVille.setListeWithSelection(gestionnaire.getListeVilles(), ville);
            Platform.runLater(() -> {
                cmbeVille.setVisible(true);
                cmbeQuartier.setVisible(quartier != null);
            });
            if (quartier != null) {
                cmbeQuartier.setListeWithSelection(gestionnaire.getListeQuartiers(), quartier);
            }
        }

    }

    public GestionnaireImbrications getGestionnaire() {
        Object pays = cmbePays.getSelection();
        ArrayList<Object> listePays = cmbePays.getListeComplete();
        Object ville = cmbeVille.getSelection();
        ArrayList<Object> listeVilles = cmbeVille.getListeComplete();
        Object quartier = cmbeQuartier.getSelection();
        ArrayList<Object> listeQuartiers = cmbeQuartier.getListeComplete();
        if (pays instanceof ObjetCSA) {
            if (quartier instanceof ObjetCSA && ville instanceof ObjetCSA) {
                gestionnaireImbrications.setNewData((ObjetCSA) pays,
                        listePays.stream()
                                .map(ObjetCSA.class::cast)
                                .collect(Collectors.toCollection(ArrayList::new)),
                        (ObjetCSA) ville,
                        listeVilles.stream()
                                .map(ObjetCSA.class::cast)
                                .collect(Collectors.toCollection(ArrayList::new)),
                        (ObjetCSA) quartier,
                        listeQuartiers.stream()
                                .map(ObjetCSA.class::cast)
                                .collect(Collectors.toCollection(ArrayList::new)));
                return gestionnaireImbrications;
            }
            if (ville instanceof ObjetCSA) {
                gestionnaireImbrications.setNewData((ObjetCSA) pays,
                        listePays.stream()
                                .map(ObjetCSA.class::cast)
                                .collect(Collectors.toCollection(ArrayList::new)),
                        (ObjetCSA) ville,
                        listeVilles.stream()
                                .map(ObjetCSA.class::cast)
                                .collect(Collectors.toCollection(ArrayList::new)),
                        null, new ArrayList<>());
                return gestionnaireImbrications;
            }
            return null;
        }
        return null;
    }
}
