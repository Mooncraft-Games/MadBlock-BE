package org.madblock.blockswap.kits;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.player.PlayerToggleSprintEvent;
import cn.nukkit.item.Item;
import cn.nukkit.potion.Effect;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.kits.ExtendedKit;
import org.madblock.newgamesapi.kits.Kit;

import java.util.Optional;

public class RunnerKit extends Kit {

    @Override
    public String getKitID() {
        return "runner";
    }

    @Override
    public String getKitDisplayName() {
        return "Runner";
    }

    @Override
    public String getKitDescription() {
        return "Everyone else is just too slow! I'm faster than everyone else! " + TextFormat.YELLOW + "Speed 2" + TextFormat.RESET + " is applied to your player!";
    }

    @Override
    public int getCost() {
        return 500;
    }

    @Override
    public void onKitEquip (Player player) {
        player.addEffect(Effect.getEffect(Effect.SPEED)
                        .setDuration(99999)
                        .setVisible(false)
                        .setAmplifier(1));
    }

    @Override
    public void onKitUnequip(Player player) {
        player.removeEffect(Effect.SPEED);
        player.setMovementSpeed(0.1f);
    }


    @Override
    public Item[] getKitItems() {
        return new Item[0];
    }

}
