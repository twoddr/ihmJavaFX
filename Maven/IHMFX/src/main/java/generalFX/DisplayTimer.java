package generalFX;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import static java.lang.Thread.sleep;

public class DisplayTimer implements Serializable {

    private final ArrayList<AfficheurTemporise> listeThreads = new ArrayList<>();
    private Integer delaiMin = 1, delaiMax = 5;
    private Node fxComponent;
    private GestionnaireJeton gestionnaireJeton;


    public DisplayTimer(int min, int max) {
        if (min > 0 && max > min) {
            setDelai(min, max);
        } else {
            System.err.println("*** DisplayTimer : J'attends une durée max supérieur à la durée min ; " +
                    "pourtant je reçois min = " + min + " et max = " + max + "\n Demande ignorée !");
        }
    }


    public void setTextSize(double taille) {
        Platform.runLater(() -> {
            if (fxComponent instanceof Label) {
                ((Label) fxComponent).setFont(new Font(taille));
            } else if (fxComponent instanceof TextField) {
                ((TextField) fxComponent).setFont(new Font(taille));
            } else if (fxComponent instanceof TextArea) {
                ((TextArea) fxComponent).setFont(new Font(taille));
            }
        });
    }

    public void setDelai(int min, int max) {
        int m = Math.abs(min);
        int M = Math.abs(max);
        if (m < M) {
            delaiMax = M;
            delaiMin = m;
        } else {
            delaiMax = m;
            delaiMin = M;
        }
    }

    public void display(String input) {
        listeThreads.add(new AfficheurTemporise(input));
        if (gestionnaireJeton == null || !gestionnaireJeton.isAlive()) {
            gestionnaireJeton = new GestionnaireJeton();
            gestionnaireJeton.start();
        }
    }

    /**
     * Permet d'écraser tous les messages en attente et afficher en permanence le texte
     *
     * @param input le texte important à afficher
     */
    public void displayPermanent(String input) {
        afficher(input);
    }

    private void afficher(String input) {
        if (fxComponent instanceof Label) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    ((Label) fxComponent).setText(input);
                }
            });
            return;
        }
        if (fxComponent instanceof TextField) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    ((TextField) fxComponent).setText(input);
                }
            });
            return;
        }
        if (fxComponent instanceof TextArea) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    ((TextArea) fxComponent).setText(input);
                }
            });
            return;
        }
        System.err.println("** DisplayTimer/affichageGeneral : " +
                "je n'ai pas reçu de composant pour afficher mes commentaires");

    }


    /**
     * Fournit le texte affiché actuellement
     *
     * @return un blanc si le composant n'existe pas, sinon, son contenu
     */
    public String getOutputText() {
        if (fxComponent instanceof Label) {
            return ((Label) fxComponent).getText();
        }
        if (fxComponent instanceof TextField) {
            return ((TextField) fxComponent).getText();
        }
        if (fxComponent instanceof TextArea) {
            return ((TextArea) fxComponent).getText();
        }

        return "";
    }

    public void assignerComposant(Object o) {
        if (o == null) {
            fxComponent = null;
        } else {
            if (o instanceof Node) {
                fxComponent = (Node) o;
            } else {
                fxComponent = null;
            }
        }
    }

    /*
     **************** classes internes ********************
     */
    private class GestionnaireJeton extends Thread {

        @Override
        public void run() {
            while (!listeThreads.isEmpty()) {
                AfficheurTemporise thread = listeThreads.remove(0);
                CountDownLatch poireauteur = new CountDownLatch(1);
                if (listeThreads.size() > 1) {
                    thread.setDuree(delaiMin);
                }
                try {
                    thread.setJeton(poireauteur);
                    thread.run();
                    poireauteur.await();
                } catch (NullPointerException ignored) {
                } catch (InterruptedException e) {
                    System.err.println("*** DisplayTimer/GestionnaireJeton : la méthode await " +
                            "a échoué !");
                }
            }
        }
    }

    private class AfficheurTemporise implements Runnable {
        private String message;
        private int delai = delaiMax;
        private CountDownLatch jeton = null;

        public AfficheurTemporise(String input) {
            message = input;
        }

        @Override
        public void run() {
            afficher(message);
            try {
                sleep(1000L * delai);
            } catch (InterruptedException ignored) {
            } finally {
                afficher("");
                if (jeton != null) {
                    jeton.countDown();
                }
            }
        }

        public void setDuree(int duree) {
            delai = duree;
        }

        public void setJeton(CountDownLatch poireauteur) {
            jeton = poireauteur;
        }
    }
}
