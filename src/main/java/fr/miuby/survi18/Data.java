package fr.miuby.survi18;

import org.bukkit.OfflinePlayer;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.Location;


public class Data implements Serializable {
    private static transient final long serialVersionUID = -1681012206529286330L;

    public final HashSet<AlphaPlayer> players;

    // Can be used for saving
    public Data(HashSet<AlphaPlayer> players) {
        this.players = players;
    }
    // Can be used for loading
    public Data(Data loadedData) {
        if(loadedData != null) {
            this.players = loadedData.players;
        } else {
            this.players = new HashSet<>();
        }
    }

    public boolean saveData(String filePath) {
        try {
            GZIPOutputStream gzip = new GZIPOutputStream(new FileOutputStream(filePath));
            BukkitObjectOutputStream out = new BukkitObjectOutputStream(gzip);
            out.writeObject(this);
            gzip.close();
            out.close();
            return true;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    public static Data loadData(String filePath) {
        try {
            new File(filePath).createNewFile(); // if file already exists will do nothing
            GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(filePath));
            BukkitObjectInputStream in = new BukkitObjectInputStream(gzip);
            Data data = (Data) in.readObject();
            gzip.close();
            in.close();
            return data;
        } catch (ClassNotFoundException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public static void Save() {
        HashSet<AlphaPlayer> players = new HashSet<AlphaPlayer>();
        for(AlphaPlayer alphaP : GameManager.getInstance().getAlphaPlayers().values()) {
            Bukkit.getServer().getLogger().log(Level.INFO, "save " + alphaP.getUUID() + " " + alphaP.getCoins() + " coins");
            players.add(alphaP);
        }

        new Data(players).saveData("Save.data");
        Bukkit.getServer().getLogger().log(Level.INFO, "Data Saved");
    }

    public static void Load() {
        /*Data data = new Data(Data.loadData("Save.data"));
        if(data.players != null) {
            for (AlphaPlayer alphaP : data.players) {
                if(alphaP != null) {
                    Bukkit.getServer().getLogger().log(Level.INFO, "load " + alphaP.getUUID() + " " + alphaP.getCoins() + " coins");
                    GameManager.getInstance().getAlphaPlayers().put(alphaP.getUUID(), alphaP);
                }
            }
        }
        Bukkit.getServer().getLogger().log(Level.INFO, "Data loaded");*/
    }
}
