package mainIHMFX.objets;

import general.ObjetCSA;

public class Ville extends ObjetCSA {
    public Ville() {
        super("Ville", "idville", "nomville");
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
        setPublicName("Ville");
        setNom("Yaoundé");
        initialiser("cp", 1980, "Code postal");
        initialiser_liste("Quartier", "Quartiers");

        setFK("fkpays", "Pays", "Pays");

    }

    public int getCp() {
        return (int) getSimpleData("cp");
    }

    public void setAnnee(int annee) {
        inserer_valeur("cp", annee);
    }

}
