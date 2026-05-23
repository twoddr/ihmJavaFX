package mainIHMFX.objets;

import general.GestionnaireImbrications;
import general.ObjetCSA;

public class Contact extends ObjetCSA {
    public Contact() {
        super("Contact", "idcontact", "adresse");
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
        setPublicName("Coordonnées de contact");
        setNom("");
        initialiser("email", "", "Courriel");
        initialiser("tel1", "", "Téléphone personnel");
        initialiser("tel2", "", "Téléphone professionnel");

        GestionnaireImbrications gestionnaireImbrications = new GestionnaireImbrications("Pays",
                "fkpays", "Ville", "fkville",
                "Quartier", 0, null, new GenerateurObjets());
        addGestionnaire("fkquartier", gestionnaireImbrications, "Quartier");

    }

    public String getCourriel() {
        return (String) getSimpleData("email");
    }

    public void setCourriel(String email) {
        inserer_valeur("email", email);
    }

    public String getTel1() {
        return (String) getSimpleData("tel1");
    }

    public void setTel1(String tel) {
        inserer_valeur("tel1", tel);
    }

    public String getTel2() {
        return (String) getSimpleData("tel2");
    }

    public void setTel2(String tel) {
        inserer_valeur("tel2", tel);
    }
}
