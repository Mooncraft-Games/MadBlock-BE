package org.madblock.newgamesapi.kits.defaultgroup;

import cn.nukkit.Player;
import cn.nukkit.item.*;
import cn.nukkit.potion.Effect;
import cn.nukkit.utils.DyeColor;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.kits.Kit;

import java.util.Optional;

public class KitArcher extends Kit {

    @Override
    public String getKitID() {
        return "archer";
    }

    @Override
    public String getKitDisplayName() {
        return "Archer Kit";
    }

    @Override
    public String getKitDescription() {
        return "Quick, ranged, and a high jumper. The archer plays from a distance.";
    }

    @Override
    public void onKitEquip(Player player) {
        player.setMovementSpeed(0.2f);
        player.addEffect(Effect.getEffect(Effect.JUMP).setAmplifier(2).setDuration(100000).setVisible(false));
    }

    @Override
    public void onKitUnequip(Player player) {
        player.setMovementSpeed(0.1f);
        player.removeAllEffects();
    }

    @Override
    public Item[] getKitItems() {
        return new Item[]{
                new ItemSwordStone().setCustomName(TextFormat.LIGHT_PURPLE+"Hero's Sword"),
                new ItemBow().setCustomName(TextFormat.LIGHT_PURPLE+"Stormbow"),
                new ItemArrow(0, 64),
                new ItemArrow(0, 64)
        };
    }

    @Override public Optional<Item> getKitHelmet() { return Optional.of(new ItemHelmetLeather().setColor(DyeColor.GREEN)); }
    @Override public Optional<Item> getKitChestplate() { return Optional.of(new ItemChestplateLeather().setColor(DyeColor.GREEN)); }
    @Override public Optional<Item> getKitLeggings() { return Optional.of(new ItemLeggingsLeather().setColor(DyeColor.GREEN)); }
    @Override public Optional<Item> getKitBoots() { return Optional.of(new ItemBootsLeather().setColor(DyeColor.BROWN)); }
}
