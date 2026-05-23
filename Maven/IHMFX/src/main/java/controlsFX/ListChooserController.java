package controlsFX;

import general.ObjetCSA;
import generalFX.GestionnaireListesFX;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;

import java.util.List;

public class ListChooserController extends EditorPaneController {
    private final ListView<ObjetCSA> listView = new ListView<>();

    @Override
    public void charger_donnees(String prefixe, ObjetCSA elementBD) {
        monObjet = elementBD;
        editeurPersonnel = null;

        appeler_bd(4, monObjet, new GestionnaireListesFX(listView));

        Platform.runLater(() -> {
            lblEntete.setText(elementBD.getPublicName());
            listView.getSelectionModel().select(elementBD);
            ScrollPane scrollPane = new ScrollPane(listView);
            scrollPane.setFitToWidth(true);  // Crucial for VBox content
            centerPane.setCenter(scrollPane);
        });
    }

    @Override
    protected void action_enregistrer(ActionEvent event) {
        Platform.runLater(() -> {
            monObjet = listView.getSelectionModel().getSelectedItem();
            // seul les objest sont importants; c'est le maitreFX qui saura quoi en faire
            List<ObjetCSA> selections = listView.getSelectionModel().getSelectedItems();
            mom.appeler_bd(-19, selections, null);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.close();
        });
    }
}
