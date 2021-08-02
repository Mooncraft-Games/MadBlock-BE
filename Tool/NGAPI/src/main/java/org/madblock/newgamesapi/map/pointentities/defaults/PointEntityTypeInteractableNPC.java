package org.madblock.newgamesapi.map.pointentities.defaults;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.pointentities.PointEntityCallData;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.newgamesapi.nukkit.entity.EntityHumanPlus;
import org.madblock.newgamesapi.nukkit.packet.AnimateEntityPacket;

import java.util.*;

public class PointEntityTypeInteractableNPC extends PointEntityTypeNPC {

    public PointEntityTypeInteractableNPC(GameHandler gameHandler) {
        super("npc_human", gameHandler);
    }

    @Override
    public String getPersistentUuidNbtLocation() {
        return "ngapi_npc_identifier";
    }

    @Override
    public void onRegister() {
        super.onRegister();
        addFunction("interact_message", this::sendMessage);
        addFunction("interact_pcommand", this::sendPlayerCommand);
    }

    protected void sendMessage(PointEntityCallData data){
        try {
            UUID uuid = UUID.fromString(data.getParameters().get("player_uuid"));
            Optional<Player> p = NewGamesAPI1.get().getServer().getPlayer(uuid);
            if(p.isPresent()){
                p.get().sendMessage(Utility.DEFAULT_TEXT_COLOUR + TextFormat.colorize(data.getParameters().getOrDefault("message", "[I am a bug and should be ashamed of my existence]")));
            } else {
                NewGamesAPI1.getPlgLogger().warning("Player passed into interact function doesn't exist. Scooby Dooby Doo where the hell are you?");
            }
        } catch (Exception err){
            NewGamesAPI1.getPlgLogger().warning("Invalid data passed via NPCHuman interact. Pls Fix.");
        }
    }

    protected void sendPlayerCommand(PointEntityCallData data){
        try {
            UUID uuid = UUID.fromString(data.getParameters().get("player_uuid"));
            Optional<Player> p = NewGamesAPI1.get().getServer().getPlayer(uuid);
            if(p.isPresent()){
                Server.getInstance().getCommandMap().dispatch(p.get(), data.getPointEntity().getStringProperties().getOrDefault("command", ""));
            } else {
                NewGamesAPI1.getPlgLogger().warning("Player passed into interact function doesn't exist. Scooby Dooby where the hell are you?");
            }
        } catch (Exception err){
            NewGamesAPI1.getPlgLogger().warning("Invalid data passed via NPCHuman interact. Pls Fix.");
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageByEntityEvent event){
        if(event.getEntity() instanceof EntityHumanPlus && event.getDamager() instanceof Player){
            EntityHumanPlus human = (EntityHumanPlus) event.getEntity();
            Player player = (Player) event.getDamager();
            String uuidloc = human.namedTag.getString(getPersistentUuidNbtLocation());
            if(!uuidloc.equals("")) {
                UUID uuid = UUID.fromString(uuidloc);
                if (npcIDs.containsValue(uuid)) {
                    PointEntity entity = npcIDs.inverse().get(uuid);
                    String interactionType = entity.getStringProperties().getOrDefault("interaction", "none");

                    HashMap<String, String> parameters = new HashMap<>();
                    parameters.put("player_uuid", player.getUniqueId().toString());

                    if (interactionType.toLowerCase().equals("message")) {
                        parameters.put("message", entity.getStringProperties().getOrDefault("message", "[Intentional Server Design]"));
                        getParentManager().getRegisteredTypes().get(entity.getType()).executeFunction("interact_message", entity, human.getLevel(), parameters);
                        return;
                    }
                    if (interactionType.toLowerCase().equals("command")){
                        getParentManager().getRegisteredTypes().get(entity.getType()).executeFunction("interact_pcommand", entity, human.getLevel(), parameters);
                    }

                    if(interactionType.equalsIgnoreCase("debug_coords")) {
                        try {
                            ListTag<DoubleTag> pos = human.namedTag.getList("Pos", DoubleTag.class);

                            String message = String.format(
                                    "eid: %s | coords official: (%s, %s, %s) | coords nbt: (%s, %s, %s)",
                                    human.getId(),
                                    human.getX(), human.getY(), human.getZ(),
                                    pos.get(0).getData(), pos.get(1).getData(), pos.get(2).getData()
                            );

                            NewGamesAPI1.getPlgLogger().info(message);
                            player.sendMessage(message);

                        } catch (Exception err) {
                            err.printStackTrace();
                        }
                    }
                }
            }
        }
    }

}
