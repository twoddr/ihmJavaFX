package mainIHMFX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import controlsFX.ComboEditableController;
import controlsFX.ComboFiltrableController;
import controlsFX.DateBoxController;
import controlsFX.DoubleListeController;
import controlsFX.EcouteurFX;
import controlsFX.EditorPaneController;
import controlsFX.ListTableBDController;
import controlsFX.ListeBDController;
import controlsFX.ListeFiltrableController;
import controlsFX.LogController;
import controlsFX.MaitreFX;
import controlsFX.TableViewCRUD;
import controlsFX.TreeCheckBoxListController;
import controlsFX.TreeViewBDController;
import controlsFX.TreeViewPlus;
import controlsFX.Calendrier.AgendaControlable;
import general.GestionnaireRetour;
import general.Individu;
import general.ObjetCSA;
import generalFX.FX_RGBConverter;
import generalFX.GenerateurColonnesTableView;
import generalFX.ObjetIHM;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import mainIHMFX.objets.Article;
import mainIHMFX.objets.GenerateurObjets;
import mainIHMFX.objets.MedecinTraitant;
import mainIHMFX.objets.Voiture;

public class TestFX extends Application implements MaitreFX, EcouteurFX {

    public ComboFiltrableController comboFiltrable;
    public ListeFiltrableController listeFiltrable;
    public ListeBDController listeBD;
    public DoubleListeController doubleListe;
    public ListTableBDController listeTableBD;
    public TreeViewBDController treeViewBD;
    public Tab tabAgenda;
    public AgendaControlable agenda;
    public TreeCheckBoxListController treeCheckList;
    public ComboEditableController<Individu> comboEditable;
    public Pane panComboEditable;
    public TreeViewPlus treeViewPlus;
    public LogController logController;
    public DateBoxController dateBox;
    public EditorPaneController editorPane;
    public TableViewCRUD tableViewCRUD;
    public TabPane tabPane;
    public BorderPane tableViewCRUDBorderPane;
    private Parent root;

    @Override
    public void start(Stage stage) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TestFX.fxml"));
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Scene scene = new Scene(root, 1080, 768);
        scene.getStylesheets().add("/styles/style.css");

        stage.setTitle("Fenêtre de test FXStandard");
        stage.setScene(scene);

        // initialize();
        // stage.setMaximized(true);

        stage.show();
        stage.setOnCloseRequest((WindowEvent t) -> {
            Platform.exit();
            System.exit(0);
        });

    }

    public void initialize() {
        comboEditable = new ComboEditableController<>(Individu.class);
        panComboEditable.getChildren().add(comboEditable);
        listeTableBD.setMaitre(this);
        treeViewBD.setMaitre(this);
        treeViewPlus.setMaitre(this);
        treeCheckList.setEcouteurFX(this);
        ArrayList<ObjetCSA> personnes = new ArrayList<>();
        Individu didier = new Individu("Didier", "Towe", "didi@gmail.be");
        didier.setId(2);
        didier.setCouleur(FX_RGBConverter.convert_color_2_rgb(Color.PINK));
        Individu cyril = new Individu("Cyril", "Towe", "cycy@gmail.be");
        cyril.setId(23);
        cyril.setCouleur(FX_RGBConverter.convert_color_2_rgb(Color.GAINSBORO));
        Individu maud = new Individu("Maud", "Lonneux", "lomau@etik.com");
        maud.setId(3);
        maud.setCouleur(FX_RGBConverter.convert_color_2_rgb(Color.CYAN));
        maud.add_variable_a_afficher("couleur");
        Individu levis = new Individu("Elvir", "Nana", "nanae@etik.com");
        levis.setId(1);
        levis.setCouleur(FX_RGBConverter.convert_color_2_rgb(Color.GREEN));
        levis.add_variable_a_afficher("couleur");

        maud.setParent(didier);
        personnes.add(didier);
        personnes.add(maud);
        personnes.add(levis);
        personnes.add(cyril);
        personnes.add(new Individu("Louis de Gonzalgues", "Atangana", ""));

        comboFiltrable.setListe(personnes);
        comboEditable.setListeWithSelection(personnes, maud);
        // comboEditable.select(maud);
        comboEditable.setMaitre(this);
        listeBD.chargerListe(new ArrayList<>(personnes));
        listeFiltrable.chargerListe(new ArrayList<>(personnes));
        listeTableBD.charger_liste(new ArrayList<>(convertir_liste(personnes)), "Individu");
        treeViewBD.charger_liste(new ArrayList<>(convertir_liste(personnes)), "Individu");
        treeViewPlus.charger_liste(new ArrayList<>(convertir_liste(personnes)), "Individu");
        String[] nomChampsEvenement = { "EvenementMMO", "id", "description", "couleurTexte",
                "idPersonnel", "debut", "fin", "couleurCadre",
                "couleurFond", "lieu" };
        agenda = new AgendaControlable(new ArrayList<>(Arrays.asList(nomChampsEvenement)));
        tabAgenda.setContent(agenda);
        treeCheckList.setListe(personnes);

        logController.printLog("Lancement réussi de la fenêtre log");
        logController.printLog("Liste des noms : \n" + listeBD.getListeComplete());
        logController.afficher_information("Nom activité réseau ?", new ArrayList<>());

        editorPane.setMaitre(this, null);
        LinkedHashMap<String, Node> pairesNomChamps = new LinkedHashMap<>();
        ComboEditableController<MedecinTraitant> cmbeMedecins = new ComboEditableController<>(MedecinTraitant.class);
        ArrayList<MedecinTraitant> docs = new ArrayList<>();
        docs.add(GenerateurObjets.getDoc());
        docs.add(GenerateurObjets.getDoc3());
        docs.add(GenerateurObjets.getDoc2());
        cmbeMedecins.setListe(docs);
        // pairesNomChamps.put("fkmedecin", cmbeMedecins);
        DoubleListeController doubleListe = new DoubleListeController();
        ArrayList<Voiture> voitures = GenerateurObjets.getListeVoitures();
        voitures.addAll(GenerateurObjets.getListeVoitures2());
        doubleListe.setListeComplete(voitures);
        pairesNomChamps.put("Voiture", doubleListe);
        TableView<ObjetIHM> tableau = new TableView<>();
        GenerateurColonnesTableView generateurColonnesTableView = new GenerateurColonnesTableView(tableau,
                string -> {
                    logController.printLog(string);
                },
                (objetCSA, nomChamp) -> {
                    if (nomChamp.equals("fkmedecin")) {
                        MedecinTraitant medecin = (MedecinTraitant) objetCSA;
                        cmbeMedecins.select(medecin);
                    }
                });
        generateurColonnesTableView.setColumn(new ObjetIHM(new Article()));
        pairesNomChamps.put("Article", tableau);

        ObjetCSA objetCSA = GenerateurObjets.createSampleObject();
        editorPane.charger_donnees("", objetCSA);

        tableViewCRUD = new TableViewCRUD();
        // tableViewCRUD.setContentFree(true);
        tableViewCRUD.charger_liste(new ArrayList<>(voitures), new Voiture());
        tableViewCRUDBorderPane.setCenter(tableViewCRUD);
    }

    private ArrayList<? extends ObjetIHM> convertir_liste(ArrayList<ObjetCSA> hashObjets) {
        ArrayList<ObjetIHM> terug = new ArrayList<>();
        hashObjets.forEach(hashObjet -> {
            terug.add(new ObjetIHM(hashObjet));
        });
        return terug;
    }

    public ObjetCSA create_filled_objetCSA() {
        Individu didier = new Individu("Towe", "Didier", "didi@gmail.be");
        didier.setId(2);
        didier.setCouleur(FX_RGBConverter.convert_color_2_rgb(Color.PINK));

        Individu maud = new Individu("Lonneux", "Maud", "lomau@etik.com");
        maud.setId(3);
        maud.setCouleur(FX_RGBConverter.convert_color_2_rgb(Color.CYAN));
        maud.add_variable_a_afficher("couleur");
        Individu levis = new Individu("Elvir", "Nana", "nanae@etik.com");
        levis.setId(1);
        levis.setCouleur(FX_RGBConverter.convert_color_2_rgb(Color.GREEN));
        levis.add_variable_a_afficher("couleur");

        return didier;
    }

    @Override
    public ObjetCSA getElementFromType(String typeAffiche) {
        ObjetCSA terug = new GenerateurObjets().getObjetFromType(typeAffiche);
        if (terug != null) {
            return terug;
        }
        return new Individu() {

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

        };
    }

    @Override
    public void action_utilisateur(Object selectedItem, Object source, String x) {
        System.out.println("\nTestFX reçoit une action utilisateur de " + source +
                "\n-> avec comme objet : " + selectedItem +
                "\n-> et comme option : " + x);
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

    @Override
    public void rafraichir() {

    }

    @Override
    public boolean isTest() {
        return true;
    }

    @Override
    public void appeler_bd(int code, Object objet, GestionnaireRetour gestionnaireRetour) {
        System.out.println("\nTestFX reçoit un appel BD pour " + gestionnaireRetour +
                "\n-> avec comme objet : " + objet +
                "\n-> et comme code : " + code);
    }

    @Override
    public void affiche_utilisateur(String message) {

    }

    @Override
    public void affiche_console(String message) {

    }

    @Override
    public void action_retour_component(Object selection, Node source, Object infoSupplementaire) {
        System.out.println("\nL'écouteur (main) reçoit un appel de " + source + " avec comme objet " + selection +
                " et comme info supplémentaire : " + infoSupplementaire);
    }
}
