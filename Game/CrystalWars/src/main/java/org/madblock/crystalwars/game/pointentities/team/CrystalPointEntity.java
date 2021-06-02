package org.madblock.crystalwars.game.pointentities.team;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.item.EntityEndCrystal;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEntityEvent;
import cn.nukkit.level.Location;
import cn.nukkit.level.Sound;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.utils.TextFormat;
import org.madblock.crystalwars.CrystalWarsPlugin;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.pointentities.PointEntityCallData;
import org.madblock.newgamesapi.map.pointentities.PointEntityType;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.newgamesapi.team.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Nicholas
 */
public class CrystalPointEntity extends PointEntityType implements Listener {
    public static final String ID = "madblock_crystalwars_crystal";

    public static final String TEAM_ID_PROPERTY = "team";

    protected final Map<Vector3, Map<UUID, Long>> biteDelays = new HashMap<>();
    protected final Map<Team, PointEntity> teamPointEntities = new HashMap<>();
    protected final Map<Vector3, Integer> crystalHealth = new HashMap<>();
    protected final Map<Vector3, EntityEndCrystal> crystals = new HashMap<>();

    public CrystalPointEntity(GameHandler gameHandler) {
        super(ID, gameHandler);
    }

    @Override
    public void onRegister() {
        addFunction("spawn_crystal", this::spawnCrystalFunction);
        CrystalWarsPlugin.getInstance().getServer().getPluginManager().registerEvents(this, CrystalWarsPlugin.getInstance());
    }

    @Override
    public void onUnregister() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void onAddPointEntity(PointEntity entity) {
        super.onAddPointEntity(entity);
        executeFunction("spawn_crystal", entity, manager.getLevelLookup().get(entity), new HashMap<>());
    }

    @Override
    public void onRemovePointEntity(PointEntity entity) {
        super.onRemovePointEntity(entity);
        teamPointEntities.remove(gameHandler.getTeams().get(entity.getStringProperties().get(TEAM_ID_PROPERTY)));
        biteDelays.remove(new Vector3((int) entity.getX(), (int) entity.getY(), (int) entity.getZ()));
    }

    public boolean isCrystalDestroyed(PointEntity entity) {
        if (!entity.getType().equals(ID)) {
            return false;
        }
        Entity theEntity = null;
        for (Entity worldEntity : gameHandler.getPrimaryMap().getEntities()) {
            if (!worldEntity.getName().equals("EndCrystal"))
                continue;
            if (worldEntity.getLocation().x == entity.getX() && worldEntity.getLocation().y == entity.getY() &&
                    worldEntity.getLocation().z == entity.getZ()) {
                theEntity = worldEntity;
                break;
            }
        }
        return theEntity == null;
    }

    protected void spawnCrystalFunction(PointEntityCallData data) {
        if (!data.getPointEntity().getStringProperties().containsKey(TEAM_ID_PROPERTY)) {
            CrystalWarsPlugin.getInstance().getLogger().error(String.format("Missing %s property for the MapID of %s (PointEntity: %s)", TEAM_ID_PROPERTY, gameHandler.getPrimaryMapID().getId(), data.getPointEntity().getId()));
            return;
        }
        if (!gameHandler.getTeams().containsKey(data.getPointEntity().getStringProperties().get(TEAM_ID_PROPERTY))) {
            CrystalWarsPlugin.getInstance().getLogger().error(String.format("Invalid %s property for the MapID of %s (PointEntity: %s)", TEAM_ID_PROPERTY, gameHandler.getPrimaryMapID().getId(), data.getPointEntity().getId()));
            return;
        }

        Team team = gameHandler.getTeams().get(data.getPointEntity().getStringProperties().get(TEAM_ID_PROPERTY));
        if (!team.isActiveGameTeam()) {
            CrystalWarsPlugin.getInstance().getLogger().error(String.format("Invalid %s property for the MapID of %s (PointEntity: %s)", TEAM_ID_PROPERTY, gameHandler.getPrimaryMapID().getId(), data.getPointEntity().getId()));
            return;
        }

        PointEntity entity = data.getPointEntity();

        Vector3 position = new Vector3((int) entity.getX(), (int) entity.getY(), (int) entity.getZ());
        FullChunk chunk = data.getLevel().getChunk((int) Math.floor(position.getX() / 16), (int) Math.floor(position.getZ() / 16), true);

        CompoundTag nbt = new CompoundTag()
                .putList(new ListTag<>("Pos")
                        .add(new DoubleTag("", position.getX()))
                        .add(new DoubleTag("", position.getY()))
                        .add(new DoubleTag("", position.getZ())))
                .putList(new ListTag<DoubleTag>("Motion")
                        .add(new DoubleTag("", 0))
                        .add(new DoubleTag("", 0))
                        .add(new DoubleTag("", 0)))
                .putList(new ListTag<FloatTag>("Rotation")
                        .add(new FloatTag("", 0f))
                        .add(new FloatTag("", 0f)));
        EntityEndCrystal endCrystal = (EntityEndCrystal) Entity.createEntity("EndCrystal", chunk, nbt);
        endCrystal.spawnToAll();

        teamPointEntities.put(team, entity);
        crystalHealth.put(position, 100);
    }

    @EventHandler
    private void onCrystalHit(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getEntity();
        if (gameHandler.getPlayers().contains(player) && entity.getName().equals("EndCrystal")) {
            event.setCancelled(updateCrystal(player, entity.getLocation()));
        }
    }

    protected boolean updateCrystal(Player player, Location location) {
        Optional<Team> playerTeam = gameHandler.getPlayerTeam(player);
        if (!playerTeam.isPresent() || !playerTeam.get().isActiveGameTeam()) {
            return true;
        }

        PointEntity targetCrystalPointEntity = null;
        for (Team team : teamPointEntities.keySet()) {
            PointEntity entity = teamPointEntities.get(team);
            if ((int) entity.getX() == location.getFloorX() && (int) entity.getY() == location.getFloorY() &&
                    (int) entity.getZ() == location.getFloorZ()) {
                targetCrystalPointEntity = entity;
                if (entity.getStringProperties().get(TEAM_ID_PROPERTY).equals(playerTeam.get().getId())) {
                    player.sendMessage(Utility.generateServerMessage("Game", TextFormat.BLUE, "You cannot destroy " +
                            "your own crystal.", TextFormat.RED));
                    return true;
                }
                break;
            }
        }

        if (targetCrystalPointEntity == null) {
            return true;
        }

        Vector3 position = new Vector3(location.getX(), location.getY(), location.getZ());
        if (crystals.containsKey(position)) {
            Integer health = crystalHealth.getOrDefault(position, 100);
            crystalHealth.put(position, health - 1);
            if (health <= 0) {
                crystals.get(position).kill();
                crystals.get(position).despawnFromAll();
                for (Player gamePlayer : gameHandler.getPlayers()) {
                    gamePlayer.sendMessage(Utility.generateServerMessage("Game", TextFormat.BLUE, String.format("%s's " +
                            "crystal was destroyed! They can no longer respawn!", playerTeam.get().getFormattedDisplayName())));
                }
                Team victimTeam = gameHandler.getTeams().get(targetCrystalPointEntity.getStringProperties().get(TEAM_ID_PROPERTY));
                for (Player victimPlayer : victimTeam.getPlayers()) {
                    victimPlayer.sendTitle(TextFormat.RED + "CRYSTAL DESTROYED", TextFormat.RED +
                            "You can no longer respawn!");
                    victimPlayer.getLevel().addSound(victimPlayer.getPosition(), Sound.MOB_ENDERDRAGON_GROWL, 0.25f, 1);
                }
            }
        }
        return true;
    }

}