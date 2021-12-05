package org.madblock.blockswap.powerups.impl;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.potion.Effect;
import org.madblock.blockswap.powerups.PowerUp;
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
        return "Get Speed IV for 15 seconds!";
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

        if (isUsingRunnerKit) {
            this.player.removeEffect(Effect.SPEED);
        }

        this.player.addEffect(Effect.getEffect(Effect.SPEED)
                .setDuration(15 * 20)
                .setVisible(false)
                .setAmplifier(3));

        if (isUsingRunnerKit) {
            this.behaviour.getSessionHandler().getGameScheduler().registerGameTask(() -> {
                this.player.addEffect(Effect.getEffect(Effect.SPEED)
                        .setDuration(Integer.MAX_VALUE)
                        .setVisible(false)
                        .setAmplifier(1));
            }, 15 * 20 + 1);
        }
    }

}
