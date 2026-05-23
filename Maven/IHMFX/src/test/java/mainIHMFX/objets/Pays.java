package mainIHMFX.objets;

import general.ObjetCSA;

public class Pays extends ObjetCSA {
    public Pays() {
        super("Pays", "idpays", "nompays");
        reinitialiser();
    }

    @Override
    public ObjetCSA getParent() {
        return null;
    }

    @Override
    public void setParent(ObjetCSA objetCSA) {

    }

    @Override
    public ObjetCSA getObjetFromType(String s) {
        return new GenerateurObjets().getObjetFromType(s);
    }

    @Override
    public void reinitialiser() {
        setPublicName("Pays");
        setNom("Cameroun");
        initialiser("indicatif", 237, "Indicatif");
        initialiser_liste("Ville", "Villes");

    }

    public int getIndicatif() {
        return (int) getSimpleData("ind");
    }

    public void setIndicatif(int annee) {
        inserer_valeur("ind", annee);
    }
}
