package generalFX;

import javafx.application.Platform;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class DialogSelector {
    private static String nomObjets;
    private static ArrayList<Object> liste;

    /**
     * Permet d'afficher une liste d'éléments lorsque go() est appelé
     *
     * @param list la liste des éléments qui apparaitront dans le combo
     * @param nom  le nom externe donné aux objets rentrés
     */
    public DialogSelector(List<Object> list, String nom) {
        liste = new ArrayList<>(list);
        nomObjets = nom;
    }

    public static Object go() {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Object> retour = new AtomicReference<>();

        Platform.runLater(() -> {
            // Create a Dialog
            Dialog<Object> dialog = new Dialog<>();
            dialog.setTitle("Choix de " + nomObjets);
            dialog.setHeaderText("Veuillez choisir parmi les items suivants");

            // Create a ComboBox with Person objects
            ComboBox<Object> comboBox = new ComboBox<>();
            comboBox.getItems().addAll(liste);
            comboBox.setPromptText("Sélectionnez un élément " + nomObjets);

            // Set a StringConverter to display the Person objects in the ComboBox
            comboBox.setConverter(new StringConverter<Object>() {
                @Override
                public String toString(Object objet) {
                    return objet == null ? "" : String.valueOf(objet);
                }

                @Override
                public Object fromString(String string) {
                    for (Object elt : liste) {
                        if (String.valueOf(elt).equals(string)) {
                            return elt;
                        }
                    }
                    return null;
                }
            });

            // Add the ComboBox to the dialog
            VBox content = new VBox(comboBox);
            content.setSpacing(10);
            dialog.getDialogPane().setContent(content);
            StylesheetsFX.apply(dialog, null);

            // Add OK and Cancel buttons
            ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

            // Enable/disable the OK button based on whether an item is selected
            comboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                dialog.getDialogPane().lookupButton(okButtonType).setDisable(newValue == null);
            });

            // Set the result converter to return the selected Person object
            dialog.setResultConverter(buttonType -> {
                if (buttonType == okButtonType) {
                    return comboBox.getValue();
                }
                return null;
            });

            // Show the dialog and handle the result
            dialog.showAndWait().ifPresent(selectedPerson -> {
                System.out.println("Objet sélectionné : " + selectedPerson);
                retour.set(selectedPerson);
            });

            // permet d'attendre la fin du runLater et retourner l'objet à l'app principale
            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return retour.get();
    }

}
