package controlsFX.Calendrier;

import general.calendrier.Evenement;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.TreeSet;

/**
 * Permet de gérer un agenda avec des événements
 */
public class AgendaControlable extends BorderPane {

    private final ArrayList<String> NOMS_VARIABLES;
    private HBox barreDeControle = new HBox();
    private AgendaSemaineListe agenda;
    private Button btnReculer = new Button(" < ");
    private Button btnAvancer = new Button(" > ");
    private Spinner<Integer> spinnerNbrJours = new Spinner<>();

    /**
     * Initialise l'agendaListe avec par défaut, l'heure d'ouverture à 8h et
     * heure de fermeture à 17h Il y aura également des blocs de trous d'une
     * demi-heure pdt les heures de travail et d'1h les WE
     *
     * @param noms c'est la liste de noms de variables de la hashmap des événements
     *             Dans l'ordre :
     *             * nomType           nom de la variable qui contient le type : Evenement, rdv, etc
     *             * nomID             nom de la variable qui contient l'id
     *             * nomDescription    nom de la variable qui contient la description
     *             * nomCouleurTexte   nom de la variable qui contient la couleur du texte
     *             * nomIDProprietaire nom de la variable qui contient l'id du propriétaire
     *             * nomDateDebut      nom de la variable qui contient la date du début
     *             * nomDateFin        nom de la variable qui contient la date de fin
     *             * nomLieu
     */
    public AgendaControlable(ArrayList<String> noms) {
        NOMS_VARIABLES = new ArrayList<>(noms);
        agenda = new AgendaSemaineListe(null, NOMS_VARIABLES);
        initialize();
    }

    /**
     * Initialise l'agendaListe avec par défaut, l'heure d'ouverture à 8h et
     * heure de fermeture à 17h Il y aura également des blocs de trous d'une
     * demi-heure pdt les heures de travail et d'1h les WE
     *
     * @param conteneurAgenda c'est le panneau contenant l'agenda. Il doit gérer
     *                        les transferts d'information
     * @param noms            c'est la liste de noms de variables de la hashmap des événements
     *                        Dans l'ordre :
     *                        * nomType           nom de la variable qui contient le type : Evenement, rdv, etc
     *                        * nomID             nom de la variable qui contient l'id
     *                        * nomDescription    nom de la variable qui contient la description
     *                        * nomCouleurTexte   nom de la variable qui contient la couleur du texte
     *                        * nomIDProprietaire nom de la variable qui contient l'id du propriétaire
     *                        * nomDateDebut      nom de la variable qui contient la date du début
     *                        * nomDateFin        nom de la variable qui contient la date de fin
     *                        * nomLieu
     */
    public AgendaControlable(ConteneurAgenda conteneurAgenda, ArrayList<String> noms) {
        NOMS_VARIABLES = new ArrayList<>(noms);

        // ceci initialise aussi l'agenda
        agenda = new AgendaSemaineListe(conteneurAgenda, NOMS_VARIABLES);

        initialize();
    }


    /**
     * Initialise l'agenda avec les paramètres par défaut des blocs de durée
     * (1440 minutes = 24h pour chaque jour de la semaine)
     */
    public void initialize() {

        agenda.setDureeBloc(DayOfWeek.MONDAY, 1440);
        agenda.setDureeBloc(DayOfWeek.TUESDAY, 1440);
        agenda.setDureeBloc(DayOfWeek.WEDNESDAY, 1440);
        agenda.setDureeBloc(DayOfWeek.THURSDAY, 1440);
        agenda.setDureeBloc(DayOfWeek.FRIDAY, 1440);
        agenda.setDureeBloc(DayOfWeek.SATURDAY, 1440);
        agenda.setDureeBloc(DayOfWeek.SUNDAY, 1440);

        Platform.runLater(() -> {
            init_barre();
            setPadding(new Insets(2, 2, 2, 2));

            setCenter(agenda);
            setBottom(barreDeControle);
        });
    }

    /**
     * Définit la durée des blocs de trou pour un jour spécifique
     *
     * @param jour  le jour de la semaine
     * @param duree la durée en minutes
     */
    public void setDureeBlocTrou(DayOfWeek jour, int duree) {
        agenda.setDureeBloc(jour, duree);
    }

    private void init_barre() {
        spinnerNbrJours.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 7, 5));
        spinnerNbrJours.setPrefWidth(90);
        agenda.setNombreJoursAffiches(5);
        barreDeControle.setSpacing(20);
        barreDeControle.setPadding(new Insets(5, 5, 5, 5));
        barreDeControle.setAlignment(Pos.CENTER);
        barreDeControle.getChildren().add(btnReculer);
        barreDeControle.getChildren().add(new Label("Nombre de jours affichés :"));
        barreDeControle.getChildren().add(spinnerNbrJours);
        barreDeControle.getChildren().add(btnAvancer);

        btnAvancer.setOnAction(this::action_avancer);
        btnReculer.setOnAction(this::action_reculer);
        btnReculer.setDisable(true);
        spinnerNbrJours.setOnMouseClicked(this::action_spinner);
    }

    /**
     * Met à jour l'affichage de l'agenda à partir d'une date donnée
     *
     * @param date la date de début d'affichage
     */
    public void update_affichage(LocalDate date) {
        agenda.update_affichage(date);
    }

    private void action_spinner(MouseEvent mouseEvent) {
        int nombreDeJours = spinnerNbrJours.getValue();
        if (nombreDeJours != agenda.getNombreJoursAffiches()) {
            agenda.setNombreJoursAffiches(nombreDeJours);

            btnAvancer.setDisable(agenda.getJourAffiche().getDayOfWeek().getValue() + nombreDeJours > 7);
            btnReculer.setDisable(agenda.getJourAffiche().getDayOfWeek().equals(DayOfWeek.MONDAY));
        }
    }

    private void action_reculer(ActionEvent actionEvent) {
        LocalDate premierJourAffiche = agenda.getJourAffiche();
        int premierJour = premierJourAffiche.getDayOfWeek().getValue();
        if (premierJour > 1) {
            boolean onEstAvantMercredi = premierJour < 3;
            btnReculer.setDisable(onEstAvantMercredi);
            btnAvancer.setDisable(agenda.getNombreJoursAffiches() < 2);

            agenda.setPremierJourAffiche(premierJourAffiche.minusDays(1));
        }
    }

    private void action_avancer(ActionEvent actionEvent) {
        LocalDate premierJourAffiche = agenda.getJourAffiche();
        LocalDate dernierJourAffiche = premierJourAffiche.plusDays(agenda.getNombreJoursAffiches() - 1);
        int dernierJour = dernierJourAffiche.getDayOfWeek().getValue();
        if (dernierJour < 7) {
            boolean onEstApresVendredi = dernierJour > 5;
            btnReculer.setDisable(agenda.getNombreJoursAffiches() < 2);
            btnAvancer.setDisable(onEstApresVendredi);

            agenda.setPremierJourAffiche(premierJourAffiche.plusDays(1));
        }
    }

    /**
     * Retourne tous les événements voisins d'un événement (le même jour)
     *
     * @param evenement l'événement dont on veut récupérer les voisins du jour
     * @return événements voisins
     */
    public ArrayList<Evenement> getEvenementsVoisinsJour(Evenement evenement) {
        return agenda.getEvenementsVoisinsJour(evenement);
    }

    /**
     * Retourne le nombre de jours actuellement affichés
     *
     * @return le nombre de jours affichés
     */
    public int getNombreJoursAffiches() {
        return agenda.getNombreJoursAffiches();
    }

    /**
     * Définit le nombre de jours à afficher
     *
     * @param valeur le nombre de jours (entre 1 et 7)
     */
    public void setNombreJoursAffiches(int valeur) {
        agenda.setNombreJoursAffiches(valeur);
        spinnerNbrJours.getValueFactory().setValue(valeur);
    }

    /**
     * Ajoute un événement à l'agenda
     *
     * @param evenement l'événement à ajouter
     */
    public void addEvenement(Evenement evenement) {
        agenda.addEvenement(evenement);
    }

    /**
     * Retire un événement de l'agenda
     *
     * @param evenement l'événement à retirer
     */
    public void removeEvenement(Evenement evenement) {
        agenda.removeEvenement(evenement);
    }

    /**
     * Retire de la liste de l'agenda l'événement avec l'id rentré
     *
     * @param id non nul de l'événement à retirer, sinon la méthode ne fait rien
     */
    public void remove_evenement_with_id(int id) {
        if (id > 0) {
            agenda.remove_evenement_with_id(id);
        }
    }

    /**
     * Sélectionne un événement dans l'agenda (met en surbrillance)
     *
     * @param evenement l'événement à sélectionner
     */
    public void select(Evenement evenement) {
        agenda.select(evenement);
    }

    /**
     * Retire plusieurs événements de l'agenda
     *
     * @param evs la liste des événements à retirer
     */
    public void removeEvenements(ArrayList<? extends Evenement> evs) {
        agenda.removeEvenements(evs);
    }

    /**
     * Vide l'agenda de tous ses événements
     */
    public void clear() {
        agenda.clear();
    }

    /**
     * Retourne tous les événements de l'agenda
     *
     * @return une copie de la liste des événements
     */
    public ArrayList<Evenement> getEvenements() {
        return new ArrayList<>(agenda.getEvenements());
    }

    /**
     * Remplace tous les événements de l'agenda
     *
     * @param treeSet le nouvel ensemble d'événements
     */
    public void setEvenements(TreeSet<? extends Evenement> treeSet) {
        agenda.setEvenements(treeSet);
    }

    /**
     * Définit les disponibilités affichées dans l'agenda
     *
     * @param treeSet l'ensemble des disponibilités
     */
    public void setDisponibilites(TreeSet<? extends Evenement> treeSet) {
        agenda.setDisponibilites(treeSet);
    }


    /**
     * Affiche un message dans la console via le conteneur
     *
     * @param s le message à afficher
     */
    public void afficheConsole(String s) {
        if (agenda.getConteneur() != null) {
            agenda.getConteneur().afficheConsole("AgendaControlable : " + s);
            return;
        }
        System.err.println("L'agendaController n'a pas de conteneur enregistré !");
    }

    /**
     * le lundi de la semaine affichée
     *
     * @return le lundi de la semaine affichée
     */
    public LocalDate getJourAffiche() {
        return agenda.getJourAffiche();
    }

    /**
     * Affiche d'après le jour demandé, le lundi de la semaine. Si la date
     * rentrée est un we, alors j'affiche le lundi suivant
     *
     * @param date date de référence
     */
    public void setAffichageSemaineSmart(LocalDate date) {
        agenda.update_affichage(date);
        btnAvancer.setDisable(agenda.getNombreJoursAffiches() > 6);
        btnReculer.setDisable(true);
    }

    /**
     * Affiche d'après le jour demandé, le lundi de la semaine.
     *
     * @param date date de référence
     */
    public void setAffichageSemaine(LocalDate date) {
        agenda.update_affichage(date);
        btnAvancer.setDisable(agenda.getNombreJoursAffiches() > 6);
        btnReculer.setDisable(true);
    }

    /**
     * Retourne les événements actuellement affichés à l'écran
     *
     * @return la liste des événements visibles
     */
    public ArrayList<Evenement> getEvenementsAffiches() {
        return agenda.getEvenementsAffiches();
    }

    /**
     * Retourne les événements d'un jour spécifique (visible)
     *
     * @param date la date du jour
     * @return la liste des événements du jour
     */
    public ArrayList<Evenement> getEvenementsJour(LocalDate date) {
        return agenda.getEvenementsJour(date);
    }

    /**
     * Retourne tous les événements d'un jour spécifique (complet)
     *
     * @param date la date du jour
     * @return la liste complète des événements du jour
     */
    public ArrayList<Evenement> getEvenementsJourFull(LocalDate date) {
        return agenda.getEvenementsJourFull(date);
    }

    /**
     * @return Le nombre de minutes total de tous les événements de l'agenda
     */
    public Long getTotalMinutes() {
        return agenda.getTotalMinutesAffichees();
    }

    /**
     * Active ou désactive l'édition de l'agenda
     *
     * @param b true pour activer l'édition, false sinon
     */
    public void setEditable(boolean b) {
        agenda.setEditable(b);
    }

    /**
     * Définit si l'agenda est en mode général
     *
     * @param b true pour le mode général, false sinon
     */
    public void setGeneral(boolean b) {
        agenda.setGeneral(b);
    }


}
