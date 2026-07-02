package generalFX;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class ListPopupController extends BorderPane implements Initializable {

    BoutonListe mom;
    private Stage dialog;
    @FXML
    private Button btnAnnuler;
    @FXML
    private Button btnOk;
    @FXML
    private ListView<?> listview;

    public ListPopupController() {
        FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource("/fxml/ListPopup.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException var3) {
            throw new RuntimeException(var3);
        }
    }

    public void initialize(URL url, ResourceBundle rb) {
        this.listview.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setScene(new Scene(this));
        //this.dialog.initStyle(StageStyle.UNDECORATED);
    }

    @FXML
    private void action_annuler(ActionEvent event) {
        if (this.mom != null) {
            this.mom.setSelectionnes(FXCollections.emptyObservableList());
        }

        this.dialog.close();
    }

    @FXML
    private void action_ok(ActionEvent event) {
        ObservableList selectionnes = this.listview.getSelectionModel().getSelectedItems();
        int taille;
        if (selectionnes.isEmpty()) {
            taille = this.listview.getItems().size();
            if (this.mom != null) {
                this.mom.setText(taille + " elt" + (taille > 1 ? "s" : ""));
            }
        } else {
            taille = selectionnes.size();
            if (this.mom != null) {
                this.mom.setText(taille + " selection" + (taille > 1 ? "s" : ""));
            }
        }

        if (this.mom != null) {
            this.mom.setSelectionnes(selectionnes);
        }

        this.dialog.close();
    }

    public void setListe(ArrayList liste) {
        this.listview.getItems().setAll(liste);
    }

    void show(Node mere) {
        if (mere instanceof BoutonListe) {
            this.mom = (BoutonListe) mere;
        }

        dialog.show();
    }
}
