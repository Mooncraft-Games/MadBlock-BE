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
import org.madblock.newgamesapi.map.pointentities.defaults.PointEntityTypeFirework;

import java.util.HashMap;

public class CommandFirework extends PluginCommand<NewGamesAPI1> {

    public CommandFirework() {
        super("firework", NewGamesAPI1.get());
        this.setDescription("Triggers all the firework point entities in your game.");
        this.setUsage("/firework [string: palette]");

        this.commandParameters.clear();
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {

        if (!sender.isPlayer()) {
            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You must be a player to execute this command.", TextFormat.RED));
            return true;
        }
        Player player = (Player) sender;

        GameHandler h = NewGamesAPI1.getGameManager().getPlayerLookup().get(player.getUniqueId());

        if(h != null) {
            player.sendMessage(Utility.generateServerMessage("SECRET", TextFormat.GOLD, "Secret feature! Launching fireworks."));
            player.getLevel().addParticleEffect(player.getPosition(), ParticleEffect.VILLAGER_HAPPY);
            player.getLevel().addSound(player.getPosition(), Sound.RANDOM_LEVELUP, 1f, 1f, player);

            HashMap<String, String> params = new HashMap<>();

            if(args.length > 0) {
                params.put("colour", args[0]);
            }

            h.getPointEntityTypeManager().getRegisteredTypes().get(PointEntityTypeFirework.ID)
                    .executeFunctionForAll(PointEntityTypeFirework.FUNC_SPAWN, params);

        } else {
            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Somehow you're not in a game. Weird.", TextFormat.RED));
        }
        return true;
    }
}
