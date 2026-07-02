package controlsFX;

import javafx.scene.Node;

public interface EcouteurFX {

    /**
     * Permet d'être au courant des actions sur comboFiltrable
     *
     * @param selection          ça devrait être l'élément sélectionné du combo, liste, etc
     * @param source             c'est le composant qui s'identifie lui-même
     * @param infoSupplementaire c'est l'événement qui a donné lieu à l'appel de la
     *                           fonction ou un string ou un booléen en fonction du contexte
     */
    void action_retour_component(Object selection,
                                 Node source,
                                 Object infoSupplementaire);

}
