package hd.sphinx.sync.listener;

import hd.sphinx.sync.util.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DeathListener implements Listener {

    // PlayerではなくUUID管理
    public static final Set<UUID> deadPlayers = new HashSet<>();

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (ConfigManager.getBoolean("settings.onlySyncPermission")
                && !player.hasPermission("sync.sync")) return;

        deadPlayers.add(player.getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        triggerRespawn(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // 念のためクリーンアップ（増殖防止）
        deadPlayers.remove(event.getPlayer().getUniqueId());
    }

    public static void triggerRespawn(Player player) {
        if (ConfigManager.getBoolean("settings.onlySyncPermission")
                && !player.hasPermission("sync.sync")) return;

        deadPlayers.remove(player.getUniqueId());
    }

    public static boolean isDead(Player player) {
        return deadPlayers.contains(player.getUniqueId());
    }
}