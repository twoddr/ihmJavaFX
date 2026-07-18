package controlsFX;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import general.GestionnaireRetour;
import general.Monostable;
import general.ObjetCSA;
import generalFX.CreateurArborescence;
import generalFX.FX_RGBConverter;
import generalFX.ObjetIHM;
import generalFX.TransformeurObjetsIHM;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class TableViewCRUD extends TreeTableView<ObjetIHM> implements GestionnaireRetour {

    private ObjetCSA elementDeReference = null;
    private Monostable activeurIHM;
    private ArrayList<String> listeEltsANePasAfficher = new ArrayList<>();
    private ArrayList<String> nomChampsEditables = new ArrayList<>();
    private ArrayList<ObjetIHM> listeOriginale = new ArrayList<>();

    // ---- Handlers fonctionnels (alternative à MaitreFX) ----
    private ContexteFX contexte;
    private ContexteFX.HandlerBD bdHandler;
    private Function<String, ObjetCSA> typeResolver;
    private BiConsumer<Object, String> userActionHandler;
    private Consumer<String> messageHandler;

    public TableViewCRUD() {
        TreeItem<ObjetIHM> racine = new TreeItem<>();
        setRoot(racine);
        setShowRoot(false);
        racine.setExpanded(true);

        getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        setPlaceholder(new Label("Aucun élément dans la liste"));
        // tableau.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Add listener to adjust column widths when TableView's width changes
        widthProperty().addListener(
                (obs, oldWidth, newWidth) -> adjustColumnWidths(this));

        // Add listener to adjust column widths when columns are added or removed
        getColumns().addListener((ListChangeListener<TreeTableColumn<ObjetIHM, ?>>) change -> adjustColumnWidths(this));
        setOnMouseClicked(mouseEvent -> {
            action_clic();
        });
    }

    private void action_clic() {
        ObjetCSA selection = getSelection();
        if (selection != null) {
            affiche_console("ListeTreeTableBD/clicListe : sélection de " + selection + " qui est du type " +
                    selection.getType() + " et les champs éditables sont : " + nomChampsEditables);
            return;
        }
        affiche_console("Sélection nulle dans le listTableTreeBD");
    }

    public ArrayList<String> getNomChampsEditables() {
        return nomChampsEditables;
    }

    public void setNomChampsEditables(ArrayList<String> nomChampsEditables) {
        this.nomChampsEditables = nomChampsEditables;
    }

    private TreeTableColumn<ObjetIHM, ObservableList> getObjetCSAIHMObservableListTreeTableColumn(String nomVariable,
            String titre) {
        TreeTableColumn<ObjetIHM, ObservableList> colonne = new TreeTableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<ObservableList>) oihm.getValue().getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Float)");
                return null;
            }
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    /*
     * public void autoResizeColumns() {
     * //Set the right policy
     * tableau.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
     * tableau.getColumns().stream().forEach((column) -> {
     * //Minimal width = columnheader
     * Text t = new Text(column.getText());
     * double max = t.getLayoutBounds().getWidth();
     * for (int i = 0; i < tableau.getItems().size(); i++) {
     * //cell must not be empty
     * if (column.getCellData(i) != null) {
     * t = new Text(column.getCellData(i).toString());
     * double calcwidth = t.getLayoutBounds().getWidth();
     * //remember new max-width
     * if (calcwidth > max) {
     * max = calcwidth;
     * }
     * }
     * }
     * //set the new max-widht with some extra space
     * column.setPrefWidth(max + 10.0d);
     * });
     * }
     */

    private TreeTableColumn<ObjetIHM, Color> getObjetCSAIHMColorTreeTableColumn(String nomVariable, String titre) {
        TreeTableColumn<ObjetIHM, Color> colonne = new TreeTableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<Color>) oihm.getValue().getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Float)");
                return null;
            }
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private TreeTableColumn<ObjetIHM, LocalDate> getObjetCSAIHMLocalDateTreeTableColumn(String nomVariable,
            String titre) {
        TreeTableColumn<ObjetIHM, LocalDate> colonne = new TreeTableColumn<>(titre);
        colonne.setCellFactory(column -> {
            return new TreeTableCell<ObjetIHM, LocalDate>() {
                @Override
                protected void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    }
                }
            };
        });
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<LocalDate>) oihm.getValue().getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "LocalDate)");
                return null;
            }
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private TreeTableColumn<ObjetIHM, LocalDateTime> getObjetCSAIHMLocalDateTimeTreeTableColumn(String nomVariable,
            String titre) {
        TreeTableColumn<ObjetIHM, LocalDateTime> colonne = new TreeTableColumn<>(titre);
        colonne.setCellFactory(column -> {
            return new TreeTableCell<ObjetIHM, LocalDateTime>() {
                @Override
                protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    }
                }
            };
        });
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<LocalDateTime>) oihm.getValue().getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "LocalDateTime)");
                return null;
            }
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private TreeTableColumn<ObjetIHM, Float> getObjetCSAIHMFloatTreeTableColumn(String nomVariable, String titre) {
        TreeTableColumn<ObjetIHM, Float> colonne = new TreeTableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<Float>) oihm.getValue().getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Float)");
                return null;
            }
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private TreeTableColumn<ObjetIHM, ObjetCSA> getObjetCSAIHMObjetCSATreeTableColumn(String nomVariable,
            String titre) {
        TreeTableColumn<ObjetIHM, ObjetCSA> colonne = new TreeTableColumn<>(titre);
        colonne.setCellValueFactory(cellDataFeatures -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<ObjetCSA>) cellDataFeatures.getValue().getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "String)");
                return null;
            }
        });

        colonne.setCellFactory((TreeTableColumn) -> {
            return new TreeTableCell<ObjetIHM, ObjetCSA>() {
                @Override
                protected void updateItem(ObjetCSA item, boolean empty) {
                    super.updateItem(item, empty);

                    this.setText(null);
                    this.setGraphic(null);

                    if (item != null && !empty) {
                        this.setText(item.getNom());
                    }
                }
            };
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private TreeTableColumn<ObjetIHM, String> getObjetCSAIHMStringTreeTableColumn(String nomVariable, String titre) {
        TreeTableColumn<ObjetIHM, String> colonne = new TreeTableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            try {
                ObjetIHM objetIHM = oihm.getValue().getValue();
                if (objetIHM == null)
                    return null;

                // 1. On tente de récupérer la propriété normale (ex: EXAMENPRESCRITE_nom)
                ObservableValue<String> property = (ObservableValue<String>) objetIHM.get(nomVariable);

                // 2. Si la propriété n'existe pas ou est vide (cas du niveau 1 après votre
                // correction)
                // et qu'on est sur la colonne principale, on utilise le "Nom" de l'objet
                // métier.
                if (property == null || (property.getValue() != null && property.getValue().isEmpty())) {
                    if (nomVariable.equals(elementDeReference.getNomNOM())) {
                        return new javafx.beans.property.SimpleStringProperty(objetIHM.getObjetCSA().getNom());
                    }
                }

                return property;
            } catch (Exception e) {
                System.err.println("** Erreur de récupération pour : " + nomVariable);
                return null;
            }
        });

        colonne.setCellFactory((TreeTableColumn) -> {
            return new TreeTableCell<ObjetIHM, String>() {
                private TextField textField;

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        // Affiche la valeur fournie par le CellValueFactory ci-dessus
                        setText(item);

                        // Gestion de la couleur
                        TreeItem<ObjetIHM> currentTreeItem = getTreeTableRow().getTreeItem();
                        if (currentTreeItem != null && currentTreeItem.getValue() != null) {
                            setTextFill(currentTreeItem.getValue().getColor());
                        }
                    }
                }

                // Switch to editing mode (edit mode)
                @Override
                public void startEdit() {
                    TreeItem<ObjetIHM> currentTreeItem = getTreeTableRow().getTreeItem();
                    if (currentTreeItem != null && isFieldEditable()) {
                        super.startEdit();

                        if (textField == null) {
                            createTextField();
                        }

                        setText(null);
                        setGraphic(textField);
                        textField.setText(getItem());
                        textField.selectAll();

                        // Request focus for user interaction
                        textField.requestFocus();
                    }
                }

                // Commit the editing
                @Override
                public void commitEdit(String newValue) {
                    super.commitEdit(newValue);

                    // Update the value in the associated TreeItem/ObjetIHM
                    TreeItem<ObjetIHM> currentTreeItem = getTreeTableRow().getTreeItem();
                    if (currentTreeItem != null) {
                        ObjetIHM objetIHM = currentTreeItem.getValue();
                        if (objetIHM != null) {
                            objetIHM.getObjetCSA().inserer_valeur(nomVariable, newValue);
                        }
                    }
                    setText(newValue);
                    setGraphic(null);
                }

                // Cancel the editing
                @Override
                public void cancelEdit() {
                    super.cancelEdit();
                    setText(getItem());
                    setGraphic(null);
                }

                // Create a TextField for editing
                private void createTextField() {
                    textField = new TextField(getItem());
                    textField.setOnAction(e -> commitEdit(textField.getText()));
                    textField.focusedProperty().addListener((obs, oldValue, newValue) -> {
                        if (!newValue) {
                            commitEdit(textField.getText());
                        }
                    });
                }

                public boolean isFieldEditable() {
                    // Get the current TreeItem associated with this cell
                    TreeItem<ObjetIHM> currentTreeItem = getTreeTableRow().getTreeItem();
                    if (currentTreeItem != null) {
                        ObjetIHM objetIHM = currentTreeItem.getValue();
                        if (objetIHM != null) {
                            return currentTreeItem.getValue().getType().equals(elementDeReference.getType()) &&
                                    nomChampsEditables.contains(nomVariable);
                        }
                    }
                    return false; // Default to non-editable if conditions aren't met
                }
            };
        });

        colonne.setEditable(true);
        return colonne;
    }

    private TreeTableColumn<ObjetIHM, Object> getObjetCSAIHMGeneralTreeTableColumn(String nomVariable, String titre) {
        TreeTableColumn<ObjetIHM, Object> colonne = new TreeTableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<Object>) oihm.getValue().getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Object)");
                return null;
            }
        });

        colonne.setCellFactory((TreeTableColumn) -> {
            return new TreeTableCell<ObjetIHM, Object>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);

                    this.setText(null);
                    this.setGraphic(null);

                    if (item != null && !empty) {
                        this.setText(String.valueOf(item));
                        // setTextFill(FX_RGBConverter.convert_rgb_2_color(item.));
                    }
                }
            };
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    private TreeTableColumn<ObjetIHM, Integer> getObjetCSAIHMIntegerTreeTableColumn(String nomVariable, String titre) {
        TreeTableColumn<ObjetIHM, Integer> colonne = new TreeTableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<Integer>) oihm.getValue().getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Integer)");
                return null;
            }
        });
        if (nomVariable.toLowerCase().contains("ouleur")) {
            colonne.setCellFactory((TreeTableColumn) -> {
                return new TreeTableCell<ObjetIHM, Integer>() {
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

    private TreeTableColumn<ObjetIHM, java.awt.Color> getDiIHMColorTreeTableColumn(String nomVariable, String titre) {
        TreeTableColumn<ObjetIHM, java.awt.Color> colonne = new TreeTableColumn<>(titre);
        colonne.setCellFactory((TreeTableColumn) -> {
            TreeTableCell<ObjetIHM, java.awt.Color> tableCell = new TreeTableCell<ObjetIHM, java.awt.Color>() {
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
                return (ObservableValue<java.awt.Color>) oihm.getValue().getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Color)");
                return null;
            }
        });
        // colonne.setPrefWidth(Math.max(titre.length() * TAILLE_CARACTERE,
        // tableau.getWidth() / 10));
        return colonne;
    }

    public void add_valeur_dans_reference(String nom, Object donnee) {
        if (elementDeReference != null) {
            elementDeReference.inserer_valeur(nom, donnee);
        }
    }

    public void add_fk_dans_reference(String nom, ObjetCSA donnee) {
        if (elementDeReference != null) {
            elementDeReference.inserer_fk(nom, donnee);
        }
    }

    public void add_fki_dans_reference(String nomTable, String nomVariable, ObjetCSA donnee) {
        if (elementDeReference != null) {
            elementDeReference.inserer_valeur_intermediaire(nomTable, nomVariable, donnee);
        }
    }

    // ---- Configuration via ContexteFX ----

    /**
     * Configure tous les handlers en une seule fois depuis un {@link ContexteFX}.
     * Les champs non-null du contexte remplacent les handlers individuels.
     *
     * @param ctx le contexte à appliquer
     */
    public void setContexte(ContexteFX ctx) {
        this.contexte = ctx;
        if (ctx == null)
            return;
        if (ctx.getBdHandler() != null)
            this.bdHandler = ctx.getBdHandler();
        if (ctx.getTypeResolver() != null)
            this.typeResolver = ctx.getTypeResolver();
        if (ctx.getMessageHandler() != null)
            this.messageHandler = ctx.getMessageHandler();
        if (ctx.getUserActionHandler() != null)
            this.userActionHandler = ctx.getUserActionHandler();
    }

    public ContexteFX getContexteFX() {
        return contexte;
    }

    /**
     * Configure le handler BD unifié.
     * Codes : 1=ajout, 2=modification, 3=suppression, 4=consultation.
     */
    public void setBdHandler(ContexteFX.HandlerBD handler) {
        this.bdHandler = handler;
    }

    /**
     * Configure le résolveur de type (remplace
     * {@code maitreFX.getElementFromType}).
     */
    public void setTypeResolver(Function<String, ObjetCSA> resolver) {
        this.typeResolver = resolver;
    }

    /**
     * Configure le handler d'actions utilisateur.
     */
    public void setUserActionHandler(BiConsumer<Object, String> handler) {
        this.userActionHandler = handler;
    }

    /**
     * Configure le handler de messages utilisateur.
     */
    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    public ObjetCSA getElementDeReference() {
        return elementDeReference;
    }

    public void charger_liste(ArrayList<Object> donnees, String nomTable) {
        ObjetCSA ref = typeResolver != null ? typeResolver.apply(nomTable) : null;
        charger_liste(donnees, ref);
    }

    /**
     * Permet de charger les données dans la listeTable
     * Les données doivent être sous forme de liste
     *
     * @param donnees liste à insérer dans le tableau
     * @param ref     le type des données affichées (nomTable)
     *                si vide, alors ce sera le type du premier élément de la liste
     */
    public void charger_liste(ArrayList<Object> donnees, ObjetCSA ref) {
        elementDeReference = ref;
        ArrayList<ObjetIHM> objetIHMS;
        ObjetIHM elementReferenceIHM;
        String nomEntete;
        if (elementDeReference == null) {
            elementReferenceIHM = null;
            objetIHMS = new ArrayList<>();
            nomEntete = "-";
        } else {
            nomEntete = elementDeReference.getPublicName();
            objetIHMS = transformer_en_objetsIHM(donnees, elementDeReference);
            elementReferenceIHM = new ObjetIHM(elementDeReference);
        }
        Platform.runLater(() -> {
            setColumn(elementReferenceIHM);
        });
        ArrayList<TreeItem<ObjetIHM>> arbre = CreateurArborescence.creer_arborescence(new ArrayList(donnees),
                elementDeReference);

        // ✅ DEBUG : Vérifier les TreeItems créés
        System.out.println("=== DEBUG ARBRE ===");
        for (TreeItem<ObjetIHM> item : arbre) {
            debugTreeItem(item, 0);
        }

        getRoot().getChildren().setAll(arbre);
        listeOriginale = new ArrayList<>(objetIHMS);
        if (userActionHandler != null) {
            userActionHandler.accept(null, "liste reçue");
            return;
        }

    }

    private ArrayList<ObjetIHM> transformer_en_objetsIHM(ArrayList<Object> donnees, ObjetCSA eltRef) {
        return TransformeurObjetsIHM.transformer_en_objetsIHM(donnees, eltRef);
    }

    private void debugTreeItem(TreeItem<ObjetIHM> item, int niveau) {
        String indent = "  ".repeat(niveau);
        ObjetIHM value = item.getValue();
        System.out.println(indent + "TreeItem: " +
                (value == null ? "NULL"
                        : value.getObjetCSA().getNom() +
                                " (leaf=" + item.isLeaf() + ", type = " + value.getObjetCSA().getType() +
                                ", id = " + value.getObjetCSA().getId() + ")"));
        for (TreeItem<ObjetIHM> child : item.getChildren()) {
            debugTreeItem(child, niveau + 1);
        }
    }

    public ObjetCSA getSelection() {
        try {
            return getSelectionModel().getSelectedItem().getValue().getObjetCSA();
        } catch (Exception e) {
            return null;
        }
    }

    public ArrayList<ObjetCSA> getSelections() {
        List<TreeItem<ObjetIHM>> objetDiIHMs = getSelectionModel().getSelectedItems();
        ArrayList<ObjetCSA> terug = new ArrayList<>();
        objetDiIHMs.forEach(elt -> {
            terug.add(elt.getValue().getObjetCSA());
        });
        return terug;
    }

    public void affiche_utilisateur(String s) {
        if (messageHandler != null) {
            messageHandler.accept(s);
            return;
        }

        System.err.println("*** Didier : Le listeBDTable n'a pas son Gestionnaire de messages !");
    }

    public void requete_BD(int code, Object objet, GestionnaireRetour appelant) {
        if (objet instanceof ObjetCSA) {
            ObjetCSA elementBD = (ObjetCSA) objet;
            if (elementBD.getId() > 0 && code < 2) {
                System.err.println("*** Didier : ListTableTreeBD ne peut créer un nouvel élément avec id !");
                return;
            }
            if (contexte != null) {
                contexte.getBdHandler().execute(code, elementBD, resultList -> Platform.runLater(() -> {
                    if (resultList != null)
                        traitementRetour(new ArrayList<>(resultList));
                }));
                return;
            }
            envoyer_requete(code, elementBD);
            return;
        }

        System.out.println("*** ListTableTreeBD/requete_BD ne peut envoyer l'objet : " + objet);
    }

    private void envoyer_requete(int code, ObjetCSA o) {
        if (bdHandler != null) {
            bdHandler.execute(code, o, resultList -> Platform.runLater(() -> {
                if (resultList != null)
                    traitementRetour(new ArrayList<>(resultList));
            }));
            return;
        }

        affiche_console("Didier : Le listeBDTable n'a pas son Gestionnaire !");
    }

    public void affiche_console(String s) {
        if (contexte != null) {
            contexte.getConsoleHandler().accept(s);
            return;
        }
        if (messageHandler != null) {
            messageHandler.accept(s);
            return;
        }

        System.err.println("*** Didier : Le listeBDTable n'a pas son Gestionnaire !" + "\n => " + s);
    }

    public ObjetCSA getElementFromType(String typeAffiche) {
        if (typeResolver != null) {
            return typeResolver.apply(typeAffiche);
        }

        return null;
    }

    public void clearSelection() {
        getSelectionModel().clearSelection();
    }

    public void clear() {
        listeOriginale.clear();
        charger_liste(new ArrayList<>(), elementDeReference);
    }

    public ArrayList<Object> getListeComplete() {
        return new ArrayList<>(listeOriginale);
    }

    /**
     * Renvoie une copie de la liste affichée
     *
     * @return une copie de la liste affichée !
     */
    public ArrayList<ObjetCSA> getListeAffichee() {
        return getListeTotaleAffichee(getRoot());
    }

    public ArrayList<ObjetCSA> getElementsSelectionnes() {
        List<TreeItem<ObjetIHM>> selections = getSelectionModel().getSelectedItems();
        ArrayList<ObjetIHM> objetIHMS = new ArrayList<>();
        selections.forEach(treeItem -> {
            objetIHMS.add(treeItem.getValue());
        });
        return transformer_en_objetsCSA(objetIHMS);
    }

    public ArrayList<ObjetCSA> getListeOriginale() {
        return transformer_en_objetsCSA(listeOriginale);
    }

    private ArrayList<ObjetCSA> transformer_en_objetsCSA(ArrayList<ObjetIHM> objetIHMs) {
        return TransformeurObjetsIHM.transformer_en_objetsCSA(objetIHMs);
    }

    /**
     * Donne la liste des feuilles affichées
     *
     * @param racine c'est la racine de l'arbre dont le contenu nous intéresse
     * @return la liste des feuilles et sous-feuilles affichées
     */
    public ArrayList<ObjetCSA> getListeFeuilles(TreeItem<ObjetIHM> racine) {
        ArrayList<ObjetCSA> terug = new ArrayList<>();
        TreeItem<ObjetIHM> root = racine == null ? getRoot() : racine;
        if (root.isLeaf()) {
            terug.add(root.getValue().getObjetCSA());
            return terug;
        }
        for (TreeItem<ObjetIHM> treeItem : root.getChildren()) {
            if (treeItem.isLeaf()) {
                terug.add(treeItem.getValue().getObjetCSA());
            } else {
                terug.addAll(getListeFeuilles(treeItem));
            }
        }
        return terug;
    }

    /**
     * Donne la liste des racines et feuilles affichées
     *
     * @param racine c'est la racine de l'arbre dont le contenu nous intéresse
     * @return la liste des feuilles et racines
     */
    public ArrayList<ObjetCSA> getListeTotaleAffichee(TreeItem<ObjetIHM> racine) {
        ArrayList<ObjetCSA> terug = new ArrayList<>();
        TreeItem<ObjetIHM> root = racine == null ? getRoot() : racine;

        // ✅ Ajoute TOUJOURS le nœud actuel (racine, parent ou feuille)
        if (root.getValue() != null) {
            terug.add(root.getValue().getObjetCSA());
        }

        // ✅ Puis ajoute récursivement tous ses enfants
        for (TreeItem<ObjetIHM> treeItem : root.getChildren()) {
            terug.addAll(getListeTotaleAffichee(treeItem));
        }

        return terug;
    }

    private void adjustColumnWidths(TreeTableView<ObjetIHM> tableView) {
        double tableWidth = tableView.getWidth();
        int columnCount = tableView.getColumns().size();
        if (columnCount > 0) {
            double colWidth = tableWidth / columnCount;
            for (TreeTableColumn<ObjetIHM, ?> column : tableView.getColumns()) {
                column.setPrefWidth(colWidth);
            }
        }
    }

    public void addListeANePasAfficher(String nom) {
        listeEltsANePasAfficher.add(nom);
    }

    public void clearListeANePasAfficher() {
        listeEltsANePasAfficher.clear();
    }

    /**
     * Permet de charger le tableau de données vierges
     *
     * @param objetIHM chaque colonne doit avoir un titre et un type qui doivent
     *                 se trouver dans un ElementBD
     */
    public void setColumn(ObjetIHM objetIHM) {
        if (objetIHM == null) {
            affiche_console("* ListeTableBD/setColumn : Aucune donnée à charger, je reçois NULL !");
            return;
        }
        Platform.runLater(() -> {
            getColumns().clear();
            ArrayList<String> variablesVisibles = objetIHM.getVariablesVisibles();
            variablesVisibles.removeAll(listeEltsANePasAfficher);
            for (String nomVariable : variablesVisibles) {
                String titre = objetIHM.getNomPublic(nomVariable);
                Object variable = objetIHM.getObjetCSA().getSimpleData(nomVariable);
                Class classeVariable = objetIHM.getTypeOfVariable(nomVariable);
                if (classeVariable != null) {
                    if (variable == null) {
                        TreeTableColumn<ObjetIHM, Object> colonne = getObjetCSAIHMGeneralTreeTableColumn(nomVariable,
                                titre);
                        getColumns().add(colonne);
                    } else if (variable instanceof String || classeVariable.equals(String.class)) {
                        TreeTableColumn<ObjetIHM, String> colonne = getObjetCSAIHMStringTreeTableColumn(nomVariable,
                                titre);
                        getColumns().add(colonne);
                    } else if (variable instanceof ObjetCSA) {
                        TreeTableColumn<ObjetIHM, ObjetCSA> colonne = getObjetCSAIHMObjetCSATreeTableColumn(nomVariable,
                                titre);
                        getColumns().add(colonne);
                    } else if (variable instanceof Integer || classeVariable.equals(Integer.class)) {
                        TreeTableColumn<ObjetIHM, Integer> colonne = getObjetCSAIHMIntegerTreeTableColumn(nomVariable,
                                titre);
                        getColumns().add(colonne);
                    } else if (variable instanceof java.awt.Color || classeVariable.equals(java.awt.Color.class)) {
                        TreeTableColumn<ObjetIHM, java.awt.Color> colonne = getDiIHMColorTreeTableColumn(nomVariable,
                                titre);
                        getColumns().add(colonne);
                    } else if (variable instanceof Float || classeVariable.equals(Float.class)) {
                        TreeTableColumn<ObjetIHM, Float> colonne = getObjetCSAIHMFloatTreeTableColumn(nomVariable,
                                titre);
                        getColumns().add(colonne);
                    } else if (variable instanceof LocalDateTime || classeVariable.equals(LocalDateTime.class)) {
                        TreeTableColumn<ObjetIHM, LocalDateTime> colonne = getObjetCSAIHMLocalDateTimeTreeTableColumn(
                                nomVariable, titre);
                        getColumns().add(colonne);
                    } else if (variable instanceof LocalDate || classeVariable.equals(LocalDate.class)) {
                        TreeTableColumn<ObjetIHM, LocalDate> colonne = getObjetCSAIHMLocalDateTreeTableColumn(
                                nomVariable, titre);
                        getColumns().add(colonne);
                    } else if (variable instanceof Boolean || classeVariable.equals(Boolean.class)) {
                        TreeTableColumn<ObjetIHM, Boolean> colonne = getObjetCSAIHMBooleanTreeTableColumn(nomVariable,
                                titre);
                        getColumns().add(colonne);
                    } else if (variable instanceof Color || classeVariable.equals(Color.class)) {
                        TreeTableColumn<ObjetIHM, Color> colonne = getObjetCSAIHMColorTreeTableColumn(nomVariable,
                                titre);
                        getColumns().add(colonne);
                    } else if (variable instanceof List) {
                        TreeTableColumn<ObjetIHM, ObservableList> colonne = getObjetCSAIHMObservableListTreeTableColumn(
                                nomVariable, titre);
                        getColumns().add(colonne);
                    } else {
                        System.err.println("** : type non géré par le tableau pour la variable : " + variable);
                    }
                }
            }
        });
    }

    private TreeTableColumn<ObjetIHM, Boolean> getObjetCSAIHMBooleanTreeTableColumn(String nomVariable, String titre) {
        TreeTableColumn<ObjetIHM, Boolean> colonne = new TreeTableColumn<>(titre);
        colonne.setCellValueFactory(oihm -> {
            // oihm.getValue() returns the ObjetCSA instance for a particular TableView row
            try {
                return (ObservableValue<Boolean>) oihm.getValue().getValue().get(nomVariable);
            } catch (Exception e) {
                System.err.println("** Explosion dans ListTableBD ! (" + nomVariable + ", " + "Boolean)");
                return null;
            }
        });
        colonne.setCellFactory(p -> {
            CheckBoxTreeTableCell<ObjetIHM, Boolean> cell = new CheckBoxTreeTableCell<>();
            cell.setAlignment(Pos.CENTER);
            cell.setDisable(false);
            cell.setOnMousePressed(event -> {
                action_clic_checkBox(cell);
            });
            return cell;
        });
        return colonne;
    }

    private void action_clic_checkBox(Object source) {
        if (source instanceof CheckBoxTableCell) {
            ObservableList<TreeTablePosition<ObjetIHM, ?>> cellules = getSelectionModel().getSelectedCells();
            String nomColonne = null;
            if (!cellules.isEmpty()) {
                TreeTablePosition celluleSelectionnee = cellules.get(0);
                nomColonne = celluleSelectionnee.getTableColumn().getText();
            }

            ObjetCSA sel = getSelectionModel().getSelectedItem().getValue().getObjetCSA();
            if (userActionHandler != null) {
                userActionHandler.accept(sel, nomColonne);
            }
        }
    }

    private String affiche_couleur(Color couleur) {
        int opacite = (int) (couleur.getOpacity() * 255);
        int rouge = (int) (couleur.getRed() * 255);
        int vert = (int) (couleur.getGreen() * 255);
        int bleu = (int) (couleur.getBlue() * 255);

        return Integer.toHexString(opacite) + Integer.toHexString(rouge) + Integer.toHexString(vert)
                + Integer.toHexString(bleu) + " (" + (opacite + rouge + vert + bleu) + ")";
    }

    private String firstCapital(String nom) {
        if (nom.isEmpty()) {
            return "";
        }
        if (nom.length() < 2) {
            return nom.toUpperCase();
        }
        Character initiale = nom.toCharArray()[0];
        return (String.valueOf(initiale)).toUpperCase() + nom.substring(1);
    }

    @Override
    public void traitementRetour(ArrayList list) {
        if (list == null) {
            System.err.println("La listeTable reçoit une liste NULL !");
            return;
        }
        if (list.isEmpty()) {
            System.err.println("La listeTable reçoit une liste vide de la BD !");
            return;
        }
        // on doit recevoir une AL => (int, nomTable, (liste d'objetDiIHM)) ou juste une
        // AL => (int)
        if (list.get(0) instanceof Integer) {
            int code = (int) list.get(0);
            affiche_console("Info : la listeTable "
                    + (elementDeReference == null ? "NULL" : elementDeReference.getType()) + " reçoit un code " + code);
            switch (code) {
                case 1:
                    // Protocole optimisé : (1, nomTable, nouvelID)
                    // La structure hiérarchique du TreeTable empêche l'insertion directe sans
                    // connaître le parent.
                    // On effectue un rechargement si maitreFX est disponible, sinon on confirme
                    // silencieusement.
                    if (list.size() >= 3 && list.get(2) instanceof Integer nouvelID) {
                        affiche_utilisateur("Enregistrement réussi ! (ID=" + nouvelID + ")");
                        // Note : contrairement à ListTableBD, pas d'ajout direct possible dans un arbre
                        // sans connaître la position hiérarchique. Rafraîchir si nécessaire.
                    } else {
                        affiche_utilisateur("Enregistrement réussi !");
                    }
                    return;
                case 2:
                    affiche_utilisateur("Enregistrement réussi !");
                    return;
                case 3:
                    affiche_utilisateur("Suppression réussie !");
                    return;
                case 4:
                    if (list.size() < 3) {
                        System.err.println("*** paquet reçu incorrect dans ListTableBD : " + list
                                + "\n...mais j'attendais une liste de 3 éléments : (int, String, liste)");
                        break;
                    }
                    String nomTable = String.valueOf(list.get(1));
                    Object liste = list.get(2);
                    if (liste instanceof ArrayList) {
                        ArrayList<Object> listeRecue = (ArrayList) liste;
                        int taille = listeRecue.size();
                        affiche_console("* la listeTable reçoit (en direct) une liste de taille " + taille);
                        charger_liste(listeRecue, getElementFromType(nomTable));
                        return;
                    }
                    System.err.println("*** Retour de envoi détail incorrect dans ListTableBD : " + list);
                    return;
                default:
                    System.err.println("*** La listeTable reçoit un retour de requête inattendu " + list
                            + "\n...je ne fais rien !");
                    return;
            }
            return;
        }
        System.err.println("La listeTable reçoit une requête non conforme : " + list);
    }

}
