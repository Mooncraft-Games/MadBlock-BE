package org.madblock.blockswap.powerups;

import cn.nukkit.Player;
import cn.nukkit.potion.Effect;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameBehavior;

public class AntidotePowerUp extends PowerUp {

    public AntidotePowerUp(GameBehavior behaviour, Player player) {
        super(behaviour, player);
    }

    @Override
    public String getName() {
        return "Antidote";
    }

    @Override
    public String getDescription() {
        return "Use this to remove negative effects given to you!";
    }

    @Override
    public boolean isInstantConsumable() {
        return false;
    }

    @Override
    public void use() {
        boolean used = false;
        if (this.player.hasEffect(Effect.SLOWNESS)) {
            this.player.removeEffect(Effect.SLOWNESS);
            used = true;
        }
        if (this.player.hasEffect(Effect.BLINDNESS)) {
            this.player.removeEffect(Effect.BLINDNESS);
            used = true;
        }
        if (used) {
            this.player.sendMessage(
                    Utility.generateServerMessage("POWERUP", TextFormat.YELLOW,
                            "" + TextFormat.GRAY + "You feel refreshed!"
                    )
            );
        } else {
            this.player.sendMessage(
                    Utility.generateServerMessage("POWERUP", TextFormat.YELLOW,
                            "" + TextFormat.GRAY + "You feel as if nothing happened!"
                    )
            );
        }
    }
}
