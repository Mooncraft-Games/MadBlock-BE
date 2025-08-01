package org.madblock.newgamesapi.game;

import org.madblock.lib.commons.style.Check;
import org.madblock.newgamesapi.game.pvp.CustomGamePVPSettings;

public class GameProperties {

    private boolean canWorldBeManipulatedPreGame;
    private boolean canWorldBeManipulated;

    private boolean isHungerEnabled;
    private boolean isNatualRegenEnabled;

    private boolean doesGameShufflePlayerSpawns;
    private int defaultCountdownLength;
    private boolean canPlayersMoveDuringCountdown;

    private boolean doesGameUseIntegratedDeathboxes;
    private boolean doesGameUseIntegratedPointEntities;

    private boolean isItemDroppingEnabledPreGame;
    private boolean isItemDroppingEnabled;

    private boolean isItemPickUpEnabledPreGame;
    private boolean isItemPickUpEnabled;

    private boolean isFallDamageEnabledPreGame;
    private boolean isFallDamageEnabled;

    private CustomGamePVPSettings customPvpSettings;
    private int spawnImmunitySeconds;

    private boolean isTourneyGamemode;

    private boolean isInternalRewardsEnabled;

    private GameHandler.AutomaticWinPolicy winPolicy;

    private int minimumPlayers;
    private int guidelinePlayers;
    private int maximumPlayers;

    private String[] requiredPermissions;

    private String iconPath;

    public GameProperties(GameHandler.AutomaticWinPolicy winPolicy) {
        this.canWorldBeManipulatedPreGame = false;
        this.canWorldBeManipulated = false;

        this.isHungerEnabled = false;
        this.isNatualRegenEnabled = true;

        this.doesGameShufflePlayerSpawns = true;
        this.defaultCountdownLength = 15;
        this.canPlayersMoveDuringCountdown = false;

        this.doesGameUseIntegratedDeathboxes = true;
        this.doesGameUseIntegratedPointEntities = true;

        this.isItemDroppingEnabledPreGame = false;
        this.isItemDroppingEnabled = false;

        this.isItemPickUpEnabledPreGame = false;
        this.isItemPickUpEnabled = false;

        this.isFallDamageEnabledPreGame = false;
        this.isFallDamageEnabled = false;

        this.customPvpSettings = new CustomGamePVPSettings();
        this.spawnImmunitySeconds = 3;

        this.isTourneyGamemode = false;

        this.isInternalRewardsEnabled = true;

        this.winPolicy = winPolicy;

        this.minimumPlayers = 2;
        this.guidelinePlayers = 4;
        this.maximumPlayers = 16;

        this.requiredPermissions = new String[0];

        this.iconPath = "";
    }

    public GameProperties(GameProperties src) {
        this.canWorldBeManipulatedPreGame = src.canWorldBeManipulatedPreGame;
        this.canWorldBeManipulated = src.canWorldBeManipulated;

        this.isHungerEnabled = src.isHungerEnabled;
        this.isNatualRegenEnabled = src.isNatualRegenEnabled;

        this.doesGameShufflePlayerSpawns = src.doesGameShufflePlayerSpawns;
        this.defaultCountdownLength = src.defaultCountdownLength;
        this.canPlayersMoveDuringCountdown = src.canPlayersMoveDuringCountdown;

        this.doesGameUseIntegratedDeathboxes = src.doesGameUseIntegratedDeathboxes;
        this.doesGameUseIntegratedPointEntities = src.doesGameUseIntegratedPointEntities;

        this.isItemDroppingEnabledPreGame = src.isItemDroppingEnabledPreGame;
        this.isItemDroppingEnabled = src.isItemDroppingEnabled;

        this.isItemPickUpEnabledPreGame = src.isItemPickUpEnabledPreGame;
        this.isItemPickUpEnabled = src.isItemPickUpEnabled;

        this.isFallDamageEnabledPreGame = src.isFallDamageEnabledPreGame;
        this.isFallDamageEnabled = src.isFallDamageEnabled;

        this.customPvpSettings = src.customPvpSettings.clone();
        this.spawnImmunitySeconds = src.spawnImmunitySeconds;

        this.isTourneyGamemode = src.isTourneyGamemode;

        this.isInternalRewardsEnabled = src.isInternalRewardsEnabled;

        this.winPolicy = src.winPolicy;

        this.minimumPlayers = src.minimumPlayers;
        this.guidelinePlayers = src.guidelinePlayers;
        this.maximumPlayers = src.maximumPlayers;

        this.requiredPermissions = src.requiredPermissions;

        this.iconPath = src.iconPath;
    }

    public boolean canWorldBeManipulatedPreGame() { return canWorldBeManipulatedPreGame; }
    public boolean canWorldBeManipulated() { return canWorldBeManipulated; }

    public boolean isHungerEnabled() { return isHungerEnabled; }
    public boolean isNatualRegenerationEnabled() { return isNatualRegenEnabled; }

    public boolean doesGameShufflePlayerSpawns() { return doesGameShufflePlayerSpawns; }
    public int getDefaultCountdownLength() { return defaultCountdownLength; }
    public boolean canPlayersMoveDuringCountdown() { return canPlayersMoveDuringCountdown; }

    public boolean doesGameUseIntegratedDeathboxes() { return doesGameUseIntegratedDeathboxes; }
    public boolean doesGameUseIntegratedPointEntities() { return doesGameUseIntegratedPointEntities; }

    public boolean isItemDroppingEnabledPreGame() { return isItemDroppingEnabledPreGame; }
    public boolean isItemDroppingEnabled(){ return isItemDroppingEnabled; }

    public boolean isItemPickUpEnabledPreGame() { return isItemPickUpEnabledPreGame; }
    public boolean isItemPickUpEnabled() { return isItemPickUpEnabled; }

    public boolean isFallDamageEnabledPreGame() { return isFallDamageEnabledPreGame; }
    public boolean isFallDamageEnabled() { return isFallDamageEnabled; }

    public boolean isTourneyGamemode() { return isTourneyGamemode; }

    /**
     * See if players should get additional rewards on the game ending.
     * (e.g. first place or kills)
     * @return if players should get additional rewards
     */
    public boolean isInternalRewardsEnabled() {
        return isInternalRewardsEnabled;
    }

    /**
     * The contents of the settings should not be modified.
     * If you need to modify the settings during a game see CustomPVPManager
     * @return settings
     */
    public CustomGamePVPSettings getCustomPvpSettings() {
        return this.customPvpSettings;
    }

    /**
     * The amount of seconds of immunity given after a game starts.
     * Does not apply to respawns.
     */
    public int getSpawnImmunitySeconds() {
        return spawnImmunitySeconds;
    }

    public GameHandler.AutomaticWinPolicy getWinPolicy() { return winPolicy; }

    public int getMinimumPlayers() { return minimumPlayers; }
    public int getGuidelinePlayers() { return guidelinePlayers; }
    public int getMaximumPlayers() { return maximumPlayers; }

    public String[] getRequiredPermissions() { return requiredPermissions; }

    public String getIconPath() {
        return iconPath;
    }

    /**
     * Sets the game world's immutability permission.
     * @param canWorldBeManipulated if true, the world will be non-immutable and players can interact with it.
     * @return self for chaining.
     */
    public GameProperties setCanWorldBeManipulatedPreGame(boolean canWorldBeManipulated) {
        this.canWorldBeManipulated = canWorldBeManipulated;
        return this;
    }

    /**
     * Sets the game world's immutability permission.
     * @param canWorldBeManipulated if true, the world will be non-immutable and players can interact with it.
     * @return self for chaining.
     */
    public GameProperties setCanWorldBeManipulated(boolean canWorldBeManipulated) {
        this.canWorldBeManipulated = canWorldBeManipulated;
        return this;
    }

    /**
     * Sets if active players in the game should have their hunger fixed to max hunger.
     * @param hungerEnabled if false, the game will stop hunger loss and set hunger to 20 for certain events.
     * @return self for chaining.
     */
    public GameProperties setHungerEnabled(boolean hungerEnabled) {
        isHungerEnabled = hungerEnabled;
        return this;
    }

    /**
     * Sets if active players in the game should have their hunger fixed to max hunger.
     * @param natualRegenEnabled if false, the game will stop hunger loss and set hunger to 20 for certain events.
     * @return self for chaining.
     */
    public GameProperties setNatualRegenerationEnabled(boolean natualRegenEnabled) {
        this.isNatualRegenEnabled = natualRegenEnabled;
        return this;
    }

    /**
     * Sets if the SpawnManager should shuffle incoming spawns.
     * @param shufflePlayerSpawns true if player spawns for a map get their order shuffled initially.
     * @return self for chaining.
     */
    public GameProperties setDoesGameShufflePlayerSpawns(boolean shufflePlayerSpawns) {
        this.doesGameShufflePlayerSpawns = shufflePlayerSpawns;
        return this;
    }

    /**
     * Sets the game's default length for the countdown if it isn't overrided by the map..
     * @param defaultCountdownLength in seconds, the length of the countdown
     * @return self for chaining.
     */
    public GameProperties setDefaultCountdownLength(int defaultCountdownLength) {
        this.defaultCountdownLength = defaultCountdownLength;
        return this;
    }

    /**
     * Sets if players are able to more or are frozen during countdown.
     * @param canPlayersMoveDuringCountdown true if players aren't frozen
     * @return self for chaining.
     */
    public GameProperties setCanPlayersMoveDuringCountdown(boolean canPlayersMoveDuringCountdown) {
        this.canPlayersMoveDuringCountdown = canPlayersMoveDuringCountdown;
        return this;
    }

    /**
     * Determines if the build in TagBehaviorDeathbox and it's inverted type are automatically registered.
     * @param doesGameUseIntegratedDeathboxes true if the built-in deathboxes should be used.
     * @return self for chaining.
     */
    public GameProperties setDoesGameUseIntegratedDeathboxes(boolean doesGameUseIntegratedDeathboxes) {
        this.doesGameUseIntegratedDeathboxes = doesGameUseIntegratedDeathboxes;
        return this;
    }

    /**
     * Determines if the game should use the API's default PointEntities. This setting is HIGHLY recommended to
     * be left true as you can override any point entities that you don't want.
     * @param doesGameUseIntegratedPointEntities true if the built-in point entity set should be used.
     * @return self for chaining.
     */
    public GameProperties setDoesGameUseIntegratedPointEntities(boolean doesGameUseIntegratedPointEntities) {
        this.doesGameUseIntegratedPointEntities = doesGameUseIntegratedPointEntities;
        return this;
    }

    /**
     * Sets if players are able to drop items from their inventories.
     * @param itemDroppingEnabled true if players are allowed to drop items
     * @return self for chaining.
     */
    public GameProperties setItemDroppingEnabledPreGame(boolean itemDroppingEnabled) {
        isItemDroppingEnabledPreGame = itemDroppingEnabled;
        return this;
    }

    /**
     * Sets if players are able to drop items from their inventories.
     * @param itemDroppingEnabled true if players are allowed to drop items
     * @return self for chaining.
     */
    public GameProperties setItemDroppingEnabled(boolean itemDroppingEnabled) {
        isItemDroppingEnabled = itemDroppingEnabled;
        return this;
    }

    /**
     * Sets if players are able to pick up items.
     * @param itemPickUpEnabled true if players are allowed to pick up items
     * @return self for chaining.
     */
    public GameProperties setItemPickUpEnabledPreGame(boolean itemPickUpEnabled) {
        isItemPickUpEnabledPreGame = itemPickUpEnabled;
        return this;
    }

    /**
     * Sets if players are able to pick up items.
     * @param itemPickUpEnabled true if players are allowed to pick up items
     * @return self for chaining.
     */
    public GameProperties setItemPickUpEnabled(boolean itemPickUpEnabled) {
        isItemPickUpEnabled = itemPickUpEnabled;
        return this;
    }

    /**
     * Sets if players are able to take fall damage.
     * @param fallDamageEnabledPreGame true if players take fall damage pre-game.
     * @return self for chaining.
     */
    public GameProperties setFallDamageEnabledPreGame(boolean fallDamageEnabledPreGame) {
        isFallDamageEnabledPreGame = fallDamageEnabledPreGame;
        return this;
    }

    /**
     * Sets if players are able to take fall damage.
     * @param fallDamageEnabled true if players take fall damage during the game.
     * @return self for chaining.
     */
    public GameProperties setFallDamageEnabled(boolean fallDamageEnabled) {
        isFallDamageEnabled = fallDamageEnabled;
        return this;
    }

    public GameProperties setTourneyGamemode(boolean tourneyGamemode) {
        isTourneyGamemode = tourneyGamemode;
        return this;
    }

    /**
     * Change if players should get additional rewards on the game ending.
     * (e.g. first place or kills)
     * @return self for chaining
     */
    public GameProperties setInternalRewardsEnabled(boolean internalRewardsEnabled) {
        isInternalRewardsEnabled = internalRewardsEnabled;
        return this;
    }

    /**
     * Replace the current knockback settings for the game.
     * @param customPvpSettings new knockback settings
     * @return self for chaining
     */
    public GameProperties setCustomPvpSettings(CustomGamePVPSettings customPvpSettings) {
        this.customPvpSettings = customPvpSettings;
        return this;
    }

    /**
     * Sets the amount of immunity given (in seconds) after the initial game start.
     * @param spawnImmunitySeconds time in seconds.
     * @return self for chaining.
     */
    public GameProperties setSpawnImmunitySeconds(int spawnImmunitySeconds) {
        this.spawnImmunitySeconds = spawnImmunitySeconds;
        return this;
    }

    /**
     * Determines exactly how a game's end should be processed.
     * @param winPolicy the condition for a game to automatically mark a win.
     * @return self for chaining.
     */
    public GameProperties setWinPolicy(GameHandler.AutomaticWinPolicy winPolicy) {
        if(winPolicy == null) throw new IllegalArgumentException("WinPolicy must be ot null");
        this.winPolicy = winPolicy;
        return this;
    }

    /**
     * Sets the minimum amount of players to operate a game. If the amount of players dips below this
     * before the game starts, the game is abandoned.
     * @param minimumPlayers minimum players for a game to start
     * @return self for chaining.
     */
    public GameProperties setMinimumPlayers(int minimumPlayers) {
        this.minimumPlayers = minimumPlayers;
        return this;
    }
    /**
     * Sets the minimum amount of players for the queue system to start a game. Should be above the minimum players
     * prevent players from often cancelling a game before it starts and trolling.
     * @param guidelinePlayers minimum players for a game to start.
     * @return self for chaining.
     */
    public GameProperties setGuidelinePlayers(int guidelinePlayers) {
        this.guidelinePlayers = guidelinePlayers;
        return this;
    }
    /**
     * Sets the maximum players a game can accept.
     * @param maximumPlayers maximum players a game can handle
     * @return self for chaining.
     */
    public GameProperties setMaximumPlayers(int maximumPlayers) {
        this.maximumPlayers = maximumPlayers;
        return this;
    }

    /**
     * Sets the permissions required to access a game.
     * @param requiredPermissions - Permissions required to join a game.
     * @return self for chaining.
     */
    public GameProperties setRequiredPermissions(String[] requiredPermissions) {
        this.requiredPermissions = requiredPermissions;
        return this;
    }

    /**
     * Sets the icon used in a Form UI for game selection.
     * @param iconPath - The URL to the icon.
     * @return self for chaining.
     */
    public GameProperties setIconPath(String iconPath) {
        this.iconPath = Check.isNull(iconPath) ? "" : iconPath;
        return this;
    }



    public GameProperties copy() {
        return new GameProperties(this);
    }
}
