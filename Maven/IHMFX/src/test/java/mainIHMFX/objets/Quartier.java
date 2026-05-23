package mainIHMFX.objets;

import general.ObjetCSA;

public class Quartier extends ObjetCSA {
    public Quartier() {
        super("Quartier", "idquartier", "nomquartier");
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
        setPublicName("Localité");
        setNom("Bastos");

        setFK("fkville", "Ville", "Ville");
    }

}
