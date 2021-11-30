package org.madblock.blockswap.behaviours;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockAir;
import cn.nukkit.block.BlockConcrete;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.HandlerList;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemBlock;
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
import org.madblock.blockswap.utils.BlockSwapConstants;
import org.madblock.blockswap.utils.BlockSwapUtility;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.data.Settings;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.game.events.GamePlayerDeathEvent;
import org.madblock.newgamesapi.map.functionalregions.FunctionalRegionCallData;
import org.madblock.newgamesapi.map.functionalregions.FunctionalRegionTag;
import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.newgamesapi.rewards.RewardChunk;
import org.madblock.newgamesapi.team.Team;

import java.util.*;

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
    protected Set<Entity> powerUpEntities = new HashSet<>();
    protected Map<UUID, PowerUp> powerUps = new HashMap<>();

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
        getSessionHandler().getFunctionalRegionManager().setTagFunction("platform_clear", this::clearRegion);
        getSessionHandler().getFunctionalRegionManager().setTagFunction("platform_gen", this::genRegion);
        return 10;
    }

    @Override
    public void onInitialCountdownEnd () {
        this.setPowerUpSpawnDelay(this.getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("powerup_spawn_seconds", BlockSwapConstants.POWERUP_SPAWN_TIMER_SECONDS) * 20);
        this.getSessionHandler().getPrimaryMap().getGameRules().setGameRule(GameRule.FALL_DAMAGE, false);
        this.setRoundTime(
                this.getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("starting_round_length", BlockSwapConstants.ROUND_SECONDS) * 20
        );
        updatePlayersRemainingScoreboard();

        for (Player player : this.getSessionHandler().getPlayers()) {
            this.getSessionHandler().getScoreboardManager().setLine(player, BlockSwapConstants.SCOREBOARD_POWERUP_INDEX, String.format("Power Up: %sNone", TextFormat.GRAY));
        }
    }

    @Override
    public void registerGameSchedulerTasks () {
        this.getSessionHandler().getGameScheduler().registerGameTask(this::gameLoopTask);
        this.getSessionHandler().getGameScheduler().registerGameTask(this::updateXPBarTask, 0, 10);
        this.getSessionHandler().getGameScheduler().registerGameTask(this::spawnPowerUpTask, this.getPowerUpSpawnDelay());
        this.getSessionHandler().getGameScheduler().registerGameTask(this::rotatePowerUpsTask, 2, 2);

        this.listener = new BlockSwapListener(this);
        BlockSwapPlugin.get().getServer().getPluginManager().registerEvents(listener, BlockSwapPlugin.get());
    }

    @Override
    public void cleanUp () {
        HandlerList.unregisterAll(listener); // Unregister this game's listener.
        for (Player player : this.getSessionHandler().getPlayers()) player.setExperience(0, 0); // Should already be called when a player leaves?
    }

    @Override
    public void onAddPlayerToTeam(Player player, Team team) {
        if (getSessionHandler().getGameState().equals(GameHandler.GameState.MAIN_LOOP)) updatePlayersRemainingScoreboard();

    }

    @Override
    public void onPlayerLeaveGame (Player player) {
        player.setExperience(0, 0);
        updatePlayersRemainingScoreboard();
    }

    @Override public void onGameDeathByBlock(GamePlayerDeathEvent event) { commonDeathEvent(event); }
    @Override public void onGameDeathByEntity(GamePlayerDeathEvent event) { commonDeathEvent(event); }
    @Override public void onGameDeathByPlayer(GamePlayerDeathEvent event) { commonDeathEvent(event); }
    @Override public void onGameMiscDeathEvent(GamePlayerDeathEvent event) { commonDeathEvent(event); }
    public void commonDeathEvent(GamePlayerDeathEvent event) {
        getSessionHandler().getScoreboardManager().setLine(event.getDeathCause().getVictim(), BlockSwapConstants.SCOREBOARD_POWERUP_INDEX, null);
        for(Player p: getSessionHandler().getPlayers()) {
            Optional<Team> t = getSessionHandler().getPlayerTeam(p);

            if((p != event.getDeathCause().getVictim()) && t.isPresent() && t.get().isActiveGameTeam()) {
                getSessionHandler().addRewardChunk(p, new RewardChunk("player_survival", "Survival", 8, 4, 2));
            }
        }
    }



    // -- Tasks ----------------------------------

    /**
     * Game task to prepare the next round.
     */
    public void gameLoopTask () {
        this.setRoundTime(Math.max(40, getRoundTime() - 10));

        // Reregister all tasks.
        this.getSessionHandler().getGameScheduler().registerGameTask(this::generateNewPlatformTask);
        this.getSessionHandler().getGameScheduler().registerGameTask(this::removeFloorTask, getRoundTime());
        this.getSessionHandler().getGameScheduler().registerGameTask(this::gameLoopTask, getRoundTime() + 60);
        this.setCompletedRounds(this.getCompletedRounds() + 1);

        // Send level complete sound effect
        for (Player player : getSessionHandler().getPlayers()) {
            getSessionHandler().getPrimaryMap().addSound(new Vector3(player.getX(), player.getY(), player.getZ()), Sound.RANDOM_LEVELUP, 1, 1, player);
        }
    }

    /**
     * Game task to generate a new platform.
     */
    public void generateNewPlatformTask () { // Update the platform first.
        this.currentGenerator = BSwapGeneratorManager.get().getRandomGenerator();
        this.setColors(BlockSwapUtility.getRandomColors(this.getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("total_colours", currentGenerator.getMaxColours())));
        this.setWinningColor(this.getColors().get((int)Math.floor(Math.random() * this.getColors().size())));

        Optional<FunctionalRegionTag> platformRegion = this.getSessionHandler().getFunctionalRegionManager().getRegionFunctionForTag("platform_gen");

        if(platformRegion.isPresent()) {
            Settings newSettings = new Settings();

            //TODO: Add some randomization!
            newSettings.set(ContextKeys.BLOCKSWAP_GAME, this);
            newSettings.set(ContextKeys.AXIS, Axis.X);
            newSettings.set(ContextKeys.NOISE_SCALE_X, 16d).set(ContextKeys.NOISE_SCALE_Y, 16d).set(ContextKeys.NOISE_SCALE_Z, 16d);

            int randomScale = 2 + new Random().nextInt(4);
            newSettings.set(ContextKeys.RANDOM_SCALE_X, randomScale).set(ContextKeys.RANDOM_SCALE_Y, randomScale).set(ContextKeys.RANDOM_SCALE_Z, randomScale);

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
    public void removeFloorTask () {
        Optional<FunctionalRegionTag> platformRegion = this.getSessionHandler().getFunctionalRegionManager().getRegionFunctionForTag("platform_clear");

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
                player.sendMessage(Utility.generateServerMessage("GENERATOR", TextFormat.DARK_PURPLE, "The current generator is: " + currentGenerator.getGeneratorID() + "!"));
                player.sendMessage(Utility.generateServerMessage("COLOUR", TextFormat.DARK_PURPLE, "The next colour is: " + colourText + "!"));

                if(team.isActiveGameTeam()) {
                    updatePlayerInventory(player);
                }
            }
        }
    }

    /**
     * Game task to update the XP bar.
     */
    public void updateXPBarTask () {

        if (this.getRoundTimeLeft() > 0) {
            this.setRoundTimeLeft(getRoundTimeLeft() - 10);
            double countdown = this.getRoundTimeLeft() / 20d;

            for (Player player : this.getSessionHandler().getPlayers()) {

                if (countdown == 0) {
                    player.setExperience(0, 0);

                } else if (countdown % 1 != 0 && this.roundTime < 40) {
                    player.setExperience(Player.calculateRequireExperience((int)(countdown + 0.5)) / 2, (int)(countdown + 1));

                } else {
                    player.setExperience(Player.calculateRequireExperience((int)(countdown + 0.5)), (int)(countdown + 0.5));
                }

                if (this.getRoundTimeLeft() + 10 > 0) {
                    // Send tick noise

                    if (this.getRoundTime() > 40) {
                        // more than 2 seconds. Okay, but is this the second tick or the first tick?
                        if (this.getRoundTimeLeft() % 20 == 0) {
                            this.getSessionHandler().getPrimaryMap().addSound(new Vector3(player.getX(), player.getY(), player.getZ()), Sound.RANDOM_CLICK, 1, 1, player);
                        }
                    } else {
                        if (this.getRoundTimeLeft() % 10 == 0) {
                            // less than or equal to 2 seconds.
                            this.getSessionHandler().getPrimaryMap().addSound(new Vector3(player.getX(), player.getY(), player.getZ()), Sound.RANDOM_CLICK, 1, 1, player);
                        }
                    }


                }
            }
        }
    }

    public void spawnPowerUpTask () {

        if (this.powerUpEntities.size() < BlockSwapConstants.MAXIMUM_POWERUPS_ON_MAP) {
            MapRegion platformRegion = this.getSessionHandler().getPrimaryMapID().getRegions().get("platform");

            float x = (int)Math.floor(Math.random() * (platformRegion.getPosGreater().getX() - platformRegion.getPosLesser().getX() + 1)) + platformRegion.getPosLesser().getX() + 0.5f;
            float z = (int)Math.floor(Math.random() * (platformRegion.getPosGreater().getZ() - platformRegion.getPosLesser().getZ() + 1)) + platformRegion.getPosLesser().getZ() + 0.5f;
            int y = platformRegion.getPosGreater().getY() + 2;

            Position powerUpPosition = new Position(x, y, z, this.getSessionHandler().getPrimaryMap());

            Entity lightning = Entity.createEntity("Lightning", powerUpPosition);
            lightning.spawnToAll();

            Entity powerUpEntity = Entity.createEntity("Chicken", powerUpPosition);
            powerUpEntity.setScale(3);
            this.addPowerUpEntity(powerUpEntity);
            powerUpEntity.spawnToAll();

            for (Player player : this.getSessionHandler().getPlayers()) {
                player.sendMessage(Utility.generateServerMessage("GAME", TextFormat.DARK_AQUA, "A power up has spawned!"));
            }
        }

        this.setPowerUpSpawnDelay(
                Math.max(BlockSwapConstants.MINIMUM_POWERUP_SPAWN_TIMER_SECONDS * 20, this.getPowerUpSpawnDelay() - 20 * BlockSwapConstants.POWERUP_SPAWN_TIMER_SECONDS_DECREMENT)
        );
        this.getSessionHandler().getGameScheduler().registerGameTask(this::spawnPowerUpTask, this.getPowerUpSpawnDelay());

    }

    public void rotatePowerUpsTask () {

        for (Entity entity : this.getPowerUpEntities()) {
            double newYaw = entity.getYaw() + 10 >= 360 ? 0 : entity.getYaw() + 10;
            entity.setRotation(newYaw, entity.getPitch());
        }
    }

    // -- General Methods -----------------------

    protected void updateTimeScoreboard() {
        double time = this.roundTimeLeft / 20d;

        for (Player player : this.getSessionHandler().getPlayers()) {
            this.getSessionHandler().getScoreboardManager().setLine(player, BlockSwapConstants.SCOREBOARD_TIME_INDEX, String.format("%s %ss", Utility.ResourcePackCharacters.TIME, this.roundTime > 40 ? (int)time : time));
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

    protected static String generateBlockID(int x, int y, int z) {
        return String.format("%s|%s|%s;", x, y, z);
    }

    /**
     * Updates a player's inventory with the current colour and their current powerup.
     * @param player the player to have their inventory updated.
     */
    public void updatePlayerInventory(Player player) {
        String colourText = "" + BlockSwapConstants.POSSIBLE_COLORS.getOrDefault(this.getWinningColor(), TextFormat.WHITE) + TextFormat.BOLD + this.getWinningColor().getName();

        Item blockItem = new ItemBlock(new BlockConcrete());
        blockItem.setDamage(this.getWinningColor().getWoolData());
        CompoundTag blockTag = blockItem.hasCompoundTag() ? blockItem.getNamedTag() : new CompoundTag();
        blockTag.putBoolean("generated", true);
        blockItem.setCompoundTag(blockTag);

        Item powerUpItem = null;


        if (this.hasPowerUp(player)) {
            powerUpItem = Item.get(this.getPowerUp(player).getDisplayItemID());
            CompoundTag itemTag = powerUpItem.hasCompoundTag() ? powerUpItem.getNamedTag() : new CompoundTag();
            itemTag.putList(new ListTag<>("ench"));
            itemTag.putString("ability", "power_up");
            itemTag.putBoolean("generated", true);
            powerUpItem.setCompoundTag(itemTag);

            powerUpItem.setCustomName(BlockSwapUtility.getPowerUpItemName(this.getPowerUp(player)));
            blockItem.setCustomName(BlockSwapUtility.getPowerUpItemName(this.getPowerUp(player)));

        } else { blockItem.setCustomName(colourText); }
        PlayerInventory inventory = player.getInventory();

        for (int i = 0; i < 9; i++) {
            Item item = inventory.getItem(i);
            CompoundTag t = item.hasCompoundTag() ? item.getNamedTag() : new CompoundTag();

            if (t != null) {
                // Ensures we aren't modifying a kit item.
                // All inventory items which are replace able should have the
                // tag "generated" equal to true.
                boolean bool = t.getBoolean("generated");
                if ((item.getId() == 0) || bool) { // Replace air or generated items.

                    // Middle 3 slots should be the powerup.
                    if((powerUpItem != null) &&(i < 6) && (i > 2)) {
                        inventory.setItem(i, powerUpItem);

                    } else {
                        inventory.setItem(i, blockItem);
                    }
                }
            }
        }
        inventory.sendContents(player);
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

                Block block = data.getLevel().getBlock(new Vector3(x, y, z));
                if (block.getDamage() != dyeColorId) data.getLevel().setBlock(new Vector3(x, y, z), new BlockAir());
            }
        }
    }

    public void genRegion(FunctionalRegionCallData data) {
        BlockVector3 lowerPos = data.getRegion().getPosLesser();
        BlockVector3 higherPos = data.getRegion().getPosGreater();

        int y = lowerPos.y; // Platform should be flat, thus Y is constant between lowerPos and higherPos.

        for (int x = lowerPos.x; x <= higherPos.x; x++) {
            for (int z = lowerPos.z; z <= higherPos.z; z++) {

                // Erased blocks should not be regenerated.
                if(!erasedBlocks.contains(generateBlockID(x, y, z))) {

                    int colourIndex = currentGenerator.getColourIndex(x, y, z, this.getColors().size());
                    DyeColor colour = colors.get(colourIndex);
                    data.getLevel().setBlock(new Vector3(x, y, z), new BlockConcrete(colour.getWoolData()));
                }
            }
        }
    }



    // -- Getters + Setters ---------------------

    public Set<Entity> getPowerUpEntities () {
        return this.powerUpEntities;
    }

    public boolean isPowerUpEntity(Entity entity) {
        return this.powerUpEntities.contains(entity);
    }

    public void addPowerUpEntity (Entity entity) {
        this.powerUpEntities.add(entity);
    }

    public void removePowerUpEntity (Entity entity) {
        this.powerUpEntities.remove(entity);
    }

    public boolean hasPowerUp (Player player) {
        return this.powerUps.containsKey(player.getUniqueId());
    }

    public PowerUp getPowerUp(Player player) {
        return this.powerUps.get(player.getUniqueId());
    }

    public void setPowerUp(Player player, PowerUp powerUp) {

        if (powerUp == null) {
            this.powerUps.remove(player.getUniqueId());

        } else {
            this.powerUps.put(player.getUniqueId(), powerUp);
        }
    }

    /**
     * Retrieve the amount of rounds that have been completed.
     * @return Rounds that have been completed
     */
    public int getCompletedRounds () {
        return this.completedRounds;
    }

    /**
     * Sets the amount of rounds that have been completed.
     * The higher this is, the lower the winning color will be generated.
     * @param rounds the amount of rounds completed
     */
    public void setCompletedRounds (int rounds) {
        this.completedRounds = rounds;
    }

    /**
     * Retrieve all the colors that are used in platform generation.
     * @return All the colors that are used in the platform
     */
    public List<DyeColor> getColors () {
        return this.colors;
    }

    /**
     * Set the colors that will be used for platform generation.
     * @param colors the palette used during generation
     */
    public void setColors (List<DyeColor> colors) {
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
    public void setWinningColor (DyeColor color) {
        this.winningColor = color;
        TextFormat textColor = BlockSwapConstants.POSSIBLE_COLORS.getOrDefault(this.getWinningColor(), TextFormat.WHITE);

        for (Player player : getSessionHandler().getPlayers()) {
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
    public void setRoundTime (int ticks) {
        this.roundTime = ticks;
        this.roundTimeLeft = ticks;
        updateTimeScoreboard();
    }

    /**
     * Set the amount of time remaining for the round in ticks.
     * @param ticks time in ticks
     */
    public void setRoundTimeLeft (int ticks) {
        this.roundTimeLeft = ticks;
        updateTimeScoreboard();
    }

    /**
     * Get the time left for this round in ticks.
     * @return Amount of ticks left that this is round is running for.
     */
    public int getRoundTimeLeft() {
        return this.roundTimeLeft;
    }

    /**
     * Set the amount of ticks in-between each power up spawning attempt.
     * @param ticks time in ticks
     */
    public void setPowerUpSpawnDelay (int ticks) {
        this.powerUpSpawnDelay = ticks;
    }

    /**
     * @return the amount of ticks in-between each power up spawning attempt.
     */
    public int getPowerUpSpawnDelay () {
        return this.powerUpSpawnDelay;
    }

    /** Returns a list of all the permanently erased blocks of the game. */
    public ArrayList<String> getErasedBlocks() {
        return erasedBlocks;
    }
}