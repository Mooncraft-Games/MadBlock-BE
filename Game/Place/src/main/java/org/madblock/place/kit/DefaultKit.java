package org.madblock.place.kit;

import cn.nukkit.item.Item;
import org.madblock.newgamesapi.kits.Kit;
import org.madblock.place.util.PlaceUtils;

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
        return PlaceUtils.getFirstPage();
    }

}
