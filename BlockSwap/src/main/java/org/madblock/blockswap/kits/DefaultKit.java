package org.madblock.blockswap.kits;

import cn.nukkit.item.Item;
import org.madblock.newgamesapi.kits.Kit;

public class DefaultKit extends Kit {

    @Override
    public String getKitID () {
        return "survivor";
    }

    @Override
    public String getKitDisplayName () {
        return "Survivor";
    }

    @Override
    public String getKitDescription () {
        return "How did I get roped in to this game of luck?! No perks are provided for this kit.";
    }

    @Override
    public Item[] getKitItems () {
        Item[] items = new Item[]{};
        return items;
    }



}
