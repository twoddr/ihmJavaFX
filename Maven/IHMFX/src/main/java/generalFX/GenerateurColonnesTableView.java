package generalFX;

import controlsFX.MaitreFX;
import general.FormatNombre;
import general.ObjetCSA;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import util.SimpleLogger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class GenerateurColonnesTableView {
    SimpleLogger logger = SimpleLogger.getLogger(GenerateurColonnesTableView.class);
    private TableView<ObjetIHM> tableau;
    private Consumer<String> consoleLogger;
    private BiConsumer<ObjetCSA, String> checkboxHandler;
    private ArrayList<String> listeEltsANePasAfficher = new ArrayList<>();

    /**
     * Constructeur sans MaitreFX — approche recommandée.
     *
     * @param tableView       le TableView à gérer
     * @param consoleLogger   callback pour afficher des messages (peut être null)
     * @param checkboxHandler callback quand une checkbox est cliquée (item
     *                        sélectionné, nom colonne)
     */
    public GenerateurColonnesTableView(TableView<ObjetIHM> tableView,
            Consumer<String> consoleLogger,
            BiConsumer<ObjetCSA, String> checkboxHandler) {
        tableau = tableView;
        this.consoleLogger = consoleLogger;
        this.checkboxHandler = checkboxHandler;
        // Add listener to adjust column widths when TableView's width changes
        tableau.widthProperty().addListener(
                (obs, oldWidth, newWidth) -> adjustColumnWidths(tableau));

        // Add listener to adjust column widths when columns are added or removed
        tableau.getColumns()
                .addListener((ListChangeListener<TableColumn<ObjetIHM, ?>>) change -> adjustColumnWidths(tableau));
    }

    /**
     * Constructeur simplifié sans callbacks (messages vers System.err).
     */
    public GenerateurColonnesTableView(TableView<ObjetIHM> tableView) {
        this(tableView, s -> System.err.println("GenerateurColonnes: " + s), null);
    }

    private void adjustColumnWidths(TableView<ObjetIHM> tableView) {
        double tableWidth = tableView.getWidth();
        int columnCount = tableView.getColumns().size();
        if (columnCount > 0) {
            double colWidth = tableWidth / columnCount;
            for (TableColumn<ObjetIHM, ?> column : tableView.getColumns()) {
                column.setPrefWidth(colWidth);
            }
        }
    }

    private void logMessage(String message) {
        if (consoleLogger != null) {
            consoleLogger.accept(message);
        } else {
            System.err.println("GenerateurColonnes: " + message);
        }
    }

    public ArrayList<String> getListeEltsANePasAfficher() {
        return listeEltsANePasAfficher;
    }

    public void setListeEltsANePasAfficher(ArrayList<String> listeEltsANePasAfficher) {
        this.listeEltsANePasAfficher = listeEltsANePasAfficher;
    }

    /**
     * Permet de charger le tableau de données vierges
     *
     * @param objetIHM chaque colonne doit avoir un titre et un type qui doivent
     *                 se trouver dans un ElementBD
     */
    public void setColumn(ObjetIHM objetIHM) {
        if (objetIHM == null) {
            logMessage("* GenerateurColonnesTableView/setColumn : Aucune donnée à charger, je reçois NULL !");
            return;
        }
        Platform.runLater(() -> {
            tableau.getColumns().clear();
            ArrayList<String> variablesVisibles = objetIHM.getVariablesVisibles();
            variablesVisibles.removeAll(listeEltsANePasAfficher);
            for (String nomVariable : variablesVisibles) {
                String nomPublic = objetIHM.getNomPublic(nomVariable);
                Object variable = objetIHM.get(nomVariable) == null ? null : objetIHM.get(nomVariable).getValue();
                Class classeVariable = objetIHM.getTypeOfVariable(nomVariable);
                if (classeVariable != null) {
                    if (variable instanceof String && classeVariable.equals(String.class)) {
                        tableau.getColumns().add(getStringTableColumn(nomVariable, nomPublic));
                    } else if (variable instanceof ObjetCSA) {
                        tableau.getColumns().add(getObjetTableColumn(nomVariable, nomPublic));
                    } else if (variable instanceof Integer && classeVariable.equals(Integer.class)) {
                        tableau.getColumns().add(getIntegerTableColumn(nomVariable, nomPublic));
                    } else if (variable instanceof java.awt.Color || classeVariable.equals(java.awt.Color.class)) {
                        tableau.getColumns().add(getAwtColorTableColumn(nomVariable, nomPublic));
                    } else if (variable instanceof Float || classeVariable.equals(Float.class)) {
                        tableau.getColumns().add(getFloatTableColumn(nomVariable, nomPublic));
                    } else if (variable instanceof LocalDateTime || classeVariable.equals(LocalDateTime.class)) {
                        tableau.getColumns().add(getTemporalTableColumn(nomVariable, nomPublic));
                    } else if (variable instanceof LocalDate || classeVariable.equals(LocalDate.class)) {
                        tableau.getColumns().add(getTemporalTableColumn(nomVariable, nomPublic));
                    } else if (variable instanceof Boolean || classeVariable.equals(Boolean.class)) {
                        tableau.getColumns().add(getBooleanTableColumn(nomVariable, nomPublic));
                    } else if (variable instanceof Color || classeVariable.equals(Color.class)) {
                        tableau.getColumns().add(getColorTableColumn(nomVariable, nomPublic));
                    } else if (variable instanceof List) {
                        tableau.getColumns().add(getListTableColumn(nomVariable, nomPublic));
                    } else {
                        System.err.println(
                                "** : type non géré par le tableau pour la variable : " + variable + " et pour la" +
                                        " classe stockée " + classeVariable);
                    }
                }
            }
        });
    }

    private void affiche_debug(String s) {
        logger.debug("\n*** DEBUG GenerateurColonnes :\n" + s + "\n*** Fin **********\n");
    }

    private TableColumn<ObjetIHM, Temporal> getTemporalTableColumn(String nomVariable, String titre) {
        TableColumn<ObjetIHM, Temporal> colonne = new TableColumn<>(titre);

        colonne.setCellFactory(column -> new TableCell<ObjetIHM, Temporal>() {
            @Override
            protected void updateItem(Temporal item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);

                if (empty || item == null) {
                    setText(""); // Ou " - " pour un placeholder
                } else {
                    // Format selon le type
                    if (item instanceof LocalDate) {
                        setText(((LocalDate) item).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    } else if (item instanceof LocalDateTime) {
                        setText(((LocalDateTime) item).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    }

                    // Appliquer la couleur si nécessaire
                    ObjetIHM objetIHM = getTableView().getItems().get(getIndex());
                    if (objetIHM != null && objetIHM.getObjetCSA().isTextColorEnable()) {
                        setTextFill(objetIHM.getColor());
                    }
                }
            }
        });

        colonne.setCellValueFactory(oihm -> {
            Property property = oihm.getValue().get(nomVariable);
            if (property != null && property.getValue() instanceof Temporal) {
                return property;
            }
            System.err.println("Bad type for " + nomVariable + ". Expected LocalDate or LocalDateTime.");
            return null;
        });

        return colonne;
    }

    private TableColumn<ObjetIHM, Float> getFloatTableColumn(String nomVariable, String titre) {
        TableColumn<ObjetIHM, Float> colonne = new TableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<Float>) oihm.getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Float)");
                return null;
            }
        });
        // Affichage "à la française" : milliers groupés et virgule décimale.
        colonne.setCellFactory(column -> new TableCell<ObjetIHM, Float>() {
            @Override
            protected void updateItem(Float item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : FormatNombre.nombre(item));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private TableColumn<ObjetIHM, ObjetCSA> getObjetTableColumn(String nomVariable, String titre) {
        TableColumn<ObjetIHM, ObjetCSA> colonne = new TableColumn<>(titre);
        colonne.setCellValueFactory(cellDataFeatures -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                ObjetIHM objetIHM = cellDataFeatures.getValue();
                // System.err.println("DEBUG TableColumn : je tente d'insérer " +
                // objetIHM.getObjetCSA() + " dans la variable " + nomVariable
                // + " à la colonne " + titre);
                // System.err.println("DEBUG TableColumn : getProperty : " +
                // objetIHM.getProperty(nomVariable));
                return (ObservableValue<ObjetCSA>) objetIHM.get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "String)");
                return null;
            }
        });

        colonne.setCellFactory((tableColumn) -> {

            return new TableCell<ObjetIHM, ObjetCSA>() {
                @Override
                protected void updateItem(ObjetCSA item, boolean empty) {
                    try {
                        // System.err.println("DEBUG TableColumn : Item actuel de setCellFactory : " +
                        // item);
                        super.updateItem(item, empty);

                        this.setText(null);
                        this.setGraphic(null);

                        if (item != null && !empty) {
                            this.setText(item.getNom().toUpperCase());
                        }
                    } catch (Exception e) {
                        System.err.println("TableColumn/setCellFactory : Explosion lors de l'insertion de " + item);
                    }
                }
            };
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private TableColumn<ObjetIHM, String> getStringTableColumn(String nomVariable, String titre) {
        TableColumn<ObjetIHM, String> colonne = new TableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                ObjetIHM objetIHM = oihm.getValue();
                Object property = objetIHM.get(nomVariable);
                /*
                 * System.err.println("DEBUG TableColumn : je tente d'insérer " +
                 * objetIHM.getObjetCSA() + " dans la variable " + nomVariable
                 * + " à la colonne " + titre);
                 */
                if (property != null) {
                    return (ObservableValue<String>) property;
                }
                return null;
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "String)");
                return null;
            }
        });

        colonne.setCellFactory((tableColumn) -> {
            return new TableCell<ObjetIHM, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    this.setText(null);
                    this.setGraphic(null);

                    if (item != null && !empty) {
                        setText(item.toUpperCase());

                        // Get the object for the current row
                        ObjetIHM objetIHM = getTableView().getItems().get(getIndex());
                        if (objetIHM.getObjetCSA().isTextColorEnable()) {
                            setTextFill(objetIHM.getColor());
                        }
                    }
                }
            };
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private TableColumn<ObjetIHM, Integer> getIntegerTableColumn(String nomVariable, String titre) {
        TableColumn<ObjetIHM, Integer> colonne = new TableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<Integer>) oihm.getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Integer)");
                return null;
            }
        });
        if (nomVariable.toLowerCase().contains("ouleur")) {
            colonne.setCellFactory((tableColumn) -> {
                return new TableCell<ObjetIHM, Integer>() {
                    @Override
                    protected void updateItem(Integer item, boolean empty) {
                        super.updateItem(item, empty);

                        if (!empty) {
                            Rectangle rectangle = new Rectangle(50, 30);
                            rectangle.setArcHeight(5);
                            rectangle.setArcWidth(5);
                            if (item == null) {
                                this.setText("ERROR !");
                                Color couleur = Color.RED;
                                rectangle.setFill(couleur);
                                System.err.println("*** ListeTable : je n'arrive pas à afficher "
                                        + "une couleur écrite sous forme d'integer !");
                            } else {
                                Color couleur = FX_RGBConverter.convert_rgb_2_color(item);
                                rectangle.setFill(couleur);
                                // System.out.println("\nDEBUG : listTable affiche une couleur " +
                                // affiche_couleur(couleur)
                                // + "de valeur entière : " + item);
                            }
                            this.setGraphic(rectangle);
                        }
                    }
                };
            });
        }
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private TableColumn<ObjetIHM, java.awt.Color> getAwtColorTableColumn(String nomVariable, String titre) {
        TableColumn<ObjetIHM, java.awt.Color> colonne = new TableColumn<>(titre);
        colonne.setCellFactory((tableColumn) -> {
            TableCell<ObjetIHM, java.awt.Color> tableCell = new TableCell<ObjetIHM, java.awt.Color>() {
                @Override
                protected void updateItem(java.awt.Color item, boolean empty) {
                    super.updateItem(item, empty);

                    this.setText(null);
                    this.setGraphic(null);

                    if (!empty && item != null) {
                        Rectangle rectangle = new Rectangle(50, 30);
                        rectangle.setArcHeight(5);
                        rectangle.setArcWidth(5);
                        rectangle.setFill(FX_RGBConverter.convert_rgb_2_color(item.getRGB()));
                        this.setGraphic(rectangle);
                    }
                }
            };
            return tableCell;
        });
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<java.awt.Color>) oihm.getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Color)");
                return null;
            }
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private TableColumn<ObjetIHM, Boolean> getBooleanTableColumn(String nomVariable, String titre) {
        TableColumn<ObjetIHM, Boolean> colonne = new TableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<Boolean>) oihm.getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Boolean)");
                return null;
            }
        });
        colonne.setCellFactory(p -> {
            CheckBoxTableCell<ObjetIHM, Boolean> cell = new CheckBoxTableCell<>();
            cell.setAlignment(Pos.CENTER);
            cell.setDisable(false);
            cell.setOnMousePressed(event -> {
                action_clic_checkBox(cell);
            });
            return cell;
        });
        return colonne;
    }

    private TableColumn<ObjetIHM, ObservableList> getListTableColumn(String nomVariable, String titre) {
        TableColumn<ObjetIHM, ObservableList> colonne = new TableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<ObservableList>) oihm.getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Float)");
                return null;
            }
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private TableColumn<ObjetIHM, Color> getColorTableColumn(String nomVariable, String titre) {
        TableColumn<ObjetIHM, Color> colonne = new TableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<Color>) oihm.getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Float)");
                return null;
            }
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private void action_clic_checkBox(Object source) {
        if (source instanceof CheckBoxTableCell) {
            ObservableList<TablePosition> cellules = tableau.getSelectionModel().getSelectedCells();
            String nomColonne = null;
            if (!cellules.isEmpty()) {
                TablePosition celluleSelectionnee = cellules.get(0);
                nomColonne = celluleSelectionnee.getTableColumn().getText();
            }
            ObjetCSA selectedItem = tableau.getSelectionModel().getSelectedItem().getObjetCSA();
            if (checkboxHandler != null) {
                checkboxHandler.accept(selectedItem, nomColonne);
                return;
            }
            System.err.println(
                    "ListTableBD/GenerateurColonnesTableView : action_clic_checkBox : checkboxHandler == null !");
        }
    }

    private TableColumn<ObjetIHM, Object> getObjetCSAIHMGeneralTableColumn(String nomVariable, String titre) {
        TableColumn<ObjetIHM, Object> colonne = new TableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<Object>) oihm.getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Object)");
                return null;
            }
        });

        colonne.setCellFactory((tableColumn) -> {
            return new TableCell<ObjetIHM, Object>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);

                    this.setText(null);
                    this.setGraphic(null);

                    if (item != null && !empty) {
                        this.setText(item instanceof Number
                                ? FormatNombre.nombre((Number) item)
                                : String.valueOf(item).toUpperCase());
                        // setTextFill(FX_RGBConverter.convert_rgb_2_color(item.));
                    }
                }
            };
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }
}
