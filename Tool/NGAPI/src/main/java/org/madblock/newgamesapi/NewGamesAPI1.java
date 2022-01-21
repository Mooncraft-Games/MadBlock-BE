package org.madblock.newgamesapi;

import cn.nukkit.AdventureSettings;
import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.entity.EntityPortalEnterEvent;
import cn.nukkit.event.entity.EntitySpawnEvent;
import cn.nukkit.event.player.*;
import cn.nukkit.network.protocol.ProtocolInfo;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.PluginLogger;
import cn.nukkit.utils.TextFormat;
import dev.cg360.mc.nukkittables.LootTableRegistry;
import org.madblock.lib.commons.data.store.settings.ControlledSettings;
import org.madblock.lib.commons.data.store.settings.Settings;
import org.madblock.newgamesapi.cache.VisitorSkinCache;
import org.madblock.newgamesapi.commands.*;
import org.madblock.newgamesapi.game.*;
import org.madblock.newgamesapi.game.internal.GameBehaviorDevmode;
import org.madblock.newgamesapi.game.internal.hub.GameBehaviorLobby;
import org.madblock.newgamesapi.game.internal.hub.GameBehaviorTourneyLobby;
import org.madblock.newgamesapi.kits.KitGroup;
import org.madblock.newgamesapi.kits.PlayerKitsManager;
import org.madblock.newgamesapi.kits.builtingroup.KitEmpty;
import org.madblock.newgamesapi.kits.builtingroup.KitFinalSpectator;
import org.madblock.newgamesapi.kits.hub.KitHub;
import org.madblock.newgamesapi.kits.hub.KitHubBuilder;
import org.madblock.newgamesapi.map.MapManager;
import org.madblock.newgamesapi.nukkit.block.BlockLeaves;
import org.madblock.newgamesapi.nukkit.block.BlockLeaves2;
import org.madblock.newgamesapi.nukkit.entity.EntityHumanPlus;
import org.madblock.newgamesapi.nukkit.packet.AnimateEntityPacket;
import org.madblock.newgamesapi.registry.GameRegistry;
import org.madblock.newgamesapi.registry.KitRegistry;
import org.madblock.newgamesapi.registry.LibraryRegistry;
import org.madblock.newgamesapi.rewards.PlayerRewardsProfile;
import org.madblock.newgamesapi.rewards.RewardsManager;
import org.madblock.newgamesapi.util.Utility;
import org.madblock.ranks.api.RankManager;
import org.madblock.ranks.api.RankProfile;
import org.madblock.ranks.enums.PrimaryRankID;

import java.io.File;
import java.sql.SQLException;
import java.util.Optional;

public class NewGamesAPI1 extends PluginBase implements Listener {

    private static NewGamesAPI1 newGamesAPI1;

    private KitRegistry kitRegistry;
    private GameRegistry gameRegistry;
    private LibraryRegistry libraryRegistry;

    private MapManager mapManager;
    private GameManager gameManager;
    private HubManager hubManager;
    private PlayerKitsManager playerKitsManager;
    private DimensionWatchdog dimensionWatchdog;

    private RewardsManager rewardsManager;
    private NavigationManager navigationManager;
    private QuiccccQueueManager queueManager;

    private VisitorSkinCache visitorSkinCache;

    private Settings configuration;

    @Override
    public void onEnable(){
        newGamesAPI1 = this;

        this.kitRegistry = new KitRegistry();
        this.gameRegistry = new GameRegistry();
        this.libraryRegistry = new LibraryRegistry();

        this.mapManager = new MapManager();
        this.gameManager = new GameManager();
        this.hubManager = new HubManager(gameManager);
        this.playerKitsManager = new PlayerKitsManager();
        this.dimensionWatchdog = new DimensionWatchdog();

        this.rewardsManager = new RewardsManager();
        this.navigationManager = new NavigationManager();
        this.queueManager = new QuiccccQueueManager();

        this.visitorSkinCache = new VisitorSkinCache();

        this.configuration = new ControlledSettings();


        this.kitRegistry.setAsPrimaryRegistry();
        this.gameRegistry.setAsPrimaryRegistry();
        this.libraryRegistry.setAsPrimaryRegistry();

        this.mapManager.setAsPrimaryManager();
        this.gameManager.setAsPrimaryManager();
        this.hubManager.setAsPrimaryManager();
        this.playerKitsManager.setAsPrimaryManager();
        this.dimensionWatchdog.setAsPrimary();
 
        this.rewardsManager.setAsPrimaryManager();

        this.navigationManager.setAsPrimaryManager();
        this.queueManager.setAsPrimaryManager();

        this.visitorSkinCache.setAsPrimaryManager();

        this.getServer().getCommandMap().register("ngapi", new CommandGame());
        this.getServer().getCommandMap().register("ngapi", new CommandKit());
        this.getServer().getCommandMap().register("ngapi", new CommandServer());
        this.getServer().getCommandMap().register("ngapi", new CommandHub());
        this.getServer().getCommandMap().register("ngapi", new CommandMatchmake());
        this.getServer().getCommandMap().register("ngapi", new CommandAddStat());
        this.getServer().getCommandMap().register("ngapi", new CommandTourney());
        this.getServer().getCommandMap().register("ngapi", new CommandSuper());
        this.getServer().getCommandMap().register("ngapi", new CommandFirework());
        this.getServer().getCommandMap().register("ngapi", new CommandLeaveQueue());


        KitGroup emptyKitGroup = new KitGroup("empty", "Empty", false, new KitEmpty());
        KitGroup coreKitGroup = new KitGroup("core", "Core", false, new KitEmpty(), new KitFinalSpectator());
        KitGroup hubKitGroup = new KitGroup(HubManager.HUB_KIT_ID, "Hub", false, new KitHub(), new KitHubBuilder());

        KitRegistry.get().registerKitGroup(emptyKitGroup);
        KitRegistry.get().registerKitGroup(coreKitGroup);
        KitRegistry.get().registerKitGroup(hubKitGroup);


        GameProperties propertiesDevTest1 = new GameProperties(GameHandler.AutomaticWinPolicy.MANUAL_CALLS_ONLY)
                .setMinimumPlayers(1)
                .setGuidelinePlayers(4)
                .setMaximumPlayers(16)
                .setTourneyGamemode(true);

        GameProperties defaultTourneyProperties = new GameProperties(GameHandler.AutomaticWinPolicy.MANUAL_CALLS_ONLY)
                .setMinimumPlayers(0)
                .setGuidelinePlayers(1)
                .setMaximumPlayers(HubManager.TOURNEY_MAX_PLAYERS)
                .setDefaultCountdownLength(-1)
                .setFallDamageEnabled(false)
                .setFallDamageEnabledPreGame(false)
                .setItemDroppingEnabled(false)
                .setItemDroppingEnabledPreGame(false)
                .setCanPlayersMoveDuringCountdown(true)
                .setCanWorldBeManipulated(true)
                .setHungerEnabled(false)
                .setNatualRegenerationEnabled(true)
                //.setRequiredPermissions(new String[]{ "newgameapi.tourney" })
                ;

        GameProperties default_secret_properties = new GameProperties(GameHandler.AutomaticWinPolicy.MANUAL_CALLS_ONLY)
                .setMinimumPlayers(0)
                .setGuidelinePlayers(1)
                .setMaximumPlayers(HubManager.HUB_MAX_PLAYERS)
                .setDefaultCountdownLength(-1)
                .setFallDamageEnabled(false)
                .setFallDamageEnabledPreGame(false)
                .setItemDroppingEnabled(false)
                .setItemDroppingEnabledPreGame(false)
                .setCanPlayersMoveDuringCountdown(true)
                .setCanWorldBeManipulated(true)
                .setHungerEnabled(false)
                .setNatualRegenerationEnabled(true)
                .setRequiredPermissions(new String[]{ "newgameapi.secrets" })
                ;

        NewGamesAPI1.getGameRegistry()
                .registerGame(new GameID("devtest1", "dev", "Developer Testing #1", "Test Test Test", KitRegistry.DEFAULT.getGroupID(), new String[]{"devtest"}, 1, propertiesDevTest1, GameBehaviorDevmode.class))
        ;

        NewGamesAPI1.getHubManager()
                .registerHubGame(HubManager.HUB_GAME_ID, HubManager.HUB_NAME, "hub", new String[]{"hub", "lobby"} );
                //Hardcode tourney hub as there's no good way to put it in the config.

        // -- Nukkit Overrides --

        this.getServer().getNetwork().registerPacket(ProtocolInfo.ANIMATE_ENTITY_PACKET, AnimateEntityPacket.class);

        Entity.registerEntity("human_plus", EntityHumanPlus.class);

        Block.list[Block.LEAVES] = BlockLeaves.class;
        Block.list[Block.LEAVES2] = BlockLeaves2.class;
        Block.fullList[Block.LEAVES] = new BlockLeaves();
        Block.fullList[Block.LEAVES2] = new BlockLeaves();


        if(this.loadConfiguration()) {

            if(NewGamesAPI1.getProperties().getOrDefault(ServerConfigProcessor.IS_TOURNEY_ENABLED))
                NewGamesAPI1.getHubManager()
                        .registerHubGame(new GameID("tourney_hub", "tourney",
                                Utility.ResourcePackCharacters.TROPHY+" Tourney Hub",
                                "Welcome olympians, astronauts, and athletes! Or something like that, I don't know.",
                                HubManager.HUB_KIT_ID, new String[]{"tourney_hub"}, 1,
                                defaultTourneyProperties, GameBehaviorTourneyLobby.class));

            NewGamesAPI1.getHubManager()
                    .registerHubGame(new GameID("secret_hub", "shhh",
                            Utility.ResourcePackCharacters.TACO + " Secret Hub",
                            "Shush, it's a secret",
                            HubManager.HUB_KIT_ID, new String[]{"secret_hub"}, 1,
                            default_secret_properties, GameBehaviorLobby.class));


            this.getServer().getPluginManager().registerEvents(this, this);
            this.getServer().getPluginManager().registerEvents(dimensionWatchdog, this);
        } else {
            newGamesAPI1 = null;
        }

    }

    public boolean loadConfiguration(){
        getLogger().info("== /!\\ == LOADING CONFIGURATIONS + ASSETS == /!\\ ==");

        try {
            File cfgDirectory = new File(NewGamesAPI1.get().getServer().getDataPath()+"./plugins/NGAPI/");
            this.configuration = ServerConfigProcessor.loadServerConfiguration(cfgDirectory, "config.json", true);

            this.mapManager.beginServerStartChecks();
            this.mapManager.loadMapDatabase();
            this.hubManager.loadHubTypesConfiguration();
            this.libraryRegistry.loadAllBooks();
            this.visitorSkinCache.createCache();

            NewGamesAPI1.getPlgLogger().info("== Loading loottables. ==");
            LootTableRegistry.get().loadAllLootTablesFromStorage("", true);
            NewGamesAPI1.getPlgLogger().info("== Completed loottable load.  ==");

            getLogger().info("== /!\\ == PASS: CONFIGURED NGAPI == /!\\ ==");
            return true;

        } catch (Exception err) {
            err.printStackTrace();

            getLogger().info("== /!\\ == CATASTROPHIC FAIL == /!\\ ==");
            getLogger().info("As NGAPI failed the configuration checks, it will not load.");
            return false;
        }
    }

    @Override
    public void onDisable() {
        visitorSkinCache.save();
        NewGamesAPI1.getPlayerKitsManager().clearDatabaseQueue();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerRespawnEvent event){
        if(event.isFirstSpawn()) {
            event.getPlayer().setCheckMovement(false);
            runMicroNodeHubSequence(event.getPlayer());
            event.setRespawnPosition(event.getPlayer().getPosition());
            if (!RewardsManager.get().getRewards(event.getPlayer()).isPresent()) {
                try {
                    RewardsManager.get().fetchRewards(event.getPlayer());
                } catch (SQLException exception) {
                    exception.printStackTrace();
                }
            }
        }
    }


    @EventHandler(priority = EventPriority.LOW)
    public void onJoinMessage(PlayerJoinEvent event) {
        event.setJoinMessage("");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onLeaveMessage(PlayerQuitEvent event) {
        event.setQuitMessage("");
    }

    @EventHandler
    public void onDebugCrouch(PlayerToggleSneakEvent event) {
        if(event.isSneaking()) {
            // Just dump anything here to test stuff.
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onTeleportToDefaultLevel(PlayerTeleportEvent event){
        if(event.getTo().getLevel() == getServer().getDefaultLevel()) {
            event.setCancelled(true);
            runMicroNodeHubSequence(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEnterLobbyPortal(EntityPortalEnterEvent event){

        if(event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            GameHandler h = getGameManager().getPlayerLookup().get(player.getUniqueId());

            if(h.getGameBehaviors() instanceof GameBehaviorLobby) {
                event.setCancelled();
                NavigationManager.get().openQuickLobbyMenu(player);
            }
        }
    }

    private void runMicroNodeHubSequence(Player player){
        Optional<String> type = HubManager.get().getLastPlayerHub(player);
        if(type.isPresent()) {
            Optional<GameHandler> hub = hubManager.getAvailableHub(type.get());
            if (hub.isPresent()) {
                if(hub.get().addPlayerToGame(player)) {
                    return; // end method here if player successfully sent
                }
            }
        }

        Optional<GameHandler> hub = hubManager.getAvailableHub(HubManager.HUB_GAME_ID);
        if (hub.isPresent()) {
            if(hub.get().addPlayerToGame(player)) {
                return; // end method here if player successfully sent
            }
        }

        player.kick("" + TextFormat.BLUE + TextFormat.BOLD + "Sorry! - " + TextFormat.RESET + TextFormat.DARK_AQUA + "Something is broken with this node. Please try again later. (HUB PANIC)");
        getLogger().critical("Missing Hub for micro-node!");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlaceBlock(BlockPlaceEvent event){
        if(!event.getPlayer().getAdventureSettings().get(AdventureSettings.Type.BUILD_AND_MINE)){
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreakBlock(BlockBreakEvent event){
        if(!event.getPlayer().getAdventureSettings().get(AdventureSettings.Type.BUILD_AND_MINE)){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void entityCreateEvent(EntitySpawnEvent event){
        if(event.getEntity() instanceof EntityHuman){
            EntityHuman human = (EntityHuman) event.getEntity();
            human.getSkin().setTrusted(true);
        }
    }

    @EventHandler
    public void onChat(PlayerChatEvent event) {
        RankManager manager = RankManager.getInstance();
        Optional<PlayerRewardsProfile> playerRewardsRecord = RewardsManager.get().getRewards(event.getPlayer());
        Optional<RankProfile> rankProfile = manager.getRankProfile(event.getPlayer());

        int level = 0;
        if (playerRewardsRecord.isPresent()) {
            level = playerRewardsRecord.get().getLevel();
        }
        String levelText = Utility.getLevelText(level);

        if (rankProfile.isPresent()) {
            PrimaryRankID rank = rankProfile.get().getPrimaryDisplayedRank();
            if (!rank.getName().isPresent()) {
                event.setFormat(String.format("%s %s%s %s%s", levelText, TextFormat.GRAY, event.getPlayer().getDisplayName(), TextFormat.WHITE, event.getMessage()));
            } else {
                event.setFormat(String.format("%s %s%s%s %s%s%s %s%s", levelText, rank.getColor().orElse(TextFormat.WHITE), TextFormat.BOLD, rank.getName().get(), TextFormat.RESET, TextFormat.GRAY, event.getPlayer().getDisplayName(), TextFormat.WHITE, event.getMessage()));
            }
        } else {
            event.setFormat(String.format("*%s %s%s %s%s", levelText, TextFormat.GRAY, event.getPlayer().getDisplayName(), TextFormat.WHITE, event.getMessage()));
        }
    }

    public PluginLogger getLogger(){ return super.getLogger(); }

    public static GameRegistry getGameRegistry() { return GameRegistry.get(); }
    public static KitRegistry getKitRegistry() { return KitRegistry.get(); }
    public LibraryRegistry getLibraryRegistry() { return libraryRegistry; }
    public static LootTableRegistry getLootTableRegistry() { return LootTableRegistry.get(); }

    public static GameManager getGameManager() { return GameManager.get(); }
    public static HubManager getHubManager() { return HubManager.get(); }
    public static MapManager getMapManager() { return MapManager.get(); }
    public static PlayerKitsManager getPlayerKitsManager() { return PlayerKitsManager.get(); }

    public static NewGamesAPI1 get(){ return newGamesAPI1; }
    public static PluginLogger getPlgLogger(){ return newGamesAPI1.getLogger(); }
    public static Settings getProperties(){ return newGamesAPI1.configuration; }

}
