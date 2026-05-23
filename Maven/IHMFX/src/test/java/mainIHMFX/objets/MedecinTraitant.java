package mainIHMFX.objets;

import general.ObjetCSA;

public class MedecinTraitant extends ObjetCSA {
    public MedecinTraitant() {
        super("Medecin", "iddoc", "nommedecin");
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
        setPublicName("Docteur en médecine");
        initialiser("prenommed", "Hector", "Prénom");

        setFK("fkcontactcabinet", "Contact", "Contact");
        setFK("fkcontacthopital", "Contact", "Contact");
        setFK("fkspecialite", "Specialite", "Spécialité");
    }
}
