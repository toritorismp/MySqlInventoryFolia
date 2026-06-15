package hd.sphinx.sync.util.scheduler;

import hd.sphinx.sync.Main;
import hd.sphinx.sync.MainManageData;
import hd.sphinx.sync.api.SyncProfile;
import hd.sphinx.sync.api.SyncSettings;
import hd.sphinx.sync.api.events.CompletedLoadingPlayerDataEvent;
import hd.sphinx.sync.api.events.SavingPlayerDataEvent;
import hd.sphinx.sync.backup.BackupHandler;
import hd.sphinx.sync.backup.CustomSyncSettings;
import hd.sphinx.sync.mysql.ManageMySQLData;
import hd.sphinx.sync.util.ConfigManager;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FoliaScheduler implements Scheduler {

    private final AsyncScheduler asyncScheduler;

    public FoliaScheduler() {
        this.asyncScheduler = Bukkit.getAsyncScheduler();
    }

    @Override
    public void scheduleBackupTask() {

        asyncScheduler.runAtFixedRate(
                Main.main,
                task -> BackupHandler.handleCycle(),
                0L,
                ConfigManager.config.getInt("settings.backup.backupCycle") / 20L,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void cancelBackupTask() {
        // AsyncSchedulerは個別保持不要（必要ならFuture管理）
    }

    @Override
    public void scheduleJoin(Player player) {

        player.getScheduler().runDelayed(Main.main, task -> {
            MainManageData.loadPlayer(player);
        }, null, 40L);
    }

    @Override
    public void scheduleExecuteCommands(Player player) {

        player.getScheduler().run(Main.main, task -> {

            MainManageData.loadedPlayerData.remove(player);

            List<String> commands =
                    MainManageData.commandHashMap.getOrDefault(player, new ArrayList<>());

            for (String command : commands) {
                if (command == null) continue;
                player.performCommand(command.replaceFirst("/", ""));
            }

        }, null);
    }

    @Override
    public void scheduleCompleteLoadEvent(Player player, SyncProfile syncProfile) {

        player.getScheduler().run(Main.main, task -> {
            Bukkit.getPluginManager().callEvent(
                    new CompletedLoadingPlayerDataEvent(
                            player,
                            new SyncSettings(),
                            syncProfile
                    )
            );
        }, null);
    }

    @Override
    public void scheduleSavingDataEvent(Player player, SyncProfile syncProfile) {

        player.getScheduler().run(Main.main, task -> {
            Bukkit.getPluginManager().callEvent(
                    new SavingPlayerDataEvent(
                            player,
                            new SyncSettings(),
                            syncProfile
                    )
            );
        }, null);
    }

    @Override
    public void scheduleMySQLGeneratePlayer(Player player) {

        asyncScheduler.runDelayed(
                Main.main,
                task -> ManageMySQLData.generatePlayer(player),
                1L,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void scheduleMySQLSavePlayer(Player player, String invBase64, String ecBase64) {

        asyncScheduler.runDelayed(
                Main.main,
                task -> ManageMySQLData.savePlayer(player, invBase64, ecBase64),
                1L,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void scheduleMySQLSavePlayer(Player player, CustomSyncSettings customSyncSettings) {

        asyncScheduler.runDelayed(
                Main.main,
                task -> ManageMySQLData.savePlayer(player, customSyncSettings),
                1L,
                TimeUnit.SECONDS
        );
    }
}