package org.madblock.towerwars.behaviors;

import cn.nukkit.Player;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.newgamesapi.team.Team;
import org.madblock.towerwars.TowerWarsPlugin;
import org.madblock.towerwars.enemies.EnemyRegistry;
import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.enemies.types.EnemyType;
import org.madblock.towerwars.enemies.types.SilverfishEnemyType;
import org.madblock.towerwars.events.EventManager;
import org.madblock.towerwars.menu.MenuManager;
import org.madblock.towerwars.pathfinding.Pathfinder;
import org.madblock.towerwars.towers.TowerRegistry;
import org.madblock.towerwars.towers.tower.Tower;
import org.madblock.towerwars.towers.types.ArcherTowerType;
import org.madblock.towerwars.towers.types.TowerType;
import org.madblock.towerwars.utils.GameRegion;

import java.util.*;
import java.util.stream.Collectors;

// The base class lacks implementation of lives, money, and teams
public abstract class TowerWarsBehavior extends GameBehavior {

    private final EnemyRegistry enemyRegistry = new EnemyRegistry();
    private final TowerRegistry towerRegistry = new TowerRegistry();
    private final EventManager eventManager = new EventManager();
    private final MenuManager menuManager = new MenuManager(this);

    private Pathfinder pathfinder;

    private final Set<Enemy> activeEnemies = new HashSet<>();
    private final Set<Tower> activeTowers = new HashSet<>();

    // Mapped by teamId
    protected final BiMap<String, GameRegion> gameRegions = HashBiMap.create();


    public TowerWarsBehavior() {
        this.registerEnemies();
        this.registerTowers();
    }

    public abstract int getLives(Player player);
    public abstract void setLives(Player player, int lives);
    public abstract int getBalance(Player player);
    public abstract void setBalance(Player player, int balance);

    /**
     * Retrieve the player's game space
     * @param player
     * @return GameRegion
     */
    public abstract GameRegion getPlayerGameRegion(Player player);

    /**
     * Retrieve a owner for a game region
     * @param gameRegion
     * @return Player
     */
    public abstract Player getGameRegionOwner(GameRegion gameRegion);

    protected int getInitialBalance() {
        return 100;
    }
    protected int getInitialLives() {
        return 20;
    }

    protected void registerEnemies() {
        this.getEnemyRegistry().register(new SilverfishEnemyType(this));
    }
    protected void registerTowers() {
        this.getTowerRegistry().register(new ArcherTowerType(this));
    }

    //
    //  Internal game logic
    //

    public EnemyRegistry getEnemyRegistry() {
        return this.enemyRegistry;
    }

    public TowerRegistry getTowerRegistry() {
        return this.towerRegistry;
    }

    public MenuManager getMenuManager() {
        return this.menuManager;
    }

    public EventManager getEventManager() {
        return this.eventManager;
    }

    public Pathfinder getPathfinder() {
        return this.pathfinder;
    }

    public List<TowerType> getUnlockedTowerTypes(Player player) {
        // TODO: Implement calculation for figuring out what towers are unlocked
        return new ArrayList<>(this.getTowerRegistry().getTypes());
    }

    public List<EnemyType> getUnlockedEnemyTypes(Player player) {
        return new ArrayList<>(this.getEnemyRegistry().getTypes());
    }

    public List<Player> getActivePlayers() {
        return this.getSessionHandler().getPlayers().stream()
                .filter(p -> this.getSessionHandler().getPlayerTeam(p).filter(Team::isActiveGameTeam).isPresent())
                .collect(Collectors.toList());
    }

    public Set<GameRegion> getActiveGameRegions() {
        return this.gameRegions.values()
                .stream()
                .filter(gameRegion -> this.getActivePlayers().contains(this.getGameRegionOwner(gameRegion)))
                .collect(Collectors.toSet());
    }

    /**
     * Retrieve all active enemies that are within a region.
     * @param region The region
     * @return
     */
    public Set<Enemy> getActiveEnemies(GameRegion region) {
        return this.activeEnemies
                .stream()
                .filter(enemy -> enemy.getGameRegion().getPlayArea().getUniqueIdentifier().equals(region.getPlayArea().getUniqueIdentifier()))
                .collect(Collectors.toSet());
    }

    public void addEnemy(Enemy enemy) {
        this.activeEnemies.add(enemy);
    }

    public void removeEnemy(Enemy enemy) {
        this.activeEnemies.remove(enemy);
    }

    public Set<Tower> getActiveTowers() {
        return Collections.unmodifiableSet(this.activeTowers);
    }

    public void addTower(Tower tower) {
        this.activeTowers.add(tower);
    }

    public void removeTower(Tower tower) {
        this.activeTowers.remove(tower);
    }

    @Override
    public void onInitialCountdownEnd() {
        this.pathfinder = new Pathfinder(this.getSessionHandler().getPrimaryMap());
        this.updateScoreboardTask();

        // Parse MapID regions
        Arrays.stream(this.getTeams())
                .filter(Team.GenericTeamBuilder::isActiveGameTeam)
                .map(Team.GenericTeamBuilder::getId)
                .forEach(teamId -> {
                    Map<String, MapRegion> regions = this.getSessionHandler().getPrimaryMapID().getRegions();

                    String playAreaId = teamId + "_area";
                    String goalId = teamId + "_goal";
                    String spawnId = teamId + "_spawn";

                    if (!regions.containsKey(playAreaId)) {
                        TowerWarsPlugin.get().getLogger().error("Missing region " + playAreaId);
                        return;
                    }
                    if (!regions.containsKey(goalId)) {
                        TowerWarsPlugin.get().getLogger().error("Missing region " + goalId);
                        return;
                    }
                    if (!regions.containsKey(spawnId)) {
                        TowerWarsPlugin.get().getLogger().error("Missing region " + spawnId);
                        return;
                    }

                    MapRegion playArea = regions.get(playAreaId);
                    MapRegion goalArea = regions.get(goalId);
                    MapRegion spawnArea = regions.get(spawnId);

                    this.gameRegions.put(teamId, new GameRegion(playArea, spawnArea, goalArea));

                });
    }

    @Override
    public void registerGameSchedulerTasks() {
        this.getSessionHandler().getGameScheduler().registerGameTask(this::gameTick, 0, 1);
    }

    @Override
    public void cleanUp() {
        this.getMenuManager().cleanUp();
        this.getPathfinder().cleanUp();
    }


    protected void updateScoreboardTask() {
        for (Player player : this.getActivePlayers()) {
            this.updateScoreboardCoinsFor(player);
            this.updateScoreboardLivesFor(player);
        }
    }

    protected void updateScoreboardCoinsFor(Player player) {
        this.getSessionHandler().getScoreboardManager().setLine(player, 0, Utility.ResourcePackCharacters.COIN + " " + this.getBalance(player));
    }

    protected void updateScoreboardLivesFor(Player player) {
        this.getSessionHandler().getScoreboardManager().setLine(player, 1, Utility.ResourcePackCharacters.HEART_FULL + " " + this.getLives(player));
    }

    private void gameTick() {
        this.activeTowers.iterator()
                .forEachRemaining(Tower::tick);
        this.activeEnemies.iterator()
                .forEachRemaining(Enemy::tick);
    }

}
