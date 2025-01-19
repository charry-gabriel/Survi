package fr.miuby.survi.database;

import fr.miuby.survi.GameManager;

import java.util.logging.Level;

/**
 * The Error class provides methods for logging errors that occur during MySQL operations,
 * such as executing statements and closing connections. It utilizes the logger from the
 * GameManager instance to log relevant error messages along with exception details.
 */
public class Error {
    /**
     * Logs an error message indicating that the execution of a MySQL statement has failed.
     *
     * @param ex the exception encountered while attempting to execute the MySQL statement
     */
    public static void execute(Exception ex){
        GameManager.getInstance().getLogger().log(Level.SEVERE, "Couldn't execute MySQL statement: ", ex);
    }
    /**
     * Logs an error message when failing to close a MySQL connection.
     *
     * @param ex the exception encountered while attempting to close the MySQL connection
     */
    public static void close(Exception ex){
        GameManager.getInstance().getLogger().log(Level.SEVERE, "Failed to close MySQL connection: ", ex);
    }
}