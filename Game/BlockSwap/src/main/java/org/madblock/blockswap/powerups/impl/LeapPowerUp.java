package org.madblock.blockswap.powerups.impl;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.math.Vector3;
import org.madblock.blockswap.powerups.PowerUp;
import org.madblock.blockswap.utils.BlockSwapConstants;
import org.madblock.newgamesapi.game.GameBehavior;

public class LeapPowerUp extends PowerUp {

    public LeapPowerUp(GameBehavior behaviour, Player player) {
        super(behaviour, player);
    }

    @Override
    public String getName() {
        return "Leap";
    }

    @Override
    public String getDescription() {
        return "Leap in any direction!";
    }

    @Override
    public boolean isInstantConsumable() {
        return false;
    }

    @Override
    public int getDisplayItemID() {
        return Item.FEATHER;
    }

    @Override
    public void use() {
        Vector3 directionVector = this.player.getDirectionVector();
        this.player.setMotion(new Vector3(directionVector.getX(), Math.abs(directionVector.getY() / 2), directionVector.getZ()).multiply(BlockSwapConstants.LEAP_STRENGTH));
    }
}
