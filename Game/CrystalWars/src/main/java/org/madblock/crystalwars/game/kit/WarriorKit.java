package org.madblock.crystalwars.game.kit;

import cn.nukkit.item.Item;
import cn.nukkit.item.ItemSwordWood;

public class WarriorKit extends BaseKit {
    @Override
    public String getKitID() {
        return "warrior";
    }

    @Override
    public String getKitDisplayName() {
        return "Warrior";
    }

    @Override
    public String getKitDescription() {
        return "You, the warrior, are set to fight the others!";
    }

    @Override
    public Item[] getHotbarItems() {
        return new Item[]{
                new ItemSwordWood(),
        };
    }
}