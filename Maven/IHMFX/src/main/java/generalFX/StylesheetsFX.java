package generalFX;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;

/**
 * Propage les stylesheets de l'application hôte vers les fenêtres / dialogues IHMFX.
 * <p>
 * Ordre de résolution :
 * <ol>
 *   <li>scène du nœud propriétaire fourni</li>
 *   <li>première fenêtre affichée ayant déjà des stylesheets</li>
 *   <li>fallback {@code /styles/style.css} embarqué dans IHMFX</li>
 * </ol>
 */
public final class StylesheetsFX {

    private static final String FALLBACK_STYLESHEET = "/styles/style.css";

    private StylesheetsFX() {
    }

    /**
     * Applique les stylesheets résolues à une {@link Scene}.
     *
     * @param scene     scène cible (non null)
     * @param ownerHint nœud de l'application hôte, ou {@code null}
     */
    public static void apply(Scene scene, Node ownerHint) {
        Objects.requireNonNull(scene, "scene");
        scene.getStylesheets().setAll(resolve(ownerHint));
    }

    /**
     * Applique les stylesheets résolues à un {@link Parent} (ex. DialogPane).
     */
    public static void apply(Parent root, Node ownerHint) {
        Objects.requireNonNull(root, "root");
        root.getStylesheets().setAll(resolve(ownerHint));
    }

    /**
     * Applique les stylesheets résolues à un {@link Dialog}.
     */
    public static void apply(Dialog<?> dialog, Node ownerHint) {
        Objects.requireNonNull(dialog, "dialog");
        DialogPane pane = dialog.getDialogPane();
        apply(pane, ownerHint);
        Window owner = findOwnerWindow(ownerHint);
        if (owner != null) {
            dialog.initOwner(owner);
        }
    }

    /**
     * Résout la liste des stylesheets à appliquer.
     */
    public static List<String> resolve(Node ownerHint) {
        LinkedHashSet<String> sheets = new LinkedHashSet<>();

        Scene fromHint = ownerHint != null ? ownerHint.getScene() : null;
        if (fromHint != null && !fromHint.getStylesheets().isEmpty()) {
            sheets.addAll(fromHint.getStylesheets());
        }

        if (sheets.isEmpty()) {
            for (Window window : Window.getWindows()) {
                if (!window.isShowing()) {
                    continue;
                }
                Scene scene = window.getScene();
                if (scene != null && !scene.getStylesheets().isEmpty()) {
                    sheets.addAll(scene.getStylesheets());
                    break;
                }
            }
        }

        if (sheets.isEmpty()) {
            String fallback = fallbackStylesheet();
            if (fallback != null) {
                sheets.add(fallback);
            }
        }

        return new ArrayList<>(sheets);
    }

    /**
     * Fenêtre propriétaire pour {@code initOwner}, ou {@code null}.
     */
    public static Window findOwnerWindow(Node ownerHint) {
        if (ownerHint != null) {
            Scene scene = ownerHint.getScene();
            if (scene != null && scene.getWindow() != null) {
                return scene.getWindow();
            }
        }
        for (Window window : Window.getWindows()) {
            if (window.isShowing() && window.getScene() != null) {
                return window;
            }
        }
        return null;
    }

    private static String fallbackStylesheet() {
        URL url = StylesheetsFX.class.getResource(FALLBACK_STYLESHEET);
        return url != null ? url.toExternalForm() : null;
    }
}
