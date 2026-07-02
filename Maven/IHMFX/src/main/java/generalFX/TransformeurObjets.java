package generalFX;

import controlsFX.MaitreFX;
import general.InformateurObjet;
import general.ObjetCSA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Function;

public class TransformeurObjets {

    /**
     * Transforme une liste en ObjetIHM en utilisant ref comme modèle.
     * Le paramètre mom est ignoré (conservé pour compatibilité ascendante).
     *
     * @deprecated Utiliser {@link #transformer_en_objetsIHM(ArrayList, ObjetCSA)} à la place.
     */
    @Deprecated
    public static ArrayList<ObjetIHM> transformer_en_objetsIHM(ArrayList<Object> donnees,
                                                               ObjetCSA ref,
                                                               MaitreFX mom) {
        return transformer_en_objetsIHM(donnees, ref);
    }

    /**
     * Transforme une liste en ObjetIHM en utilisant ref comme modèle de type.
     */
    public static ArrayList<ObjetIHM> transformer_en_objetsIHM(ArrayList<Object> donnees,
                                                               ObjetCSA ref) {
        ArrayList<ObjetIHM> terug = new ArrayList<>();
        for (Object element : donnees) {
            terug.add(transformer_en_objetsIHM(element, ref));
        }
        return terug;
    }

    /**
     * @deprecated Utiliser {@link #transformer_en_objetsIHM(Object, ObjetCSA)} à la place.
     */
    @Deprecated
    public static ObjetIHM transformer_en_objetsIHM(Object element, ObjetCSA ref, MaitreFX mom) {
        return transformer_en_objetsIHM(element, ref);
    }

    public static ArrayList<ObjetIHM> transformer_en_objetsIHM(ArrayList<? extends ObjetCSA> donnees) {
        ArrayList<ObjetIHM> terug = new ArrayList<>();
        for (ObjetCSA element : donnees) {
            element.clearModifications();
            terug.add(new ObjetIHM(element));
        }
        return terug;
    }

    public static ObjetIHM transformer_en_objetsIHM(Object element,
                                                    ObjetCSA ref) {
        if (element instanceof ObjetCSA) {
            ObjetCSA hashObjet = (ObjetCSA) element;
            return new ObjetIHM(hashObjet);
        }
        if (element instanceof ObjetIHM) {
            ((ObjetIHM) element).getObjetCSA().clearModifications();
            return (ObjetIHM) element;
        }
        if (element instanceof HashMap) {
            HashMap<String, Object> hashMap = (HashMap<String, Object>) element;
            ObjetCSA hashObjet = ref.getNewInstance();
            hashObjet.setData(hashMap, null, true, false);
            hashObjet.clearModifications();
            return new ObjetIHM(hashObjet);
        }
        return null;
    }

    /**
     * Surcharge avec un résolveur de type fonctionnel (sans MaitreFX).
     *
     * @param typeResolver la fonction pour créer une instance vide du type donné
     */
    public static ArrayList<ObjetCSA> transformer_en_objetsDi(ArrayList<?> donnees,
                                                              String nomTable,
                                                              HashMap<String, Object> hashlevis,
                                                              Function<String, ObjetCSA> typeResolver) {
        InformateurObjet adaptateur = typeResolver != null ? typeResolver::apply : t -> null;
        return transformer_en_objetsDi(donnees, nomTable, hashlevis, (Object) adaptateur);
    }

    public static ArrayList<ObjetCSA> transformer_en_objetsDi(ArrayList<?> donnees,
                                                              String nomTable,
                                                              HashMap<String, Object> hashlevis,
                                                              Object informateur) {
        ArrayList<ObjetCSA> terug = new ArrayList<>();
        for (Object donnee : donnees) {
            terug.add(transformer_en_objetDi(donnee, nomTable, hashlevis, informateur));
        }
        return terug;
    }

    /**
     * Transforme une liste formatée d'hashmaps en liste formatée d'objetDis
     *
     * @param objets      une liste de type (code, nomTable, (liste d'hashmaps),hashmap)
     *                    la hasmap est utilisée pour recréer les détails des objets créés
     * @param informateur celui qui connait le nom des tables
     * @return retourne (code, nomTable, (liste d'objetDis))
     */
    public static ArrayList<Object> transformerListe(ArrayList<Object> objets,
                                                     InformateurObjet informateur) {
        if (objets.size() > 2 &&
                objets.get(0) instanceof Integer &&
                objets.get(2) instanceof ArrayList) {
            ArrayList liste = (ArrayList) objets.get(2);
            Integer code = (Integer) objets.get(0);
            HashMap<String, Object> hashLevis = null;
            if (objets.size() > 3 && objets.get(3) instanceof HashMap) {
                HashMap hashMap = (HashMap) objets.get(3);
                hashLevis = hashMap;
            }
            String nomTable = String.valueOf(objets.get(1));
            ArrayList<Object> retour = new ArrayList<>();
            retour.add(code);
            retour.add(nomTable);
            retour.add(transformerListePure(liste, nomTable, hashLevis, informateur));
            return retour;
        }
        return null;
    }

    /**
     * Transforme une liste d'hashmaps en objetDis
     *
     * @param objects     les hashmaps
     * @param hashLevis   les données détaillées
     * @param informateur un maitreFX ou un informateurObjets
     * @return liste vide si pas d'hashmap
     */
    private static ArrayList<ObjetCSA> transformerListePure(ArrayList<Object> objects, String nomTable,
                                                            HashMap<String, Object> hashLevis,
                                                            Object informateur) {
        ArrayList<ObjetCSA> terug = new ArrayList<>();
        for (Object object : objects) {
            terug.add(transformer_en_objetDi(object, nomTable, hashLevis, informateur));
        }
        return terug;
    }

    /**
     * Transforme en ObjetCSA, un objetIHM ou une hashmap
     *
     * @param element     peut importe ce que c'est, on fera ce qu'on pourra
     * @param nomTable    le nom de la table concernée
     * @param hashlevis   une hashmap de détails
     * @param informateur celui qui connait les tables
     * @return un objetDi avec les paramètres fournis
     */
    public static ObjetCSA transformer_en_objetDi(Object element,
                                                  String nomTable,
                                                  HashMap<String, Object> hashlevis,
                                                  Object informateur) {
        if (element instanceof ObjetCSA) {
            ObjetCSA objetDi = (ObjetCSA) element;
            objetDi.clearModifications();
            return objetDi;
        }
        if (element instanceof ObjetIHM) {
            ObjetIHM objetIHM = (ObjetIHM) element;
            ObjetCSA hashObjet = objetIHM.getObjetCSA();
            hashObjet.clearModifications();
            return hashObjet;
        }
        if (element instanceof HashMap) {
            HashMap<String, Object> hashMap = (HashMap<String, Object>) element;
            ObjetCSA hashObjet = null;
            if (informateur instanceof InformateurObjet) {
                InformateurObjet informateurObjet = (InformateurObjet) informateur;
                hashObjet = informateurObjet.getObjetFromType(nomTable);
            } else if (informateur instanceof MaitreFX) {
                MaitreFX maitreFX = (MaitreFX) informateur;
                hashObjet = maitreFX.getElementFromType(nomTable);
            } else {
                System.err.println("TransfomateurObjets : Je n'ai pas d'informateur pour créer un objetDi àpd " + nomTable +
                        " et " + element);
                return null;
            }
            hashObjet.setData(hashMap, hashlevis, true, false);
            hashObjet.clearModifications();
            return hashObjet;
        }
        return null;
    }

    public static ArrayList<ObjetCSA> transformer_en_objetsDi(Collection<ObjetIHM> objetDiIHMS) {
        ArrayList<ObjetCSA> terug = new ArrayList<>();
        objetDiIHMS.forEach(etl -> {
            etl.getObjetCSA().clearModifications();
            terug.add(etl.getObjetCSA());
        });
        return terug;
    }

}
