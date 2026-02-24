package fr.miuby.survi.system.database;

/**
 * The Errors class defines a set of constant error messages that are commonly used
 * throughout the application for MySQL-related operations. These constants help in
 * centralizing error messages to maintain consistency and improve code readability.
 */
public class Errors {
    public static final String sqlConnectionExecute = "Couldn't execute MySQL statement: ";
    public static final String sqlConnectionClose = "Failed to close MySQL connection: ";
    public static final String noSQLConnection = "Unable to retrieve MYSQL connection: ";
    //public static final String noTableFound = "Database Error: No Table Found";
    public static final String nullException = "Null exception";
}