package org.madblock.crystalwars.game.pointentities.shop.item;

import cn.nukkit.Player;

public interface IShopData {
    /**
     * Called when a item is selected
     *
     * @param player the player who is attempting to purchase the item
     *
     * @return whether or not the purchase should go through
     */
    boolean onQuery(Player player);

    /**
     * Called when a item should be purchased
     *
     * @param player the player who purchased the item
     */
    void onPurchase(Player player);

    /**
     * Name of the item in the GUI
     *
     * @return name of the item to show in the GUI
     */
    String getLabel();

    String getPurchaseMessage(Player player);

    String getFailedToPurchaseMessage(Player player);

    String getImage();
}