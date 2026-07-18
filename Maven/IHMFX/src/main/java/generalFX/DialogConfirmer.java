package generalFX;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public class DialogConfirmer {
    /**
     * Lance une fenêtre ok/annuler avec un texte
     *
     * @param message le texte à afficher ; une question normalement
     * @param entete  Texte à afficher en en-tête
     * @return 0 si annulation, 1 si ok
     */
    public static int go(String entete, String message) {
        // Create a confirmation Alert dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Veuillez confirmer, svp");
        alert.setHeaderText(entete);
        alert.setContentText(message);
        StylesheetsFX.apply(alert, null);

        // Show the dialog and wait for the user's response
        Optional<ButtonType> result = alert.showAndWait();

        // Handle the user's response
        if (result.isPresent() && result.get() == ButtonType.OK) {
            return 1;
        }
        return 0;
    }
}
