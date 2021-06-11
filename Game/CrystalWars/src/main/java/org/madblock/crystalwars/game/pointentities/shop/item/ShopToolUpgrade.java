package org.madblock.crystalwars.game.pointentities.shop.item;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemTool;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.utils.TextFormat;
import org.madblock.crystalwars.game.CrystalWarsGame;
import org.madblock.crystalwars.game.upgrades.CrystalTeamUpgrade;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.team.Team;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Nicholas
 */
public class ShopToolUpgrade extends ShopItem {
    protected final CrystalWarsGame gameBehavior;

    public ShopToolUpgrade(ItemTool item, Item cost, String imageLink, CrystalWarsGame base) {
        super(item, cost, null, imageLink, base);
        gameBehavior = base;
    }

    public ShopToolUpgrade(ItemTool item, Item cost, CrystalWarsGame base) {
        super(item, cost, base);
        gameBehavior = base;
    }

    @Override
    public boolean onQuery(Player player) {
        ItemTool givenToolItem = (ItemTool) givenItems[0];

        if (player.getInventory().contains(soldItem)) {
            if (givenToolItem.isSword()) {
                for (Item item : player.getInventory().getContents().values()) {
                    if (item.getId() == Item.DIAMOND_SWORD || item.getId() == givenToolItem.getId()) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPurchase(Player player) {
        ItemTool givenToolItem = (ItemTool) givenItems[0];
        player.getInventory().removeItem(soldItem);

        Set<Integer> lookingForItemIds = new HashSet<>();
        if (givenToolItem.isSword()) {

            // Sharpness
            Team team = gameBehavior.getSessionHandler().getPlayerTeam(player).get();
            if (gameBehavior.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.SHARPNESS_TWO)) {
                givenToolItem.addEnchantment(Enchantment.get(Enchantment.ID_DAMAGE_ALL).setLevel(2));
            } else if (gameBehavior.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.SHARPNESS_ONE)) {
                givenToolItem.addEnchantment(Enchantment.get(Enchantment.ID_DAMAGE_ALL));
            }

            // What item are we looking to replace?
            switch (givenToolItem.getTier()) {
                case ItemTool.TIER_IRON:
                    lookingForItemIds.add(Item.WOODEN_SWORD);
                    break;
                case ItemTool.TIER_DIAMOND:
                    lookingForItemIds.add(Item.IRON_SWORD);
                    lookingForItemIds.add(Item.WOODEN_SWORD);
                    break;
            }

        }

        boolean found = false;
        for (Map.Entry<Integer, Item> entry : player.getInventory().getContents().entrySet()) {
            for (int id : lookingForItemIds) {
                if (entry.getValue().getId() == id) {
                    found = true;
                    player.getInventory().setItem(entry.getKey(), givenToolItem);
                }
            }
        }

        if (!found) {
            boolean addedToHotbar = false;
            for (int i = 0; i < 8; i++) {
                if (player.getInventory().getItem(i) == null) {
                    player.getInventory().setItem(i, givenToolItem);
                    addedToHotbar = true;
                    break;
                }
            }
            if (!addedToHotbar) {
                player.getInventory().addItem(givenToolItem);
            }
        }

        player.getInventory().sendContents(player);
    }

    @Override
    public String getFailedToPurchaseMessage(Player player) {
        if (!player.getInventory().contains(soldItem)) {
            return super.getFailedToPurchaseMessage(player);
        } else {
            return Utility.generateServerMessage("Game", TextFormat.BLUE, "You can only upgrade your sword.", TextFormat.RED);
        }
    }
}