package mainIHMFX.objets;

import general.ExtracteurPrimitif;
import general.ObjetCSA;

public class Article extends ObjetCSA {
    public Article() {
        super("Article", "idarticle", "nomarticle");
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
        setPublicName("Article");
        initialiser("nomarticle", "", "Article");
        initialiser("prixarticle", 0f, "Prix unitaire");
        initialiser("nombrearticle", 0, "Qté");

        add_variable_a_afficher("nomarticle");
        add_variable_a_afficher("prixarticle");
        add_variable_a_afficher("nombrearticle");

        inserer_valeur("prixtotal", 0);
        add_variable_a_afficher("prixtotal");

    }

    public float getPrixTotal() {
        return getPrixUnitaire() * getQte();
    }

    private int getQte() {
        return ExtracteurPrimitif.extraire_int(getSimpleData("nombrearticle"));
    }

    private float getPrixUnitaire() {
        return ExtracteurPrimitif.extraire_float(getSimpleData("prixarticle"));
    }
}
