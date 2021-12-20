package org.madblock.newgamesapi.game.deaths;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByBlockEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.level.Sound;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.util.Utility;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.game.events.GamePlayerDeathEvent;
import org.madblock.newgamesapi.kits.Kit;
import org.madblock.newgamesapi.kits.KitGroup;
import org.madblock.newgamesapi.kits.PlayerKitsManager;
import org.madblock.newgamesapi.registry.KitRegistry;
import org.madblock.newgamesapi.rewards.RewardChunk;
import org.madblock.newgamesapi.team.DeadTeam;
import org.madblock.newgamesapi.team.SpectatingTeam;
import org.madblock.newgamesapi.team.Team;
import org.madblock.newgamesapi.team.TeamPresets;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class DeathManager implements Listener {

    static {
        HashMap<String, String[]> gdm = new HashMap<>();
        gdm.put("*", new String[]{
                "{player} straight up died.",
                "{player} got demonetized.",
                "{player} did an oopsie.",
                "{player} might've slipped or might've just died. We do not know.",
                "{player} forgot to wear a mask.",
                "{player} signed up for a rocket boots test too early.",
                "{player} was an early adopter. They hadn't quite fixed all the bugs.",
                "{player} went kapoot.",
                "{player} panicked professionally. Subsequently, they died.",
                "{player} was hit by an out-of-date meteor.",
                "{player} just did a thing. That thing was die.",
                "{player} ejected the game during autosave.",
                "{player} ignored the warnings of sadistic geese in the area."
        });
        gdm.put("killer_player", new String[]{
                "{player} died to {killer}'s fatal attack.",
                "{player} couldn't handle {killer}'s ever-growing kindness and killing spree.",
                "{player} forgot to dodge {killer}.",
                "{killer} goes pew pew. {player} goes bleh bleh.",
                "{killer} yelled 'duck!' - Meanwhile, {player} got hit by {killer}'s goose.",
                "The time was noon. {player} didn't draw in time. {killer} won the duel!",
                "{killer} was too stealthy for {player}'s liking. Too bad {player} died.",
                "'Dis {player} empty. YEET!' went {killer}."
        });
        gdm.put("killer_entity", new String[]{
                "{player} managed to die to a {killer}."
        });
        gdm.put("killer_block", new String[]{
                "{player} didn't read chapter 3 on how to breathe within a block.",
                "{player} drowned within a solid. Fascinating!",
                "{player} now knows what the inside of a {killer} looks like."
        });
        gdm.put("fire", new String[]{
                "{player} burned to a crisp.",
                "{player} didn't put the flames out in time.",
                "{player} left the oven on too long.",
                "{player} just couldn't stop playing with fire.",
                "{player} said 'fire go hiss :))' and promptly burned.",
                "{player} studied combustion a bit too close."
        });
        GLOBAL_DEATH_MESSAGES = gdm;
    }

    public static final HashMap<String, String[]> GLOBAL_DEATH_MESSAGES;
    public static final TextFormat DEATH_MESSAGE_COLOUR = TextFormat.GRAY;

    public static final int PLAYER_KILL_XP = 20;
    public static final int PLAYER_KILL_COINS = 6;
    public static final int PLAYER_KILL_TOURNEY_POINTS = 2;

    protected GameHandler gameHandler;

    protected ArrayList<Player> playerDeathOrder;
    protected ArrayList<Team> teamDeathOrder;
    protected ArrayList<DeathCause> fullDeathLog;

    protected HashSet<Player> pendingRespawns;
    protected HashMap<Player, UUID> damageImmunities;

    public DeathManager(GameHandler gameHandler) {
        this.gameHandler = gameHandler;

        this.playerDeathOrder = new ArrayList<>();
        this.teamDeathOrder = new ArrayList<>();
        this.fullDeathLog = new ArrayList<>();

        this.pendingRespawns = new HashSet<>();
        this.damageImmunities = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGamePlayerDeath(EntityDamageEvent event) {

        // Doesn't belong here but oh well
        // It works without being clunky.
        if(event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;

            if (e.getDamager() instanceof Player) {
                Player p = (Player) e.getDamager();

                if(gameHandler.getTourneyMasters().contains(p)) {
                    event.setCancelled();
                    return;
                }
            }
        }

        if(!event.isCancelled()) {
            if (event.getEntity() instanceof Player) {
                Player victim = (Player) event.getEntity();

                if((damageImmunities.containsKey(victim)) && (event.getEntity().getHealth() < 500)) {
                    event.setCancelled(true);
                    return;
                }

                if (gameHandler.getPlayers().contains(victim)) {

                    if((event.getEntity().getHealth() - event.getFinalDamage()) < 1) {
                        event.setCancelled(true);
                        Optional<Team> victimTeam = gameHandler.getPlayerTeam(victim);

                        DeathCategory category = DeathCategory.MISC;

                        if (event instanceof EntityDamageByEntityEvent) {
                            EntityDamageByEntityEvent e = ((EntityDamageByEntityEvent) event);

                            if (e.getDamager() instanceof Player) {
                                category = DeathCategory.KILLER_PLAYER;

                            } else {
                                category = DeathCategory.KILLER_ENTITY;
                            }
                        } else if (event instanceof EntityDamageByBlockEvent) {
                            category = DeathCategory.KILLER_BLOCK;
                        }

                        DeathCause.DeathCauseBuilder builder = DeathCause.builder(
                                victim,
                                victimTeam.orElse(gameHandler.getTeams().get(TeamPresets.SPECTATOR_TEAM_ID)),
                                category,
                                DeathSubCategory.getCategoryFromDamageCause(event.getCause()),
                                event.getFinalDamage()
                        );

                        builder.setRawEvent(event);
                        switch (category) {
                            case KILLER_PLAYER:
                                Player killer = (Player) ((EntityDamageByEntityEvent) event).getDamager();
                                if (gameHandler.getGameID().getGameProperties().isInternalRewardsEnabled()) {
                                    gameHandler.addRewardChunk(killer, new RewardChunk("kill", "Player Kill", PLAYER_KILL_XP, PLAYER_KILL_COINS, PLAYER_KILL_TOURNEY_POINTS));
                                }
                                builder.setKillerPlayer(killer);
                                break;
                            case KILLER_ENTITY:
                                builder.setKillerEntity(((EntityDamageByEntityEvent) event).getDamager());
                                break;
                            case KILLER_BLOCK:
                                builder.setKillerBlock(((EntityDamageByBlockEvent) event).getDamager());
                                break;
                        }
                        killPlayer(builder.build(), true);
                        return;
                    }
                }

                if (gameHandler.getTourneyMasters().contains(victim)) {
                    event.setCancelled(true);
                    gameHandler.getSpawnManager().placePlayerInSpawnPosition(victim, gameHandler.getTeams().get(TeamPresets.SPECTATOR_TEAM_ID));
                }
            }
        }
    }

    public void appendDeathlogs(DeathCause deathCause){
        playerDeathOrder.remove(deathCause.getVictim());
        teamDeathOrder.remove(deathCause.getVictimTeam());
        playerDeathOrder.add(0, deathCause.getVictim());
        teamDeathOrder.add(0, deathCause.getVictimTeam());
        fullDeathLog.add(0, deathCause);
        gameHandler.checkDeathWinPolicyConditions();
    }

    public void killPlayer(DeathCause deathCause, boolean showDeathMessage){
        Optional<Team> t = gameHandler.getPlayerTeam(deathCause.getVictim());

        if(t.isPresent()){
            Team team = t.get();
            Team dt = gameHandler.getTeams().get(TeamPresets.DEAD_TEAM_ID);

            if(  ( (!(team instanceof SpectatingTeam)) && dt instanceof DeadTeam )  || (gameHandler.getGameState() != GameHandler.GameState.MAIN_LOOP) ) {
                GamePlayerDeathEvent result = sendGameBehaviorEvent(deathCause);

                if (result.getDeathState() != GamePlayerDeathEvent.DeathState.CANCELLED) {
                    appendDeathlogs(deathCause);
                    if (showDeathMessage && result.shouldShowDeathMessage()) sendGameDeathMessage(deathCause);

                    Optional<Kit> prevkit = gameHandler.removePlayerKit(deathCause.getVictim(), true);
                    Kit finalPrevKit = prevkit.orElse(getPlayerKitPreference(deathCause.getVictim()));

                    switch (result.getDeathState()){

                        case INSTANT_RESPAWN:
                            startDamageImmunity(deathCause.getVictim(), result.getRespawnImmunitySeconds());
                            executeRespawnInstantlyKillType(deathCause.getVictim(), team, finalPrevKit);
                            break;

                        case TIMED_RESPAWN:
                            startDamageImmunity(deathCause.getVictim(), result.getRespawnImmunitySeconds());
                            executeRespawnTimedKillType(deathCause.getVictim(), team, finalPrevKit, result.getRespawnSeconds());
                            break;

                        case MOVE_TO_DEAD_SPECTATORS:
                            executePermanentKillType(deathCause.getVictim(), team);
                            break;
                    }
                }

            } else gameHandler.getSpawnManager().placePlayerInSpawnPosition(deathCause.getVictim(), team);

        }
    }

    public void startDamageImmunity(Player victim, int immunity) {
        if(immunity > 0) {
            // In order to not cancel immunity early (should be rare anyway)
            // this only cancels damage if the uuid hasn't been changed from when
            // the immunity started, else a newer immunity period is counting.
            UUID immuneClashUUID = UUID.randomUUID();
            damageImmunities.put(victim, immuneClashUUID);

            gameHandler.getGameScheduler().registerSelfCancellableGameTask(task -> {

                if(damageImmunities.containsKey(victim)) {

                    if(damageImmunities.get(victim) == immuneClashUUID) {
                        damageImmunities.remove(victim);
                    }
                }

            }, 0, 20 * immunity);

        }
    }

    protected void executeRespawnInstantlyKillType (Player victim, Team previousTeam, Kit prevkit){
        gameHandler.getSpawnManager().placePlayerInSpawnPosition(victim, previousTeam);
        gameHandler.equipPlayerKit(victim, prevkit, false);
    }

    protected void executeRespawnTimedKillType (Player victim, Team previousTeam, Kit prevkit, int respawnTime){
        pendingRespawns.add(victim);
        executePermanentKillType(victim, previousTeam);

        AtomicInteger countdown = new AtomicInteger(respawnTime);
        gameHandler.getGameScheduler().registerSelfCancellableGameTask(task -> {
            if(!gameHandler.getPlayers().contains(victim)){
                pendingRespawns.remove(victim);
            }

            if(!pendingRespawns.contains(victim)){
                task.cancel();
            }

            int c = countdown.getAndDecrement();
            if(c <= 0 ) {
                pendingRespawns.remove(victim);
                revivePlayerFromDeadTeam(victim, prevkit);
                task.cancel();
            } else {
                victim.sendTitle(String.format("%s%s Respawning in...", TextFormat.RED, TextFormat.BOLD), String.format("%s%s", TextFormat.DARK_RED, c), 0, 20, 0);
                if(c <= 10) {
                    victim.getLevel().addSound(victim.getPosition(), Sound.MOB_SHEEP_SHEAR, 1f, 0.8f, victim);
                }
            }
        }, 0, 20);
    }

    protected void executePermanentKillType (Player victim, Team previousTeam){
        DeadTeam deadTeam = (DeadTeam) gameHandler.getTeams().get(TeamPresets.DEAD_TEAM_ID);
        gameHandler.switchPlayerToTeam(victim, deadTeam, previousTeam,false);
        gameHandler.getSpawnManager().placePlayerInSpawnPosition(victim, deadTeam);

        KitRegistry.get().getKitGroup("core").ifPresent(k -> {
            gameHandler.equipPlayerKit(victim, k.getGroupKits().getOrDefault("spectate", k.getDefaultKit()), false);
        });
    }

    protected GamePlayerDeathEvent sendGameBehaviorEvent(DeathCause deathCause){
        switch (deathCause.getDeathCategory()){
            case KILLER_PLAYER:
                return sendKillerPlayerBehaviorEvent(deathCause);
            case KILLER_ENTITY:
                return sendKillerEntityBehaviorEvent(deathCause);
            case KILLER_BLOCK:
                return sendKillerBlockBehaviorEvent(deathCause);
            default:
                return sendGenericBehaviorEvent(deathCause);
        }
    }

    protected GamePlayerDeathEvent sendKillerPlayerBehaviorEvent(DeathCause deathCause){
        if(deathCause.getKillerPlayer().isPresent()) {
            GamePlayerDeathEvent deathEvent = new GamePlayerDeathEvent(deathCause);
            gameHandler.getGameBehaviors().onGameDeathByPlayer(deathEvent);
            return deathEvent;
        } else {
            return sendGenericBehaviorEvent(deathCause);
        }
    }

    protected GamePlayerDeathEvent sendKillerEntityBehaviorEvent(DeathCause deathCause){
        if(deathCause.getKillerEntity().isPresent()) {
            GamePlayerDeathEvent deathEvent = new GamePlayerDeathEvent(deathCause);
            gameHandler.getGameBehaviors().onGameDeathByEntity(deathEvent);
            return deathEvent;
        } else {
            return sendGenericBehaviorEvent(deathCause);
        }
    }

    protected GamePlayerDeathEvent sendKillerBlockBehaviorEvent(DeathCause deathCause){
        if(deathCause.getKillerBlock().isPresent()) {
            GamePlayerDeathEvent deathEvent = new GamePlayerDeathEvent(deathCause);
            gameHandler.getGameBehaviors().onGameDeathByBlock(deathEvent);
            return deathEvent;
        } else {
            return sendGenericBehaviorEvent(deathCause);
        }
    }

    protected GamePlayerDeathEvent sendGenericBehaviorEvent(DeathCause deathCause){
        GamePlayerDeathEvent deathEvent = new GamePlayerDeathEvent(deathCause);
        gameHandler.getGameBehaviors().onGameMiscDeathEvent(deathEvent);
        return deathEvent;
    }

    public void revivePlayerFromDeadTeam(Player player){
        revivePlayerFromDeadTeam(player, null);
    }

    public void revivePlayerFromDeadTeam(Player player, Kit kit){
        Team team = gameHandler.getTeams().get(TeamPresets.DEAD_TEAM_ID);
        if(team instanceof DeadTeam){
            Kit playerKit = kit;
            if(playerKit == null) playerKit = getPlayerKitPreference(player);
            DeadTeam dead = (DeadTeam) team;
            if(dead.getPreviousTeams().containsKey(player)) {
                Team prevTeam = dead.getPreviousTeams().get(player);
                gameHandler.getSpawnManager().placePlayerInSpawnPosition(player, prevTeam);
                dead.revivePlayer(player);
                gameHandler.equipPlayerKit(player, playerKit, true);
            }
        }
    }

    public void sendGameDeathMessage(DeathCause deathCause){
        ArrayList<String> mesagePool = new ArrayList<>(Arrays.asList(GLOBAL_DEATH_MESSAGES.get("*")));
        HashMap<String, String[]> mapIDMessages = gameHandler.getPrimaryMapID().getDeathMessages();

        String deathCategoryID = deathCause.getDeathCategory().name().toLowerCase();
        String deathSubCategoryID = deathCause.getDeathSubCategory().name().toLowerCase();
        if(GLOBAL_DEATH_MESSAGES.containsKey(deathCategoryID)) mesagePool.addAll(Arrays.asList(GLOBAL_DEATH_MESSAGES.get(deathCategoryID)));
        if(GLOBAL_DEATH_MESSAGES.containsKey(deathSubCategoryID)) mesagePool.addAll(Arrays.asList(GLOBAL_DEATH_MESSAGES.get(deathSubCategoryID)));
        if(mapIDMessages.containsKey(deathCategoryID)) mesagePool.addAll(Arrays.asList(mapIDMessages.get(deathCategoryID)));
        if(mapIDMessages.containsKey(deathSubCategoryID)) mesagePool.addAll(Arrays.asList(mapIDMessages.get(deathSubCategoryID)));

        String selectedMessage = mesagePool.get(new Random().nextInt(mesagePool.size()));
        selectedMessage = selectedMessage.replaceAll(Pattern.compile(Pattern.quote("{player}"), Pattern.CASE_INSENSITIVE).pattern(), deathCause.getVictim().getDisplayName()+TextFormat.RESET+DEATH_MESSAGE_COLOUR);
        String killerName = "???";
        switch (deathCause.getDeathCategory()){
            case KILLER_PLAYER:
                Optional<Player> p = deathCause.getKillerPlayer();
                if(p.isPresent()) killerName = p.get().getDisplayName();
                break;
            case KILLER_ENTITY:
                Optional<Entity> e = deathCause.getKillerEntity();
                if(e.isPresent()) killerName = e.get().getName();
                break;
            case KILLER_BLOCK:
                Optional<Block> b = deathCause.getKillerBlock();
                if(b.isPresent()) killerName = b.get().getName();
                break;
        }
        selectedMessage = selectedMessage.replaceAll(Pattern.compile(Pattern.quote("{killer}"), Pattern.CASE_INSENSITIVE).pattern(), killerName+TextFormat.RESET+DEATH_MESSAGE_COLOUR);
        String deathMessage = Utility.generateServerMessage(Utility.ResourcePackCharacters.SKULL, TextFormat.DARK_GRAY, selectedMessage, DEATH_MESSAGE_COLOUR);

        for(Player player:gameHandler.getPlayers()) player.sendMessage(deathMessage);
        for(Player player:gameHandler.getTourneyMasters()) player.sendMessage(deathMessage);

    }

    protected Kit getPlayerKitPreference(Player player){
        KitGroup gameKits = gameHandler.getGameID().getGameKits();
        return gameKits.getGroupKits().getOrDefault(PlayerKitsManager.get().getPlayerPreferenceForGroup(player, gameKits), gameKits.getDefaultKit());
    }

    public ArrayList<Player> getPlayerDeathOrder() { return playerDeathOrder; } // Does not contain any duplicates. Used for calculating winners.
    public ArrayList<Team> getTeamDeathOrder() { return teamDeathOrder; } // Does not contain any duplicates. Used for calculating winners.
    public ArrayList<DeathCause> getFullDeathLog() { return fullDeathLog; } // Can contain duplicates.

    public HashSet<Player> getPendingRespawns() { return pendingRespawns; }
}
