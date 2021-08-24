package org.madblock.crystalwars.game.pointentities.shop.types;

import cn.nukkit.Player;
import cn.nukkit.block.BlockEndStone;
import cn.nukkit.block.BlockPlanks;
import cn.nukkit.block.BlockWool;
import cn.nukkit.item.*;
import cn.nukkit.utils.DyeColor;
import cn.nukkit.utils.TextFormat;
import org.madblock.crystalwars.game.CrystalWarsGame;
import org.madblock.crystalwars.game.pointentities.shop.ShopPointEntity;
import org.madblock.crystalwars.game.pointentities.shop.item.ShopItem;
import org.madblock.crystalwars.util.CrystalWarsUtility;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.team.Team;

import java.util.Optional;

public class IronShopPointEntity extends ShopPointEntity {
    public static String ID = "madblock_crystalwars_ironshop";

    public IronShopPointEntity(CrystalWarsGame base) {
        super(ID, base.getSessionHandler(), base);
    }

    @Override
    protected boolean reopenOnQuery() {
        return true;
    }

    @Override
    protected ShopItem[] getShopItems(Player player) {
        Optional<Team> playerTeam = gameHandler.getPlayerTeam(player).filter(Team::isActiveGameTeam);

        DyeColor dyeColor = DyeColor.WHITE;

        if (playerTeam.isPresent()) {
            dyeColor = CrystalWarsUtility.resolveTeamColor(playerTeam.get());
        }

        Item woolBlock = new BlockWool(dyeColor).toItem();
        woolBlock.setCount(32);

        Item woodBlock = new BlockPlanks().toItem();
        woodBlock.setCount(32);

        return new ShopItem[]{
                new ShopItem(woolBlock, new ItemIngotIron(0, 5), gameBehavior),
                new ShopItem(woodBlock, new ItemIngotIron(0, 10), gameBehavior),
                new ShopItem(CrystalWarsUtility.makeUnbreakable(new ItemSwordIron()), new ItemIngotIron(0, 5), gameBehavior),
                new ShopItem(CrystalWarsUtility.makeUnbreakable(new ItemPickaxeIron()), new ItemIngotIron(0, 8), gameBehavior),
                new ShopItem(CrystalWarsUtility.makeUnbreakable(new ItemAxeIron()), new ItemIngotIron(0, 8), gameBehavior),
                new ShopItem(new Item[] {
                        CrystalWarsUtility.makeUnbreakable(new ItemHelmetChain()),
                        CrystalWarsUtility.makeUnbreakable(new ItemChestplateChain()),
                        CrystalWarsUtility.makeUnbreakable(new ItemLeggingsChain()),
                        CrystalWarsUtility.makeUnbreakable(new ItemBootsChain())
                }, new ItemIngotIron(0, 25), "Full Chain Armor", null, gameBehavior),
                new ShopItem(CrystalWarsUtility.makeUnbreakable(new ItemShears()), new ItemIngotIron(0, 10), gameBehavior),
                new ShopItem(new ItemBlock(new BlockEndStone(), 0, 16), new ItemIngotIron(0, 12), gameBehavior)
        };
    }

    @Override
    protected String getTitle(Player player) {
        int iron = player.getInventory().slots.values().stream()
                .filter(item -> item.getId() == ItemID.IRON_INGOT)
                .map(Item::getCount)
                .reduce(0, Integer::sum);
        return "Iron Shop - " + TextFormat.WHITE + iron + TextFormat.RESET + " " + Utility.ResourcePackCharacters.IRON_INGOT;
    }

    @Override
    protected String getShopNameHeader() {
        return "" + TextFormat.WHITE + TextFormat.BOLD + "Iron Shop";
    }
}