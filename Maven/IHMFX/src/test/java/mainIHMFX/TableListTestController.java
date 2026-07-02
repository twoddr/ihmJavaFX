package mainIHMFX;

import controlsFX.ComboFiltrableController;
import controlsFX.ListTableBDController;
import controlsFX.MaitreFX;
import general.GestionnaireRetour;
import general.ObjetCSA;
import generalFX.ObjetIHM;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

public class TableListTestController extends BorderPane implements MaitreFX {
    public ListTableBDController listTable;
    public Label lblCommentaires;

    public TableListTestController() {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("../../../main/resources/fxml/TableListTest.fxml"));

        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void charger_liste(String nomType,
                              Object combo_ou_liste,
                              ArrayList<ObjetIHM> coquilles) {

        // listes des champs et composants
        LinkedHashMap<String, Node> paires_nom_champs = new LinkedHashMap<>();
        paires_nom_champs.put("nom", new TextField());
        paires_nom_champs.put("prenom", new TextField());
        paires_nom_champs.put("dateNaissance", new DatePicker());
        paires_nom_champs.put("tva", new TextField());
        paires_nom_champs.put("adresse", new TextField());
        ComboBox<Integer> comboBox = new ComboBox<>();
        Integer[] cps = {1000, 2000, 3000, 4000, 4870, 5000};
        comboBox.getItems().addAll(Arrays.asList(cps));
        paires_nom_champs.put("codePostal", comboBox);
        ArrayList<ObjetCSA> localites = new ArrayList<>();
//        Localite bruxelles = new Localite("Bruxelles", 1000);
//        bruxelles.setId(2);
//        Localite anvers = new Localite("Anvers", 2000);
//        anvers.setId(3);
//        Localite hasselt = new Localite("Hasselt", 3000);
//        Localite trooz = new Localite("Trooz", 4870);
//        trooz.setId(1);
//        Localite namur = new Localite("Namur", 5000);
//        namur.setId(5);
//        localites.add(bruxelles);
//        localites.add(anvers);
//        localites.add(trooz);
//        localites.add(namur);
//        localites.add(hasselt);
//        localites.add(new Localite("Liège", 4000));
        ComboFiltrableController cmbf = new ComboFiltrableController();
        cmbf.setListe(localites);
        paires_nom_champs.put("idLocalite", cmbf);
        paires_nom_champs.put("courriel", new TextField());
        paires_nom_champs.put("", new Label());
        paires_nom_champs.put("gsm", new TextField());

        listTable.setMaitre(this);
        listTable.charger_liste(new ArrayList<>(coquilles),
                coquilles.isEmpty() ? "ElementBD" : coquilles.get(0).getClass().getSimpleName());
    }


    @Override
    public void appeler_bd(int code, Object objet, GestionnaireRetour gestionnaireRetour) {

    }


    @Override
    public void affiche_utilisateur(String texte) {
        lblCommentaires.setText(texte);
    }

    @Override
    public void affiche_console(String texte) {
        System.out.println(texte);
    }


    @Override
    public ObjetCSA getElementFromType(String typeAffiche) {
        return new MyBDObject("Object", "id", "nom");
    }


    @Override
    public void action_utilisateur(Object selectedItem, Object source, String x) {
        System.err.println("TableListTestController ne gère pas les actions" +
                " utilisateur de ses enfants !");
    }

    @Override
    public void affiche_details(Object selectedItem) {

    }

    @Override
    public void nettoyer_champs() {

    }

    @Override
    public void charger_donnees_vers_enfant(ObjetCSA hashObjet) {

    }

    public void modifier(ArrayList<ObjetCSA> liste) {
        lblCommentaires.setText("Demande d'enregistrement bien reçue :-)");
        System.out.println("Enregistrement de : " + liste);
    }

    public void ajouter(ArrayList<ObjetCSA> liste) {
        lblCommentaires.setText("Demande d'enregistrement bien reçue :-)");
        System.out.println("Enregistrement de : " + liste);
    }

    public void rafraichir() {
        lblCommentaires.setText("Demande de rafraichissement reçue :-)");
    }


    @Override
    public boolean isTest() {
        return true;
    }

    public void supprimer(ArrayList<ObjetCSA> selections) {
        lblCommentaires.setText("Suppression demandée :-)");
        System.out.println("Supression demandée pour : " + selections);
    }


    class MyBDObject extends ObjetCSA {

        /**
         * Pour créer un objetIHM, il faut lui donner les noms des variables nécessaires
         * Les informations nécessaires permettent d'identifier, dans l'hashmap de données,
         * quelles sont celles qu'on observe généralement dans une interface graphique
         * L'id et l'id intermédiaire sont utiles lors des sélections et échanges de liste...
         *
         * @param nomType  le nom de la classe de l'objet représenté
         * @param nomID    le nom de la variable qui stocke l'id
         * @param nomObjet le nom du nom de la variable nom
         */
        public MyBDObject(String nomType, String nomID, String nomObjet) {
            super(nomType, nomID, nomObjet);
        }

        @Override
        public ObjetCSA getParent() {
            return null;
        }

        @Override
        public void setParent(ObjetCSA objetDi) {

        }


        @Override
        public ObjetCSA getObjetFromType(String s) {
            return null;
        }

        @Override
        public void reinitialiser() {

        }

    }
}
