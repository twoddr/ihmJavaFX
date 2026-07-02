package controlsFX.Calendrier;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

public class CalendrierMensuelController extends GridPane {

    private LocalDate dateDebut = null, dateFin = null;
    private ArrayList<StackPane> stackPanes = new ArrayList<>();
    @FXML
    private Text t01, t02, t03, t04, t05, t06, t07, t11, t12, t13,
            t14, t15, t16, t17, t21, t22, t23, t24, t25, t26, t27, t31, t32, t33, t34, t35,
            t36, t37, t41, t42, t43, t44, t45, t46, t47, t51, t52, t53, t54, t55, t56, t57;

    @FXML
    private Rectangle r01, r02, r03, r04, r05, r06, r07, r11, r12, r13,
            r14, r15, r16, r17, r21, r22, r23, r24, r25, r26, r27, r31, r32, r33, r34, r35,
            r36, r37, r41, r42, r43, r44, r45, r46, r47, r51, r52, r53, r54, r55, r56, r57;

    @FXML
    private StackPane s01, s02, s03, s04, s05, s06, s07, s11, s12, s13,
            s14, s15, s16, s17, s21, s22, s23, s24, s25, s26, s27, s31, s32, s33, s34, s35,
            s36, s37, s41, s42, s43, s44, s45, s46, s47, s51, s52, s53, s54, s55, s56, s57;
    @FXML
    private Label lbl1, lbl2, lbl3, lbl4, lbl5, lbl6;
    private ArrayList<Label> listeSemaine = new ArrayList<>();
    private String nom = "";
    private Thread service = null;

    public CalendrierMensuelController() {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/fxml/CalendrierMensuel.fxml"));

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

        int annee = LocalDate.now().getYear();
        Month mois = LocalDate.now().getMonth();
        LocalDate premier = LocalDate.of(annee, mois, 1);

        stackPanes.add(s01);
        stackPanes.add(s02);
        stackPanes.add(s03);
        stackPanes.add(s04);
        stackPanes.add(s05);
        stackPanes.add(s06);
        stackPanes.add(s07);
        stackPanes.add(s11);
        stackPanes.add(s12);
        stackPanes.add(s13);
        stackPanes.add(s14);
        stackPanes.add(s15);
        stackPanes.add(s16);
        stackPanes.add(s17);
        stackPanes.add(s21);
        stackPanes.add(s22);
        stackPanes.add(s23);
        stackPanes.add(s24);
        stackPanes.add(s25);
        stackPanes.add(s26);
        stackPanes.add(s27);
        stackPanes.add(s31);
        stackPanes.add(s32);
        stackPanes.add(s33);
        stackPanes.add(s34);
        stackPanes.add(s35);
        stackPanes.add(s36);
        stackPanes.add(s37);
        stackPanes.add(s41);
        stackPanes.add(s42);
        stackPanes.add(s43);
        stackPanes.add(s44);
        stackPanes.add(s45);
        stackPanes.add(s46);
        stackPanes.add(s47);
        stackPanes.add(s51);
        stackPanes.add(s52);
        stackPanes.add(s53);
        stackPanes.add(s54);
        stackPanes.add(s55);
        stackPanes.add(s56);
        stackPanes.add(s57);

        listeSemaine.add(lbl1);
        listeSemaine.add(lbl2);
        listeSemaine.add(lbl3);
        listeSemaine.add(lbl4);
        listeSemaine.add(lbl5);
        listeSemaine.add(lbl6);

        setDates(premier, LocalDate.now().plusMonths(1));

    }

    public void setService(Thread s) {
        service = s;
    }

    public Month getMoisAffiche() {
        if (dateDebut != null) {
            return dateDebut.getMonth();
        }
        return LocalDate.now().getMonth();
    }

    public StackPane getStackPaneAtDate(LocalDate date) {
        int index = (int) ChronoUnit.DAYS.between(getPremierJour().atStartOfDay(), date.atStartOfDay());
        return stackPanes.get(index);
    }

    public Rectangle getRectangleAtDate(LocalDate date) {
        int index = (int) ChronoUnit.DAYS.between(getPremierJour().atStartOfDay(), date.atStartOfDay());
        StackPane stackPane = stackPanes.get(index);
        for (Node noeud : stackPane.getChildren()) {
            if (noeud instanceof Rectangle) {
                return (Rectangle) noeud;
            }
        }
        return null;
    }

    public Text getTextAtDate(LocalDate date) {
        int index = (int) ChronoUnit.DAYS.between(getPremierJour().atStartOfDay(), date.atStartOfDay());
        StackPane stackPane = stackPanes.get(index);
        for (Node noeud : stackPane.getChildren()) {
            if (noeud instanceof Text) {
                return (Text) noeud;
            }
        }
        return null;
    }

    public LocalDate getPremierJour() {
        return dateDebut.minusDays(dateDebut.getDayOfWeek().getValue() - 1);
    }

    /**
     * Permet de charger l'agenda contenant les dates rentrées
     *
     * @param debut date de la palge
     * @param fin   date de la plage
     */
    public void setDates(LocalDate debut, LocalDate fin) {
        if (debut.isAfter(fin)) {
            dateDebut = fin;
            dateFin = debut;
        } else {
            dateDebut = debut;
            dateFin = fin;
        }

        charger_dates(dateDebut, dateFin);
    }

    private void charger_dates(LocalDate begin, LocalDate end) {
        Platform.runLater(() -> {
            LocalDate premiereDate = getPremierJour();
            for (int i = 0; i < 42; i++) {
                LocalDate dateEnCours = premiereDate.plusDays(i);

                if (i % 7 == 0) {
                    listeSemaine.get(i / 7).setText("" + (1 + ((52 * dateEnCours.getDayOfYear()) / 365)));
                }

                StackPane stackPaneEnCours = stackPanes.get(i);
                stackPaneEnCours.getChildren().forEach(noeud -> {
                    if (noeud instanceof Text) {
                        ((Text) noeud).setText(dateEnCours.format(DateTimeFormatter.ofPattern("dd/MM")));
                    }
                    if (noeud instanceof Rectangle) {
                        ((Rectangle) noeud).setFill(Color.gray(0.8));
                    }
                });

                stackPaneEnCours.setVisible(dateEnCours.isAfter(begin.minusDays(1))
                        && dateEnCours.isBefore(end.plusDays(1)));
            }
        });
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String name) {
        nom = name;
    }
}
