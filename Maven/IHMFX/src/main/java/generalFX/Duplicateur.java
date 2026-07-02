package generalFX;


import general.ObjetCSA;

public class Duplicateur {

    Object objet;

    public Duplicateur(Object x) {
        objet = x;
    }

    public Object obtenirDuplicata() {
        if (objet instanceof ObjetCSA dbObject) {
            dbObject.setId(0);
            dbObject.setNom(creer_nouveau_nom(dbObject.getNom()));
        } else if (objet instanceof String) {
            return creer_nouveau_nom(((String) objet));
        }
        return objet;
    }

    private String creer_nouveau_nom(String nom) {
        if (!nom.isEmpty()) {
            Integer longueurUtile = nom.length() - 1;
            String dernierChar = nom.substring(longueurUtile);
            Integer dernierCaractere = null;
            try {
                dernierCaractere = Integer.parseInt(dernierChar);
                if (dernierCaractere == 9) {
                    return nom + "1";
                }
                return nom.substring(0, longueurUtile) + (dernierCaractere + 1);
            } catch (NumberFormatException e) {
                return nom + "_1";
            }
        }
        return nom + "_1";
    }
}
