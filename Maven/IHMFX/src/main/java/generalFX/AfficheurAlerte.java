package generalFX;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * Permet d'afficher des messages d'alerte, d'erreur, de confirmation et d'avertissement
 */
public class AfficheurAlerte {

    /**
     * Permet d'afficher un message d'alerte
     *
     * @param title   le titre de la fenêtre
     * @param message le message à afficher à l'utilisateur
     */
    public static void afficher(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null); // No header
            alert.setContentText(message);
            StylesheetsFX.apply(alert, null);
            alert.showAndWait();
        });
    }

    /**
     * Affiche une boîte de dialogue d'erreur
     *
     * @param titre   le titre de la fenêtre
     * @param message le message d'erreur à afficher
     */
    public static void afficherErreur(String titre, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(titre);
            alert.setHeaderText(null);
            alert.setContentText(message);
            StylesheetsFX.apply(alert, null);
            alert.showAndWait();
        });
    }

    /**
     * Affiche une boîte de dialogue de confirmation
     *
     * @param titre   le titre de la fenêtre
     * @param message le message de confirmation
     * @return true si l'utilisateur a confirmé, false sinon
     */
    public static boolean afficherConfirmation(String titre, String message) {
        boolean[] result = {false};
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(titre);
            alert.setHeaderText(null);
            alert.setContentText(message);
            StylesheetsFX.apply(alert, null);

            Optional<ButtonType> resultat = alert.showAndWait();
            result[0] = resultat.isPresent() && resultat.get() == ButtonType.OK;
        });
        return result[0];
    }

    /**
     * Affiche une boîte de dialogue d'avertissement
     *
     * @param titre   le titre de la fenêtre
     * @param message le message d'avertissement
     */
    public static void afficherAvertissement(String titre, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(titre);
            alert.setHeaderText(null);
            alert.setContentText(message);
            StylesheetsFX.apply(alert, null);
            alert.showAndWait();
        });
    }
}
