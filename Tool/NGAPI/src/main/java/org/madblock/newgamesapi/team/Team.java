package org.madblock.newgamesapi.team;

import cn.nukkit.AdventureSettings;
import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.util.Utility;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.ranks.api.RankManager;
import org.madblock.ranks.api.RankProfile;
import org.madblock.ranks.enums.PrimaryRankID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

public class Team implements Listener {

    public static final String COLOUR_CHARACTER = "\u00A7";

    protected GameHandler gameHandler;

    protected String id;
    protected String displayname;
    protected Colour colour;
    protected HashSet<Player> players;

    protected boolean isActiveGameTeam;
    protected boolean doesParticipateInTeamBalancing;

    protected boolean canDealDamage;
    protected boolean isFriendlyFireEnabled;
    protected boolean isTeamNameDisplayed;
    protected boolean canPlaceAndBreakBlocks;
    protected boolean canPlayersDropItems;
    protected boolean canPlayersInteractWithBlocks;
    protected boolean canPlayersPickUpItems;
    protected boolean arePlayersVisible;
    protected boolean canPlayersTakeFallDamage;
    protected boolean isFlightEnabled;
    protected boolean isNoClipEnabled;

    protected Team(GameHandler gameHandler, String id, String displayname, Colour colour, boolean isActiveGameTeam, boolean doesParticipateInTeamBalancing, boolean canDealDamage, boolean isFriendlyFireEnabled, boolean isTeamNameDisplayed, boolean canPlaceAndBreakBlocks, boolean canPlayersDropItems, boolean canPlayersInteractWithBlocks, boolean canPlayersPickUpItems, boolean arePlayersVisible, boolean canPlayersTakeFallDamage, boolean isFlightEnabled, boolean isNoClipEnabled) {
        this.gameHandler = gameHandler;
        this.id = id;
        this.displayname = displayname;
        this.colour = colour;
        this.players = new HashSet<>();

        this.isActiveGameTeam = isActiveGameTeam;
        this.doesParticipateInTeamBalancing = doesParticipateInTeamBalancing;

        this.canDealDamage = canDealDamage;
        this.isFriendlyFireEnabled = isFriendlyFireEnabled;
        this.isTeamNameDisplayed = isTeamNameDisplayed;
        this.canPlaceAndBreakBlocks = canPlaceAndBreakBlocks;
        this.canPlayersDropItems = canPlayersDropItems;
        this.canPlayersInteractWithBlocks = canPlayersInteractWithBlocks;
        this.canPlayersPickUpItems = canPlayersPickUpItems;
        this.arePlayersVisible = arePlayersVisible;
        this.canPlayersTakeFallDamage = canPlayersTakeFallDamage;
        this.isFlightEnabled = isFlightEnabled;
        this.isNoClipEnabled = isNoClipEnabled;
    }

    /** Register listeners and such. */
    public void activate(){
        NewGamesAPI1.get().getServer().getPluginManager().registerEvents(this, NewGamesAPI1.get());
    }

    /** Unregisters listeners, remove players, etc etc. Just make the team unusable.*/
    public void destroy() {
        HandlerList.unregisterAll(this);
        for(Player player: players){
            removePlayerFromTeam(player);
        }
    }

    /**
     * Adds the specified player from the team, changing their name colour to the team
     * colour.
     * @return true if the player was added (And not already on the team)
     */
    public boolean addPlayerToTeam(Player player) {
        if(gameHandler.getPlayers().contains(player) && this.players.add(player)){
            updatePlayerDisplayName(player, false);
            updateBuildingPermissionsState(player, false);
            updateHungerState(player);
            updatePlayerVisibilityState(player, false);
            updatePlayerFlightState(player, false);
            gameHandler.onTeamAddPlayer(player, this);
            return true;
        }
        return false;
    }
    /**
     * Removes the specified player from the team and removes name colour.
     *  @return true if player was found on team.
     */
    public boolean removePlayerFromTeam(Player player) {
        if(players.remove(player)){
            updatePlayerDisplayName(player, true);
            updatePlayerVisibilityState(player, true);
            updatePlayerFlightState(player, true);
            gameHandler.onTeamRemovePlayer(player, this);
            return true;
        }
        return false;
    }

    /** @return returns the gamehandler. Potentially not assigned.*/
    public GameHandler getGameHandler() {
        return gameHandler;
    }
    /** @return the internal id used to identify the team.*/
    public String getId() { return id; }
    /** @return the name of the team intended to be displayed to players, without colour.*/
    public String getDisplayName() { return displayname; }
    /** @return the name of the team intended to be displayed to players, with colour and bold.*/
    public String getFormattedDisplayName() { return COLOUR_CHARACTER + "l" + COLOUR_CHARACTER + colour.COLOUR_CHAR + displayname + TextFormat.RESET; }
    /** @return the enum colour of the team which can be converted into a chat colour.*/
    public Colour getColour() { return colour; }
    /** @return a hashset (Fancy List) of all the players on the team. */
    public HashSet<Player> getPlayers() { return players; }
    /**
     * Used to determine if a team should be counted when checking for victories or
     * if its behaviors are more like a spectating team.
     * @return true if the team counts towards a victory.
     */
    public boolean isActiveGameTeam() { return isActiveGameTeam; }
    /** @return true if the team is included in team balancing. */
    public boolean doesParticipateInTeamBalancing() { return doesParticipateInTeamBalancing; }
    /** @return true if players on the team can do damage to entities. */
    public boolean canDealDamage() { return canDealDamage; }
    /** @return true if players on the team can do damage to players on their own team. */
    public boolean isFriendlyFireEnabled() { return isFriendlyFireEnabled; }
    /** @return true if the team name is displayed in player nametags */
    public boolean isTeamNameDisplayed() { return isTeamNameDisplayed; }
    /** @return true if the team's players can place and break blocks (Only if game properties enables it) */
    public boolean canPlaceAndBreakBlocks() { return canPlaceAndBreakBlocks; }
    /** @return true if the team can drop items */
    public boolean canPlayersDropItems() { return canPlayersDropItems; }
    /** @return true if the player can interact with blocks */
    public boolean canPlayersInteractWithBlocks() { return canPlayersInteractWithBlocks; }
    /** @return true if the team can pick up items */
    public boolean canPlayersPickUpItems () { return canPlayersPickUpItems; }
    /** @return true if the players of the team are invisible. */
    public boolean arePlayersVisible() { return arePlayersVisible; }
    /** @return true if the can take fall damage. */
    public boolean canPlayersTakeFallDamage() { return canPlayersTakeFallDamage; }
    /** @return true if players on the team can fly. */
    public boolean isFlightEnabled() { return isFlightEnabled; }
    /** @return true if players on the team can noclip whilst flying. */
    public boolean isNoClipEnabled() { return isNoClipEnabled; }

    public void setCanDealDamage(boolean state) { this.canDealDamage = state; }
    public void setFriendlyFireEnabled(boolean state) { this.isFriendlyFireEnabled = state; }
    public void setTeamNameDisplayed(boolean state) { this.isTeamNameDisplayed = state; updateTeamDisplayNames(); }
    public void setCanPlaceAndBreakBlocks(boolean state) { this.canPlaceAndBreakBlocks = state; updateTeamBuildingPermissionsState(); }
    public void setCanPlayersDropItems(boolean state) { this.canPlayersDropItems = state; }
    public void setCanPlayersPickUpItems(boolean state) { this.canPlayersPickUpItems = state; }
    public void setArePlayersVisible(boolean state) { this.arePlayersVisible = state; updateTeamVisibilityState(); }
    public void setCanPlayersTakeFallDamage(boolean canPlayersTakeFallDamage) { this.canPlayersTakeFallDamage = canPlayersTakeFallDamage; }
    public void setFlightEnabled(boolean flightEnabled) { isFlightEnabled = flightEnabled; updateTeamFlightState(); }
    public void setNoClipEnabled(boolean noClipEnabled) { isNoClipEnabled = noClipEnabled; updateTeamFlightState(); }

    protected void updatePlayerDisplayName(Player player, boolean isLeavingTeam){
        Optional<RankProfile> rankProfile = RankManager.getInstance().getRankProfile(player);
        if(players.contains(player)) {
            if (isTeamNameDisplayed) {
                String newname;
                if (rankProfile.isPresent()) {
                    PrimaryRankID displayRank = rankProfile.get().getPrimaryDisplayedRank();
                    if (displayRank.getName().isPresent()) {
                        newname = getFormattedDisplayName() +  " " + displayRank.getColor().orElse(TextFormat.WHITE) + TextFormat.BOLD + displayRank.getName().get() + TextFormat.RESET + " " + colour.getColourString() + player.getName();
                    } else {
                        newname = getFormattedDisplayName() + " " + colour.getColourString() + player.getName();
                    }
                } else {
                    newname = getFormattedDisplayName() + " " + colour.getColourString() + player.getName();
                }
                player.setNameTag(newname);
            } else {
                String newname;
                if (rankProfile.isPresent()) {
                    PrimaryRankID displayRank = rankProfile.get().getPrimaryDisplayedRank();
                    if (displayRank.getName().isPresent()) {
                        newname = "" + displayRank.getColor().orElse(TextFormat.WHITE) + TextFormat.BOLD + displayRank.getName().get() + TextFormat.RESET + " " + colour.getColourString() + player.getName();
                    } else {
                        newname = colour.getColourString() + player.getName();
                    }
                } else {
                    newname = colour.getColourString() + player.getName();
                }
                player.setNameTag(newname);
            }
        }
        if(isLeavingTeam){
            if (rankProfile.isPresent()) {
                PrimaryRankID displayRank = rankProfile.get().getPrimaryDisplayedRank();
                if (displayRank.getName().isPresent()) {
                    player.setNameTag("" + displayRank.getColor().orElse(TextFormat.WHITE) + TextFormat.BOLD + displayRank.getName().get() + TextFormat.RESET + " " + player.getName());
                } else {
                    player.setNameTag(player.getName());
                }
            } else {
                player.setNameTag(player.getName());
            }
            return;
        }
    }

    protected void updateBuildingPermissionsState(Player player, boolean isLeavingTeam){
        if(getPlayers().contains(player)) {
            GameHandler.GameState gameState = gameHandler.getGameState();
            if (gameState == GameHandler.GameState.END) {
                player.getAdventureSettings().set(AdventureSettings.Type.BUILD_AND_MINE, false);
                player.getAdventureSettings().set(AdventureSettings.Type.WORLD_BUILDER, false);
                player.getAdventureSettings().set(AdventureSettings.Type.WORLD_IMMUTABLE, true);
                //player.getAdventureSettings().update();
                Utility.executeExperimentalAdventureSettingsUpdate(player);
                return;
            }
            if (gameState == GameHandler.GameState.MAIN_LOOP) {
                if (!gameHandler.getGameID().getGameProperties().canWorldBeManipulated()) {
                    player.getAdventureSettings().set(AdventureSettings.Type.BUILD_AND_MINE, false);
                    player.getAdventureSettings().set(AdventureSettings.Type.WORLD_BUILDER, false);
                    player.getAdventureSettings().set(AdventureSettings.Type.WORLD_IMMUTABLE, true);
                    //player.getAdventureSettings().update();
                    Utility.executeExperimentalAdventureSettingsUpdate(player);
                    return;
                }
            } else {
                if (!gameHandler.getGameID().getGameProperties().canWorldBeManipulatedPreGame()) {
                    player.getAdventureSettings().set(AdventureSettings.Type.BUILD_AND_MINE, false);
                    player.getAdventureSettings().set(AdventureSettings.Type.WORLD_BUILDER,  false);
                    player.getAdventureSettings().set(AdventureSettings.Type.WORLD_IMMUTABLE, true);
                    //player.getAdventureSettings().update();
                    Utility.executeExperimentalAdventureSettingsUpdate(player);
                    return;
                }
            }

            player.getAdventureSettings().set(AdventureSettings.Type.BUILD_AND_MINE, canPlaceAndBreakBlocks());
            player.getAdventureSettings().set(AdventureSettings.Type.WORLD_BUILDER, canPlaceAndBreakBlocks());
            player.getAdventureSettings().set(AdventureSettings.Type.WORLD_IMMUTABLE, !canPlaceAndBreakBlocks());
            //player.getAdventureSettings().update();
            Utility.executeExperimentalAdventureSettingsUpdate(player);
        }

        if(isLeavingTeam) {
            player.getAdventureSettings().set(AdventureSettings.Type.BUILD_AND_MINE, true);
            player.getAdventureSettings().set(AdventureSettings.Type.WORLD_BUILDER, true);
            player.getAdventureSettings().set(AdventureSettings.Type.WORLD_IMMUTABLE, false);
            //player.getAdventureSettings().update();
            Utility.executeExperimentalAdventureSettingsUpdate(player);
            return;
        }
    }

    protected void updateHungerState(Player player) {
        if(!gameHandler.getGameID().getGameProperties().isHungerEnabled()){
            player.getFoodData().setLevel(20, 20);
            player.setFoodEnabled(false);
        } else {
            player.setFoodEnabled(true);
        }
    }

    protected void updatePlayerVisibilityState(Player player, boolean isLeavingTeam){
        ArrayList<Player> others = new ArrayList<>(NewGamesAPI1.get().getServer().getOnlinePlayers().values());
        others.remove(player);
        if(players.contains(player)){
            if(arePlayersVisible()){
                for(Player p: others) p.showPlayer(player);
            } else {
                for(Player p: others) p.hidePlayer(player);
            }
        } else if (isLeavingTeam) {
            for(Player p: others) p.showPlayer(player);
        }
    }

    protected void updatePlayerFlightState(Player player, boolean isLeavingTeam){
        if(players.contains(player)){
            player.getAdventureSettings().set(AdventureSettings.Type.ALLOW_FLIGHT, isFlightEnabled);
            if(isFlightEnabled) {
                player.getAdventureSettings().set(AdventureSettings.Type.FLYING, isNoClipEnabled);
                player.getAdventureSettings().set(AdventureSettings.Type.NO_CLIP, isNoClipEnabled);
            } else {
                player.getAdventureSettings().set(AdventureSettings.Type.FLYING, false);
                player.getAdventureSettings().set(AdventureSettings.Type.NO_CLIP, false);
            }
        }
        if(isLeavingTeam){
            player.getAdventureSettings().set(AdventureSettings.Type.ALLOW_FLIGHT, false);
            player.getAdventureSettings().set(AdventureSettings.Type.FLYING, false);
            player.getAdventureSettings().set(AdventureSettings.Type.NO_CLIP, false);
        }
        //player.getAdventureSettings().update();
        Utility.executeExperimentalAdventureSettingsUpdate(player);
    }

    public void updateTeamDisplayNames(){ for(Player player: players) updatePlayerDisplayName(player, false); }
    public void updateTeamVisibilityState(){ for(Player player: players) updatePlayerVisibilityState(player, false); }
    public void updateTeamBuildingPermissionsState(){ for(Player player: players) updateBuildingPermissionsState(player, false); }
    public void updateTeamFlightState(){ for(Player player: getPlayers()) updatePlayerFlightState(player, false); }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamageByEntity(EntityDamageByEntityEvent event){
        if(!canDealDamage){
            if(event.getDamager() instanceof Player) {
                Player playerAttacker = (Player) event.getDamager();
                if(players.contains(playerAttacker)){
                    event.setCancelled(true);
                    return;
                }
            }
        }
        if(!isFriendlyFireEnabled){
            if(event.getDamager() instanceof Player && event.getEntity() instanceof Player){
                Player playerAttacker = (Player) event.getDamager();
                Player playerVictim = (Player) event.getEntity();
                if(players.contains(playerAttacker) && players.contains(playerVictim)){
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageEvent event){
        if(event.getEntity() instanceof Player){
            Player player = (Player) event.getEntity();
            if(players.contains(player)){
                if((event.getCause() == EntityDamageEvent.DamageCause.FALL)){
                    if(gameHandler.getGameState() == GameHandler.GameState.MAIN_LOOP) {
                        if(!(gameHandler.getGameID().getGameProperties().isFallDamageEnabled() && canPlayersTakeFallDamage)) event.setCancelled(true);
                    } else if(!gameHandler.getGameID().getGameProperties().isFallDamageEnabledPreGame()){
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    public static TeamBuilder newBasicTeamBuilder(String id, String teamname, Colour colour){
        return new TeamBuilder(id, teamname, colour);
    }

    public static class TeamBuilder extends GenericTeamBuilder{

        protected boolean canDealDamage;
        protected boolean isFriendlyFireEnabled;
        protected boolean isTeamNameDisplayed;
        protected boolean canPlaceAndBreakBlocks;
        protected boolean canPlayersDropItems;
        protected boolean canPlayersInteractWithBlocks;
        protected boolean canPlayersPickUpItems;
        protected boolean arePlayersVisible;
        protected boolean canPlayersTakeFallDamage;
        protected boolean isFlightEnabled;
        protected boolean isNoClipEnabled;

        public TeamBuilder(String id, String displayName, Colour colour){
            super(id, displayName, colour);
            this.canDealDamage = true;
            this.isFriendlyFireEnabled = false;
            this.isTeamNameDisplayed = false;
            this.canPlaceAndBreakBlocks = true;
            this.canPlayersDropItems = true;
            this.canPlayersInteractWithBlocks = true;
            this.canPlayersPickUpItems = true;
            this.arePlayersVisible = true;
            this.canPlayersTakeFallDamage = true;
            this.isFlightEnabled = false;
            this.isNoClipEnabled = false;
        }

        @Override
        public Team build(GameHandler handler){
            verify(handler);
            Team team = new Team(
                    handler,
                    id,
                    displayName,
                    colour,
                    isActiveGameTeam,
                    doesParticipateInTeamBalancing,
                    canDealDamage,
                    isFriendlyFireEnabled,
                    isTeamNameDisplayed,
                    canPlaceAndBreakBlocks,
                    canPlayersDropItems,
                    canPlayersInteractWithBlocks,
                    canPlayersPickUpItems,
                    arePlayersVisible,
                    canPlayersTakeFallDamage,
                    isFlightEnabled,
                    isNoClipEnabled
            );
            team.activate();
            return team;
        }

        @Override
        protected void verify(GameHandler handler){
            super.verify(handler);
            if(isNoClipEnabled) isFlightEnabled = true;
        }

        public boolean canPlayersDealDamage() { return canDealDamage; }
        public boolean isFriendlyFireEnabled() { return isFriendlyFireEnabled; }
        public boolean isTeamNameDisplayed() { return isTeamNameDisplayed; }
        public boolean canPlaceAndBreakBlocks() { return canPlaceAndBreakBlocks; }
        public boolean canPlayersDropItems() { return canPlayersDropItems; }
        public boolean canPlayersInteractWithBlocks() { return canPlayersInteractWithBlocks; }
        public boolean canPlayersPickUpItems() { return canPlayersPickUpItems; }
        public boolean arePlayersVisible() { return arePlayersVisible; }
        public boolean isFlightEnabled() { return isFlightEnabled; }
        public boolean isNoClipEnabled() { return isNoClipEnabled; }

        /** @param state Sets the teams ability to deal damage to entities. */
        public TeamBuilder setCanDealDamage(boolean state) { this.canDealDamage = state; return this; }
        /** @param state Sets the teams ability to deal damage to other team members on their team. */
        public TeamBuilder setFriendlyFireEnabled(boolean state) { this.isFriendlyFireEnabled = state; return this; }
        /** @param state Sets if the team name should be displayed in team nametags */
        public TeamBuilder setTeamNameDisplayed(boolean state) { this.isTeamNameDisplayed = state; return this; }
        /** @param state Sets if the team allows block placing + breaking. */
        public TeamBuilder setCanPlaceAndBreakBlocks(boolean state) { this.canPlaceAndBreakBlocks = state; return this; }
        /** @param state Sets if the team allows dropping of items.*/
        public TeamBuilder setCanPlayersDropItems(boolean state) { this.canPlayersDropItems = state; return this; }
        /** @param state Sets if the team allows interactions with blocks */
        public TeamBuilder setCanPlayersInteractWithBlocks(boolean state) { this.canPlayersInteractWithBlocks = state; return this; }
        /** @param state Sets if the team allows picking up items.*/
        public TeamBuilder setCanPlayersPickUpItems(boolean state) { this.canPlayersPickUpItems = state; return this; }
        /** @param state Sets if the team's players are visible or not. <b>Invisible players have no collision so they can't be hit!</b> */
        public TeamBuilder setArePlayersVisible(boolean state) { this.arePlayersVisible = state; return this; }
        public TeamBuilder setFlightEnabled(boolean flightEnabled) { isFlightEnabled = flightEnabled; return this; }
        public TeamBuilder setNoClipEnabled(boolean noClipEnabled) { isNoClipEnabled = noClipEnabled; return this; }
    }

    public static abstract class GenericTeamBuilder {

        protected String id;
        protected String displayName;
        protected Colour colour;

        protected boolean isActiveGameTeam;
        protected boolean doesParticipateInTeamBalancing;

        public GenericTeamBuilder(String id, String displayName, Colour colour) {
            if(id == null) throw new IllegalArgumentException("Team ID must not be null!");
            if(displayName == null) throw new IllegalArgumentException("Team Name must not be null!");
            if(colour == null) throw new IllegalArgumentException("Team Colour must not be null!");
            this.id = id.toLowerCase();
            this.displayName = displayName;
            this.colour = colour;

            this.isActiveGameTeam = true;
            this.doesParticipateInTeamBalancing = true;
        }

        public abstract Team build(GameHandler handler);

        protected void verify(GameHandler handler){
            if(handler == null) throw new IllegalArgumentException("GameHandler must not be null!");
            if((!isActiveGameTeam)) doesParticipateInTeamBalancing = false;
        }

        /** @param state Sets how the team is treated. False often treats the team like a spectator to a game. */
        public GenericTeamBuilder setActiveGameTeam(boolean state) { this.isActiveGameTeam = state; return this; }
        /** @param state Sets if the team is filled during team balancing. */
        public GenericTeamBuilder setDoesParticipateInTeamBalancing(boolean state) { this.doesParticipateInTeamBalancing = state; return this; }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public Colour getColour() { return colour; }
        public boolean isActiveGameTeam() { return isActiveGameTeam; }
        public boolean isDoesParticipateInTeamBalancing() { return doesParticipateInTeamBalancing; }
    }

    public enum Colour {
        BLACK('0'),
        INDIGO('1'),
        GREEN('2'),
        CYAN('3'),
        DARK_RED('4'),
        PURPLE('5'),
        ORANGE('6'),
        LIGHT_GRAY('7'),
        GRAY('8'),
        BLUE('9'),
        LIME('a'),
        AQUA('b'),
        RED('c'),
        MAGENTA('d'),
        YELLOW('e'),
        WHITE('f');

        private final char COLOUR_CHAR;
        Colour(char colour_char){ this.COLOUR_CHAR = colour_char; }
        public char getColourChar(){ return COLOUR_CHAR; }
        public String getColourString(){ return COLOUR_CHARACTER+COLOUR_CHAR; }
    }

}
