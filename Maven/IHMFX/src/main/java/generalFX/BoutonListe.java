package generalFX;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;

import java.util.ArrayList;

public class BoutonListe extends Button {

    private ListPopupController popup;
    private ObservableList selectionnes;

    public BoutonListe() {
        Platform.runLater(() -> {
            popup = new ListPopupController();
        });
        this.setOnAction((event) -> {
            this.popup.show(this);
        });
        this.setText("Liste");
    }

    public void setListe(ArrayList liste) {
        this.popup.setListe(liste);
        this.setText(liste.size() + " elt" + (liste.size() > 1 ? "s" : ""));
    }

    public ObservableList getSelectionnes() {
        return this.selectionnes;
    }

    public void setSelectionnes(ObservableList sList) {
        this.selectionnes = sList;
    }
}
