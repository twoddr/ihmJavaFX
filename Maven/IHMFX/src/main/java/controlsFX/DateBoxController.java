package controlsFX;

import general.CalculateurAge;
import general.DateString;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class DateBoxController extends VBox
        implements Initializable {
    @FXML
    public Label validationLabel;
    @FXML
    public TextField dateTextField;

    public DateBoxController() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/DateBox.fxml"));

        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dateTextField.setPromptText("JJ/MM/AAAA");

        // Contrôle de saisie
        dateTextField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("^\\d{0,2}(/\\d{0,2}(/\\d{0,4})?)?$")) {
                dateTextField.setText(oldVal);
            }

            // Auto-insertion des /
            if (newVal.length() == 2 && oldVal.length() == 1) {
                dateTextField.setText(newVal + "/");
            } else if (newVal.length() == 5 && oldVal.length() == 4) {
                dateTextField.setText(newVal + "/");
            }
        });

        // Validation finale
        dateTextField.focusedProperty().addListener((obs,
                                                     wasFocused,
                                                     isNowFocused) -> {
            if (!isNowFocused) {
                if (isValidDate(dateTextField.getText())) {
                    String texteDate = dateTextField.getText();
                    DateString dateString = new DateString(texteDate);
                    LocalDate date = dateString.getLocalDate();
                    String age = new CalculateurAge(date.atStartOfDay()).getAgeString();
                    validationLabel.setText(age);
                    validationLabel.setStyle("-fx-text-fill: green;");
                } else {
                    validationLabel.setText("Format invalide (JJ/MM/AAAA)");
                    validationLabel.setStyle("-fx-text-fill: red;");
                }
            }
        });
    }

    public void desactiver_label() {
        getChildren().remove(validationLabel);
    }

    public void activer_label() {
        if (!getChildren().contains(validationLabel)) {
            getChildren().add(validationLabel);
        }
    }

    private boolean isValidDate(String date) {
        return date.matches("^\\d{2}/\\d{2}/\\d{4}$");
    }

    public void setDate(LocalDate date, DateTimeFormatter dateTimeFormatter) {
        Platform.runLater(() -> {
            dateTextField.setText(date.format(dateTimeFormatter));
            validationLabel.setText(new CalculateurAge(date.atStartOfDay()).getAgeString());
        });
    }

    public LocalDate getDate() {
        String texteDate = dateTextField.getText();
        DateString dateString = new DateString(texteDate);
        return dateString.getLocalDate();
    }

    public void setDate(LocalDate date) {
        Platform.runLater(() -> {
            if (date == null) {
                dateTextField.setText("");
                validationLabel.setText("Pas de date");
                return;
            }
            dateTextField.setText(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            validationLabel.setText(new CalculateurAge(date.atStartOfDay()).getAgeString());
        });
    }
}
