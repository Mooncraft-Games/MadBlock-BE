package org.madblock.crystalwars.game.pointentities.shop.types;

import cn.nukkit.Player;
import cn.nukkit.block.BlockGlass;
import cn.nukkit.block.BlockTNT;
import cn.nukkit.item.*;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.utils.TextFormat;
import org.madblock.crystalwars.game.CrystalWarsGame;
import org.madblock.crystalwars.game.pointentities.shop.ShopPointEntity;
import org.madblock.crystalwars.game.pointentities.shop.item.IShopData;
import org.madblock.crystalwars.game.pointentities.shop.item.ShopItem;
import org.madblock.crystalwars.util.CrystalWarsUtility;

/**
 * @author Nicholas
 */
public class EmeraldShopPointEntity extends ShopPointEntity {
    public static final String ID = "madblock_crystalwars_emeraldshop";

    public EmeraldShopPointEntity(CrystalWarsGame base) {
        super(ID, base.getSessionHandler(), base);
    }

    @Override
    protected boolean reopenOnQuery() {
        return true;
    }

    @Override
    protected IShopData[] getShopItems(Player player) {
        Item tntItem = new BlockTNT().toItem();
        Item knockbackStick = CrystalWarsUtility.makeUnbreakable(new ItemStick());
        knockbackStick.addEnchantment(Enchantment.get(Enchantment.ID_KNOCKBACK));
        knockbackStick.setCustomName(TextFormat.YELLOW + "Knockback Stick");

        Item blastProofGlass = new BlockGlass().toItem();
        blastProofGlass.setCount(6);
        blastProofGlass.setCustomName(TextFormat.LIGHT_PURPLE + "Blast Proof Glass");

        Item trackingCompass = new ItemCompass();
        trackingCompass.setCustomName(TextFormat.GREEN + "Tracking Compass");

        return new IShopData[]{
                new ShopItem(new ItemAppleGold(), new ItemEmerald(0, 8), gameBehavior),
                new ShopItem(new ItemSnowball(0, 6), new ItemEmerald(0, 1), gameBehavior),
                new ShopItem(new ItemEnderPearl(), new ItemEmerald(0, 3), gameBehavior),
                new ShopItem(CrystalWarsUtility.makeUnbreakable(new ItemSwordDiamond()), new ItemEmerald(0, 10), gameBehavior),
                new ShopItem(CrystalWarsUtility.makeUnbreakable(new ItemPickaxeDiamond()), new ItemEmerald(0, 10), gameBehavior),
                new ShopItem(new Item[] {
                        CrystalWarsUtility.makeUnbreakable(new ItemHelmetDiamond()),
                        CrystalWarsUtility.makeUnbreakable(new ItemChestplateDiamond()),
                        CrystalWarsUtility.makeUnbreakable(new ItemLeggingsDiamond()),
                        CrystalWarsUtility.makeUnbreakable(new ItemBootsDiamond())
                }, new ItemEmerald(0, 32), "Full Diamond Armor", null, gameBehavior),
                new ShopItem(knockbackStick, new ItemEmerald(0, 10), gameBehavior),
                new ShopItem(tntItem, new ItemEmerald(0, 10), gameBehavior),
                new ShopItem(blastProofGlass, new ItemEmerald(0, 6), gameBehavior),
                new ShopItem(trackingCompass, new ItemEmerald(0, 1), gameBehavior),
                new ShopItem(new ItemPotion(ItemPotion.INVISIBLE), new ItemEmerald(0, 3), "Invisibility Potion", null, gameBehavior),
                new ShopItem(new ItemPotion(ItemPotion.LEAPING), new ItemEmerald(0, 2), "Leaping Potion", null, gameBehavior),
        };
    }

    @Override
    protected String getTitle(Player player) {
        int emeralds = player.getInventory().slots.values().stream()
                .filter(item -> item.getId() == ItemID.EMERALD)
                .map(Item::getCount)
                .reduce(0, Integer::sum);
        return "Emerald Shop - " + TextFormat.GREEN + emeralds + TextFormat.RESET + " Emeralds";
    }

    @Override
    protected String getShopNameHeader() {
        return "" + TextFormat.BOLD + TextFormat.GREEN + "Emerald Shop";
    }
}