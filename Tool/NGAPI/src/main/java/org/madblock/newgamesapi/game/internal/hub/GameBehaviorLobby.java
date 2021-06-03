package org.madblock.newgamesapi.game.internal.hub;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockAir;
import cn.nukkit.block.BlockDragonEgg;
import cn.nukkit.entity.item.*;
import cn.nukkit.entity.mob.EntityWither;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.entity.*;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Sound;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.game.events.GamePlayerDeathEvent;
import org.madblock.newgamesapi.game.internal.hub.pointentities.PointEntityTypeKitNPC;
import org.madblock.newgamesapi.game.internal.hub.regions.RegionTagHubBuildArea;
import org.madblock.newgamesapi.rewards.PlayerRewardsProfile;
import org.madblock.newgamesapi.rewards.RewardsManager;
import org.madblock.newgamesapi.team.Team;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;

public class GameBehaviorLobby extends GameBehavior {

    public static final String LOBBY_BUILDER_TEAM_ID = "lobby_builder";
    public static final String LOBBY_BUILDER_KIT_ID = "hub_builder";
    public static final String LOBBY_SUPER_TEAM_ID = "lobby_super";

    protected ArrayList<Player> superPlayers;
    protected RegionTagHubBuildArea buildAreaLogic;

    @Override
    public Team.GenericTeamBuilder[] getTeams() {
        return new Team.TeamBuilder[] {
                Team.newBasicTeamBuilder("default", "Member", Team.Colour.LIGHT_GRAY)
                        .setCanPlayersDropItems(false)
                        .setCanPlayersPickUpItems(false)
                        .setFriendlyFireEnabled(false)
                        .setCanPlaceAndBreakBlocks(false)
                        .setCanPlayersInteractWithBlocks(false)
                        .setCanDealDamage(false),
                (Team.TeamBuilder) Team.newBasicTeamBuilder(LOBBY_BUILDER_TEAM_ID, "Builder", Team.Colour.LIGHT_GRAY)
                        .setCanPlayersDropItems(false)
                        .setCanPlayersPickUpItems(false)
                        .setFriendlyFireEnabled(false)
                        .setCanPlaceAndBreakBlocks(true)
                        .setCanDealDamage(false)
                        .setFlightEnabled(true)
                        .setDoesParticipateInTeamBalancing(false),
                (Team.TeamBuilder) Team.newBasicTeamBuilder(LOBBY_SUPER_TEAM_ID, Utility.ResourcePackCharacters.TAG_SUPER, Team.Colour.MAGENTA)
                        .setCanPlayersDropItems(true)
                        .setCanPlayersPickUpItems(true)
                        .setFriendlyFireEnabled(true)
                        .setCanPlaceAndBreakBlocks(true)
                        .setCanDealDamage(true)
                        .setFlightEnabled(true)
                        .setCanPlayersInteractWithBlocks(true)
                        .setTeamNameDisplayed(true)
                        .setDoesParticipateInTeamBalancing(false),
        };
    }

    @Override public int onGameBegin() {
        this.superPlayers = new ArrayList<>();
        this.buildAreaLogic = new RegionTagHubBuildArea(this.getSessionHandler(), LOBBY_BUILDER_TEAM_ID, "default");
        this.getSessionHandler().getFunctionalRegionManager().setTagFunction("hub_build_area", buildAreaLogic, 5, 5);
        return 0;
    }

    @Override public void onInitialCountdownEnd() {
        getSessionHandler().getPointEntityTypeManager().registerPointEntityType(new PointEntityTypeKitNPC(getSessionHandler()));
    }
    @Override public void registerGameSchedulerTasks() {
        getSessionHandler().getGameScheduler().registerGameTask(this::scoreboardUpdateTick, 40, 40);
    }

    @Override public void onGameDeathByBlock(GamePlayerDeathEvent event) { onDeath(event); }
    @Override public void onGameDeathByEntity(GamePlayerDeathEvent event) { onDeath(event); }
    @Override public void onGameDeathByPlayer(GamePlayerDeathEvent event) { onDeath(event); }
    @Override public void onGameMiscDeathEvent(GamePlayerDeathEvent event) { onDeath(event); }
    public void onDeath(GamePlayerDeathEvent event){
        event.setShowDeathMessage(false);
        event.setDeathState(GamePlayerDeathEvent.DeathState.INSTANT_RESPAWN);
    }

    @Override public Optional<Team> onPreGameJoinEvent(Player player) { return commonJoin(player); }
    @Override public Optional<Team> onCountdownJoinEvent(Player player) { return commonJoin(player); }
    @Override public Optional<Team> onMidGameJoinEvent(Player player) { return commonJoin(player); }
    public Optional<Team> commonJoin(Player player){
        getSessionHandler().getGameScheduler().registerGameTask(() -> {
            for(Player p : getSessionHandler().getPlayers()) {
                player.getLevel().addSound(player.getPosition(), Sound.MOB_ENDERMEN_PORTAL, 1f, 0.8f, p);
            }
        }, 5);


        return Optional.of(getSessionHandler().getTeams().get("default"));
    }

    @Override
    public void onSuper(Player player) {

        if(!superPlayers.contains(player)) {
            getSessionHandler().switchPlayerToTeam(player, getSessionHandler().getTeams().get(LOBBY_SUPER_TEAM_ID), true);
            Utility.setGamemodeWorkaround(player, Player.CREATIVE, false, null);
            getSessionHandler().equipPlayerKit(player, getSessionHandler().getGameID().getGameKits().getGroupKits().getOrDefault(GameBehaviorLobby.LOBBY_BUILDER_KIT_ID, getSessionHandler().getGameID().getGameKits().getDefaultKit()), true);
            superPlayers.add(player);

            player.sendMessage(Utility.generateServerMessage("SUPER", TextFormat.BLUE, "You can now build across the whole lobby! :^)"));
        }
    }

    @Override
    public void onPlayerLeaveGame(Player player) {
        player.setGamemode(Player.SURVIVAL);
        superPlayers.remove(player);
    }

    public void scoreboardUpdateTick(){
        for(Player player: getSessionHandler().getPlayers()) updateScoreboards(player);
    }

    protected void updateScoreboards(Player player){

        Optional<PlayerRewardsProfile> rewards = RewardsManager.get().getRewards(player);

        String coins = "???";
        String level = "???";

        if (rewards.isPresent()) {
            PlayerRewardsProfile p = rewards.get();
            coins = String.valueOf(p.getCoins());
            level = String.format("%s %s(%s%s%s/%s%sxp%s)", Utility.getLevelText(p.getLevel()), TextFormat.GRAY, TextFormat.GREEN, p.getExperience(), TextFormat.GRAY, TextFormat.DARK_GREEN, p.getExperience() + p.getXPRequiredToLevelUp(), TextFormat.GRAY);
        } else {
            NewGamesAPI1.get().getServer().getScheduler().scheduleTask(NewGamesAPI1.get(), () -> {
                try {
                    RewardsManager.get().fetchRewards(player);
                } catch (SQLException exception) {}
            }, true);
        }

        getSessionHandler().getScoreboardManager().setLine(player, 0, String.format("%s %s%s", Utility.ResourcePackCharacters.PING_BEST, TextFormat.RED, getSessionHandler().getServerID()));
        getSessionHandler().getScoreboardManager().setLine(player, 1, String.format("%s %s%s", Utility.ResourcePackCharacters.COIN, TextFormat.GOLD, coins));
        getSessionHandler().getScoreboardManager().setLine(player, 2, String.format("%s%s%sXP %s%s", TextFormat.GREEN, TextFormat.BOLD, TextFormat.UNDERLINE, TextFormat.RESET, level));
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event){
        if(event.getEntity() instanceof Player){
            Player p = (Player) event.getEntity();
            if(getSessionHandler().getPlayers().contains(p)){
                event.setCancelled(true);
            }
        } else {
            if(event.getEntity().getLevel() == getSessionHandler().getPrimaryMap()){
                if(buildAreaLogic.isLegalPlacement(event.getEntity().getLevel(), event.getEntity().getPosition().asBlockVector3())){
                    event.getEntity().close();
                }
            }
        }
    }

    @EventHandler
    public void onPlayerTriggeredSpawning(CreatureSpawnEvent event){
        if(event.getPosition().getLevel() == getSessionHandler().getPrimaryMap()){
            if( event.getReason() == CreatureSpawnEvent.SpawnReason.SPAWN_EGG ||
                event.getReason() == CreatureSpawnEvent.SpawnReason.DISPENSE_EGG ||
                event.getReason() == CreatureSpawnEvent.SpawnReason.EGG ||
                event.getReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
                    if(!buildAreaLogic.isLegalPlacement(getSessionHandler().getPrimaryMap(), event.getPosition().asBlockVector3())) {
                        event.setCancelled(true);
                    }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlacklistedEntitySpawn(EntitySpawnEvent event){
        if(event.getEntity().getLevel() == getSessionHandler().getPrimaryMap()) {
            if (event.getEntity() instanceof EntityPrimedTNT ||
                event.getEntity() instanceof EntityMinecartTNT ||
                event.getEntity() instanceof EntityEndCrystal ||
                event.getEntity() instanceof EntityWither ||

                event.getEntity() instanceof EntityProjectile ||

                event.getEntity() instanceof EntityXPOrb ||
                event.getEntity() instanceof EntityItem
            ) {
                if(!(event.getEntity().namedTag.exist("bypass_removal") && event.getEntity().namedTag.getBoolean("bypass_removal"))) {
                    event.getEntity().close();
                }
            }
        }
    }

    @EventHandler
    public void onDragonEggInteract(PlayerInteractEvent event){
        if(getSessionHandler().getPlayers().contains(event.getPlayer())) {
            if(event.getBlock() instanceof BlockDragonEgg){
                event.setCancelled(true);
                event.getBlock().getLevel().addParticleEffect(event.getBlock(), ParticleEffect.DRAGON_DYING_EXPLOSION);
                event.getBlock().getLevel().setBlock(event.getBlock(), new BlockAir());
            }
        }
    }

    @EventHandler
    public void onActualRegularExplosion(ExplosionPrimeEvent event){
        if(event.getEntity().getLevel() == getSessionHandler().getPrimaryMap()) {
            event.setBlockBreaking(false);
        }
    }

    @EventHandler
    public void onActualEntityExplosion(EntityExplosionPrimeEvent event){
        if(event.getEntity().getLevel() == getSessionHandler().getPrimaryMap()) {
            event.setBlockBreaking(false);
        }
    }

    @EventHandler
    public void onBuildAreaBlockPlace(BlockPlaceEvent event){
        if(getSessionHandler().getPlayers().contains(event.getPlayer()) && (!superPlayers.contains(event.getPlayer()))) {
            Block b = event.getBlock();
            if (!buildAreaLogic.isLegalPlacement(b.getLevel(), b.asBlockVector3())) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBuildAreaBlockBreak(BlockBreakEvent event){
        if(getSessionHandler().getPlayers().contains(event.getPlayer()) && (!superPlayers.contains(event.getPlayer()))) {
            Block b = event.getBlock();
            if (!buildAreaLogic.isLegalPlacement(b.getLevel(), b.asBlockVector3())) event.setCancelled(true);
        }
    }

    public ArrayList<Player> getSuperPlayers() {
        return new ArrayList<>(superPlayers);
    }
}
