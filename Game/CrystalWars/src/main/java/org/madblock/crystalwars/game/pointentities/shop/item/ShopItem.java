package org.madblock.crystalwars.game.pointentities.shop.item;

import cn.nukkit.Player;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.utils.TextFormat;
import org.madblock.crystalwars.game.CrystalWarsGame;
import org.madblock.crystalwars.game.upgrades.CrystalTeamUpgrade;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.team.Team;

import java.util.Map;

/**
 * @author Nicholas
 */
public class ShopItem implements IShopData {
    protected final CrystalWarsGame gameBehavior;
    protected final Item[] givenItems;
    protected final Item soldItem;
    protected final String name;
    protected final String image;

    public ShopItem(Item[] items, Item cost, String itemName, String imageLink, CrystalWarsGame base) {
        gameBehavior = base;
        givenItems = items;
        soldItem = cost;
        name = itemName;
        image = imageLink;
    }

    public ShopItem(Item[] items, Item cost, CrystalWarsGame base) {
        gameBehavior = base;
        givenItems = items;
        soldItem = cost;
        name = null;
        image = null;
    }

    public ShopItem(Item item, Item cost, String displayName, String imageLink, CrystalWarsGame base) {
        gameBehavior = base;
        givenItems = new Item[]{item};
        soldItem = cost;
        name = displayName;
        image = imageLink;
    }

    public ShopItem(Item item, Item cost, CrystalWarsGame base) {
        gameBehavior = base;
        givenItems = new Item[]{item};
        soldItem = cost;
        name = null;
        image = null;
    }

    @Override
    public boolean onQuery(Player player) {
        return player.getInventory().contains(soldItem);
    }

    @Override
    public void onPurchase(Player player) {
        player.getInventory().removeItem(soldItem);
        PlayerInventory inventory = player.getInventory();
        for (Item item : givenItems) {
            if (item.isArmor()) {
                Team team = gameBehavior.getSessionHandler().getPlayerTeam(player).get();
                if (gameBehavior.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.PROTECTION_TWO)) {
                    item.addEnchantment(Enchantment.getEnchantment(Enchantment.ID_PROTECTION_ALL).setLevel(2));
                } else if (gameBehavior.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.PROTECTION_ONE)) {
                    item.addEnchantment(Enchantment.getEnchantment(Enchantment.ID_PROTECTION_ALL));
                }
            }
            if (item.isHelmet()) {
                inventory.setHelmet(item);
            } else if (item.isChestplate()) {
                inventory.setChestplate(item);
            } else if (item.isLeggings()) {
                inventory.setLeggings(item);
            } else if (item.isBoots()) {
                inventory.setBoots(item);
            } else {
                // Not Armor
                if (item.isSword()) {
                    int slot = 0;
                    boolean replace = false;
                    for (Map.Entry<Integer, Item> entry : inventory.getContents().entrySet()) {
                        if (entry.getValue().isSword()) {
                            slot = entry.getKey();
                            replace = true;
                            inventory.remove(entry.getValue());
                        }
                    }
                    if (replace)
                        inventory.setItem(slot, item);
                    else
                        inventory.addItem(item);
                } else if (item.isPickaxe()) {
                    int slot = 0;
                    boolean replace = false;
                    for (Map.Entry<Integer, Item> entry : inventory.getContents().entrySet()) {
                        if (entry.getValue().isPickaxe()) {
                            slot = entry.getKey();
                            replace = true;
                            inventory.remove(entry.getValue());
                            break;
                        }
                    }
                    if (replace)
                        inventory.setItem(slot, item);
                    else
                        inventory.addItem(item);
                } else if (item.isAxe()) {
                    int slot = 0;
                    boolean replace = false;
                    for (Map.Entry<Integer, Item> entry : inventory.getContents().entrySet()) {
                        if (entry.getValue().isAxe()) {
                            slot = entry.getKey();
                            replace = true;
                            inventory.remove(entry.getValue());
                            break;
                        }
                    }
                    if (replace)
                        inventory.setItem(slot, item);
                    else
                        inventory.addItem(item);
                }
            }
        }
        player.getInventory().sendContents(player);
        player.getInventory().sendArmorContents(player);
    }

    @Override
    public String getLabel() {
        StringBuilder label = new StringBuilder();
        String name;
        Item givenItem = givenItems[0];

        if (this.name != null) {
            name = this.name;
        } else {
            name = givenItem.getName();
        }

        if (givenItem.getCount() > 1) {
            label.append(givenItem.getCount()).append(" X ").append(TextFormat.clean(name));
        } else {
            label.append(TextFormat.clean(name));
        }

        label.append('\n').append(TextFormat.YELLOW);

        label.append(soldItem.getCount()).append(" ").append(TextFormat.clean(soldItem.getName()));

        return label.toString();
    }

    @Override
    public String getPurchaseMessage(Player player) {
        String itemName;
        if (name != null) {
            itemName = name;
        } else {
            Item givenItem = givenItems[0];
            if (givenItem.getCount() > 1) {
                itemName = givenItem.getCount() + " " + givenItem.getName();
            } else {
                itemName = givenItem.getName();
            }
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