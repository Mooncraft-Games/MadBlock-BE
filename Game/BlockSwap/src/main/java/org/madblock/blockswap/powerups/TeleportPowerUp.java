package org.madblock.blockswap.powerups;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.team.TeamPresets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TeleportPowerUp extends PowerUp {

    public TeleportPowerUp(GameBehavior behaviour, Player player) {
        super(behaviour, player);
    }

    @Override
    public String getName() {
        return "Teleport";
    }

    @Override
    public String getDescription() {
        return "Use this to teleport to a random player!";
    }

    @Override
    public boolean isInstantConsumable() {
        return false;
    }

    @Override
    public int getDisplayItemID() {
        return Item.ENDER_EYE;
    }

    @Override
    public void use() {
        HashSet<Player> deadTeamPlayers = this.behaviour.getSessionHandler().getTeams().get(TeamPresets.DEAD_TEAM_ID).getPlayers();
        List<Player> allPlayers = new ArrayList<>(this.behaviour.getSessionHandler().getPlayers());
        if (this.behaviour.getSessionHandler().getPlayers().size() - deadTeamPlayers.size() > 1) {
            Player target;
            do {
                target = allPlayers.get((int)Math.floor(Math.random() * allPlayers.size()));
            } while (target.getId() == this.player.getId() || deadTeamPlayers.contains(target));

            Position targetPos = target.getPosition();

            this.player.teleport(targetPos.add(new Vector3(0, 3, 0)));

            target.sendMessage(
                    Utility.generateServerMessage("POWERUP", TextFormat.YELLOW,
                            this.player.getDisplayName() + TextFormat.GRAY + " teleported to your location!"
                    )
            );
            this.player.sendMessage(
                    Utility.generateServerMessage("POWERUP", TextFormat.YELLOW,
                            TextFormat.GRAY + " You teleported to " + target.getDisplayName() + TextFormat.GRAY + "!"
                    )
            );

        }
    }
}
