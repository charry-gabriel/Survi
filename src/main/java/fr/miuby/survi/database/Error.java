package fr.miuby.survi.database;

import fr.miuby.survi.GameManager;

import java.util.logging.Level;

public class Error {
    public static void execute(Exception ex){
        GameManager.getInstance().getLogger().log(Level.SEVERE, "Couldn't execute MySQL statement: ", ex);
    }
    public static void close(Exception ex){
        GameManager.getInstance().getLogger().log(Level.SEVERE, "Failed to close MySQL connection: ", ex);
    }
}