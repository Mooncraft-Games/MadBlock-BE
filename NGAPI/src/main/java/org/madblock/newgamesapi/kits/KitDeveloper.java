package org.madblock.newgamesapi.kits;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemApple;
import cn.nukkit.item.ItemAxeIron;
import cn.nukkit.item.ItemCake;
import cn.nukkit.potion.Effect;

public class KitDeveloper extends Kit{

    @Override
    public String getKitID() {
        return "developer";
    }

    @Override
    public String getKitDisplayName() {
        return "Developer Kit";
    }

    @Override
    public String getKitDescription() {
        return "Tap Tap. Keybord go clik. :DD";
    }

    @Override
    public void onKitEquip(Player player) {
        player.addEffect(Effect.getEffect(Effect.SWIFTNESS).setAmplifier(3).setDuration(100000));
    }

    @Override
    public void onKitUnequip(Player player) {
        player.removeAllEffects();
    }

    @Override
    public Item[] getKitItems() {
        return new Item[]{
                new ItemApple().setCustomName("CG360's apple. Don't Steal :((("),
                new ItemAxeIron().setCustomName("CG360's axe"),
                new ItemCake().setCustomName("CG360's blessing")
        };
    }
}
