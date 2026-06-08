package fr.miuby.survi.display;

import com.google.common.collect.ImmutableMultimap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.system.log.ELogTag;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Charge et fournit des textures Minecraft signées par Mojang pour les faux joueurs du tab.
 *
 * <h3>Pourquoi les signatures sont obligatoires en online mode ?</h3>
 * En online mode, le client Minecraft valide la signature RSA de chaque propriété
 * {@code textures}. Sans signature ({@code signature=null}), le client ignore la texture
 * silencieusement et affiche Steve/Alex selon le hash de l'UUID.
 * C'est exactement ce que montrent les logs : {@code signature=null} sur toutes les entrées.
 *
 * <h3>Comment obtenir une signature valide ?</h3>
 * La signature doit être émise par Mojang. La seule source est leur session server :
 * <pre>
 *   GET https://sessionserver.mojang.com/session/minecraft/profile/{UUID}?unsigned=false
 * </pre>
 * {@code ?unsigned=false} est obligatoire — sans lui, Mojang omet le champ {@code signature}.
 *
 * <h3>Comment trouver l'UUID d'un skin ?</h3>
 * minecraft-heads.com → choisir une tête → onglet "Other" → champ "UUID".
 * Mettre cet UUID dans les constantes ci-dessous et appeler {@link #load} au démarrage.
 */
public final class TabSkins {

    // ─────────────────────────────────────────────────────────────────────────
    // UUIDs sources
    // Trouver sur minecraft-heads.com → onglet "Other" → champ "UUID"
    // ─────────────────────────────────────────────────────────────────────────

    /** UUID du compte Minecraft dont le skin est la tête grise (séparateurs du tab). */
    private static final String GRAY_UUID = "3202e327-87fd-4b6b-9beb-d0a926077b6d";

    /** UUID du compte MHF_Villager (ou autre compte avec le skin villageois voulu). */
    private static final String BLUE_UUID = "545d2f32-e796-4554-a2d1-d6a6b3e5f619";

    // ─────────────────────────────────────────────────────────────────────────
    // Cache des Property signées — chargées async dans load()
    // ─────────────────────────────────────────────────────────────────────────

    private static volatile Property GRAY_PROP     = null;
    private static volatile Property BLUE_PROP = null;

    /**
     * Textures des métiers. Alimenter dans {@link #load} :
     * <pre>
     *   JOB_PROPS.put(EJob.MINER,      fetchSigned("UUID_MINER",      "miner"));
     *   JOB_PROPS.put(EJob.LUMBERJACK, fetchSigned("UUID_LUMBERJACK", "lumberjack"));
     * </pre>
     * Les métiers absents utilisent {@link #gray()} comme fallback (voir TabInfoColumn).
     */
    static final Map<EJob, Property> JOB_PROPS = new EnumMap<>(EJob.class);

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private TabSkins() {}

    // ─────────────────────────────────────────────────────────────────────────
    // API package-private
    // ─────────────────────────────────────────────────────────────────────────

    /** Property signée pour la tête grise. Peut être null si UUID non encore configuré. */
    static Property gray()     { return GRAY_PROP; }

    /** Property signée pour la tête de villageois. Peut être null si UUID non encore configuré. */
    static Property blue() { return BLUE_PROP; }

    /**
     * Charge toutes les textures signées depuis le session server Mojang (async).
     * <b>À appeler une seule fois dans {@code JavaPlugin#onEnable()}.</b>
     */
    public static void load(JavaPlugin plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            GRAY_PROP     = fetchSigned(GRAY_UUID,     "gray");
            BLUE_PROP = fetchSigned(BLUE_UUID, "blue");

            // ── Skins de métiers ──────────────────────────────────────────────
            // JOB_PROPS.put(EJob.MINER,      fetchSigned("UUID_MINER",      "miner"));
            // JOB_PROPS.put(EJob.LUMBERJACK, fetchSigned("UUID_LUMBERJACK", "lumberjack"));
        });
    }

    /**
     * Crée un {@link GameProfile} avec la {@link Property} signée fournie.
     *
     * @param skinProp Property issue de {@link #gray()}, {@link #blue()} ou {@link #JOB_PROPS}.
     *                 Si {@code null} (UUID non configuré ou fetch raté), retourne un profil sans texture.
     */
    static GameProfile createProfile(UUID uuid, String name, Property skinProp) {
        if (skinProp == null) return new GameProfile(uuid, name);
        try {
            return new GameProfile(uuid, name,
                    new PropertyMap(ImmutableMultimap.of("textures", skinProp)));
        } catch (Exception e) {
            return new GameProfile(uuid, name);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fetch interne
    // ─────────────────────────────────────────────────────────────────────────

    private static Property fetchSigned(String playerUUID, String label) {
        if (playerUUID.startsWith("TODO")) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.SYSTEM, "[TabSkins] UUID non configuré pour le skin : " + label);
            return null;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + playerUUID + "?unsigned=false"))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.SYSTEM, "[TabSkins] " + label + " → HTTP " + response.statusCode());
                return null;
            }

            JsonObject json  = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject prop  = json.getAsJsonArray("properties").get(0).getAsJsonObject();
            String value     = prop.get("value").getAsString();
            String signature = prop.has("signature") ? prop.get("signature").getAsString() : null;

            if (signature == null) {
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.SYSTEM, "[TabSkins] " + label + " → signature absente (unsigned=false ignoré ?)");
            }

            return signature != null
                    ? new Property("textures", value, signature)
                    : new Property("textures", value);

        } catch (Exception e) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.SYSTEM, "[TabSkins] Erreur fetch [" + label + "] : " + e.getMessage());
            return null;
        }
    }
}