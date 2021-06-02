package org.madblock.newgamesapi.kits.builtingroup;

import cn.nukkit.Player;
import cn.nukkit.block.BlockAir;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.game.NavigationManager;
import org.madblock.newgamesapi.kits.ExtendedKit;
import org.madblock.newgamesapi.kits.Kit;

import java.util.Optional;

public class KitFinalSpectator extends Kit {

    public static final String NBT_FUNCTION_LOCATION = "ngapi_func";

    public static final String HUB_TELEPORTER_FUNC_NAME = "hubtp";
    public static final String SPECTATE_FUNC_NAME = "player_spec";

    @Override public String getKitID() {
        return "spectate";
    }
    @Override public String getKitDisplayName() {
        return "???";
    }
    @Override public String getKitDescription() {
        return "You can leave the game. :)";
    }

    @Override public boolean isVisibleInKitSelector() { return false; }

    @Override
    public Item[] getHotbarItems() {
        Item hubReturn = Item.get(ItemID.BED);
        CompoundTag thr = hubReturn.hasCompoundTag() ? hubReturn.getNamedTag() : new CompoundTag();
        thr.putString(NBT_FUNCTION_LOCATION, HUB_TELEPORTER_FUNC_NAME);
        hubReturn.setCompoundTag(thr);
        hubReturn.setCustomName(String.format("%s%sLeave Game", TextFormat.BLUE, TextFormat.BOLD));

        Item spectate = Item.get(ItemID.COMPASS);
        CompoundTag tsp = spectate.hasCompoundTag() ? spectate.getNamedTag() : new CompoundTag();
        tsp.putString(NBT_FUNCTION_LOCATION, SPECTATE_FUNC_NAME);
        spectate.setCompoundTag(tsp);
        spectate.setCustomName(String.format("%s%sSpectate Player", TextFormat.GOLD, TextFormat.BOLD));

        return new Item[]{
                spectate,
                new BlockAir().toItem(),
                new BlockAir().toItem(),
                new BlockAir().toItem(),
                new BlockAir().toItem(),
                new BlockAir().toItem(),
                new BlockAir().toItem(),
                new BlockAir().toItem(),
                hubReturn
        };
    }

    @Override
    public void onKitEquip(Player player) {
    }

    @Override
    public void onKitUnequip(Player player) {
    }

    @Override
    public Optional<Class<? extends ExtendedKit>> getExtendedKitFeatures() {
        return Optional.of(ExtendedKitSpectate.class);
    }

    public static class ExtendedKitSpectate extends ExtendedKit {

        boolean itemCooldown = false;

        @EventHandler(ignoreCancelled = true)
        public void onItemInteract(PlayerInteractEvent event){

            if(checkEventIsForTargetPlayer(event.getPlayer()) && !itemCooldown){

                if(event.getItem() != null && event.getItem().hasCompoundTag()){
                    CompoundTag c = event.getItem().getNamedTag();

                    if(c.contains(NBT_FUNCTION_LOCATION)){
                        itemCooldown = true;
                        NewGamesAPI1.get().getServer().getScheduler().scheduleDelayedTask(NewGamesAPI1.get(), () -> itemCooldown = false, 10);

                        switch (c.getString(NBT_FUNCTION_LOCATION).toLowerCase()){
                            case HUB_TELEPORTER_FUNC_NAME:
                                NavigationManager.get().openQuickLobbyMenu(event.getPlayer());
                                break;
                            case SPECTATE_FUNC_NAME:
                                NavigationManager.get().openSpectateMenu(event.getPlayer());
                                break;
                        }
                    }
                }
            }
        }

    }

}
