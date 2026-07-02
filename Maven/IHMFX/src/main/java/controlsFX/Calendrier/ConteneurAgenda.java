
package controlsFX.Calendrier;


import general.ObjetCSA;
import general.calendrier.Evenement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;

public interface ConteneurAgenda {

    void envoyerPrestations(Collection<? extends Evenement> listeEvenements);

    ObjetCSA getElementBD();


    void demande_suppression_liste(ArrayList<? extends Evenement> listeASupprimer);

    String enregistrer(Evenement Evenement, String priorite);

    void afficher_evenement(Evenement Evenement);

    void ajouter(Evenement Evenement);

    void afficheUtilisateur(String s);

    void modifier(ArrayList<Evenement> Evenements);

    void afficheConsole(String s);

    /**
     * renvoie la liste d'événements préalablement copiés ou coupés
     *
     * @return la liste ordonnée des événements
     */
    TreeSet<? extends Evenement> getListeSouris();

    /**
     * La listeJour du calendrier copie les événements copiés ici
     *
     * @param EvenementTreeSet Événements à copier/couper
     */
    void setListeSouris(TreeSet<Evenement> EvenementTreeSet);

    /**
     * Détermine si on a copié ou coupé la listeSouris
     *
     * @return oui si on a coupé
     */
    boolean isCouper();

    /**
     * Spécifie le mode de copiage pour adapter le collage de listeSouris
     *
     * @param b il faut couper ou pas?
     */
    void setCouper(boolean b);
}
