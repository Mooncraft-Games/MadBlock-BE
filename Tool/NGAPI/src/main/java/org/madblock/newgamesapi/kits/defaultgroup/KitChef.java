package org.madblock.newgamesapi.kits.defaultgroup;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.player.PlayerInteractEntityEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.item.*;
import cn.nukkit.level.Sound;
import cn.nukkit.potion.Effect;
import cn.nukkit.utils.DyeColor;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.kits.ExtendedKit;
import org.madblock.newgamesapi.kits.Kit;
import org.madblock.newgamesapi.rewards.RewardChunk;

import java.util.Optional;

public class KitChef extends Kit {

    @Override
    public String getKitID() {
        return "chef";
    }

    @Override
    public String getKitDisplayName() {
        return "Chef Kit";
    }

    @Override
    public String getKitDescription() {
        return "Who wants to play assist? The chef does! The chef can heal other players with cake and heal themselves with apples. They also have constant regen!";
    }

    @Override
    public void onKitEquip(Player player) {
        player.addEffect(Effect.getEffect(Effect.REGENERATION).setAmplifier(1).setDuration(100000).setVisible(false));
    }

    @Override
    public void onKitUnequip(Player player) {
        player.removeAllEffects();
    }

    @Override
    public Item[] getKitItems() {
        return new Item[]{
                new ItemSwordIron().setCustomName(TextFormat.LIGHT_PURPLE+"Kitchen Knife"),
                new ItemCake(0, 4), // Use to heal other players?
                new ItemApple(0, 8)
        };
    }

    @Override public Optional<Item> getKitHelmet() { return Optional.of(new ItemHelmetLeather().setColor(DyeColor.WHITE)); }
    @Override public Optional<Item> getKitChestplate() { return Optional.of(new ItemChestplateIron()); }
    @Override public Optional<Item> getKitLeggings() { return Optional.of(new ItemLeggingsLeather().setColor(DyeColor.WHITE)); }
    @Override public Optional<Item> getKitBoots() { return Optional.of(new ItemBootsLeather().setColor(DyeColor.WHITE)); }

    @Override
    public Optional<Class<? extends ExtendedKit>> getExtendedKitFeatures() {
        return Optional.of(KitChefExtended.class);
    }

    public static class KitChefExtended extends ExtendedKit {

        public boolean appleCooldownActive;

        @Override
        protected void onPrepareExtendedKit() {
            appleCooldownActive = false;
        }

        @EventHandler
        public void onCakeCheck(PlayerInteractEntityEvent event){
            if(!checkEventIsForTargetPlayer(event.getPlayer())){ return; }
            if(event.getItem() instanceof ItemCake){
                if(event.getEntity() instanceof Player){
                    Player interactedPlayer = (Player) event.getEntity();
                    interactedPlayer.addEffect(Effect.getEffect(Effect.REGENERATION).setAmplifier(3).setDuration(5).setVisible(true));

                    interactedPlayer.getLevel().addSound(interactedPlayer.getPosition(), Sound.MOB_WANDERINGTRADER_DRINK_POTION, 0.7f, 1f, getTarget(), interactedPlayer);
                    interactedPlayer.getLevel().addSound(interactedPlayer.getPosition(), Sound.NOTE_PLING, 0.7f, 1.5f, getTarget(), interactedPlayer);
                    getGameHandler().addRewardChunk(getTarget(), new RewardChunk("assist_heal", "Healed Teammate", 10, 3));
                }
            }
        }
        @EventHandler
        public void onAppleCheck(PlayerInteractEvent event){
            if(!checkEventIsForTargetPlayer(event.getPlayer())){ return; }
            if(event.getItem() instanceof ItemApple && !appleCooldownActive){
                event.getItem().setCount(event.getItem().getCount());
                getTarget().setHealth(Math.min(getTarget().getHealth()+3f, getTarget().getMaxHealth()));
                appleCooldownActive = true;
                getGameHandler().getGameScheduler().registerGameTask(() -> { appleCooldownActive = false; }, 30);
                getTarget().getLevel().addSound(getTarget().getPosition(), Sound.RANDOM_EAT, 0.7f, 1f, getTarget(), getTarget());
                getTarget().getLevel().addSound(getTarget().getPosition(), Sound.NOTE_PLING, 0.7f, 1.5f, getTarget(), getTarget());

            }
        }

        @Override
        protected void onRemoveExtendedKit() {
            appleCooldownActive = false;
        }
    }
}
