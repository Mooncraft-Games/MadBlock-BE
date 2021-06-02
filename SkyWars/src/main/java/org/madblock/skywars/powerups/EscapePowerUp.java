package org.madblock.skywars.powerups;

import cn.nukkit.Player;
import cn.nukkit.block.BlockAir;
import cn.nukkit.block.BlockGlass;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.Sound;
import cn.nukkit.level.particle.HugeExplodeParticle;
import cn.nukkit.math.Vector3;
import org.madblock.newgamesapi.game.GameBehavior;

public class EscapePowerUp extends PowerUp {

    private static final int EXPLODE_RADIUS = 5;

    public EscapePowerUp(GameBehavior behaviour) {
        super(behaviour);
    }

    @Override
    public String getName() {
        return "Emergency Escape";
    }

    @Override
    public String getDescription() {
        return "Tap this to summon a tower and a strong knockback force around you!";
    }

    @Override
    public int getItemId() {
        return ItemID.LEAD;
    }

    @Override
    public void use(Player user) {

        Vector3 pos = user.getPosition();

        for (Player player : behaviour.getSessionHandler().getPlayers()) {

            if (player != user && user.distance(player) <= EXPLODE_RADIUS) {

                Vector3 targetPos = player.getPosition();
                player.getLevel().addParticle(
                        new HugeExplodeParticle(pos),
                        player
                );
                player.knockBack(user, 0, targetPos.getX() - pos.getX(), targetPos.getZ() - pos.getZ(), 1);

            }
            player.getLevel().addSound(player.getPosition(), Sound.RANDOM_EXPLODE);

        }

        // Make the player leap.
        user.setMotion(new Vector3(0, 1, 0));
        behaviour.getSessionHandler().getGameScheduler().registerGameTask(() -> {
            if (behaviour.getSessionHandler().getPlayers().contains(user)) {

                int x = user.getFloorX() - 1;
                int z = user.getFloorZ() - 1;
                int y = user.getFloorY();

                for (int tempX = x; tempX <= x + 2; tempX++) {
                    for (int tempZ = z; tempZ <= z + 2; tempZ++) {
                        user.getLevel().setBlock(new Vector3(tempX, y, tempZ), new BlockGlass());
                    }
                }

                behaviour.getSessionHandler().getGameScheduler().registerGameTask(() -> {

                    for (int tempX = x; tempX <= x + 2; tempX++) {
                        for (int tempZ = z; tempZ <= z + 2; tempZ++) {
                            user.getLevel().setBlock(new Vector3(tempX, y, tempZ), new BlockAir());
                        }
                    }

                }, 100);

            }
        }, 10);



    }
}
