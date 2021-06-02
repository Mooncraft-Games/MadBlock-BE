package org.madblock.newgamesapi.kits.defaultgroup;

import cn.nukkit.Player;
import cn.nukkit.item.*;
import cn.nukkit.potion.Effect;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.kits.Kit;

import java.util.Optional;

public class KitSwordsman extends Kit {

    @Override
    public String getKitID() {
        return "swordsman";
    }

    @Override
    public String getKitDisplayName() {
        return "Knight Kit";
    }

    @Override
    public String getKitDescription() {
        return "Noble and basic, the swordsman has a powerful sword, a good chunk of strength, as well as a touch of swiftness";
    }

    @Override
    public void onKitEquip(Player player) {
        player.addEffect(Effect.getEffect(Effect.SWIFTNESS).setAmplifier(1).setDuration(100000).setVisible(false));
        player.addEffect(Effect.getEffect(Effect.STRENGTH).setAmplifier(3).setDuration(100000).setVisible(false));
    }

    @Override
    public void onKitUnequip(Player player) {
        player.removeAllEffects();
    }

    @Override
    public Item[] getKitItems() {
        return new Item[]{
                new ItemSwordGold().setCustomName(TextFormat.ITALIC+"Fernando")
        };
    }

    @Override public Optional<Item> getKitHelmet() { return Optional.of(new ItemHelmetIron()); }
    @Override public Optional<Item> getKitChestplate() { return Optional.of(new ItemChestplateChain()); }
    @Override public Optional<Item> getKitLeggings() { return Optional.of(new ItemLeggingsLeather()); }
    @Override public Optional<Item> getKitBoots() { return Optional.of(new ItemBootsIron()); }
}
