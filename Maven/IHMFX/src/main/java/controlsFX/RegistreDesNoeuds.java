package controlsFX;

import general.ObjetCSA;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class RegistreDesNoeuds {
    public static final RegistreDesNoeuds instance = new RegistreDesNoeuds();
    // UN SEUL registre : chemin → (node + callbacks)
    private final Map<String, NodeEntry> registry = new HashMap<>();
    // Configurations globales par type d'objet (inchangé)
    private final Map<String, Map<String, Function<ObjetCSA, Node>>> nodeFactories = new HashMap<>();

    public static RegistreDesNoeuds getInstance() {
        return instance;
    }

    /**
     * Permet d'associer une série d'instructions à exécuter à un node
     * C'est l'usine à noeuds. À n'utiliser que si les valeurs par défaut de
     * l'éditorPane sont autres ou s'il faut rajouter un code spécifique au chargement
     *
     * @param typeObjet Le type d'objetCSA manipulé
     * @param nomChamp  Le nom du champ concerné
     * @param factory   la fonction à lui attribuer
     */
    public void setNodeFactory(String typeObjet, String nomChamp, Function<ObjetCSA, Node> factory) {
        nodeFactories.computeIfAbsent(typeObjet, k -> new HashMap<>())
                .put(nomChamp, factory);
    }

    /**
     * Indique si il existe déjà un node associé à un objet
     *
     * @param typeObjet le type de l'objetCSA
     * @param nomChamp  le nom du champ auquel il est associé
     * @param objetCSA  l'objetCSA qu'on veut lier à un champ
     * @return le node préalablement existant (inscrit) ou null
     */
    public Node getCustomNode(String typeObjet, String nomChamp, ObjetCSA objetCSA) {
        if (nodeFactories.containsKey(typeObjet)) {
            Map<String, Function<ObjetCSA, Node>> factories = nodeFactories.get(typeObjet);
            if (factories.containsKey(nomChamp)) {
                return factories.get(nomChamp).apply(objetCSA);
            }
        }
        return null;
    }

    /**
     * Enregistrer un node avec son chemin complet
     *
     * @param fullPath le chemin complet qui permettra de le retrouver
     * @param node     Le noeud à enregistrer
     */
    public void register(String fullPath, Node node) {
        registry.computeIfAbsent(fullPath, k -> new NodeEntry(node));
    }

    /**
     * Récupérer un node par son chemin
     *
     * @param fullPath le chemin complet du noeud qu'on cherche
     * @return Le noeud enregistré
     */
    public Node getNode(String fullPath) {
        NodeEntry entry = registry.get(fullPath);
        return entry != null ? entry.node : null;
    }

    /**
     * Récupérer un node par son chemin sans cast. Tout ce qui étend Node est bon
     *
     * @param fullPath le chemin complet du noeud qu'on cherche
     * @param <T>      Un composant graphique
     * @return un héritier de Node
     */
    @SuppressWarnings("unchecked")
    public <T extends Node> T obtenir_node(String fullPath) {
        return (T) getNode(fullPath);
    }

    /**
     * S'abonner aux changements d'un champ spécifique
     * Le node DOIT déjà être enregistré
     *
     * @param fieldName le nom du champ associé à l'action à lui appliquer
     * @param callback  l'action à appliquer lors de l'appel de publish
     */
    public void subscribe(String fieldName, Consumer<Object> callback) {
        NodeEntry entry = registry.get(fieldName);
        if (entry == null) {
            System.err.println("⚠️ Tentative d'abonnement sur '" + fieldName + "' qui n'existe pas dans le registre !\n" +
                    "Donc d'abord faire un register ensuite un subscribe");
            // On peut choisir de créer l'entrée quand même ou de refuser
            entry = new NodeEntry(null); // Node sera ajouté plus tard via register()
            registry.put(fieldName, entry);
        }
        entry.callbacks.add(callback);
    }


    /**
     * Publier une donnée vers tous les abonnés avec ce fieldName exact
     *
     * @param fieldName le nom du champ associé à l'action à lui appliquer
     * @param data      l'argument de l'action à appliquer lors de l'exécution de la fonction enregistrée
     */
    public void publish(String fieldName, Object data) {
        NodeEntry entry = registry.get(fieldName);
        if (entry != null && !entry.callbacks.isEmpty()) {
            entry.callbacks.forEach(callback -> callback.accept(data));
        }
    }

    /**
     * Publier vers tous les abonnés dont les clés se terminent par fieldName
     *
     * @param fieldName le suffixe du nom de champ associé à l'action à lui appliquer
     * @param data      l'argument de l'action à appliquer lors de l'exécution de la fonction enregistrée
     */
    public void publishAll(String fieldName, Object data) {
        String suffix = "." + fieldName;
        registry.entrySet().stream()
                .filter(e -> e.getKey().endsWith(suffix) || e.getKey().equals(fieldName))
                .forEach(e -> e.getValue().callbacks.forEach(cb -> cb.accept(data)));
    }

    /**
     * Publier vers tous les abonnés dont la clé commence par le préfixe
     *
     * @param prefix le préfixe du nom de champ associé à l'action à lui appliquer
     * @param data   l'argument de l'action à appliquer lors de l'exécution de la fonction enregistrée
     */
    public void publishWithPrefix(String prefix, Object data) {
        registry.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .forEach(e -> e.getValue().callbacks.forEach(cb -> cb.accept(data)));
    }

    /**
     * Nettoyer le registre (à appeler quand un EditorPane se ferme)
     *
     * @param prefix le préfixe du champ à retirer ainsi que tous ses descendants
     */
    public void unregisterPrefix(String prefix) {
        registry.keySet().removeIf(key -> key.startsWith(prefix));
    }

    /**
     * Afficher l'état du registre (debug)
     */
    public void printRegistry() {
        System.out.println("=== REGISTRE DES NOEUDS ===");
        registry.forEach((path, entry) -> {
            System.out.println(path + " → " +
                    (entry.node != null ? entry.node.getClass().getSimpleName() : "null") +
                    " (" + entry.callbacks.size() + " callbacks)");
        });
    }

    // ========== MÉTHODES DE DEBUG (bonus) ==========

    /**
     * Vérifier si un node a des callbacks
     *
     * @param fullPath le chemin à vérifier
     * @return Vrai s'il existe des callbacks pour ce nom de chemin
     */
    public boolean hasCallbacks(String fullPath) {
        NodeEntry entry = registry.get(fullPath);
        return entry != null && !entry.callbacks.isEmpty();
    }

    /**
     * Obtenir le nombre de callbacks pour un node
     *
     * @param fullPath le chemin à vérifier
     * @return le nombre trouvé
     */
    public int getCallbackCount(String fullPath) {
        NodeEntry entry = registry.get(fullPath);
        return entry != null ? entry.callbacks.size() : 0;
    }

    // Classe interne pour encapsuler node + callbacks
    private static class NodeEntry {
        final Node node;
        final List<Consumer<Object>> callbacks = new ArrayList<>();

        NodeEntry(Node node) {
            this.node = node;
        }
    }
}