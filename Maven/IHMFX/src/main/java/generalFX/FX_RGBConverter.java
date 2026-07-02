package generalFX;


import javafx.scene.paint.Color;

public class FX_RGBConverter {
    /**
     * Convertit un int en javaFXColor
     *
     * @param couleurAWT int représentant par octets, dans l'ordre RGBO
     * @return La couleur convertie de l'awt
     */
    public static Color convert_rgb_2_color(int couleurAWT) {
        int red = (couleurAWT >> 24) & 0xFF;
        int green = (couleurAWT >> 16) & 0xFF;
        int blue = (couleurAWT >> 8) & 0xFF;
        int opacity = couleurAWT & 0xFF;

        return Color.color(red / 255f,
                green / 255f,
                blue / 255f,
                opacity / 255f);
    }

    public static int convert_color_2_rgb(Color color) {
        int rouge = ((int) (color.getRed() * 255)) << 24;
        int vert = ((int) (color.getGreen() * 255)) << 16;
        int bleu = ((int) (color.getBlue() * 255)) << 8;
        int opacite = ((int) (color.getOpacity() * 255));

        return rouge + vert + bleu + opacite;
    }

}
