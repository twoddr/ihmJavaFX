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
 *   <li>stylesheets du nœud propriétaire et de ses ancêtres {@link Parent}</li>
 *   <li>stylesheets de la {@link Scene} et de sa racine</li>
 *   <li>première fenêtre affichée ayant des stylesheets (scène et/ou racine)</li>
 *   <li>fallback {@code /styles/style.css} embarqué dans IHMFX</li>
 * </ol>
 * Note : beaucoup d'apps (ex. CentreMedical) attachent le CSS sur le
 * {@code Parent} racine plutôt que sur la {@code Scene}.
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
        if (owner != null && dialog.getOwner() == null) {
            dialog.initOwner(owner);
        }
    }

    /**
     * Résout la liste des stylesheets à appliquer.
     */
    public static List<String> resolve(Node ownerHint) {
        LinkedHashSet<String> sheets = new LinkedHashSet<>();

        collectFromNodeHierarchy(ownerHint, sheets);

        if (sheets.isEmpty() && ownerHint != null) {
            collectFromScene(ownerHint.getScene(), sheets);
        }

        if (sheets.isEmpty()) {
            for (Window window : Window.getWindows()) {
                if (!window.isShowing()) {
                    continue;
                }
                collectFromScene(window.getScene(), sheets);
                if (!sheets.isEmpty()) {
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

    private static void collectFromNodeHierarchy(Node node, LinkedHashSet<String> sheets) {
        Node current = node;
        while (current != null) {
            if (current instanceof Parent parent) {
                addValid(sheets, parent.getStylesheets());
            }
            current = current.getParent();
        }
        if (node != null) {
            collectFromScene(node.getScene(), sheets);
        }
    }

    private static void collectFromScene(Scene scene, LinkedHashSet<String> sheets) {
        if (scene == null) {
            return;
        }
        addValid(sheets, scene.getStylesheets());
        Parent root = scene.getRoot();
        if (root != null) {
            addValid(sheets, root.getStylesheets());
        }
    }

    private static void addValid(LinkedHashSet<String> sheets, Iterable<String> candidates) {
        if (candidates == null) {
            return;
        }
        for (String sheet : candidates) {
            if (sheet != null && !sheet.isBlank() && !"null".equals(sheet)) {
                sheets.add(sheet);
            }
        }
    }

    private static String fallbackStylesheet() {
        URL url = StylesheetsFX.class.getResource(FALLBACK_STYLESHEET);
        return url != null ? url.toExternalForm() : null;
    }
}
