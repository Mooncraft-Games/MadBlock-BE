package org.madblock.newgamesapi.kits.defaultgroup;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.player.PlayerToggleSneakEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemSwordIron;
import cn.nukkit.level.Sound;
import cn.nukkit.potion.Effect;
import cn.nukkit.scheduler.TaskHandler;
import org.madblock.newgamesapi.kits.ExtendedKit;
import org.madblock.newgamesapi.kits.Kit;

import java.util.Optional;

public class KitTank extends Kit {

    @Override
    public String getKitID() {
        return "tank";
    }

    @Override
    public String getKitDisplayName() {
        return "Tank Kit";
    }

    @Override
    public String getKitDescription() {
        return "For a playstyle where you are right in the action, while the tank may be slower, their extended health and resistance allows them to withstand any hit. " +
                "Further, its sentry ability locks them in place with extra resistance and strength by crouching.";
    }

    @Override
    public void onKitEquip(Player player) {
        player.addEffect(Effect.getEffect(Effect.DAMAGE_RESISTANCE).setAmplifier(1).setDuration(100000).setVisible(false));
        player.addEffect(Effect.getEffect(Effect.SLOWNESS).setAmplifier(1).setDuration(100000).setVisible(false));
    }

    @Override
    public void onKitUnequip(Player player) {
        player.removeAllEffects();
    }

    @Override
    public Item[] getKitItems() {
        return new Item[]{
                new ItemSwordIron().setCustomName("Hefty Sword"),
        };
    }

    @Override
    public Optional<Class<? extends ExtendedKit>> getExtendedKitFeatures() {
        return Optional.of(KitTankExtended.class);
    }

    public static class KitTankExtended extends ExtendedKit {
        private boolean guarding;
        private TaskHandler handler;

        @Override
        protected void onPrepareExtendedKit() {
            guarding = false;
            handler = getGameHandler().getGameScheduler().registerGameTask(() -> {
                if(guarding) {
                    getTarget().addEffect(Effect.getEffect(Effect.DAMAGE_RESISTANCE).setAmplifier(3).setDuration(3).setVisible(true));
                    getTarget().addEffect(Effect.getEffect(Effect.STRENGTH).setAmplifier(3).setDuration(3).setVisible(true));
                }
            }, 0, 2);
        }

        @Override
        protected void onRemoveExtendedKit() {
            guarding = false;
            getTarget().setImmobile(false);
            handler.cancel();
        }

        @EventHandler
        public void onCrouch(PlayerToggleSneakEvent event){
            if(!checkEventIsForTargetPlayer(event.getPlayer())) { return; }
            guarding = event.isSneaking();
            if(guarding){
                getTarget().setImmobile(true);
                //Guard ability just started
                event.getPlayer().getLevel().addSound(event.getPlayer().getPosition(), Sound.MOB_WITHER_HURT, 0.5f, 0.7f, getGameHandler().getPlayers());
                event.getPlayer().getLevel().addSound(event.getPlayer().getPosition(), Sound.ARMOR_EQUIP_IRON, 0.8f, 1f, getGameHandler().getPlayers());
            } else {
                //Guard ability stopped.
                getTarget().setImmobile(false);
                event.getPlayer().getLevel().addSound(event.getPlayer().getPosition(), Sound.MOB_WITHER_BREAK_BLOCK, 0.6f, 0.9f, getGameHandler().getPlayers());
            }
        }
    }

}
