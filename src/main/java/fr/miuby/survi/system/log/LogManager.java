package fr.miuby.survi.system.log;

import com.google.common.base.CaseFormat;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogManager {
    private static LogManager instance = null;

    public static LogManager getInstance(){
        if(instance == null){
            instance = new LogManager();
        }
        return instance;
    }

    @Getter
    private final Logger logger = Logger.getLogger("Survi");
    private final Map<ETagLog, Boolean> enabledLogs = new HashMap<>();

    private LogManager() {
        for (ETagLog tag : ETagLog.values()) {
            enabledLogs.put(tag, true);
        }
    }

    public enum ETagLog {
        PLAYER,
        VILLAGER,
        QUEST,
        REPUTATION,
        ITEM,
        ROLE,
        WORLD,
        SYSTEM,
    }

    public void log(Level level, ETagLog tag, String message) {
        if (enabledLogs.getOrDefault(tag, false)) {
            logger.log(level, "[" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, tag.toString()) + "] " + message);
        }
    }

    public void toggleLog(ETagLog tag) {
        enabledLogs.compute(tag, (k, currentState) -> !Boolean.TRUE.equals(currentState));
    }

    public boolean isLogEnabled(ETagLog tag) {
        return enabledLogs.getOrDefault(tag, false);
    }
}
