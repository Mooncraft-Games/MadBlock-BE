package org.madblock.skywars.kits;

import cn.nukkit.Player;
import cn.nukkit.block.BlockAir;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemCompass;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.kits.Kit;
import org.madblock.skywars.utils.Constants;

public class DefenderKit extends Kit {
    @Override
    public String getKitID() {
        return "defender";
    }

    @Override
    public String getKitDisplayName() {
        return "Defender";
    }

    @Override
    public int getCost() {
        return 500;
    }

    @Override
    public String getKitDescription() {
        return "Defend yourself against others! You get a small " + TextFormat.YELLOW + "Health Boost" + TextFormat.RESET + " to help you protect yourself against others!";
    }

    @Override
    public void onKitEquip(Player player) {
        player.setMaxHealth(24);
        player.setHealth(24);
    }

    @Override
    public Item[] getHotbarItems() {

        Item compassItem = new ItemCompass()
                .setCustomName(String.format("%s%sPlayer Compass", TextFormat.BOLD, TextFormat.GREEN));
        CompoundTag tag = compassItem.getNamedTag();
        tag.putBoolean(Constants.COMPASS_ITEM_NBT_ID, true);
        compassItem.setCompoundTag(tag);

        return new Item[]{
            new BlockAir().toItem(),
            new BlockAir().toItem(),
            new BlockAir().toItem(),
            new BlockAir().toItem(),
            new BlockAir().toItem(),
            new BlockAir().toItem(),
            new BlockAir().toItem(),
            new BlockAir().toItem(),
            compassItem
        };
    }

    @Override
    public void onKitUnequip(Player player) {
        player.setMaxHealth(20);
    }
}
