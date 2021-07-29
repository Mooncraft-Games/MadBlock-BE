package org.madblock.skywars.behaviours;

import cn.nukkit.Player;
import cn.nukkit.block.*;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.item.EntityItem;
import cn.nukkit.entity.item.EntityPrimedTNT;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.entity.*;
import cn.nukkit.event.entity.EntityDamageEvent.*;
import cn.nukkit.event.inventory.InventoryPickupItemEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.item.*;
import cn.nukkit.level.Position;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.SetSpawnPositionPacket;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.game.events.GamePlayerDeathEvent;
import org.madblock.newgamesapi.map.MapID;
import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.newgamesapi.team.Team;
import org.madblock.newgamesapi.team.TeamPresets;
import org.madblock.skywars.pointentities.PointEntityTypeCorruption;
import org.madblock.skywars.pointentities.PointEntityTypePowerUp;
import org.madblock.skywars.powerups.GiantSnowBallPowerUp;
import org.madblock.skywars.powerups.PowerUp;
import org.madblock.skywars.utils.Constants;
import org.madblock.skywars.utils.SkywarsUtils;

import java.util.*;

public class SkywarsGameBehaviour extends GameBehavior {

    private MapRegion playArea;

    protected Map<Integer, Item> replacementItemDrops = new HashMap<>();

    protected Map<Integer, PowerUp> powerUps;

    protected Map<Player, Integer> kills;

    protected boolean allowDamage;

    protected int ticksUntilCorruption;

    @Override
    public Team.GenericTeamBuilder[] getTeams () {
        return new Team.GenericTeamBuilder[]{Team.newBasicTeamBuilder("players", "Players", Team.Colour.BLUE).setFriendlyFireEnabled(true)};
    }

    @Override
    public void onInitialCountdownEnd() {
        this.playArea = this.getSessionHandler().getPrimaryMapID().getRegions().get("play_zone");
        this.powerUps = new HashMap<>();
        this.kills = new HashMap<>();
        this.allowDamage = false;
        this.ticksUntilCorruption = this.getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("seconds_until_corruption_starts", Constants.DEFAULT_CORRUPTION_TIME) * 20;

        replacementItemDrops.put(Block.IRON_ORE, new ItemIngotIron());
        replacementItemDrops.put(Block.GOLD_ORE, new ItemIngotGold());
        replacementItemDrops.put(Block.LOG, new ItemBlock(new BlockPlanks(BlockPlanks.OAK), BlockPlanks.OAK, 4));


        this.getSessionHandler().getPointEntityTypeManager().registerPointEntityType(new PointEntityTypeCorruption(this.getSessionHandler()));
        this.getSessionHandler().getPointEntityTypeManager().registerPointEntityType(new PointEntityTypePowerUp(this.getSessionHandler()));

        for (Player player : getSessionHandler().getPlayers()) {
            updateKillsScoreboard(player);
        }
        updatePlayersScoreboard();
    }

    @Override
    public void registerGameSchedulerTasks() {

        MapID gameMap = this.getSessionHandler().getPrimaryMapID();

        this.getSessionHandler().getGameScheduler().registerGameTask(() -> this.allowDamage = true, gameMap.getIntegers().getOrDefault("grace_period", Constants.DEFAULT_GRACE_PERIOD) * 20);
        this.getSessionHandler().getGameScheduler().registerGameTask(this::startCorruptionTask, this.ticksUntilCorruption);
        this.getSessionHandler().getGameScheduler().registerGameTask(this::updateCompassTask, 0, 20);
        this.getSessionHandler().getGameScheduler().registerSelfCancellableGameTask(this::updateTimeTask, 0, 10);
    }

    @Override
    public void onAddPlayerToTeam(Player player, Team team) {
        if (getSessionHandler().getGameState().equals(GameHandler.GameState.MAIN_LOOP)) {
            updatePlayersScoreboard();
        }
    }

    @Override
    public void onGameDeathByPlayer(GamePlayerDeathEvent event) {
        Player killerPlayer = event.getDeathCause().getKillerPlayer().get();
        kills.put(killerPlayer, kills.getOrDefault(killerPlayer, 0) + 1);
        updateKillsScoreboard(killerPlayer);
    }

    // Stops block placement outside play area and sets corruption blocks if placed in corruption
    @EventHandler
    public void onBlockPlaced (BlockPlaceEvent event) {
        if (getSessionHandler().getPlayers().contains(event.getPlayer())) {
            Vector3 pos = new Vector3(
                    event.getBlock().getX(),
                    event.getBlock().getY(),
                    event.getBlock().getZ()
            );
            if (!playArea.isWithinThisRegion(pos)) {
                event.setCancelled();
                return;
            }

            if (isInCorruption(pos)) {
                Block corruptionBlock = SkywarsUtils.getRandomCorruptionBlock();
                event.setCancelled();
                event.getBlock().getLevel().setBlock(pos, corruptionBlock, false, false);
            }
        }
    }



    // Prevents corruption blocks from being broken
    @EventHandler
    public void onBlockBreak (BlockBreakEvent event) {
        if (getSessionHandler().getPlayers().contains(event.getPlayer())) {
            if (isInCorruption(event.getBlock().asVector3f().asVector3())) {
                event.setCancelled();
            }
        }
    }

    // Prevent blocks near power up entities from being broken.
    @EventHandler
    public void onExplosionPrimeEvent (EntityExplosionPrimeEvent event) {
        if (getSessionHandler().getPrimaryMap().getId() == event.getEntity().getLevel().getId()) {
            Collection<PointEntity> pointEntities = getSessionHandler().getPrimaryMapID().getPointEntities().values();
            Position explosionPosition = event.getEntity().getPosition();
            for (PointEntity pointEntity : pointEntities) {
                if (pointEntity.getType().equals(PointEntityTypePowerUp.ID) && explosionPosition.distance(pointEntity.positionToVector3()) <= 8) {
                    event.setBlockBreaking(false);
                }
            }
        }
    }

    // Send description when powerup is picked up.
    @EventHandler
    public void onItemPickUp (InventoryPickupItemEvent event) {
        if (event.getInventory().getHolder() instanceof Player && getSessionHandler().getPlayers().contains((Player)event.getInventory().getHolder()) && event.getItem().getItem().hasCompoundTag()) {

            CompoundTag nbt = event.getItem().getItem().getNamedTag();
            if (nbt.contains(Constants.POWERUP_ITEM_NBT_ID)) {
                PowerUp powerUp = getPowerUp(nbt.getInt(Constants.POWERUP_ITEM_NBT_ID));
                ((Player)event.getInventory().getHolder()).sendMessage(Utility.generateServerMessage(
                        "POWERUP",
                        TextFormat.YELLOW,
                        String.format("%sYou got the %s power up! %s", TextFormat.AQUA, powerUp.getName(), powerUp.getDescription())
                ));
            }

        }
    }

    // When a powerup itme dies.
    @EventHandler
    public void onEntityDeath (EntityDeathEvent event) {

        if (getSessionHandler().getPrimaryMap().getId() == event.getEntity().getLevel().getId()) {
            if (event.getEntity() instanceof EntityItem && ((EntityItem) event.getEntity()).getItem().getNamedTag().contains(Constants.POWERUP_ITEM_NBT_ID)) {

                CompoundTag nbt = ((EntityItem) event.getEntity()).getItem().getNamedTag();
                if (nbt.contains(Constants.POWERUP_ITEM_NBT_ID)) {
                    // PowerUp powerUp = getPowerUp(nbt.getInt(Constants.POWERUP_ITEM_NBT_ID));
                    removePowerUp(nbt.getInt(Constants.POWERUP_ITEM_NBT_ID));
                }
            }
        }
    }

    // Handles power up usage.
    @EventHandler
    public void onInteract (PlayerInteractEvent event) {
        Item item = event.getItem();
        if (getSessionHandler().getPlayers().contains(event.getPlayer()) && item.hasCompoundTag()) {
            CompoundTag namedTag = item.getNamedTag();

            if (namedTag.contains(Constants.POWERUP_ITEM_NBT_ID)) {
                event.getPlayer().getInventory().remove(item);
                PowerUp powerUp = getPowerUp(namedTag.getInt(Constants.POWERUP_ITEM_NBT_ID));
                powerUp.use(event.getPlayer());
                removePowerUp(powerUp.getId());
            }

        }
    }

    // Handles fall damage at start.
    @EventHandler
    public void onDamage (EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && getSessionHandler().getPlayers().contains(event.getEntity())) {
            if (!isDamageEnabled()) {
                event.setCancelled();
            }
        }
    }

    @EventHandler
    public void onItemSpawn (ItemSpawnEvent event) {
        if (getSessionHandler().getPrimaryMap().getId() == event.getEntity().getLevel().getId() && replacementItemDrops.containsKey(event.getEntity().getItem().getId())) {

            event.getEntity().getLevel().dropItem(event.getEntity().getPosition(), replacementItemDrops.get(event.getEntity().getItem().getId()));
            event.getEntity().kill();

        }
    }

    @EventHandler
    public void onTNTAttack (EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && getSessionHandler().getPlayers().contains(event.getEntity()) && event.getDamager() instanceof EntityPrimedTNT) {
            event.setDamage(6);
        }
    }

    public boolean isInCorruption (Vector3 pos) {
        for (PointEntity pointEntity : this.getSessionHandler().getPrimaryMapID().getPointEntities().values()) {
            if (pointEntity.getType().equals(PointEntityTypeCorruption.POINT_ENTITY_TYPE)) {

                int originalRadius = pointEntity.getIntegerProperties().getOrDefault("radius", Constants.DEFAULT_CORRUPTION_RADIUS);

                double distanceToPointEntity = (new Vector3(pos.getX(), 0, pos.getZ())).distanceSquared(new Vector3(pointEntity.getX(), 0, pointEntity.getZ()));

                if (distanceToPointEntity < Math.pow(originalRadius, 2) && distanceToPointEntity > Math.pow(((PointEntityTypeCorruption)this.getSessionHandler().getPointEntityTypeManager().getRegisteredTypes().get(PointEntityTypeCorruption.POINT_ENTITY_TYPE)).getCurrentCorruptionRadius(pointEntity), 2)) {
                    return true;
                }

            }
        }
        return false;
    }

    public boolean isInCorruption (Entity entity) {
        return isInCorruption(entity.getPosition());// && entity.getLevel().getBlock(entity.getPosition()).getId() != Block.AIR;
    }

    public boolean isDamageEnabled() {
        return this.allowDamage;
    }

    protected void updateCompassTask() {

        Set<Player> deadPlayers = getSessionHandler().getTeams().get(TeamPresets.DEAD_TEAM_ID).getPlayers();
        for (Player player : getSessionHandler().getPlayers()) {
            if (!deadPlayers.contains(player)) {
                Optional<Player> closestPlayer = getSessionHandler().getPlayers().stream()
                        .filter(targetPlayer -> !deadPlayers.contains(player) && player != targetPlayer)
                        .sorted((playerA, playerB) -> (int)(playerA.distance(player) - playerB.distance(player))).findFirst();

                if (closestPlayer.isPresent()) {

                    SetSpawnPositionPacket packet = new SetSpawnPositionPacket();
                    packet.spawnType = SetSpawnPositionPacket.TYPE_WORLD_SPAWN;
                    packet.x = closestPlayer.get().getFloorX();
                    packet.y = closestPlayer.get().getFloorY();
                    packet.z = closestPlayer.get().getFloorZ();
                    player.dataPacket(packet);

                }

            }
        }

    }

    protected void updateTimeTask(Task task) {
        if (this.ticksUntilCorruption <= 0) {

            for (Player player : getSessionHandler().getPlayers()) {
                getSessionHandler().getScoreboardManager().setLine(player, Constants.SCOREBOARD_CORRUPTION_INDEX, null);
            }
            task.cancel();

        } else {

            this.ticksUntilCorruption -= 10;
            StringBuilder timeUntilCorruption = new StringBuilder(Utility.ResourcePackCharacters.TIME).append(TextFormat.RED).append(" Corruption: ").append(TextFormat.RESET);

            int minutes = ticksUntilCorruption / (20 * 60);
            int seconds = (ticksUntilCorruption - minutes * 20 * 60) / 20;

            if (minutes >= 10) {
                timeUntilCorruption.append(minutes);
            } else {
                timeUntilCorruption.append("0").append(minutes);
            }
            timeUntilCorruption.append(":");
            if (seconds >= 10) {
                timeUntilCorruption.append(seconds);
            } else {
                timeUntilCorruption.append("0").append(seconds);
            }

            for (Player player : getSessionHandler().getPlayers()) {
                getSessionHandler().getScoreboardManager().setLine(player, Constants.SCOREBOARD_CORRUPTION_INDEX, timeUntilCorruption.toString());
            }

        }
    }

    protected void startCorruptionTask() {

        for (Player player : this.getSessionHandler().getPlayers()) {
            getSessionHandler().getPrimaryMap().addSound(player.getPosition(), Sound.MOB_ENDERDRAGON_GROWL, 0.12f, 1f, player);
            player.sendMessage(Utility.generateServerMessage("GAME", TextFormat.AQUA, String.format("%s%sThe world is being corrupted! Make your way to the middle!", TextFormat.BOLD, TextFormat.YELLOW)));
        }
        this.getSessionHandler().getGameScheduler().registerGameTask(this::takeCorruptionTask, 0, 10);
        this.getSessionHandler().getGameScheduler().registerSelfCancellableGameTask(this::corruptionTask, 0, this.getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("corruption_tick_speed", Constants.CORRUPTION_SPEED * 20));
    }

    protected void takeCorruptionTask() {
        Set<Player> deadPlayers = this.getSessionHandler().getTeams().get(TeamPresets.DEAD_TEAM_ID).getPlayers();
        for (Player player : this.getSessionHandler().getPlayers()) {
            if (!deadPlayers.contains(player) && this.isInCorruption(player)) {
                player.sendMessage(Utility.generateServerMessage("CORRUPTION", TextFormat.RED, String.format("%s%sGET OFF THE CORRUPTION!", TextFormat.BOLD, TextFormat.DARK_RED)));
                player.attack(
                        new EntityDamageEvent(player, DamageCause.MAGIC, 1)
                );
            }
        }
    }

    protected void corruptionTask(Task task) {

        Collection<PointEntity> pointEntities = this.getSessionHandler().getPrimaryMapID().getPointEntities().values();

        boolean stillCorrupting = false;
        for (PointEntity pointEntity : pointEntities) {
            if (pointEntity.getType().equals(PointEntityTypeCorruption.POINT_ENTITY_TYPE)) {

                PointEntityTypeCorruption type = (PointEntityTypeCorruption)(this.getSessionHandler().getPointEntityTypeManager().getRegisteredTypes().get(PointEntityTypeCorruption.POINT_ENTITY_TYPE));

                if (type.getCurrentCorruptionRadius(pointEntity) > 0) {
                    stillCorrupting = true;
                    type.executeFunction("apply_corruption", pointEntity, this.getSessionHandler().getPrimaryMap(), new HashMap<>());
                }

            }
        }

        if (!stillCorrupting) {
            task.cancel();
        }

    }

    protected void updateKillsScoreboard(Player player) {
        this.getSessionHandler().getScoreboardManager().setLine(player, Constants.SCOREBOARD_KILLS_INDEX, String.format("%s %d", Utility.ResourcePackCharacters.SKULL, kills.getOrDefault(player, 0)));
    }

    protected void updatePlayersScoreboard() {
        int activePlayers = (int)getSessionHandler().getPlayers().stream().filter(player -> getSessionHandler().getPlayerTeam(player).filter(Team::isActiveGameTeam).isPresent()).count();
        for (Player player : getSessionHandler().getPlayers()) {
            this.getSessionHandler().getScoreboardManager().setLine(player, Constants.SCOREBOARD_PLAYERS_INDEX, String.format("%s %d", Utility.ResourcePackCharacters.MORE_PEOPLE, activePlayers));
        }
    }

    public void addPowerUp (PowerUp powerUp) {
        this.powerUps.put(powerUp.getId(), powerUp);
    }

    public PowerUp getPowerUp (int id) {
        return this.powerUps.get(id);
    }

    public void removePowerUp (int id) {
        this.powerUps.remove(id);
    }

}
