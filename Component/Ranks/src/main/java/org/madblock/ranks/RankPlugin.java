package org.madblock.ranks;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.level.Sound;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;
import org.madblock.ranks.api.RankManager;
import org.madblock.ranks.api.RankProfile;
import org.madblock.ranks.commands.RankCommand;
import org.madblock.ranks.enums.PrimaryRankID;
import org.madblock.ranks.enums.SubRankID;
import org.madblock.ranks.ranks.PrimaryRank;
import org.madblock.ranks.ranks.SubRank;
import org.madblock.ranks.util.Utility;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;

public class RankPlugin extends PluginBase implements Listener {

    @Override
    public void onEnable() {

        RankManager rankManager = new RankManager();
        this.getServer().getPluginManager().registerEvents(this, this);

        rankManager.addPrimaryRank(new PrimaryRank(PrimaryRankID.PLAYER, 0, PermissionGroup.BASE));
        rankManager.addPrimaryRank(new PrimaryRank(PrimaryRankID.VIP, 1, PermissionGroup.BASE));
        rankManager.addPrimaryRank(new PrimaryRank(PrimaryRankID.VIP_PLUS, 2, PermissionGroup.BASE));
        rankManager.addPrimaryRank(new PrimaryRank(PrimaryRankID.ELITE, 3, PermissionGroup.BASE));
        rankManager.addPrimaryRank(new PrimaryRank(PrimaryRankID.OVERLORD, 4, PermissionGroup.BASE));

        rankManager.addPrimaryRank(new PrimaryRank(PrimaryRankID.YT, 50, PermissionGroup.BASE));

        rankManager.addPrimaryRank(new PrimaryRank(PrimaryRankID.CONTENT, 100, PermissionGroup.CONTENT));
        rankManager.addPrimaryRank(new PrimaryRank(PrimaryRankID.CONTENT_2, 101, PermissionGroup.CONTENT));
        rankManager.addPrimaryRank(new PrimaryRank(PrimaryRankID.HELPER, 103, PermissionGroup.MINI_MOD));
        rankManager.addPrimaryRank(new PrimaryRank(PrimaryRankID.MOD, 104, PermissionGroup.MODERATOR));
        rankManager.addPrimaryRank(new PrimaryRank(PrimaryRankID.SRMOD, 105, PermissionGroup.MODERATOR));

        rankManager.addPrimaryRank(new PrimaryRank(PrimaryRankID.DEV, 200, PermissionGroup.ADMINISTRATOR));
        rankManager.addPrimaryRank(new PrimaryRank(PrimaryRankID.ADMIN, 201, PermissionGroup.ADMINISTRATOR));
        rankManager.addPrimaryRank(new PrimaryRank(PrimaryRankID.ADMIN_2, 202, PermissionGroup.ADMINISTRATOR));

        rankManager.addSubRank(new SubRank(SubRankID.COMMUNITY_RELATIONS, PermissionGroup.COMMUNITY_RELATIONS));
        rankManager.addSubRank(new SubRank(SubRankID.MEDIA, PermissionGroup.MEDIA));
        rankManager.addSubRank(new SubRank(SubRankID.STAFF_COORDINATION, PermissionGroup.EMPTY));
        rankManager.addSubRank(new SubRank(SubRankID.SUPPORT, PermissionGroup.SUPPORT));
        rankManager.addSubRank(new SubRank(SubRankID.TOURNEY, PermissionGroup.TOURNEY));

        RankManager.setInstance(rankManager);

        this.getServer().getCommandMap().register("rank", new RankCommand(this));

    }

    @EventHandler
    public void onPlayerJoin (PlayerJoinEvent event) {

        RankManager manager = RankManager.getInstance();

        if (!manager.getRankProfile(event.getPlayer()).isPresent()) {
            getServer().getScheduler().scheduleTask(this, () -> {
                Optional<RankProfile> profile;
                try {
                    profile = manager.fetchRankProfile(event.getPlayer());
                } catch (SQLException exception) {
                    exception.printStackTrace();
                    getServer().getScheduler().scheduleTask(this, () -> event.getPlayer().sendMessage(
                            Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An error occurred while trying to retrieve your rank profile.", TextFormat.RED)
                    ));
                    return;
                }
                profile.ifPresent(rankProfile -> applyRankPurchases(event.getPlayer(), rankProfile));
            }, true);
        } else {
            applyRankPurchases(event.getPlayer(), manager.getRankProfile(event.getPlayer()).get());
        }

    }

    private void applyRankPurchases (Player player, RankProfile profile) {
        // Okay, now let's check purchases.
        getServer().getScheduler().scheduleTask(this, () -> {
            RankManager manager = RankManager.getInstance();
            Collection<PrimaryRankID> rankPurchases;
            try {
                rankPurchases = manager.fetchRankPurchases(player);
            } catch (SQLException exception) {
                exception.printStackTrace();
                getServer().getScheduler().scheduleDelayedTask(this, () -> {
                    player.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An error occurred while fetch your current ranks purchases.", TextFormat.RED));
                    player.getLevel().addSound(player.getPosition(), Sound.NOTE_BASS, 1, 1, player);
                }, 100);
                return;
            }
            for (PrimaryRankID rank : rankPurchases) {
                if (!profile.hasPrimaryRank(rank)) {
                    boolean success;
                    try {
                        success = profile.addPrimaryRank(rank);
                    } catch (SQLException exception) {
                        exception.printStackTrace();
                        getServer().getScheduler().scheduleDelayedTask(this, () -> {
                            player.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An error occurred while trying to apply your purchased ranks. If the problem persists on relogging, please make a support ticket.", TextFormat.RED));
                            player.getLevel().addSound(player.getPosition(), Sound.NOTE_BASS, 1, 1, player);
                        }, 100);
                        continue;
                    }
                    getServer().getScheduler().scheduleDelayedTask(this, () -> {
                        if (success) {
                            player.sendMessage(Utility.generateServerMessage("PURCHASE", TextFormat.AQUA, "Thank you for supporting MooncraftGames! You now have the " + rank.getColor() + TextFormat.BOLD + rank.getName() + TextFormat.RESET + TextFormat.GREEN + " rank!", TextFormat.GREEN));
                            player.getLevel().addSound(player.getPosition(), Sound.RANDOM_LEVELUP, 1, 1, player);
                        } else {
                            player.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An error occurred while trying to apply your purchased ranks. If the problem persists on relogging, please make a support ticket.", TextFormat.RED));
                            player.getLevel().addSound(player.getPosition(), Sound.NOTE_BASS, 1, 1, player);
                        }
                    }, 100);
                }
            }
        }, true);
    }
}