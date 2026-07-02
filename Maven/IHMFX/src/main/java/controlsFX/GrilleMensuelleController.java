package controlsFX;

import general.calendrier.Semainier;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.TreeSet;

public class GrilleMensuelleController extends BorderPane {

    private LocalDate dateCliquee = null, dateAffichee = null;
    private ArrayList<ToggleButton> toggleButtons = new ArrayList<>();
    @FXML
    private ToggleButton tb01, tb02, tb03, tb04, tb05, tb06, tb07, tb08, tb09, tb10,
            tb11, tb12, tb13, tb14, tb15, tb16, tb17, tb18, tb19, tb20, tb21, tb22,
            tb23, tb24, tb25, tb26, tb27, tb28, tb29, tb30, tb31, tb32, tb33, tb34, tb35;
    @FXML
    private Label lbl1, lbl2, lbl3, lbl4, lbl5, lblMoisAnnee;
    private ArrayList<Label> listeSemaine = new ArrayList<>();
    private TreeSet<LocalDate> datesSelectionnees = new TreeSet<>();
    private TreeSet<LocalDate> datesGrisees = new TreeSet<>();
    private ConteneurGrilleMensuelle conteneur = null;
    private LocalDate premiereDate = null;


    public GrilleMensuelleController() {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/fxml/GrilleMensuelle.fxml"));

        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @FXML
    public void initialize() {
        toggleButtons.add(tb01);
        toggleButtons.add(tb02);
        toggleButtons.add(tb03);
        toggleButtons.add(tb04);
        toggleButtons.add(tb05);
        toggleButtons.add(tb06);
        toggleButtons.add(tb07);
        toggleButtons.add(tb08);
        toggleButtons.add(tb09);
        toggleButtons.add(tb10);
        toggleButtons.add(tb11);
        toggleButtons.add(tb12);
        toggleButtons.add(tb13);
        toggleButtons.add(tb14);
        toggleButtons.add(tb15);
        toggleButtons.add(tb16);
        toggleButtons.add(tb17);
        toggleButtons.add(tb18);
        toggleButtons.add(tb19);
        toggleButtons.add(tb20);
        toggleButtons.add(tb21);
        toggleButtons.add(tb22);
        toggleButtons.add(tb23);
        toggleButtons.add(tb24);
        toggleButtons.add(tb25);
        toggleButtons.add(tb26);
        toggleButtons.add(tb27);
        toggleButtons.add(tb28);
        toggleButtons.add(tb29);
        toggleButtons.add(tb30);
        toggleButtons.add(tb31);
        toggleButtons.add(tb32);
        toggleButtons.add(tb33);
        toggleButtons.add(tb34);
        toggleButtons.add(tb35);

        listeSemaine.add(lbl1);
        listeSemaine.add(lbl2);
        listeSemaine.add(lbl3);
        listeSemaine.add(lbl4);
        listeSemaine.add(lbl5);

        LocalDate maintenant = LocalDate.now();
        setMoisAffiche(maintenant);
    }

    public ConteneurGrilleMensuelle getConteneur() {
        return conteneur;
    }

    public void setConteneur(ConteneurGrilleMensuelle conteur) {
        conteneur = conteur;
    }

    public Month getMoisAffiche() {
        return dateAffichee.getMonth();
    }

    /**
     * Permet de choisir le mois à afficher
     *
     * @param date date contenue dans le mois à afficher
     */
    public void setMoisAffiche(LocalDate date) {
        dateAffichee = date;
        updateAffichage();
    }


    private void updateAffichage() {
        LocalDate premierDuMois = LocalDate.of(dateAffichee.getYear(),
                dateAffichee.getMonth(),
                1);
        Semainier semainier = new Semainier(premierDuMois);
        LocalDate premierAAfficher = semainier.getLundi();

        charger_dates(premierAAfficher, premierDuMois);
    }

    public void griser(LocalDate date, boolean griser) {
        if (griser) {
            datesGrisees.add(date);
        } else {
            datesGrisees.remove(date);
        }
        updateAffichage();
    }


    public ToggleButton getToggleButtonAtDate(LocalDate date) {
        int index = (int) ChronoUnit.DAYS.between(premiereDate, date);
        try {
            return toggleButtons.get(index);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isSelectionnee(LocalDate date) {
        try {
            return getToggleButtonAtDate(date).isSelected();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retourne le premier jour du mois affiché
     *
     * @return premier jour du mois
     */
    public LocalDate getPremierJour() {
        return LocalDate.of(dateAffichee.getYear(), dateAffichee.getMonth(), 1);
    }

    /**
     * Grise les dates rentrées dans la grille
     *
     * @param dates  dates à griser
     * @param griser vrai s'il faut griser, faux s'il faut activer
     */
    public void griser(ArrayList<LocalDate> dates, boolean griser) {
        if (griser) {
            for (LocalDate date : dates) {
                datesGrisees.add(date);
            }
        } else {
            for (LocalDate date : dates) {
                datesGrisees.remove(date);
            }
        }
        updateAffichage();
    }


    /**
     * Renvoie une COPIE des dates sélectionnées
     *
     * @return La liste des dates sélectionnées
     */
    public ArrayList<LocalDate> getDatesSelectionnees() {
        return new ArrayList<>(datesSelectionnees);
    }

    public void setDatesSelectionnees(ArrayList<LocalDate> dates) {
        for (LocalDate date : dates) {
            datesSelectionnees.add(date);
        }
        updateAffichage();
    }

    /**
     * Renvoie une COPIE des dates grisées
     *
     * @return liste des dates grisées
     */
    public ArrayList<LocalDate> getDatesGrisees() {
        return new ArrayList<>(datesGrisees);
    }

    public void setDatesGrisees(ArrayList<LocalDate> dates) {
        for (LocalDate date : dates) {
            datesGrisees.add(date);
        }
        updateAffichage();
    }

    /**
     * Retourne une copie de la date cliquée
     *
     * @return liste des dates cliquées
     */
    public LocalDate getDateCliquee() {
        return dateCliquee == null ?
                null :
                LocalDate.of(dateCliquee.getYear(), dateCliquee.getMonth(), dateCliquee.getDayOfMonth());
    }

    @FXML
    public void action_toggle(ActionEvent actionEvent) {
        Platform.runLater(() -> {
            Object source = actionEvent.getSource();
            try {
                if (source instanceof ToggleButton) {
                    int index = toggleButtons.indexOf(source);
                    dateCliquee = premiereDate.plusDays(index);
                    if (((ToggleButton) source).isSelected()) {
                        LocalDate stock = LocalDate.from(dateCliquee);
                        datesSelectionnees.add(stock);
                    } else {
                        datesSelectionnees.remove(dateCliquee);
                    }
                }
                if (conteneur != null) {
                    conteneur.traiter_grilleMensuelle_clic(dateCliquee);
                } else {
                    System.out.println("** Aucun conteneur n'écoute les clics sur la grille !");
                }

            } catch (Exception e) {
                System.err.println("Problème dans la grille mensuelle : Impossible d'enregistrer la date cliquée");
                e.printStackTrace();
            }
        });
    }

    /**
     * Alimente les 35 zones du calendrier en données
     *
     * @param begin Date de début
     */
    private void charger_dates(LocalDate begin, LocalDate firstVisible) {
        Platform.runLater(() -> {
            Month mois = firstVisible.getMonth();
            dateCliquee = null;
            premiereDate = begin;
            for (int i = 0; i < 35; i++) {
                LocalDate dateEnCours = premiereDate.plusDays(i);

                if (i % 7 == 0) {
                    listeSemaine.get(i / 7).setText("" + (1 + ((52 * dateEnCours.getDayOfYear()) / 365)));
                }

                ToggleButton toggleButtonEnCours = toggleButtons.get(i);
                toggleButtonEnCours.setText(dateEnCours.format(DateTimeFormatter.ofPattern("dd/MM")));

                toggleButtonEnCours.setVisible(dateEnCours.getMonth().equals(mois));
                toggleButtonEnCours.setDisable(datesGrisees.contains(dateEnCours));
                toggleButtonEnCours.setSelected(datesSelectionnees.contains(dateEnCours));
            }

            lblMoisAnnee.setText(firstVisible.format(DateTimeFormatter.ofPattern("LLL yyyy")));
        });
    }

    public void selectionner(LocalDate date, boolean griser) {
        if (griser) {
            datesSelectionnees.add(date);
        } else {
            datesSelectionnees.remove(date);
        }
        updateAffichage();
    }

    /**
     * Sélectionne les dates rentrées dans la grille
     *
     * @param dates  dates à sélectionner
     * @param select vrai s'il faut griser, faux s'il faut activer
     */
    public void selectionner(ArrayList<LocalDate> dates, boolean select) {
        if (select) {
            for (LocalDate date : dates) {
                datesSelectionnees.add(date);
            }
        } else {
            for (LocalDate date : dates) {
                datesSelectionnees.remove(date);
            }
        }
        updateAffichage();
    }

}
