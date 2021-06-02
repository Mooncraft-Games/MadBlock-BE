package org.madblock.crystalwars.game.pointentities.shop.item;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.Utility;

/**
 * @author Nicholas
 */
public class ShopItem implements IShopData {
    protected final Item givenItem;
    protected final Item soldItem;
    protected final String image;

    public ShopItem(Item item, Item cost, String imageLink) {
        givenItem = item;
        soldItem = cost;
        image = imageLink;
    }

    public ShopItem(Item item, Item cost) {
        givenItem = item;
        soldItem = cost;
        image = null;
    }

    @Override
    public boolean onQuery(Player player) {
        return player.getInventory().contains(soldItem);
    }

    @Override
    public void onPurchase(Player player) {
        player.getInventory().removeItem(soldItem);
        player.getInventory().addItem(givenItem);
        player.getInventory().sendContents(player);
    }

    @Override
    public String getLabel() {
        StringBuilder label = new StringBuilder();

        if (givenItem.getCount() > 1) {
            label.append(givenItem.getCount()).append(" X ").append(TextFormat.clean(givenItem.getName()));
        } else {
            label.append(TextFormat.clean(givenItem.getName()));
        }

        label.append('\n').append(TextFormat.YELLOW);

        if (soldItem.getCount() > 1) {
            label.append(soldItem.getCount()).append(" ").append(TextFormat.clean(soldItem.getName()));
        } else {
            label.append(TextFormat.clean(soldItem.getName()));
        }

        return label.toString();
    }

    @Override
    public String getPurchaseMessage(Player player) {
        String itemName;
        if (givenItem.getCount() > 1) {
            itemName = givenItem.getCount() + " " + givenItem.getName();
        } else {
            itemName = givenItem.getName();
        }
        return Utility.generateServerMessage("Game", TextFormat.BLUE, "You purchased " + TextFormat.YELLOW +
                TextFormat.clean(itemName) + Utility.DEFAULT_TEXT_COLOUR + ".");
    }

    @Override
    public String getFailedToPurchaseMessage(Player player) {
        return Utility.generateServerMessage("Game", TextFormat.BLUE, "You do not have enough resources to buy " +
                "this item!", TextFormat.RED);
    }

    @Override
    public String getImage() {
        return image;
    }
}