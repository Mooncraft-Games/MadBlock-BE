package org.madblock.blockswap.powerups.impl;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.level.Position;
import cn.nukkit.potion.Effect;
import cn.nukkit.utils.TextFormat;
import org.madblock.blockswap.behaviours.BlockSwapGameBehaviour;
import org.madblock.blockswap.powerups.PowerUp;
import org.madblock.newgamesapi.util.Utility;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.team.Team;
import org.madblock.newgamesapi.team.TeamPresets;

public class BlindnessPowerUp extends PowerUp {

    private static final int RADIUS = 10;


    public BlindnessPowerUp(GameBehavior behaviour, Player player) {
        super(behaviour, player);
    }

    @Override
    public String getName() {
        return "Blindness";
    }

    @Override
    public String getDescription() {
        return String.format("Use this to blind players that are within a %s block radius!", RADIUS);
    }

    @Override
    public boolean isInstantConsumable() {
        return false;
    }

    @Override
    public int getDisplayItemID() {
        return Item.FERMENTED_SPIDER_EYE;
    }

    @Override
    public void use() {
        Team deadTeam = this.behaviour.getSessionHandler().getTeams().get(TeamPresets.DEAD_TEAM_ID);
        Position pos = this.player.getPosition();
        int tickTime = 20 * (int)(((BlockSwapGameBehaviour)this.behaviour).getCompletedRounds() / 2 + 0.5);
        for (Player player : this.behaviour.getSessionHandler().getPlayers()) {
            if (!deadTeam.getPlayers().contains(player) && player.getId() != this.player.getId()) {

                Position targetPos = player.getPosition();
                if (Math.abs(targetPos.getX() - pos.getX()) < RADIUS && Math.abs(targetPos.getZ() - pos.getZ()) < RADIUS) {
                    player.sendMessage(
                            Utility.generateServerMessage(
                                    "POWERUP",
                                    TextFormat.YELLOW,
                                    this.player.getDisplayName() + TextFormat.GRAY + " blinded you for " + (tickTime / 20) + " seconds."
                            )
                    );
                    player.addEffect(
                            new Effect(Effect.BLINDNESS, "Blindness", 0, 0, 0)
                                    .setDuration(tickTime)
                                    .setVisible(false)
                    );
                }
            }
        }
    }
}
