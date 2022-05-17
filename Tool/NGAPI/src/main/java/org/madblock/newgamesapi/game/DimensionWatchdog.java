package org.madblock.newgamesapi.game;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.player.PlayerRespawnEvent;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.event.server.DataPacketSendEvent;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.math.BlockVector3;
import cn.nukkit.network.protocol.*;
import org.madblock.newgamesapi.nukkit.packet.EmptyLevelChunkPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Handles dimension transfers. Stops new players from being forced through dimension transfers.
 */
public class DimensionWatchdog implements Listener {

    public static final int CHUNK_RADIUS = 1;
    private static DimensionWatchdog dimensionWatchdog;

    protected HashMap<UUID, Location> dimensionAckIDs; // to-do: maybe we should make this it's own thing.
    protected ArrayList<Long> freshPlayers;

    public DimensionWatchdog() {
        this.dimensionAckIDs = new HashMap<>();
        this.freshPlayers = new ArrayList<>();
    }

    public boolean setAsPrimary() {
        if(dimensionWatchdog == null) {
            dimensionWatchdog = this;
            return true;
        }
        return false;
    }

    public static DimensionWatchdog get() {
        return dimensionWatchdog;
    }

    /**
     * Sends/manages dimension packets and transfers player to the level and position.
     * @param player the player changing dimension
     * @param position the position of the player
     */
    public void dimensionTransfer(Player player, Location position, Level target) {
        if(target == null) throw new IllegalArgumentException("Target level must not be null.");

        if(freshPlayers.contains(player.getId())) {
            freshPlayers.remove(player.getId());
            Location location = new Location(position.x, position.y, position.z, position.pitch, position.yaw, target);
            player.teleport(location);

        } else {
            switchDimension(position, player, target,true);
        }
    }

    //TODO: If we ever add support for dimensions that are not the overworld, adjust ids
    private void switchDimension(Location pos, Player player, Level target, boolean fake) {
        ChangeDimensionPacket changeDimensionPacket = new ChangeDimensionPacket();
        changeDimensionPacket.dimension = fake ? 1 : 0;
        changeDimensionPacket.x = (float) pos.x;
        changeDimensionPacket.y = (float) pos.y;
        changeDimensionPacket.z = (float) pos.z;
        changeDimensionPacket.respawn = true;
        player.dataPacket(changeDimensionPacket);

        NetworkChunkPublisherUpdatePacket publishPacket = new NetworkChunkPublisherUpdatePacket();
        publishPacket.position = new BlockVector3((int) changeDimensionPacket.x, (int) changeDimensionPacket.y, (int) changeDimensionPacket.z);
        publishPacket.radius = player.getViewDistance() * 16;
        player.dataPacket(publishPacket);

        if (fake) {
            // Send empty chunks as required.
            // (otherwise the client will not be able to transfer back from the nether properly)
            for (int cX = pos.getChunkX() - CHUNK_RADIUS; cX <= pos.getChunkX() + CHUNK_RADIUS; cX++) {
                for (int cZ = pos.getChunkZ() - CHUNK_RADIUS; cZ <= pos.getChunkZ() + CHUNK_RADIUS; cZ++) {
                    player.dataPacket(provideEmptyChunkPacket(cX, cZ));
                }
            }

            // Queue an ack check.
            // Also prevents actual chunks from the previous map being sent mid-dimension transfer.
            dimensionAckIDs.put(player.getUniqueId(), new Location(pos.x, pos.y, pos.z, pos.yaw, pos.pitch, target));
        } else {
            // Send the actual chunks of the map we're teleporting to.
            player.teleport(pos);

            for (int cX = pos.getChunkX() - CHUNK_RADIUS; cX <= pos.getChunkX() + CHUNK_RADIUS; cX++) {
                for (int cZ = pos.getChunkZ() - CHUNK_RADIUS; cZ <= pos.getChunkZ() + CHUNK_RADIUS; cZ++) {
                    target.requestChunk(cX, cZ, player);
                }
            }

            // Force the client to close the dimension transfer screen.
            PlayStatusPacket statusPacket = new PlayStatusPacket();
            statusPacket.status = PlayStatusPacket.PLAYER_SPAWN;
            player.dataPacket(statusPacket);
        }
    }

    @EventHandler
    public void onDimensionSuccessPacket(DataPacketReceiveEvent event) {
        if (event.getPacket() instanceof PlayerActionPacket) {
            PlayerActionPacket actionPacket = (PlayerActionPacket) event.getPacket();

            if (actionPacket.action == PlayerActionPacket.ACTION_DIMENSION_CHANGE_ACK) {
                if (dimensionAckIDs.containsKey(event.getPlayer().getUniqueId())) {
                    Location pos = dimensionAckIDs.remove(event.getPlayer().getUniqueId());
                    switchDimension(pos, event.getPlayer(), pos.level, false);

                    StopSoundPacket stopSoundPacket = new StopSoundPacket();
                    stopSoundPacket.stopAll = true;
                    stopSoundPacket.name = "portal.travel";
                    event.getPlayer().dataPacket(stopSoundPacket);
                }
            }
        }
    }

    @EventHandler
    public void onSendChunkPacket(DataPacketSendEvent event) {
        if (this.dimensionAckIDs.containsKey(event.getPlayer().getUniqueId())
                && event.getPacket() instanceof LevelChunkPacket) {
            // If a LevelChunkPacket is sent from the map prior to the nether dimension transfer
            // during the nether transfer, the client may hang/crash from it.
            event.setCancelled();
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerRespawnEvent event) {
        freshPlayers.add(event.getPlayer().getId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        dimensionAckIDs.remove(event.getPlayer().getUniqueId());
        freshPlayers.remove(event.getPlayer().getId());
    }

    private static EmptyLevelChunkPacket provideEmptyChunkPacket(int cX, int cZ) {
        EmptyLevelChunkPacket chunkData = new EmptyLevelChunkPacket();
        chunkData.chunkX = cX;
        chunkData.chunkZ = cZ;
        return chunkData;
    }

}
