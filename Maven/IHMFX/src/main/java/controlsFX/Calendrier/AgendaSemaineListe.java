package controlsFX.Calendrier;

import general.calendrier.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;

public class AgendaSemaineListe extends HBox {

    private final ArrayList<Evenement> evenements = new ArrayList<>();
    private final ArrayList<String> NOMS_VARIABLES = new ArrayList<>();
    private final ArrayList<Indisponibilite> indisponibilites = new ArrayList<>();
    private LocalDate jourAffiche;
    private int nombreJoursAffiches = 5;
    private ConteneurAgenda conteneur;
    private LocalTime heureOuverture = null, heureFermeture = null;
    private boolean editable = true;
    private ListeJour listeJourLundi;
    private ListeJour listeJourMardi;
    private ListeJour listeJourMercredi;
    private ListeJour listeJourJeudi;
    private ListeJour listeJourVendredi;
    private ListeJour listeJourSamedi;
    private ListeJour listeJourDimanche;


    /**
     * Initialise l'agendaListe avec par défaut, l'heure d'ouverture à 8h et
     * heure de fermeture à 17h Il y aura également des blocs de trous d'une
     * demi-heure pdt les heures de travail et d'1h les WE
     *
     * @param conteneurAgenda c'est le panneau contenant l'agenda. Il doit gérer
     *                        les transferts d'information
     * @param nomsVariables   c'est la liste de noms de variables de la hashmap des événements
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
    public AgendaSemaineListe(ConteneurAgenda conteneurAgenda, ArrayList<String> nomsVariables) {
        conteneur = conteneurAgenda;
        NOMS_VARIABLES.addAll(nomsVariables);

        initialize();
    }

    /**
     * Initialise l'agendaListe Il y aura également des blocs de trous d'une
     * demi-heure pdt les heures de travail et d'1h les WE
     *
     * @param conteneurAgenda c'est le panneau contenant l'agenda. Il doit gérer
     *                        les transferts d'information
     * @param ouverture       l'heure d'ouverture des bureaux
     * @param fermeture       l'heure de fermeture des bureaux
     * @param nomsVariables   la liste des noms de variable
     */
    public AgendaSemaineListe(ConteneurAgenda conteneurAgenda, LocalTime ouverture, LocalTime fermeture, ArrayList<String> nomsVariables) {
        conteneur = conteneurAgenda;
        heureFermeture = fermeture;
        heureOuverture = ouverture;
        NOMS_VARIABLES.addAll(nomsVariables);
        initialize();
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean b) {
        editable = b;
    }

    public TreeSet<? extends Evenement> getListeSouris() {
        if (conteneur == null) {
            return new TreeSet<>();
        }
        return conteneur.getListeSouris();
    }

    public void setListeSouris(TreeSet<Evenement> listeSouris) {
        if (conteneur == null) {
            System.err.println("** Le conteneur de l'agendaListe n'est pas initialisé. Action de copie ignorée !");
            return;
        }
        conteneur.setListeSouris(listeSouris);
    }

    /**
     * Affiche le nombre de jours affichés apd jour rentré
     *
     * @param date date de début de l'affichage
     */
    public void setPremierJourAffiche(LocalDate date) {
        jourAffiche = date;
        update_affichage();
    }

    public void setDureeBloc(DayOfWeek jour, int nombre) {
        getListeJourAtDate(jour).setDureeBloc(nombre);
    }

    public void setHeureOuverture(LocalTime heureOuverture) {
        this.heureOuverture = heureOuverture;
    }

    public void setHeureFermeture(LocalTime heureFermeture) {
        this.heureFermeture = heureFermeture;
    }

    private void update_affichage() {
        listeJourLundi.setIndisponibilites(jourAffiche, indisponibilites);
        listeJourLundi.setEvenements(jourAffiche, evenements);
        LocalDate mardi = jourAffiche.plusDays(1);
        listeJourMardi.setIndisponibilites(mardi, indisponibilites);
        listeJourMardi.setEvenements(mardi, evenements);
        LocalDate mercredi = jourAffiche.plusDays(2);
        listeJourMercredi.setIndisponibilites(mercredi, indisponibilites);
        listeJourMercredi.setEvenements(mercredi, evenements);
        LocalDate jeudi = jourAffiche.plusDays(3);
        listeJourJeudi.setIndisponibilites(jeudi, indisponibilites);
        listeJourJeudi.setEvenements(jeudi, evenements);
        LocalDate vendredi = jourAffiche.plusDays(4);
        listeJourVendredi.setIndisponibilites(vendredi, indisponibilites);
        listeJourVendredi.setEvenements(vendredi, evenements);
        LocalDate samedi = jourAffiche.plusDays(5);
        listeJourSamedi.setIndisponibilites(samedi, indisponibilites);
        listeJourSamedi.setEvenements(samedi, evenements);
        LocalDate dimanche = jourAffiche.plusDays(6);
        listeJourDimanche.setIndisponibilites(dimanche, indisponibilites);
        listeJourDimanche.setEvenements(dimanche, evenements);

        Platform.runLater(() -> {
            getChildren().clear();
            getChildren().add(listeJourLundi);
            if (nombreJoursAffiches > 1) getChildren().add(listeJourMardi);
            if (nombreJoursAffiches > 2) getChildren().add(listeJourMercredi);
            if (nombreJoursAffiches > 3) getChildren().add(listeJourJeudi);
            if (nombreJoursAffiches > 4) getChildren().add(listeJourVendredi);
            if (nombreJoursAffiches > 5) getChildren().add(listeJourSamedi);
            if (nombreJoursAffiches > 6) getChildren().add(listeJourDimanche);

            double largeur = getPrefWidth() / nombreJoursAffiches;
            listeJourDimanche.setPrefWidth(largeur);
            listeJourSamedi.setPrefWidth(largeur);
            listeJourVendredi.setPrefWidth(largeur);
            listeJourJeudi.setPrefWidth(largeur);
            listeJourMercredi.setPrefWidth(largeur);
            listeJourMardi.setPrefWidth(largeur);
            listeJourLundi.setPrefWidth(largeur);
        });

    }

    public ConteneurAgenda getConteneur() {
        return conteneur;
    }

    /**
     * Affiche d'après le jour demandé, le lundi de la semaine. Si la date
     * rentrée est un we, alors j'affiche le lundi suivant
     *
     * @param date date de référence
     */
    public void update_affichage(LocalDate date) {
        switch (date.getDayOfWeek()) {
            case SUNDAY:
                jourAffiche = date.plusDays(1);
                break;
            case SATURDAY:
                jourAffiche = date.plusDays(2);
                break;
            default:
                jourAffiche = (new Semainier(date)).getLundi();
                break;
        }
        update_affichage();
    }

    public void addEvenement(Evenement Evenement) {
        evenements.add(Evenement);
        update_affichage();
    }

    public void addEvenements(Collection<Evenement> events) {
        if (!evenements.addAll(events)) {
            System.err.println("Échec de l'ajout des événements " + events + " dans " + evenements);
        }
        update_affichage();
    }

    public void removeEvenement(Evenement Evenement) {
        evenements.remove(Evenement);
        update_affichage();
    }

    public void removeEvenements(ArrayList<? extends Evenement> events) {
        System.out.println("* Retrait de " + events + " de la liste initiale de l'agenda");
        evenements.removeAll(events);
        update_affichage();
    }

    public void clear() {
        evenements.clear();
        update_affichage();
    }

    private void affiche_console(String s) {
        System.out.println("Agenda liste : " + s);
    }

    private void affiche_utilisateur(String s) {
        System.out.println(s);
    }

    /**
     * le lundi de la semaine affichée
     *
     * @return le lundi de la semaine affichée
     */
    public LocalDate getJourAffiche() {
        return jourAffiche;
    }


    public int getNombreJoursAffiches() {
        return nombreJoursAffiches;
    }

    /**
     * Définit le nombre de jours à afficher
     *
     * @param n doit être compris entre 1 et 7, sinon, je ne fais rien !
     */
    public void setNombreJoursAffiches(int n) {
        if (n > 0 && n < 8) {
            nombreJoursAffiches = n;
            update_affichage();
        } else {
            affiche_console("Le nombre de jours affichés doit être compris entre 0 et 7, mais j'ai : " + n);
        }
    }

    public Collection<Evenement> getEvenements() {
        return new ArrayList<>(evenements);
    }

    public void setEvenements(TreeSet<? extends Evenement> collection) {
        evenements.clear();
        evenements.addAll(collection);
        update_affichage();
    }

    public void setDisponibilites(TreeSet<? extends Evenement> collection) {
        indisponibilites.clear();
        indisponibilites.addAll(generer_complement(collection));
        update_affichage();
    }

    private Collection<? extends Indisponibilite> generer_complement(TreeSet<? extends Evenement> evenementIHMS) {
        ChercheurTrou chercheurTrou = new ChercheurTrou(NOMS_VARIABLES, null);
        TreeSet<Trou> trous = null;
        try {
            trous = chercheurTrou.chercher_liste_trous(evenementIHMS);
            // Les trous sur un jour complet ne sont pas générés à l'extérieur des événements,
            // donc il faut vérifier s'il y en a et en rajouter si nécessaire
            trous.addAll(remplir_jours_exterieurs_vides(evenementIHMS));
            //chaque trou doit être changé en indisponibilité
            ArrayList<Indisponibilite> retour = new ArrayList<>();
            for (Trou trou : trous) {
                retour.add(new Indisponibilite(trou.getCopyOf(), null));
            }
            return retour;
        } catch (Exception e) {
            return new ArrayList<>();
        }

    }

    private Collection<? extends Trou> remplir_jours_exterieurs_vides(TreeSet<? extends Evenement> evenementIHMS) {
        TreeSet<Trou> trous = new TreeSet<>();
        if (!evenementIHMS.isEmpty()) {
            LocalDate premierJour = evenementIHMS.first().getDebut().toLocalDate();
            LocalDate dernierJour = evenementIHMS.last().getDebut().toLocalDate();
            Semainier semainier = new Semainier(premierJour);
            for (LocalDate iter = semainier.getLundi();
                 iter.isBefore(semainier.getDimanche().plusDays(1));
                 iter = iter.plusDays(1)) {
                boolean estAvantPremier = iter.getDayOfWeek().getValue() < premierJour.getDayOfWeek().getValue();
                boolean estApresDernier = iter.getDayOfWeek().getValue() > dernierJour.getDayOfWeek().getValue();
                if (estAvantPremier || estApresDernier) {
                    Trou trou = new Trou(evenementIHMS.first().getNomVariables(), null);
                    trou.setDebut_Duree(iter.atStartOfDay(), 1439);
                    trous.add(trou);
                }
            }
        }
        return trous;
    }

    public void setIndisponibilites(TreeSet<Indisponibilite> collection) {
        indisponibilites.clear();
        indisponibilites.addAll(collection);
        update_affichage();
    }

    public void clearSelection() {
        getChildren().forEach(noeud -> {
            if (noeud instanceof ListeJour) {
                ((ListeJour) noeud).clearSelection();
            }
        });
    }

    public ArrayList<Evenement> getEvenementsAffiches() {
        ArrayList<Evenement> sortie = new ArrayList<>(listeJourLundi.getEvenements());
        if (nombreJoursAffiches < 2) return sortie;
        sortie.addAll(listeJourMardi.getEvenements());
        if (nombreJoursAffiches < 3) return sortie;
        sortie.addAll(listeJourMercredi.getEvenements());
        if (nombreJoursAffiches < 4) return sortie;
        sortie.addAll(listeJourJeudi.getEvenements());
        if (nombreJoursAffiches < 5) return sortie;
        sortie.addAll(listeJourVendredi.getEvenements());
        if (nombreJoursAffiches < 6) return sortie;
        sortie.addAll(listeJourSamedi.getEvenements());
        if (nombreJoursAffiches < 7) return sortie;
        sortie.addAll(listeJourDimanche.getEvenements());
        return sortie;
    }


    public void setGeneral(boolean b) {
        listeJourLundi.setGeneral(b);
        listeJourMardi.setGeneral(b);
        listeJourMercredi.setGeneral(b);
        listeJourJeudi.setGeneral(b);
        listeJourVendredi.setGeneral(b);
        listeJourSamedi.setGeneral(b);
        listeJourDimanche.setGeneral(b);
    }

    /**
     * Retourne la liste des événements d'un jour
     *
     * @param date jour de référence
     * @return liste vide si le jour rentré sort de la zone affichée
     */
    public ArrayList<Evenement> getEvenementsJour(LocalDate date) {
        switch (date.getDayOfWeek()) {
            case MONDAY:
                return listeJourLundi.getEvenements();
            case TUESDAY:
                return listeJourMardi.getEvenements();
            case WEDNESDAY:
                return listeJourMercredi.getEvenements();
            case THURSDAY:
                return listeJourJeudi.getEvenements();
            case FRIDAY:
                return listeJourVendredi.getEvenements();
            case SATURDAY:
                return listeJourSamedi.getEvenements();
            case SUNDAY:
                return listeJourDimanche.getEvenements();
            default:
                return new ArrayList<>();
        }

    }

    /**
     * Retourne la liste des événements d'un jour, y compris les indisponibilités
     *
     * @param date jour de référence
     * @return liste vide si le jour rentré sort de la zone affichée
     */
    public ArrayList<Evenement> getEvenementsJourFull(LocalDate date) {
        switch (date.getDayOfWeek()) {
            case MONDAY:
                return listeJourLundi.getEvenementsFull();
            case TUESDAY:
                return listeJourMardi.getEvenementsFull();
            case WEDNESDAY:
                return listeJourMercredi.getEvenementsFull();
            case THURSDAY:
                return listeJourJeudi.getEvenementsFull();
            case FRIDAY:
                return listeJourVendredi.getEvenementsFull();
            case SATURDAY:
                return listeJourSamedi.getEvenementsFull();
            case SUNDAY:
                return listeJourDimanche.getEvenementsFull();
            default:
                return new ArrayList<>();
        }

    }

    /**
     * Retourne tous les événements voisins d'un événement (le même jour)
     *
     * @param Evenement l'événement
     * @return la liste de ses voisins
     */
    public ArrayList<Evenement> getEvenementsVoisinsJour(Evenement Evenement) {
        return getEvenementsJour(Evenement.getDebut().toLocalDate());
    }

    /**
     * @return Le nombre de minutes total de tous les événements
     * affichés de l'agenda
     */
    public Long getTotalMinutesAffichees() {
        long retour = 0;
        for (Evenement Evenement : listeJourLundi.getEvenements()) {
            retour += ChronoUnit.MINUTES.between(Evenement.getDebut(), Evenement.getFin());
        }
        if (nombreJoursAffiches < 2) return retour;
        for (Evenement Evenement : listeJourMardi.getEvenements()) {
            retour += ChronoUnit.MINUTES.between(Evenement.getDebut(), Evenement.getFin());
        }
        if (nombreJoursAffiches < 3) return retour;
        for (Evenement Evenement : listeJourMercredi.getEvenements()) {
            retour += ChronoUnit.MINUTES.between(Evenement.getDebut(), Evenement.getFin());
        }
        if (nombreJoursAffiches < 4) return retour;
        for (Evenement Evenement : listeJourJeudi.getEvenements()) {
            retour += ChronoUnit.MINUTES.between(Evenement.getDebut(), Evenement.getFin());
        }
        if (nombreJoursAffiches < 5) return retour;
        for (Evenement evt : listeJourVendredi.getEvenements()) {
            retour += ChronoUnit.MINUTES.between(evt.getDebut(), evt.getFin());
        }
        if (nombreJoursAffiches < 6) return retour;
        for (Evenement Evenement : listeJourSamedi.getEvenements()) {
            retour += ChronoUnit.MINUTES.between(Evenement.getDebut(), Evenement.getFin());
        }
        if (nombreJoursAffiches < 7) return retour;
        for (Evenement Evenement : listeJourDimanche.getEvenements()) {
            retour += ChronoUnit.MINUTES.between(Evenement.getDebut(), Evenement.getFin());
        }
        return retour;
    }

    /**
     * @return Le nombre de minutes total de tous les événements
     * de l'agenda
     */
    public Long getTotalMinutes() {
        long retour = 0;
        for (Evenement Evenement : listeJourLundi.getEvenements()) {
            retour += ChronoUnit.MINUTES.between(Evenement.getDebut(), Evenement.getFin());
        }
        for (Evenement Evenement : listeJourMardi.getEvenements()) {
            retour += ChronoUnit.MINUTES.between(Evenement.getDebut(), Evenement.getFin());
        }
        for (Evenement Evenement : listeJourMercredi.getEvenements()) {
            retour += ChronoUnit.MINUTES.between(Evenement.getDebut(), Evenement.getFin());
        }
        for (Evenement Evenement : listeJourJeudi.getEvenements()) {
            retour += ChronoUnit.MINUTES.between(Evenement.getDebut(), Evenement.getFin());
        }
        for (Evenement Evenement : listeJourVendredi.getEvenements()) {
            retour += ChronoUnit.MINUTES.between(Evenement.getDebut(), Evenement.getFin());
        }
        for (Evenement Evenement : listeJourSamedi.getEvenements()) {
            retour += ChronoUnit.MINUTES.between(Evenement.getDebut(), Evenement.getFin());
        }
        for (Evenement Evenement : listeJourDimanche.getEvenements()) {
            retour += ChronoUnit.MINUTES.between(Evenement.getDebut(), Evenement.getFin());
        }
        return retour;
    }

    public boolean isCouper() {
        if (conteneur == null) {
            return false;
        }
        return conteneur.isCouper();
    }

    void setCouper(boolean b) {
        if (conteneur != null) {
            conteneur.setCouper(b);
        }
    }

    public void initialize() {
        setPadding(new Insets(5, 2, 5, 2));
        setSpacing(5);
        BackgroundFill bf = new BackgroundFill(Color.GAINSBORO, new CornerRadii(1), null);
        setBackground(new Background(bf));
        setFillHeight(true);

        jourAffiche = (new Semainier(LocalDate.now())).getLundi();

        listeJourLundi = new ListeJour(jourAffiche, evenements, heureOuverture, heureFermeture, 60, this, NOMS_VARIABLES);
        listeJourMardi = new ListeJour(jourAffiche.plusDays(1), evenements, heureOuverture, heureFermeture, 60, this, NOMS_VARIABLES);
        listeJourMercredi = new ListeJour(jourAffiche.plusDays(2), evenements, heureOuverture, heureFermeture, 60, this, NOMS_VARIABLES);
        listeJourJeudi = new ListeJour(jourAffiche.plusDays(3), evenements, heureOuverture, heureFermeture, 60, this, NOMS_VARIABLES);
        listeJourVendredi = new ListeJour(jourAffiche.plusDays(4), evenements, heureOuverture, heureFermeture, 60, this, NOMS_VARIABLES);
        listeJourSamedi = new ListeJour(jourAffiche.plusDays(5), evenements, heureOuverture, heureFermeture, 60, this, NOMS_VARIABLES);
        listeJourDimanche = new ListeJour(jourAffiche.plusDays(6), evenements, heureOuverture, heureFermeture, 60, this, NOMS_VARIABLES);
        setHgrow(listeJourDimanche, Priority.ALWAYS);
        setHgrow(listeJourMardi, Priority.ALWAYS);
        setHgrow(listeJourLundi, Priority.ALWAYS);
        setHgrow(listeJourMercredi, Priority.ALWAYS);
        setHgrow(listeJourJeudi, Priority.ALWAYS);
        setHgrow(listeJourVendredi, Priority.ALWAYS);
        setHgrow(listeJourSamedi, Priority.ALWAYS);
        update_affichage();
    }

    /**
     * Retire de la liste d'événements, le premier avec l'id rentré
     *
     * @param id rentré qui doit être différent de 0, sinon, rien n'est fait !
     */
    public void remove_evenement_with_id(int id) {
        if (id > 0) {
            Evenement aTuer = null;
            for (Evenement evt : evenements) {
                if (evt.getId() == id) {
                    aTuer = evt;
                    break;
                }
            }
            if (aTuer != null) {
                evenements.remove(aTuer);
            }
        }
        update_affichage();
    }

    public void select(Evenement Evenement) {
        ListeJour listeJour = getListeJourAtDate(Evenement.getDebut());
        if (listeJour == null) {
            System.err.println("*** Tentative de sélection de " + Evenement + " dans la listeJou" + " du " + Evenement.getDebut().format(DateTimeFormatter.ofPattern("dd/MM/yy")));
            return;
        }
        listeJour.select(Evenement);
    }

    public ListeJour getListeJourAtDate(LocalDateTime date) {
        return getListeJourAtDate(date.getDayOfWeek());
    }

    public ListeJour getListeJourAtDate(DayOfWeek jour) {
        switch (jour) {
            case MONDAY:
                return listeJourLundi;
            case TUESDAY:
                return listeJourMardi;
            case WEDNESDAY:
                return listeJourMercredi;
            case THURSDAY:
                return listeJourJeudi;
            case FRIDAY:
                return listeJourVendredi;
            case SATURDAY:
                return listeJourSamedi;
            case SUNDAY:
                return listeJourDimanche;
            default:
                return null;
        }
    }
}
