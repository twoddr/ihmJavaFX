package generalFX;

import java.util.ArrayList;
import java.util.HashMap;

import general.ObjetCSA;

/**
 * Utilitaire de transformation entre ObjetCSA et ObjetIHM.
 * Centralise les méthodes qui étaient dupliquées dans
 * ListTableBDController, ListTableTreeBD et TableViewCRUD.
 * <p>
 * Cette classe fait partie de la couche IHM et dépend de {@link ObjetIHM}.
 * Pour les transformations sans IHM, voir {@code general.TransformeurObjets}
 * (dans la librairie "communs").
 */
public final class TransformeurObjetsIHM {

    private TransformeurObjetsIHM() {
        // Classe utilitaire non instanciable
    }

    /**
     * Transforme une liste d'objets CSA en liste d'Objets IHM.
     *
     * @param objetsCSA liste d'objets métier
     * @return liste d'objets IHM correspondants
     */
    public static ArrayList<ObjetIHM> transformer_en_objetsIHM(ArrayList<? extends ObjetCSA> objetsCSA) {
        ArrayList<ObjetIHM> retour = new ArrayList<>();
        if (objetsCSA == null)
            return retour;
        for (ObjetCSA objetCSA : objetsCSA) {
            retour.add(new ObjetIHM(objetCSA));
        }
        return retour;
    }

    /**
     * Transforme une liste d'objets hétérogènes (Object) en liste d'Objets IHM.
     * Gère les cas où les éléments sont déjà des {@link ObjetCSA},
     * des {@link HashMap} (données brutes) ou des {@link ObjetIHM}.
     *
     * @param donnees liste d'objets à transformer
     * @param eltRef  objet de référence pour créer une instance via getCopy() si
     *                nécessaire
     * @return liste d'objets IHM
     */
    @SuppressWarnings("unchecked")
    public static ArrayList<ObjetIHM> transformer_en_objetsIHM(ArrayList<Object> donnees, ObjetCSA eltRef) {
        ArrayList<ObjetIHM> retour = new ArrayList<>();
        if (donnees == null)
            return retour;
        for (Object o : donnees) {
            if (o instanceof ObjetCSA objetCSA) {
                retour.add(new ObjetIHM(objetCSA));
            } else if (o instanceof HashMap hashmap) {
                ObjetCSA objetCSA = eltRef.getCopy();
                objetCSA.setData(hashmap, null, true, false);
                retour.add(new ObjetIHM(objetCSA));
            } else if (o instanceof ObjetIHM oIhm) {
                retour.add(oIhm);
            }
        }
        return retour;
    }

    /**
     * Transforme une liste d'Objets IHM en liste d'objets CSA.
     *
     * @param objetsIHM liste d'objets IHM
     * @return liste d'objets métier
     */
    public static ArrayList<ObjetCSA> transformer_en_objetsCSA(ArrayList<? extends ObjetIHM> objetsIHM) {
        ArrayList<ObjetCSA> retour = new ArrayList<>();
        if (objetsIHM == null)
            return retour;
        for (ObjetIHM objetIHM : objetsIHM) {
            retour.add(objetIHM.getObjetCSA());
        }
        return retour;
    }
}
