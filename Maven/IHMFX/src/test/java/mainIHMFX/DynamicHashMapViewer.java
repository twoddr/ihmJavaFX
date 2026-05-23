package mainIHMFX;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 ******** Suggestion de Perplexity ************
 */
public class DynamicHashMapViewer extends Application {

    // Entry point
    public static void main(String[] args) {
        launch(args);
    }

    // Create UI recursively
    private Node createNodeForValue(Object value, String key) {
        if (value instanceof Map) {
            // Recursively build pane for HashMap
            VBox box = new VBox(5);
            ((Map<?, ?>) value).forEach((k, v) -> {
                box.getChildren().add(buildRow(k.toString(), v));
            });
            TitledPane pane = new TitledPane(key, box);
            pane.setExpanded(false);
            return pane;
        } else if (value instanceof List) {
            VBox box = new VBox(5);
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                box.getChildren().add(buildRow(key + "[" + i + "]", list.get(i)));
            }
            TitledPane pane = new TitledPane(key + " (List)", box);
            pane.setExpanded(false);
            return pane;
        } else {
            // Primitive/leaf
            TextField tf = new TextField(value == null ? "" : value.toString());
            tf.setPrefWidth(200);
            // Optional: add event handler to update your data structure
            HBox row = new HBox(new Label(key + ": "), tf);
            row.setSpacing(10);
            return row;
        }
    }

    private Node buildRow(String key, Object value) {
        return createNodeForValue(value, key);
    }

    @Override
    public void start(Stage primaryStage) {
        // Sample nested data
        HashMap<String, Object> address = new HashMap<>();
        address.put("street", "123 Main St");
        address.put("city", "Smalltown");

        HashMap<String, Object> person = new HashMap<>();
        person.put("name", "Alice");
        person.put("age", 30);
        person.put("address", address);

        ArrayList<Object> siblings = new ArrayList<>();

// First sibling
        HashMap<String, Object> bob = new HashMap<>();
        bob.put("name", "Bob");
        bob.put("age", 28);
        siblings.add(bob);

// Second sibling
        HashMap<String, Object> carol = new HashMap<>();
        carol.put("name", "Carol");
        carol.put("age", 25);
        siblings.add(carol);

// Add to your person map/object
        person.put("siblings", siblings);

        // Main UI build
        ScrollPane root = new ScrollPane();
        VBox content = new VBox(10);
        content.getChildren().add(buildRow("Person", person));
        root.setContent(content);

        primaryStage.setScene(new Scene(root, 500, 400));
        primaryStage.setTitle("Dynamic HashMap Viewer");
        primaryStage.show();
    }
}
