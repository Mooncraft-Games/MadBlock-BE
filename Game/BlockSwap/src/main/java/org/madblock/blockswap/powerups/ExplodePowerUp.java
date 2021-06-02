package org.madblock.blockswap.powerups;

import cn.nukkit.Player;
import cn.nukkit.level.Position;
import cn.nukkit.level.particle.HugeExplodeParticle;
import cn.nukkit.level.particle.Particle;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.team.Team;
import org.madblock.newgamesapi.team.TeamPresets;

public class ExplodePowerUp extends PowerUp {

    private static int RADIUS = 5;

    public ExplodePowerUp(GameBehavior behaviour, Player player) {
        super(behaviour, player);
    }

    @Override
    public String getName() {
        return "Explode";
    }

    @Override
    public String getDescription() {
        return "Boom! Make players explode and fly away from your location!";
    }

    @Override
    public boolean isInstantConsumable() {
        return false;
    }

    @Override
    public void use() {
        Team deadTeam = this.behaviour.getSessionHandler().getTeams().get(TeamPresets.DEAD_TEAM_ID);
        Position pos = this.player.getPosition();
        Particle explosion = new HugeExplodeParticle(pos);

        for (Player player : this.behaviour.getSessionHandler().getPlayers()) {
            if (player.getId() != this.player.getId()) {
                player.getLevel().addParticle(explosion, player);
            }
            if (!deadTeam.getPlayers().contains(player) && player.getId() != this.player.getId()) {
                Position targetPos = player.getPosition();
                if (Math.abs(targetPos.getX() - pos.getX()) < 3 && Math.abs(targetPos.getZ() - pos.getZ()) < RADIUS && Math.abs(targetPos.getY() - pos.getY()) < RADIUS) {
                    player.sendMessage(
                            Utility.generateServerMessage("POWERUP", TextFormat.YELLOW,
                                    this.player.getDisplayName() + TextFormat.GRAY + " summoned a great explosion and sent you flying!"
                            )
                    );
                    player.knockBack(this.player, 0, targetPos.getX() - pos.getX(), targetPos.getZ() - pos.getZ(), 1);
                }
            }
        }
    }
}
