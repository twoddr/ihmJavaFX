package generalFX;


import general.ObjetCSA;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ObjetIHM extends HashMap<String, Property> implements Comparable {
    private ObjetCSA objetCSA;
    private HashMap<String, Class> hashClasses = new HashMap<>();


    public ObjetIHM(ObjetCSA oDi) {
        initialiser(oDi);
    }

    private void initialiser(ObjetCSA oDi) {
        this.objetCSA = oDi;

        objetCSA.getFKs().forEach((key, objetDi1) -> {
            objetDi1.getSimpleData().forEach((nomVariable, valeur) -> {
                put(key + "_" + nomVariable, valeur, objetDi1.getVariableClass(nomVariable));
            });
        });
        objetCSA.getFKIs().forEach((key, objetDi1) -> {
            new ObjetIHM(objetDi1).getData().forEach((nomVariable, valeur) -> {
                put(key + "_" + nomVariable, valeur, objetDi1.getVariableClass(nomVariable));
            });
        });
        objetCSA.getHashmapDependants().forEach((key, hashMap) -> {
            hashMap.forEach((nomVariable, valeur) -> {
                if (valeur == null) {
                    put(key + "_" + nomVariable, " - ", String.class);
                } else {
                    put(key + "_" + nomVariable, valeur, valeur.getClass());
                }
            });
        });
        objetCSA.getGestionnairesImbrications().forEach((key, gestionnaire) -> {
            ObjetCSA elementDispo = gestionnaire.getQuartier();
            if (elementDispo == null) elementDispo = gestionnaire.getVille();
            if (elementDispo == null) elementDispo = gestionnaire.getPays();
            put(key, elementDispo.toExport(), String.class);
        });
        objetCSA.getHashListes().forEach((cle, sousObjetCSAs) -> {
            put(cle, creer_liste_ihm(sousObjetCSAs), List.class);
        });
        // au cas où il y aurait des noms de variable identiques dans plusieurs
        // tables, il vaut mieux travailler sur la liste principale en dernier !
        objetCSA.getSimpleData().forEach((nomVariable, valeur) -> {
            put(nomVariable, valeur, objetCSA.getVariableClass(nomVariable));
        });
    }

    private ArrayList<ObjetIHM> creer_liste_ihm(Object sousObjetCSAs) {
        ArrayList<ObjetIHM> terug = new ArrayList<>();
        if (sousObjetCSAs instanceof List) {
            ((List<?>) sousObjetCSAs).forEach(elt -> {
                if (elt instanceof ObjetCSA) {
                    terug.add(new ObjetIHM((ObjetCSA) elt));
                }
            });
        }
        return terug;
    }


    public ObjetCSA getObjetCSA() {
        return objetCSA;
    }

    public void setObjetCSA(ObjetCSA objetCSA) {
        initialiser(objetCSA);
    }

    public String getType() {
        return objetCSA.getType();
    }

    /**
     * Récupère l'ID de l'élément actuel
     *
     * @return l'id pour la BD
     */
    public int getId() {
        return objetCSA.getId();
    }

    public void setId(int i) {
        objetCSA.setId(i);
    }

    public boolean isModifie() {
        return !objetCSA.getHashModifications().isEmpty();
    }

    public String getNom() {
        return objetCSA.getNom();
    }

    public void setNom(String s) {
        objetCSA.setNom(s);
    }

    public Color getColor() {
        java.awt.Color awtColor = general.RGBConverter.convert_int_2_color(objetCSA.getCouleur());
        int r = awtColor.getRed();
        int g = awtColor.getGreen();
        int b = awtColor.getBlue();
        int a = awtColor.getAlpha();
        double opacity = a / 255.0;
        return Color.rgb(r, g, b, 1);
    }

    /**
     * Cette méthode vérifie d'abord que la valeur rentrée est du
     * type attendu avant d'effectuer l'insertion. Autrement, c'est ignoré !
     * Egalement ignoré si la valeur vaut NULL
     * * Permet d'insérer la valeur dans la property stockée
     * * S'il n'existe par de property avec le nom indiqué, une nouvelle est créée
     * * avec le type de l'objet valeur. Si le nom existe déjà, le type est vérifié
     * *
     * * @param nom    le nom de la property
     * * @param valeur la nouvelle valeur à attribuer
     * * @return Vrai si tout s'est bien passé, false sinon
     *
     * @param nom    le nom de la variable
     * @param valeur la valeur à insérer; ne peut être null
     * @param classe C'est le type de l'objet (intéressant lorsque la valeur vaut NULL)
     * @return vrai si succès, faux sinon
     */
    public boolean put(String nom, Object valeur, Class classe) {
        Property ancienneProperty = super.get(nom);

        if (ancienneProperty != null) {
            if (ancienneProperty instanceof SimpleListProperty &&
                    valeur instanceof List) {
                return insert_value(nom, valeur, classe);
            }
            // accepter les int en tant que bool
            if (ancienneProperty instanceof BooleanProperty &&
                    valeur instanceof Integer) {
                int nombre = (int) valeur;
                boolean vraiFaux = nombre != 0;
                System.out.println("* ObjetIHM/insererPrudemment : le nombre " + nombre +
                        " a été transformé en booléen : " + vraiFaux);
                return insert_value(nom, vraiFaux, classe);
            }
            // accepter les int en tant que float
            if (ancienneProperty instanceof FloatProperty &&
                    valeur instanceof Integer) {
                float nombre = ((Integer) valeur).floatValue();
                System.out.println("* ObjetIHM/insererPrudemment : le nombre " + valeur +
                        " a été transformé en float : " + nombre);
                return insert_value(nom, nombre, classe);
            }
            // accepter les float en tant que int
            if (ancienneProperty instanceof IntegerProperty &&
                    valeur instanceof Float) {
                int nombre = ((Float) valeur).intValue();
                System.out.println("* ObjetIHM/insererPrudemment : le nombre " + valeur +
                        " a été transformé en int : " + nombre);
                return insert_value(nom, nombre, classe);
            }
            // accepter les localdatetime en tant que localdate
            if (ancienneProperty.getValue() instanceof LocalDate &&
                    valeur instanceof LocalDateTime) {
                LocalDate date = ((LocalDateTime) valeur).toLocalDate();
                System.out.println("* ObjetIHM/insererPrudemment : la dateTime " + valeur +
                        " a été transformé en LocalDate : " + date);
                return insert_value(nom, date, classe);
            }
            // accepter les localdate en tant que localdateTime
            if (ancienneProperty.getValue() instanceof LocalDateTime &&
                    valeur instanceof LocalDate) {
                LocalDateTime dateTime = ((LocalDate) valeur).atStartOfDay();
                System.out.println("* ObjetIHM/insererPrudemment : la date " + valeur +
                        " a été transformé en LocalDateTime : " + dateTime);
                return insert_value(nom, dateTime, classe);
            }
        }

        return insert_value(nom, valeur, classe);
    }

    private boolean insert_value(String nom, Object valeur, Class classe) {
        if (valeur == null) {
            if (classe == null) {
                System.err.println("ObjetIHM/insertValue : Problème : je ne connais pas le type de la variable " +
                        nom + " de l'objet [" + this + "]. Insertion annulée !!!");
                return false;
            }
            Property simpleProperty = getPropertyFromClass(classe);
            if (simpleProperty != null) {
                super.put(nom, simpleProperty);
                hashClasses.put(nom, classe);
                return true;
            }
            System.err.println("ObjetIHM/insertValue : Problème : je n'ai pas pu trouver une propriété pour la variable " +
                    nom + " de l'objet [" + this + "] qui est de type " + classe.getSimpleName() + ". Insertion annulée !!!");
            return false;
        }
        hashClasses.put(nom, classe);
        if (valeur instanceof String) {
            SimpleStringProperty simpleStringProperty = new SimpleStringProperty(this, nom, String.valueOf(valeur));
            super.put(nom, simpleStringProperty);
            return true;
        }
        if (valeur instanceof Integer) {
            SimpleIntegerProperty simpleIntegerProperty = new SimpleIntegerProperty(this, nom, (Integer) valeur);
            super.put(nom, simpleIntegerProperty);
            return true;
        }
        if (valeur instanceof Long) {
            SimpleLongProperty simpleLongProperty = new SimpleLongProperty(this, nom, (Long) valeur);
            super.put(nom, simpleLongProperty);
            return true;
        }
        if (valeur instanceof Float) {
            SimpleFloatProperty simpleFloatProperty = new SimpleFloatProperty(this, nom, (Float) valeur);
            super.put(nom, simpleFloatProperty);
            return true;
        }
        if (valeur instanceof Double) {
            SimpleDoubleProperty property = new SimpleDoubleProperty(this, nom, (Double) valeur);
            super.put(nom, property);
            return true;
        }
        if (valeur instanceof Boolean) {
            SimpleBooleanProperty property = new SimpleBooleanProperty(this, nom, (Boolean) valeur);
            super.put(nom, property);
            return true;
        }
        if (valeur instanceof List) {
            ObservableList observableList = FXCollections.observableList((List<?>) valeur);
            SimpleListProperty property = new SimpleListProperty(this, nom, observableList);
            super.put(nom, property);
            return true;
        }
        SimpleObjectProperty property = new SimpleObjectProperty(this, nom, valeur);
        super.put(nom, property);

        return valeur instanceof LocalDateTime ||
                valeur instanceof LocalDate ||
                valeur instanceof Timestamp;
    }

    private Property getPropertyFromClass(Class variableClass) {
        if (variableClass == null) {
            return null;
        }
        if (variableClass.equals(String.class)) {
            return new SimpleStringProperty();
        }
        if (variableClass.equals(Integer.class)) {
            return new SimpleIntegerProperty();
        }
        if (variableClass.equals(Long.class)) {
            return new SimpleLongProperty();
        }
        if (variableClass.equals(Float.class)) {
            return new SimpleFloatProperty();
        }
        if (variableClass.equals(Double.class)) {
            return new SimpleDoubleProperty();
        }
        if (variableClass.equals(Boolean.class)) {
            return new SimpleBooleanProperty();
        }
        if (variableClass.equals(List.class)) {
            return new SimpleListProperty();
        }

        return new SimpleObjectProperty();
    }

    /**
     * Permet de dire si deux eltIHM sont égaux ou pas.
     * Les critères d'égalité sont : l'equals de l'objetDi interne
     *
     * @param o l'autre eltIHM
     * @return vrai si les objetDi répondent equals entre eux
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof ObjetIHM) {
            ObjetIHM autre = (ObjetIHM) o;
            if (getId() == 0 && autre.getId() == 0) {
                return getNom().equals(autre.getNom());
            }
            return objetCSA.equals(autre.objetCSA);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (getId() == 0) {
            return Objects.hash(getNom()); // Hash based on name for objects with ID=0
        } else {
            return Objects.hash(objetCSA); // Hash based on the objetCSA for others
        }
    }

    /**
     * Donne une copie du contenu des variables par paires de (nomBD, data)
     * Les noms de variable sont les noms des champs de la BD
     *
     * @return une hashmap de (nom, donnée)
     */
    public HashMap<String, Object> getData() {
        return objetCSA.getData();
    }

    /**
     * Sert à récupérer les données modifiées de l'objet courant Sous forme de
     * paires (nomBD, valeur)
     *
     * @return une hashmap de (nom, donnéee) correspondant aux modifications
     */
    public HashMap<String, Object> getModifications() {
        return objetCSA.getHashModifications();
    }

    @Override
    public String toString() {
        return objetCSA.toString();
    }

    public String getPublicName() {
        return objetCSA.getPublicName();
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof ObjetIHM) {
            ObjetIHM autre = (ObjetIHM) o;
            if (getId() == 0 && autre.getId() == 0) {
                return getNom().compareTo(autre.getNom());
            }
            return objetCSA.compareTo(((ObjetIHM) o).getObjetCSA());
        }
        return 0;
    }

    public String getNomPublic(String name) {
        String reponse = objetCSA.getListeNomsPublics().get(name);
        return reponse == null ? name : reponse;
    }

    public String getNomPublic() {
        return objetCSA.getPublicName();
    }

    /**
     * renvoie une COPIE de la liste des variables à afficher
     *
     * @return une copie des variables visibles
     */
    public ArrayList<String> getVariablesVisibles() {
        return objetCSA.getVariablesAAfficher();
    }

    /**
     * renvoie une COPIE de la liste des paires (nom, nom public)
     *
     * @return une COPIE de la liste des paires (nom, nom public)
     */
    public HashMap<String, String> getNomsPublics() {
        return objetCSA.getListeNomsPublics();
    }

    public Class getTypeOfVariable(String variable) {
        return hashClasses.get(variable);
    }
}
