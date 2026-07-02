/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mainIHMFX;

import controlsFX.ComboFiltrableController;
import controlsFX.DoubleListeController;
import controlsFX.EcouteurFX;
import general.Individu;
import general.ObjetCSA;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author didier
 */
public class SampleController implements Initializable, EcouteurFX {

    private static ArrayList<Individu> ali = new ArrayList<>();
    @FXML
    private Label lblCommentaires;
    @FXML
    private ComboFiltrableController comboF;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ali.add(new Individu() {

            @Override
            public ObjetCSA getParent() {
                return null;
            }

            @Override
            public ObjetCSA getObjetFromType(String s) {
                return null;
            }

            @Override
            public void setParent(ObjetCSA objetDi) {

            }


        });
        ali.add(new Individu() {

            @Override
            public ObjetCSA getParent() {
                return null;
            }

            @Override
            public ObjetCSA getObjetFromType(String s) {
                return null;
            }

            @Override
            public void setParent(ObjetCSA objetDi) {

            }


        });
        ali.add(new Individu() {

            @Override
            public ObjetCSA getParent() {
                return null;
            }

            @Override
            public ObjetCSA getObjetFromType(String s) {
                return null;
            }

            @Override
            public void setParent(ObjetCSA objetDi) {

            }


        });
        ali.add(new Individu() {

            @Override
            public ObjetCSA getParent() {
                return null;
            }

            @Override
            public ObjetCSA getObjetFromType(String s) {
                return null;
            }

            @Override
            public void setParent(ObjetCSA objetDi) {

            }


        });
        comboF.setEcouteurFX(this);
        comboF.setListe(ali);
    }

    @Override
    public void action_retour_component(Object o, Node cfc, Object event) {
        if (cfc instanceof ComboFiltrableController) {
            Object selection = comboF.getSelection();
            if (selection == null) {
                lblCommentaires.setText("Aucune sélection");
            } else {
                lblCommentaires.setText("Élément choisi : " + selection);
            }
            return;
        }
        if (cfc instanceof DoubleListeController) {
            System.err.println("** DoubleListe non implémentée dans le sampleController");
        }
    }

}
