package org.madblock.newgamesapi.game;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.level.Location;
import cn.nukkit.network.protocol.*;
import org.madblock.newgamesapi.exception.InvalidMapIDException;
import org.madblock.newgamesapi.map.MapID;
import org.madblock.newgamesapi.map.types.RotatablePosition;
import org.madblock.newgamesapi.team.Team;
import org.madblock.newgamesapi.team.TeamPresets;

import java.util.*;

public final class SpawnManager implements Listener {

    public static final String GLOBAL_TEAM_SPAWN_STRING = "*";
    public static final int CHUNK_RADIUS = 4;

    private HashMap<String, ArrayList<RotatablePosition>> spawnQueues;
    private HashMap<Team, String> assignedTeamQueues;

    private HashMap<UUID, Location> dimensionAckXUIDs; // to-do: maybe we should make this it's own thing.

    private GameHandler handler;

    public SpawnManager(GameHandler handler, HashMap<String, Team> teams, MapID mapID, boolean shuffleSpawns){
        this.spawnQueues = new HashMap<>();
        this.assignedTeamQueues = new HashMap<>();
        this.dimensionAckXUIDs = new HashMap<>();
        this.handler = handler;

        ArrayList<Team> activeTeams = new ArrayList<>();
        for(Team team: teams.values()) {
            if ((!team.getId().equals(TeamPresets.SPECTATOR_TEAM_ID)) || (!team.getId().equals(TeamPresets.DEAD_TEAM_ID))) {
                activeTeams.add(team);
            }
        }

        // Global spawns
        boolean areGlobalSpawnsEnabled = false;
        if(mapID.getSpawns().containsKey(GLOBAL_TEAM_SPAWN_STRING) && mapID.getSpawns().get(GLOBAL_TEAM_SPAWN_STRING).length != 0){
            spawnQueues.put(GLOBAL_TEAM_SPAWN_STRING, new ArrayList<>(Arrays.asList(mapID.getSpawns().get(GLOBAL_TEAM_SPAWN_STRING))));
            areGlobalSpawnsEnabled = true;
        }

        // Spectator Spawns
        if(!mapID.getSpawns().containsKey(TeamPresets.SPECTATOR_TEAM_ID) || mapID.getSpawns().get(TeamPresets.SPECTATOR_TEAM_ID).length == 0){
            if(areGlobalSpawnsEnabled){
                assignedTeamQueues.put(teams.get(TeamPresets.SPECTATOR_TEAM_ID), GLOBAL_TEAM_SPAWN_STRING);
            } else {
                throw new InvalidMapIDException(String.format("MapID [%s] does not contain spawns for spectators, nor does it contain any global", mapID.getId()));
            }
        } else {
            spawnQueues.put(TeamPresets.SPECTATOR_TEAM_ID, new ArrayList<>(Arrays.asList(mapID.getSpawns().get(TeamPresets.SPECTATOR_TEAM_ID))));
            assignedTeamQueues.put(teams.get(TeamPresets.SPECTATOR_TEAM_ID), TeamPresets.SPECTATOR_TEAM_ID);
        }

        // Dead Spawns: Use Spectator spawns if not present.
        if(!mapID.getSpawns().containsKey(TeamPresets.DEAD_TEAM_ID) || mapID.getSpawns().get(TeamPresets.DEAD_TEAM_ID).length == 0){
            assignedTeamQueues.put(teams.get(TeamPresets.DEAD_TEAM_ID), assignedTeamQueues.get(teams.get(TeamPresets.SPECTATOR_TEAM_ID))); //Remap dead spawns to spectator spawn queue
        } else {
            spawnQueues.put(TeamPresets.DEAD_TEAM_ID, new ArrayList<>(Arrays.asList(mapID.getSpawns().get(TeamPresets.DEAD_TEAM_ID))));
            assignedTeamQueues.put(teams.get(TeamPresets.DEAD_TEAM_ID), TeamPresets.DEAD_TEAM_ID);
        }

        for(Team team: activeTeams){
            if(mapID.getSpawns().containsKey(team.getId()) && mapID.getSpawns().get(team.getId()).length != 0){
                spawnQueues.put(team.getId(), new ArrayList<>(Arrays.asList(mapID.getSpawns().get(team.getId()))));
                assignedTeamQueues.put(teams.get(team.getId()), team.getId());
            } else {
                if(areGlobalSpawnsEnabled){
                    assignedTeamQueues.put(team, GLOBAL_TEAM_SPAWN_STRING);
                } else {
                    throw new InvalidMapIDException(String.format("MapID [%s] does not contain spawns for team [%s], nor does it contain any global spawns", mapID.getId(), team.getId()));
                }
            }
        }


        if(shuffleSpawns) {
            for (ArrayList<RotatablePosition> queue : spawnQueues.values()) {
                for (RotatablePosition spawn : new ArrayList<>(queue)) {
                    queue.remove(spawn);
                    queue.add(new Random().nextInt(queue.size()+1), spawn);
                }
            }
        }
    }

    public Location nextSpawnPosition(Team team) throws IllegalArgumentException {
        if(!assignedTeamQueues.containsKey(team)) {
            throw new IllegalArgumentException(String.format("Team %s doesn't have any spawnpoint queue for it. Was it added after the GameBehaviors#getTeams() call? ", team.getId()));
        }
        String queueId = assignedTeamQueues.get(team);
        RotatablePosition blockposition = spawnQueues.get(queueId).get(0);
        //Add to the end of the queue.
        spawnQueues.get(queueId).remove(0);
        spawnQueues.get(queueId).add(blockposition);
        double addValue = blockposition.isAccuratePosition() ? 0 : 0.5d;
        return blockposition.addPosition(addValue).toLocation(null);
    }

    public Optional<Location> placePlayerInSpawnPosition(Player player, Team team){
        if(this.handler == null){
            return Optional.empty();
        }
        try {
            if(handler.getTeams().containsKey(team.getId())){
                Location position = nextSpawnPosition(team).add(0, 2, 0);
                player.setHealth(player.getMaxHealth());
                player.setOnFire(0);

                if(!handler.getPrimaryMap().getName().equals(player.getLevel().getName())) {
                    boolean justEntered = player.getLevel().getId() == 1;   // Ensure that we aren't sending dimension packets to players who are just logging in
                    if (!justEntered) {
                        dimensionTransfer(player, position);
                    } else {
                        position.setLevel(handler.getPrimaryMap());
                        player.teleport(position);
                    }
                } else {
                    position.setLevel(handler.getPrimaryMap());
                    player.teleport(position);
                }

                return Optional.of(position);
            }
            return Optional.empty();
        } catch (Exception err){
            return Optional.empty();
        }
    }

    public boolean setAssignedTeamQueue(Team team, String queue) {
        String queueID = queue.toLowerCase();
        if(handler.getTeams().containsValue(team) && spawnQueues.containsKey(queueID)){
            if(spawnQueues.get(queueID).size() > 0){
                assignedTeamQueues.put(team, queue);
                return true;
            }
        }
        return false;
    }


    /**
     * Sends/manages dimension packets and transfers player to the level and position.
     * @param player the player changing dimension
     * @param position the position of the player
     */
    private void dimensionTransfer(Player player, Location position) {
        //player.usedChunks.clear();
        switchDimension(position, player, true);
    }

    //TODO: If we ever add support for dimensions that are not the overworld, adjust ids
    private void switchDimension(Location pos, Player player, boolean fake) {

        ChangeDimensionPacket changeDimensionPacket = new ChangeDimensionPacket();
        changeDimensionPacket.dimension = fake ? 1 : 0;
        changeDimensionPacket.respawn = true;
        changeDimensionPacket.x = (float) pos.x;
        changeDimensionPacket.y = (float) pos.y;
        changeDimensionPacket.z = (float) pos.z;
        player.dataPacket(changeDimensionPacket);

        // Send empty chunks for the nether.
        if(fake) {
            NetworkChunkPublisherUpdatePacket publishPacket = new NetworkChunkPublisherUpdatePacket();
            publishPacket.position = pos.asBlockVector3();
            publishPacket.radius = CHUNK_RADIUS;
            player.dataPacket(publishPacket);

            for (int cX = pos.getChunkX() - CHUNK_RADIUS; cX <= pos.getChunkX() + CHUNK_RADIUS; cX++) {
                for (int cZ = pos.getChunkZ() - CHUNK_RADIUS; cZ <= pos.getChunkZ() + CHUNK_RADIUS; cZ++) {
                    player.dataPacket(provideEmptyChunkPacket(cX, cZ));
                }
            }

            // Queue an ack check
            dimensionAckXUIDs.put(player.getUniqueId(), new Location(pos.x, pos.y, pos.z, pos.yaw, pos.pitch, handler.getPrimaryMap()));

        } else {

            for (int cX = pos.getChunkX() - CHUNK_RADIUS; cX <= pos.getChunkX() + CHUNK_RADIUS; cX++) {
                for (int cZ = pos.getChunkZ() - CHUNK_RADIUS; cZ <= pos.getChunkZ() + CHUNK_RADIUS; cZ++) {
                    handler.getPrimaryMap().requestChunk(cX, cZ, player);
                }
            }

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

                if(dimensionAckXUIDs.containsKey(event.getPlayer().getUniqueId())) {
                    Location pos = dimensionAckXUIDs.remove(event.getPlayer().getUniqueId());
                    event.getPlayer().teleport(pos);
                    event.getPlayer().setLevel(handler.getPrimaryMap());
                    switchDimension(pos, event.getPlayer(), false);

                    StopSoundPacket stopSoundPacket = new StopSoundPacket();
                    stopSoundPacket.stopAll = true;
                    stopSoundPacket.name = "portal.travel";
                    event.getPlayer().dataPacket(stopSoundPacket);
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        dimensionAckXUIDs.remove(event.getPlayer().getUniqueId());
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