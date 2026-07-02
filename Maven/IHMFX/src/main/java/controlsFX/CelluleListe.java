package controlsFX;

import general.ObjetCSA;
import general.calendrier.Evenement;
import generalFX.FX_RGBConverter;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class CelluleListe extends ListCell<Object> {

    @Override
    public void updateItem(Object objet, boolean empty) {
        // calling super here is very important - don't skip this!
        super.updateItem(objet, empty);

        Color couleur;
        Text texte = objet == null ? new Text("") : new Text(objet.toString());

        Rectangle rect = new Rectangle(300, 25);
        if (objet instanceof Evenement) {
            Evenement evenementIHM = (Evenement) objet;
            couleur = FX_RGBConverter.convert_rgb_2_color(evenementIHM.getCouleurTexte());
        } else if (objet instanceof ObjetCSA) {
            ObjetCSA objetDi = (ObjetCSA) objet;
            java.awt.Color awtColor = general.RGBConverter.convert_int_2_color(objetDi.getCouleur());
            int r = awtColor.getRed();
            int g = awtColor.getGreen();
            int b = awtColor.getBlue();
            int a = awtColor.getAlpha();
            double opacity = a / 255.0;
            try {
                couleur = Color.rgb(r, g, b, 1);
                /*System.out.println("CelluleListe : conversion de " + awtColor + " = " + objetIHM.getCouleur()
                        + " en " + couleur + " = (" + couleur.getRed() + ", " + couleur.getGreen() + ", " + couleur.getBlue()
                        + ") avec opacité de : " + opacity);*/
            } catch (Exception e) {
                System.err.println("CelluleListe/updateItem : Crash au moment de la conversion de couleur " + awtColor);
                couleur = Color.DARKBLUE;
            }
        } else {
            couleur = Color.DARKBLUE;
        }
        texte.setFill(couleur);
        rect.setFill(Color.WHITESMOKE);

        StackPane stack = new StackPane();
        stack.setMinWidth(0);
        stack.setPrefWidth(1);
        StackPane.setAlignment(texte, Pos.CENTER_LEFT);
        rect.widthProperty().bind(stack.widthProperty().subtract(0));

        stack.getChildren().add(0, rect);
        stack.getChildren().add(1, texte);

        setBorder(new Border(new BorderStroke(Color.LIGHTGRAY,
                BorderStrokeStyle.SOLID,
                new CornerRadii(4),
                new BorderWidths(2))));

        setGraphic(stack);
    }
}
