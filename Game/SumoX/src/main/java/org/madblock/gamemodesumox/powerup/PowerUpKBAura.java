package org.madblock.gamemodesumox.powerup;

import cn.nukkit.Player;
import cn.nukkit.block.BlockID;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import org.madblock.gamemodesumox.SumoXConstants;
import org.madblock.gamemodesumox.games.GBehaveSumoBase;
import org.madblock.newgamesapi.game.GameHandler;

public class PowerUpKBAura extends PowerUp {

    public PowerUpKBAura(GameHandler gameHandler) {
        super(gameHandler);
    }

    @Override
    public String getName() {
        return "Forcefield";
    }

    @Override
    public String getDescription() {
        return "Knocks players back within a certain range.";
    }

    @Override
    public String getUsage() {
        return String.format("Use item to knock any player within %s blocks away from you.", SumoXConstants.POWERUP_KBAURA_RADIUS);
    }

    @Override
    public Sound useSound() {
        return Sound.MOB_ENDERDRAGON_FLAP;
    }

    @Override
    public float useSoundPitch() {
        return 1f;
    }

    @Override
    public int getWeight() {
        return 100;
    }

    @Override
    public Integer getItemID() {
        return BlockID.TNT;
    }

    @Override
    public boolean isConsumedImmediatley() {
        return false;
    }

    @Override
    public boolean use(PowerUpContext context) {
        Player p = context.getPlayer();
        for(Player v: gameHandler.getPlayers()) {
            if(v != p) {
                double deltaX = v.getX() - p.getX();
                double deltaZ = v.getZ() - p.getZ();

                double distance = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaZ, 2));

                double multiplier = 1f;

                if(gameHandler.getGameBehaviors() instanceof GBehaveSumoBase) {
                    GBehaveSumoBase gameBehave = (GBehaveSumoBase) gameHandler.getGameBehaviors();
                    multiplier *= Math.min(gameBehave.calculatePanicKnockbackMultiplier(), 5);
                }

                if (distance <= SumoXConstants.POWERUP_KBAURA_RADIUS) {
                    Vector3 dir = new Vector3(deltaX, 0, deltaZ).normalize();
                    v.setMotion(new Vector3(dir.x, SumoXConstants.POWERUP_KBAURA_Y_VELOCITY, dir.z).multiply(SumoXConstants.POWERUP_KBAURA_POWER).multiply(multiplier));
                }
            }
        }
        return true;
    }

    @Override
    public void cleanUp() { }

}
