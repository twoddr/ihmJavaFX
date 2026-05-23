package controlsFX;

import general.ObjetCSA;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Regroupe tous les handlers fonctionnels utilisés par les composants IHMFX
 * ({@link ListTableBDController}, {@link EditorPaneController}, {@link ComboEditableController}).
 * <p>
 * Permet de configurer une hiérarchie de composants imbriqués en une seule ligne
 * plutôt que d'appeler plusieurs {@code setXxxHandler()} à chaque niveau.
 * Le contexte se propage automatiquement vers les enfants lors de l'ouverture
 * d'un éditeur ou d'une liste.
 * <p>
 * <b>Codes opération BD :</b>
 * <ul>
 *   <li>{@code 1} — ajout (création)</li>
 *   <li>{@code 2} — modification</li>
 *   <li>{@code 3} — suppression</li>
 *   <li>{@code 4} — consultation (détail si {@code objet.id > 0}, liste sinon)</li>
 * </ul>
 * <p>
 * <b>Exemple d'utilisation :</b>
 * <pre>{@code
 * ContexteFX ctx = new ContexteFX()
 *     .withBdHandler((code, objet, callback) -> switch (code) {
 *         case 1, 2 -> service.save(objet, callback);
 *         case 3    -> service.delete(objet, callback);
 *         case 4    -> objet != null && objet.getId() > 0
 *                          ? service.loadDetails(objet, callback)
 *                          : service.loadList(objet, callback);
 *         default   -> callback.accept(null);
 *     })
 *     .withTypeResolver(type -> ObjetFactory.create(type))
 *     .withMessageHandler(statusBar::setText);
 *
 * listTable.setContexte(ctx);
 * }</pre>
 * <p>
 * Le retour BD est toujours de la forme {@code [code, nomTable, data]} :
 * <ul>
 *   <li>{@code list.get(0)} — code entier (positif = succès)</li>
 *   <li>{@code list.get(1)} — nom de la table</li>
 *   <li>{@code list.get(2)} — données (ObjetCSA pour détail, ArrayList pour liste)</li>
 * </ul>
 * <p>
 * <b>Handlers propagés automatiquement :</b>
 * <ul>
 *   <li>{@code bdHandler} — handler BD unifié (codes 1 à 4)</li>
 *   <li>{@code typeResolver} — résolution de type par nom de table</li>
 *   <li>{@code messageHandler} — affichage de messages utilisateur</li>
 *   <li>{@code consoleHandler} — affichage de messages console</li>
 *   <li>{@code userActionHandler} — notification d'actions utilisateur</li>
 * </ul>
 * <p>
 * Note : {@code parentNotifier} n'est pas dans le contexte car il est spécifique
 * à chaque niveau de la hiérarchie (chaque composant génère sa propre notification).
 */
public class ContexteFX {

    // -------------------------------------------------------------------------
    // Types internes
    // -------------------------------------------------------------------------

    /**
     * Handler BD unifié (remplace les 4 handlers individuels).
     */
    private HandlerBD bdHandler;
    private Function<String, ObjetCSA> typeResolver;

    // -------------------------------------------------------------------------
    // Champs
    // -------------------------------------------------------------------------
    private Consumer<String> messageHandler;
    private Consumer<String> consoleHandler;
    private BiConsumer<Object, String> userActionHandler;

    /**
     * Handler BD unifié gérant SAVE, DELETE, REFRESH_DETAILS et REFRESH_LIST.
     * La réponse est toujours {@code [code, nomTable, data]}.
     */
    public ContexteFX withBdHandler(HandlerBD handler) {
        this.bdHandler = handler;
        return this;
    }

    /**
     * Résolveur de type : à partir d'un nom de table, retourne une instance de l'ObjetCSA correspondant.
     */
    public ContexteFX withTypeResolver(Function<String, ObjetCSA> resolver) {
        this.typeResolver = resolver;
        return this;
    }

    // -------------------------------------------------------------------------
    // Fluent setters (withXxx)
    // -------------------------------------------------------------------------

    /**
     * Handler d'affichage de messages utilisateur (status bar, label, etc.).
     */
    public ContexteFX withMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
        return this;
    }

    /**
     * Handler d'affichage de messages console/debug.
     */
    public ContexteFX withConsoleHandler(Consumer<String> handler) {
        this.consoleHandler = handler;
        return this;
    }

    /**
     * Handler de notification d'actions utilisateur (sélection, double-clic, etc.).
     */
    public ContexteFX withUserActionHandler(BiConsumer<Object, String> handler) {
        this.userActionHandler = handler;
        return this;
    }

    public HandlerBD getBdHandler() {
        return bdHandler;
    }

    public Function<String, ObjetCSA> getTypeResolver() {
        return typeResolver;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Consumer<String> getMessageHandler() {
        return messageHandler;
    }

    public Consumer<String> getConsoleHandler() {
        return consoleHandler;
    }

    public BiConsumer<Object, String> getUserActionHandler() {
        return userActionHandler;
    }

    /**
     * Handler BD unifié.
     * <p>
     * Codes : 1=ajout, 2=modification, 3=suppression, 4=consultation
     * (détail si {@code objet.id > 0}, liste sinon).
     * <p>
     * La réponse transmise au callback est toujours de la forme :
     * {@code [code (int), nomTable (String), data (ObjetCSA ou ArrayList)]}.
     */
    @FunctionalInterface
    public interface HandlerBD {
        /**
         * @param code     1=ajout, 2=modification, 3=suppression, 4=consultation
         * @param objet    objet concerné (peut être null pour une liste)
         * @param callback consumer qui recevra le retour BD sous forme {@code List<Object>}
         */
        void execute(int code, ObjetCSA objet, Consumer<List<Object>> callback);
    }
}
