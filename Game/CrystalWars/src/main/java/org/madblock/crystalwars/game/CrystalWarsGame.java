package org.madblock.crystalwars.game;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockTNT;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.item.EntityPrimedTNT;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.entity.EntityExplodeEvent;
import cn.nukkit.event.inventory.InventoryMoveItemEvent;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.TextFormat;
import org.madblock.crystalwars.game.pointentities.team.CrystalPointEntity;
import org.madblock.crystalwars.game.upgrades.CrystalTeamUpgrade;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.game.events.GamePlayerDeathEvent;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.newgamesapi.team.Team;

import java.util.*;

/**
 * @author Nicholas
 */
public class CrystalWarsGame extends GameBehavior {
    protected Set<Vector3> placedBlocks = new HashSet<>();
    protected Map<Team, Set<CrystalTeamUpgrade>> upgrades = new HashMap<>();

    @Override
    public void onInitialCountdownEnd() {

    }

    @Override
    public void registerGameSchedulerTasks() {
        getSessionHandler().getGameScheduler().registerGameTask(this::updateActionBar, 0, 40);
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

    protected void handleGameDeath(GamePlayerDeathEvent event) {
        if (crystalExistsForTeam(event.getDeathCause().getVictimTeam())) {
            event.setDeathState(GamePlayerDeathEvent.DeathState.TIMED_RESPAWN);
            int respawnSeconds = 5;
            event.setRespawnSeconds(respawnSeconds);
            event.getDeathCause().getVictim().sendTitle(TextFormat.RED + "OUCH!", TextFormat.RED + "Respawning in " +
                    respawnSeconds + " seconds...", 0, 40, 15);
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

    protected void updateActionBar() {
        StringBuilder textToDisplay = new StringBuilder();
        for (Team team : getSessionHandler().getTeams().values()) {
            if (!team.isActiveGameTeam())
                continue;
            boolean crystalExists = crystalExistsForTeam(team);

            char crystalCharacter = crystalExists ? '*' : ' ';
            int playerCount = team.getPlayers().size();

            if (crystalExists || playerCount > 0) {
                textToDisplay.append(String.format("%s[%s] %s [%s] ", team.getColour().getColourString(), crystalCharacter, team.getDisplayName(), playerCount));
            }
        }

        for (Player player : getSessionHandler().getPlayers()) {
            player.sendActionBar(textToDisplay.toString().trim(), 0, 1, 0);
        }
    }
}