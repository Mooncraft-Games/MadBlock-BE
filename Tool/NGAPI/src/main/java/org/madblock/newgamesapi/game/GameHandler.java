package org.madblock.newgamesapi.game;

import cn.nukkit.AdventureSettings;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.event.inventory.InventoryPickupItemEvent;
import cn.nukkit.event.player.PlayerDropItemEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.level.Position;
import cn.nukkit.level.generator.Generator;
import cn.nukkit.scheduler.TaskHandler;
import cn.nukkit.utils.DummyBossBar;
import cn.nukkit.utils.TextFormat;
import org.madblock.lib.stattrack.statistic.StatisticCollection;
import org.madblock.lib.stattrack.statistic.StatisticEntitiesList;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.exception.LackOfContentException;
import org.madblock.newgamesapi.game.deaths.DeathManager;
import org.madblock.newgamesapi.game.pvp.CustomPVPManager;
import org.madblock.newgamesapi.game.scheduler.GameScheduler;
import org.madblock.newgamesapi.game.scheduler.tasks.TaskQueueCountdown;
import org.madblock.newgamesapi.game.scheduler.tasks.TaskStartSessionLoops;
import org.madblock.newgamesapi.kits.Kit;
import org.madblock.newgamesapi.kits.KitGroup;
import org.madblock.newgamesapi.kits.PlayerKitsManager;
import org.madblock.newgamesapi.map.MapID;
import org.madblock.newgamesapi.map.MapManager;
import org.madblock.newgamesapi.map.functionalregions.defaults.TagBehaviorFixedLaunchRegion;
import org.madblock.newgamesapi.map.functionalregions.FunctionalRegionManager;
import org.madblock.newgamesapi.map.functionalregions.defaults.TagBehaviorVarLaunchRegion;
import org.madblock.newgamesapi.map.functionalregions.defaults.TagBehaviorDeathbox;
import org.madblock.newgamesapi.map.functionalregions.defaults.TagBehaviorInvertedDeathbox;
import org.madblock.newgamesapi.map.pointentities.PointEntityTypeManager;
import org.madblock.newgamesapi.map.pointentities.defaults.PointEntityTypeFirework;
import org.madblock.newgamesapi.map.pointentities.defaults.PointEntityTypeHologram;
import org.madblock.newgamesapi.map.pointentities.defaults.PointEntityTypeInteractableNPC;
import org.madblock.newgamesapi.map.pointentities.defaults.PointEntityTypeLootchest;
import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.newgamesapi.registry.KitRegistry;
import org.madblock.newgamesapi.rewards.PlayerRewardsProfile;
import org.madblock.newgamesapi.rewards.RewardChunk;
import org.madblock.newgamesapi.rewards.RewardsManager;
import org.madblock.newgamesapi.team.DeadTeam;
import org.madblock.newgamesapi.team.Team;
import org.madblock.newgamesapi.team.TeamPopulator;
import org.madblock.newgamesapi.team.TeamPresets;
import org.apache.commons.io.FileUtils;
import org.madblock.ranks.api.RankManager;
import org.madblock.ranks.api.RankProfile;
import org.madblock.ranks.enums.PrimaryRankID;

import java.io.File;
import java.sql.SQLException;
import java.util.*;

public class GameHandler implements Listener {

    public static final int TOURNEY_WIN_POINTS = 5;
    public static final int TOURNEY_TOP3_POINTS = 2;

    protected String token;
    protected GameManager gameManager;
    protected int cleanupWaitTime;

    protected String serverID;
    protected GameID gameID;
    protected GameBehavior gameBehaviors;
    protected GameState gameState;

    protected MapID mapID;
    protected Level primaryMap;
    protected HashMap<String, Level> additionalLevels;

    protected HashMap<String, Team> teams;
    protected HashSet<Player> players;
    protected HashMap<Player, HashMap<String, RewardChunk>> rewardChunks;
    protected HashMap<Player, Kit> kits;
    protected HashMap<Player, ArrayList<DummyBossBar>> bossbars;

    protected TeamPopulator teamPopulator;
    protected GameScheduler gameScheduler;
    protected SpawnManager spawnManager;
    protected DeathManager deathManager;
    protected FunctionalRegionManager functionalRegionManager;
    protected PointEntityTypeManager pointEntityTypeManager;
    protected ScoreboardManager scoreboardManager;
    protected CustomPVPManager customPVPManager;

    protected HashSet<Player> tourneyMasters;
    protected boolean tourneyStarted;

    protected ArrayList<TaskHandler> taskHandlers;

    public GameHandler(GameID id, GameBehavior behavior, String serverID, MapID mapID, Level mapLevel, GameManager gameManager, int cleanUpWaitTime){
        this.token = Utility.generateUniqueToken(4, 6);
        this.gameManager = gameManager;
        this.cleanupWaitTime = cleanUpWaitTime;

        this.serverID = serverID;
        this.gameID = id;
        this.gameBehaviors = behavior;
        this.gameState = GameState.PRE_PREPARE;

        this.mapID = mapID;
        this.primaryMap = mapLevel;
        this.additionalLevels = new HashMap<>();
        this.primaryMap.getGameRules().setGameRule(GameRule.NATURAL_REGENERATION, gameID.getGameProperties().isNatualRegenerationEnabled());

        this.teams = new HashMap<>();
        this.players = new HashSet<>();
        this.rewardChunks = new HashMap<>();
        this.kits = new HashMap<>();
        this.bossbars = new HashMap<>();

        this.tourneyMasters = new HashSet<>();
        this.tourneyStarted = false;

        this.taskHandlers = new ArrayList<>();
        if(this.gameBehaviors.getTeams().length == 0 || this.gameBehaviors.getTeams() == null){
            //Throw a hissy fit.
            throw new LackOfContentException("Missing teams for game: "+id.getGameIdentifier());
        }
    }

    /**
     * Prepares the game for a start, beginning setup.
     * @param playersIn The players to join the game in a batch.
     * @return any players which were rejected from the game.
     */
    public Player[] prepare(Player[] playersIn){
        if(gameState != GameState.PRE_PREPARE){ return playersIn; }
        gameState = GameState.PREPARING;
        if(playersIn.length < getGameID().getGameProperties().getMinimumPlayers()) return playersIn;

        NewGamesAPI1.get().getServer().getPluginManager().registerEvents(this, NewGamesAPI1.get());
        for(Team.GenericTeamBuilder team: this.gameBehaviors.getTeams()){ teams.put(team.getId(), team.build(this)); }
        for(Team.GenericTeamBuilder team: TeamPresets.DEFAULT){ teams.put(team.getId(), team.build(this)); }
        this.gameBehaviors.setSessionHandler(this);
        this.teamPopulator = new TeamPopulator(this);
        this.gameScheduler = new GameScheduler(this, token);
        this.spawnManager = new SpawnManager(this, teams, getPrimaryMapID(), mapID.getSwitches().getOrDefault("shuffle_spawns", getGameID().getGameProperties().doesGameShufflePlayerSpawns()));
        this.deathManager = new DeathManager(this);
        this.functionalRegionManager = new FunctionalRegionManager(this);
        this.pointEntityTypeManager = new PointEntityTypeManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.customPVPManager = new CustomPVPManager(this);

        for(MapRegion region: mapID.getRegions().values()) functionalRegionManager.registerRegion(region, primaryMap);
        for(PointEntity entity: mapID.getPointEntities().values()) pointEntityTypeManager.addPointEntity(entity, primaryMap);

        ArrayList<Player> rejects = new ArrayList<>(Arrays.asList(playersIn));


        for(int i = 0; (i < playersIn.length) && (i < getGameID().getGameProperties().getMaximumPlayers()); i++){
            Player player = playersIn[i];
            boolean passed = true;
            for(String permission: getGameID().getGameProperties().getRequiredPermissions()){
                Optional<RankProfile> profile = RankManager.getInstance().getRankProfile(player);
                if (!profile.isPresent() || !profile.get().hasPermission(permission)) {
                    player.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You do not have permission to join this game.", TextFormat.RED));
                    passed = false;
                    break;
                }
            }
            if(passed && registerPlayerInGame(player)) rejects.remove(player);
        }

        gameState = GameState.PRE_COUNTDOWN;
        int onGameBeginResult = gameBehaviors.onGameBegin();
        NewGamesAPI1.get().getServer().getPluginManager().registerEvents(getGameBehaviors(), NewGamesAPI1.get());
        NewGamesAPI1.get().getServer().getPluginManager().registerEvents(deathManager, NewGamesAPI1.get());
        NewGamesAPI1.get().getServer().getPluginManager().registerEvents(spawnManager, NewGamesAPI1.get());
        NewGamesAPI1.get().getServer().getPluginManager().registerEvents(customPVPManager, NewGamesAPI1.get());
        NewGamesAPI1.get().getServer().getScheduler().scheduleDelayedTask(new TaskQueueCountdown(token, this), onGameBeginResult*20);

        applyIntegratedFeatures();

        return rejects.toArray(new Player[0]);
    }

    public boolean addPlayerToGame(Player player){
        if(gameState == GameState.PRE_PREPARE){
            return prepare(new Player[]{player}).length > 0; //Rejects has players meaning the game couldn't start.
            // I hate how inelegant this fix is but it works I guess.
            // This is why next time there'll only be one public facing player join process.
        }
        if(!(gameState == GameState.END)) {
            return registerPlayerInGame(player);
        }
        return false;
    }

    // I hate this. Seperate players and spectators in 2.0 PLEASEEEEE
    public boolean registerTourneymasterToGame(Player player){
        if(players.contains(player)) return false;
        if(tourneyMasters.contains(player)) return false;

        if((!((gameState == GameState.PRE_PREPARE) || (gameState == GameState.END))) && getGameID().getGameProperties().isTourneyGamemode()){
            gameManager.appendPlayerToLookup(player, this);
            tourneyMasters.add(player);

            player.getAdventureSettings()
                    .set(AdventureSettings.Type.FLYING, true)
                    .set(AdventureSettings.Type.NO_CLIP, true)
                    .set(AdventureSettings.Type.BUILD_AND_MINE, true)
                    .set(AdventureSettings.Type.WORLD_BUILDER, true)
                    .set(AdventureSettings.Type.WORLD_IMMUTABLE, false)
                    .set(AdventureSettings.Type.ATTACK_PLAYERS, true)
                    .set(AdventureSettings.Type.ATTACK_MOBS, true)
                    .set(AdventureSettings.Type.DOORS_AND_SWITCHED, true)
                    .set(AdventureSettings.Type.ALLOW_FLIGHT, true)
                    .set(AdventureSettings.Type.OPEN_CONTAINERS, true);
            Utility.executeExperimentalAdventureSettingsUpdate(player);
            Utility.setGamemodeWorkaround(player, Player.CREATIVE, false, player.getAdventureSettings());

            player.getFoodData().setLevel(20, 20);
            player.setFoodEnabled(false);

            for(Player p: NewGamesAPI1.get().getServer().getOnlinePlayers().values()) p.hidePlayer(player);

            String name = TextFormat.GOLD + "" +TextFormat.BOLD + Utility.ResourcePackCharacters.TAG_TOURNEY + " " + TextFormat.RESET + TextFormat.GOLD + player.getName();
            player.setNameTag(name);
            player.setDisplayName(name);

            KitRegistry.get().getKitGroup("core").ifPresent(k -> {
                equipPlayerKit(player, k.getGroupKits().getOrDefault("spectate", k.getDefaultKit()), true);
            });

            spawnManager.placePlayerInSpawnPosition(player, teams.get(TeamPresets.SPECTATOR_TEAM_ID));
            return true;
        }
        return false;
    }

    // I hate this. Seperate players and spectators in 2.0 PLEASEEEEE
    public boolean removeTourneymasterFromGame(Player player, boolean beingTransferredToNewGame){
        if(tourneyMasters.contains(player)){
            tourneyMasters.remove(player);

            player.getAdventureSettings()
                    .set(AdventureSettings.Type.FLYING, false)
                    .set(AdventureSettings.Type.NO_CLIP, false)
                    .set(AdventureSettings.Type.BUILD_AND_MINE, true)
                    .set(AdventureSettings.Type.WORLD_BUILDER, true)
                    .set(AdventureSettings.Type.WORLD_IMMUTABLE, false)
                    .set(AdventureSettings.Type.ATTACK_PLAYERS, true)
                    .set(AdventureSettings.Type.ATTACK_MOBS, true)
                    .set(AdventureSettings.Type.DOORS_AND_SWITCHED, true)
                    .set(AdventureSettings.Type.ALLOW_FLIGHT, false)
                    .set(AdventureSettings.Type.OPEN_CONTAINERS, true);
            Utility.executeExperimentalAdventureSettingsUpdate(player);
            Utility.setGamemodeWorkaround(player, Player.SURVIVAL, false, player.getAdventureSettings());

            player.getFoodData().setLevel(20, 20);
            player.setFoodEnabled(true);

            for(Player p: NewGamesAPI1.get().getServer().getOnlinePlayers().values()) p.showPlayer(player);

            Optional<RankProfile> rankProfile = RankManager.getInstance().getRankProfile(player);
            if (rankProfile.isPresent()) {
                PrimaryRankID displayRank = rankProfile.get().getPrimaryDisplayedRank();
                if (displayRank.getName().isPresent()) {
                    player.setNameTag("" + displayRank.getColor().orElse(TextFormat.WHITE) + TextFormat.BOLD + displayRank.getName().get() + TextFormat.RESET + " " + player.getName());
                } else {
                    player.setNameTag(player.getName());
                }
                player.setDisplayName(player.getName());
            } else {
                player.setNameTag(player.getName());
                player.setDisplayName(player.getName());
            }

            if (kits.containsKey(player)) {
                kits.get(player).removeKit(player, this, true);
            }

            gameManager.removePlayerFromLookup(player, this);
            if(!beingTransferredToNewGame) {
                Position spawn = NewGamesAPI1.get().getServer().getDefaultLevel().getSpawnLocation();
                Location dest = spawn.getLocation();
                dest.setLevel(NewGamesAPI1.get().getServer().getDefaultLevel());
                player.switchLevel(dest.getLevel());
                player.teleport(dest);
            }
            return true;
        }
        return false;
    }

    protected boolean registerPlayerInGame(Player player){
        if((players.size() < gameID.getGameProperties().getMaximumPlayers()) && (!players.contains(player))) {
            players.add(player);
            String teamid = null;
            switch (gameState) {
                case PREPARING:
                case PRE_COUNTDOWN:
                    Optional<Team> potentialPrepTeam = getGameBehaviors().onPreGameJoinEvent(player);
                    if (potentialPrepTeam.isPresent()) {
                        Team t = potentialPrepTeam.get();
                        teamid = teams.containsValue(t) ? t.getId() : teamPopulator.getPriorityFillTeam();
                    } else {
                        teamid = teamPopulator.getPriorityFillTeam();
                    }
                    player.setImmobile(!getGameID().getGameProperties().canPlayersMoveDuringCountdown());
                    break;
                case COUNTDOWN:
                    Optional<Team> potentialCountdownTeam = getGameBehaviors().onCountdownJoinEvent(player);
                    if (potentialCountdownTeam.isPresent()) {
                        Team t = potentialCountdownTeam.get();
                        teamid = teams.containsValue(t) ? t.getId() : teamPopulator.getPriorityFillTeam();
                    } else {
                        teamid = teamPopulator.getPriorityFillTeam();
                    }
                    player.setImmobile(!getGameID().getGameProperties().canPlayersMoveDuringCountdown());
                    break;
                case PRE_MAIN_LOOP:
                case MAIN_LOOP:
                    Optional<Team> potentialLoopTeam = getGameBehaviors().onMidGameJoinEvent(player);
                    if (potentialLoopTeam.isPresent()) {
                        Team t = potentialLoopTeam.get();
                        teamid = teams.containsValue(t) ? t.getId() : TeamPresets.SPECTATOR_TEAM_ID;
                    } else {
                        teamid = TeamPresets.SPECTATOR_TEAM_ID;
                    }
                    player.setImmobile(false);
                    break;
                default:
                    players.remove(player);
                    player.setImmobile(false);
                    return false;
            }
            Team targetTeam = teams.get(teamid);
            gameManager.appendPlayerToLookup(player, this);
            switchPlayerToTeam(player, targetTeam, false);
            spawnManager.placePlayerInSpawnPosition(player, targetTeam);
            bossbars.put(player, new ArrayList<>());
            rewardChunks.put(player, new HashMap<>());

            if (targetTeam.isActiveGameTeam()) {
                KitGroup group = getGameID().getGameKits();
                PlayerKitsManager prefs = PlayerKitsManager.get();
                String preference = prefs.getPlayerPreferenceForGroup(player, group);
                Kit kit = group.getGroupKits().getOrDefault(preference, group.getDefaultKit());
                kit.applyKit(player, this, true);
            } else {
                KitRegistry.get().getKitGroup("core").ifPresent(k -> {
                    equipPlayerKit(player, k.getGroupKits().getOrDefault("spectate", k.getDefaultKit()), true);
                });
            }

            switch (gameState){
                case COUNTDOWN:
                    TaskQueueCountdown.executePerPlayerActivites(player, this);
                    break;
                case MAIN_LOOP:
                    TaskStartSessionLoops.preparePlayer(player, this);
                    break;
            }
            return true;
        }
        return false;
    }

    public void removePlayerFromGame(Player player) {
        removePlayerFromGame(player, false);
    }

    public void removePlayerFromGame(Player player, boolean beingTransferredToNewGame){
        if(tourneyMasters.contains(player)) {
            removeTourneymasterFromGame(player, beingTransferredToNewGame);
            return;
        }

        if(players.contains(player)) {
            gameBehaviors.onPlayerLeaveGame(player);
            if (kits.containsKey(player)) {
                kits.get(player).removeKit(player, this, true);
            }
            for (Team team : getTeams().values()) {
                if (team.getPlayers().contains(player)) {
                    team.removePlayerFromTeam(player);
                    break;
                }
            }
            if (bossbars.containsKey(player)) {
                for (DummyBossBar bossBar : bossbars.get(player)) {
                    bossBar.destroy();
                }
                bossbars.remove(player);
            }
            scoreboardManager.cleanUp(player);
            players.remove(player);
            gameManager.removePlayerFromLookup(player, this);
            player.getEnderChestInventory().clearAll();
            if(!beingTransferredToNewGame) {
                Position spawn = NewGamesAPI1.get().getServer().getDefaultLevel().getSpawnLocation();
                Location dest = spawn.getLocation();
                dest.setLevel(NewGamesAPI1.get().getServer().getDefaultLevel());
                player.switchLevel(dest.getLevel());
                player.teleport(dest);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLeave(PlayerQuitEvent event){
        removePlayerFromGame(event.getPlayer());
        scoreboardManager.cleanUp(event.getPlayer());
        checkDeathWinPolicyConditions();
    }

    public void applyIntegratedFeatures(){
        if(getGameID().getGameProperties().doesGameUseIntegratedDeathboxes()){
            functionalRegionManager.setTagFunction("deathbox", new TagBehaviorDeathbox(this), 2);
            functionalRegionManager.setTagFunction("deathbox_inverted", new TagBehaviorInvertedDeathbox(this), 2, 1);
            functionalRegionManager.setTagFunction("var_launch", new TagBehaviorVarLaunchRegion(this), 3);
            functionalRegionManager.setTagFunction("fixed_launch", new TagBehaviorFixedLaunchRegion(this), 3);
        }
        if(getGameID().getGameProperties().doesGameUseIntegratedPointEntities()){
            pointEntityTypeManager.registerPointEntityType(new PointEntityTypeInteractableNPC(this));
            pointEntityTypeManager.registerPointEntityType(new PointEntityTypeLootchest(this));
            pointEntityTypeManager.registerPointEntityType(new PointEntityTypeHologram(this));
            pointEntityTypeManager.registerPointEntityType(new PointEntityTypeFirework(this));
        }
    }

    public boolean enterMainLoop(String token){
        if(this.token.equals(token)){
            getGameScheduler().registerGameTask(this::checkDeathWinPolicyConditions, 0, 10);
            getGameScheduler().registerGameTask(this::checkAutomaticClosePolicy, 5, 10);
            for(Team team: getTeams().values()) team.updateTeamBuildingPermissionsState();

            applyGeneralGameStats();

            return true;
        } else return false;
    }

    public void applyGeneralGameStats() {
        StatisticCollection mapStats = StatisticEntitiesList.get().createCollection(mapID);
        StatisticCollection gameStats = StatisticEntitiesList.get().createCollection(gameID);

        mapStats.createStatistic("times_played").increment();
        mapStats.createStatistic("total_players").modify(players.size()); // used to average the player counts

        gameStats.createStatistic("games_started").increment();
        gameStats.createStatistic("total_players").modify(players.size());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK && getPlayers().contains(event.getPlayer())) {
            if(gameState != GameState.MAIN_LOOP) {
                event.setCancelled(true);
                return;
            }
            for(Team team: teams.values()){
                if(team.getPlayers().contains(event.getPlayer())){
                    if(!team.canPlayersInteractWithBlocks()) {

                        switch (event.getBlock().getId()) {
                            case Block.CRAFTING_TABLE:
                            case Block.FURNACE:
                            case Block.ANVIL:
                            case Block.ENCHANTING_TABLE:
                            case Block.CHEST:
                            case Block.TRAPPED_CHEST:
                            case Block.ENDER_CHEST:
                            case Block.ITEM_FRAME_BLOCK:
                            case Block.STONE_BUTTON:
                            case Block.WOODEN_BUTTON:
                            case Block.ACACIA_DOOR_BLOCK:
                            case Block.BIRCH_DOOR_BLOCK:
                            case Block.JUNGLE_DOOR_BLOCK:
                            case Block.SPRUCE_DOOR_BLOCK:
                            case Block.WOODEN_DOOR_BLOCK:
                            case Block.DRAGON_EGG:
                            case Block.TRAPDOOR:
                                event.setCancelled(true);
                                break;
                        }

                    }
                    return;
                }
            }

        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerItemPickUp(InventoryPickupItemEvent event) {
        if(event.getInventory().getHolder() instanceof Player && getPlayers().contains(event.getInventory().getHolder())){
            Player player = (Player) event.getInventory().getHolder();
            if(gameState == GameState.END) {
                event.setCancelled(true);
                return;
            }
            if(gameState == GameState.MAIN_LOOP){
                if(!gameID.getGameProperties().isItemPickUpEnabled()){
                    event.setCancelled(true);
                    return;
                }
            } else {
                if(!gameID.getGameProperties().isItemPickUpEnabledPreGame()){
                    event.setCancelled(true);
                    return;
                }
            }
            for(Team team: teams.values()){
                if(team.getPlayers().contains(player)){
                    if(!team.canPlayersPickUpItems()) event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerItemDrop(PlayerDropItemEvent event){
        if(getPlayers().contains(event.getPlayer())){
            if(gameState == GameState.END) {
                event.setCancelled(true);
                return;
            }
            if(gameState == GameState.MAIN_LOOP){
                if(!gameID.getGameProperties().isItemDroppingEnabled()){
                    event.setCancelled(true);
                    return;
                }
            } else {
                if(!gameID.getGameProperties().isItemDroppingEnabledPreGame()){
                    event.setCancelled(true);
                    return;
                }
            }
            for(Team team: teams.values()){
                if(team.getPlayers().contains(event.getPlayer())){
                    if(!team.canPlayersDropItems()) event.setCancelled(true);
                    return;
                }
            }
        }
    }

    public void onTeamAddPlayer(Player player, Team team){
        if(teams.containsValue(team) && team.getPlayers().contains(player)){
            gameBehaviors.onAddPlayerToTeam(player, team);
        }
    }

    public void onTeamRemovePlayer(Player player, Team team){
        if(players.contains(player) && teams.containsValue(team)){
            gameBehaviors.onRemovePlayerFromTeam(player, team);
        }
    }

    /**
     * Removes player from other teams and adds them to this team.
     * @param player the player that the switch should be performed on.
     * @param target the target team which the player should be switched to.
     * @return true if the switch occured.
     */
    public boolean switchPlayerToTeam(Player player, Team target){
        return switchPlayerToTeam(player, target, null, false);
    }

    /**
     * Removes player from other teams and adds them to this team.
     * @param player the player that the switch should be performed on.
     * @param target the target team which the player should be switched to.
     * @param previousTeam the team the player was last on (If null, the game will figure it out)
     * @return true if the switch occured.
     */
    public boolean switchPlayerToTeam(Player player, Team target, Team previousTeam){
        return switchPlayerToTeam(player, target, previousTeam, false);
    }

    /**
     * Removes player from other teams and adds them to this team.
     * @param player the player that the switch should be performed on.
     * @param target the target team which the player should be switched to.
     * @param silentSwitch should the switch quiet (true), or be announced (false)
     * @return true if the switch occured.
     */
    public boolean switchPlayerToTeam(Player player, Team target, boolean silentSwitch){
        return switchPlayerToTeam(player, target, null, silentSwitch);
    }

    /**
     * Removes player from other teams and adds them to this team.
     * @param player the player that the switch should be performed on.
     * @param target the target team which the player should be switched to.
     * @param previousTeam the team the player was last on (If null, the game will figure it out)
     * @param silentSwitch should the switch quiet (true), or be announced (false)
     * @return true if the switch occured.
     */
    public boolean switchPlayerToTeam(Player player, Team target, Team previousTeam, boolean silentSwitch){
        Team oldteam = null;
        if(previousTeam == null) {
            Optional<Team> t = getPlayerTeam(player);
            if (!t.isPresent()) {
                boolean result = target.addPlayerToTeam(player);
                if ((!silentSwitch) && result) {
                    player.sendMessage(Utility.generateServerMessage("TEAMS", TextFormat.BLUE, String.format("You have been moved to team %s.", target.getFormattedDisplayName())));
                }
                return result;
            } else {
                oldteam = t.get();
            }
        } else {
            oldteam = previousTeam;
        }
        if(target != oldteam) {
            if(teams.containsValue(target)){
                boolean result;
                oldteam.removePlayerFromTeam(player);
                if(target instanceof DeadTeam){
                    result = ((DeadTeam) target).addPlayerToTeamAsDead(player, oldteam);
                } else {
                    result = target.addPlayerToTeam(player);
                }
                if((!silentSwitch) && result){
                    player.sendMessage(Utility.generateServerMessage("TEAMS", TextFormat.BLUE, String.format("You have been moved from team %s %s%sto team %s.", oldteam.getFormattedDisplayName(), TextFormat.RESET, Utility.DEFAULT_TEXT_COLOUR, target.getFormattedDisplayName())));
                }
                return result;
            }
        }
        return false;
    }

    public Optional<Team> getPlayerTeam(Player player){
        Team t = null;
        for(Team team: getTeams().values()){
            if(team.getPlayers().contains(player)) t = team;
        }
        return Optional.ofNullable(t);
    }

    public Optional<Kit> getPlayerKit(Player player) {
        if (kits.containsKey(player)) {
            return Optional.of(kits.get(player));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Generates a level for the game, adding the game session id.
     * @return true if the level was generated. False can mean it already exists or there was an error.
     */
    public boolean generateLevel(String name, long seed){
        return generateLevel(name, seed, null, new HashMap<>());
    }

    /**
     * Generates a level for the game, adding the game session id.
     * @return true if the level was generated. False can mean it already exists or there was an error.
     */
    public boolean generateLevel(String name, long seed, Class<? extends Generator> generator){
        return generateLevel(name, seed, generator, new HashMap<>());
    }

    /**
     * Generates a level for the game, adding the game session id.
     * @return true if the level was generated. False can mean it already exists or there was an error.
     */
    public boolean generateLevel(String name, long seed, Class<? extends Generator> generator, HashMap<String, Object> options){
        if(name.toLowerCase().equals("map")) return false;
        String levelName = MapManager.parseLevelNameStringFromSeries(serverID, name);
        if(Server.getInstance().generateLevel(levelName, seed, generator, options)){
            Level level = NewGamesAPI1.get().getServer().getLevelByName(levelName);
            level.getProvider().updateLevelName(levelName);
            level.getGameRules().setGameRule(GameRule.NATURAL_REGENERATION, gameID.getGameProperties().isNatualRegenerationEnabled());
            additionalLevels.put(name, level);
            return true;
        }
        return false;
    }

    public void equipPlayerKit(Player player, Kit kit, boolean clearWholeInventory){
        kit.applyKit(player, this, clearWholeInventory);
    }

    public Optional<Kit> removePlayerKit(Player player, boolean clearWholeInventory){
        Kit prevkit = kits.get(player);
        if(prevkit != null){
            prevkit.removeKit(player, this, clearWholeInventory);
        }
        return Optional.ofNullable(prevkit);
    }

    public void addRewardChunk(Player player, RewardChunk rewardChunk){
        if(this.gameState == GameState.MAIN_LOOP){
            addBypassRewardChunk(player, rewardChunk);
        }
    }

    private void addBypassRewardChunk(Player player, RewardChunk rewardChunk){
        HashMap<String, RewardChunk> chunks = rewardChunks.get(player);
        if(chunks != null){
            if(chunks.containsKey(rewardChunk.getInternalID())){
                chunks.get(rewardChunk.getInternalID()).appendChunk(rewardChunk);
            } else {
                chunks.put(rewardChunk.getInternalID(), rewardChunk);
            }
        }
    }

    public boolean checkDeathWinPolicyConditions(){
        AutomaticWinPolicy winPolicy = getGameID().getGameProperties().getWinPolicy();
        if(winPolicy == AutomaticWinPolicy.OPPOSING_PLAYERS_DEAD){
            ArrayList<Player> playerPool = new ArrayList<>();
            for(Team team: getTeams().values()){
                if(team.isActiveGameTeam()){
                    playerPool.addAll(team.getPlayers());
                    if(playerPool.size() > 1){
                        // Condition is already false. Skip ahead.
                        return false;
                    }
                }
            }
            ArrayList<Player> deathLog = deathManager.getPlayerDeathOrder();
            if(playerPool.size() < 1){
                declareVictoryInPlayerOrder(
                        deathLog.size() > 0 ? deathLog.get(0) : null,
                        deathLog.size() > 1 ? deathLog.get(1) : null,
                        deathLog.size() > 2 ? deathLog.get(2) : null
                        );
                return true;
            }
            if(playerPool.size() == 1){
                deathLog.remove(playerPool.get(0));
                declareVictoryInPlayerOrder(
                        playerPool.get(0),
                        deathLog.size() > 0 ? deathLog.get(0) : null,
                        deathLog.size() > 1 ? deathLog.get(1) : null);
                return true;
            }
            return false;
        }
        if(winPolicy == AutomaticWinPolicy.OPPOSING_TEAMS_DEAD){
            Team largestTeam = null;
            for(Team team: getTeams().values()){
                if(team.isActiveGameTeam()) {
                    if (team.getPlayers().size() > 0) {
                        if(largestTeam == null){
                            largestTeam = team;
                        } else {
                            return false; // There's already a team so it's false.
                        }
                    }
                }
            }
            ArrayList<Team> teamDeathLog = deathManager.getTeamDeathOrder();
            if(largestTeam == null){
                declareVictoryInTeamOrder(
                        teamDeathLog.size() > 0 ? teamDeathLog.get(0) : null,
                        teamDeathLog.size() > 1 ? teamDeathLog.get(1) : null,
                        teamDeathLog.size() > 2 ? teamDeathLog.get(2) : null
                );
            } else {
                teamDeathLog.remove(largestTeam);
                declareVictoryInTeamOrder(
                        largestTeam,
                        teamDeathLog.size() > 0 ? teamDeathLog.get(0) : null,
                        teamDeathLog.size() > 1 ? teamDeathLog.get(1) : null
                );
            }
            return true;
        }
        return false;
    }

    public void checkAutomaticClosePolicy(){
        if(getPlayers().size() < getGameID().getGameProperties().getMinimumPlayers()){
            if(getPlayers().size() > 1){
                boolean autoPolicyExecuted = false;
                if(checkDeathWinPolicyConditions()) autoPolicyExecuted = true;

                if(!autoPolicyExecuted) declareTie();
            } else {
                endGame(true);
            }
        }
    }

    /** Declares a loss for all players in the game. */
    public void declareLoss(){
        if(gameState == GameState.MAIN_LOOP) {
            endGame(true);

            HashMap<String, String> params = new HashMap<>();
            params.put("colour", PointEntityTypeFirework.Palettes.AQUATIC);
            pointEntityTypeManager.getRegisteredTypes().get(PointEntityTypeFirework.ID)
                    .executeFunctionForAll(PointEntityTypeFirework.FUNC_SPAWN, params);

            if (getGameID().getGameProperties().isInternalRewardsEnabled()) {
                getPlayers().forEach(player -> addBypassRewardChunk(player, new RewardChunk("loser", "" + TextFormat.BLUE + TextFormat.BOLD + "Pity Points :(", 40, 10)));
            }

            String[] paragraphs = new String[]{
                    TextFormat.DARK_AQUA + "Better luck next time!",
                    "",
                    String.format("%s%sEveryone loses the game.", TextFormat.BLUE, TextFormat.BOLD)
            };

            String paragraph = Utility.generateParagraph(paragraphs, TextFormat.GRAY, TextFormat.GRAY, 35);
            for (Player player : getPlayers()) {
                player.sendMessage(paragraph);
                applyRewardChunks(player);
            }

            for (Player player : getTourneyMasters()) player.sendMessage(paragraph);

        }
    }

    /** Declares a tie for a whole team.*/
    public void declareTie(){
        if(gameState == GameState.MAIN_LOOP) {
            endGame(true);

            pointEntityTypeManager.getRegisteredTypes().get(PointEntityTypeFirework.ID)
                    .executeFunctionForAll(PointEntityTypeFirework.FUNC_SPAWN, new HashMap<>());

            if (getGameID().getGameProperties().isInternalRewardsEnabled()) {
                getPlayers().forEach(player -> addBypassRewardChunk(player, new RewardChunk("tie", "" + TextFormat.GOLD + TextFormat.BOLD + "'You Tried and Tied'", 150, 45)));
            }

            String[] paragraphs = new String[]{
                    "" + TextFormat.GOLD + TextFormat.BOLD + "Winners:",
                    "",
                    String.format("%s%sIt was a %s%sTie!", TextFormat.YELLOW, TextFormat.BOLD, TextFormat.GOLD, TextFormat.BOLD)
            };

            String paragraph = Utility.generateParagraph(paragraphs, TextFormat.GRAY, TextFormat.GRAY, 35);

            for (Player player : getPlayers()) {
                player.sendMessage(paragraph);
                applyRewardChunks(player);
            }

            for (Player player : getTourneyMasters()) player.sendMessage(paragraph);
        }
    }

    public void declareVictoryForEveryone(){
        if(gameState == GameState.MAIN_LOOP) {
            endGame(true);

            pointEntityTypeManager.getRegisteredTypes().get(PointEntityTypeFirework.ID)
                    .executeFunctionForAll(PointEntityTypeFirework.FUNC_SPAWN, new HashMap<>());

            if (getGameID().getGameProperties().isInternalRewardsEnabled()) {
                getPlayers().forEach(player -> addBypassRewardChunk(player, new RewardChunk("winner", "" + TextFormat.GOLD + TextFormat.BOLD + "Victory!", 300, 90, TOURNEY_TOP3_POINTS)));
            }

            String[] paragraphs = new String[]{
                    "" + TextFormat.GOLD + TextFormat.BOLD + "Winners:",
                    "",
                    String.format("%s%sEveryone %s%sWins!", TextFormat.YELLOW, TextFormat.BOLD, TextFormat.GOLD, TextFormat.BOLD)
            };

            String paragraph = Utility.generateParagraph(paragraphs, TextFormat.GRAY, TextFormat.GRAY, 35);

            for (Player player : getPlayers()) {
                player.sendMessage(paragraph);
                applyRewardChunks(player);
            }

            for (Player player : getTourneyMasters()) player.sendMessage(paragraph);
        }
    }

    /** Declares victory for a whole team.*/
    public void declareVictoryForTeam(Team team){ declareVictoryInTeamOrder(team, null, null); }
    /** Declares victory players. */
    public void declareVictoryInTeamOrder(Team first, Team second){ declareVictoryInTeamOrder(first, second, null); }
    /** Declares victory players. */
    public void declareVictoryInTeamOrder(Team first, Team second, Team third){
        if(first == null){
            declareLoss();
            return;
        }
        if(gameState == GameState.MAIN_LOOP) {

            pointEntityTypeManager.getRegisteredTypes().get(PointEntityTypeFirework.ID)
                    .executeFunctionForAll(PointEntityTypeFirework.FUNC_SPAWN, new HashMap<>());
            //TODO: Give teams their own firework palettes and use them for #1 instead.

            if (getGameID().getGameProperties().isInternalRewardsEnabled()) {
                first.getPlayers().forEach(player -> addBypassRewardChunk(player, new RewardChunk("first", "" + TextFormat.GOLD + TextFormat.BOLD + "First Place", 400, 100, TOURNEY_TOP3_POINTS + TOURNEY_WIN_POINTS)));
                if (second != null) second.getPlayers().forEach(player -> addBypassRewardChunk(player, new RewardChunk("second", "" + TextFormat.GOLD + TextFormat.BOLD + "Second Place", 300, 75, TOURNEY_TOP3_POINTS)));
                if (third != null) third.getPlayers().forEach(player -> addBypassRewardChunk(player, new RewardChunk("third", "" + TextFormat.DARK_RED + TextFormat.BOLD + "Third Place", 200, 50, TOURNEY_TOP3_POINTS)));
            }
            endGame(true); // THIS MOVES PLAYERS OUT OF THEIR TEAMS !!!

            String[] paragraphs = new String[3 + (second == null ? 0 : 1) + (third == null ? 0 : 1)];
            paragraphs[0] = "" + TextFormat.GOLD + TextFormat.BOLD + "Winners:";
            paragraphs[1] = "";
            paragraphs[2] = String.format("%s%s1st Place: %s%s%s", TextFormat.GOLD, TextFormat.BOLD, TextFormat.RESET, TextFormat.YELLOW, first.getFormattedDisplayName());

            if (second != null) paragraphs[3] = String.format("%s%s2nd Place: %s%s%s", TextFormat.GRAY, TextFormat.BOLD, TextFormat.RESET, TextFormat.DARK_GRAY, second.getFormattedDisplayName());
            if (third != null) paragraphs[4] = String.format("%s%s3rd Place: %s%s%s", TextFormat.DARK_RED, TextFormat.BOLD, TextFormat.RESET, TextFormat.RED, third.getFormattedDisplayName());

            String paragraph = Utility.generateParagraph(paragraphs, TextFormat.GRAY, TextFormat.GRAY, 35);

            for (Player player : getPlayers()) {
                player.sendMessage(paragraph);
                applyRewardChunks(player);
            }

            for (Player player : getTourneyMasters()) player.sendMessage(paragraph);
        }
    }

    /** Declares victory for players. */
    public void declareVictoryForPlayer(Player first){ declareVictoryInPlayerOrder(first, null, null); }
    /** Declares victory players. */
    public void declareVictoryInPlayerOrder(Player first, Player second){ declareVictoryInPlayerOrder(first, second, null); }
    /** Declares victory players. */
    public void declareVictoryInPlayerOrder(Player first, Player second, Player third){
        if(first == null){
            declareLoss();
            return;
        }
        if(gameState == GameState.MAIN_LOOP) {

            pointEntityTypeManager.getRegisteredTypes().get(PointEntityTypeFirework.ID)
                    .executeFunctionForAll(PointEntityTypeFirework.FUNC_SPAWN, new HashMap<>());

            endGame(true);

            if (getGameID().getGameProperties().isInternalRewardsEnabled()) {
                addBypassRewardChunk(first, new RewardChunk("first", "" + TextFormat.GOLD + TextFormat.BOLD + "First Place", 400, 100, TOURNEY_TOP3_POINTS + TOURNEY_WIN_POINTS));
                if (second != null) addBypassRewardChunk(second, new RewardChunk("second", "" + TextFormat.GOLD + TextFormat.BOLD + "Second Place", 300, 75, TOURNEY_TOP3_POINTS));
                if (third != null) addBypassRewardChunk(third, new RewardChunk("third", "" + TextFormat.DARK_RED + TextFormat.BOLD + "Third Place", 200, 50, TOURNEY_TOP3_POINTS));
            }

            String[] paragraphs = new String[3 + (second == null ? 0 : 1) + (third == null ? 0 : 1)];
            paragraphs[0] = "" + TextFormat.GOLD + TextFormat.BOLD + "Winners:";
            paragraphs[1] = "";
            paragraphs[2] = String.format("%s%s1st Place: %s%s%s", TextFormat.GOLD, TextFormat.BOLD, TextFormat.RESET, TextFormat.YELLOW, first.getName());

            if (second != null) paragraphs[3] = String.format("%s%s2nd Place: %s%s%s", TextFormat.GRAY, TextFormat.BOLD, TextFormat.RESET, TextFormat.DARK_GRAY, second.getName());
            if (third != null) paragraphs[4] = String.format("%s%s3rd Place: %s%s%s", TextFormat.DARK_RED, TextFormat.BOLD, TextFormat.RESET, TextFormat.RED, third.getName());

            String paragraph = Utility.generateParagraph(paragraphs, TextFormat.GRAY, TextFormat.GRAY, 35);

            for (Player player : getPlayers()) {
                player.sendMessage(paragraph);
                applyRewardChunks(player);
            }

            for (Player player : getTourneyMasters()) player.sendMessage(paragraph);
        }
    }

    private void applyRewardChunks(Player player){
        HashMap<String, RewardChunk> chunks = rewardChunks.get(player);
        if(chunks != null){
            String[] paras = new String[4+chunks.size()];
            paras[0] = TextFormat.BOLD + "Rewards!";
            paras[1] = "";
            int index = 2;
            int totalCoins = 0;
            int totalExperience = 0;
            int totalTourney = 0;
            for(RewardChunk chunk: chunks.values()){
                paras[index] = " "+chunk.getMessage(TextFormat.DARK_GREEN, TextFormat.GREEN, gameID.getGameProperties().isTourneyGamemode());
                totalCoins += chunk.getCoins();
                totalExperience += chunk.getExperience();
                totalTourney += gameID.getGameProperties().isTourneyGamemode() ? chunk.getTourneyPoints() : 0;
                index++;
            }
            final RewardChunk finalRewards = new RewardChunk("total-rewards", "Total Rewards", totalExperience, totalCoins, totalTourney);
            Optional<PlayerRewardsProfile> rewardsRecord = RewardsManager.get().getRewards(player);
            final int xpLeftIndex = index + 1;
            paras[index] = "";
            paras[xpLeftIndex] = "";
            if (rewardsRecord.isPresent()) {
                final int oldLevel = rewardsRecord.get().getLevel();
                if (oldLevel < 100) {
                    NewGamesAPI1.get().getServer().getScheduler().scheduleTask(NewGamesAPI1.get(), () -> {
                        try {
                            rewardsRecord.get().addRewards(finalRewards);
                        } catch (SQLException exception) {
                            exception.printStackTrace();
                            NewGamesAPI1.get().getServer().getScheduler().scheduleTask(NewGamesAPI1.get(), () ->
                                    player.sendMessage(
                                            Utility.generateUnlimitedParagraph(paras, TextFormat.DARK_GREEN, TextFormat.GREEN, 35)
                                    )
                            );
                            return;
                        }
                        if (oldLevel != rewardsRecord.get().getLevel()) {
                            paras[xpLeftIndex] = " " + String.format("%s%sLEVEL UP!%s %s%s%s more experience required until you reach level %s!", TextFormat.BOLD, TextFormat.AQUA, TextFormat.RESET, TextFormat.YELLOW, rewardsRecord.get().getXPRequiredToLevelUp(), TextFormat.DARK_GREEN, rewardsRecord.get().getLevel() + 1);
                        } else {
                            paras[xpLeftIndex] = " " + String.format("%s%s%s more experience required until you reach level %s!", TextFormat.YELLOW, rewardsRecord.get().getXPRequiredToLevelUp(), TextFormat.DARK_GREEN, rewardsRecord.get().getLevel() + 1);
                        }
                        NewGamesAPI1.get().getServer().getScheduler().scheduleTask(NewGamesAPI1.get(), () ->
                                player.sendMessage(
                                        Utility.generateUnlimitedParagraph(paras, TextFormat.DARK_GREEN, TextFormat.GREEN, 35)
                                )
                        );
                    }, true);
                } else {
                    player.sendMessage(Utility.generateUnlimitedParagraph(paras, TextFormat.DARK_GREEN, TextFormat.GREEN, 35));
                }
            } else {
                NewGamesAPI1.get().getServer().getScheduler().scheduleTask(NewGamesAPI1.get(), () -> {
                    try {
                        PlayerRewardsProfile profile = RewardsManager.get().fetchRewards(player);
                        final int oldLevel = profile.getLevel();
                        if (oldLevel < 100) {
                            profile.addRewards(finalRewards);
                            if (oldLevel != profile.getLevel()) {
                                paras[xpLeftIndex] = " " + String.format("%s%sLEVEL UP!%s %s%s%s more experience required until you reach level %s!", TextFormat.BOLD, TextFormat.AQUA, TextFormat.RESET, TextFormat.YELLOW, rewardsRecord.get().getXPRequiredToLevelUp(), TextFormat.DARK_GREEN, rewardsRecord.get().getLevel() + 1);
                            } else {
                                paras[xpLeftIndex] = " " + String.format("%s%s%s more experience required until you reach level %s!", TextFormat.YELLOW, rewardsRecord.get().getXPRequiredToLevelUp(), TextFormat.DARK_GREEN, rewardsRecord.get().getLevel() + 1);
                            }
                            NewGamesAPI1.get().getServer().getScheduler().scheduleTask(NewGamesAPI1.get(), () ->
                                    player.sendMessage(
                                            Utility.generateUnlimitedParagraph(paras, TextFormat.DARK_GREEN, TextFormat.GREEN, 35)
                                    )
                            );
                        } else {
                            NewGamesAPI1.get().getServer().getScheduler().scheduleTask(NewGamesAPI1.get(), () ->
                                    player.sendMessage(
                                            Utility.generateUnlimitedParagraph(paras, TextFormat.DARK_GREEN, TextFormat.GREEN, 35)
                                    )
                            );
                        }
                    } catch (SQLException exception) {
                        exception.printStackTrace();
                        NewGamesAPI1.get().getServer().getScheduler().scheduleTask(NewGamesAPI1.get(), () ->
                                player.sendMessage(
                                        Utility.generateUnlimitedParagraph(paras, TextFormat.DARK_GREEN, TextFormat.GREEN, 35)
                                )
                        );
                    }
                }, true);
            }
        }
    }

    public GameManager getGameManager() { return gameManager; }

    public String getServerID() { return serverID; }
    public GameID getGameID() { return gameID; }
    public GameBehavior getGameBehaviors() { return gameBehaviors; }
    public GameState getGameState() { return gameState; }

    public MapID getPrimaryMapID() { return mapID; }
    public Level getPrimaryMap() { return primaryMap; }
    public HashMap<String, Level> getAdditionalLevels() { return new HashMap<>(additionalLevels); }

    public HashMap<String, Team> getTeams() { return teams; }
    public HashSet<Player> getPlayers() { return players; }
    public HashMap<Player, Kit> getAppliedSessionKits() { return kits; }
    public HashMap<Player, ArrayList<DummyBossBar>> getBossbars() { return bossbars; }

    public GameScheduler getGameScheduler() { return gameScheduler; }
    public SpawnManager getSpawnManager() { return spawnManager; }
    public DeathManager getDeathManager() { return deathManager; }
    public FunctionalRegionManager getFunctionalRegionManager() { return functionalRegionManager; }
    public PointEntityTypeManager getPointEntityTypeManager() { return pointEntityTypeManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public CustomPVPManager getCustomPVPManager() { return customPVPManager; }

    public boolean isTourneyStarted() { return tourneyStarted; }
    public HashSet<Player> getTourneyMasters() { return tourneyMasters; }

    public ArrayList<TaskHandler> getTaskHandlers() { return taskHandlers; }

    /**
     * You should not use this unless the handler provided the token to the component
     * in question!
     * @param handlerPrivateToken - Private token held by the handler.
     * @param gameState the current gamestate of the handler
     * @return true if the token was accepted.
     */
    public boolean setGameState(String handlerPrivateToken, GameState gameState) {
        if(!handlerPrivateToken.equals(token)){ return false; }
        this.gameState = gameState;
        return true;
    }
    /**
     * You should not use this unless the handler provided the token to the component
     * in question!
     * @param handlerPrivateToken - Private token held by the handler.
     * @param teams the current teams of the handler
     * @return true if the token was accepted.
     */
    public boolean setTeams(String handlerPrivateToken, HashMap<String, Team> teams) {
        if(!handlerPrivateToken.equals(token)){ return false; }
        this.teams = teams;
        return true;
    }

    public void setTourneyStarted(boolean tourneyStarted) {
        this.tourneyStarted = tourneyStarted;
    }

    public boolean endGame(boolean forceStop) { return endGame(forceStop, false); }
    public boolean endGame(boolean forceStop, boolean cleanUpImmediatley){

        if(!forceStop) {
            if (gameState != GameState.END){
                return false;
            }
        } else {
            if(gameState == GameState.PRE_PREPARE){
                return false;
            }
            gameState = GameState.END;
        }

        for(TaskHandler taskHandler: new ArrayList<>(getGameScheduler().getTaskHandlers())){
            taskHandler.cancel();
            getGameScheduler().getTaskHandlers().remove(taskHandler);
        }
        getGameBehaviors().cleanUp();
        Team deadTeam = teams.get(TeamPresets.DEAD_TEAM_ID);
        Team spectatorTeam = teams.get(TeamPresets.DEAD_TEAM_ID);

        for(Player player: players){
            if (getGameID().getGameProperties().isInternalRewardsEnabled()) {
                addBypassRewardChunk(player, new RewardChunk("participation", "Participation", 60, 4));
            }
            KitRegistry.get().getKitGroup("core").ifPresent(k -> {
                equipPlayerKit(player, k.getGroupKits().getOrDefault("spectate", k.getDefaultKit()), true);
            });

            if(((!deadTeam.getPlayers().contains(player)) && (!spectatorTeam.getPlayers().contains(player)))){
                switchPlayerToTeam(player, deadTeam, true);
            }

            if(bossbars.containsKey(player)) {
                for (DummyBossBar bossBar : bossbars.get(player)) {
                    bossBar.destroy();
                }
                bossbars.remove(player);
            }
        }

        GameManager.get().removeSession(this.serverID);

        if (cleanUpImmediatley) {
            this.cleanUpGame();
        } else {
            NewGamesAPI1.get().getServer().getScheduler().scheduleDelayedTask(NewGamesAPI1.get(), this::cleanUpGame, cleanupWaitTime);
        }
        return true;
    }

    public boolean cleanUpGame(){
        if(gameState != GameState.END){ return false; }

        for(Player player: new ArrayList<>(getPlayers())) removePlayerFromGame(player);
        for(Player player: new ArrayList<>(getTourneyMasters())) removeTourneymasterFromGame(player, false);


        for(Team team: teams.values()){
            team.destroy();
        }
        for(Level level: new ArrayList<>(getAdditionalLevels().values())){
            try {
                additionalLevels.values().remove(level);
                String levelName = level.getFolderName();
                level.unload(true);
                FileUtils.deleteDirectory(new File(MapManager.get().API_WORLDS_PATH+levelName));
            } catch (Exception err){
                err.printStackTrace();
            }
        }
        try {
            getPrimaryMap().unload(true);
            FileUtils.deleteDirectory(new File(MapManager.get().API_WORLDS_PATH+MapManager.parseLevelNameStringFromSeries(serverID, "map")));
        } catch (Exception err){
            err.printStackTrace();
            NewGamesAPI1.getPlgLogger().error("Unable to delete world. This has potential to cause issues.");
        }
        for(TaskHandler taskHandler: new ArrayList<>(getTaskHandlers())){
            taskHandler.cancel();
            getTaskHandlers().remove(taskHandler);
        }
        HandlerList.unregisterAll(this);
        HandlerList.unregisterAll(getGameBehaviors());
        HandlerList.unregisterAll(deathManager);
        HandlerList.unregisterAll(spawnManager);
        HandlerList.unregisterAll(customPVPManager);
        return true;
    }

    public enum GameState {
        PRE_PREPARE,
        PREPARING, //No countdown. State should be left as soon as possible.
        PRE_COUNTDOWN, // Game has loaded but hasn't entered an official countdown.
        COUNTDOWN,
        PRE_MAIN_LOOP,
        MAIN_LOOP,
        END
    }

    public enum AutomaticWinPolicy {
        OPPOSING_TEAMS_DEAD,
        OPPOSING_PLAYERS_DEAD,
        MANUAL_CALLS_ONLY // Limits win conditions to manual method calls
    }
}
