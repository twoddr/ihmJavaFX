package mainIHMFX;

import controlsFX.MaitreFX;
import general.GestionnaireRetour;
import general.Individu;
import general.ObjetCSA;
import generalFX.ObjetIHM;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;

public class Main extends Application implements MaitreFX {

    public static ArrayList<ObjetCSA> alc = new ArrayList<>();

    public static void main(String[] args) {
        alc.add(new Individu() {

            @Override
            public ObjetCSA getParent() {
                return null;
            }

            @Override
            public ObjetCSA getObjetFromType(String s) {
                return null;
            }

            @Override
            public void setParent(ObjetCSA objetDi) {

            }


        });
        alc.add(new Individu() {

            @Override
            public ObjetCSA getParent() {
                return null;
            }

            @Override
            public ObjetCSA getObjetFromType(String s) {
                return null;
            }

            @Override
            public void setParent(ObjetCSA objetDi) {

            }


        });
        alc.add(new Individu() {

            @Override
            public ObjetCSA getParent() {
                return null;
            }

            @Override
            public ObjetCSA getObjetFromType(String s) {
                return null;
            }

            @Override
            public void setParent(ObjetCSA objetDi) {

            }


        });

        Individu susan = new Individu() {

            @Override
            public ObjetCSA getParent() {
                return null;
            }

            @Override
            public ObjetCSA getObjetFromType(String s) {
                return null;
            }

            @Override
            public void setParent(ObjetCSA objetDi) {

            }


        };
        susan.setIdLocalite(1);
        susan.setCodePostal(4870);
        susan.setDateNaissance(LocalDate.of(1976, 2, 8).atStartOfDay());
        alc.add(susan);

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        TableListTestController root = new TableListTestController();
        primaryStage.setTitle("Hello World");
        //root.set(this);
        root.charger_liste("Personne", null, convertir_liste(alc));
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();

    }

    private ArrayList<ObjetIHM> convertir_liste(ArrayList<ObjetCSA> hashObjets) {
        ArrayList<ObjetIHM> terug = new ArrayList<>();
        hashObjets.forEach(hashObjet -> {
            terug.add(new ObjetIHM(hashObjet));
        });
        return terug;
    }


    @Override
    public void appeler_bd(int code, Object objet, GestionnaireRetour gestionnaireRetour) {

    }


    @Override
    public void affiche_utilisateur(String texte) {

    }

    @Override
    public void affiche_console(String texte) {

    }

    @Override
    public ObjetCSA getElementFromType(String typeAffiche) {
        return null;
    }


    @Override
    public void affiche_details(Object selectedItem) {

    }

    @Override
    public void nettoyer_champs() {

    }

    @Override
    public void charger_donnees_vers_enfant(ObjetCSA hashObjet) {

    }

    @Override
    public void rafraichir() {

    }


    @Override
    public boolean isTest() {
        return true;
    }

    @Override
    public void action_utilisateur(Object selectedItem, Object source, String nom) {
        System.err.println("Main ne gère pas les actions" +
                " utilisateur de ses enfants !");
    }

    public static class Personne extends Individu {
//        public final StringProperty propertyNom = new SimpleStringProperty("nom");
//        public final StringProperty propertyPrenom = new SimpleStringProperty("prenom");
//        public final StringProperty propertyCourriel = new SimpleStringProperty("courriel");
//        public final IntegerProperty propertyCP = new SimpleIntegerProperty(1000);
//        public final IntegerProperty propertyAge = new SimpleIntegerProperty(18);
//        public final ObjectProperty propertyDate = new SimpleObjectProperty(LocalDate.now());


        public Personne(int i, String n, String p, String e) {
            setId(i);
            setNom(n);
            setPrenom(p);
            setCourriel(e);

            setPublicName("Patient Test");

            variablesAAfficher.clear();
            add_variable_a_afficher("nom");
            add_variable_a_afficher("dateNaissance");
            add_variable_a_afficher("courriel");
            add_variable_a_afficher("id");
            add_variable_a_afficher("prenom");
        }

        @Override
        public ObjetCSA getParent() {
            return null;
        }

        @Override
        public void setParent(ObjetCSA objetDi) {

        }

        @Override
        public ObjetCSA getObjetFromType(String s) {
            return null;
        }

//        public int getPropertyAge() {
//            return (int) (new CalculateurAge(((LocalDate) propertyDate.get()).atStartOfDay())).getAge();
//        }
//
//        public void setPropertyAge(int propertyAge) {
//            this.propertyAge.set(propertyAge);
//        }
//
//        public IntegerProperty propertyAgeProperty() {
//            return propertyAge;
//        }
//
//        public String getPropertyNom() {
//            return propertyNom.get();
//        }
//
//        public void setPropertyNom(String propertyNom) {
//            this.propertyNom.set(propertyNom);
//        }
//
//        public StringProperty propertyNomProperty() {
//            return propertyNom;
//        }
//
//        public String getPropertyPrenom() {
//            return propertyPrenom.get();
//        }
//
//        public void setPropertyPrenom(String propertyPrenom) {
//            this.propertyPrenom.set(propertyPrenom);
//        }
//
//        public StringProperty propertyPrenomProperty() {
//            return propertyPrenom;
//        }
//
//        public String getPropertyCourriel() {
//            return propertyCourriel.get();
//        }
//
//        public void setPropertyCourriel(String propertyCourriel) {
//            this.propertyCourriel.set(propertyCourriel);
//        }
//
//        public StringProperty propertyCourrielProperty() {
//            return propertyCourriel;
//        }
//
//        public int getPropertyCP() {
//            return propertyCP.get();
//        }
//
//        public void setPropertyCP(int propertyCP) {
//            this.propertyCP.set(propertyCP);
//        }
//
//        public IntegerProperty propertyCPProperty() {
//            return propertyCP;
//        }
//
//        public Object getPropertyDate() {
//            return propertyDate.get();
//        }
//
//        public void setPropertyDate(Object propertyDate) {
//            if (propertyDate instanceof LocalDate) {
//                this.propertyDate.set(propertyDate);
//                propertyAge.setValue((new CalculateurAge(((LocalDate) propertyDate).atStartOfDay())).getAge());
//            }
//        }
//
//        public ObjectProperty propertyDateProperty() {
//            return propertyDate;
//        }
    }
}
