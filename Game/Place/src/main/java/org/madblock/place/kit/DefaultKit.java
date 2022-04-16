package org.madblock.place.kit;

import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.utils.DyeColor;
import org.madblock.newgamesapi.kits.Kit;

public class DefaultKit extends Kit {

    @Override
    public String getKitID() {
        return "default_place";
    }

    @Override
    public String getKitDisplayName() {
        return "lol this is the default funny kit for r/place XD";
    }

    @Override
    public String getKitDescription() {
        return "haha XD why are you looking at this. go paint?";
    }

    @Override
    public Item[] getKitItems() {
        return new Item[] {
            Item.get(ItemID.DYE, DyeColor.WHITE.getDyeData()),
            Item.get(ItemID.DYE, DyeColor.BLACK.getDyeData()),
            Item.get(ItemID.DYE, DyeColor.BROWN.getDyeData()),
            Item.get(ItemID.DYE, DyeColor.RED.getDyeData()),
            Item.get(ItemID.DYE, DyeColor.ORANGE.getDyeData()),
            Item.get(ItemID.DYE, DyeColor.YELLOW.getDyeData()),
            Item.get(ItemID.DYE, DyeColor.LIME.getDyeData()),
            Item.get(ItemID.DYE, DyeColor.BLUE.getDyeData()),
            Item.get(ItemID.DYE, DyeColor.MAGENTA.getDyeData())
        };
    }

}
