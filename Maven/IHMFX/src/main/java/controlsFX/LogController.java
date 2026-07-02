package controlsFX;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author didiertowe
 */
public class LogController extends BorderPane implements Initializable {

    @FXML
    private TextArea taConsole, taDetailsReseau;
    @FXML
    private Button btnEnregistrer;
    @FXML
    private Button btnEffacer;
    @FXML
    private Button btnRepere;
    @FXML
    private ListView<String> listeIO, listeOrphelins;

    private String nomFichier = "fichiers/consoleLog.txt";

    public LogController() {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/fxml/Log.fxml"));

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
        btnEffacer.setOnAction(this::action_effacer);
        btnEnregistrer.setOnAction(this::action_enregistrer);
        btnRepere.setOnAction(this::action_inserer_repere);
    }

    @FXML
    private void action_enregistrer(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog("consoleLog.txt");
        dialog.setTitle("Fichier de sauvegarde");
        dialog.setHeaderText("Veuillez rentrer le nom du fichier se sortie svp.");
        dialog.setContentText("Nom :");

// Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(s -> {
            nomFichier = s;
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(nomFichier));
                writer.write(taConsole.getText());

                //Close writer
                writer.close();
                printLog("Enregistrement effectué avec succès !");
            } catch (IOException e) {
                e.printStackTrace();
            }
            taConsole.appendText("\n> Sauvegarde du log dans le fichier : " + s);

        });
    }

    @FXML
    private void action_effacer(ActionEvent event) {
        taConsole.setText("");
    }

    @FXML
    private void action_clic_liste_IO(Event event) {
        String selection = listeIO.getSelectionModel().getSelectedItem();
        if (selection != null) {
            taDetailsReseau.setText("\nContenu de la requête :" + selection +
                    "\n*********************\n\n");
            return;
        }
        taDetailsReseau.setText("");
    }

    @FXML
    private void action_clic_liste_orphelins(Event event) {
        String selection = listeOrphelins.getSelectionModel().getSelectedItem();
        if (selection != null) {
            taDetailsReseau.setText("\nContenu de la requête :" + selection +
                    "\n*********************\n\n");
            return;
        }
        taDetailsReseau.setText("");
    }

    @FXML
    private void action_inserer_repere(ActionEvent event) {
        taConsole.appendText("\n***\n*****************************************************\n***\n");
    }

    public void printLog(String texte) {
        LocalDate date = LocalDate.now();
        LocalTime heure = LocalTime.now();
        taConsole.appendText("\n\n> " + date.getDayOfMonth() + "/" + date.getMonth() + "/" + date.getYear()
                + " - " + heure.getHour() + ":" + heure.getMinute()
                + "\n" + texte);
    }

    public TextArea getTextArea() {
        return taConsole;
    }

    public void afficher_information(String activiteReseau, ArrayList<String> orphelins) {
        Platform.runLater(() -> {
            listeIO.getItems().add(activiteReseau);
            if (listeIO.getItems().size() > 2000) {
                listeIO.getItems().remove(0, 9);
            }

            listeOrphelins.getItems().setAll(orphelins);
        });
    }

}
