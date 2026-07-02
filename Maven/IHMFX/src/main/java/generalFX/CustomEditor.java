package generalFX;

import controlsFX.EditorPaneController;
import general.GestionnaireRetour;
import general.ObjetCSA;
import javafx.scene.layout.BorderPane;

import java.util.ArrayList;

/**
 * CustomEditor - Éditeur personnalisé pour formulaires d'édition d'objets.
 * <p>
 * C'est un BorderPane qui implémente des méthodes comme :
 * charger_donnees, enregistrer et effacer_champs.
 * <p>
 * Il est destiné à être utilisé dans les EditorPaneController pour fournir une interface d'édition personnalisée pour différents types d'objets.
 *
 */
public abstract class CustomEditor extends BorderPane {

    protected final EditorPaneController editorPane;

    public CustomEditor(EditorPaneController editorPane) {
        this.editorPane = editorPane;
    }

    protected void appeler_bd(int code, ObjetCSA objet, GestionnaireRetour retour) {
        editorPane.appeler_bd(code, objet, retour);
    }

    protected void affiche_message(String texte) {
        editorPane.affiche_utilisateur(texte);
    }

    public abstract void charger_donnees(ObjetCSA objetDi);

    /**
     * Permet de remplir l'objetihm par les données qui se trouvent
     * actuellement dans les champs de données
     * Ne doit pas communiquer avec la BD !!!
     *
     * @return l'objetihm qui contiendra les informations des champs; null si échec !
     */
    public abstract ObjetCSA aspirer_donnees();

    public abstract void nettoyer_champs();

    /**
     * Ordonne au custumEditor de demander un rafraichissement de toutes ses listes
     */
    public abstract void rafraichir_listes();

    /**
     * Permet de rafraichir les liste du type fourni par la liste fournie
     *
     * @param objets c'est la liste qui devrait écraser la liste correspondante dans un des Composants
     * @param type   c'est le nom de table dans la BD du type de données qui est rafraichi
     */
    public abstract void rafraichir_liste(ArrayList<?> objets, String type);

}
