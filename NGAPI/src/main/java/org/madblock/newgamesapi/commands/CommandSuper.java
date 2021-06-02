package org.madblock.newgamesapi.commands;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Sound;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.ranks.api.RankManager;
import org.madblock.ranks.api.RankProfile;

import java.util.Optional;

public class CommandSuper extends PluginCommand<NewGamesAPI1> {

    public CommandSuper() {
        super("super", NewGamesAPI1.get());
        this.setDescription("Enables 'super mode' in games which support it.");
        this.setUsage("/super");

        this.commandParameters.clear();
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {

        if (!sender.isPlayer()) {
            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You must be a player to execute this command.", TextFormat.RED));
            return true;
        }
        Player player = (Player) sender;

        Optional<RankProfile> profile = RankManager.getInstance().getRankProfile((Player)sender);
        if (!profile.isPresent() || !profile.get().hasPermission("newgameapi.commands.super")) {
            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You do not have permission to execute this command.", TextFormat.RED));
            return true;
        }
        GameHandler h = NewGamesAPI1.getGameManager().getPlayerLookup().get(player.getUniqueId());

        if(h != null) {
            sender.sendMessage(Utility.generateServerMessage("!!!", TextFormat.RED, TextFormat.colorize('&',"&cS &6U &eP &aE &bR   &dM &cO &6D &eE &a! &b! &d!"), TextFormat.WHITE));
            player.getLevel().addParticleEffect(player.getPosition(), ParticleEffect.HUGE_EXPLOSION_LEVEL);

            for(Player p: h.getPlayers()) p.getLevel().addSound(p.getPosition(), Sound.BLOCK_END_PORTAL_SPAWN, 0.9f, 1f, p);
            h.getGameBehaviors().onSuper(player);

        } else {
            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Somehow you're not in a game. Weird.", TextFormat.RED));
        }
        return true;
    }
}
