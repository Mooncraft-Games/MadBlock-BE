package org.madblock.skywars.powerups;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import org.madblock.newgamesapi.game.GameBehavior;

public class GiantSnowBallPowerUp extends PowerUp {

    public static final String GIANT_SNOWBALL_NBT_TAG = "skywars_giant_snowball_entity";

    public GiantSnowBallPowerUp(GameBehavior behaviour) {
        super(behaviour);
    }

    @Override
    public String getName() {
        return "Giant Snowball";
    }

    @Override
    public String getDescription() {
        return "Tap to throw an even larger snowball!";
    }

    @Override
    public int getItemId() {
        return ItemID.SNOWBALL;
    }

    @Override
    public void use(Player user) {

        user.getLevel().addSound(user.getPosition(), Sound.MOB_SNOWGOLEM_SHOOT, 1f, 1f, user);

        Vector3 direction = user.getDirectionVector();
        Entity snowBallEntity = Entity.createEntity("Snowball", user.getPosition().add(new Vector3(0, user.getEyeHeight() * 2, 0)));
        snowBallEntity.setScale(2);
        snowBallEntity.setMotion(direction);
        snowBallEntity.namedTag.putBoolean(GIANT_SNOWBALL_NBT_TAG, true);
        snowBallEntity.spawnToAll();

    }
}
