package org.madblock.crystalwars.game.kit;

import cn.nukkit.item.Item;
import cn.nukkit.item.ItemArrow;
import cn.nukkit.item.ItemBow;

/**
 * @author Nicholas
 */
public class ArcherKit extends BaseKit {
    @Override
    public String getKitID() {
        return "archer";
    }

    @Override
    public String getKitDisplayName() {
        return "Archer";
    }

    @Override
    public String getKitDescription() {
        return "I'm readying my bow! Shoot, where are my arrows..?";
    }

    @Override
    public Item[] getHotbarItems() {
        return new Item[]{
                new ItemBow(),
                new ItemArrow(0, 32)
        };
    }
}