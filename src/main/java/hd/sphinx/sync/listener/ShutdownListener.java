package hd.sphinx.sync.listener;

import hd.sphinx.sync.MainManageData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ShutdownListener implements Listener {

    @EventHandler
    public void onPreCommand(PlayerCommandPreprocessEvent event) {
        String[] commandArgs = event.getMessage().split(" ");
        if (!commandArgs[0].equalsIgnoreCase("/stop")) return;
        if (!(event.getPlayer().hasPermission("minecraft.command.stop")) && !(event.getPlayer().isOp())) return;
        MainManageData.startShutdown();
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (MainManageData.serverStopping) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventory(InventoryClickEvent e) {
        if (MainManageData.serverStopping) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (MainManageData.serverStopping) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (MainManageData.serverStopping) {
            e.setCancelled(true);
        }
    }
}