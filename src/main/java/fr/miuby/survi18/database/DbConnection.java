package fr.miuby.survi18.database;

import fr.miuby.survi18.GameManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnection {
    private DbCredentials dbCredentials;
    private Connection connection;

    public DbConnection(DbCredentials dbCredentials) {
        this.dbCredentials = dbCredentials;
        this.connect();
    }

    private void connect() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection(this.dbCredentials.toURI(), this.dbCredentials.getUser(), this.dbCredentials.getPass());

            GameManager.getInstance().getLogger().info("Successfully connected to DB.");
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void close() throws SQLException {
        if(this.connection != null && !this.connection.isClosed()) {
            this.connection.close();
        }
    }

    public Connection getConnection() throws SQLException {
        if(this.connection != null && !this.connection.isClosed()) {
            return this.connection;
        }
        connect();
        return this.connection;
    }
}
