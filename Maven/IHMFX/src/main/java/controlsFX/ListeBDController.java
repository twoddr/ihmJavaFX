package controlsFX;

import java.io.IOException;
import java.net.URL;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.TreeSet;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import general.ObjetCSA;
import generalFX.FenetreGlissante;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

/**
 * Contrôleur pour une liste de données BD avec fonctionnalités de filtrage,
 * sélection et gestion des actions CRUD.
 *
 * @author didie
 */
public class ListeBDController extends VBox implements Initializable {

    private static final int SEUIL_GRANDE_LISTE = 200;
    private static final int MIN_CARACTERES_FILTRE = 3;
    private final ArrayList<Object> listeOriginale = new ArrayList<>();
    private FenetreGlissante<Object> fenetreGlissante = new FenetreGlissante<>();
    @FXML
    private Button btnNouveau;
    @FXML
    private Button btnEnregistrer;
    @FXML
    private Button btnSupprimer;
    @FXML
    private Button btnDupliquer;
    @FXML
    private TextField tfFiltre;
    @FXML
    private ListView<Object> liste;
    @FXML
    private Button btnRafraichir;
    @FXML
    private HBox ligneFiltre, ligneBoutons, ligneRafraichir;
    @FXML
    private ScrollPane scrollPane;
    private MaitreFX maitre;
    private EcouteurFX ecouteurFX;
    private Object selection = null;
    private String dernierTypeVu;

    public ListeBDController() {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/fxml/ListeBD.fxml"));

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
        liste.setCellFactory(new Callback<ListView<Object>, ListCell<Object>>() {
            @Override
            public ListCell<Object> call(ListView<Object> list) {
                return new CelluleListe();
            }
        });
        liste.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // tooltips
        btnDupliquer.setTooltip(new Tooltip("Permet de créer un nouvel élément à partir des " +
                "données contenues dans l'élément affiché"));
        btnEnregistrer.setTooltip(new Tooltip("Permet d'enregistrer les informations affichées, " +
                "dans la base de données"));
        btnSupprimer.setTooltip(new Tooltip("Permet de supprimer l'élément sélectionné de la base " +
                "de données"));
        btnNouveau.setTooltip(new Tooltip("Permet de créer un nouvel élément à partir de données vierges"));
        btnRafraichir.setTooltip(new Tooltip("Permet de synchroniser l'affichage de la liste avec la " +
                "base de données"));
        VBox.setVgrow(ligneFiltre, Priority.ALWAYS);
        VBox.setVgrow(ligneBoutons, Priority.ALWAYS);
        VBox.setVgrow(liste, Priority.ALWAYS);
        VBox.setVgrow(ligneRafraichir, Priority.NEVER);

        // Scroll listener pour ListView
        liste.setOnScroll(event -> {
            if (fenetreGlissante.needsPagination()) {
                int selectedIndex = liste.getSelectionModel().getSelectedIndex();
                int size = liste.getItems().size();

                if (event.getDeltaY() < 0 && selectedIndex > size - 5) {
                    charger_batch_suivant();
                } else if (event.getDeltaY() > 0 && selectedIndex < 5) {
                    charger_batch_precedent();
                }
            }
        });
    }

    /**
     * Ajoute un élément à la liste originale et rafraîchit l'affichage.
     *
     * @param x l'élément à ajouter
     */
    public void addElement(Object x) {
        listeOriginale.add(x);
        action_filtrer(null);
    }

    /**
     * Supprime les éléments actuellement sélectionnés de la liste.
     */
    public void removeSelected() {
        Collection selections = liste.getSelectionModel().getSelectedItems();
        if (selections.isEmpty()) {
            System.out.println("** ListeBD reçoit un ordre de supprimer des lignes alors que rien n'est" +
                    " sélectionné. Ignoré !");
            return;
        }
        Platform.runLater(() -> {
            liste.getItems().removeAll(selections);
        });
    }

    /**
     * Sélectionne un élément dans la liste.
     *
     * @param o l'objet à sélectionner, ou null pour effacer la sélection
     */
    public void select(Object o) {
        if (o == null) {
            clearSelection();
        } else {
            liste.getSelectionModel().select(o);
        }
    }

    /**
     * Vide le champ de filtre et réinitialise la sélection.
     *
     * @param action l'événement déclencheur
     */
    @FXML
    public void action_vider_textField(ActionEvent action) {
        tfFiltre.setText("");
        action_filtrer(null);
        clearSelection();
    }

    /**
     * Permet d'attribuer la fenêtre d'édition et la fenêtre maître
     * de cette liste
     *
     * @param maitreFX tous les clics utilisateurs lui seront envoyés
     */
    public void setMaitre(MaitreFX maitreFX) {
        maitre = maitreFX;
    }

    /**
     * Définit l'écouteur FX pour les événements de cette liste.
     *
     * @param ecouteur l'écouteur à associer
     */
    public void setEcouteur(EcouteurFX ecouteur) {
        ecouteurFX = ecouteur;
    }

    /**
     * Efface la sélection courante et désactive les boutons d'action.
     */
    public void clearSelection() {
        liste.getSelectionModel().clearSelection();
        action_activer_selection(false);
        selection = null;
    }

    /**
     * Filtre la liste en fonction du texte saisi dans le champ de filtre.
     *
     * @param keyEvent l'événement clavier déclencheur
     */
    @FXML
    public void action_filtrer(KeyEvent keyEvent) {
        Platform.runLater(() -> {
            if (tfFiltre.getText().isEmpty()) {
                liste.getItems().setAll(listeOriginale);
                // liste.refresh();
                if (selection == null) {
                    clearSelection();
                } else {
                    if (selection instanceof ObjetCSA) {
                        liste.getSelectionModel().select(selection);
                    } else {
                        if (liste.getItems().contains(selection)) {
                            liste.getSelectionModel().select(selection);
                        } else {
                            clearSelection();
                        }
                    }
                }
                liste.refresh();
            } else {
                String filtre = tfFiltre.getText();
                filtrer(filtre);
            }
        });
    }

    /**
     * Filtre la liste avec le texte spécifié (normalisé).
     *
     * @param texteFiltre le texte de filtrage
     */
    public void filtrer(String texteFiltre) {
        Platform.runLater(() -> {
            String texte = Normalizer.normalize(texteFiltre.trim().toUpperCase(),
                    Normalizer.Form.NFD).replaceAll("[\u0300-\u036F]",
                            "");
            TreeSet<Object> inclus = new TreeSet<>();

            Integer nombre = null;
            try {
                nombre = Integer.parseInt(texteFiltre.trim());
            } catch (Exception ingored) {
            } finally {
                Integer finalNombre = nombre;
                listeOriginale.forEach((elt) -> {
                    String selection;

                    selection = Normalizer.normalize(elt.toString().trim().toUpperCase(),
                            Normalizer.Form.NFD).replaceAll("[\u0300-\u036F]",
                                    "");

                    if (selection.startsWith(texte)) {
                        inclus.add(elt);
                    }
                });

                liste.getItems().setAll(inclus);
                // liste.refresh();

                if (selection != null) {
                    if (selection instanceof ObjetCSA) {
                        liste.getSelectionModel().select(selection);
                    } else {
                        if (liste.getItems().contains(selection)) {
                            liste.getSelectionModel().select(selection);
                        } else {
                            clearSelection();
                        }
                    }
                } else {
                    clearSelection();
                }
            }
        });
    }

    /**
     * Charge une liste d'objets dans le contrôleur.
     *
     * @param l la liste d'objets à charger
     */
    public void chargerListe(ArrayList<Object> l) {
        listeOriginale.clear();
        listeOriginale.addAll(l);
        fenetreGlissante.chargerTout(l);

        if (!l.isEmpty() && l.get(0) instanceof ObjetCSA) {
            dernierTypeVu = ((ObjetCSA) l.get(0)).getType();
        }

        afficher_fenetre_actuelle();
    }

    public void setTypeAffiche(String type) {
        dernierTypeVu = type;
        afficher_fenetre_actuelle();
    }

    private void afficher_fenetre_actuelle() {
        Platform.runLater(() -> {
            liste.getItems().setAll(fenetreGlissante.getFenetreActuelle());
            liste.refresh();
        });
    }

    private void charger_batch_suivant() {
        if (fenetreGlissante.chargerSuivants()) {
            afficher_fenetre_actuelle();
        }
    }

    private void charger_batch_precedent() {
        if (fenetreGlissante.chargerPrecedents()) {
            afficher_fenetre_actuelle();
        }
    }

    /**
     * Vide complètement la liste et sa sélection.
     */
    public void clear() {
        listeOriginale.clear();
        selection = null;
        Platform.runLater(() -> {
            liste.getItems().clear();
        });

    }

    /**
     * Retourne une copie de la liste complète des éléments.
     *
     * @return une copie de la liste originale
     */
    public ArrayList<Object> getListeComplete() {
        ArrayList<Object> listeRetour = new ArrayList<>();
        listeRetour.addAll(listeOriginale);
        return listeRetour;
    }

    /**
     * Renvoie une copie de la liste affichée
     *
     * @return copie de la liste visible
     */
    public ArrayList<Object> getListeAffichee() {
        return new ArrayList<>(liste.getItems());
    }

    /**
     * Retourne la liste des éléments actuellement sélectionnés.
     *
     * @return une ObservableList des éléments sélectionnés
     */
    public ObservableList<Object> getElementsSelectionnes() {
        return liste.getSelectionModel().getSelectedItems();
    }

    /**
     * Retourne l'objet actuellement sélectionné.
     *
     * @return l'objet sélectionné, ou null si aucune sélection
     */
    public Object getSelection() {
        return selection;
    }

    @FXML
    private void action_clic_liste(MouseEvent event) {
        selection = liste.getSelectionModel().getSelectedItem();
        if (maitre == null) {
            if (ecouteurFX == null) {
                System.err.println("La liste BD ne trouve pas d'éditeur. Impossible de charger les données !");
                return;
            }
            ecouteurFX.action_retour_component(selection, this, "clic");
            return;
        }
        maitre.affiche_details(liste.getSelectionModel().getSelectedItem());
        action_activer_selection(true);
    }

    @FXML
    private void action_nouveau(ActionEvent event) {
        clearSelection();
        if (maitre != null) {
            maitre.nettoyer_champs();
            return;
        }
        if (ecouteurFX == null) {
            System.err.println("** ListeBDController/nettoyer_champs : L'éditeur n'a pas été initialisé !");
            return;
        }
        ecouteurFX.action_retour_component(liste.getSelectionModel().getSelectedItem(),
                this,
                "nouveau");
    }

    @FXML
    private void action_enregistrer(ActionEvent event) {
        ObservableList<Object> selections = liste.getSelectionModel().getSelectedItems();
        if (maitre == null) {
            if (ecouteurFX == null) {
                System.err.println("La liste BD ne trouve pas d'éditeur. Impossible de modifier les données !");
                return;
            }
            ecouteurFX.action_retour_component(liste.getSelectionModel().getSelectedItem(), this, "modifier");
            return;
        }
        ArrayList<ObjetCSA> aAjouter = new ArrayList<>();
        ArrayList<ObjetCSA> aModifier = new ArrayList<>();
        selections.forEach(elt -> {
            if (elt instanceof ObjetCSA) {
                int id = ((ObjetCSA) elt).getId();
                if (id > 0) {
                    aModifier.add((ObjetCSA) elt);
                } else {
                    aAjouter.add((ObjetCSA) elt);
                }
            } else {
                System.err.println("Tentative d'enregistrement de " + elt + " qui n'est pas " +
                        "un élément BD !");
            }
        });
        if (!aAjouter.isEmpty()) {
            maitre.appeler_bd(1, aAjouter, null);
        }
        if (!aModifier.isEmpty()) {
            maitre.appeler_bd(2, aModifier, null);
        }
    }

    @FXML
    private void action_supprimer(ActionEvent event) {
        ObservableList<Object> selections = liste.getSelectionModel().getSelectedItems();
        if (maitre == null) {
            if (ecouteurFX == null) {
                System.err.println("La liste BD ne trouve pas d'éditeur. Impossible de supprimer les données !");
                return;
            }
            ecouteurFX.action_retour_component(liste.getSelectionModel().getSelectedItem(), this, "supprimer");
            return;
        }
        maitre.appeler_bd(3, selections, null);
    }

    @FXML
    private void action_dupliquer(ActionEvent event) {
        Object selection = liste.getSelectionModel().getSelectedItem();
        if (maitre == null) {
            System.err.println("La liste BD ne trouve pas de maître. Impossible de dupliquer les données !");
            return;
        }
        if (selection == null) {
            maitre.affiche_utilisateur("Veuillez sélectionner un élément à dupliquer, svp");
            return;
        }
        if (selection instanceof ObjetCSA) {
            ObjetCSA objetIHM = (ObjetCSA) selection;
            ObjetCSA nouveau = objetIHM.getNewInstance();
            nouveau.setData(objetIHM.getData(), null, true, false);
            nouveau.setId(0);
            nouveau.setNom(objetIHM.getNom() + "_1");
            maitre.charger_donnees_vers_enfant(nouveau);
            return;
        }
        maitre.affiche_utilisateur("Impossible de dupliquer un objet de ce type");
    }

    @FXML
    private void action_rafraichir(ActionEvent event) {
        if (maitre == null) {
            System.err.println("La liste BD ne trouve pas d'éditeur. Impossible de rafraichir les données !");
        } else {
            maitre.appeler_bd(4, maitre.getElementFromType(dernierTypeVu), null);
        }
    }

    /**
     * Active ou désactive les boutons de duplication et suppression.
     *
     * @param b true pour activer, false pour désactiver
     */
    public void action_activer_selection(Boolean b) {
        btnDupliquer.setDisable(!b);
        btnSupprimer.setDisable(!b);
    }

    /**
     * Active ou désactive l'affichage du bouton d'enregistrement.
     *
     * @param b true pour afficher, false pour masquer
     */
    public void activer_enregistrement(Boolean b) {
        btnEnregistrer.setVisible(b);
    }

    /**
     * Active ou désactive l'affichage du bouton de suppression.
     *
     * @param b true pour afficher, false pour masquer
     */
    public void activer_suppression(Boolean b) {
        btnSupprimer.setVisible(b);
    }

    /**
     * Active ou désactive l'affichage du bouton de duplication.
     *
     * @param b true pour afficher, false pour masquer
     */
    public void activer_duplication(Boolean b) {
        btnDupliquer.setVisible(b);
    }

    /*
     * public ListView<Object> getListView() {
     * return liste;
     * }
     */

    /**
     * Active ou désactive l'affichage de la ligne de boutons.
     *
     * @param b true pour afficher, false pour masquer
     */
    public void activer_ligneBoutons(boolean b) {
        Platform.runLater(() -> {
            ligneBoutons.setVisible(b);
            ligneBoutons.setMaxHeight(b ? 30 : 0);
        });
    }

    /**
     * Active ou désactive l'affichage de la ligne de filtre.
     *
     * @param b true pour afficher, false pour masquer
     */
    public void activer_ligneFiltre(boolean b) {
        Platform.runLater(() -> {
            ligneFiltre.setVisible(b);
            ligneFiltre.setMaxHeight(b ? 30 : 0);
        });
    }

    /**
     * Retire définitivement la ligne de filtre du layout.
     */
    public void retirer_ligneFiltre() {
        Platform.runLater(() -> {
            getChildren().remove(ligneFiltre);
        });
    }

    /**
     * Retire définitivement la ligne de boutons du layout.
     */
    public void retirer_ligneBoutons() {
        Platform.runLater(() -> {
            getChildren().remove(ligneBoutons);
        });
    }

    /**
     * Retire définitivement la ligne de rafraîchissement du layout.
     */
    public void retirer_ligneRafraichir() {
        Platform.runLater(() -> {
            getChildren().remove(ligneRafraichir);
        });
    }

}
