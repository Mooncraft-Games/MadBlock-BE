package org.madblock.crystalwars.game.pointentities.shop.item;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.Utility;

/**
 * @author Nicholas
 */
public class ShopUniqueItem extends ShopItem {
    public ShopUniqueItem(Item item, Item cost, String imageLink) {
        super(item, cost, imageLink);
    }

    public ShopUniqueItem(Item item, Item cost) {
        super(item, cost);
    }

    @Override
    public boolean onQuery(Player player) {
        return !player.getInventory().contains(givenItem) && player.getInventory().contains(soldItem);
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