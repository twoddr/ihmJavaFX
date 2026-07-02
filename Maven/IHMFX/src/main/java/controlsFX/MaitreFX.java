package controlsFX;


import general.GestionnaireRetour;
import general.ObjetCSA;

/**
 * Ce titre est donné à toute fenêtre qui présente des
 * filles ; ces filles doivent obligatoirement passer par leur
 * fenêtre mère.
 *
 * @deprecated Utiliser les callbacks fonctionnels à la place (setSaveHandler, setDeleteHandler,
 * setRefreshDetailsHandler, setRefreshListHandler, setTypeResolver, setUserActionHandler,
 * setMessageHandler). Voir MIGRATION_LISTTABLE_HANDLERS.md et MIGRATION_CALLBACKS.md.
 */
@Deprecated
public interface MaitreFX {

    /**
     * Permet d'envoyer des requêtes vers le réseau/BD
     * Attention : Si le gestionnaire retour est null, il faudra le gérer soi-même
     *
     * @param code               le code du transfert. Par convention, si négatif,
     *                           il s'agit d'une opération interne. À ne pas envoyer vers la BD
     * @param objet              un objetIHM ou une liste d'objets IHM
     * @param gestionnaireRetour l'appelant
     */
    void appeler_bd(int code, Object objet, GestionnaireRetour gestionnaireRetour);

    void affiche_utilisateur(String texte);

    void affiche_console(String texte);

    ObjetCSA getElementFromType(String type);


    /**
     * Permet de notifier d'une action
     *
     * @param source       l'objet à l'origine (par exemple ListTableBD)
     * @param selectedItem l'élément sélectionné
     * @param extraInfo    info supplémentaire
     */
    void action_utilisateur(Object selectedItem, Object source, String extraInfo);

    /**
     * (cherche et) affiche les détails de la sélection en provenance d'une
     * ListTableBD par exemple
     *
     * @param selectedItem l'élément dont on veut le détail
     */
    void affiche_details(Object selectedItem);

    /**
     * Permet de nettoyer les champs sous ordre d'une listeFiltrable par exemple
     */
    void nettoyer_champs();

    /**
     * Permet de charger de nouvelles données pour édition. Par exemple, lors
     * d'une demande de duplication
     *
     * @param objetCSA c'est objetCSA de communs.jar
     */
    void charger_donnees_vers_enfant(ObjetCSA objetCSA);

    void rafraichir();

    boolean isTest();

}
