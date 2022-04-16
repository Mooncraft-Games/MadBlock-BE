package org.madblock.place.util;

import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.utils.DyeColor;
import cn.nukkit.utils.TextFormat;

public class PlaceUtils {

    private PlaceUtils() {

    }

    public static Item[] getFirstPage() {
        return new Item[] {
                getDyeItem(DyeColor.WHITE),
                getDyeItem(DyeColor.LIGHT_GRAY),
                getDyeItem(DyeColor.GRAY),
                getDyeItem(DyeColor.BLACK),
                getDyeItem(DyeColor.BROWN),
                getDyeItem(DyeColor.RED),
                getDyeItem(DyeColor.ORANGE),
                getDyeItem(DyeColor.YELLOW),
                getSwapPageItem(0)
        };
    }

    public static Item[] getSecondPage() {
        return new Item[] {
                getDyeItem(DyeColor.LIME),
                getDyeItem(DyeColor.GREEN),
                getDyeItem(DyeColor.CYAN),
                getDyeItem(DyeColor.LIGHT_BLUE),
                getDyeItem(DyeColor.BLUE),
                getDyeItem(DyeColor.PURPLE),
                getDyeItem(DyeColor.MAGENTA),
                getDyeItem(DyeColor.PINK),
                getSwapPageItem(1)
        };
    }

    private static Item getDyeItem(DyeColor color) {
        Item dye = Item.get(ItemID.DYE, color.getDyeData());
        dye.setCustomName(TextFormat.RESET + "" + TextFormat.WHITE + color.getName());
        return dye;
    }

    private static Item getSwapPageItem(int page) {
        Item swapPage = Item.get(ItemID.HEART_OF_THE_SEA);
        swapPage.setCompoundTag(new CompoundTag()
                .putList(new ListTag<>("ench"))
                .putInt("page", page));
        swapPage.setCustomName("" + TextFormat.RESET + TextFormat.AQUA + "Swap Page");

        return swapPage;
    }

}
