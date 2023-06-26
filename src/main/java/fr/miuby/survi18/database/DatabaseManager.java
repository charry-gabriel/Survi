package fr.miuby.survi18.database;

import fr.miuby.survi18.AlphaPlayer;
import fr.miuby.survi18.GameManager;
import fr.miuby.survi18.village.Village;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class DatabaseManager {
    private DbConnection dbConnection;

    public DatabaseManager() {
        this.dbConnection = new DbConnection(new DbCredentials("141.95.159.90", "Miuby", "Gaby__11", "minecraft"));
    }

    public void close() {
        try {
            this.dbConnection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public DbConnection getDbConnection() {
        return dbConnection;
    }

    public void updatePlayer(UUID uuid, String column, int value) {
        Bukkit.getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            final PreparedStatement preparedStatement;
            try {
                preparedStatement = dbConnection.getConnection().prepareStatement("UPDATE player SET "+column+" = ? WHERE uuid = ?");
                preparedStatement.setInt(1, value);
                preparedStatement.setString(2, uuid.toString());
                preparedStatement.executeUpdate();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });
    }

    public void createAlphaPlayers() {
        try {
            final Connection connection = dbConnection.getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement("SELECT uuid FROM player");
            final ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                GameManager.getInstance().getAlphaPlayers().put(uuid, new AlphaPlayer(uuid));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void createVillagers(Village village) {
        try {
            final Connection connection = dbConnection.getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement("SELECT name FROM villager");
            final ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                village.getVillagersLevel().get(resultSet.getString("name")).SetLevel(resultSet.getInt("level"));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void updateVillager(String name, int level) {
        Bukkit.getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            final PreparedStatement preparedStatement;
            try {
                preparedStatement = dbConnection.getConnection().prepareStatement("UPDATE villager SET level = ? WHERE name = ?");
                preparedStatement.setInt(1, level);
                preparedStatement.setString(2, name);
                preparedStatement.executeUpdate();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });
    }
}
