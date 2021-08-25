package org.madblock.crystalwars.game;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.block.BlockTNT;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.item.EntityPrimedTNT;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.entity.EntityExplodeEvent;
import cn.nukkit.event.inventory.InventoryMoveItemEvent;
import cn.nukkit.math.Vector3;
import org.madblock.crystalwars.game.pointentities.capturepoint.GoldCapturePointEntity;
import org.madblock.crystalwars.game.pointentities.capturepoint.MiddleCapturePointEntity;
import org.madblock.crystalwars.game.pointentities.shop.types.IronShopPointEntity;
import org.madblock.crystalwars.game.pointentities.shop.types.GoldShopPointEntity;
import org.madblock.crystalwars.game.pointentities.shop.types.DiamondShopPointEntity;
import org.madblock.crystalwars.game.pointentities.team.CrystalPointEntity;
import org.madblock.crystalwars.game.pointentities.team.GeneratorPointEntity;
import org.madblock.crystalwars.game.upgrades.CrystalTeamUpgrade;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.game.events.GamePlayerDeathEvent;
import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.newgamesapi.team.Team;
import org.madblock.newgamesapi.team.TeamPresets;

import java.util.*;

public class CrystalWarsGame extends GameBehavior {

    protected Set<Vector3> placedBlocks = new HashSet<>();
    protected Map<Team, Set<CrystalTeamUpgrade>> upgrades = new HashMap<>();

    protected ArrayList<MapRegion> repairRegions = new ArrayList<>();
    protected Random random;

    protected int repairCrystal_startDelay;
    protected int repairCrystal_minDelay;
    protected int repairCrystal_maxDelay;

    @Override
    public int onGameBegin() {

        // default: 80 ticks
        this.repairCrystal_startDelay = Math.max(10, getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("c_repair_time_start", 20 * 80));

        // default: 30 ticks
        this.repairCrystal_minDelay = Math.max(10, getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("c_repair_time_min", 20 * 30));

        //default: 60 ticks
        this.repairCrystal_maxDelay = Math.max(10, getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("c_repair_time_max", 20 * 60));

        return 5;
    }

    @Override
    public void onInitialCountdownEnd() {
        getSessionHandler().getPointEntityTypeManager().registerPointEntityType(new CrystalPointEntity(getSessionHandler()));
        getSessionHandler().getPointEntityTypeManager().registerPointEntityType(new GeneratorPointEntity(this));

        getSessionHandler().getPointEntityTypeManager().registerPointEntityType(new IronShopPointEntity(this));
        getSessionHandler().getPointEntityTypeManager().registerPointEntityType(new GoldShopPointEntity(this));
        getSessionHandler().getPointEntityTypeManager().registerPointEntityType(new DiamondShopPointEntity(this));

        getSessionHandler().getPointEntityTypeManager().registerPointEntityType(new GoldCapturePointEntity(getSessionHandler()));
        getSessionHandler().getPointEntityTypeManager().registerPointEntityType(new MiddleCapturePointEntity(getSessionHandler()));

        for (Player player : getSessionHandler().getPlayers()) {
            if (!getSessionHandler().getPlayerTeam(player).isPresent())
                return;
            Team team = getSessionHandler().getPlayerTeam(player).get();
            getSessionHandler().getScoreboardManager().setLine(player, 0, String.format("%s",
                    team.getFormattedDisplayName()));
        }

        // I've given up with MapRegions as they're so utterly useless
        // in their current state. Rewrite inbound!
        // This just gathers all the current regions which spawn crystals so that a looping timer can spawn
        // repair crystals.
        for(MapRegion region: getSessionHandler().getPrimaryMapID().getRegions().values()) {
            for(String str: region.getTags()) {
                if(str.equalsIgnoreCase("repair_crystal_region")) {
                    repairRegions.add(region);
                    break;
                }
            }
        }

        this.random = new Random();
    }

    @Override
    public void registerGameSchedulerTasks() {
        getSessionHandler().getGameScheduler().registerGameTask(this::spawnRepairCrystal, repairCrystal_startDelay);
    }

    @Override
    public void onPlayerLeaveGame(Player player) {
        player.removeAllEffects();
        checkWin();
    }

    @Override
    public void onRemovePlayerFromTeam(Player player, Team team) {
        checkWin();
    }

    @Override
    public void onGameDeathByBlock(GamePlayerDeathEvent event) {
        handleGameDeath(event);
    }

    @Override
    public void onGameMiscDeathEvent(GamePlayerDeathEvent event) {
        handleGameDeath(event);
    }

    @Override
    public void onGameDeathByEntity(GamePlayerDeathEvent event) {
        handleGameDeath(event);
    }

    @Override
    public void onGameDeathByPlayer(GamePlayerDeathEvent event) {
        handleGameDeath(event);
    }

    public void addUpgradeForTeam(Team team, CrystalTeamUpgrade upgrade) {
        upgrades.computeIfAbsent(team, (key) -> new HashSet<>());
        upgrades.get(team).add(upgrade);
    }

    public boolean doesTeamHaveUpgrade(Team team, CrystalTeamUpgrade upgrade) {
        return upgrades.containsKey(team) && upgrades.get(team).contains(upgrade);
    }

    @EventHandler
    private void onPlace(BlockPlaceEvent event) {
        if (getSessionHandler().getPlayers().contains(event.getPlayer())) {
            if (event.getBlock() instanceof BlockTNT) {
                Entity primedTntEntity = Entity.createEntity(EntityPrimedTNT.NETWORK_ID, event.getBlock().add(0.5, 0, 0.5));
                primedTntEntity.setMotion(new Vector3(0, 0.3, 0));
                primedTntEntity.spawnToAll();

                event.getPlayer().getInventory().removeItem(new BlockTNT().toItem());
                event.setCancelled(true);
            } else {
                placedBlocks.add(event.getBlock().getLocation());
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (getSessionHandler().getPlayers().contains(event.getPlayer())) {
            if (!placedBlocks.contains(event.getBlock().getLocation())) {
                event.setCancelled(true);
            } else {
                placedBlocks.remove(event.getBlock().getLocation());
                if (event.getPlayer().getInventory().getItemInHand().isSword()) {
                    event.setCancelled(false);
                }
            }
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (getSessionHandler().getPrimaryMap() == event.getEntity().getLevel()) {
            Iterator<Block> blockIterator = event.getBlockList().iterator();
            while (blockIterator.hasNext()) {
                Block block = blockIterator.next();
                if (block.getId() == BlockID.GLASS) {
                    blockIterator.remove();
                    continue;
                }
                if (!placedBlocks.contains(block)) {
                    blockIterator.remove();
                } else {
                    placedBlocks.remove(block);
                }
            }
        }
    }

    @EventHandler
    public void onRemoveArmor(InventoryMoveItemEvent event) {
        if (getSessionHandler().getPlayers().contains((Player)event.getSource()) && event.getItem().isArmor()) {
            event.setCancelled();
        }
    }

    public void spawnRepairCrystal() {
        if(repairRegions.size() > 0) {

            // -- Spawn the crystal entity
            int zoneIndex = repairRegions.size() == 1 ? 0 : random.nextInt(repairRegions.size());
            MapRegion zone = repairRegions.get(zoneIndex);

            // Randomise position: Snap to the lowest y.
            int y = zone.getPosLesser().getY();
            int dX = zone.getPosGreater().getX() - zone.getPosLesser().getX();
            int dZ = zone.getPosGreater().getZ() - zone.getPosLesser().getZ();

            int x = dX < 1 ? 0 : zone.getPosLesser().getX() + random.nextInt(dX + 1);
            int z = dZ < 1 ? 0 : zone.getPosLesser().getZ() + random.nextInt(dZ + 1);

            //TODO: Spawn entity.

            // -- Start the next spawn cycle.
            // check delay isn't the same or less than random bounds.
            int delta = repairCrystal_maxDelay - repairCrystal_minDelay;
            int delay = delta <= 0
                    ? repairCrystal_minDelay
                    : repairCrystal_minDelay + random.nextInt( delta + 1);
            // schedule again in [delay] ticks time.
            getSessionHandler().getGameScheduler().registerGameTask(this::spawnRepairCrystal, delay);
        }
    }

    protected void handleGameDeath(GamePlayerDeathEvent event) {
        if (crystalExistsForTeam(event.getDeathCause().getVictimTeam())) {
            event.setDeathState(GamePlayerDeathEvent.DeathState.TIMED_RESPAWN);
            event.setRespawnSeconds(5);
        } else {
            checkWin();
        }
    }


    protected void checkWin() {
        Team aliveTeam = null;
        for (Team team : getSessionHandler().getTeams().values()) {
            if (team.isActiveGameTeam() && (team.getPlayers().size() > 0 || crystalExistsForTeam(team))) {
                if (aliveTeam != null)
                    return;
                aliveTeam = team;
            }
        }
        if (aliveTeam != null) {
            getSessionHandler().declareVictoryForTeam(aliveTeam);
        }
    }

    protected boolean crystalExistsForTeam(Team team) {
        Optional<PointEntity> crystalPointEntity = getSessionHandler()
                .getPointEntityTypeManager()
                .getTypeLookup()
                .get(CrystalPointEntity.ID)
                .stream()
                .filter(entity -> entity.getStringProperties().getOrDefault(CrystalPointEntity.TEAM_ID_PROPERTY, "").equals(team.getId()))
                .findAny();

        return !crystalPointEntity.filter(entity -> ((CrystalPointEntity) getSessionHandler()
                .getPointEntityTypeManager()
                .getRegisteredTypes()
                .get(CrystalPointEntity.ID))
                .isCrystalDestroyed(entity)).isPresent();
    }

    @Override
    public Team.GenericTeamBuilder[] getTeams() {
        return TeamPresets.FOUR_TEAMS;
    }
}