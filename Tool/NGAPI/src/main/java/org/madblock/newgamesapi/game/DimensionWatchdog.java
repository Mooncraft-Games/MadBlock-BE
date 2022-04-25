package org.madblock.newgamesapi.game;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.player.PlayerRespawnEvent;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.level.DimensionData;
import cn.nukkit.level.DimensionEnum;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.level.biome.Biome;
import cn.nukkit.level.biome.EnumBiome;
import cn.nukkit.level.util.PalettedBlockStorage;
import cn.nukkit.network.protocol.*;
import cn.nukkit.utils.BinaryStream;

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
            publishPacket.radius = player.getViewDistance() * 16;
            player.dataPacket(publishPacket);

            for (int cX = pos.getChunkX() - CHUNK_RADIUS; cX <= pos.getChunkX() + CHUNK_RADIUS; cX++) {
                for (int cZ = pos.getChunkZ() - CHUNK_RADIUS; cZ <= pos.getChunkZ() + CHUNK_RADIUS; cZ++) {
                    player.dataPacket(provideEmptyChunkPacket(cX, cZ, DimensionEnum.getDataFromId(changeDimensionPacket.dimension)));
                }
            }

            // Queue an ack check
            dimensionAckIDs.put(player.getUniqueId(), new Location(pos.x, pos.y, pos.z, pos.yaw, pos.pitch, target));

        } else {

            for (int cX = pos.getChunkX() - CHUNK_RADIUS; cX <= pos.getChunkX() + CHUNK_RADIUS; cX++) {
                for (int cZ = pos.getChunkZ() - CHUNK_RADIUS; cZ <= pos.getChunkZ() + CHUNK_RADIUS; cZ++) {
                    target.requestChunk(cX, cZ, player);
                }
            }

            player.switchLevel(pos.level);
            player.teleport(pos);

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

                if(dimensionAckIDs.containsKey(event.getPlayer().getUniqueId())) {
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


    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerRespawnEvent event) {
        freshPlayers.add(event.getPlayer().getId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        dimensionAckIDs.remove(event.getPlayer().getUniqueId());
        freshPlayers.remove(event.getPlayer().getId());
    }


    private static LevelChunkPacket provideEmptyChunkPacket(int cX, int cZ, DimensionData dimensionData) {
        BinaryStream payload = new BinaryStream();

        // 3D biome data
        BinaryStream biomeStream = new BinaryStream();
        PalettedBlockStorage palette = PalettedBlockStorage.createWithDefaultState(EnumBiome.OCEAN.id);
        palette.writeTo(biomeStream);
        byte[] biomePayload = biomeStream.getBuffer();

        // Put biome data x amount depending on dimension max height.
        for (int i = 0; i < dimensionData.getHeight() >> 4; i++) {
            payload.put(biomePayload);
        }

        // border blocks (useless)
        payload.putByte((byte) 0);

        LevelChunkPacket chunkData = new LevelChunkPacket();
        chunkData.chunkX = cX;
        chunkData.chunkZ = cZ;
        chunkData.subChunkCount = 0;
        chunkData.data = payload.getBuffer();
        chunkData.cacheEnabled = false;
        return chunkData;
    }

}
