package mainIHMFX.objets;

import general.ExtracteurPrimitif;
import general.ObjetCSA;

public class Prestation extends ObjetCSA {
    public Prestation() {
        super("Prestation", "idprestation", "nomprestation");
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
        setPublicName("Prestation");
        initialiser("nomprestation", "", "Article");
        initialiser("prixprestation", 0f, "Prix unitaire");
        initialiser("nombreprestation", 0, "Qté");

        add_variable_a_afficher("nomprestation");
        add_variable_a_afficher("prixprestation");
        add_variable_a_afficher("nombreprestation");

        inserer_valeur("prixtotal", 0f);
        add_variable_a_afficher("prixtotal");

    }

    public float getPrixTotal() {
        return getPrixUnitaire() * getQte();
    }

    private int getQte() {
        return ExtracteurPrimitif.extraire_int(getSimpleData("nombreprestation"));
    }

    private float getPrixUnitaire() {
        return ExtracteurPrimitif.extraire_float(getSimpleData("prixprestation"));
    }
}
