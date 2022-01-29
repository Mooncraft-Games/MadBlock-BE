package org.madblock.gamemodesumox.powerup;

import cn.nukkit.Player;
import cn.nukkit.block.BlockID;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Sound;
import cn.nukkit.level.particle.Particle;
import cn.nukkit.math.Vector3;
import org.madblock.gamemodesumox.SumoXConstants;
import org.madblock.gamemodesumox.games.GBehaveSumoBase;
import org.madblock.newgamesapi.game.GameHandler;

import java.util.concurrent.atomic.AtomicInteger;

public class PowerUpKBAura extends PowerUp {

    public static final int COUNTDOWN_LENGTH = 8;

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
        return Sound.TILE_PISTON_IN;
    }

    @Override
    public float useSoundPitch() {
        return 0.7f;
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
        AtomicInteger counter = new AtomicInteger(COUNTDOWN_LENGTH);

        this.gameHandler.getGameScheduler().registerSelfCancellableGameTask(task -> {
            int newCount = counter.decrementAndGet();

            if(newCount < 1) {
                for(Player v: gameHandler.getPlayers()) {
                    if(v != p) {
                        double deltaX = v.getX() - p.getX();
                        double deltaZ = v.getZ() - p.getZ();

                        double distance = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaZ, 2));

                        double multiplier = 1f;

                        if(gameHandler.getGameBehaviors() instanceof GBehaveSumoBase) {
                            GBehaveSumoBase gameBehave = (GBehaveSumoBase) gameHandler.getGameBehaviors();
                            multiplier *= Math.min(gameBehave.calculatePanicKnockbackMultiplier(), 5);

                            if(PowerUpImmunity.getImmuneEntities().contains(v))
                                multiplier *= 0;
                        }

                        if (distance <= SumoXConstants.POWERUP_KBAURA_RADIUS) {
                            Vector3 dir = new Vector3(deltaX, 0, deltaZ).normalize();
                            v.setMotion(new Vector3(dir.x, SumoXConstants.POWERUP_KBAURA_Y_VELOCITY, dir.z).multiply(SumoXConstants.POWERUP_KBAURA_POWER).multiply(multiplier));
                        }
                    }
                }

                // kaboom :)
                context.getPlayer().getLevel().addParticleEffect(context.getPlayer(), ParticleEffect.HUGE_EXPLOSION_LEVEL);
                context.getPlayer().getLevel().addSound(context.getPlayer(), Sound.MOB_ENDERDRAGON_FLAP, 0.9f, 1f);
                task.cancel();

            } else {
                // A fuse kinda sound in a way, let players know it's coming.
                context.getPlayer().getLevel().addSound(context.getPlayer(), Sound.NOTE_HARP, 0.9f, 1.2f + ((1 - (((float) newCount) / COUNTDOWN_LENGTH)) / 2f) );
            }
        }, 0, 2);

        return true;
    }

    @Override
    public void cleanUp() { }

}
