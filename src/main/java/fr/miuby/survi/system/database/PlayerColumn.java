package fr.miuby.survi.system.database;

import lombok.Getter;

/**
 * Whitelist of player table columns allowed for updates.
 * Prevents SQL injection and makes update sites explicit.
 */
@Getter
public enum PlayerColumn {
    MORT("mort"),
    SUCCESS("success"),
    ROLE("role"),
    PSEUDO("pseudo"),
    SUBROLES("subroles");

    private final String columnName;

    PlayerColumn(String columnName) {
        this.columnName = columnName;
    }

}
