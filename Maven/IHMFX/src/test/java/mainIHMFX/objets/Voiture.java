package mainIHMFX.objets;

import general.ObjetCSA;

public class Voiture extends ObjetCSA {
    public Voiture() {
        super("Voiture", "idvoiture", "marque");
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
        setPublicName("Voiture");
        setNom("");
        initialiser("modelevoiture", "", "Modèle");
        initialiser("anneevoiture", 1980, "Année");
        add_variable_a_afficher("marque");
        add_variable_a_afficher("modelevoiture");
        add_variable_a_afficher("anneevoiture");
    }

    public int getAnnee() {
        return (int) getSimpleData("anneevoiture");
    }

    public void setAnnee(int annee) {
        inserer_valeur("anneevoiture", annee);
    }

    public String getMarque() {
        return String.valueOf(getSimpleData("marque"));
    }

    public void setMarque(String marque) {
        inserer_valeur("marque", marque);
    }
}
