package org.madblock.newgamesapi.kits.hub;

import cn.nukkit.item.Item;

public class KitHubBuilder extends KitHub {

    @Override public String getKitID() {
        return "hub_builder";
    }
    @Override public String getKitDisplayName() {
        return "Builder";
    }
    @Override public boolean isVisibleInKitSelector() { return false; }
    @Override public Item[] getKitItems() { return new Item[]{ }; }
    @Override public Item[] getHotbarItems() { return new Item[]{ }; }

}
