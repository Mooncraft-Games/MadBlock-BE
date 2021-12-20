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
import org.madblock.newgamesapi.util.Utility;

public class GoldShopPointEntity extends ShopPointEntity {
    public static final String ID = "madblock_crystalwars_goldshop";

    public GoldShopPointEntity(CrystalWarsGame base) {
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
                new ShopItem(new ItemAppleGold(), new ItemIngotGold(0, 8), gameBehavior),
                new ShopItem(new ItemSnowball(0, 3), new ItemIngotGold(0, 1), gameBehavior),
                new ShopItem(new ItemEnderPearl(), new ItemIngotGold(0, 3), gameBehavior),
                new ShopItem(CrystalWarsUtility.makeUnbreakable(new ItemSwordDiamond()), new ItemIngotGold(0, 10), gameBehavior),
                new ShopItem(CrystalWarsUtility.makeUnbreakable(new ItemPickaxeDiamond()), new ItemIngotGold(0, 10), gameBehavior),
                new ShopItem(new Item[] {
                        CrystalWarsUtility.makeUnbreakable(new ItemHelmetIron()),
                        CrystalWarsUtility.makeUnbreakable(new ItemChestplateIron()),
                        CrystalWarsUtility.makeUnbreakable(new ItemLeggingsIron()),
                        CrystalWarsUtility.makeUnbreakable(new ItemBootsIron())
                }, new ItemIngotGold(0, 32), "Full Iron Armor", null, gameBehavior),
                new ShopItem(knockbackStick, new ItemIngotGold(0, 10), gameBehavior),
                new ShopItem(tntItem, new ItemIngotGold(0, 10), gameBehavior),
                new ShopItem(blastProofGlass, new ItemIngotGold(0, 6), gameBehavior),
                new ShopItem(trackingCompass, new ItemIngotGold(0, 1), gameBehavior),
                new ShopItem(new ItemPotion(ItemPotion.INVISIBLE), new ItemIngotGold(0, 3), "Invisibility Potion", null, gameBehavior),
                new ShopItem(new ItemPotion(ItemPotion.LEAPING), new ItemIngotGold(0, 2), "Leaping Potion", null, gameBehavior),
        };
    }

    @Override
    protected String getTitle(Player player) {
        int gold = player.getInventory().slots.values().stream()
                .filter(item -> item.getId() == ItemID.GOLD_INGOT)
                .map(Item::getCount)
                .reduce(0, Integer::sum);
        return "Gold Shop - " + TextFormat.GOLD + gold + TextFormat.RESET + " " + Utility.ResourcePackCharacters.GOLD_INGOT;
    }

    @Override
    protected String getShopNameHeader() {
        return "" + TextFormat.GOLD + TextFormat.BOLD + "Gold Shop";
    }
}