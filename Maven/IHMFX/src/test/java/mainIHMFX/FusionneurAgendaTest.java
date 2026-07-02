package mainIHMFX;


import general.calendrier.Evenement;
import general.calendrier.FusionneurAgendas;
import general.calendrier.FusionneurEvenements;
import general.calendrier.Trou;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.TreeSet;

public class FusionneurAgendaTest {

    private static final String NOM_TYPE = "type";
    private static final String NOM_ID = "id";
    private static final String NOM_DESCRIPTION = "description";
    private static final String NOM_COULEUR = "couleurTexte";
    private static final String NOM_IDINTERMEDIAIRE = "idIntermediaire";
    private static final String NOM_DATEDU = "dateDu";
    private static final String NOM_DATEAU = "dateAu";
    private static final String NOM_COULEUR_CADRE = "couleurCadre";
    private static final String NOM_COULEUR_FOND = "couleurFond";
    private static final String NOM_LIEU = "LIEU";
    private static final Evenement evenementIsole = new Evenement(NOM_TYPE, NOM_ID, NOM_DESCRIPTION,
            NOM_COULEUR, NOM_IDINTERMEDIAIRE, NOM_DATEDU, NOM_DATEAU,
            NOM_COULEUR_CADRE, NOM_COULEUR_FOND, NOM_LIEU, null);
    private static ArrayList<Evenement> evenements = new ArrayList<>();

    public static void main(String[] args) {
        LocalDate aujourdhui = LocalDate.now();
        evenementIsole.setDebut_Duree(aujourdhui.atTime(10, 0), 240);
        evenementIsole.setDescription("[bloc de 4h]");

        // remplir les événements
        for (LocalDateTime parcours = aujourdhui.atTime(6, 0);
             parcours.isBefore(aujourdhui.atTime(19, 59));
             parcours = parcours.plusHours(2)) {
            Double duree = Math.random() * 120;
            Evenement evenement = new Evenement(NOM_TYPE, NOM_ID, NOM_DESCRIPTION,
                    NOM_COULEUR, NOM_IDINTERMEDIAIRE, NOM_DATEDU, NOM_DATEAU,
                    NOM_COULEUR_CADRE, NOM_COULEUR_FOND, NOM_LIEU, null);
            evenement.setDebut_Duree(parcours.plusMinutes(0), duree.intValue());
            evenement.setDescription("[" + evenement.getDuree() + "]");
            evenements.add(evenement);
        }

        /*
         ********** interactions avec un événement
         */
        Evenement evenementTest = new Evenement(NOM_TYPE, NOM_ID, NOM_DESCRIPTION,
                NOM_COULEUR, NOM_IDINTERMEDIAIRE, NOM_DATEDU, NOM_DATEAU,
                NOM_COULEUR_CADRE, NOM_COULEUR_FOND, NOM_LIEU, null);
        FusionneurEvenements telescopeur = new FusionneurEvenements();
        // événement est avant
        evenementTest.setDebut_Duree(evenementIsole.getDebut().minusHours(3), 120);
        evenementTest.setDescription("fin à " + evenementTest.getFin().format(DateTimeFormatter.ofPattern("HH:mm")));
        System.out.println("*** Télescopeur : Fusion de " + evenementTest + " avec " + evenementIsole);
        TreeSet<Evenement> resultat = telescopeur.telescoper(evenementTest, evenementIsole);
        afficher_contenu_telescopeur(telescopeur, resultat);
        // événement est en chevauchement sup
        evenementTest.setDebut_Duree(evenementIsole.getDebut().minusHours(1), 120);
        evenementTest.setDescription("fin à " + evenementTest.getFin().format(DateTimeFormatter.ofPattern("HH:mm")));
        System.out.println("*** Télescopeur : Fusion de " + evenementTest + " avec " + evenementIsole);
        TreeSet<Evenement> resultat1 = telescopeur.telescoper(evenementTest, evenementIsole);
        afficher_contenu_telescopeur(telescopeur, resultat1);
        // événement est englobé
        evenementTest.setDebut_Duree(evenementIsole.getDebut().plusHours(2), 120);
        evenementTest.setDescription("fin à " + evenementTest.getFin().format(DateTimeFormatter.ofPattern("HH:mm")));
        System.out.println("*** Télescopeur : Fusion de " + evenementTest + " avec " + evenementIsole);
        TreeSet<Evenement> resultat2 = telescopeur.telescoper(evenementTest, evenementIsole);
        afficher_contenu_telescopeur(telescopeur, resultat2);
        // événement est en chevauchement inf
        evenementTest.setDebut_Duree(evenementIsole.getFin().minusHours(1), 120);
        evenementTest.setDescription("fin à " + evenementTest.getFin().format(DateTimeFormatter.ofPattern("HH:mm")));
        System.out.println("*** Télescopeur : Fusion de " + evenementTest + " avec " + evenementIsole);
        TreeSet<Evenement> resultat3 = telescopeur.telescoper(evenementTest, evenementIsole);
        afficher_contenu_telescopeur(telescopeur, resultat3);
        // événement est après
        evenementTest.setDebut_Duree(evenementIsole.getFin().plusHours(1), 120);
        evenementTest.setDescription("fin à " + evenementTest.getFin().format(DateTimeFormatter.ofPattern("HH:mm")));
        System.out.println("*** Télescopeur : Fusion de " + evenementTest + " avec " + evenementIsole);
        TreeSet<Evenement> resultat4 = telescopeur.telescoper(evenementTest, evenementIsole);
        afficher_contenu_telescopeur(telescopeur, resultat4);

        /*
         ***** interactions avec un agenda

         */
// non prioritaire
        System.out.println("\n*** Insertion de trous sur toute la journée dans " + evenements);
        FusionneurAgendas fusionneurAgendas = new FusionneurAgendas(NOM_TYPE, NOM_ID, NOM_DESCRIPTION,
                NOM_COULEUR, NOM_IDINTERMEDIAIRE, NOM_DATEDU, NOM_DATEAU,
                NOM_COULEUR_CADRE, NOM_COULEUR_FOND, NOM_LIEU, null);
        Trou nouveau = new Trou(NOM_TYPE, NOM_ID, NOM_DESCRIPTION,
                NOM_COULEUR, NOM_IDINTERMEDIAIRE, NOM_DATEDU, NOM_DATEAU,
                NOM_COULEUR_CADRE, NOM_COULEUR_FOND, NOM_LIEU, null);
        nouveau.setDebut_Fin(aujourdhui.atStartOfDay(),
                aujourdhui.atTime(23, 59));
        fusionneurAgendas.inserer_non_prioritaire(nouveau,
                new TreeSet<>(evenements));
        afficher_contenu_fusionneur(fusionneurAgendas);

        // prioritaire
        System.out.println("\n*** Insertion d'un prioritaire : " + evenementTest
                + " dans " + evenements);
        FusionneurAgendas fusionneurAgendas2 = new FusionneurAgendas(NOM_TYPE, NOM_ID, NOM_DESCRIPTION,
                NOM_COULEUR, NOM_IDINTERMEDIAIRE, NOM_DATEDU, NOM_DATEAU,
                NOM_COULEUR_CADRE, NOM_COULEUR_FOND, NOM_LIEU, null);
        fusionneurAgendas2.inserer_prioritaire(evenementTest, new TreeSet<>(evenements));
        afficher_contenu_fusionneur(fusionneurAgendas2);

        // autre prioritaire
        ArrayList<Trou> trous = new ArrayList<>();
        Trou trou = new Trou(NOM_TYPE, NOM_ID, NOM_DESCRIPTION,
                NOM_COULEUR, NOM_IDINTERMEDIAIRE, NOM_DATEDU, NOM_DATEAU,
                NOM_COULEUR_CADRE, NOM_COULEUR_FOND, NOM_LIEU, null);
        trou.setDebut_Fin(aujourdhui.atStartOfDay(),
                aujourdhui.atTime(23, 59));
        trous.add(trou);
        System.out.println("\n*** Insertion d'un prioritaire : " + evenementTest
                + " dans " + trous);
        FusionneurAgendas fusionneurAgendas3 = new FusionneurAgendas(NOM_TYPE, NOM_ID, NOM_DESCRIPTION,
                NOM_COULEUR, NOM_IDINTERMEDIAIRE, NOM_DATEDU, NOM_DATEAU,
                NOM_COULEUR_CADRE, NOM_COULEUR_FOND, NOM_LIEU, null);
        fusionneurAgendas3.inserer_prioritaire(evenementTest, new TreeSet<>(trous));
        afficher_contenu_fusionneur(fusionneurAgendas3);

    }

    private static void afficher_contenu_telescopeur(FusionneurEvenements telescopeur,
                                                     TreeSet<Evenement> resultat) {
        System.out.println("________________");
        System.out.println("Collision ? " + (resultat.size() == 0));
        System.out.println("Gauche :" + telescopeur.getNonPrioritaireGauche());
        System.out.println("MiLIEU,null :" + telescopeur.getPrioritaire());
        System.out.println("Droite :" + telescopeur.getNonPrioritaireDroite());
        System.out.println("Retour = " + resultat);
        System.out.println("________________");
    }

    private static void afficher_contenu_fusionneur(FusionneurAgendas fusionneur) {
        System.out.println("________________");
        System.out.println("Créés :" + fusionneur.getCrees());
        System.out.println("Tronqués :" + fusionneur.getTronques());
        System.out.println("Sauvés :" + fusionneur.getSauves());
        System.out.println("Bannis = " + fusionneur.getBannis());
        System.out.println("________________");
    }
}
