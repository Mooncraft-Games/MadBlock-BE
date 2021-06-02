package org.madblock.newgamesapi.kits.builtingroup;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.potion.Effect;
import org.madblock.newgamesapi.kits.Kit;

public class KitEmpty extends Kit {

    @Override public String getKitID() {
        return "empty";
    }
    @Override public String getKitDisplayName() {
        return "???";
    }
    @Override public String getKitDescription() {
        return "It appears you have nothing. :)";
    }

    @Override public Item[] getKitItems() { return new Item[0]; }

    @Override
    public void onKitEquip(Player player) {
        player.setMovementSpeed(0.2f);
        player.addEffect(Effect.getEffect(Effect.JUMP).setAmplifier(2).setDuration(100000).setVisible(false));
    }

    @Override
    public void onKitUnequip(Player player) {
        player.setMovementSpeed(0.1f);
        player.removeAllEffects();
    }

}
