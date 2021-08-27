package org.madblock.crystalwars.game.pointentities.team;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.item.EntityItem;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.item.*;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.Sound;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.nbt.NBTIO;
import org.madblock.crystalwars.CrystalWarsPlugin;
import org.madblock.crystalwars.game.CrystalWarsGame;
import org.madblock.crystalwars.game.pointentities.capturepoint.CapturePointEntity;
import org.madblock.crystalwars.game.pointentities.capturepoint.GoldCapturePointEntity;
import org.madblock.crystalwars.game.pointentities.capturepoint.MiddleCapturePointEntity;
import org.madblock.crystalwars.game.upgrades.CrystalTeamUpgrade;
import org.madblock.newgamesapi.map.pointentities.PointEntityCallData;
import org.madblock.newgamesapi.map.pointentities.PointEntityType;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.newgamesapi.team.Team;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class GeneratorPointEntity extends PointEntityType implements Listener {
    public static final String ID = "madblock_crystalwars_generator";

    public static final String TEAM_ID_PROPERTY = "team";

    private static final double PLAYER_MIN_GENERATOR_DISTANCE = 1.5;

    protected CrystalWarsGame gameBehavior;

    public GeneratorPointEntity(CrystalWarsGame base) {
        super(ID, base.getSessionHandler());
        gameBehavior = base;
    }

    @Override
    public void onRegister() {
        CrystalWarsPlugin.getInstance().getServer().getPluginManager().registerEvents(this, CrystalWarsPlugin.getInstance());

        gameHandler.getGameScheduler().registerGameTask(() -> executeFunctionForAll("spawn_iron", new HashMap<>()), 0, 3 * 20);
        gameHandler.getGameScheduler().registerGameTask(() -> executeFunctionForAll("spawn_gold", new HashMap<>()), 0, 6 * 20);
        gameHandler.getGameScheduler().registerGameTask(() -> executeFunctionForAll("spawn_diamonds", new HashMap<>()), 0, 10 * 20);

        addFunction("spawn_iron", this::spawnIron);
        addFunction("spawn_gold", this::spawnGold);
        addFunction("spawn_diamonds", this::spawnDiamonds);

    }

    @Override
    public void onUnregister() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void onAddPointEntity(PointEntity entity) {
        super.onAddPointEntity(entity);
        if (!entity.getStringProperties().containsKey(TEAM_ID_PROPERTY)) {
            CrystalWarsPlugin.getInstance().getLogger().error(String.format("Missing %s property for the MapID of %s (PointEntity: %s)", TEAM_ID_PROPERTY, gameHandler.getPrimaryMapID().getId(), entity.getId()));
            manager.removePointEntity(entity);
        }
    }

    @Override
    public void onRemovePointEntity(PointEntity entity) {
        super.onRemovePointEntity(entity);
    }

    protected void spawnIron(PointEntityCallData data) {
        String teamId = data.getPointEntity().getStringProperties().get(TEAM_ID_PROPERTY);
        int ironToSpawn = getMultiplier(teamId);
        spawnItem(getPosition(data.getPointEntity(), data.getLevel()), new ItemIngotIron(0, ironToSpawn));
    }

    protected void spawnGold(PointEntityCallData data) {
        String teamId = data.getPointEntity().getStringProperties().get(TEAM_ID_PROPERTY);
        int goldToSpawn = getMultiplier(teamId) * getCapturePointsCount(data.getPointEntity().getStringProperties().get(TEAM_ID_PROPERTY),
                GoldCapturePointEntity.ID);
        if (goldToSpawn > 0) {
            spawnItem(getPosition(data.getPointEntity(), data.getLevel()), new ItemIngotGold(0, goldToSpawn));
        }
    }

    protected void spawnDiamonds(PointEntityCallData data) {
        String teamId = data.getPointEntity().getStringProperties().get(TEAM_ID_PROPERTY);
        int diamondsToSpawn = getMultiplier(teamId) * getCapturePointsCount(teamId, MiddleCapturePointEntity.ID);
        if (diamondsToSpawn > 0) {
            spawnItem(getPosition(data.getPointEntity(), data.getLevel()), new ItemDiamond(0, diamondsToSpawn));
        }
    }

    protected int getMultiplier(String teamId) {
        Team team = gameHandler.getTeams().get(teamId);
        return gameBehavior.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.RESOURCES_TWO) ? 3 :
                gameBehavior.doesTeamHaveUpgrade(team, CrystalTeamUpgrade.RESOURCES_ONE) ? 2 : 1;
    }

    protected void spawnItem(Position position, Item item) {
        Set<Player> closePlayers = gameHandler.getPlayers()
                .stream()
                .filter(player -> player.distance(position) <= PLAYER_MIN_GENERATOR_DISTANCE)
                .filter(player -> gameHandler.getPlayerTeam(player).filter(Team::isActiveGameTeam).isPresent())
                .collect(Collectors.toSet());

        if (closePlayers.size() > 0) {
            for (Player player : closePlayers) {
                player.getLevel().addSound(player, Sound.ITEM_BONE_MEAL_USE, 0.6f, 1f, player);
                player.getInventory().addItem(item);
                player.getInventory().sendContents(player);
            }
        } else {
            position.getLevel().addSound(position, Sound.ITEM_BONE_MEAL_USE, 0.6f, 1f);
            Optional<Entity> existingEntity = Arrays.stream(
                    position.getLevel().getNearbyEntities(
                            new SimpleAxisAlignedBB(
                                    position.getX() - PLAYER_MIN_GENERATOR_DISTANCE, position.getY() - PLAYER_MIN_GENERATOR_DISTANCE, position.getZ() - PLAYER_MIN_GENERATOR_DISTANCE,
                                    position.getX() + PLAYER_MIN_GENERATOR_DISTANCE, position.getY() + PLAYER_MIN_GENERATOR_DISTANCE, position.getZ() + PLAYER_MIN_GENERATOR_DISTANCE
                            )
                    )
            ).filter(entity -> entity instanceof EntityItem && ((EntityItem)entity).getItem().getId() == item.getId()).findFirst();
            if (existingEntity.isPresent()) {
                ((EntityItem)existingEntity.get()).getItem().setCount(Math.min(((EntityItem)existingEntity.get()).getItem().getCount() + 1, 30));
            } else {
                EntityItem entity = new EntityItem(
                        position.getChunk(),
                        Entity.getDefaultNBT(position)
                                .putShort("Health", 5)
                                .putCompound("Item", NBTIO.putItemHelper(item))
                                .putShort("PickupDelay", 0)
                );
                entity.spawnToAll();
            }
        }
    }

    protected int getCapturePointsCount(String teamId, String capturePointEntityId) {
        CapturePointEntity typeEntity = ((CapturePointEntity)manager.getRegisteredTypes().get(capturePointEntityId));
        return (int)manager.getTypeLookup().get(capturePointEntityId)
                .stream()
                .filter(entity -> typeEntity.getTeam(entity).filter(team -> team.getId().equals(teamId)).isPresent())
                .count();
    }

    private Position getPosition(PointEntity entity, Level level) {
        return new Position(entity.getX(), entity.getY(), entity.getZ(), level);
    }
}