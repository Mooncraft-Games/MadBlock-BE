package org.madblock.blockswap.behaviours;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockConcrete;
import cn.nukkit.block.BlockID;
import cn.nukkit.event.HandlerList;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.Position;
import cn.nukkit.level.Sound;
import cn.nukkit.math.BlockVector3;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.utils.DyeColor;
import cn.nukkit.utils.TextFormat;
import org.madblock.blockswap.BlockSwapPlugin;
import org.madblock.blockswap.generator.BSwapGenerator;
import org.madblock.blockswap.generator.BSwapGeneratorManager;
import org.madblock.blockswap.generator.util.Axis;
import org.madblock.blockswap.generator.util.ContextKeys;
import org.madblock.blockswap.listeners.BlockSwapListener;
import org.madblock.blockswap.powerups.PowerUp;
import org.madblock.blockswap.powerups.PowerUpManager;
import org.madblock.blockswap.utils.BlockSwapConstants;
import org.madblock.blockswap.utils.BlockSwapUtility;
import org.madblock.lib.commons.data.store.settings.ControlledSettings;
import org.madblock.newgamesapi.util.Utility;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.game.events.GamePlayerDeathEvent;
import org.madblock.newgamesapi.map.functionalregions.FunctionalRegionCallData;
import org.madblock.newgamesapi.map.functionalregions.FunctionalRegionTag;
import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.newgamesapi.rewards.RewardChunk;
import org.madblock.newgamesapi.team.Team;

import java.util.*;
import java.util.stream.Collectors;

// Significant Updates to how it works:
// - Generation occurs in the generator manager
// - Bswap platform now uses 2 tags. platform_clear & platform_gen
// - Multiple platforms can be used so generation is no-longer assumed to be one square.
// - Wool -> Concrete for bolder colours

public class BlockSwapGameBehaviour extends GameBehavior {

    protected BSwapGenerator currentGenerator; // The rounds current generator.
    protected DyeColor winningColor; // Colour that players must run to.

    protected int roundTime; // Original amount of round ticks.
    protected int roundTimeLeft; // Amount of ticks left in this round.
    protected int powerUpSpawnDelay; // Amount of ticks in-between each power up spawn attempt.
    protected int completedRounds = 0; // Rounds completed.

    protected List<DyeColor> colors; // Colours that are used for platform generation.

    protected PowerUpManager powerUpManager;
    protected BlockSwapListener listener;

    protected ArrayList<String> erasedBlocks = new ArrayList<>(); // Permanently erased blocks!



    // -- API Methods ----------------------------

    @Override
    public Team.TeamBuilder[] getTeams() {
        return new Team.TeamBuilder[]{
                new Team.TeamBuilder("players", "Players", Team.Colour.LIGHT_GRAY).setCanPlayersDropItems(false)
        };
    }

    @Override
    public int onGameBegin() {
        this.getSessionHandler().getFunctionalRegionManager().setTagFunction(BlockSwapConstants.FUNCTION_TAG_CLEAR_PLATFORM, this::clearRegion);
        this.getSessionHandler().getFunctionalRegionManager().setTagFunction(BlockSwapConstants.FUNCTION_TAG_GENERATE_PLATFORM, this::genRegion);
        return 10;
    }

    @Override
    public void onInitialCountdownEnd() {
        this.getSessionHandler().getPrimaryMap().getGameRules().setGameRule(GameRule.FALL_DAMAGE, false);
        this.setRoundTime(
                this.getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("starting_round_length", BlockSwapConstants.ROUND_SECONDS) * 20
        );
        this.updatePlayersRemainingScoreboard();
    }

    @Override
    public void registerGameSchedulerTasks() {
        this.powerUpSpawnDelay = this.getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("powerup_spawn_seconds", BlockSwapConstants.POWERUP_SPAWN_TIMER_SECONDS) * 20;
        this.powerUpManager = new PowerUpManager(this);
        this.listener = new BlockSwapListener(this);

        this.getSessionHandler().getGameScheduler().registerGameTask(this::gameLoopTask);
        this.getSessionHandler().getGameScheduler().registerGameTask(this::spawnPowerUpTask, this.powerUpSpawnDelay);

        BlockSwapPlugin.get().getServer().getPluginManager().registerEvents(this.listener, BlockSwapPlugin.get());
    }

    @Override
    public void cleanUp() {
        HandlerList.unregisterAll(this.listener); // Unregister this game's listener.
        for (Player player : this.getSessionHandler().getPlayers()) player.setExperience(0, 0); // Should already be called when a player leaves?
    }

    @Override
    public void onAddPlayerToTeam(Player player, Team team) {
        if (this.getSessionHandler().getGameState().equals(GameHandler.GameState.MAIN_LOOP)) {
            this.updatePlayersRemainingScoreboard();
        }

    }

    @Override
    public void onPlayerLeaveGame(Player player) {
        player.setExperience(0, 0);
        this.updatePlayersRemainingScoreboard();
    }

    @Override public void onGameDeathByBlock(GamePlayerDeathEvent event) { this.commonDeathEvent(event); }
    @Override public void onGameDeathByEntity(GamePlayerDeathEvent event) { this.commonDeathEvent(event); }
    @Override public void onGameDeathByPlayer(GamePlayerDeathEvent event) { this.commonDeathEvent(event); }
    @Override public void onGameMiscDeathEvent(GamePlayerDeathEvent event) { this.commonDeathEvent(event); }
    public void commonDeathEvent(GamePlayerDeathEvent event) {
        for(Player p: this.getSessionHandler().getPlayers()) {
            Optional<Team> t = this.getSessionHandler().getPlayerTeam(p);

            if((p != event.getDeathCause().getVictim()) && t.isPresent() && t.get().isActiveGameTeam()) {
                this.getSessionHandler().addRewardChunk(p, new RewardChunk("player_survival", "Survival", 8, 4, 2));
            }
        }
    }



    // -- Tasks ----------------------------------

    /**
     * Game task to prepare the next round.
     */
    public void gameLoopTask() {
        if (this.getCompletedRounds() % 2 == 0 && this.getCompletedRounds() > 0) {
            this.setRoundTime(Math.max(40, this.getRoundTime() - 10));
        }
        this.setRoundTimeLeft(this.getRoundTime());
        this.setCompletedRounds(0);

        // Reregister all tasks.
        this.getSessionHandler().getGameScheduler().registerGameTask(this::generateNewPlatformTask);
        this.getSessionHandler().getGameScheduler().registerGameTask(this::removeFloorTask, getRoundTime());
        this.getSessionHandler().getGameScheduler().registerGameTask(() -> {
            this.setCompletedRounds(this.getCompletedRounds() + 1);
            this.gameLoopTask();
        }, getRoundTime() + 60);
        this.getSessionHandler().getGameScheduler().registerSelfCancellableGameTask(task -> {
            this.decreaseTimeTask();
            if (this.getRoundTimeLeft() == 0) {
                task.cancel();
            }
        }, 10, 10);

        // Send level complete sound effect
        for (Player player : getSessionHandler().getPlayers()) {
            this.getSessionHandler().getPrimaryMap().addSound(new Vector3(player.getX(), player.getY(), player.getZ()), Sound.RANDOM_LEVELUP, 1, 1, player);
            this.getSessionHandler().getPrimaryMap().addSound(new Vector3(player.getX(), player.getY(), player.getZ()), Sound.RANDOM_CLICK, 1, 1, player);
        }
    }

    /**
     * Game task to generate a new platform.
     */
    public void generateNewPlatformTask() { // Update the platform first.
        this.currentGenerator = BSwapGeneratorManager.get().getRandomGenerator();
        this.setColors(BlockSwapUtility.getRandomColors(this.getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("total_colours", currentGenerator.getMaxColours())));
        this.setWinningColor(this.getColors().get((int)Math.floor(Math.random() * this.getColors().size())));

        Optional<FunctionalRegionTag> platformRegion = this.getSessionHandler().getFunctionalRegionManager().getRegionFunctionForTag(BlockSwapConstants.FUNCTION_TAG_GENERATE_PLATFORM);

        if(platformRegion.isPresent()) {
            ControlledSettings newSettings = new ControlledSettings();
            Random random = new Random();

            //TODO: Add some randomization!
            newSettings.set(ContextKeys.BLOCKSWAP_GAME, this);

            if(this.currentGenerator.getGeneratorID().equalsIgnoreCase("blockswap:striped")) {
                newSettings.set(ContextKeys.AXIS, random.nextInt(2) == 0 ? Axis.X : Axis.Z);
                int randomScale = 1 + new Random().nextInt(2);
                newSettings.set(ContextKeys.RANDOM_SCALE_X, randomScale).set(ContextKeys.RANDOM_SCALE_Y, randomScale).set(ContextKeys.RANDOM_SCALE_Z, randomScale);

            } else {
                newSettings.set(ContextKeys.AXIS, Axis.X);
                newSettings.set(ContextKeys.NOISE_SCALE_X, 16d).set(ContextKeys.NOISE_SCALE_Y, 16d).set(ContextKeys.NOISE_SCALE_Z, 16d);

                int randomScale = 2 + new Random().nextInt(4);
                newSettings.set(ContextKeys.RANDOM_SCALE_X, randomScale).set(ContextKeys.RANDOM_SCALE_Y, randomScale).set(ContextKeys.RANDOM_SCALE_Z, randomScale);
            }

            this.currentGenerator.setContext(newSettings); // Claim generator
            platformRegion.get().runFunction();
            this.currentGenerator.resetContext(); // Reset it for other games to use.

        } else { BlockSwapPlugin.get().getLogger().info("No MapRegion function for platform_clear!"); }

        for(Player p: getSessionHandler().getPlayers()) {
            Optional<Team> t = getSessionHandler().getPlayerTeam(p);

            if(t.isPresent() && t.get().isActiveGameTeam()) {
                getSessionHandler().addRewardChunk(p, new RewardChunk("round_survival", "Round Survival", 10, 3, 1));
            }
        }

        // Update player inventories.
        this.getSessionHandler().getGameScheduler().registerGameTask(this::updateInventoryTask);
    }

    /**
     * Game task to remove all wool block that are not the winning colour.
     */
    public void removeFloorTask() {
        Optional<FunctionalRegionTag> platformRegion = this.getSessionHandler().getFunctionalRegionManager().getRegionFunctionForTag(BlockSwapConstants.FUNCTION_TAG_CLEAR_PLATFORM);

        if(platformRegion.isPresent()) {
            platformRegion.get().runFunction();

        } else { BlockSwapPlugin.get().getLogger().info("No MapRegion function for platform_clear!"); }

        for(Player player: this.getSessionHandler().getPlayers()){
            this.getSessionHandler().getPrimaryMap().addSound(player.getPosition(), Sound.MOB_ENDERDRAGON_FLAP, 0.8f, 0.9f, player);
        }
    }

    /**
     * Game task to update player inventories to the next wool colour.
     */
    public void updateInventoryTask() {
        String colourText = "" + BlockSwapConstants.POSSIBLE_COLORS.getOrDefault(this.getWinningColor(), TextFormat.WHITE) + TextFormat.BOLD + this.getWinningColor().getName();

        for(Team team : this.getSessionHandler().getTeams().values()) {

            for (Player player : team.getPlayers()) {
                player.sendTitle(colourText, TextFormat.GOLD + "- Run to the colour! -", 4, 16, 4);

                if(team.isActiveGameTeam()) {
                    this.updatePlayerInventory(player);
                }
            }
        }
    }

    /**
     * Game task to decrease the time every half second.
     */
    public void decreaseTimeTask() {
        if (this.getRoundTimeLeft() > 0) {
            this.setRoundTimeLeft(this.getRoundTimeLeft() - 10);

            boolean sendTickNoise = this.getRoundTimeLeft() % 10 == 0 && this.getRoundTimeLeft() > 0;
            if (sendTickNoise) {
                for (Player player : this.getSessionHandler().getPlayers()) {
                    this.getSessionHandler().getPrimaryMap().addSound(new Vector3(player.getX(), player.getY(), player.getZ()), Sound.RANDOM_CLICK, 1, 1, player);
                }
            }
        }
    }

    public void spawnPowerUpTask() {
        if (this.getPowerUpManager().getEntities().size() < BlockSwapConstants.MAXIMUM_POWERUPS_ON_MAP) {
            List<MapRegion> possibleRegions = this.getSessionHandler()
                    .getPrimaryMapID()
                    .getRegions()
                    .values().stream()
                    .filter(region -> Arrays.asList(region.getTags()).contains(BlockSwapConstants.FUNCTION_TAG_GENERATE_PLATFORM))
                    .collect(Collectors.toList());
            MapRegion platformRegion = possibleRegions.get((int) Math.floor(Math.random() * possibleRegions.size()));

            float x = (int)Math.floor(Math.random() * (platformRegion.getPosGreater().getX() - platformRegion.getPosLesser().getX() + 1)) + platformRegion.getPosLesser().getX() + 0.5f;
            float z = (int)Math.floor(Math.random() * (platformRegion.getPosGreater().getZ() - platformRegion.getPosLesser().getZ() + 1)) + platformRegion.getPosLesser().getZ() + 0.5f;
            int y = platformRegion.getPosGreater().getY() + 2;

            Position powerUpPosition = new Position(x, y, z, this.getSessionHandler().getPrimaryMap());
            this.powerUpManager.spawnAt(powerUpPosition);

            for (Player player : this.getSessionHandler().getPlayers()) {
                player.sendTip("A power up has spawned!");
            }
        }

        this.powerUpSpawnDelay = Math.max(BlockSwapConstants.MINIMUM_POWERUP_SPAWN_TIMER_SECONDS * 20, this.powerUpSpawnDelay - 20 * BlockSwapConstants.POWERUP_SPAWN_TIMER_SECONDS_DECREMENT);
        this.getSessionHandler().getGameScheduler().registerGameTask(this::spawnPowerUpTask, this.powerUpSpawnDelay);
    }

    // -- General Methods -----------------------

    protected void updateTimeDisplay() {
        double time = this.roundTimeLeft / 20d;

        for (Player player : this.getSessionHandler().getPlayers()) {
            player.sendActionBar(String.format("%s %ss", Utility.ResourcePackCharacters.TIME, time));
        }
    }

    protected void updatePlayersRemainingScoreboard() {
        int activePlayers = (int) this.getSessionHandler().getPlayers()
                .stream().filter(player ->
                        this.getSessionHandler().getPlayerTeam(player).filter(Team::isActiveGameTeam).isPresent()).count();

        for (Player player : this.getSessionHandler().getPlayers()) {
            this.getSessionHandler().getScoreboardManager().setLine(player, BlockSwapConstants.SCOREBOARD_PLAYERS_INDEX, String.format("%s %d", Utility.ResourcePackCharacters.MORE_PEOPLE, activePlayers));
        }
    }

    protected void updateCurrentRoundScoreboard() {
        for (Player player : this.getSessionHandler().getPlayers()) {
            this.getSessionHandler().getScoreboardManager().setLine(player, BlockSwapConstants.SCOREBOARD_ROUND_INDEX, String.format("%s %d", Utility.ResourcePackCharacters.TROPHY, completedRounds + 1));
        }
    }



    protected static String generateBlockID(int x, int y, int z) {
        return String.format("%s|%s|%s;", x, y, z);
    }

    /**
     * Updates a player's inventory with the current colour and their current powerup.
     * @param player the player to have their inventory updated.
     */
    public void updatePlayerInventory(Player player) {
        Item blockItem = Block.get(BlockID.CONCRETE, this.getWinningColor().getWoolData()).toItem();
        blockItem.setCustomName(BlockSwapUtility.getBlockItemName(this.getWinningColor()));
        CompoundTag blockTag = blockItem.hasCompoundTag() ? blockItem.getNamedTag() : new CompoundTag();
        blockTag.putBoolean("volatile", true);
        blockItem.setCompoundTag(blockTag);

        this.updatePowerUpSlot(0, player, 3);
        this.updatePowerUpSlot(1, player, 4);
        this.updatePowerUpSlot(2, player, 5);

        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < 9; i++) {
            Item item = inventory.getItem(i);
            CompoundTag t = item.hasCompoundTag() ? item.getNamedTag() : new CompoundTag();

            if (t != null) {
                // Ensures we aren't modifying a kit item.
                // All inventory items which are replace able should have the
                // tag "volatile" equal to true.
                boolean isVolatileItem = t.getBoolean("volatile");
                if ((item.getId() == 0) || isVolatileItem) { // Replace air or generated items.
                    inventory.setItem(i, blockItem);
                }
            }
        }
        inventory.sendContents(player);
    }

    private void updatePowerUpSlot(int slot, Player player, int targetInventorySlot) {
        if (this.getPowerUpManager().getPowerUp(player, slot).isPresent()) {
            PowerUp powerUp = this.getPowerUpManager().getPowerUp(player, slot).get();

            Item powerUpItem = Item.get(powerUp.getDisplayItemID());
            powerUpItem.setCustomName(BlockSwapUtility.getPowerUpItemName(powerUp));
            CompoundTag itemTag = powerUpItem.hasCompoundTag() ? powerUpItem.getNamedTag() : new CompoundTag();
            itemTag.putList(new ListTag<>("ench"));
            itemTag.putString("ability", "power_up");
            powerUpItem.setCompoundTag(itemTag);

            player.getInventory().setItem(targetInventorySlot, powerUpItem);
        } else {
            Item noPowerUpItem = Item.get(Item.DYE, this.getWinningColor().getDyeData());
            noPowerUpItem.setCustomName(BlockSwapUtility.getBlockItemName(this.getWinningColor()));

            player.getInventory().setItem(targetInventorySlot, noPowerUpItem);
        }
    }

    // -- Region Func's --------------------------
    // In NGAPI 2.0, give these their own class and use parameters to set the safe colour.

    public void clearRegion(FunctionalRegionCallData data) {
        BlockVector3 lowerPos = data.getRegion().getPosLesser();
        BlockVector3 higherPos = data.getRegion().getPosGreater();

        int y = lowerPos.y; // Platform should be flat, thus Y is constant between lowerPos and higherPos.
        int dyeColorId = this.getWinningColor().getWoolData();

        for (int x = lowerPos.x; x <= higherPos.x; x++) {
            for (int z = lowerPos.z; z <= higherPos.z; z++) {
                Vector3 blockCoordinate = new Vector3(x, y, z);

                Block block = data.getLevel().getBlock(blockCoordinate);
                if (block.getDamage() != dyeColorId) data.getLevel().setBlock(new Vector3(x, y, z), Block.get(BlockID.AIR));
            }
        }
    }

    public void genRegion(FunctionalRegionCallData data) {
        BlockVector3 lowerPos = data.getRegion().getPosLesser();
        BlockVector3 higherPos = data.getRegion().getPosGreater();

        int y = lowerPos.y; // Platform should be flat, thus Y is constant between lowerPos and higherPos.
        for (int x = lowerPos.x; x <= higherPos.x; x++) {
            for (int z = lowerPos.z; z <= higherPos.z; z++) {
                Vector3 blockCoordinate = new Vector3(x, y, z);

                // Erased blocks should not be regenerated.
                if(!this.erasedBlocks.contains(generateBlockID(x, y, z))) {

                    int colourIndex = this.currentGenerator.getColourIndex(x, y, z, this.getColors().size());
                    DyeColor colour = this.colors.get(colourIndex);
                    data.getLevel().setBlock(blockCoordinate, new BlockConcrete(colour.getWoolData()));
                }
            }
        }
    }



    // -- Getters + Setters ---------------------

    public PowerUpManager getPowerUpManager() {
        return this.powerUpManager;
    }

    /**
     * Retrieve the amount of rounds that have been completed.
     * @return Rounds that have been completed
     */
    public int getCompletedRounds() {
        return this.completedRounds;
    }

    /**
     * Sets the amount of rounds that have been completed.
     * The higher this is, the lower the winning color will be generated.
     * @param rounds the amount of rounds completed
     */
    public void setCompletedRounds(int rounds) {
        this.completedRounds = rounds;
        this.updateCurrentRoundScoreboard();
    }

    /**
     * Retrieve all the colors that are used in platform generation.
     * @return All the colors that are used in the platform
     */
    public List<DyeColor> getColors() {
        return this.colors;
    }

    /**
     * Set the colors that will be used for platform generation.
     * @param colors the palette used during generation
     */
    public void setColors(List<DyeColor> colors) {
        this.colors = colors;
    }

    /**
     * Retrieve the color that will not be destroyed.
     * @return The color that will not be destroyed
     */
    public DyeColor getWinningColor() {
        return this.winningColor;
    }

    /**
     * Set the color that will not be destroyed.
     * @param color the winning colour (safe)
     */
    public void setWinningColor(DyeColor color) {
        this.winningColor = color;
        TextFormat textColor = BlockSwapConstants.POSSIBLE_COLORS.getOrDefault(this.getWinningColor(), TextFormat.WHITE);

        for (Player player : this.getSessionHandler().getPlayers()) {
            this.getSessionHandler().getScoreboardManager().setLine(player, BlockSwapConstants.SCOREBOARD_COLOR_INDEX, String.format("%s%s", textColor.toString(), color.getName()));
        }
    }

    /**
     * Get the amount of time that this round is supposed to run for in ticks.
     * @return Amount of ticks this round is supposed to run for
     */
    public int getRoundTime() {
        return this.roundTime;
    }

    /**
     * Set the round time and time remaining in ticks.
     * @param ticks time in ticks
     */
    public void setRoundTime(int ticks) {
        this.roundTime = ticks;
        this.roundTimeLeft = ticks;
        this.updateTimeDisplay();
    }

    /**
     * Set the amount of time remaining for the round in ticks.
     * @param ticks time in ticks
     */
    public void setRoundTimeLeft(int ticks) {
        this.roundTimeLeft = ticks;
        this.updateTimeDisplay();
    }

    /**
     * Get the time left for this round in ticks.
     * @return Amount of ticks left that this is round is running for.
     */
    public int getRoundTimeLeft() {
        return this.roundTimeLeft;
    }

    /** Returns a list of all the permanently erased blocks of the game. */
    public ArrayList<String> getErasedBlocks() {
        return erasedBlocks;
    }
}