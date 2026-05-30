package fr.miuby.survi.system;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.log.ELogTag;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;

/**
 * Gère la mise à jour automatique des fichiers de ressources (.yml, etc.)
 * embarqués dans le jar vers le dossier de données du plugin.
 *
 * Comportement :
 * - Si le fichier n'existe pas sur disque → il est créé.
 * - Si le fichier existe mais son contenu diffère de la version dans le jar → il est écrasé.
 * - Si le fichier est identique → rien ne se passe.
 *
 * Utilisation :
 *   YmlResourceManager.update(plugin, "quests.yml");
 *   YmlResourceManager.update(plugin, "villagers/marchand.yml");
 */
public class YmlResourceManager {

    private YmlResourceManager() {}

    /**
     * Vérifie si la ressource embarquée dans le jar diffère du fichier sur disque,
     * et l'écrase si c'est le cas.
     *
     * @param plugin       L'instance du plugin (pour accéder aux ressources et au dossier de données)
     * @param resourcePath Chemin relatif de la ressource (ex: "quests.yml", "villagers/bob.yml")
     */
    public static void update(JavaPlugin plugin, String resourcePath) {
        File diskFile = new File(plugin.getDataFolder(), resourcePath);

        // Vérifier que la ressource existe dans le jar
        InputStream jarStream = plugin.getResource(resourcePath);
        if (jarStream == null) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.SYSTEM,
                    "[YmlResourceManager] Ressource introuvable dans le jar : " + resourcePath);
            return;
        }

        // Si le fichier n'existe pas sur disque, on le crée directement
        if (!diskFile.exists()) {
            diskFile.getParentFile().mkdirs();
            plugin.saveResource(resourcePath, false);
            MLLogManager.getInstance().log(Level.INFO, ELogTag.SYSTEM,
                    "[YmlResourceManager] Créé : " + resourcePath);
            return;
        }

        // Comparer les hash MD5 : jar vs disque
        try {
            byte[] jarHash  = hash(jarStream);
            byte[] diskHash = hash(Files.newInputStream(diskFile.toPath()));

            if (!MessageDigest.isEqual(jarHash, diskHash)) {
                plugin.saveResource(resourcePath, true); // true = écraser
                MLLogManager.getInstance().log(Level.INFO, ELogTag.SYSTEM,
                        "[YmlResourceManager] Mis à jour (contenu modifié) : " + resourcePath);
            }
            // Si identique, on ne touche rien
        } catch (IOException | NoSuchAlgorithmException e) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.SYSTEM,
                    "[YmlResourceManager] Erreur lors de la comparaison de " + resourcePath + " : " + e.getMessage());
        }
    }

    private static byte[] hash(InputStream stream) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (DigestInputStream dis = new DigestInputStream(stream, md)) {
            dis.readAllBytes();
        }
        return md.digest();
    }
}