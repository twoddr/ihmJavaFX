package controlsFX;

import general.DetecteurModification;
import general.ObjetCSA;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class DoubleListeController extends BorderPane implements Initializable {

    private final ArrayList listeCibleOriginale = new ArrayList();
    @FXML
    private Label lblCible;
    @FXML
    private Label lblSource;
    @FXML
    private ListView<Object> listeCible;
    @FXML
    private ListView<Object> listeSource;
    @FXML
    private Button btnToutVersCible;
    @FXML
    private Button btnVersCible;
    @FXML
    private Button btnVersSource;
    @FXML
    private Button btnToutVersSource;
    private Comparator comparator = Comparator.naturalOrder();
    private EcouteurFX ecouteurFX = null;

    // Gestion des sélections en attente (éléments à déplacer vers la cible)
    private ArrayList<Object> pendingSelections = new ArrayList<>();
    private volatile boolean isProgrammaticChange = false;

    public DoubleListeController() {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/fxml/DoubleListe.fxml"));

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
        listeCible.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listeSource.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        EventHandler<ActionEvent> buttonHandler = (ActionEvent event) -> {
            Object source = event.getSource();
            if (source.equals(btnToutVersCible)) {
                envoyer_vers_cible(listeSource.getItems());
            } else if (source.equals(btnToutVersSource)) {
                envoyer_vers_source(listeCible.getItems());
            } else if (source.equals(btnVersCible)) {
                envoyer_vers_cible(listeSource.getSelectionModel().getSelectedItems());
            } else if (source.equals(btnVersSource)) {
                envoyer_vers_source(listeCible.getSelectionModel().getSelectedItems());
            }
            if (ecouteurFX != null) {
                ecouteurFX.action_retour_component(null, this, event);
            }

            event.consume();
        };
        btnToutVersCible.setOnAction(buttonHandler);
        btnToutVersSource.setOnAction(buttonHandler);
        btnVersCible.setOnAction(buttonHandler);
        btnVersSource.setOnAction(buttonHandler);
    }

    @FXML
    public void action_clic_listeCible(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            envoyer_vers_source(listeCible.getSelectionModel().getSelectedItems());
        }
    }


    @FXML
    public void action_clic_listeSource(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            envoyer_vers_cible(listeSource.getSelectionModel().getSelectedItems());
        }
    }


    /**
     * Cette méthode ne tient pas compte des délais de chargement
     * des listes (runLater)
     *
     * @return La liste de tous les élts de la doubleListe
     */
    public ArrayList getListeComplete() {
        ArrayList<Object> retour = new ArrayList<>();
        retour.addAll(listeCible.getItems());
        retour.addAll(listeSource.getItems());
        return retour;
    }



    /*
     ****************** GETTERS ET SETTERS
     */

    /**
     * Charge la liste source
     *
     * @param l c'est la liste qui va remplacer le contenu actuel
     */
    public void setListeComplete(Collection l) {
        Platform.runLater(() -> {
            listeSource.getItems().clear();
            listeCible.getItems().clear();
            listeSource.getItems().addAll(l);
            listeSource.getItems().sort(comparator);
            // Tenter d'appliquer les sélections en attente (déplacer vers la cible)
            applyPendingSelections();
        });
    }

    /**
     * Applique les sélections en attente (déplace les éléments vers la cible)
     */
    private void applyPendingSelections() {
        if (!pendingSelections.isEmpty()) {
            ArrayList<Object> foundElements = new ArrayList<>();
            ArrayList<Object> stillPendingElements = new ArrayList<>();

            for (Object element : pendingSelections) {
                if (listeSource.getItems().contains(element)) {
                    foundElements.add(element);
                } else {
                    stillPendingElements.add(element);
                }
            }

            if (!foundElements.isEmpty()) {
                System.out.println("DoubleListe: Application des sélections différées (déplacement vers cible): " + foundElements);
                // Déplacer les éléments trouvés vers la cible directement (sans appeler envoyer_vers_cible pour éviter la récursivité)
                Platform.runLater(() -> {
                    listeCible.getItems().addAll(foundElements);
                    listeSource.getItems().removeAll(foundElements);
                    listeSource.getItems().sort(comparator);
                    listeCible.getItems().sort(comparator);

                    if (ecouteurFX != null) {
                        ecouteurFX.action_retour_component(foundElements, this, null);
                    }
                });
            }

            // Mettre à jour la liste des sélections en attente
            pendingSelections = stillPendingElements;
        }
    }

    public EcouteurFX getEcouteurFX() {
        return ecouteurFX;
    }

    public void setEcouteurFX(EcouteurFX ecouteurFX) {
        this.ecouteurFX = ecouteurFX;
    }

    public Label getLblCible() {
        return lblCible;
    }

    public Label getLblSource() {
        return lblSource;
    }

    public ListView getListeCible() {
        return listeCible;
    }

    public ListView getListeSource() {
        return listeSource;
    }

    public Button getBtnToutVersCible() {
        return btnToutVersCible;
    }

    public Button getBtnVersCible() {
        return btnVersCible;
    }

    public Button getBtnVersSource() {
        return btnVersSource;
    }

    public Comparator getComparator() {
        return comparator;
    }

    public void setComparator(Comparator comparator) {
        this.comparator = comparator;
    }

    public Button getBtnToutVersSource() {
        return btnToutVersSource;
    }

    /**
     * Sélectionne des éléments (les déplace vers la cible)
     *
     * @param elements liste des éléments à déplacer vers la cible
     * @return true si tous les éléments ont été déplacés, false si certains sont en attente
     */
    public boolean selectElements(Collection<Object> elements) {
        if (elements == null || elements.isEmpty()) {
            clearSelections();
            return true;
        }

        ArrayList<Object> foundElements = new ArrayList<>();
        ArrayList<Object> notFoundElements = new ArrayList<>();

        for (Object element : elements) {
            if (listeSource.getItems().contains(element)) {
                foundElements.add(element);
            } else {
                notFoundElements.add(element);
            }
        }

        // Déplacer les éléments trouvés vers la cible
        if (!foundElements.isEmpty()) {
            envoyer_vers_cible(foundElements);
        }

        // Mémoriser les éléments non trouvés
        pendingSelections.addAll(notFoundElements);

        if (!notFoundElements.isEmpty()) {
            System.out.println("DoubleListe: Sélections différées mémorisées (déplacement vers cible): " + notFoundElements);
        }

        return notFoundElements.isEmpty();
    }

    /**
     * Sélectionne un élément (le déplace vers la cible)
     */
    public boolean selectElement(Object element) {
        return selectElements(Collections.singletonList(element));
    }

    /**
     * Efface toutes les sélections en attente
     */
    public void clearSelections() {
        pendingSelections.clear();
    }

    /**
     * Retourne les sélections en attente (éléments à déplacer vers la cible)
     */
    public ArrayList<Object> getPendingSelections() {
        return new ArrayList<>(pendingSelections);
    }


    public void envoyer_vers_cible(Collection collection) {
        Platform.runLater(() -> {
            ObservableList lSource = listeSource.getItems();
            ArrayList<Object> elements = new ArrayList<>();
            for (Object element : collection) {
                if (lSource.contains(element)) {
                    elements.add(element);
                } else {
                    System.err.println("** DoubleListe : envoi vers cible d'un élément qui n'est " +
                            "pas dans la liste : " + element);
                }
            }
            if (ecouteurFX != null) {
                ecouteurFX.action_retour_component(collection, this, null);
            }
            listeCible.getItems().addAll(elements);
            listeSource.getItems().removeAll(elements);
            listeSource.getItems().sort(comparator);
            listeCible.getItems().sort(comparator);

            // Nettoyer les éléments déplacés de la liste des sélections en attente
            pendingSelections.removeAll(elements);
        });
    }

    private Integer getIdNonAttribue(TreeMap<Integer, Object> paires) {
        for (Map.Entry<Integer, Object> paire : paires.entrySet()) {
            if (paire.getValue() == null) {
                return paire.getKey();
            }
        }
        return null;
    }

    /**
     * Déplace les objets de la liste de la cible vers la liste source
     *
     * @param collection c'est la liste d'éléments à déplacer vers la droite
     */
    private void envoyer_vers_source(Collection collection) {
        ArrayList<Object> elements = new ArrayList<>(collection);

        Platform.runLater(() -> {
            listeSource.getItems().addAll(elements);
            listeCible.getItems().removeAll(elements);
            listeSource.getItems().sort(comparator);
            listeCible.getItems().sort(comparator);
        });

        if (ecouteurFX != null) {
            ecouteurFX.action_retour_component(collection, this, null);
        }
    }

    /**
     * Cherche un ElementBD dans une liste, à partir de son id
     *
     * @param items liste d'élémentsBD hypothétiques
     * @return le premier elt trouvé, null si pas trouvé ou si la liste ne
     * contient pas des elementBD
     */
    private ObjetCSA extraire_elt_with_id(int id, Collection items) {
        for (Object elt : items) {
            if (elt instanceof ObjetCSA) {
                if (((ObjetCSA) elt).getId() == id) {
                    return (ObjetCSA) elt;
                }
            } else {
                System.err.println("** Tentative d'extraction d'un eltBD àparti d'un id"
                        + ", mais la liste à examiner ne contient pas des elementBDs !!!");
                return null;
            }
        }
        System.err.println("** : id " + id + " non trouvé dans la liste source de la DoubleListe : " + items);
        return null;
    }

    /**
     * Permet de calculer la liste des nouveaux de la liste cible
     *
     * @return ceux qui sont arrivés dans la liste source et qui n'y étaient pas au départ
     */
    public ArrayList<Object> getNouveaux() {
        ObservableList lCible = listeCible.getItems();
        DetecteurModification detecteur = new DetecteurModification(listeCibleOriginale, new ArrayList<>(lCible));

        return detecteur.getListeAAjouter();
    }

    /**
     * Permet de calculer la liste des éléments présents dès le départ
     *
     * @return ceux qui sont restés dans la liste cible
     */
    public ArrayList<Object> getAciens() {
        ArrayList<Object> terug = new ArrayList<>();
        ObservableList lCible = listeCible.getItems();
        lCible.forEach(elt -> {
            if (listeCibleOriginale.contains(elt)) {
                terug.add((ObjetCSA) elt);
            }
        });
        return terug;
    }

    /**
     * Permet de calculer la liste des absents de la liste de départ
     *
     * @return ceux qui sont retournés dans la liste source
     */
    public ArrayList<Object> getAbsents() {
        ObservableList lCible = listeCible.getItems();
        DetecteurModification detecteur = new DetecteurModification(listeCibleOriginale, new ArrayList<>(lCible));

        return detecteur.getListeASupprimer();
    }


}
