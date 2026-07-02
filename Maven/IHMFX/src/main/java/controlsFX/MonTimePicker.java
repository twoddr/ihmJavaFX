package controlsFX;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.net.URL;
import java.time.LocalTime;
import java.util.ResourceBundle;

public class MonTimePicker extends HBox implements Initializable {
    @FXML
    private Spinner<Integer> spnHeure;

    @FXML
    private Spinner<Integer> spnMinutes;

    public MonTimePicker() {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/fxml/MonTimePicker.fxml"));

        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

    }

    @Override
    public void initialize(URL event, ResourceBundle rb) {
        // Value factory.
        SpinnerValueFactory<Integer> valueFactory_heure =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 12);
        spnHeure.setValueFactory(valueFactory_heure);

        // Value factory minutes.
        SpinnerValueFactory<Integer> valueFactory_minutes =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 30);

        spnMinutes.setValueFactory(valueFactory_minutes);

    }

    public Spinner<Integer> getSpnHeure() {
        return spnHeure;
    }

    public Spinner<Integer> getSpnMinutes() {
        return spnMinutes;
    }

    public LocalTime getValue() {
        int heure = spnHeure.getValue();
        int minutes = extraire_minutes();
        return LocalTime.of(heure, minutes, 0);
    }

    public void setValue(LocalTime temps) {
        if (temps == null) {
            spnHeure.getValueFactory().setValue(12);
            spnHeure.getEditor().setText("");
            spnMinutes.getValueFactory().setValue(30);
            spnMinutes.getEditor().setText("");
            return;
        }
        spnHeure.getValueFactory().setValue(temps.getHour());
        spnMinutes.getValueFactory().setValue(temps.getMinute());
    }

    private int extraire_minutes() {
        Integer minutes = spnMinutes.getValue();
        try {
            minutes = Integer.parseInt(spnMinutes.getEditor().getText());
            if (minutes > -1 && minutes < 60) {
                return minutes;
            }
            System.err.println("Mauvais format de minutes rentré dans le spinner : " + minutes
                    + "\nRemplacé par 0 !");
        } catch (Exception x) {
            if (minutes != null) {
                return minutes;
            }
        }
        System.err.println("Echec à la lecture du spinner minutes !");
        return 0;
    }
}
