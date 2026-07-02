/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controlsFX;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author didiertowe
 */
public class PanneauListingController implements Initializable {

    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;
    @FXML
    private Button btnRafraichir;
    @FXML
    private TextField tfFiltre;
    @FXML
    private ListView<?> listeClients;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    @FXML
    private void action_ajouter(ActionEvent event) {
    }

    @FXML
    private void action_modifier(ActionEvent event) {
    }

    @FXML
    private void action_supprimer(ActionEvent event) {
    }

    @FXML
    private void action_selection_client(MouseEvent event) {
    }

    public void desactiver_bouton(Button bouton) {
        bouton.setVisible(false);
    }

    public void activer_bouton(Button bouton) {
        bouton.setVisible(true);
    }

    public Button getBtnAjouter() {
        return btnAjouter;
    }

    public Button getBtnModifier() {
        return btnModifier;
    }

    public Button getBtnSupprimer() {
        return btnSupprimer;
    }

    public Button getBtnRafraichir() {
        return btnRafraichir;
    }

    public TextField getTfFiltre() {
        return tfFiltre;
    }

    public ListView<?> getListeClients() {
        return listeClients;
    }
}
