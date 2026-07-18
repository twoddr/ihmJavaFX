package mainIHMFX;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import controlsFX.ComboEditableController;
import general.GestionnaireImbrications;
import general.ObjetCSA;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import mainIHMFX.objets.Contact;
import mainIHMFX.objets.MedecinTraitant;
import mainIHMFX.objets.Quidam;
import mainIHMFX.objets.Specialite;
import mainIHMFX.objets.Voiture;

/*
 ******** DeepSeek suggestion
 */
public class EnhancedHashMapViewer extends Application {

    private Map<String, Object> currentData;
    private ObjetCSA monObjet;

    public static void main(String[] args) {
        launch(args);
    }

    private static MedecinTraitant getDoc() {
        MedecinTraitant doc = new MedecinTraitant();
        doc.inserer_valeur("prenommed", "Hector");
        doc.setNom("Denis");

        Contact adrCabinet = new Contact();
        adrCabinet.setId(2);
        adrCabinet.setNom("Avenue Amadou Ahidjo 123");
        adrCabinet.inserer_valeur("email", "alamimboudnd@hotmail.cm");
        adrCabinet.inserer_valeur("tel1", "098765432");
        adrCabinet.inserer_valeur("tel2", "+1234567890");
        doc.inserer_fk("fkcontactcabinet", adrCabinet);
        Contact adrHopital = new Contact();
        adrHopital.setId(3);
        adrHopital.setNom("Dernier virage 43");
        adrHopital.inserer_valeur("email", "docteur_alamimbou@chy.cm");
        adrHopital.inserer_valeur("tel1", "0532");
        adrHopital.inserer_valeur("tel2", "+4567");
        doc.inserer_fk("fkcontacthopital", adrHopital);

        Specialite specialite = new Specialite();
        specialite.setNom("Gynécologie");
        specialite.inserer_valeur("prix", 23f);
        specialite.inserer_valeur("nomcourtspec", "GYN");
        doc.inserer_fk("fkspecialite", specialite);
        return doc;
    }

    @Override
    public void start(Stage primaryStage) {
        currentData = createSampleData();
        monObjet = createSampleObject();

        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new javafx.geometry.Insets(10));

        // Header with refresh button
        HBox header = new HBox(10);
        Button refreshBtn = new Button("Refresh");
        Button saveBtn = new Button("Save Changes");
        header.getChildren().addAll(refreshBtn, saveBtn);

        ScrollPane scrollPane = new ScrollPane();
        VBox formContainer = new VBox(10);
        formContainer.setPadding(new javafx.geometry.Insets(10));

        // buildDynamicForm(formContainer, currentData, "Root");
        buildCSADynamicForm(formContainer, monObjet);

        scrollPane.setContent(formContainer);
        scrollPane.setFitToWidth(true);

        refreshBtn.setOnAction(e -> {
            formContainer.getChildren().clear();
            // buildDynamicForm(formContainer, currentData, "Root");
            buildCSADynamicForm(formContainer, monObjet);
        });

        saveBtn.setOnAction(e -> {
            // Here you would implement saving logic
            System.out.println("Data would be saved here");
            // showDataSummary(currentData);
            showDataSummary(monObjet);
        });

        mainLayout.getChildren().addAll(header, scrollPane);

        Scene scene = new Scene(mainLayout, 700, 500);
        primaryStage.setTitle("Enhanced HashMap Viewer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void buildDynamicForm(VBox container, Map<String, Object> data, String title) {
        TitledPane titledPane = new TitledPane();
        titledPane.setText(title);
        titledPane.setExpanded(true);

        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(10));

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedData = (Map<String, Object>) value;
                buildDynamicForm(content, nestedData, key);
            } else if (value instanceof List) {
                Accordion listAccordion = new Accordion();
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;

                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemData = (Map<String, Object>) item;
                        VBox itemContainer = new VBox(5);
                        buildDynamicForm(itemContainer, itemData, "Item " + (i + 1));
                        TitledPane itemPane = new TitledPane("Item " + (i + 1), itemContainer);
                        listAccordion.getPanes().add(itemPane);
                    } else {
                        HBox simpleItem = new HBox(10);
                        simpleItem.getChildren().addAll(
                                new Label("Value:"),
                                createInputField(item, data, key, i));
                        listAccordion.getPanes().add(new TitledPane("Item " + (i + 1), simpleItem));
                    }
                }

                VBox listContainer = new VBox(5);
                listContainer.getChildren().addAll(
                        new Label(key + " (List):"),
                        listAccordion);
                content.getChildren().add(listContainer);
            } else {
                HBox fieldRow = new HBox(10);
                fieldRow.getChildren().addAll(
                        new Label(key + ":"),
                        createInputField(value, data, key, -1));
                content.getChildren().add(fieldRow);
            }
        }

        titledPane.setContent(content);
        container.getChildren().add(titledPane);
    }

    private void buildListDynamicForm(VBox container, ObjetCSA objetCSA) {
        TitledPane titledPane = new TitledPane();
        titledPane.setText("Listes de " + objetCSA);
        titledPane.setExpanded(true);

        Accordion listAccordion = new Accordion();
        listAccordion.setPadding(new javafx.geometry.Insets(10));

        for (Map.Entry<String, Object> entry : objetCSA.getHashListes().entrySet()) {
            String nomTable = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List list = (List) value;

                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    ListView<ObjetCSA> listView = new ListView<>();
                    listView.getItems().setAll(list);
                    TitledPane itemPane = new TitledPane((i + 1) + ") " + nomTable, listView);
                    listAccordion.getPanes().add(itemPane);
                }
            } else {
                System.err.println("L'objet " + objetCSA + " est supposé contenir une liste de " + nomTable +
                        ", mais je reçois " + value);
            }
        }

        titledPane.setContent(listAccordion);
        container.getChildren().add(titledPane);
    }

    private void buildCSADynamicForm(VBox container, ObjetCSA objetCSA) {
        TitledPane titledPane = new TitledPane();
        titledPane.setText(objetCSA.getPublicName());
        titledPane.setExpanded(true);

        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(10));

        // Regular data (donnees et donnees non-attendues)
        buildSimpleElements(content, objetCSA);

        // add intermediaire
        content.getChildren().add(new Separator());
        for (Map.Entry<String, ObjetCSA> couple : objetCSA.getFKIs().entrySet()) {
            buildSimpleElements(content, couple.getValue());
        }

        // add dependants
        for (Map.Entry<String, ObjetCSA> paire : objetCSA.getHashDependants().entrySet()) {
            buildCSADynamicForm(content, paire.getValue());
        }

        // add fk
        for (Map.Entry<String, ObjetCSA> paire : objetCSA.getFKs().entrySet()) {
            buildCSADynamicForm(content, paire.getValue());
        }

        // add imbriqués
        for (Map.Entry<String, GestionnaireImbrications> paire : objetCSA.getGestionnairesImbrications().entrySet()) {
            buildImbricationDynamicForm(content, paire.getValue());
        }

        // add listes
        buildListDynamicForm(content, objetCSA);

        titledPane.setContent(content);
        container.getChildren().add(titledPane);
    }

    private void buildImbricationDynamicForm(VBox content, GestionnaireImbrications gestionnaire) {
        String nomCle = gestionnaire.getNomPublic();
        ObjetCSA pays = gestionnaire.getPays();
        ObjetCSA ville = gestionnaire.getVille();
        ObjetCSA quartier = gestionnaire.getQuartier();

        HBox fieldRow = new HBox(10);
        VBox boxPays = new VBox();
        ComboEditableController<ObjetCSA> cmbePays = new ComboEditableController<>(ObjetCSA.class);
        cmbePays.setListeWithSelection(gestionnaire.getListePays(), pays);
        boxPays.getChildren().add(cmbePays);
        boxPays.getChildren().add(new Label("Pays"));
        fieldRow.getChildren().add(boxPays);
        if (ville != null) {
            VBox boxVille = new VBox();
            ComboEditableController<ObjetCSA> cmbeVille = new ComboEditableController<>(ObjetCSA.class);
            cmbeVille.setListeWithSelection(gestionnaire.getListeVilles(), ville);
            boxVille.getChildren().add(cmbeVille);
            boxVille.getChildren().add(new Label("Ville"));
            fieldRow.getChildren().addAll(boxVille);
            if (quartier != null) {
                VBox boxQuartier = new VBox();
                ComboEditableController<ObjetCSA> cmbeQ = new ComboEditableController<>(ObjetCSA.class);
                cmbeQ.setListeWithSelection(gestionnaire.getListeQuartiers(), quartier);
                boxQuartier.getChildren().add(cmbeQ);
                boxQuartier.getChildren().add(new Label("Quartier"));
                fieldRow.getChildren().add(boxQuartier);
            }
        }
        TitledPane titledPane = new TitledPane(nomCle, fieldRow);
        content.getChildren().add(titledPane);
    }

    private void buildSimpleElements(VBox content, ObjetCSA objetCSA) {
        for (Map.Entry<String, Object> entry : objetCSA.getSimpleData().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String nomCle = objetCSA.getNomPublic(key);
            if (nomCle == null || nomCle.equals("*")) {
                nomCle = key;
            }

            HBox fieldRow = new HBox(10);
            fieldRow.getChildren().addAll(
                    new Label(nomCle + " :"),
                    createInputField(value, objetCSA, key, -1));
            content.getChildren().add(fieldRow);
        }

    }

    private Control createInputField(Object value, Map<String, Object> parent, String key, int index) {
        Control field;

        if (value instanceof String) {
            TextField textField = new TextField((String) value);
            textField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (index == -1) {
                    parent.put(key, newVal);
                } else {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) parent.get(key);
                    list.set(index, newVal);
                }
            });
            field = textField;
        } else if (value instanceof Integer) {
            TextField numberField = new TextField(value.toString());
            numberField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*")) {
                    numberField.setText(newVal.replaceAll("[^\\d]", ""));
                }
                try {
                    int numValue = Integer.parseInt(newVal);
                    if (index == -1) {
                        parent.put(key, numValue);
                    } else {
                        @SuppressWarnings("unchecked")
                        List<Object> list = (List<Object>) parent.get(key);
                        list.set(index, numValue);
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid numbers temporarily
                }
            });
            field = numberField;
        } else if (value instanceof Boolean) {
            CheckBox checkBox = new CheckBox();
            checkBox.setSelected((Boolean) value);
            checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (index == -1) {
                    parent.put(key, newVal);
                } else {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) parent.get(key);
                    list.set(index, newVal);
                }
            });
            field = checkBox;
        } else {
            TextField textField = new TextField(value != null ? value.toString() : "");
            field = textField;
        }

        field.setMinWidth(200);
        return field;
    }

    private Control createInputField(Object value, ObjetCSA parent, String key, int index) {
        Control field;

        if (value instanceof String) {
            TextField textField = new TextField((String) value);
            textField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (index == -1) {
                    parent.inserer_valeur(key, newVal);
                } else {
                    @SuppressWarnings("unchecked")
                    ArrayList list = parent.getListe(key);
                    list.set(index, newVal);
                }
            });
            field = textField;
        } else if (value instanceof Integer) {
            TextField numberField = new TextField(value.toString());
            numberField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*")) {
                    numberField.setText(newVal.replaceAll("[^\\d]", ""));
                }
                try {
                    int numValue = Integer.parseInt(newVal);
                    if (index == -1) {
                        parent.inserer_valeur(key, numValue);
                    } else {
                        @SuppressWarnings("unchecked")
                        List<Object> list = (ArrayList) parent.getListe(key);
                        list.set(index, numValue);
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid numbers temporarily
                }
            });
            field = numberField;
        } else if (value instanceof Boolean) {
            CheckBox checkBox = new CheckBox();
            checkBox.setSelected((Boolean) value);
            checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (index == -1) {
                    parent.inserer_valeur(key, newVal);
                } else {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (ArrayList) parent.getListe(key);
                    list.set(index, newVal);
                }
            });
            field = checkBox;
        } else {
            TextField textField = new TextField(value != null ? value.toString() : "");
            field = textField;
        }

        field.setMinWidth(200);
        return field;
    }

    private void showDataSummary(Map<String, Object> data) {
        System.out.println("Current data structure:");
        printMap(data, 0);
    }

    private void showDataSummary(ObjetCSA objetCSA) {
        System.out.println("Current data structure:");
        objetCSA.getFormattedData();
    }

    private void printMap(Map<String, Object> map, int indent) {
        StringBuilder indentBuilder = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            indentBuilder.append("  ");
        }
        String indentStr = indentBuilder.toString();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                System.out.println(indentStr + entry.getKey() + ":");
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) entry.getValue();
                printMap(nested, indent + 1);
            } else if (entry.getValue() instanceof List) {
                System.out.println(indentStr + entry.getKey() + ":");
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) entry.getValue();
                for (int i = 0; i < list.size(); i++) {
                    System.out.println(indentStr + "  [" + i + "]: " + list.get(i));
                }
            } else {
                System.out.println(indentStr + entry.getKey() + ": " + entry.getValue());
            }
        }
    }

    private Map<String, Object> createSampleData() {
        ObjetCSA individu = createSampleObject();

        return individu.getFormattedData();
    }

    private ObjetCSA createSampleObject() {
        ObjetCSA individu = new Quidam();
        // Same as previous example
        individu.setNom("Alamimbou");
        individu.setId(1);
        individu.inserer_valeur("prenomquidam", "Dieu ne dort");
        individu.inserer_valeur("datenaissancequidam", LocalDate.of(1976, 2, 8));
        individu.inserer_valeur("hospitalise", true);
        individu.inserer_valeur("sexequidam", "Masculin");

        Contact adresse = new Contact();
        adresse.setId(1);
        adresse.setNom("Rue de la liberté 1");
        adresse.inserer_valeur("email", "alamimboudnd@hotmail.cm");
        adresse.inserer_valeur("tel1", "098765432");
        adresse.inserer_valeur("tel2", "+1234567890");
        individu.inserer_fk("fkcontact", adresse);

        MedecinTraitant doc = getDoc();
        individu.inserer_fk("fkmedecin", doc);

        ArrayList<Voiture> listeVoitures = getListeVoitures();
        individu.inserer_liste("Voiture", listeVoitures);
        return individu;
    }

    private ArrayList<Voiture> getListeVoitures() {
        ArrayList<Voiture> voitures = new ArrayList<>();
        Voiture voiture1 = new Voiture();
        voiture1.setNom("Dodge");
        voiture1.setId(1);
        voiture1.inserer_valeur("modelevoiture", "Viper");
        voiture1.inserer_valeur("anneevoiture", 1980);
        Voiture voiture2 = new Voiture();
        voiture2.setId(2);
        voiture2.setNom("Toyota");
        voiture2.inserer_valeur("modelevoiture", "CHR");
        voiture2.inserer_valeur("anneevoiture", 2021);

        voitures.add(voiture1);
        voitures.add(voiture2);
        return voitures;
    }
}
