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
import org.madblock.newgamesapi.team.Team;

import java.util.Optional;

/**
 * @author Nicholas
 */
public class BrickShopPointEntity extends ShopPointEntity {
    public static String ID = "madblock_crystalwars_brickshop";

    public BrickShopPointEntity(CrystalWarsGame base) {
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
                new ShopItem(woolBlock, new ItemBrick(0, 5), gameBehavior),
                new ShopItem(woodBlock, new ItemBrick(0, 10), gameBehavior),
                new ShopItem(CrystalWarsUtility.makeUnbreakable(new ItemSwordIron()), new ItemBrick(0, 5), gameBehavior),
                new ShopItem(CrystalWarsUtility.makeUnbreakable(new ItemPickaxeIron()), new ItemBrick(0, 8), gameBehavior),
                new ShopItem(new Item[] {
                        CrystalWarsUtility.makeUnbreakable(new ItemHelmetIron()),
                        CrystalWarsUtility.makeUnbreakable(new ItemChestplateIron()),
                        CrystalWarsUtility.makeUnbreakable(new ItemLeggingsIron()),
                        CrystalWarsUtility.makeUnbreakable(new ItemBootsIron())
                }, new ItemBrick(0, 25), "Full Iron Armor", null, gameBehavior),
                new ShopItem(CrystalWarsUtility.makeUnbreakable(new ItemShears()), new ItemBrick(0, 10), gameBehavior),
                new ShopItem(new ItemBlock(new BlockEndStone(), 0, 8), new ItemBrick(0, 12), gameBehavior)
        };
    }

    @Override
    protected String getTitle(Player player) {
        int bricks = player.getInventory().slots.values().stream()
                .filter(item -> item.getId() == ItemID.BRICK)
                .map(Item::getCount)
                .reduce(0, Integer::sum);
        return "Brick Shop - " + TextFormat.RED + bricks + TextFormat.RESET + " Bricks";
    }

    @Override
    protected String getShopNameHeader() {
        return "" + TextFormat.GOLD + TextFormat.BOLD + "Brick Shop";
    }
}