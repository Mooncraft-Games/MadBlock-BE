package org.madblock.crystalwars.game.pointentities.shop.item;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.utils.TextFormat;
import org.madblock.crystalwars.game.CrystalWarsGame;
import org.madblock.newgamesapi.Utility;

public class ShopUniqueItem extends ShopItem {
    public ShopUniqueItem(Item item, Item cost, String imageLink, CrystalWarsGame base) {
        super(item, cost, null, imageLink, base);
    }

    public ShopUniqueItem(Item item, Item cost, CrystalWarsGame base) {
        super(item, cost, base);
    }

    @Override
    public boolean onQuery(Player player) {
        return !player.getInventory().contains(givenItems[0]) && player.getInventory().contains(soldItem);
    }

    @Override
    public String getFailedToPurchaseMessage(Player player) {
        if (player.getInventory().contains(soldItem)) {
            return Utility.generateServerMessage("Game", TextFormat.BLUE, "You already own this item!", TextFormat.RED);
        } else {
            return super.getFailedToPurchaseMessage(player);
        }
    }
}