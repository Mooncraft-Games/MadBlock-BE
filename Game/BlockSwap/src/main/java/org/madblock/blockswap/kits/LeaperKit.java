package org.madblock.blockswap.kits;

import cn.nukkit.item.Item;
import cn.nukkit.item.ItemFeather;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.kits.Kit;

public class LeaperKit extends Kit {


    @Override
    public String getKitID () { return "leaper"; }

    @Override
    public String getKitDisplayName () { return "Leaper"; }

    @Override
    public String getKitDescription () { return "Jump higher and higher and higher! Leap into the air by tapping your " + TextFormat.YELLOW + "feather" + TextFormat.RESET + "!"; }

    @Override
    public int getCost () {
        return 1000;
    }

    @Override
    public Item[] getKitItems () {
        Item[] items = new Item[1];

        ItemFeather leapAbility = (ItemFeather) new ItemFeather()
                .setCustomName(String.format("%sLeap", TextFormat.GREEN));
        
        leapAbility.setCompoundTag(
                leapAbility.getNamedTag()
                        .putString("ability", "leap")
                        .putList(new ListTag<>("ench"))
        );

        items[0] = leapAbility;
        return items;
    }

}
