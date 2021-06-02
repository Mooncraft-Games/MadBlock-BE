package org.madblock.skywars.kits;

import cn.nukkit.block.BlockAir;
import cn.nukkit.item.*;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.kits.Kit;
import org.madblock.skywars.utils.Constants;

public class DefaultKit extends Kit {
    @Override
    public String getKitID() {
        return "skyraider";
    }

    @Override
    public String getKitDisplayName() {
        return "Sky Raider";
    }

    @Override
    public String getKitDescription() {
        return "I was prepared for a fight in the skies! Start out with " + TextFormat.YELLOW + "Stone Tools" + TextFormat.RESET + " when you spawn!";
    }

    @Override
    public Item[] getHotbarItems() {

        Item compassItem = new ItemCompass()
                .setCustomName(String.format("%s%sPlayer Compass", TextFormat.BOLD, TextFormat.GREEN));
        CompoundTag tag = compassItem.getNamedTag();
        tag.putBoolean(Constants.COMPASS_ITEM_NBT_ID, true);
        compassItem.setCompoundTag(tag);

        return new Item[]{
                new ItemSwordStone(),
                new ItemPickaxeStone(),
                new ItemAxeStone(),
                new ItemShovelStone(),
                new BlockAir().toItem(),
                new BlockAir().toItem(),
                new BlockAir().toItem(),
                new BlockAir().toItem(),
                compassItem
        };
    }
}
