package mainIHMFX.objets;

import general.ObjetCSA;

public class Specialite extends ObjetCSA {
    public Specialite() {
        super("specialite", "idspec", "nomspecialite");
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
        initialiser("prix", 0f,"Montant");
        initialiser("nomcourtspec", "","px");
    }
}
