package controlsFX;

import java.time.LocalDate;

public interface ConteneurGrilleMensuelle {
    void affiche_utilisateur(String texte);

    void traiter_grilleMensuelle_clic(LocalDate dateCliquee);
}
