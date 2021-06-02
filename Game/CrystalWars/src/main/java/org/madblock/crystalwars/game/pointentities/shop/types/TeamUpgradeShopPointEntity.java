package org.madblock.crystalwars.game.pointentities.shop.types;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemDiamond;
import cn.nukkit.item.ItemID;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.potion.Effect;
import cn.nukkit.utils.TextFormat;
import org.madblock.crystalwars.game.CrystalWarsGame;
import org.madblock.crystalwars.game.pointentities.shop.ShopPointEntity;
import org.madblock.crystalwars.game.pointentities.shop.item.IShopData;
import org.madblock.crystalwars.game.pointentities.shop.item.ShopTeamUpgrade;
import org.madblock.crystalwars.game.upgrades.CrystalTeamUpgrade;
import org.madblock.newgamesapi.team.Team;

/**
 * @author Nicholas
 */
public class TeamUpgradeShopPointEntity extends ShopPointEntity {
    public static final String ID = "madblock_crystalwars_upgradeshop";

    protected CrystalWarsGame base;

    public TeamUpgradeShopPointEntity(CrystalWarsGame baseGame) {
        super(ID, baseGame.getSessionHandler(), baseGame);
        base = baseGame;
    }

    @Override
    protected boolean reopenOnQuery() {
        return false;
    }

    @Override
    protected IShopData[] getShopItems(Player player) {
        IShopData[] upgrades = new IShopData[4];
        Team team = gameHandler.getPlayerTeam(player).get();

        if (!base.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.PROTECTION_ONE)) {
            upgrades[0] = new ShopTeamUpgrade(CrystalTeamUpgrade.PROTECTION_ONE, new ItemDiamond(0, 6), base);
        } else if (!base.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.PROTECTION_TWO)) {
            upgrades[0] = new ShopTeamUpgrade(CrystalTeamUpgrade.PROTECTION_TWO, new ItemDiamond(0, 12), base);
        } else {
            upgrades[0] = new ShopTeamUpgrade(CrystalTeamUpgrade.PROTECTION_TWO, base);
        }
        ((ShopTeamUpgrade) upgrades[0]).setPurchaseCallback(this::upgradedProtection);

        if (!base.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.SHARPNESS_ONE)) {
            upgrades[1] = new ShopTeamUpgrade(CrystalTeamUpgrade.SHARPNESS_ONE, new ItemDiamond(0, 6), base);
        } else if (!base.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.SHARPNESS_TWO)) {
            upgrades[1] = new ShopTeamUpgrade(CrystalTeamUpgrade.SHARPNESS_TWO, new ItemDiamond(0, 12), base);
        } else {
            upgrades[1] = new ShopTeamUpgrade(CrystalTeamUpgrade.SHARPNESS_TWO, base);
        }
        ((ShopTeamUpgrade) upgrades[1]).setPurchaseCallback(this::upgradedSharpness);

        if (!base.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.HASTE_ONE)) {
            upgrades[2] = new ShopTeamUpgrade(CrystalTeamUpgrade.HASTE_ONE, new ItemDiamond(0, 4), base);
        } else if (!base.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.HASTE_TWO)) {
            upgrades[2] = new ShopTeamUpgrade(CrystalTeamUpgrade.HASTE_TWO, new ItemDiamond(0, 10), base);
        } else {
            upgrades[2] = new ShopTeamUpgrade(CrystalTeamUpgrade.HASTE_TWO, base);
        }
        ((ShopTeamUpgrade) upgrades[2]).setPurchaseCallback(this::upgradedHaste);

        if (!base.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.RESOURCES_ONE)) {
            upgrades[3] = new ShopTeamUpgrade(CrystalTeamUpgrade.RESOURCES_ONE, new ItemDiamond(0, 10), base);
        } else if (!base.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.RESOURCES_TWO)) {
            upgrades[3] = new ShopTeamUpgrade(CrystalTeamUpgrade.RESOURCES_TWO, new ItemDiamond(0, 20), base);
        } else {
            upgrades[3] = new ShopTeamUpgrade(CrystalTeamUpgrade.RESOURCES_TWO, base);
        }

        return upgrades;
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

    protected void upgradedHaste(Player player) {
        gameHandler.getPlayerTeam(player).filter(Team::isActiveGameTeam).ifPresent(team -> {
            Effect haste = Effect.getEffect(Effect.HASTE).setDuration(Integer.MAX_VALUE);
            if (gameBehavior.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.HASTE_TWO)) {
                haste.setAmplifier(1);
            }

            for (Player p : team.getPlayers()) {
                p.addEffect(haste);
            }
        });
    }

    @Override
    protected String getTitle(Player player) {
        int stars = player.getInventory().slots.values().stream()
                .filter(item -> item.getId() == ItemID.DIAMOND)
                .map(Item::getCount)
                .reduce(0, Integer::sum);
        return "Team Upgrade Shop - " + TextFormat.BLUE + stars + TextFormat.RESET + " Diamonds";
    }

    @Override
    protected String getShopNameHeader() {
        return "" + TextFormat.BOLD + TextFormat.AQUA + "Team Upgrade Shop";
    }
}