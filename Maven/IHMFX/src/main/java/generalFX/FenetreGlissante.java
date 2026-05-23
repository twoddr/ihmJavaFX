package generalFX;

import java.util.ArrayList;

public class FenetreGlissante<T> {
    // Pour usage externe (dans les listeners de scroll)
    public static final int SCROLL_THRESHOLD = 10;
    public static final double SCROLL_POSITION_HAUTE = 0.2;
    public static final double SCROLL_POSITION_BASSE = 0.8;
    // CONFIGURATION POUR PC MODESTE
    private static final int TAILLE_FENETRE = 150;
    private static final int TAILLE_BATCH = 50;
    private static final int OVERLAP = 25;
    private static final int GC_SUGGEST_INTERVAL = 50;
    private final ArrayList<T> listeTotale = new ArrayList<>();
    private int indexDebut = 0;
    private int indexFin = 0;
    private int operationCount = 0;

    /**
     * Charge l'intégralité des données et initialise la première fenêtre
     *
     * @param donnees les données à charger
     */
    public void chargerTout(ArrayList<T> donnees) {
        listeTotale.clear();
        listeTotale.addAll(donnees);
        indexDebut = 0;
        indexFin = Math.min(TAILLE_FENETRE, listeTotale.size());
        operationCount = 0;
    }

    /**
     * Retourne les éléments de la fenêtre actuelle
     *
     * @return liste présentement affichée
     */
    public ArrayList<T> getFenetreActuelle() {
        if (indexDebut >= listeTotale.size()) {
            return new ArrayList<>();
        }

        ArrayList<T> fenetre = new ArrayList<>(
                listeTotale.subList(indexDebut, indexFin)
        );

        // GC périodique pour PC modeste
        operationCount++;
        if (operationCount % GC_SUGGEST_INTERVAL == 0) {
            System.gc();
        }

        return fenetre;
    }

    /**
     * Charge le batch suivant avec overlap
     *
     * @return true si des données ont été chargées, false si fin de liste
     */
    public boolean chargerSuivants() {
        if (indexFin >= listeTotale.size()) {
            return false;
        }

        // Nouveau début = ancien fin - overlap
        indexDebut = Math.max(0, indexFin - OVERLAP);

        // Nouvelle fin = ancien fin + batch
        indexFin = Math.min(indexFin + TAILLE_BATCH, listeTotale.size());

        return true;
    }

    /**
     * Charge le batch précédent avec overlap
     *
     * @return true si des données ont été chargées, false si début de liste
     */
    public boolean chargerPrecedents() {
        if (indexDebut <= 0) {
            return false;
        }

        // Nouveau début = ancien début - batch
        int nouveauDebut = Math.max(0, indexDebut - TAILLE_BATCH);

        // Nouvelle fin = ancien début + overlap
        indexFin = Math.min(indexDebut + OVERLAP, listeTotale.size());
        indexDebut = nouveauDebut;

        return true;
    }

    /**
     * Réinitialise la fenêtre au début
     */
    public void resetFenetre() {
        indexDebut = 0;
        indexFin = Math.min(TAILLE_FENETRE, listeTotale.size());
    }

    /**
     * Vérifie si la pagination est nécessaire
     *
     * @return la taille de la liste est plus grande que la fenêtre?
     */
    public boolean needsPagination() {
        return listeTotale.size() > TAILLE_FENETRE;
    }

    /**
     * Retourne la taille totale de la liste
     *
     * @return taille de la liste
     */
    public int getTailleTotale() {
        return listeTotale.size();
    }

    /**
     * Retourne la taille de la fenêtre actuelle
     *
     * @return taille de la fenêtre
     */
    public int getTailleFenetre() {
        return indexFin - indexDebut;
    }

    /**
     * Retourne l'index de début
     *
     * @return premier indice affiché
     */
    public int getIndexDebut() {
        return indexDebut;
    }

    /**
     * Retourne l'index de fin
     *
     * @return dernier indice affiché
     */
    public int getIndexFin() {
        return indexFin;
    }

    /**
     * Vérifie si on est au début de la liste
     *
     * @return début de la liste?
     */
    public boolean isAtStart() {
        return indexDebut == 0;
    }

    /**
     * Vérifie si on est à la fin de la liste
     *
     * @return fin de la liste?
     */
    public boolean isAtEnd() {
        return indexFin >= listeTotale.size();
    }

    /**
     * Nettoie et libère la mémoire
     */
    public void clear() {
        listeTotale.clear();
        listeTotale.trimToSize();
        indexDebut = 0;
        indexFin = 0;
        operationCount = 0;
    }

    /**
     * Retourne des infos de debug
     *
     * @return informations de déboguage sur les tailles de fenetre et de liste
     */
    public String getDebugInfo() {
        return String.format("Fenêtre [%d-%d] sur %d éléments (%.1f%%)",
                indexDebut, indexFin, listeTotale.size(),
                (indexFin * 100.0) / Math.max(1, listeTotale.size()));
    }
}