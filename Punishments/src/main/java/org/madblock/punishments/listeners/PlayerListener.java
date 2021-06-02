package org.madblock.punishments.listeners;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.event.player.PlayerPreLoginEvent;
import org.madblock.punishments.PunishmentsPlugin;
import org.madblock.punishments.api.PunishmentEntry;
import org.madblock.punishments.api.PunishmentManager;
import org.madblock.punishments.logic.PunishmentLogic;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class PlayerListener implements Listener {

    private PunishmentsPlugin plugin;

    public PlayerListener (PunishmentsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat (PlayerChatEvent event) {

        // Only blocking code.

        List<PunishmentEntry> punishments;
        try {
            punishments = PunishmentManager.getInstance().getActivePunishments(event.getPlayer());  // Will use cache when possible
        } catch (SQLException exception) {
            exception.printStackTrace();
            return;
        }

        for (PunishmentEntry punishment : punishments) {
            Optional<PunishmentLogic> punishmentLogic = PunishmentManager.getInstance().getLogic(punishment.getType());
            if (punishmentLogic.isPresent()) {
                if (!punishmentLogic.get().onChat(event.getPlayer(), punishment)) {
                    event.setCancelled();
                    return;
                }
            }
        }

    }

    @EventHandler
    public void onPlayerJoin (PlayerPreLoginEvent event) {

        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
            List<PunishmentEntry> punishments;
            try {
                // Do we have any punishment that is preventing us from joining?
                punishments = PunishmentManager.getInstance().getActivePunishments(event.getPlayer());
            } catch (SQLException exception) {
                plugin.getLogger().warning("Failed to get active (if any) punishments for connecting user.");
                exception.printStackTrace();
                return;
            }
            plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                for (PunishmentEntry punishment : punishments) {
                    Optional<PunishmentLogic> punishmentLogic = PunishmentManager.getInstance().getLogic(punishment.getType());
                    if (!punishmentLogic.isPresent()) {
                        this.plugin.getLogger().critical(punishment.getType().name() + " does not have any logic registered.");
                        continue;
                    }
                    if (!punishmentLogic.get().onJoin(event.getPlayer(), punishment)) {
                        break;
                    }
                }
            });
        }, true);

    }

}
