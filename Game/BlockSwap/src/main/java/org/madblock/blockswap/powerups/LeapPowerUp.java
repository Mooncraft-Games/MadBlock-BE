package org.madblock.blockswap.powerups;

import cn.nukkit.Player;
import cn.nukkit.math.Vector3;
import org.madblock.blockswap.utils.BlockSwapConstants;
import org.madblock.newgamesapi.game.GameBehavior;

public class LeapPowerUp extends PowerUp {

    public LeapPowerUp(GameBehavior behaviour, Player player) {
        super(behaviour, player);
    }

    @Override
    public String getName() {
        return "Strength";
    }

    @Override
    public String getDescription() {
        return "Whoosh! You feel like you could jump higher!";
    }

    @Override
    public boolean isInstantConsumable() {
        return false;
    }

    @Override
    public void use() {
        Vector3 directionVector = player.getDirectionVector();
        player.setMotion(new Vector3(directionVector.getX(), Math.abs(directionVector.getY()), directionVector.getZ()).multiply(BlockSwapConstants.LEAP_STRENGTH));
    }
}
