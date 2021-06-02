package org.madblock.newgamesapi.game.internal.hub.pointentities;

import cn.nukkit.Player;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.kits.KitGroup;
import org.madblock.newgamesapi.kits.PlayerKitsManager;
import org.madblock.newgamesapi.map.pointentities.PointEntityCallData;
import org.madblock.newgamesapi.map.pointentities.defaults.PointEntityTypeNPC;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.newgamesapi.registry.KitRegistry;

import java.util.*;


public class PointEntityTypeKitNPC extends PointEntityTypeNPC {

    public PointEntityTypeKitNPC(GameHandler gameHandler) {
        super("npc_kit_selector", gameHandler);
    }

    @Override
    public String getPersistentUuidNbtLocation() {
        return "ngapi_kit_npc_identifier";
    }

    @Override
    public void onRegister() {
        super.onRegister();
        addFunction("select_kit_gui", this::sendKitGUI);
    }

    protected void sendKitGUI (PointEntityCallData data) {

        String kitGroupId = data.getPointEntity().getStringProperties().getOrDefault("kit_group_id", KitRegistry.DEFAULT.getGroupID());

        try {
            UUID uuid = UUID.fromString(data.getParameters().get("player_uuid"));
            Optional<Player> p = NewGamesAPI1.get().getServer().getPlayer(uuid);
            if(p.isPresent()){
                Optional<KitGroup> kitGroup = KitRegistry.get().getKitGroup(kitGroupId);
                if (kitGroup.isPresent()) {
                    PlayerKitsManager.get().sendKitSelectionWindow(p.get(), kitGroup.get());
                } else {
                    NewGamesAPI1.getPlgLogger().warning("Invalid kit group provided for NPCKitHuman.");
                    p.get().sendMessage(
                            Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "This kit NPC is not set up properly! Whoops!", TextFormat.RED)
                    );
                }
            } else {
                NewGamesAPI1.getPlgLogger().warning("Player passed into interact function doesn't exist. Scooby Dooby where the hell are you?");
            }
        } catch (Exception err){
            NewGamesAPI1.getPlgLogger().warning("Invalid data passed via NPCKitHuman interact. Pls Fix.");
        }

    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageByEntityEvent event){
        if(event.getEntity() instanceof EntityHuman && event.getDamager() instanceof Player){
            EntityHuman human = (EntityHuman) event.getEntity();
            Player player = (Player) event.getDamager();
            String uuidloc = human.namedTag.getString(getPersistentUuidNbtLocation());
            if(!uuidloc.equals("")) {
                UUID uuid = UUID.fromString(uuidloc);
                if (npcIDs.containsValue(uuid)) {
                    PointEntity entity = npcIDs.inverse().get(uuid);

                    HashMap<String, String> parameters = new HashMap<>();
                    parameters.put("player_uuid", player.getUniqueId().toString());

                    getParentManager().getRegisteredTypes().get(entity.getType()).executeFunction("select_kit_gui", entity, human.getLevel(), parameters);

                }
            }
        }
    }

}
