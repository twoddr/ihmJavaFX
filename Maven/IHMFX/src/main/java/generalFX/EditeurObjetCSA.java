package generalFX;

import general.ObjetCSA;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

public class EditeurObjetCSA extends ScrollPane {
    VBox formContainer;

    public void initialize() {
        formContainer = new VBox(10);
        formContainer.setPadding(new javafx.geometry.Insets(10));
    }

    public void charger_donnees(ObjetCSA objetCSA) {
        Map<String, Object> currentData = objetCSA.getFormattedData();
        buildDynamicForm(formContainer, currentData, objetCSA.getPublicName());

        setContent(formContainer);
        setFitToWidth(true);

    }

    private void showDataSummary(Map<String, Object> data) {
        System.out.println("Current data structure:");
        printMap(data, 0);
    }

    private void printMap(Map<String, Object> map, int indent) {
        StringBuilder indentBuilder = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            indentBuilder.append("  ");
        }
        String indentStr = indentBuilder.toString();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                System.out.println(indentStr + entry.getKey() + ":");
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) entry.getValue();
                printMap(nested, indent + 1);
            } else if (entry.getValue() instanceof List) {
                System.out.println(indentStr + entry.getKey() + ":");
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) entry.getValue();
                for (int i = 0; i < list.size(); i++) {
                    System.out.println(indentStr + "  [" + i + "]: " + list.get(i));
                }
            } else {
                System.out.println(indentStr + entry.getKey() + ": " + entry.getValue());
            }
        }
    }


    private void buildDynamicForm(VBox container, Map<String, Object> data, String title) {
        TitledPane titledPane = new TitledPane();
        titledPane.setText(title);
        titledPane.setExpanded(true);

        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(10));

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedData = (Map<String, Object>) value;
                buildDynamicForm(content, nestedData, key);
            } else if (value instanceof List) {
                Accordion listAccordion = new Accordion();
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;

                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemData = (Map<String, Object>) item;
                        VBox itemContainer = new VBox(5);
                        buildDynamicForm(itemContainer, itemData, "Item " + (i + 1));
                        TitledPane itemPane = new TitledPane("Item " + (i + 1), itemContainer);
                        listAccordion.getPanes().add(itemPane);
                    } else {
                        HBox simpleItem = new HBox(10);
                        simpleItem.getChildren().addAll(
                                new Label("Value:"),
                                createInputField(item, data, key, i)
                        );
                        listAccordion.getPanes().add(new TitledPane("Item " + (i + 1), simpleItem));
                    }
                }

                VBox listContainer = new VBox(5);
                listContainer.getChildren().addAll(
                        new Label(key + " (List):"),
                        listAccordion
                );
                content.getChildren().add(listContainer);
            } else {
                HBox fieldRow = new HBox(10);
                fieldRow.getChildren().addAll(
                        new Label(key + ":"),
                        createInputField(value, data, key, -1)
                );
                content.getChildren().add(fieldRow);
            }
        }

        titledPane.setContent(content);
        container.getChildren().add(titledPane);
    }

    private Control createInputField(Object value, Map<String, Object> parent, String key, int index) {
        Control field;

        if (value instanceof String) {
            TextField textField = new TextField((String) value);
            textField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (index == -1) {
                    parent.put(key, newVal);
                } else {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) parent.get(key);
                    list.set(index, newVal);
                }
            });
            field = textField;
        } else if (value instanceof Integer) {
            TextField numberField = new TextField(value.toString());
            numberField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*")) {
                    numberField.setText(newVal.replaceAll("[^\\d]", ""));
                }
                try {
                    int numValue = Integer.parseInt(newVal);
                    if (index == -1) {
                        parent.put(key, numValue);
                    } else {
                        @SuppressWarnings("unchecked")
                        List<Object> list = (List<Object>) parent.get(key);
                        list.set(index, numValue);
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid numbers temporarily
                }
            });
            field = numberField;
        } else if (value instanceof Boolean) {
            CheckBox checkBox = new CheckBox();
            checkBox.setSelected((Boolean) value);
            checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (index == -1) {
                    parent.put(key, newVal);
                } else {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) parent.get(key);
                    list.set(index, newVal);
                }
            });
            field = checkBox;
        } else {
            TextField textField = new TextField(value != null ? value.toString() : "");
            field = textField;
        }

        field.setMinWidth(200);
        return field;
    }
}
