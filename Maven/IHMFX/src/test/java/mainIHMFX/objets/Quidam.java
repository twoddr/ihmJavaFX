package mainIHMFX.objets;

import general.ObjetCSA;

import java.time.LocalDate;

public class Quidam extends ObjetCSA {
    public Quidam() {
        super("Quidam", "idQuidam", "nomquidam");
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
        setPublicName("Client ou patient");
        initialiser("nomquidam", "", "Nom");
        initialiser("prenomquidam", "", "Prénom");
        initialiser("sexequidam", "Indéterminé", "Sexe");
        initialiser("datenaissancequidam", LocalDate.now().minusYears(18), "Date de naissance");
        initialiser("hospitalise", false, "Hospitalisé ?");

        setFK("fkcontact", "Contact", "Coordonnées de contact");
        setFK("fkmedecin", "Medecin", "Doc");

        initialiser_liste("Voiture", "Voitures");
        initialiser_liste("Prestation", "Prestations");
        initialiser_liste("Article", "Articles");

    }
}
