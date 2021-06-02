package org.madblock.skywars.powerups;

import cn.nukkit.Player;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.skywars.utils.Constants;

public class LeapPowerUp extends PowerUp {

    public LeapPowerUp(GameBehavior behaviour) {
        super(behaviour);
    }

    @Override
    public String getName() {
        return "Leap";
    }

    @Override
    public String getDescription() {
        return "Tap to leap in any direction!";
    }

    @Override
    public int getItemId() {
        return ItemID.FEATHER;
    }

    @Override
    public void use(Player user) {
        user.getLevel().addSound(user.getPosition(), Sound.MOB_ENDERDRAGON_FLAP, 0.25f, 1);
        Vector3 directionVector = user.getDirectionVector();
        user.setMotion(new Vector3(directionVector.getX(), Math.abs(directionVector.getY()), directionVector.getZ()).multiply(Constants.LEAP_STRENGTH));
    }
}
