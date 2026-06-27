package hd.sphinx.sync;

import hd.sphinx.sync.backup.BackupHandler;
import hd.sphinx.sync.backup.CustomSyncSettings;
import hd.sphinx.sync.listener.DeathListener;
import hd.sphinx.sync.mongo.ManageMongoData;
import hd.sphinx.sync.mongo.MongoDB;
import hd.sphinx.sync.mysql.ManageMySQLData;
import hd.sphinx.sync.mysql.MySQL;
import hd.sphinx.sync.util.ConfigManager;
import hd.sphinx.sync.util.InventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MainManageData {

    public static StorageType storageType;
    public static boolean serverStopping = false;

    public static ArrayList<Player> loadedPlayerData = new ArrayList<>();
    public static HashMap<Player, ArrayList<String>> commandHashMap = new HashMap<>();

    // ★追加：kit編集中ロック
    public static Set<Player> editingKit = new HashSet<>();

    public static void initialize() {
        try {
            storageType = StorageType.valueOf(ConfigManager.getString("settings.storageType"));
        } catch (Exception ignored) {
            Main.logger.severe("No valid StorageType is set in Config!\n Disabling Plugin!");
            Bukkit.getPluginManager().disablePlugin(Main.main);
        }

        if (storageType == StorageType.MYSQL) {
            MySQL.connectMySQL();
            try {
                MySQL.registerMySQL();
            } catch (SQLException ignored) {
                Main.logger.severe("Could not initialize Database!\n Disabling Plugin!");
                Bukkit.getPluginManager().disablePlugin(Main.main);
            }
        } else if (storageType == StorageType.MONGODB) {
            MongoDB.connectMongoDB();
        }

        BackupHandler.initialize();
    }

    public static void reload() {
        BackupHandler.shutdown();

        try {
            storageType = StorageType.valueOf(ConfigManager.getString("settings.storageType"));
        } catch (Exception exception) {
            Main.logger.severe("No valid StorageType is set in Config!\n Disabling Plugin!");
            Bukkit.getPluginManager().disablePlugin(Main.main);
        }

        if (storageType == StorageType.MYSQL) {
            MySQL.disconnectMySQL();
            MySQL.connectMySQL();
        } else if (storageType == StorageType.MONGODB) {
            MongoDB.disconnectMongoDB();
            MongoDB.connectMongoDB();
        }

        BackupHandler.initialize();
    }

    public static void startShutdown() {
        serverStopping = true;

        BackupHandler.shutdown();

        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayer(player);
            player.kickPlayer("Server restarting...");
        }

        shutdown();
    }

    public static void shutdown() {
        if (storageType == StorageType.MYSQL) {
            MySQL.disconnectMySQL();
        } else if (storageType == StorageType.MONGODB) {
            MongoDB.disconnectMongoDB();
        }
    }

    public static Boolean isPlayerKnown(Player player) {
        if (storageType == StorageType.MYSQL) {
            return ManageMySQLData.isPlayerInDB(player);
        } else if (storageType == StorageType.MONGODB) {
            return ManageMongoData.isPlayerInDB(player);
        }
        return false;
    }

    public static void generatePlayer(Player player) {
        if (storageType == StorageType.MYSQL) {
            ManageMySQLData.generatePlayer(player);
        } else if (storageType == StorageType.MONGODB) {
            ManageMongoData.generatePlayer(player);
        }
    }

    public static void loadPlayer(Player player) {
        if (storageType == StorageType.MYSQL) {
            ManageMySQLData.loadPlayer(player);
        } else if (storageType == StorageType.MONGODB) {
            ManageMongoData.loadPlayer(player);
        }
    }

    /**
     * =========================
     * SAFE SAVE (最小修正版)
     * =========================
     */
    public static void savePlayer(Player player) {

        // ★追加①：kit編集中は保存しない（超重要）
        if (editingKit.contains(player)) {
            return;
        }

        // cursor回収（そのまま維持）
        try {
            ItemStack cursor = player.getItemOnCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                if (player.getOpenInventory().getTopInventory().getSize() > 0) {
                    return;
                }
                player.getInventory().addItem(cursor);
                player.setItemOnCursor(new ItemStack(Material.AIR));
            }
        } catch (Exception ignored) {}

        // 死亡処理
        if (DeathListener.deadPlayers.contains(player)) {
            player.getInventory().clear();
            player.setHealth(20);
            player.setFoodLevel(20);
        }

        // ★追加②：インベントリスナップショット（競合防止）
        PlayerInventory inv = player.getInventory();

        ItemStack[] contents = inv.getContents() != null ? inv.getContents().clone() : new ItemStack[0];
        ItemStack[] armor = inv.getArmorContents() != null ? inv.getArmorContents().clone() : new ItemStack[0];
        ItemStack offhand = inv.getItemInOffHand();

        String ecBase64 = InventoryManager.saveEChest(player);
        if (ecBase64 == null) return;

        if (storageType == StorageType.MYSQL) {
            ManageMySQLData.savePlayer(
                    player,
                    InventoryManager.saveItems(player, inv),
                    ecBase64
            );
        } else if (storageType == StorageType.MONGODB) {
            ManageMongoData.savePlayer(
                    player,
                    InventoryManager.saveItems(player, inv),
                    ecBase64
            );
        }
    }

    public static void savePlayer(Player player, CustomSyncSettings customSyncSettings) {

        // ★追加：同じくガード
        if (editingKit.contains(player)) {
            return;
        }

        try {
            ItemStack cursor = player.getItemOnCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                if (player.getOpenInventory().getTopInventory().getSize() > 0) {
                    return;
                }
                player.getInventory().addItem(cursor);
                player.setItemOnCursor(new ItemStack(Material.AIR));
            }
        } catch (Exception ignored) {}

        if (storageType == StorageType.MYSQL) {
            ManageMySQLData.savePlayer(player, customSyncSettings);
        } else if (storageType == StorageType.MONGODB) {
            ManageMongoData.savePlayer(player, customSyncSettings);
        }
    }

    public enum StorageType {
        MYSQL,
        MONGODB,
        CLOUD;
    }
}