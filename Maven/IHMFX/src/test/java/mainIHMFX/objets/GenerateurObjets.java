package mainIHMFX.objets;

import general.GestionnaireImbrications;
import general.Individu;
import general.InformateurObjet;
import general.ObjetCSA;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class GenerateurObjets implements InformateurObjet {
    public static MedecinTraitant getDoc() {
        MedecinTraitant doc = new MedecinTraitant();
        doc.inserer_valeur("prenommed", "Hector");
        doc.setNom("Denis");
        doc.setId(1);

        Contact adrCabinet = new Contact();
        adrCabinet.setId(2);
        adrCabinet.setNom("Avenue Amadou Ahidjo 123");
        adrCabinet.inserer_valeur("email", "alamimboudnd@hotmail.cm");
        adrCabinet.inserer_valeur("tel1", "098765432");
        adrCabinet.inserer_valeur("tel2", "+1234567890");
        doc.inserer_fk("fkcontactcabinet", adrCabinet);
        Contact adrHopital = new Contact();
        adrHopital.setId(3);
        adrHopital.setNom("Dernier virage 43");
        adrHopital.inserer_valeur("email", "docteur_alamimbou@chy.cm");
        adrHopital.inserer_valeur("tel1", "0532");
        adrHopital.inserer_valeur("tel2", "+4567");
        doc.inserer_fk("fkcontacthopital", adrHopital);

        Specialite specialite = new Specialite();
        specialite.setNom("Gynécologie");
        specialite.inserer_valeur("prix", 23f);
        specialite.inserer_valeur("nomcourtspec", "GYN");
        doc.inserer_fk("fkspecialite", specialite);
        return doc;
    }

    public static Pays getPaysCMR() {
        Pays pays = new Pays();
        pays.inserer_valeur("indicatif", 237);
        pays.setNom("Cameroun");
        pays.setId(1);
        pays.inserer_valeur("nomcourt", "CMR");
        ArrayList<Ville> villes = new ArrayList<>();
        villes.add(getDouala());
        villes.add(getYaounde());
        pays.inserer_liste("Ville", villes);
        return pays;
    }

    public static Pays getPaysBE() {
        Pays pays = new Pays();
        pays.inserer_valeur("indicatif", 33);
        pays.setNom("Belgique");
        pays.setId(2);
        pays.inserer_valeur("nomcourt", "BE");
        ArrayList<Ville> villes = new ArrayList<>();
        villes.add(getLiege());
        villes.add(getBruxelles());
        pays.inserer_liste("Ville", villes);
        return pays;
    }

    public static Ville getLiege() {
        Ville ville = new Ville();
        ville.inserer_valeur("cp", 4000);
        ville.setNom("Liège");
        ville.setId(1);
        ville.inserer_valeur("nomcourt", "Lg");
        ArrayList<Quartier> quartiers = new ArrayList<>();
        quartiers.add(getFleron());
        quartiers.add(getDroixhe());
        ville.inserer_liste("Quartier", quartiers);
        Pays pays = new Pays();
        pays.setNom("Belgique");
        pays.setId(2);
        ville.inserer_fk("fkpays", pays);
        return ville;
    }

    public static Ville getBruxelles() {
        Ville ville = new Ville();
        ville.inserer_valeur("cp", 1000);
        ville.setNom("Bruxelles");
        ville.setId(2);
        ville.inserer_valeur("nomcourt", "Bxl");
        ArrayList<Quartier> quartiers = new ArrayList<>();
        quartiers.add(getMatongue());
        quartiers.add(getLouise());
        ville.inserer_liste("Quartier", quartiers);
        Pays pays = new Pays();
        pays.setNom("Belgique");
        pays.setId(2);
        ville.inserer_fk("fkpays", pays);
        return ville;
    }

    public static Ville getDouala() {
        Ville ville = new Ville();
        ville.inserer_valeur("cp", 20);
        ville.setNom("Douala");
        ville.setId(3);
        ville.inserer_valeur("nomcourt", "DLA");
        ArrayList<Quartier> quartiers = new ArrayList<>();
        quartiers.add(getBonanjo());
        quartiers.add(getAkwa());
        ville.inserer_liste("Quartier", quartiers);
        Pays pays = new Pays();
        pays.setNom("Cameroun");
        pays.setId(1);
        ville.inserer_fk("fkpays", pays);
        return ville;
    }

    public static Ville getYaounde() {
        Ville ville = new Ville();
        ville.inserer_valeur("cp", 10);
        ville.setNom("Yaoundé");
        ville.setId(4);
        ville.inserer_valeur("nomcourt", "Ydé");
        ArrayList<Quartier> quartiers = new ArrayList<>();
        quartiers.add(getBastos());
        ville.inserer_liste("Quartier", quartiers);
        Pays pays = new Pays();
        pays.setNom("Cameroun");
        pays.setId(1);
        ville.inserer_fk("fkpays", pays);
        return ville;
    }

    public static Quartier getBonanjo() {
        Quartier quartier = new Quartier();
        quartier.setId(1);
        quartier.setNom("Bonanjo");
        Ville douala = new Ville();
        douala.setNom("Douala");
        douala.setId(3);
        quartier.inserer_fk("Ville", douala);
        return quartier;
    }

    public static Quartier getAkwa() {
        Quartier quartier = new Quartier();
        quartier.setId(2);
        quartier.setNom("Akwa");
        Ville douala = new Ville();
        douala.setNom("Douala");
        douala.setId(3);
        quartier.inserer_fk("Ville", douala);
        return quartier;
    }

    public static Quartier getBastos() {
        Quartier quartier = new Quartier();
        quartier.setId(3);
        quartier.setNom("Bastos");
        Ville yde = new Ville();
        yde.setNom("Liège");
        yde.setId(4);
        quartier.inserer_fk("Ville", yde);
        return quartier;
    }

    public static Quartier getFleron() {
        Quartier quartier = new Quartier();
        quartier.setId(4);
        quartier.setNom("Fléron");
        Ville lg = new Ville();
        lg.setNom("Liège");
        lg.setId(1);
        quartier.inserer_fk("Ville", lg);
        return quartier;
    }

    public static Quartier getDroixhe() {
        Quartier quartier = new Quartier();
        quartier.setId(5);
        quartier.setNom("Bressoux");
        Ville lg = new Ville();
        lg.setNom("Liège");
        lg.setId(1);
        quartier.inserer_fk("Ville", lg);
        return quartier;
    }

    public static Quartier getMatongue() {
        Quartier quartier = new Quartier();
        quartier.setId(6);
        quartier.setNom("Matongue");
        Ville bxl = new Ville();
        bxl.setNom("Bruxelles");
        bxl.setId(2);
        quartier.inserer_fk("Ville", bxl);
        return quartier;
    }

    public static Quartier getLouise() {
        Quartier quartier = new Quartier();
        quartier.setId(7);
        quartier.setNom("Louise");
        Ville bxl = new Ville();
        bxl.setNom("Bruxelles");
        bxl.setId(2);
        quartier.inserer_fk("Ville", bxl);
        return quartier;
    }

    public static MedecinTraitant getDoc2() {
        MedecinTraitant doc = new MedecinTraitant();
        doc.inserer_valeur("prenommed", "Nadine");
        doc.setNom("Nana");
        doc.setId(2);

        Contact adrCabinet = new Contact();
        adrCabinet.setId(4);
        adrCabinet.setNom("Avenue Amadou Ahidjo 123");
        adrCabinet.inserer_valeur("email", "nanana@hotmail.cm");
        adrCabinet.inserer_valeur("tel1", "0987355432");
        adrCabinet.inserer_valeur("tel2", "+34566888");
        doc.inserer_fk("fkcontactcabinet", adrCabinet);
        Contact adrHopital = new Contact();
        adrHopital.setId(3);
        adrHopital.setNom("Dernier virage 43");
        adrHopital.inserer_valeur("email", "docteur_alamimbou@chy.cm");
        adrHopital.inserer_valeur("tel1", "0532");
        adrHopital.inserer_valeur("tel2", "+4567");
        doc.inserer_fk("fkcontacthopital", adrHopital);

        Specialite specialite = new Specialite();
        specialite.setNom("Gynécologie");
        specialite.inserer_valeur("prix", 23f);
        specialite.inserer_valeur("nomcourtspec", "GYN");
        doc.inserer_fk("fkspecialite", specialite);
        return doc;
    }

    public static MedecinTraitant getDoc3() {
        MedecinTraitant doc = new MedecinTraitant();
        doc.inserer_valeur("prenommed", "Ali");
        doc.setNom("Mohamed");
        doc.setId(3);

        Contact adrCabinet = new Contact();
        adrCabinet.setId(6);
        adrCabinet.setNom("Avenue Amadou Ahidjo 14");
        adrCabinet.inserer_valeur("email", "mohali@hotmail.cm");
        adrCabinet.inserer_valeur("tel1", "098765432");
        adrCabinet.inserer_valeur("tel2", "+123456780");
        doc.inserer_fk("fkcontactcabinet", adrCabinet);
        Contact adrHopital = new Contact();
        adrHopital.setId(3);
        adrHopital.setNom("Dernier virage 43");
        adrHopital.inserer_valeur("email", "docteur_alamimbou@chy.cm");
        adrHopital.inserer_valeur("tel1", "0532");
        adrHopital.inserer_valeur("tel2", "+4567");
        doc.inserer_fk("fkcontacthopital", adrHopital);

        Specialite specialite = new Specialite();
        specialite.setNom("Gynécologie");
        specialite.inserer_valeur("prix", 23f);
        specialite.inserer_valeur("nomcourtspec", "GYN");
        doc.inserer_fk("fkspecialite", specialite);
        return doc;
    }

    public static ArrayList<Voiture> getListeVoitures() {
        ArrayList<Voiture> voitures = new ArrayList<>();
        Voiture voiture1 = new Voiture();
        voiture1.setNom("Dodge");
        voiture1.setId(1);
        voiture1.inserer_valeur("modelevoiture", "Viper");
        voiture1.inserer_valeur("anneevoiture", 1980);
        Voiture voiture2 = new Voiture();
        voiture2.setId(2);
        voiture2.setNom("Toyota");
        voiture2.inserer_valeur("modelevoiture", "CHR");
        voiture2.inserer_valeur("anneevoiture", 2021);

        voitures.add(voiture1);
        voitures.add(voiture2);
        return voitures;
    }

    public static ArrayList<Voiture> getListeVoitures2() {
        ArrayList<Voiture> voitures = new ArrayList<>();
        Voiture voiture1 = new Voiture();
        voiture1.setNom("BYD");
        voiture1.setId(4);
        voiture1.inserer_valeur("modelevoiture", "Atto 3");
        voiture1.inserer_valeur("anneevoiture", 2025);
        Voiture voiture2 = new Voiture();
        voiture2.setId(5);
        voiture2.setNom("Kia");
        voiture2.inserer_valeur("modelevoiture", "Picanto");
        voiture2.inserer_valeur("anneevoiture", 2021);
        Voiture voiture3 = new Voiture();
        voiture3.setId(6);
        voiture3.setNom("Tesla");
        voiture3.inserer_valeur("modelevoiture", "Model S");
        voiture3.inserer_valeur("anneevoiture", 2023);

        voitures.add(voiture1);
        voitures.add(voiture3);
        voitures.add(voiture2);
        return voitures;
    }

    public static ArrayList<Prestation> getListePrestations() {
        ArrayList<Prestation> prestations = new ArrayList<>();
        Prestation prestation1 = new Prestation();
        prestation1.setNom("Consultation simple");
        prestation1.setId(4);
        prestation1.inserer_valeur("prixprestation", 23.4f);
        prestation1.inserer_valeur("nombreprestation", 1);
        Prestation prestation2 = new Prestation();
        prestation2.setId(5);
        prestation2.setNom("Revisite");
        prestation2.inserer_valeur("prixprestation", 18.1f);
        prestation2.inserer_valeur("nombreprestation", 3);
        Prestation prestation3 = new Prestation();
        prestation3.setId(6);
        prestation3.setNom("Opération chirurgicale");
        prestation3.inserer_valeur("prixprestation", 234.5f);
        prestation3.inserer_valeur("nombreprestation", 2);

        prestations.add(prestation1);
        prestations.add(prestation3);
        prestations.add(prestation2);
        return prestations;
    }

    public static ArrayList<Article> getListeArticles() {
        ArrayList<Article> articles = new ArrayList<>();
        Article article1 = new Article();
        article1.setNom("Consultation simple");
        article1.setId(4);
        article1.inserer_valeur("prixarticle", 23.4f);
        article1.inserer_valeur("nombrearticle", 1);
        Article article2 = new Article();
        article2.setId(5);
        article2.setNom("Revisite");
        article2.inserer_valeur("prixarticle", 18.1f);
        article2.inserer_valeur("nombrearticle", 3);
        Article article3 = new Article();
        article3.setId(6);
        article3.setNom("Opération chirurgicale");
        article3.inserer_valeur("prixarticle", 234.5f);
        article3.inserer_valeur("nombrearticle", 2);

        articles.add(article1);
        articles.add(article3);
        articles.add(article2);
        return articles;
    }

    public static ObjetCSA createSampleObject() {
        ObjetCSA individu = new Quidam();
        // Same as previous example
        individu.setNom("Alamimbou");
        individu.setId(1);
        individu.inserer_valeur("prenomquidam", "Dieu ne dort");
        individu.inserer_valeur("datenaissancequidam", LocalDate.of(1976, 2, 8));
        individu.inserer_valeur("hospitalise", true);
        individu.inserer_valeur("sexequidam", "Masculin");

        individu.inserer_valeur("adresse", "Rue de la liberté 1");
        individu.inserer_valeur("email", "alamimboudnd@hotmail.cm");
        individu.inserer_valeur("tel1", "098765432");
        individu.inserer_valeur("tel2", "+1234567890");

        GestionnaireImbrications gestionnaireImbrications = getLocaliteBonanjo();
        individu.addGestionnaire("fkquartier", gestionnaireImbrications, "Quartier");

        MedecinTraitant doc = getDoc();
        individu.inserer_fk("fkmedecin", doc);

        ArrayList<Voiture> listeVoitures = getListeVoitures();
        individu.inserer_liste("Voiture", listeVoitures);

        ArrayList<Prestation> prestations = GenerateurObjets.getListePrestations();
        individu.inserer_liste("Prestation", prestations);

        ArrayList<Article> articles = GenerateurObjets.getListeArticles();
        individu.inserer_liste("Article", articles);

        return individu;
    }

    private static GestionnaireImbrications getLocaliteBonanjo() {
        GestionnaireImbrications gestionnaireImbrications = new GestionnaireImbrications("Pays",
                "fkpays", "Ville", "fkville",
                "Quartier", 0, null, new GenerateurObjets());

        HashMap<String, Object> hashLevis = new HashMap<>();
        hashLevis.put("Quartier-1", new ArrayList<>(Arrays.asList("Quartier",
                new HashMap<String, Object>() {{
                    put("nomquartier", "Hout-Si-Plout");
                    put("idquartier", 1);
                    put("fkville", 1);
                }})));
        hashLevis.put("Ville-1", new ArrayList<>(Arrays.asList("Ville",
                new HashMap<String, Object>() {{
                    put("nomville", "Douala");
                    put("idville", 1);
                    put("fkpays", 1);
                    put("cp", 20);
                }})));
        hashLevis.put("Ville_1", new ArrayList<>(Arrays.asList("Quartier",
                new HashMap<String, Object>() {{
                    put("nomquartier", "Hout-Si-Plout");
                    put("idquartier", 1);
                    put("fkville", 1);
                }},
                new HashMap<String, Object>() {{
                    put("nomquartier", "Akwa");
                    put("idquartier", 2);
                    put("fkville", 1);
                }},
                new HashMap<String, Object>() {{
                    put("nomquartier", "Bonamoussadi");
                    put("idquartier", 3);
                    put("fkville", 1);
                }})));
        hashLevis.put("Pays_1", new ArrayList<>(Arrays.asList("Ville",
                new HashMap<String, Object>() {{
                    put("nomville", "Yaoundé");
                    put("idville", 2);
                    put("fkpays", 1);
                }},
                new HashMap<String, Object>() {{
                    put("nomville", "Douala");
                    put("idville", 1);
                    put("fkpays", 1);
                }},
                new HashMap<String, Object>() {{
                    put("nomville", "Nkongsamba");
                    put("idville", 3);
                    put("fkpays", 1);
                }})));

        gestionnaireImbrications.remplir_donnees(1, hashLevis);
        gestionnaireImbrications.setListePays(new ArrayList<>(Arrays.asList(getPaysBE(), getPaysCMR())));

        return gestionnaireImbrications;
    }

    @Override
    public ObjetCSA getObjetFromType(String s) {
        switch (s) {
            case "Contact":
                return new Contact();
            case "Medecin":
                return new MedecinTraitant();
            case "Pays":
                return new Pays();
            case "Ville":
                return new Ville();
            case "Quartier":
                return new Quartier();
            case "Voiture":
                return new Voiture();
            case "Specialite":
                return new Specialite();
            case "Article":
                return new Article();
            case "Prestation":
                return new Prestation();
            case "Individu":
                return new Individu();
        }
        System.err.println("Attention : L'informateurObjets generateurObjets ne connait pas le type " + s +
                "\nJe retourne NULL");
        return null;
    }
}
