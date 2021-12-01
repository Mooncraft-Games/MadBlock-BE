package org.madblock.blockswap.powerups.impl;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.potion.Effect;
import org.madblock.blockswap.powerups.PowerUp;
import org.madblock.blockswap.utils.BlockSwapConstants;
import org.madblock.newgamesapi.game.GameBehavior;

public class SpeedPowerUp extends PowerUp {

    public SpeedPowerUp(GameBehavior behaviour, Player player) {
        super(behaviour, player);
    }

    @Override
    public String getName() {
        return "Speed";
    }

    @Override
    public String getDescription() {
        return "Get Speed II for 15 seconds!";
    }

    @Override
    public boolean isInstantConsumable() {
        return false;
    }

    @Override
    public int getDisplayItemID() {
        return Item.RABBIT_FOOT;
    }

    @Override
    public void use() {
        boolean isUsingRunnerKit = this.behaviour.getSessionHandler().getPlayerKit(this.player).filter(kit -> kit.getKitID().equals("runner")).isPresent();

        if (!isUsingRunnerKit) {
            this.player.addEffect(Effect.getEffect(Effect.SPEED)
                    .setDuration(15 * 20)
                    .setVisible(false)
                    .setAmplifier(1));

            if(this.player.isSprinting()) {
                this.player.setMovementSpeed((BlockSwapConstants.VANILLA_BASE_SPEED * BlockSwapConstants.VANILLA_BASE_SPRINT_MULTIPLIER) * BlockSwapConstants.SPEED_MULTIPLIER); //Vanilla is a 30% increase
            } else {
                this.player.setMovementSpeed(BlockSwapConstants.VANILLA_BASE_SPEED * BlockSwapConstants.SPEED_MULTIPLIER);
            }
        }
    }

}
