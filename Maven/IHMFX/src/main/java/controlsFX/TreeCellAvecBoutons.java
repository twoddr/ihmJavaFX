package controlsFX;

import generalFX.ObjetIHM;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

public class TreeCellAvecBoutons extends TreeCell<ObjetIHM> {
    private final HBox cellBox;
    private final Label label;
    private final Button btnAjouter;
    private final Button btnRafraichir;
    private final Button btnSupprimer;
    private Consumer<TreeItem<ObjetIHM>> onAjouter;
    private Consumer<TreeItem<ObjetIHM>> onRafraichir;
    private Consumer<TreeItem<ObjetIHM>> onSupprimer;

    public TreeCellAvecBoutons() {
        cellBox = new HBox(5);
        cellBox.setAlignment(Pos.CENTER_LEFT);

        label = new Label();
        label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);

        // Bouton Ajouter
        btnAjouter = new Button();
        btnAjouter.setGraphic(createIcon("/images/add.png"));
        btnAjouter.setTooltip(new Tooltip("Ajouter un enfant"));
        btnAjouter.getStyleClass().add("tree-button");
        btnAjouter.setFocusTraversable(false);
        btnAjouter.setOnAction(e -> {
            if (onAjouter != null && getTreeItem() != null) {
                onAjouter.accept(getTreeItem());
            }
            e.consume();
        });

        // Bouton Rafraîchir
        btnRafraichir = new Button();
        btnRafraichir.setGraphic(createIcon("/images/refresh.png"));
        btnRafraichir.setTooltip(new Tooltip("Rafraîchir les enfants"));
        btnRafraichir.getStyleClass().add("tree-button");
        btnRafraichir.setFocusTraversable(false);
        btnRafraichir.setOnAction(e -> {
            if (onRafraichir != null && getTreeItem() != null) {
                onRafraichir.accept(getTreeItem());
            }
            e.consume();
        });

        // Bouton Supprimer (visible seulement sur les feuilles)
        btnSupprimer = new Button();
        btnSupprimer.setGraphic(createIcon("/images/delete.png"));
        btnSupprimer.setTooltip(new Tooltip("Supprimer cet élément"));
        btnSupprimer.getStyleClass().add("tree-button-danger");
        btnSupprimer.setFocusTraversable(false);
        btnSupprimer.setOnAction(e -> {
            if (onSupprimer != null && getTreeItem() != null) {
                onSupprimer.accept(getTreeItem());
            }
            e.consume();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.SOMETIMES);

        cellBox.getChildren().addAll(label, spacer, btnAjouter, btnRafraichir, btnSupprimer);
    }

    private ImageView createIcon(String path) {
        try {
            ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream(path)));
            imageView.setFitHeight(16);
            imageView.setFitWidth(16);
            imageView.setPreserveRatio(true);
            return imageView;
        } catch (Exception e) {
            System.err.println("Impossible de charger l'icône: " + path);
            return new ImageView();
        }
    }

    public void setOnAjouter(Consumer<TreeItem<ObjetIHM>> handler) {
        this.onAjouter = handler;
    }

    public void setOnRafraichir(Consumer<TreeItem<ObjetIHM>> handler) {
        this.onRafraichir = handler;
    }

    public void setOnSupprimer(Consumer<TreeItem<ObjetIHM>> handler) {
        this.onSupprimer = handler;
    }

    @Override
    protected void updateItem(ObjetIHM item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            label.setText(item.toString());

            // Vérifier si l'objet peut avoir des enfants via getTypeEnfant()
            boolean canHaveChildren = item.getObjetCSA().getTypeEnfant() != null;
            boolean isParent = getTreeItem() != null && !getTreeItem().isLeaf();
            boolean isLeaf = getTreeItem() != null && getTreeItem().isLeaf();

            // Afficher bouton "Ajouter" si l'objet peut avoir des enfants
            btnAjouter.setVisible(canHaveChildren);
            btnAjouter.setManaged(canHaveChildren);

            // Afficher bouton "Rafraîchir" si c'est un parent avec des enfants chargés
            btnRafraichir.setVisible(isParent && canHaveChildren);
            btnRafraichir.setManaged(isParent && canHaveChildren);

            // Afficher bouton "Supprimer" seulement pour les feuilles non-placeholder
            boolean isPlaceholder = ConstantesFX.STRING_VIDE.equals(item.toString());
            boolean showSupprimer = isLeaf && !isPlaceholder;
            btnSupprimer.setVisible(showSupprimer);
            btnSupprimer.setManaged(showSupprimer);

            // Style en gras pour les parents potentiels
            label.setStyle(canHaveChildren ? "-fx-font-weight: bold;" : "-fx-font-weight: normal;");

            setGraphic(cellBox);
        }
    }
}