package fr.miuby.survi.system.log;

import fr.miuby.lib.log.MLLogPersistence;
import fr.miuby.survi.system.database.repository.SystemRepository;
import org.jspecify.annotations.Nullable;

/**
 * Branche la persistence des états de log ({@link MLLogPersistence})
 * sur le {@link SystemRepository} de Survi (table {@code server_data}).
 *
 * <p>Instanciée dans {@link fr.miuby.survi.GameManager} et passée à
 * {@code MLLogManager.getInstance().initialize(persistence)}.</p>
 */
public class LogPersistence implements MLLogPersistence {
    private final SystemRepository systemRepository;

    public LogPersistence(SystemRepository systemRepository) {
        this.systemRepository = systemRepository;
    }

    @Override
    public @Nullable Boolean getTagState(String tagName) {
        return systemRepository.getLogTagState(tagName);
    }

    @Override
    public void saveTagState(String tagName, boolean enabled) {
        systemRepository.saveLogTagState(tagName, enabled);
    }

    @Override
    public @Nullable Boolean getLevelState(String levelName) {
        return systemRepository.getLogLevelState(levelName);
    }

    @Override
    public void saveLevelState(String levelName, boolean enabled) {
        systemRepository.saveLogLevelState(levelName, enabled);
    }
}
