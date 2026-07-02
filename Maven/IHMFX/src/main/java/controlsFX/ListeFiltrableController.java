package controlsFX;


import general.ObjetCSA;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.net.URL;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.TreeSet;

public class ListeFiltrableController extends AnchorPane implements Initializable {

    private static final int SEUIL_GRANDE_LISTE = 200;
    private static final int MIN_CARACTERES_FILTRE = 1;
    @FXML
    public TextField tfFiltre;
    @FXML
    public ListView<Object> liste;
    @FXML
    public AnchorPane ListeFiltrable;
    @FXML
    public HBox ligneFiltre;
    private ArrayList<Object> listeOriginale = new ArrayList<>();
    /**
     * @deprecated Utiliser {@link #setOnSelection(java.util.function.Consumer)} à la place.
     */
    @Deprecated
    private MaitreFX maitre;
    private java.util.function.Consumer<ObjetCSA> onSelectionHandler;
    private java.util.function.Consumer<String> messageHandler;
    private Object selection;

    public ListeFiltrableController() {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/fxml/ListeFiltrable.fxml"));

        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // textProperty listener : déclenche le filtre à chaque frappe, collage, suppression…
        tfFiltre.textProperty().addListener((obs, oldVal, newVal) -> action_filtrer(null));
        liste.setOnMouseReleased(this::action_clic_liste);
        liste.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        if (listeOriginale.size() > SEUIL_GRANDE_LISTE) {
            tfFiltre.setPromptText("Min. " + MIN_CARACTERES_FILTRE + " caractères");
        }
    }

    /**
     * Mémorise l'ObjetCSA à sélectionner et tente de l'appliquer dans la liste courante.
     * Si la liste n'est pas encore chargée ou ne contient pas l'élément,
     * la sélection sera appliquée automatiquement au prochain chargement de liste.
     *
     * @param obj l'ObjetCSA à sélectionner (null pour effacer la sélection mémorisée)
     */
    public void select(ObjetCSA obj) {
        selection = obj;
        if (obj == null) {
            Platform.runLater(this::clearSelection);
            return;
        }
        Platform.runLater(() -> appliquerSelectionDansListe());
    }

    /**
     * Définit le callback appelé lors d'une sélection dans la liste.
     *
     * @param handler appelé avec l'ObjetCSA sélectionné
     */
    public void setOnSelection(java.util.function.Consumer<ObjetCSA> handler) {
        this.onSelectionHandler = handler;
    }

    /**
     * Définit le callback pour afficher des messages.
     *
     * @param handler appelé avec le message à afficher
     */
    public void setMessageHandler(java.util.function.Consumer<String> handler) {
        this.messageHandler = handler;
    }

    /**
     * @deprecated Utiliser {@link #setOnSelection(java.util.function.Consumer)} à la place.
     */
    @Deprecated
    public void setMaitre(MaitreFX fx) {
        maitre = fx;
        // Pont de compatibilité : on crée les callbacks à partir du MaitreFX
        this.onSelectionHandler = obj -> fx.charger_donnees_vers_enfant(obj);
        this.messageHandler = msg -> fx.affiche_console(msg);
    }

    @FXML
    public void action_vider_textField(ActionEvent action) {
        tfFiltre.setText("");
        action_filtrer(null);
        selection = null;
    }

    public void setVisibiliteFiltre(boolean visible) {
        tfFiltre.setVisible(visible);
    }

    public void activer_ligneFiltre(boolean b) {
        ligneFiltre.setVisible(b);
        ligneFiltre.setPrefHeight(b ? 35 : 0);
    }

    public void removeSelected() {
        Collection selections = liste.getSelectionModel().getSelectedItems();
        if (selections.isEmpty()) {
            System.out.println("** ListeFiltrable reçoit un ordre de supprimer des lignes alors que rien n'est" +
                    " sélectionné. Ignoré !");
            return;
        }
        Platform.runLater(() -> {
            liste.getItems().removeAll(selections);
        });
    }

    public boolean contientDeja(Object nouveau) {
        if (listeOriginale.contains(nouveau)) {
            return true;
        }
        if (nouveau instanceof ObjetCSA) {
            ObjetCSA intrus = (ObjetCSA) nouveau;
            for (Object elt : listeOriginale) {
                if (elt instanceof ObjetCSA) {
                    if (((ObjetCSA) elt).getType().equals(intrus.getType()) &&
                            ((ObjetCSA) elt).getId() == intrus.getId()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @FXML
    public void action_filtrer(KeyEvent keyEvent) {
        if (!tfFiltre.getText().isEmpty()) {
            String filtre = Normalizer.normalize(tfFiltre.getText().trim().toUpperCase(),
                    Normalizer.Form.NFD).replaceAll("[\u0300-\u036F]", "");
            TreeSet<Object> inclus = new TreeSet<>();

            listeOriginale.forEach((elt) -> {
                String eltNormalise = Normalizer.normalize(elt.toString().trim().toUpperCase(),
                        Normalizer.Form.NFD).replaceAll("[\u0300-\u036F]", "");
                if (eltNormalise.contains(filtre)) {
                    inclus.add(elt);
                }
            });
            Platform.runLater(() -> {
                liste.getItems().setAll(inclus);
                appliquerSelectionDansListe();
            });
        } else {
            Platform.runLater(() -> {
                liste.getItems().clear();
                liste.getItems().addAll(listeOriginale);
                appliquerSelectionDansListe();
            });
        }
    }

    /**
     * Tente d'appliquer la sélection mémorisée dans la liste actuellement affichée.
     * Si l'élément n'est pas trouvé, la sélection mémorisée est effacée.
     * Doit être appelée depuis le thread JavaFX.
     */
    private void appliquerSelectionDansListe() {
        if (selection == null) {
            clearSelection();
            return;
        }
        for (Object item : liste.getItems()) {
            if (correspondA(item, selection)) {
                liste.getSelectionModel().select(item);
                liste.scrollTo(item);
                return;
            }
        }
        // Élément non trouvé dans la liste courante → on efface la sélection mémorisée
        selection = null;
        clearSelection();
    }

    /**
     * Vérifie si deux objets correspondent, en tenant compte de l'identité ObjetCSA.
     */
    private boolean correspondA(Object itemListe, Object ref) {
        if (itemListe instanceof ObjetCSA && ref instanceof ObjetCSA) {
            ObjetCSA a = (ObjetCSA) itemListe;
            ObjetCSA b = (ObjetCSA) ref;
            return a.getType().equals(b.getType()) && a.getId() == b.getId();
        }
        return itemListe.equals(ref);
    }

    private void clearSelection() {
        if (!liste.getItems().isEmpty()) {
            liste.getSelectionModel().clearAndSelect(-1);
            liste.refresh();
        }
    }

    @FXML
    public void action_clic_liste(MouseEvent mouseEvent) {
        selection = liste.getSelectionModel().getSelectedItem();
        if (selection instanceof ObjetCSA) {
            ObjetCSA objetCSA = (ObjetCSA) selection;
            if (onSelectionHandler != null) {
                onSelectionHandler.accept(objetCSA);
                return;
            }
            if (maitre != null) {
                maitre.charger_donnees_vers_enfant(objetCSA);
                return;
            }
        } else if (selection != null) {
            String msg = "ListeFiltrableController/action_clic_liste : la sélection n'est pas un ObjetCSA. Action ignorée.";
            if (messageHandler != null) messageHandler.accept(msg);
            else if (maitre != null) maitre.affiche_console(msg);
            return;
        }
        System.err.println("La liste filtrable ne trouve pas d'éditeur. Impossible de charger les données !");
    }

    public void chargerListe(ArrayList<Object> l) {
        listeOriginale.clear();
        listeOriginale.addAll(l);
        action_filtrer(null);
    }

    public void addElement(Object x) {
        listeOriginale.add(x);
        action_filtrer(null);
    }

    public ArrayList<Object> getListeComplete() {
        return new ArrayList<>(listeOriginale);
    }

    public ArrayList<Object> getListeAffichee() {
        ObservableList<Object> listeInterne = liste.getItems();
        return new ArrayList<>(listeInterne);
    }

    public String getTexteFiltre() {
        return tfFiltre.getText();
    }

    public void setTexteFiltre(String s) {
        tfFiltre.setText(s);
    }

    public ObservableList<Object> getElementsSelectionnes() {
        return liste.getSelectionModel().getSelectedItems();
    }

    public TextField getTfFiltre() {
        return tfFiltre;
    }

    public ListView<Object> getListe() {
        return liste;
    }

}
