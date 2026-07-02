package controlsFX.Calendrier;

import general.calendrier.*;
import generalFX.FX_RGBConverter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Callback;

import java.awt.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ListeJour extends BorderPane implements Comparable {

    private final TreeSet<Evenement> evenements = new TreeSet<>();
    private final AgendaSemaineListe agenda;
    private final TreeSet<Indisponibilite> indisponibilites = new TreeSet<>();
    private ListView<Evenement> listView = new ListView<>();
    private LocalDate dateJour;
    private ContextMenuController contextMenu;
    private boolean general = false;
    private LocalTime heureDebut, heureFin;
    private int dureeBloc;
    private Label lblEntete = new Label();
    private String NOM_TYPE, NOM_ID, NOM_DESCRIPTION, NOM_COULEUR, NOM_IDINTERMEDIAIRE,
            NOM_DATEDU, NOM_DATEAU, NOM_COULEUR_CADRE, NOM_COULEUR_FOND, NOM_LIEU;


    /**
     * Permet de créer une liste d'événements d'agenda jour
     *
     * @param date              le jour
     * @param heureDebutJournee debut des heures de bureau
     * @param heureFinJourneee  fin des heures de bureau
     * @param dureeParDefaut    durée des blocs d'événement
     * @param origine           L'agenda qui contient la présente listeJour
     * @param listeNoms         les noms des variables utilisées pour les données.
     *                          Dans l'ordre :
     *                          * nomType           nom de la variable qui contient le type : Evenement, rdv, etc
     *                          * nomID             nom de la variable qui contient l'id
     *                          * nomDescription    nom de la variable qui contient la description
     *                          * nomCouleurTexte   nom de la variable qui contient la couleur du texte
     *                          * nomIDProprietaire nom de la variable qui contient l'id du propriétaire
     *                          * nomDateDebut      nom de la variable qui contient la date du début
     *                          * nomDateFin        nom de la variable qui contient la date de fin
     *                          * nomLieu
     */
    public ListeJour(
            LocalDate date,
            LocalTime heureDebutJournee,
            LocalTime heureFinJourneee,
            int dureeParDefaut,
            AgendaSemaineListe origine,
            ArrayList<String> listeNoms) {
        dateJour = date;
        heureDebut = heureDebutJournee;
        heureFin = heureFinJourneee;
        dureeBloc = dureeParDefaut;
        agenda = origine;
        contextMenu = new ContextMenuController();
        initialiser_nom_variables(listeNoms);
        initialiser();
    }

    public ListeJour(
            LocalDate date,
            Collection<Evenement> events,
            LocalTime heureDebutJournee,
            LocalTime heureFinJourneee,
            int dureeParDefaut,
            AgendaSemaineListe origine,
            ArrayList<String> listeNoms) {
        dateJour = date;
        heureDebut = heureDebutJournee;
        heureFin = heureFinJourneee;
        dureeBloc = dureeParDefaut;
        agenda = origine;
        contextMenu = new ContextMenuController();
        listView.setContextMenu(contextMenu);

        events.forEach(elt -> {
            if (elt.getDebut().toLocalDate().equals(date)) {
                evenements.add(elt);
            }
        });

        initialiser_nom_variables(listeNoms);
        initialiser();
    }

    private void initialiser_nom_variables(ArrayList<String> als) {
        int taille = als.size();
        if (taille < 10) {
            System.err.println("*** ListeJour : La liste des noms de variables n'est pas bonne !" +
                    " Nombre d'éléments manquants : " + Math.min(0, (taille - 10)));
            return;
        }
        NOM_TYPE = als.get(0);
        NOM_ID = als.get(1);
        NOM_DESCRIPTION = als.get(2);
        NOM_COULEUR = als.get(3);
        NOM_IDINTERMEDIAIRE = als.get(4);
        NOM_DATEDU = als.get(5);
        NOM_DATEAU = als.get(6);
        NOM_COULEUR_CADRE = als.get(7);
        NOM_COULEUR_FOND = als.get(8);
        NOM_LIEU = als.get(9);
    }


    private void action_delete(KeyEvent keyEvent) {
        if (agenda.isEditable()) {
            KeyCode touche = keyEvent.getCode();
            if (touche.equals(KeyCode.DELETE)
                    || touche.equals(KeyCode.BACK_SPACE)) {
                supprimer();
            }
            return;
        }
        afficher_utilisateur("Agenda non modifiable !");
    }


    private void action_clic_gauche(MouseEvent mouseEvent) {
        if (agenda.isEditable()) {
            int nbrClic = mouseEvent.getClickCount();
            Evenement selection = listView.getSelectionModel().getSelectedItem();
            if (selection != null) {
                if (nbrClic > 1) {
                    editer();
                }
            }
            agenda.getConteneur().afficher_evenement(selection);
            return;
        }
        clearSelection();
        agenda.getConteneur().afficher_evenement(null);
        afficher_utilisateur("Agenda non modifiable !");
    }

    public void addEvenement(Evenement evenement) {
        LocalDate dateEvenement = evenement.getDebut().toLocalDate();
        if (dateEvenement.equals(evenement.getFin().toLocalDate())) {
            if (dateEvenement.equals(dateJour)) {
                evenements.add(evenement);
                update_affichage();
                return;
            }
            System.err.println("Attention : tentative d'insertion d'un événement le " + dateEvenement
                    + " alors que la listeJour est datée du " + dateJour);
            return;
        }
        System.err.println("Attention : Impossible d'insérer un événement qui est sur plusieurs jours !" +
                " - Ignoré ! => " + evenement);
    }

    /**
     * Insère de nouveaux événements en plus des anciens ! Mais avant insertion,
     * je vérifie que le jour correspond bien à la date de la ListeJour ...
     *
     * @param evenements1 événements à insérer
     */
    public void addEvenements(Collection<? extends Evenement> evenements1) {

    }

    /**
     * Retourne les événements et les indisponibilités
     *
     * @return la liste complète des événements du jour
     */
    public ArrayList<Evenement> getEvenementsFull() {
        ArrayList<Evenement> listeRetour = new ArrayList<>(evenements);
        listeRetour.addAll(indisponibilites);
        return listeRetour;
    }

    /**
     * Retourne les événements sans les indisponibilités
     *
     * @return la liste complète des événements du jour, sans les indisponibilités
     */
    public ArrayList<Evenement> getEvenements() {
        return new ArrayList<>(evenements);
    }

    /**
     * Retourne les indisponibilités
     *
     * @return la liste complète des indisponibilités du jour
     */
    public ArrayList<Evenement> getIndisponibilites() {
        return new ArrayList<>(indisponibilites);
    }

    public void setEvenements(LocalDate date, ArrayList<? extends Evenement> collection) {
        setDateJour(date);
        evenements.clear();
        evenements.addAll(collection.stream()
                .filter(elt ->
                        (elt.getDebut().toLocalDate().equals(dateJour) &&
                                elt.getFin().toLocalDate().equals(dateJour))).collect(Collectors.toList()));

        update_affichage();
    }

    public void setIndisponibilites(LocalDate date, ArrayList<Indisponibilite> collection) {
        indisponibilites.clear();
        indisponibilites.addAll(collection.stream()
                .filter(elt ->
                        (elt.getDebut().toLocalDate().equals(dateJour) &&
                                elt.getFin().toLocalDate().equals(dateJour))).collect(Collectors.toList()));
        update_affichage();
    }

    public ArrayList<Evenement> getEvenementsVoisins(Evenement evt) {
        ArrayList<Evenement> terug = new ArrayList<>();
        for (Evenement evenement : getEvenements()) {
            FusionneurEvenements fusionneurEvenements = new FusionneurEvenements();
            if (fusionneurEvenements.telescoper(evt, evenement).isEmpty()) {
                terug.add(evenement);
            }
        }
        return terug;
    }

    public LocalTime getHeureDebut() {
        return heureDebut;
    }

    public void setHeureDebut(LocalTime heureDebut) {
        this.heureDebut = heureDebut;
    }

    public LocalTime getHeureFin() {
        return heureFin;
    }

    public void setHeureFin(LocalTime heureFin) {
        this.heureFin = heureFin;
    }

    public int getDureeBloc() {
        return dureeBloc;
    }

    public void setDureeBloc(int dureeBloc) {
        this.dureeBloc = dureeBloc;
    }

    public void nouveau() {

        Evenement evenement = new Evenement(NOM_TYPE, NOM_ID, NOM_DESCRIPTION,
                NOM_COULEUR, NOM_IDINTERMEDIAIRE, NOM_DATEDU, NOM_DATEAU,
                NOM_COULEUR_CADRE, NOM_COULEUR_FOND, NOM_LIEU, null);
        //System.out.println("* : Liste jour : Initialisation d'un nouvel événement le " + evenement);
        //System.out.println(">>> : Liste jour : " + dateJour);

        Object selection = listView.getSelectionModel().getSelectedItem();
        if (selection instanceof Trou) {
            Trou trou = (Trou) selection;
            evenement.setDebut_Duree(trou.getDebut(),
                    trou.getDuree());
            // System.out.println("* : Liste jour : nouvel événement remplaçant un trou, le " + evenement);
        } else {
            evenement = charger_meilleur_candidat();
            // System.out.println("* : Liste jour : nouvel événement inséré en dernier car une sélection, le " + evenement);
        }

        agenda.getConteneur().ajouter(evenement);
    }

    private Evenement charger_meilleur_candidat() {
        Evenement evenement = new Evenement(NOM_TYPE, NOM_ID, NOM_DESCRIPTION,
                NOM_COULEUR, NOM_IDINTERMEDIAIRE, NOM_DATEDU, NOM_DATEAU,
                NOM_COULEUR_CADRE, NOM_COULEUR_FOND, NOM_LIEU, null);
        Trou premierTrou = chercher_premier_trou();
        // on cherche le premier trou
        if (premierTrou != null) {
            evenement.setDebut_Fin(premierTrou.getDebut(), premierTrou.getFin());
            return evenement;
        }
        // on cherche le dernier événement
        Evenement dernierDeListe = evenements.last();
        LocalDateTime limite = dateJour.atTime(23, 50);
        LocalDateTime finDernier;
        if (dernierDeListe != null) {
            finDernier = dernierDeListe.getFin();
            if (finDernier.isBefore(limite)) {
                evenement.setDebut_Duree(finDernier, 20);
            } else {
                evenement.setDebut_Duree(dateJour.atTime(12, 0), 20);
            }
        } else {
            evenement.setDebut_Fin(dateJour.atTime(12, 0), dateJour.atTime(12, 20));
        }
        return evenement;
    }

    private Trou chercher_premier_trou() {
        for (Evenement evenementIHM : listView.getItems()) {
            if (evenementIHM instanceof Trou) {
                return (Trou) evenementIHM;
            }
        }
        return null;
    }

    private void update_affichage() {
        Platform.runLater(() -> {
            if (general) {
                lblEntete.setText(dateJour.format(DateTimeFormatter.ofPattern("eeee")));
            } else {
                lblEntete.setText(dateJour.format(DateTimeFormatter.ofPattern("eee dd/MM/yy")));
            }
            TreeSet<Evenement> listeIntermediaire = inserer_trous(indisponibilites,
                    heureDebut,
                    heureFin,
                    dureeBloc);
            TreeSet<Evenement> listeAAfficher = inserer_evenements(evenements,
                    listeIntermediaire);
            ObservableList<Evenement> observableList = FXCollections.
                    observableArrayList(new ArrayList<>(listeAAfficher));
            listView.setItems(observableList);
            listView.refresh();
        });

        //regenerer_affichage_car_mauvais_refresh_javafx(listeAAfficher);
    }


    /**
     * Permet d'insérer des trous où nécessaire au milieu d'événements avec des
     * trous de taille choisie
     *
     * @param evenements  la liste d'événements à entourer
     * @param hDebut      heure à partir de laquelle les blocs de trous sont découpés
     *                    par durée de blocMinutes
     * @param hFin        heure après laquelle les blocs de trous ne sont plus découpés
     *                    par durée de blocMinutes
     * @param blocMinutes c'est la durée max d'un trou s'il est inséré entre
     *                    heureDebut et heureFin
     * @return la liste de départ entourée de trous de taille adéquate
     */
    private TreeSet<Evenement> inserer_trous(
            TreeSet<? extends Evenement> evenements,
            LocalTime hDebut,
            LocalTime hFin,
            int blocMinutes) {
        TreeSet<Trou> blocsTrou = generer_blocs_trous(getDateJour(), hDebut, hFin, blocMinutes);
        TreeSet<Evenement> back = new TreeSet<Evenement>(blocsTrou);

        for (Evenement evenement : evenements) {
            FusionneurAgendas fusionneur = new FusionneurAgendas(NOM_TYPE, NOM_ID, NOM_DESCRIPTION,
                    NOM_COULEUR, NOM_IDINTERMEDIAIRE, NOM_DATEDU, NOM_DATEAU,
                    NOM_COULEUR_CADRE, NOM_COULEUR_FOND, NOM_LIEU, null);

            fusionneur.inserer_prioritaire(evenement, back);

            back = new TreeSet<>();
            back.addAll(fusionneur.getCrees());
            back.addAll(fusionneur.getSauves());
            back.addAll(fusionneur.getTronques());

        }

        return back;
    }

    /**
     * Permet d'insérer des événements avec des indisponibilités et des
     * trous
     *
     * @param evenements         la liste d'événements à insérer dans la listeJour
     * @param listeIntermediaire la liste d'événements déjà présents (trous et indisponibilité)
     * @return la liste de départ entourée de trous de taille adéquate
     */
    private TreeSet<Evenement> inserer_evenements(TreeSet<? extends Evenement> evenements,
                                                  TreeSet<Evenement> listeIntermediaire) {

        TreeSet<Evenement> back = new TreeSet<Evenement>(listeIntermediaire);

        for (Evenement evenement : new TreeSet<>(evenements)) {
            FusionneurAgendas fusionneur = new FusionneurAgendas(NOM_TYPE, NOM_ID, NOM_DESCRIPTION,
                    NOM_COULEUR, NOM_IDINTERMEDIAIRE, NOM_DATEDU, NOM_DATEAU,
                    NOM_COULEUR_CADRE, NOM_COULEUR_FOND, NOM_LIEU, null);

            fusionneur.inserer_prioritaire(evenement, back);

            back = new TreeSet<>();
            back.addAll(fusionneur.getCrees());
            back.addAll(fusionneur.getSauves());
            back.addAll(fusionneur.getTronques());

        }

        return back;
    }

    private TreeSet<Trou> generer_blocs_trous(
            LocalDate jour,
            LocalTime heureDebut,
            LocalTime heureFin,
            int blocMinutes) {

        if (heureDebut == null) {
            heureDebut = LocalTime.of(0, 0, 0);
        }
        if (heureFin == null) {
            heureFin = LocalTime.of(23, 59, 59);
        }
        TreeSet<Trou> terug = new TreeSet<>();

        // premier bloc
        Trou premierTrou = new Trou(NOM_TYPE, NOM_ID, NOM_DESCRIPTION,
                NOM_COULEUR, NOM_IDINTERMEDIAIRE, NOM_DATEDU, NOM_DATEAU,
                NOM_COULEUR_CADRE, NOM_COULEUR_FOND, NOM_LIEU, null);
        premierTrou.setDebut_Duree(jour.atStartOfDay(), 1439);

        if (heureDebut.isAfter(LocalTime.MIDNIGHT)) {
            premierTrou.setDebut_Fin(jour.atStartOfDay(), jour.atTime(heureDebut));
            terug.add(premierTrou);
            //System.err.println("Création du premier Trou : " + premierTrou);
        }

        // blocs de blocMinutes
        int reserve = (int) ChronoUnit.MINUTES.between(heureDebut, heureFin);
        LocalDateTime debutBloc = jour.atTime(heureDebut);
        LocalDateTime finBloc = jour.atTime(heureFin);
        while (reserve > blocMinutes) {

            Trou bloc = new Trou(NOM_TYPE, NOM_ID, NOM_DESCRIPTION,
                    NOM_COULEUR, NOM_IDINTERMEDIAIRE, NOM_DATEDU, NOM_DATEAU,
                    NOM_COULEUR_CADRE, NOM_COULEUR_FOND, NOM_LIEU, null);
            if (finBloc.isAfter(debutBloc)) {
                bloc.setDebut_Fin(debutBloc, finBloc);
                if (bloc.getDuree() > blocMinutes) {
                    bloc.setDebut_Duree(bloc.getDebut(), blocMinutes);
                    terug.add(bloc);
                    debutBloc = bloc.getFin();
                    finBloc = jour.atTime(heureFin);
                } else {
                    if (bloc.getDuree() > 1) {
                        terug.add(bloc);
                    }
                }
            }
            reserve -= blocMinutes;
        }

        // dernier bloc
        if (finBloc.isBefore(finBloc.toLocalDate().atTime(23, 59, 1))) {
            Trou dernierTrou = new Trou(NOM_TYPE, NOM_ID, NOM_DESCRIPTION,
                    NOM_COULEUR, NOM_IDINTERMEDIAIRE, NOM_DATEDU, NOM_DATEAU,
                    NOM_COULEUR_CADRE, NOM_COULEUR_FOND, NOM_LIEU, null);
            dernierTrou.setDebut_Fin(finBloc, finBloc.toLocalDate().atTime(23, 59, 59));
            if (dernierTrou.getDuree() > 1) {
                terug.add(dernierTrou);
            }
        }

        if (terug.isEmpty()) {
            Trou trou = new Trou(NOM_TYPE, NOM_ID, NOM_DESCRIPTION,
                    NOM_COULEUR, NOM_IDINTERMEDIAIRE, NOM_DATEDU, NOM_DATEAU,
                    NOM_COULEUR_CADRE, NOM_COULEUR_FOND, NOM_LIEU, null);
            trou.setDebut_Duree(jour.atStartOfDay(), 1439);
            terug.add(trou);

        }

        return terug;
    }

    /**
     * Donne la liste des éléments sélectionnés hormis les trous !
     *
     * @return la liste des éléments sélectionnés hormis les trous !
     */
    public TreeSet<Evenement> getEvenementsSelectionnes() {
        TreeSet<Evenement> retour = new TreeSet<>();
        listView.getSelectionModel().getSelectedItems().forEach(elt -> {
            if (!(elt instanceof Trou)) {
                retour.add(elt);
            }
        });
        return retour;
    }

    private void afficher_utilisateur(String s) {
        agenda.getConteneur().afficheUtilisateur(s);
    }

    public void editer() {
        TreeSet<Evenement> selections = getEvenementsSelectionnes();
        if (selections.isEmpty()) {
            nouveau();
            return;
        }
        ArrayList<Evenement> events = new ArrayList<>(selections);
        agenda.getConteneur().modifier(events);

    }

    public void dupliquer() {
        TreeSet<Evenement> selections = getEvenementsSelectionnes();
        if (getEvenementsSelectionnes().isEmpty()) {
            afficher_utilisateur("Rien à dupliquer. Il faut d'abord sélectionner un événement");
        } else {

            if (selections.size() > 1) {
                afficher_utilisateur("Duplication du premier élément de la sélection."
                        + "Les autres seront ignorés.");
            }
            Evenement selection = selections.first();
            long minutes = selection.getDuree();
            Evenement nouveau = selection.getCopyOf();
            nouveau.setId(0);
            if (minutes > 0) {
                nouveau.setDebut_Fin(selection.getFin(),
                        selection.getFin().plusMinutes(minutes));

                nouveau.setDescription(selection.getDescription());

                Trou trou = (new ChercheurTrou(NOM_TYPE, NOM_ID, NOM_DESCRIPTION, NOM_COULEUR, NOM_IDINTERMEDIAIRE,
                        NOM_DATEDU, NOM_DATEAU, NOM_COULEUR_CADRE, NOM_COULEUR_FOND, NOM_LIEU, null)).chercher_meilleur_trou(nouveau,
                        extraire_trous(listView.getItems()));
                if (trou.getDuree() > 10) {
                    nouveau.setDebut_Duree(trou.getDebut(), trou.getDuree());
                    getEvenements().add(nouveau);
                    agenda.getConteneur().enregistrer(nouveau, "Faible");
                } else {
                    afficher_utilisateur("Pas assez de place pour insérer un doublon");
                }
            } else {
                afficher_utilisateur("Evénement à dupliquer inexistant !");
            }
        }
    }

    private TreeSet<Trou> extraire_trous(ObservableList<Evenement> items) {
        TreeSet<Trou> retour = new TreeSet<>();
        for (Evenement evenement : items) {
            if (evenement instanceof Trou) {
                retour.add((Trou) evenement);
            }
        }
        return retour;
    }

    public void supprimer() {
        TreeSet<Evenement> aSupprimer = getEvenementsSelectionnes();
        if (aSupprimer.isEmpty()) {
            afficher_utilisateur("Rien à supprimer. Il faut d'abord sélectionner un événement");
            return;
        }

        int taille = aSupprimer.size();
        if (taille == 1) {
            agenda.getConteneur().demande_suppression_liste(new ArrayList<>(aSupprimer));
            afficher_utilisateur("Suppression d'un événement : " + aSupprimer.first());
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Veuillez confirmer la suppression"
                + " des " + taille + " éléments\n"
                + "Ceci est irréversible !");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                agenda.getConteneur().demande_suppression_liste(new ArrayList<>(aSupprimer));
            } else {
                afficher_utilisateur("Annulation de l'opération de suppression");
            }
        });
    }

    /*public void etendre_haut() {
        if (getEvenementsSelectionnes().isEmpty()) {
            afficher_utilisateur("Rien à étendre. Il faut d'abord sélectionner un événement");
        } else {
            Evenement selection = getEvenementsSelectionnes().get(0);
            LocalDate date = selection.getDebut().toLocalDate();
            TreeSet<Evenement> listeDEvenements = new TreeSet<>(getEvenements());
            Evenement precedent = listeDEvenements.lower(selection);
            if (precedent == null) {
                selection.setDebut_Duree(date.atTime(0, 0), selection.getDuree());
            } else {
                if (precedent.getFin().isBefore(selection.getDebut().minusSeconds(1))) {
                    selection.setDebut_Fin(precedent.getFin(), selection.getFin());
                    agenda.getConteneur().enregistrer(selection, "Faible");
                }
            }
        }
    }

    public void etendre_bas() {
        if (getEvenementsSelectionnes().isEmpty()) {
            afficher_utilisateur("Rien à étendre. Il faut d'abord sélectionner un événement");
        } else {
            Evenement selection = getEvenementsSelectionnes().get(0);
            LocalDate date = selection.getDebut().toLocalDate();
            TreeSet<Evenement> listeDEvenements = new TreeSet<>(getEvenements());
            Evenement suivant = listeDEvenements.higher(selection);
            if (suivant == null) {
                long duree = ChronoUnit.MINUTES.between(selection.getDebut(), date.atTime(23, 59, 59));
                selection.setDebut_Duree(selection.getDebut(), duree);
            } else {
                if (suivant.getDebut().isAfter(selection.getFin().plusSeconds(1))) {
                    long duree = ChronoUnit.MINUTES.between(selection.getDebut(), suivant.getDebut());
                    selection.setDebut_Duree(selection.getDebut(), duree);
                    agenda.getConteneur().enregistrer(selection, "Faible");
                }
            }
        }
    }*/

    public LocalDate getDateJour() {
        return dateJour;
    }

    private void setDateJour(LocalDate date) {
        dateJour = date;
    }

    @Override
    public String toString() {
        return "Liste jour : [" + dateJour.format(DateTimeFormatter.ofPattern("dd/MM:yy")) + ", " + evenements + "]";
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof ListeJour) {
            return dateJour.compareTo(((ListeJour) o).getDateJour());
        }
        return 0;
    }

    public void clearSelection() {
        listView.getSelectionModel().clearSelection();
    }

    public boolean isGeneral() {
        return general;
    }

    /**
     * Permet d'informer la liste qu'il s'agit d'un agenda qui se répète
     * (général) Les jours de semaine seront affichés, et pas les dates
     * complètes
     *
     * @param general Est ce un agenda général?
     */
    public void setGeneral(boolean general) {
        this.general = general;
    }

    private void copier_dans_souris() {
        ObservableList<? extends Evenement> selections = listView.getSelectionModel().getSelectedItems();
        envoyer_liste_dans_agenda(selections);
        agenda.setCouper(false);
    }

    private void couper_dans_souris() {
        ObservableList<? extends Evenement> selections = listView.getSelectionModel().getSelectedItems();
        envoyer_liste_dans_agenda(selections);
        agenda.setCouper(true);
    }

    private void envoyer_liste_dans_agenda(ObservableList<? extends Evenement> selections) {
        if (selections.isEmpty()) {
            afficher_utilisateur("Aucune sélection détectée. Veuillez sélectionner l'événement à copier svp");
            agenda.setListeSouris(new TreeSet<>());
            return;
        }
        TreeSet<Evenement> versPressPapier = new TreeSet<>();
        selections.forEach(elt -> {
            if (!(elt instanceof Trou)) {
                versPressPapier.add(elt);
            }
        });
        if (versPressPapier.size() == 1) {
            afficher_utilisateur("Copie de " + versPressPapier.first() + " dans la souris");
        } else {
            afficher_utilisateur("Copie de " + versPressPapier.size() + " éléments dans la souris");
        }
        agenda.setListeSouris(versPressPapier);
    }

    private void coller() {
        if (agenda.getListeSouris().isEmpty()) {
            afficher_utilisateur("Contenu du press-papiers vide. Rien à coller ici.");
            return;
        }
        TreeSet<Evenement> nouvelleListe = new TreeSet<>(evenements);

        for (Evenement elt : agenda.getListeSouris()) {
            LocalTime hf = elt.getFin().toLocalTime();
            LocalTime hd = elt.getDebut().toLocalTime();
            Evenement nouvelEvenement = elt.getCopyOf();
            nouvelEvenement.setId(0);
            nouvelEvenement.setDebut_Fin(dateJour.atTime(hd), dateJour.atTime(hf));

            FusionneurAgendas fusionneur = new FusionneurAgendas(NOM_TYPE, NOM_ID, NOM_DESCRIPTION,
                    NOM_COULEUR, NOM_IDINTERMEDIAIRE, NOM_DATEDU, NOM_DATEAU,
                    NOM_COULEUR_CADRE, NOM_COULEUR_FOND, NOM_LIEU, null);
            fusionneur.inserer_malin(nouvelEvenement, nouvelleListe);

            nouvelleListe.clear();
            nouvelleListe.addAll(fusionneur.getCrees());
            nouvelleListe.addAll(fusionneur.getSauves());
            nouvelleListe.addAll(fusionneur.getTronques());
        }

        agenda.getConteneur().envoyerPrestations(nouvelleListe);

        //System.out.println("* Nouvelle liste à coller : " + nouvelleListe);
        afficher_utilisateur("Collage de " + nouvelleListe.size() + " élément(s)");
        agenda.addEvenements(nouvelleListe);
        if (agenda.isCouper()) {
            agenda.getConteneur().demande_suppression_liste(new ArrayList<>(agenda.getListeSouris()));
            agenda.setCouper(false);
        }
    }

    private void initialiser() {

        lblEntete.setPadding(new Insets(1, 1, 1, 5));

        setPadding(new Insets(5, 5, 5, 5));
        setBackground(new Background(new BackgroundFill(Color.LIGHTGRAY, CornerRadii.EMPTY, Insets.EMPTY)));

        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setBackground(new Background(new BackgroundFill(Color.ANTIQUEWHITE,
                new CornerRadii(10),
                null)));

        listView.setCellFactory(new Callback<ListView<Evenement>, ListCell<Evenement>>() {
            @Override
            public ListCell<Evenement> call(ListView<Evenement> list) {
                return new ColorRectCell();
            }
        });
        listView.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
            @Override
            public void handle(ContextMenuEvent event) {
                Point p = MouseInfo.getPointerInfo().getLocation();
                double cursorPositionX = p.getX();
                double cursorPositionY = p.getY();

                if (contextMenu != null) {
                    ObservableList<Evenement> items = listView.getItems();
                    boolean listePleine = getMinutesUtilisees() > 1435;
                    boolean listeVide = items.isEmpty();
                    boolean existeSelection = !listView.getSelectionModel().getSelectedItems().isEmpty();
                    boolean indispo_selectionnee = listView.getSelectionModel().getSelectedItem() instanceof Indisponibilite;
                    boolean uneSelection = listView.getSelectionModel().getSelectedItems().size() == 1;
                    boolean trouSelectionne = uneSelection
                            && listView.getSelectionModel().getSelectedItem() instanceof Trou;
                    boolean pressePapierVide = agenda.getListeSouris().isEmpty();

                    contextMenu.griser_menu_editer(!agenda.isEditable() ||
                            trouSelectionne || !existeSelection ||
                            indispo_selectionnee || listeVide);
                    contextMenu.griser_menu_copier(!agenda.isEditable() || trouSelectionne
                            || !existeSelection || indispo_selectionnee || listeVide);
                    contextMenu.griser_menu_supprimer(!agenda.isEditable() || trouSelectionne ||
                            !existeSelection || indispo_selectionnee || listeVide);
                    contextMenu.griser_menu_dupliquer(!agenda.isEditable() || trouSelectionne ||
                            !existeSelection || indispo_selectionnee || listeVide);
                    contextMenu.griser_menu_coller(!agenda.isEditable() || pressePapierVide ||
                            indispo_selectionnee || listePleine);
                    contextMenu.griser_menu_ajouter(!agenda.isEditable() ||
                            indispo_selectionnee || listePleine);
                    contextMenu.griser_menu_selectionner_tout(!agenda.isEditable() ||
                            indispo_selectionnee || listeVide);

                    //contextMenu.show(listView, event.getScreenX(), event.getScreenY());
                    //cursorPositionX,
                    //cursorPositionY);
                    if (indispo_selectionnee) {
                        afficher_utilisateur("Zone non éditable !");
                    }
                } else {
                    afficher_utilisateur("L'agenda n'est pas modifiable");
                }
            }
        });
        listView.setOnMousePressed(this::action_clic_gauche);
        listView.setOnKeyReleased(this::action_delete);

        Platform.runLater(() -> {
            listView.prefWidthProperty().bind(widthProperty());
            listView.prefHeightProperty().bind(heightProperty());

            setCenter(listView);
            setTop(lblEntete);
        });

    }

    public int getMinutesUtilisees() {
        int minutes = 0;
        for (Evenement evenementIHM : evenements) {
            minutes += evenementIHM.getDuree();
        }
        for (Evenement evenementIHM : indisponibilites) {
            minutes += evenementIHM.getDuree();
        }
        return minutes;
    }

    public void select(Evenement evenement) {
        listView.getSelectionModel().select(evenement);
    }

    /*
     ************** CLASSES INTERNES ******************************************
     */
    static class ColorRectCell extends ListCell<Evenement> {
        @FXML
        public Label label;
        @FXML
        public ImageView imageView;
        @FXML
        public Rectangle rectangle;
        @FXML
        HBox cellule;

        @Override
        public void updateItem(Evenement evenement, boolean empty) {
            // calling super here is very important - don't skip this!
            super.updateItem(evenement, empty);

            Color couleur = evenement == null ?
                    Color.DARKBLUE :
                    FX_RGBConverter.convert_rgb_2_color(evenement.getCouleur());
            Color couleurTexte = evenement == null ?
                    Color.DARKBLUE :
                    FX_RGBConverter.convert_rgb_2_color(evenement.getCouleurTexte());
            Color couleurFond = evenement == null ?
                    Color.WHITE :
                    FX_RGBConverter.convert_rgb_2_color(evenement.getCouleurFond());
            Color couleurCadre = evenement == null ?
                    Color.WHITE :
                    FX_RGBConverter.convert_rgb_2_color(evenement.getCouleurCadre());

            Text texte = evenement == null ? new Text("") : new Text(evenement.getCalendarString());
            //texte.setFont(Font.font("Verdana", FontWeight.BOLD, FontPosture.ITALIC, 35));

            Rectangle rect = new Rectangle(200, 35);

            texte.setFill(couleurTexte);
            rect.setFill(couleurFond);

            StackPane stack = new StackPane();
            stack.setMinWidth(0);
            stack.setPrefWidth(100);
            StackPane.setAlignment(texte, Pos.CENTER_LEFT);
            rect.widthProperty().bind(stack.widthProperty().subtract(-18));
            stack.setPadding(new Insets(-7, -5, -7, -5));
            stack.getChildren().add(rect);
            stack.getChildren().add(texte);

            setBorder(new Border(new BorderStroke(couleurCadre,
                    BorderStrokeStyle.SOLID,
                    new CornerRadii(4),
                    new BorderWidths(6))));

            setText(null);
            setGraphic(stack);

            /*if (empty || evenement == null) {
                setText(null);
                setGraphic(null);
                return;
            }*/


           /* FXMLLoader mLLoader = new FXMLLoader(getClass().getResource("/views/CelluleListeEvenement.fxml"));
            mLLoader.setController(this);

            try {
                mLLoader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Color couleurTexte = (new RGBConverter()).convert_rgb_2_color(evenement.getCouleurTexte());
            Color couleurFond = (new RGBConverter()).convert_rgb_2_color(evenement.getCouleurFond());

            label.setText(evenement.getCalendarString());
            label.setTextFill(couleurTexte);
            rectangle.setFill(couleurFond);
            setBorder(new Border(new BorderStroke((new RGBConverter()).convert_rgb_2_color(evenement.getCouleurCadre()),
                    BorderStrokeStyle.SOLID,
                    new CornerRadii(2),
                    new BorderWidths(1))));*/

        }
    }

    /*
     **************************************************************************
     */
    protected class ContextMenuController extends ContextMenu {

        @FXML
        private javafx.scene.control.MenuItem menuNouveau, menuEditer, menuDupliquer,
                menuCouper, menuCopier, menuColler, menuSupprimer, menuSelectionnerTout;

        protected ContextMenuController() {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ContextMenu.fxml"));

            fxmlLoader.setRoot(this);
            fxmlLoader.setController(this);

            try {
                fxmlLoader.load();
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }

        public void griser_menu_editer(boolean griser) {
            menuEditer.setDisable(griser);
        }

        public void griser_menu_ajouter(boolean griser) {
            menuNouveau.setDisable(griser);
        }

        public void griser_menu_copier(boolean griser) {
            menuCopier.setDisable(griser);
            menuCouper.setDisable(griser);
        }

        public void griser_menu_coller(boolean griser) {
            menuColler.setDisable(griser);
        }

        @FXML
        public void action_menu_nouveau(javafx.event.Event event) {
            System.out.println("* Sélection du menu contextuel ajout");
            nouveau();
        }

        @FXML
        public void action_menu_selectionner_tout(javafx.event.Event event) {
            System.out.println("* Sélection de tous les événements de la listeJour");
            Platform.runLater(() -> {
                int indice = 0;
                for (Evenement evenement : listView.getItems()) {
                    if (!(evenement instanceof Trou)) {
                        listView.getSelectionModel().select(indice);
                    }
                    indice++;
                }
            });

        }

        @FXML
        public void action_menu_editer(javafx.event.Event event) {
            System.out.println("* Sélection du menu contextuel édition");
            editer();
        }

        @FXML
        public void action_dupliquer(javafx.event.Event event) {
            System.out.println("* Sélection du menu contextuel duplication");
            dupliquer();
        }

        @FXML
        public void action_menu_copier(javafx.event.Event event) {
            copier_dans_souris();
        }

        @FXML
        public void action_menu_couper(javafx.event.Event event) {
            couper_dans_souris();
        }

        @FXML
        public void action_menu_coller(javafx.event.Event event) {
            coller();
        }

        @FXML
        public void action_supprimer(javafx.event.Event event) {
            System.out.println("* Sélection du menu contextuel suppression");
            supprimer();
        }

        public void griser_menu_supprimer(boolean b) {
            menuSupprimer.setDisable(b);
        }

        public void griser_menu_dupliquer(boolean b) {
            menuDupliquer.setDisable(b);
        }

        public void griser_menu_selectionner_tout(boolean b) {
            menuSelectionnerTout.setDisable(b);
        }
    }

}
