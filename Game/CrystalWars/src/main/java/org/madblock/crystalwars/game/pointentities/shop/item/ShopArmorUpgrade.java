package org.madblock.crystalwars.game.pointentities.shop.item;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemArmor;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.utils.TextFormat;
import org.madblock.crystalwars.game.CrystalWarsGame;
import org.madblock.crystalwars.game.upgrades.CrystalTeamUpgrade;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.team.Team;

/**
 * @author Nicholas
 */
public class ShopArmorUpgrade extends ShopItem {
    protected final CrystalWarsGame gameBehavior;

    public ShopArmorUpgrade(ItemArmor item, Item cost, CrystalWarsGame base) {
        super(item, cost);
        gameBehavior = base;
    }

    public ShopArmorUpgrade(ItemArmor item, Item cost, String imageLink, CrystalWarsGame base) {
        super(item, cost, imageLink);
        gameBehavior = base;
    }

    @Override
    public boolean onQuery(Player player) {
        ItemArmor givenArmorItem = (ItemArmor)givenItem;

        if (player.getInventory().contains(soldItem)) {
            int comparingArmorPoints;
            if (givenArmorItem.isHelmet()) {

                comparingArmorPoints = player.getInventory().getHelmet().getArmorPoints();

            } else if (givenArmorItem.isChestplate()) {

                comparingArmorPoints = player.getInventory().getChestplate().getArmorPoints();

            } else if (givenArmorItem.isLeggings()) {

                comparingArmorPoints = player.getInventory().getLeggings().getArmorPoints();

            } else {

                comparingArmorPoints = player.getInventory().getBoots().getArmorPoints();

            }
            return givenArmorItem.getArmorPoints() > comparingArmorPoints;
        } else {
            return false;
        }
    }

    @Override
    public void onPurchase(Player player) {
        player.getInventory().removeItem(soldItem);
        ItemArmor givenArmorItem = (ItemArmor)givenItem;

        // Protection
        Team team = gameBehavior.getSessionHandler().getPlayerTeam(player).get();
        if (gameBehavior.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.PROTECTION_TWO)) {
            givenArmorItem.addEnchantment(Enchantment.getEnchantment(Enchantment.ID_PROTECTION_ALL).setLevel(2));
        } else if (gameBehavior.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.PROTECTION_ONE)) {
            givenArmorItem.addEnchantment(Enchantment.getEnchantment(Enchantment.ID_PROTECTION_ALL));
        }

        if (givenArmorItem.isHelmet()) {
            player.getInventory().setHelmet(givenArmorItem);
        } else if (givenArmorItem.isChestplate()) {
            player.getInventory().setChestplate(givenArmorItem);
        } else if (givenArmorItem.isLeggings()) {
            player.getInventory().setLeggings(givenArmorItem);
        } else {
            player.getInventory().setBoots(givenArmorItem);
        }

        player.getInventory().sendArmorContents(player);
    }

    @Override
    public String getFailedToPurchaseMessage(Player player) {
        if (!player.getInventory().contains(soldItem)) {
            return super.getFailedToPurchaseMessage(player);
        } else {
            return Utility.generateServerMessage("Game", TextFormat.BLUE, "You can only upgrade your armor.", TextFormat.RED);
        }
    }
}