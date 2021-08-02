package org.madblock.newgamesapi.game;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.player.PlayerRespawnEvent;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.network.protocol.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Handles dimension transfers. Stops new players from being forced through dimension transfers.
 */
public class DimensionWatchdog implements Listener {

    public static final int CHUNK_RADIUS = 4;
    private static DimensionWatchdog dimensionWatchdog;

    protected HashMap<UUID, Location> dimensionAckXUIDs; // to-do: maybe we should make this it's own thing.
    protected ArrayList<Long> freshPlayers;

    public DimensionWatchdog() {
        this.dimensionAckXUIDs = new HashMap<>();
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
        changeDimensionPacket.respawn = true;
        changeDimensionPacket.x = (float) pos.x;
        changeDimensionPacket.y = (float) pos.y;
        changeDimensionPacket.z = (float) pos.z;
        player.locallyInitialized = false;
        player.dataPacket(changeDimensionPacket);

        // Send empty chunks for the nether.
        if(fake) {
            NetworkChunkPublisherUpdatePacket publishPacket = new NetworkChunkPublisherUpdatePacket();
            publishPacket.position = pos.asBlockVector3();
            publishPacket.radius = CHUNK_RADIUS * 16;
            player.dataPacket(publishPacket);

            for (int cX = pos.getChunkX() - CHUNK_RADIUS; cX <= pos.getChunkX() + CHUNK_RADIUS; cX++) {
                for (int cZ = pos.getChunkZ() - CHUNK_RADIUS; cZ <= pos.getChunkZ() + CHUNK_RADIUS; cZ++) {
                    player.dataPacket(provideEmptyChunkPacket(cX, cZ));
                }
            }

            // Queue an ack check
            dimensionAckXUIDs.put(player.getUniqueId(), new Location(pos.x, pos.y, pos.z, pos.yaw, pos.pitch, target));

        } else {

            for (int cX = pos.getChunkX() - CHUNK_RADIUS; cX <= pos.getChunkX() + CHUNK_RADIUS; cX++) {
                for (int cZ = pos.getChunkZ() - CHUNK_RADIUS; cZ <= pos.getChunkZ() + CHUNK_RADIUS; cZ++) {
                    target.requestChunk(cX, cZ, player);
                }
            }

            PlayStatusPacket statusPacket = new PlayStatusPacket();
            statusPacket.status = PlayStatusPacket.PLAYER_SPAWN;
            player.locallyInitialized = false;
            player.dataPacket(statusPacket);

        }
    }


    @EventHandler
    public void onDimensionSuccessPacket(DataPacketReceiveEvent event) {

        if (event.getPacket() instanceof PlayerActionPacket) {
            PlayerActionPacket actionPacket = (PlayerActionPacket) event.getPacket();

            if (actionPacket.action == PlayerActionPacket.ACTION_DIMENSION_CHANGE_ACK) {
                event.getPlayer().locallyInitialized = true;

                if(dimensionAckXUIDs.containsKey(event.getPlayer().getUniqueId())) {
                    Location pos = dimensionAckXUIDs.remove(event.getPlayer().getUniqueId());
                    event.getPlayer().teleport(pos);
                    switchDimension(pos, event.getPlayer(), pos.level, false);

                    StopSoundPacket stopSoundPacket = new StopSoundPacket();
                    stopSoundPacket.stopAll = true;
                    stopSoundPacket.name = "portal.travel";
                    event.getPlayer().dataPacket(stopSoundPacket);
                }
            }
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerRespawnEvent event) {
        freshPlayers.add(event.getPlayer().getId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        dimensionAckXUIDs.remove(event.getPlayer().getUniqueId());
        freshPlayers.remove(event.getPlayer().getId());
    }


    private static LevelChunkPacket provideEmptyChunkPacket(int cX, int cZ) {
        LevelChunkPacket chunkData = new LevelChunkPacket();
        chunkData.chunkX = cX;
        chunkData.chunkZ = cZ;
        chunkData.subChunkCount = 0;
        chunkData.data = new byte[257];
        chunkData.cacheEnabled = false;
        return chunkData;
    }

}
