package generalFX;

import general.Coeur;
import general.Hacheur;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Reflection;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Objects;

public class FXLoginWindow extends Stage {

    final TextField txtUserName = new TextField();
    final PasswordField pf = new PasswordField();
    final Label lblMessage = new Label();
    //private Stage stage;
    private final Coeur mom;
    //Implementing Nodes for GridPane
    Label lblUserName = new Label("Nom d'utilisateur ");
    Label lblPassword = new Label("Mot de passe :");
    Button btnOk = new Button("Ok"), btnCancel = new Button("Quitter");
    private String titre;

    public FXLoginWindow(Coeur mere, String prelogin, String comment) {
        mom = mere;
        txtUserName.setText(prelogin);
        lblMessage.setText(comment);
        lblMessage.setTextFill(Color.BLUE);
    }


    private void check_login_pswd(String checkUser, String checkPw, Event e) {
        ArrayList<Object> requete = new ArrayList<>();
        requete.add(checkUser);
        requete.add(checkPw);
        mom.action_retour_login(this, requete);

        // empêche de fermer la boite de dialogue ?
        e.consume();
    }


    public void setHeader(String s) {
        titre = s;
    }

    public void go(String[] x) {
        Platform.runLater(() -> {
            setResizable(true);
            BorderPane borderPane = new BorderPane();
            borderPane.setPadding(new Insets(10, 50, 50, 50));
            //Adding HBox
            HBox hBox = new HBox();
            hBox.setPadding(new Insets(20, 20, 20, 30));
            //Adding GridPane
            GridPane gridPane = new GridPane();
            gridPane.setPadding(new Insets(20, 20, 20, 20));
            gridPane.setHgap(5);
            gridPane.setVgap(5);

            //Adding Nodes to GridPane layout
            gridPane.add(lblUserName, 0, 0);
            gridPane.add(txtUserName, 1, 0);
            gridPane.add(lblPassword, 0, 1);
            gridPane.add(pf, 1, 1);
            gridPane.add(lblMessage, 0, 2, 2, 2);
            HBox ligneAction = new HBox(btnOk, btnCancel);
            ligneAction.setSpacing(20);
            gridPane.add(ligneAction, 1, 4);

            //Reflection for gridPane
            Reflection r = new Reflection();
            r.setFraction(0.7f);
            gridPane.setEffect(r);
            //DropShadow effect
            DropShadow dropShadow = new DropShadow();
            dropShadow.setOffsetX(5);
            dropShadow.setOffsetY(5);
            //Adding text and DropShadow effect to it
            Text text = new Text((titre == null || titre.isEmpty()) ? "CSA-Soft" : titre);
            text.setFont(Font.font("Courier New", FontWeight.BOLD, 28));
            text.setEffect(dropShadow);
            //Adding text to HBox
            hBox.getChildren().add(text);
            //Add ID's to Nodes
            borderPane.setId("bp");
            gridPane.setId("root");
            btnOk.setId("btnLogin");
            btnCancel.setId("btnCancel");
            text.setId("text");

            btnCancel.setOnAction(e -> {
                mom.action_retour_login(FXLoginWindow.this, new ArrayList<>());
                close();
            });

            btnOk.setOnAction(this::verifier_donnees);

            txtUserName.setOnKeyReleased(keyEvent -> {
                btnOk.setDisable(txtUserName.getText().isEmpty());
            });
            pf.setOnKeyReleased(keyEvent -> {
                KeyCode touche = keyEvent.getCode();
                if (touche.equals(KeyCode.ENTER)) {
                    verifier_donnees(keyEvent);
                }
            });

            //Add HBox and GridPane layout to BorderPane Layout
            borderPane.setTop(hBox);
            borderPane.setCenter(gridPane);
            //Adding BorderPane to the scene and loading CSS
            Scene scene = new Scene(borderPane);
            setScene(scene);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/login.css"))
                    .toExternalForm());
            show();

            // Request focus on the username field by default.
            txtUserName.requestFocus();

        });

    }

    private void verifier_donnees(Event e) {
        String checkUser = txtUserName.getText();
        String checkPw = Hacheur.hashPassword(pf.getText());

        Platform.runLater(() -> {
            lblMessage.setText("Vérification des informations rentrées ...");
            lblMessage.setTextFill(Color.DARKORANGE);
        });
        check_login_pswd(checkUser, checkPw, e);

        // empêche de fermer la boite de dialogue ?
        e.consume();
    }

    public void affiche_utilisateur(String texte) {
        Platform.runLater(() -> {
            lblMessage.setText(texte);
            lblMessage.setTextFill(Color.RED);
        });
    }

}
