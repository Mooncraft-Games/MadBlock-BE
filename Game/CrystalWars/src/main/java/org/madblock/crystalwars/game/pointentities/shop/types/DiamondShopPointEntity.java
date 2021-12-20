package org.madblock.crystalwars.game.pointentities.shop.types;

import cn.nukkit.Player;
import cn.nukkit.item.*;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.utils.TextFormat;
import org.madblock.crystalwars.game.CrystalWarsGame;
import org.madblock.crystalwars.game.pointentities.shop.ShopPointEntity;
import org.madblock.crystalwars.game.pointentities.shop.item.IShopData;
import org.madblock.crystalwars.game.pointentities.shop.item.ShopItem;
import org.madblock.crystalwars.game.pointentities.shop.item.ShopTeamUpgrade;
import org.madblock.crystalwars.game.upgrades.CrystalTeamUpgrade;
import org.madblock.crystalwars.util.CrystalWarsUtility;
import org.madblock.newgamesapi.util.Utility;
import org.madblock.newgamesapi.team.Team;

public class DiamondShopPointEntity extends ShopPointEntity {
    public static final String ID = "madblock_crystalwars_diamondshop";

    protected CrystalWarsGame base;

    public DiamondShopPointEntity(CrystalWarsGame baseGame) {
        super(ID, baseGame.getSessionHandler(), baseGame);
        base = baseGame;
        getGameHandler().getGameScheduler().registerGameTask(this::checkForUpgrades, 0, 20);
    }

    @Override
    protected boolean reopenOnQuery() {
        return false;
    }

    @Override
    protected IShopData[] getShopItems(Player player) {
        IShopData[] items = new IShopData[4];
        Team team = gameHandler.getPlayerTeam(player).get();

        if (!base.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.PROTECTION_ONE)) {
            items[0] = new ShopTeamUpgrade(CrystalTeamUpgrade.PROTECTION_ONE, new ItemDiamond(0, 6), base);
        } else if (!base.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.PROTECTION_TWO)) {
            items[0] = new ShopTeamUpgrade(CrystalTeamUpgrade.PROTECTION_TWO, new ItemDiamond(0, 12), base);
        } else {
            items[0] = new ShopTeamUpgrade(CrystalTeamUpgrade.PROTECTION_TWO, base);
        }
        ((ShopTeamUpgrade) items[0]).setPurchaseCallback(this::upgradedProtection);

        if (!base.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.SHARPNESS_ONE)) {
            items[1] = new ShopTeamUpgrade(CrystalTeamUpgrade.SHARPNESS_ONE, new ItemDiamond(0, 6), base);
        } else if (!base.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.SHARPNESS_TWO)) {
            items[1] = new ShopTeamUpgrade(CrystalTeamUpgrade.SHARPNESS_TWO, new ItemDiamond(0, 12), base);
        } else {
            items[1] = new ShopTeamUpgrade(CrystalTeamUpgrade.SHARPNESS_TWO, base);
        }
        ((ShopTeamUpgrade) items[1]).setPurchaseCallback(this::upgradedSharpness);

        if (!base.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.RESOURCES_ONE)) {
            items[2] = new ShopTeamUpgrade(CrystalTeamUpgrade.RESOURCES_ONE, new ItemDiamond(0, 10), base);
        } else if (!base.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.RESOURCES_TWO)) {
            items[2] = new ShopTeamUpgrade(CrystalTeamUpgrade.RESOURCES_TWO, new ItemDiamond(0, 20), base);
        } else {
            items[2] = new ShopTeamUpgrade(CrystalTeamUpgrade.RESOURCES_TWO, base);
        }

        // Actual Items
        items[3] = new ShopItem(new Item[] {
                CrystalWarsUtility.makeUnbreakable(new ItemHelmetDiamond()),
                CrystalWarsUtility.makeUnbreakable(new ItemChestplateDiamond()),
                CrystalWarsUtility.makeUnbreakable(new ItemLeggingsDiamond()),
                CrystalWarsUtility.makeUnbreakable(new ItemBootsDiamond())
        }, new ItemDiamond(0, 32), "Full Diamond Armor", null, gameBehavior);

        return items;
    }

    protected void upgradedSharpness(Player player) {
        gameHandler.getPlayerTeam(player).filter(Team::isActiveGameTeam).ifPresent(team -> {
            Enchantment sharpness = Enchantment.get(Enchantment.ID_DAMAGE_ALL);
            if (gameBehavior.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.SHARPNESS_TWO)) {
                sharpness.setLevel(2);
            }

            for (Player p : team.getPlayers()) {
                for (Item item : p.getInventory().getContents().values()) {
                    if (item.isSword()) {
                        item.addEnchantment(sharpness);
                    }
                }

                p.getInventory().sendContents(p);
            }
        });
    }

    protected void upgradedProtection(Player player) {
        gameHandler.getPlayerTeam(player).filter(Team::isActiveGameTeam).ifPresent(team -> {
            Enchantment protection = Enchantment.get(Enchantment.ID_PROTECTION_ALL);
            if (gameBehavior.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.PROTECTION_TWO)) {
                protection.setLevel(2);
            }

            for (Player p : team.getPlayers()) {
                Item helmet = p.getInventory().getHelmet();
                Item chestplate = p.getInventory().getChestplate();
                Item leggings = p.getInventory().getLeggings();
                Item boots = p.getInventory().getBoots();

                helmet.addEnchantment(protection);
                chestplate.addEnchantment(protection);
                leggings.addEnchantment(protection);
                boots.addEnchantment(protection);

                p.getInventory().setHelmet(helmet);
                p.getInventory().setChestplate(chestplate);
                p.getInventory().setLeggings(leggings);
                p.getInventory().setBoots(boots);
                p.getInventory().sendArmorContents(p);
            }
        });
    }

    protected void checkForUpgrades() {
        for (Player player : gameHandler.getPlayers()) {
            gameHandler.getPlayerTeam(player).filter(Team::isActiveGameTeam).ifPresent(team -> {
                if (gameBehavior.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.PROTECTION_ONE)
                        || gameBehavior.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.PROTECTION_TWO)) {
                    Enchantment protection = Enchantment.get(Enchantment.ID_PROTECTION_ALL);
                    if (gameBehavior.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.PROTECTION_TWO)) {
                        protection.setLevel(2);
                    }

                    for (Player p : team.getPlayers()) {
                        Item helmet = p.getInventory().getHelmet();
                        Item chestplate = p.getInventory().getChestplate();
                        Item leggings = p.getInventory().getLeggings();
                        Item boots = p.getInventory().getBoots();

                        helmet.addEnchantment(protection);
                        chestplate.addEnchantment(protection);
                        leggings.addEnchantment(protection);
                        boots.addEnchantment(protection);

                        p.getInventory().setHelmet(helmet);
                        p.getInventory().setChestplate(chestplate);
                        p.getInventory().setLeggings(leggings);
                        p.getInventory().setBoots(boots);
                        p.getInventory().sendArmorContents(p);
                    }
                }

                if (gameBehavior.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.SHARPNESS_ONE)
                        || gameBehavior.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.SHARPNESS_TWO)) {
                    Enchantment sharpness = Enchantment.get(Enchantment.ID_DAMAGE_ALL);
                    if (gameBehavior.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.SHARPNESS_TWO)) {
                        sharpness.setLevel(2);
                    }

                    for (Player p : team.getPlayers()) {
                        for (Item item : p.getInventory().getContents().values()) {
                            if (item.isSword()) {
                                item.addEnchantment(sharpness);
                            }
                        }

                        p.getInventory().sendContents(p);
                    }
                }
            });
        }
    }

    @Override
    protected String getTitle(Player player) {
        int diamonds = player.getInventory().slots.values().stream()
                .filter(item -> item.getId() == ItemID.DIAMOND)
                .map(Item::getCount)
                .reduce(0, Integer::sum);
        return "Diamond Shop - " + TextFormat.AQUA + diamonds + TextFormat.RESET + " " + Utility.ResourcePackCharacters.DIAMOND;
    }

    @Override
    protected String getShopNameHeader() {
        return "" + TextFormat.BOLD + TextFormat.AQUA + "Diamond Shop";
    }
}