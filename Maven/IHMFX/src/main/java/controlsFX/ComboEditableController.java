package controlsFX;

import general.Monostable;
import general.ObjetCSA;
import generalFX.CustomEditor;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Contrôleur pour un ComboBox éditable avec fonctionnalités avancées de filtrage et sélection.
 * Permet la gestion de listes d'objets avec recherche, filtrage et sélection intelligente.
 *
 * @param <T> le type d'objet géré par ce contrôleur
 */
public class ComboEditableController<T> extends HBox
        implements Initializable {

    private static final int MAX_RESULTATS_COMBO = 50;
    private static final int LIMITE_AFFICHAGE_AUTO = 10;
    private static final boolean DEBUG_COMBO = true;

    private final TreeMap<String, Object> hashOriginale = new TreeMap<>();
    private final TreeMap<String, Object> hashComplete = new TreeMap<>();
    private final Monostable actionEnabler = new Monostable(true, false);
    private final Set<String> toStringDupliques = new HashSet<>();
    private final Object operationLock = new Object();
    // FIX #1: must be volatile — read/written from multiple threads
    private volatile boolean isProgrammaticChange = false;
    // Synchronization control
    private volatile boolean operationInProgress = false;
    // Listener inhibition control
    private volatile boolean listenersInhibited = false;

    @FXML
    private ComboBox<String> combo;
    @FXML
    private Button btnEffacer, btnAjouter, btnRafraichir;

    private Object selection = null;
    private Object pendingSelection = null;
    private Class<T> type;
    private java.util.function.Function<EditorPaneController, CustomEditor> editeurPersonnel = null;
    private Predicate<ObjetCSA> postCreateCallback;
    private int previousTextLength = 0;

    // Handlers fonctionnels (remplacent MaitreFX)
    /**
     * Contexte partagé (reçu du composant parent).
     */
    private ContexteFX contexte;

    /**
     * Handlers locaux — quand définis, ils priment sur le contexte.
     * Permet de surcharger un comportement spécifique sans remplacer tout le contexte.
     */
    private BiConsumer<Object, String> userActionHandler;
    private Consumer<String> messageHandler;
    private Consumer<String> consoleHandler;
    private Function<String, ObjetCSA> typeResolver;
    private ContexteFX.HandlerBD bdHandler;
    private Runnable refreshHandler;

    /**
     * Construit un ComboEditableController pour le type spécifié.
     *
     * @param type la classe du type géré par ce contrôleur (ne peut pas être null)
     */
    public ComboEditableController(Class<T> type) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ComboEditable.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.type = Objects.requireNonNull(type, "Type cannot be null");
    }

    private static String normalize(String s) {
        return Normalizer.normalize(s.trim().toUpperCase(), Normalizer.Form.NFD)
                .replaceAll("[\\u0300-\\u036F]", "");
    }

    private void log(String msg) {
        if (DEBUG_COMBO)
            System.out.printf("[ComboEditable %s] %s%n",
                    type != null ? type.getSimpleName() : "?", msg);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        btnEffacer.setTooltip(new Tooltip("Réinitialiser la sélection"));
        btnAjouter.setTooltip(new Tooltip("Créer un nouvel item"));
        btnRafraichir.setTooltip(new Tooltip("Rafraîchir la liste"));

        combo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (isProgrammaticChange || listenersInhibited) return;
            if (newVal == null || newVal.equals(oldVal)) return;
            if (!actionEnabler.isStable()) return;

            actionEnabler.go();
            selection = hashOriginale.get(newVal);
            BiConsumer<Object, String> uah = effectiveUserActionHandler();
            if (uah != null)
                uah.accept(selection, "choix combo effectué");
        });

        combo.setOnShowing(x -> {
            if (hashComplete.size() > MAX_RESULTATS_COMBO)
                combo.setPromptText(String.format("%d résultats — tapez pour filtrer", hashComplete.size()));
        });

        combo.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.length() > 25 ? item.substring(0, 22) + "..." : item);
                    setTooltip(new Tooltip(item));
                }
            }
        });

        combo.getEditor().textProperty().addListener((obs, ov, nv) ->
                log("EDITOR text: '" + ov + "' -> '" + nv + "'"));

        combo.getItems().addListener((javafx.collections.ListChangeListener<String>) c ->
                log("ITEMS changed, size=" + combo.getItems().size()));

        combo.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) combo.hide();
        });
    }

    /**
     * Retourne le type de classe géré par ce contrôleur.
     *
     * @return la classe du type T
     */
    public Class<T> getType() {
        return type;
    }

    /**
     * Définit le type de classe géré par ce contrôleur.
     *
     * @param type la nouvelle classe de type
     */
    public void setType(Class<T> type) {
        this.type = type;
    }


    /**
     * Retourne le texte actuellement saisi dans le combo.
     *
     * @return le texte du champ éditable
     */
    public String getTexte() {
        return combo.getEditor().getText();
    }

    /**
     * Définit le texte du combo (sans déclencher les écouteurs).
     *
     * @param texte le texte à afficher
     */
    public void setTexte(String texte) {
        // flag set INSIDE runLater
        Platform.runLater(() -> {
            isProgrammaticChange = true;
            try {
                combo.getEditor().setText(texte);
            } finally {
                isProgrammaticChange = false;
            }
        });
    }

    /**
     * Active ou désactive l'édition du texte dans le combo.
     *
     * @param editable true pour permettre l'édition, false sinon
     */
    public void setTextEditable(boolean editable) {
        Platform.runLater(() -> combo.getEditor().setEditable(editable));
    }

    /**
     * Retourne l'objet actuellement sélectionné.
     *
     * @return l'objet sélectionné, ou chaîne vide si null et type String
     */
    public Object getSelection() {
        return (selection == null && type == String.class) ? "" : selection;
    }

    /**
     * Retourne la liste originale.
     */
    public ArrayList<Object> getListeComplete() {
        return new ArrayList<>(hashOriginale.values());
    }

    // -------------------------------------------------------------------------
    // Unified Fill and Select API
    // -------------------------------------------------------------------------

    /**
     * Unified method to fill combo with data and select an item.
     * This method ensures thread safety and prevents side effects.
     *
     * @param liste            Collection of items to populate the combo
     * @param aSelectionner    Item to select after population (can be null)
     * @param inhibitListeners If true, all listeners will be inhibited during operation
     * @param fireMasterEvent  If true, fires master event after selection (only if listeners not inhibited)
     * @throws IllegalStateException if another operation is in progress
     */
    public void fillAndSelect(Collection<?> liste, Object aSelectionner, boolean inhibitListeners, boolean fireMasterEvent) {
        synchronized (operationLock) {
            if (operationInProgress) {
                throw new IllegalStateException("Another combo operation is already in progress. Please wait for completion.");
            }
            operationInProgress = true;
        }

        try {
            // Store listener state
            listenersInhibited = inhibitListeners;
            // Restore initial list and handle selection
            Object targetSelection = (aSelectionner == null) ? selection : aSelectionner;
            // Populate the complete hash
            populerHashComplete(liste);
            restaurer_liste_initiale_unified(targetSelection, !fireMasterEvent);

        } finally {
            operationInProgress = false;
            listenersInhibited = false;
        }
    }

    /**
     * Méthode pratique pour remplir et sélectionner sans lancer les écouteurs ni l'événement maître.
     *
     * @param liste         la collection d'éléments à afficher
     * @param aSelectionner l'élément à sélectionner (peut être null)
     */
    public void fillAndSelectSilent(Collection<?> liste, Object aSelectionner) {
        fillAndSelect(liste, aSelectionner, true, false);
    }

    /**
     * Méthode pratique pour remplir et sélectionner avec les écouteurs actifs et l'événement maître.
     *
     * @param liste         la collection d'éléments à afficher
     * @param aSelectionner l'élément à sélectionner (peut être null)
     */
    public void fillAndSelectWithEvent(Collection<?> liste, Object aSelectionner) {
        fillAndSelect(liste, aSelectionner, false, true);
    }

    // -------------------------------------------------------------------------
    // List population
    // -------------------------------------------------------------------------

    /**
     * Retourne l'indice de l'élément actuellement sélectionné.
     *
     * @return l'indice sélectionné, ou -1 si aucune sélection
     */
    public Integer getIndiceSelectionne() {
        return combo.getSelectionModel().getSelectedIndex();
    }

    /**
     * Permet d'insérer une liste sans sélectionner d'élément
     * ATTENTION : l'appel est silencieux, donc les écouteurs ne sont pas lancés
     * Mais si une sélection a été initiée (de manière asynchrone), elle est conservée
     *
     * @param liste La nouvelle liste à insérer
     */
    public void setListe(Collection<?> liste) {
        log("setListe : " + liste + " avec sélection=" + selection + " et pendigSelection=" + pendingSelection);
        fillAndSelectSilent(liste, pendingSelection != null ? pendingSelection : selection);
    }

    /**
     * Insère la nouvelle liste et effectue la sélection et lance les écouteurs
     *
     * @param aSelectionner l'élément de la liste à sélectionner après chargement
     * @param liste         la liste a insérer dans le combo
     */
    public void setListeWithSelection(Collection<?> liste, Object aSelectionner) {
        fillAndSelectWithEvent(liste, aSelectionner);
    }

    /**
     * Insère la nouvelle liste et effectue la sélection sans lancer les écouteurs
     *
     * @param aSelectionner l'élément de la liste à sélectionner après chargement
     * @param liste         la liste a insérer dans le combo
     */
    public void setListeWithSilentSelection(Collection<?> liste, Object aSelectionner) {
        fillAndSelectSilent(liste, aSelectionner);
    }


    // -------------------------------------------------------------------------
    // Selection
    // -------------------------------------------------------------------------

    /**
     * Insère la nouvelle liste et effectue la sélection et lance les écouteurs
     *
     * @param liste la liste a insérer dans le combo
     */
    public void setListeKeepSelection(Collection<?> liste) {
        log("setListeKeepSelection : " + liste + " avec sélection=" + selection + " et pendigSelection=" + pendingSelection);
        fillAndSelectWithEvent(liste, pendingSelection != null ? pendingSelection : selection);
    }

    /**
     * Sélectionne un élément et lance l'événement maître.
     *
     * @param nouveau l'élément à sélectionner
     * @return true si la sélection a réussi, false sinon
     */
    public boolean select(Object nouveau) {
        synchronized (operationLock) {
            if (operationInProgress) {
                throw new IllegalStateException("Another combo operation is already in progress. Please wait for selection of" +
                        nouveau);
            }
            operationInProgress = true;
        }

        try {
            return selectInternal(nouveau, false);
        } finally {
            operationInProgress = false;
        }
    }

    /**
     * Sélectionne un élément silencieusement (sans événement maître).
     *
     * @param nouveau l'élément à sélectionner
     */
    public void selectSilently(Object nouveau) {
        synchronized (operationLock) {
            if (operationInProgress) {
                throw new IllegalStateException("Another combo operation is already in progress. Please wait for selection of" +
                        nouveau);
            }
            operationInProgress = true;
        }

        try {
            selectInternal(nouveau, true);
        } finally {
            operationInProgress = false;
        }
    }

    /**
     * Flag is now set/reset INSIDE the Platform.runLater lambda in all paths.
     * pendingSelection is cleared when a subsequent setListe still can't find the object
     * (handled in restaurer_liste_initiale).
     * silent mode is enforced because the flag is active when the listener fires.
     */
    private boolean selectInternal(Object nouveau, boolean silent) {
        if (nouveau == null) {
            clearSelection();
            return false;
        }

        String clefToSelect = null;
        Object valeurTrouvee = null;

        if (nouveau instanceof String s) {
            if (hashOriginale.containsKey(s)) {
                clefToSelect = s;
                valeurTrouvee = hashOriginale.get(s);
            }
        } else {
            for (Map.Entry<String, Object> e : hashOriginale.entrySet()) {
                if (isEquivalent(nouveau, e.getValue())) {
                    clefToSelect = e.getKey();
                    valeurTrouvee = e.getValue();
                    break;
                }
            }
            if (clefToSelect == null) {
                for (Map.Entry<String, Object> e : hashComplete.entrySet()) {
                    if (isEquivalent(nouveau, e.getValue())) {
                        clefToSelect = e.getKey();
                        valeurTrouvee = e.getValue();
                        hashOriginale.putIfAbsent(clefToSelect, valeurTrouvee);
                        break;
                    }
                }
            }
        }

        if (clefToSelect == null) {
            // memorize the pending request; it will be retried in restaurer_liste_initiale
            pendingSelection = nouveau;
            log("Sélection différée mémorisée pour: " + nouveau);
            return false;
        }

        final String finalKey = clefToSelect;
        final Object finalVal = valeurTrouvee;

        //flag is set INSIDE the lambda — it is active when the listener fires
        Platform.runLater(() -> {
            isProgrammaticChange = true;
            try {
                if (!combo.getItems().contains(finalKey))
                    combo.getItems().setAll(hashOriginale.keySet());
                combo.getSelectionModel().select(finalKey);
                selection = finalVal;
                pendingSelection = null; // successfully applied
            } finally {
                isProgrammaticChange = false;
            }
            BiConsumer<Object, String> uah = effectiveUserActionHandler();
            if (!silent && uah != null && !listenersInhibited)
                uah.accept(selection, "sélection interne");
        });
        return true;
    }

    /**
     * Blocking select — safe to call from a non-FX thread.
     * If already on the FX thread, executes synchronously.
     *
     * @param nouveau       l'objet à sélectionner dans la liste
     * @param timeoutMillis temps à attendre pour que la liste se remplisse
     */
    public boolean selectAndWait(Object nouveau, long timeoutMillis) {
        Map.Entry<String, Object> entry = findEntryFor(nouveau);
        if (entry == null) {
            clearSelection();
            return false;
        }

        final String key = entry.getKey();
        final Object val = entry.getValue();

        if (Platform.isFxApplicationThread()) {
            isProgrammaticChange = true;
            try {
                if (!combo.getItems().contains(key)) combo.getItems().setAll(hashOriginale.keySet());
                combo.getSelectionModel().select(key);
                selection = val;
            } finally {
                isProgrammaticChange = false;
            }
            BiConsumer<Object, String> uah = effectiveUserActionHandler();
            if (uah != null && !listenersInhibited) {
                uah.accept(selection, "sélection sync");
            }
            return true;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean result = new AtomicBoolean(false);
        Platform.runLater(() -> {
            isProgrammaticChange = true;
            try {
                if (!combo.getItems().contains(key)) combo.getItems().setAll(hashOriginale.keySet());
                combo.getSelectionModel().select(key);
                selection = val;
                result.set(true);
            } finally {
                isProgrammaticChange = false;
                BiConsumer<Object, String> uah = effectiveUserActionHandler();
                if (uah != null && !listenersInhibited)
                    uah.accept(selection, "sélection sync");
                latch.countDown();
            }
        });

        try {
            return latch.await(timeoutMillis, TimeUnit.MILLISECONDS) && result.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Selects by ObjetCSA id+type match.
     */
    public boolean select_with_id(ObjetCSA elementBD) {
        for (String item : combo.getItems()) {
            Object obj = hashOriginale.get(item);
            if (obj instanceof ObjetCSA iter &&
                    iter.getId() == elementBD.getId() &&
                    iter.getType().equals(elementBD.getType())) {
                Platform.runLater(() -> {
                    isProgrammaticChange = true;
                    try {
                        combo.getSelectionModel().select(item);
                        selection = iter;
                    } finally {
                        isProgrammaticChange = false;
                    }
                });
                return true;
            }
        }
        clearSelection();
        return false;
    }

    /**
     * Sélectionne l'élément à l'indice spécifié.
     *
     * @param i l'indice de l'élément à sélectionner
     */
    public void selectIndex(int i) {
        Platform.runLater(() -> {
            isProgrammaticChange = true;
            try {
                combo.getSelectionModel().select(i);
            } finally {
                isProgrammaticChange = false;
            }
        });
    }

    /**
     * Efface la sélection courante.
     */
    public void clearSelection() {
        action_effacer(null);
    }

    // -------------------------------------------------------------------------
    // FXML actions
    // -------------------------------------------------------------------------

    /**
     * Vide la liste et efface la sélection.
     */
    public void clear() {
        setListe(new ArrayList<>());
    }

    /**
     * Action déclenchée par le bouton effacer.
     *
     * @param action l'événement déclencheur
     */
    @FXML
    public void action_effacer(ActionEvent action) {
        log("action_effacer()");
        hashOriginale.clear(); // rebuild before runLater
        int compteur = 0;
        for (Map.Entry<String, Object> entry : hashComplete.entrySet()) {
            if (compteur++ >= MAX_RESULTATS_COMBO) break;
            hashOriginale.put(entry.getKey(), entry.getValue());
        }
        Platform.runLater(() -> {
            isProgrammaticChange = true;
            try {
                combo.getEditor().clear();
                selection = null;
                pendingSelection = null;
                restaurerUI(); // ← direct call, no nested runLater
            } finally {
                isProgrammaticChange = false;
            }
        });
    }


    private void restaurerUI() {
        // must be called from FX thread, with isProgrammaticChange already = true
        combo.getItems().setAll(hashOriginale.keySet());
        combo.getSelectionModel().clearSelection();
        combo.setPromptText(hashComplete.size() > MAX_RESULTATS_COMBO
                ? String.format("Affichage %d/%d — Tapez pour filtrer", MAX_RESULTATS_COMBO, hashComplete.size())
                : (type == null ? "" : type.getSimpleName()));
    }

    private void populerHashComplete(Collection<?> liste) {
        hashComplete.clear();
        // Ne pas effacer `selection` ici : si deux appels setListeKeepSelection arrivent
        // rapidement, le second verrait selection=null et perdrait la cible de sélection.
        // La gestion de selection est déléguée à restaurer_liste_initiale_unified.
        toStringDupliques.clear();

        List<?> listeTrie = new ArrayList<>(liste);
        if (!listeTrie.isEmpty() && listeTrie.get(0) instanceof ObjetCSA)
            Collections.sort((List<ObjetCSA>) listeTrie);

        // Detect duplicate toString() values to disambiguate keys
        Map<String, Integer> compteurToString = new HashMap<>();
        for (Object elt : listeTrie) {
            if (type.isInstance(elt))
                compteurToString.merge(elt.toString(), 1, Integer::sum);
        }
        compteurToString.forEach((k, v) -> {
            if (v > 1) toStringDupliques.add(k);
        });

        for (Object elt : listeTrie) {
            if (type.isInstance(elt))
                hashComplete.put(genererCle(elt), elt);
            else
                affiche_console("Insertion non autorisée de " + elt + " dans " + this);
        }
    }

    /**
     * Action déclenchée par le bouton ajouter.
     *
     * @param action l'événement déclencheur
     */
    @FXML
    public void action_ajouter(ActionEvent action) {
        if (type == null) {
            affiche_utilisateur("Impossible d'ajouter un élément dans cette liste déroulante");
            return;
        }
        if (!ObjetCSA.class.isAssignableFrom(type)) {
            affiche_console("** " + this + " ne gère pas l'ajout de " + type);
            affiche_utilisateur("Impossible d'ajouter un objet de type <" + type + ">");
            return;
        }
        try {
            ObjetCSA instance = (ObjetCSA) type.getDeclaredConstructor().newInstance();
            // si le callback retourne false, on n'ouvre pas l'éditeur et on n'ajoute pas l'instance
            if (postCreateCallback != null &&
                    !postCreateCallback.test(instance)) {
                return;
            }
            charger_donnees_vers_enfant(instance);
        } catch (InstantiationException | IllegalAccessException |
                 NoSuchMethodException | InvocationTargetException e) {
            affiche_console("action_ajouter: impossible de créer " + type.getName());
            affiche_utilisateur("Impossible d'ajouter un élément dans cette liste déroulante");
        }
    }

    /**
     * Action déclenchée par le bouton rafraîchir.
     *
     * @param action l'événement déclencheur
     */
    @FXML
    public void action_rafraichir(ActionEvent action) {
        if (type == null) {
            affiche_utilisateur("Impossible de rafraîchir cette liste déroulante");
            return;
        }
        rafraichir_liste();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Filtre la liste en fonction du texte saisi.
     * actionEnabler.go() is only called when a real user selection occurred,
     * not on every keystroke. Filtering does not block the selection listener anymore.
     *
     * @param event l'événement clavier déclencheur
     */
    @FXML
    public void action_filtrer(KeyEvent event) {
        log("action_filtrer() event=" + (event != null ? event.getCode() : "null"));

        String texteTape = combo.getEditor().getText().trim();
        int currentLength = texteTape.length();
        boolean isBackspace = event != null && event.getCode() == KeyCode.BACK_SPACE;
        boolean isDelete = event != null && event.getCode() == KeyCode.DELETE;
        boolean isEnter = event != null && event.getCode() == KeyCode.ENTER;
        boolean isShrinking = currentLength < previousTextLength;
        previousTextLength = currentLength;

        String texte = normalize(texteTape);

        if (texte.isEmpty()) {
            restaurer_liste_initiale(pendingSelection, true);
            if (event != null) {
                combo.hide();
                Platform.runLater(combo::show);
            }
            return;
        }

        TreeMap<String, Object> inclus = new TreeMap<>();
        for (Map.Entry<String, Object> entry : hashComplete.entrySet()) {
            if (inclus.size() >= MAX_RESULTATS_COMBO) break;
            if (normalize(entry.getKey()).startsWith(texte))
                inclus.put(entry.getKey(), entry.getValue());
        }
        log("Résultats filtrés: " + inclus.size());

        hashOriginale.clear();
        hashOriginale.putAll(inclus);

        boolean doAutocomplete = !isBackspace && !isDelete && !isShrinking;

        Platform.runLater(() -> {
            isProgrammaticChange = true;
            try {
                combo.getItems().setAll(inclus.keySet());
                combo.getSelectionModel().clearSelection();
                selection = null;

                if (doAutocomplete && !inclus.isEmpty()) {
                    String premier = inclus.firstKey();
                    if (premier.length() >= texteTape.length()) {
                        combo.getEditor().setText(premier);
                        combo.getEditor().selectRange(texteTape.length(), premier.length());
                    }
                } else {
                    combo.getEditor().setText(texteTape);
                    combo.getEditor().positionCaret(texteTape.length());
                }

                if (!inclus.isEmpty() && inclus.size() <= LIMITE_AFFICHAGE_AUTO && !isEnter) {
                    combo.hide();
                    Platform.runLater(combo::show);
                }

                if (isEnter && !inclus.isEmpty()) {
                    Map.Entry<String, Object> first = inclus.firstEntry();
                    selection = first.getValue();
                    combo.getSelectionModel().select(first.getKey());
                    combo.hide();
                }
            } finally {
                isProgrammaticChange = false;
            }
            // Fire parent notification AFTER flag is cleared, outside try/finally
            BiConsumer<Object, String> uah = effectiveUserActionHandler();
            if (isEnter && !inclus.isEmpty() && uah != null && !listenersInhibited)
                uah.accept(selection, String.valueOf(event));
        });
    }

    private void restaurer_liste_initiale_unified(Object aSelectionner, boolean silent) {
        hashOriginale.clear();
        int compteur = 0;
        for (Map.Entry<String, Object> entry : hashComplete.entrySet()) {
            if (compteur++ >= MAX_RESULTATS_COMBO) break;
            hashOriginale.put(entry.getKey(), entry.getValue());
        }
        Platform.runLater(() -> {
            isProgrammaticChange = true;
            try {
                restaurerUI();
                if (aSelectionner != null) {
                    Map.Entry<String, Object> entry = findEntryFor(aSelectionner);
                    if (entry != null) {
                        combo.getSelectionModel().select(entry.getKey());
                        selection = entry.getValue();
                        pendingSelection = null;
                    } else {
                        // L'objet à sélectionner n'est pas dans la nouvelle liste :
                        // effacer la sélection périmée et mémoriser pour réessai
                        selection = null;
                        pendingSelection = aSelectionner;
                    }
                } else {
                    // Aucune cible : effacer toute sélection résiduelle
                    selection = null;
                }
            } finally {
                isProgrammaticChange = false;
            }
            // Fire master event only if not silent and listeners are not inhibited
            BiConsumer<Object, String> uah = effectiveUserActionHandler();
            if (!silent && aSelectionner != null && selection != null && uah != null && !listenersInhibited)
                uah.accept(selection, "sélection unifiée");
        });
    }

    private void restaurer_liste_initiale(Object aSelectionner, boolean silent) {
        hashOriginale.clear();
        int compteur = 0;
        for (Map.Entry<String, Object> entry : hashComplete.entrySet()) {
            if (compteur++ >= MAX_RESULTATS_COMBO) break;
            hashOriginale.put(entry.getKey(), entry.getValue());
        }
        Platform.runLater(() -> {
            isProgrammaticChange = true;
            try {
                restaurerUI();
                if (aSelectionner != null) {
                    Map.Entry<String, Object> entry = findEntryFor(aSelectionner);
                    if (entry != null) {
                        combo.getSelectionModel().select(entry.getKey());
                        selection = entry.getValue();
                        pendingSelection = null;
                    } else {
                        selection = null;
                        pendingSelection = aSelectionner;
                    }
                } else {
                    selection = null;
                }
            } finally {
                isProgrammaticChange = false;
            }
            if (!silent && aSelectionner != null && selection != null && contexte != null && !listenersInhibited)
                contexte.getUserActionHandler().accept(selection, "sélection différée");
        });
    }

    private Map.Entry<String, Object> findEntryFor(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String key) {
            Object val = hashOriginale.get(key);
            return val != null ? new AbstractMap.SimpleEntry<>(key, val) : null;
        }
        for (Map.Entry<String, Object> e : hashOriginale.entrySet())
            if (isEquivalent(obj, e.getValue()))
                return new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue());
        for (Map.Entry<String, Object> e : hashComplete.entrySet())
            if (isEquivalent(obj, e.getValue())) {
                hashOriginale.putIfAbsent(e.getKey(), e.getValue());
                return new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue());
            }
        return null;
    }

    /**
     * Détermine si selon moi, les objets sont équivalents
     *
     * @param a c'est le nouveau
     * @param b c'est l'ancien
     * @return vrai si a égal b (et si l'id de a est nul, alors on compare les noms)
     */
    private boolean isEquivalent(Object a, Object b) {
        if (a == null || b == null) return false;
        if (a.equals(b)) return true;
        if (a instanceof ObjetCSA ca && b instanceof ObjetCSA cb) {
            if (!ca.getType().equals(cb.getType())) return false;
            return (ca.getId() > 0) ? ca.getId() == cb.getId()
                    : ca.toString().equals(cb.toString());
        }
        return false;
    }

    private String genererCle(Object elt) {
        String s = elt.toString();
        if (elt instanceof ObjetCSA obj && obj.getId() > 0 && toStringDupliques.contains(s))
            return s + " [#" + obj.getId() + "]";
        return s;
    }

    // -------------------------------------------------------------------------
    // Misc public methods
    // -------------------------------------------------------------------------

    public void fireModification() {
        BiConsumer<Object, String> uah = effectiveUserActionHandler();
        if (uah != null && !listenersInhibited)
            uah.accept(selection, "modification du comboEditable");
        else
            System.err.println("** fireModification: pas de userActionHandler");
    }

    public void setPostCreateCallback(Predicate<ObjetCSA> callback) {
        this.postCreateCallback = callback;
    }

    public void enlever_bouton_ajout() {
        getChildren().remove(btnAjouter);
    }

    public void enlever_bouton_refresh() {
        getChildren().remove(btnRafraichir);
    }

    public void setEditeurPersonnel(java.util.function.Function<EditorPaneController, CustomEditor> factory) {
        editeurPersonnel = factory;
    }

    public ObjetCSA getObjetReference() {
        try {
            return (ObjetCSA) type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            affiche_console("Impossible de créer une instance de " + type);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Handlers (approche moderne, remplaçant MaitreFX)
    // -------------------------------------------------------------------------

    // -- Résolveurs effectifs : handler local en priorité, sinon contexte --

    private BiConsumer<Object, String> effectiveUserActionHandler() {
        if (userActionHandler != null) return userActionHandler;
        return (contexte != null) ? contexte.getUserActionHandler() : null;
    }

    private Consumer<String> effectiveMessageHandler() {
        if (messageHandler != null) return messageHandler;
        return (contexte != null) ? contexte.getMessageHandler() : null;
    }

    private Consumer<String> effectiveConsoleHandler() {
        if (consoleHandler != null) return consoleHandler;
        return (contexte != null) ? contexte.getConsoleHandler() : null;
    }

    private Function<String, ObjetCSA> effectiveTypeResolver() {
        if (typeResolver != null) return typeResolver;
        return (contexte != null) ? contexte.getTypeResolver() : null;
    }

    private ContexteFX.HandlerBD effectiveBdHandler() {
        if (bdHandler != null) return bdHandler;
        return (contexte != null) ? contexte.getBdHandler() : null;
    }

    private Runnable effectiveRefreshHandler() {
        return refreshHandler; // handler local uniquement (ContexteFX ne le fournit pas)
    }

    /**
     * Configure tous les handlers en une seule fois depuis un {@link ContexteFX}.
     * Remplace les appels individuels à {@code setMessageHandler}, {@code setTypeResolver}, etc.
     * Les handlers locaux définis via les setters individuels <b>priment</b> sur le contexte.
     *
     * @param ctx le contexte à appliquer
     */
    public void setContexte(ContexteFX ctx) {
        this.contexte = ctx;
    }

    /**
     * Surcharge le {@code userActionHandler} du contexte.
     * Appelé lors de chaque sélection, filtrage ou modification par l'utilisateur.
     */
    public void setUserActionHandler(BiConsumer<Object, String> handler) {
        this.userActionHandler = handler;
    }

    /**
     * Surcharge le {@code messageHandler} du contexte.
     * Appelé par {@link #affiche_utilisateur(String)}.
     */
    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    /**
     * Surcharge le {@code consoleHandler} du contexte.
     * Appelé par {@link #affiche_console(String)}.
     */
    public void setConsoleHandler(Consumer<String> handler) {
        this.consoleHandler = handler;
    }

    /**
     * Surcharge le {@code typeResolver} du contexte.
     * Appelé par {@link #getElementFromType(String)}.
     */
    public void setTypeResolver(Function<String, ObjetCSA> resolver) {
        this.typeResolver = resolver;
    }

    /**
     * Surcharge le {@code bdHandler} du contexte.
     * Utilisé pour rafraîchir la liste et créer/modifier un objet dans l'éditeur.
     */
    public void setBdHandler(ContexteFX.HandlerBD handler) {
        this.bdHandler = handler;
    }

    /**
     * Surcharge le {@code refreshHandler} du contexte.
     * Appelé après une opération BD réussie (ajout/modif/suppression) pour notifier le parent.
     */
    public void setRefreshHandler(Runnable handler) {
        this.refreshHandler = handler;
    }

    /**
     * Compatibilité descendante : injecte automatiquement tous les handlers à partir d'un MaitreFX.
     * Préférer les setters individuels dans le nouveau code.
     *
     * @deprecated Utiliser les handlers individuels pour découpler du MaitreFX.
     */
    @Deprecated
    public void setMaitre(MaitreFX maitre) {
        if (maitre == null) return;
        contexte = new ContexteFX().withMessageHandler(maitre::affiche_utilisateur)
                .withConsoleHandler(maitre::affiche_console)
                .withTypeResolver(maitre::getElementFromType)
                .withUserActionHandler((obj, info) -> maitre.action_utilisateur(obj, this, info))
                .withBdHandler((code, obj, callback) -> {
                    maitre.appeler_bd(code, obj, new generalFX.GestionnaireListesFX(this));
                });
    }

    // -------------------------------------------------------------------------
    // Méthodes utilitaires (anciennement via MaitreFX)
    // -------------------------------------------------------------------------

    public void affiche_utilisateur(String texte) {
        Consumer<String> mh = effectiveMessageHandler();
        if (mh != null)
            mh.accept(texte);
        else
            System.out.println("* comboEditable: " + texte);
    }

    public void affiche_console(String texte) {
        Consumer<String> ch = effectiveConsoleHandler();
        if (ch != null)
            ch.accept("comboEditable: " + texte);
        else
            System.err.println("** comboEditable: " + texte);
    }

    public ObjetCSA getElementFromType(String t) {
        Function<String, ObjetCSA> tr = effectiveTypeResolver();
        if (tr != null) {
            return tr.apply(t);
        }
        System.err.println("** comboEditable: typeResolver non défini pour getElementFromType(" + t + ")");
        return null;
    }

    public void nettoyer_champs() {
        action_filtrer(null);
    }

    public void rafraichir() {
        action_filtrer(null);
    }

    public void charger_donnees_vers_enfant(ObjetCSA objetCSA) {
        ObjetCSA ref = getObjetReference();
        if (ref == null) {
            affiche_utilisateur("Aucune possibilité de gérer les éléments ici");
            return;
        }
        EditorPaneController editor = new EditorPaneController();
        if (editeurPersonnel != null) editor.setCustomEditor(editeurPersonnel);

        if (contexte != null) editor.setContexte(contexte);
        editor.setParentNotifier((code, obj) -> {
            pendingSelection = obj;
            if (code == 1 && obj.getId() > 0) {
                // Ajout d'un nouvel élément
                ajouter_element_direct(obj);
            } else if (code == 2) {
                // Modification d'un élément existant
                // On rafraîchit la liste et on tente de resélectionner l'objet modifié
                rafraichir_liste();
            } else {
                // Autres cas : afficher un message et rafraîchir la liste
                affiche_console("Code " + code + " inattendu dans le callback retour de ComboEditable/charger_donnees_vers_enfant. Objet: " + obj);
                rafraichir_liste();
            }
        });
        editor.charger_donnees(ref.getType(), objetCSA);
        editor.show();
    }

    /**
     * Demande un rafraîchissement de la liste via le refreshHandler.
     */
    public void rafraichir_liste() {
        ContexteFX.HandlerBD bh = effectiveBdHandler();
        if (bh != null && getObjetReference() != null) {
            bh.execute(4, getObjetReference(), liste -> {
                if (liste.size() > 2 && liste.get(0) instanceof Integer code &&
                        liste.get(2) instanceof ArrayList<?> listeRecue) {
                    if (code == 4) {
                        setListeKeepSelection(listeRecue);
                        return;
                    }
                    affiche_console("Code " + code + " inattendu dans le callback retour de ComboEditable/rafraichir_liste");
                    return;
                }
                affiche_console("Retour de callback rafraichir_liste de comboEditable inattendu : " + liste);
            });
            return;
        }
        affiche_console("Pas de bdHandler ni de contexte pour rafraichir_liste()");
    }

    /**
     * Ajoute directement un élément au combo sans rechargement depuis la BD.
     * L'élément doit posséder un ID valide (setId() déjà appelé par EditorPaneController).
     *
     * @param obj l'objet à ajouter (avec son ID final)
     */
    public void ajouter_element_direct(ObjetCSA obj) {
        if (!type.isInstance(obj)) return;
        // Reconstruire la liste complète avec le nouvel élément et sélectionner
        ArrayList<Object> nouvelleListe = new ArrayList<>(hashComplete.values());
        nouvelleListe.add(obj);
        setListeWithSelection(nouvelleListe, obj);
    }

}