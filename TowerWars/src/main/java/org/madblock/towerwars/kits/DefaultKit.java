package org.madblock.towerwars.kits;

import cn.nukkit.entity.passive.EntitySheep;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.kits.ExtendedKit;
import org.madblock.newgamesapi.kits.Kit;
import org.madblock.towerwars.kits.features.DefaultKitFeatures;

import java.util.Optional;

public class DefaultKit extends Kit {

    @Override
    public String getKitID() {
        return "default";
    }

    @Override
    public String getKitDisplayName() {
        return "King";
    }

    @Override
    public String getKitDescription() {
        return "I will protect my land!";
    }

    @Override
    public Item[] getHotbarItems() {

        Item purchaseTowersItem = Item.get(ItemID.ARMOR_STAND)
                .setCustomName("" + TextFormat.RESET + TextFormat.BOLD + TextFormat.LIGHT_PURPLE + "Tower Shop" + "\n"
                                  + TextFormat.RESET  + TextFormat.AQUA + "Tap to purchase towers!");
        Item purchaseEnemiesItem = Item.get(ItemID.SPAWN_EGG, EntitySheep.NETWORK_ID)
                .setCustomName("" + TextFormat.RESET + TextFormat.BOLD + TextFormat.LIGHT_PURPLE + "Monster Shop" + "\n"
                        + TextFormat.RESET  + TextFormat.AQUA + "Tap to purchase monsters!");

        return new Item[]{
            purchaseTowersItem,
            purchaseEnemiesItem
        };
    }

    @Override
    public Optional<Class<? extends ExtendedKit>> getExtendedKitFeatures() {
        return Optional.of(DefaultKitFeatures.class);
    }

}
