package org.madblock.blockswap.behaviours;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockAir;
import cn.nukkit.block.BlockWool;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.HandlerList;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemBlock;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.Sound;
import cn.nukkit.math.BlockVector3;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.utils.DyeColor;
import cn.nukkit.utils.TextFormat;
import org.madblock.blockswap.BlockSwapPlugin;
import org.madblock.blockswap.listeners.GameEventListeners;
import org.madblock.blockswap.powerups.PowerUp;
import org.madblock.blockswap.utils.BlockSwapConstants;
import org.madblock.blockswap.utils.BlockSwapUtility;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.game.events.GamePlayerDeathEvent;
import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.newgamesapi.team.Team;

import java.util.*;

public class BlockSwapGameBehaviour extends GameBehavior {

    /** colors that are used for platform generation */
    protected List<DyeColor> colors;

    /** color that players must run to */
    protected DyeColor winningColor;

    /** original amount of round ticks */
    protected int roundTime;

    /** amount of ticks left in this round */
    protected int roundTimeLeft;

    /** Amount of ticks in-between each power up spawn attempt */
    protected int powerUpSpawnDelay;

    /** rounds completed */
    protected int completedRounds = 0;

    protected Set<Entity> powerUpEntities = new HashSet<>();

    protected Map<UUID, PowerUp> powerUps = new HashMap<>();

    private final GameEventListeners listener = new GameEventListeners(this);

    @Override
    public Team.TeamBuilder[] getTeams() {
        Team.TeamBuilder[] teams = new Team.TeamBuilder[]{
                new Team.TeamBuilder("players", "Players", Team.Colour.LIGHT_GRAY).setCanPlayersDropItems(false)
        };
        return teams;
    }

    @Override
    public void onInitialCountdownEnd() {
        this.setPowerUpSpawnDelay(this.getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("powerup_spawn_seconds", BlockSwapConstants.POWERUP_SPAWN_TIMER_SECONDS) * 20);
        this.getSessionHandler().getPrimaryMap().getGameRules().setGameRule(GameRule.FALL_DAMAGE, false);
        this.setRoundTime(
                this.getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("starting_round_length", BlockSwapConstants.ROUND_SECONDS) * 20
        );
        updatePlayersRemainingScoreboard();
        for (Player player : this.getSessionHandler().getPlayers()) {
            this.getSessionHandler().getScoreboardManager().setLine(player, BlockSwapConstants.SCOREBOARD_POWERUP_INDEX, String.format("Power Up: %sNone", TextFormat.GRAY));
        }
        Server.getInstance().getPluginManager().registerEvents(this.listener, BlockSwapPlugin.getInstance());
    }

    @Override
    public void registerGameSchedulerTasks() {
        this.getSessionHandler().getGameScheduler().registerGameTask(this::gameLoopTask);
        this.getSessionHandler().getGameScheduler().registerGameTask(this::updateXPBarTask, 0, 10);
        this.getSessionHandler().getGameScheduler().registerGameTask(this::spawnPowerUpTask, this.getPowerUpSpawnDelay());
        this.getSessionHandler().getGameScheduler().registerGameTask(this::rotatePowerUpsTask, 2, 2);
    }

    @Override
    public void cleanUp() {
        for (Player player : this.getSessionHandler().getPlayers()) {
            player.setExperience(0, 0);
        }
        HandlerList.unregisterAll(this.listener);
    }

    @Override
    public void onAddPlayerToTeam(Player player, Team team) {
        if (getSessionHandler().getGameState().equals(GameHandler.GameState.MAIN_LOOP)) {
            updatePlayersRemainingScoreboard();
        }
    }

    @Override
    public void onPlayerLeaveGame(Player player) {
        player.setExperience(0, 0);
        updatePlayersRemainingScoreboard();
    }

    @Override
    public void onGameDeathByBlock(GamePlayerDeathEvent event) {
        getSessionHandler().getScoreboardManager().setLine(event.getDeathCause().getVictim(), BlockSwapConstants.SCOREBOARD_POWERUP_INDEX, null);
    }

    @Override
    public void onGameDeathByEntity(GamePlayerDeathEvent event) {
        getSessionHandler().getScoreboardManager().setLine(event.getDeathCause().getVictim(), BlockSwapConstants.SCOREBOARD_POWERUP_INDEX, null);
    }

    @Override
    public void onGameDeathByPlayer(GamePlayerDeathEvent event) {
        getSessionHandler().getScoreboardManager().setLine(event.getDeathCause().getVictim(), BlockSwapConstants.SCOREBOARD_POWERUP_INDEX, null);
    }

    @Override
    public void onGameMiscDeathEvent(GamePlayerDeathEvent event) {
        getSessionHandler().getScoreboardManager().setLine(event.getDeathCause().getVictim(), BlockSwapConstants.SCOREBOARD_POWERUP_INDEX, null);
    }

    /**
     * Game task to prepare the next round.
     */
    public void gameLoopTask() {
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
    public void generateNewPlatformTask() {

        // Update the platform first.

        this.setColors(BlockSwapUtility.getRandomColors(this.getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("total_colours", BlockSwapConstants.COLORS_TO_BE_USED)));
        this.setWinningColor(this.getColors().get((int)Math.floor(Math.random() * this.getColors().size())));

        Level level = this.getSessionHandler().getPrimaryMap();
        MapRegion platformRegion = this.getSessionHandler().getPrimaryMapID().getRegions().get("platform");

        BlockVector3 lowerPos = platformRegion.getPosLesser();
        BlockVector3 higherPos = platformRegion.getPosGreater();

        int y = lowerPos.y; // Y is constant between lowerPos and higherPos.

        int minimumScale = Math.max(1, this.getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("colour_scale_min", BlockSwapConstants.MINIMUM_COLOR_SCALE));
        int maximumScale = Math.max(minimumScale, Math.min(30, this.getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("colour_scale_max", BlockSwapConstants.MAXIMUM_COLOR_SCALE)));

        //If both are the same, use the minimum. Else, generate a random scale
        int scale = minimumScale == maximumScale ? minimumScale : minimumScale + new Random().nextInt(maximumScale - minimumScale);

        int totalTiles = ((higherPos.x - lowerPos.x) / scale) * ((higherPos.z - lowerPos.z) / scale);
        int minCorrectTiles = this.getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("min_correct_tiles", 1);
        int maxRangePerForcedTile = totalTiles / minCorrectTiles;

        int correctTiles = 0;
        int forcedTileIndex = 0;
        int nextForcedTileCheck = (int)Math.floor(Math.random() * (maxRangePerForcedTile + 1));
        for (int x = lowerPos.x; x <= higherPos.x; x += scale) {
            for (int z = lowerPos.z; z <= higherPos.z; z += scale) {
                BlockWool woolBlock = new BlockWool();
                DyeColor randomColor = BlockSwapUtility.getRandomColor(this.getColors(), this.getWinningColor(), this.getCompletedRounds());

                // Ensures that we have the minimum amount of platforms.
                if (correctTiles < minCorrectTiles) {
                    if (randomColor.getWoolData() == this.getWinningColor().getWoolData()) {
                        correctTiles++;
                    } else if (forcedTileIndex == nextForcedTileCheck) {
                        randomColor = this.getWinningColor();
                        forcedTileIndex = 0;
                        nextForcedTileCheck = (int)Math.floor(Math.random() * (maxRangePerForcedTile + 1));
                    } else {
                        forcedTileIndex++;
                    }
                }

                woolBlock.setDamage(randomColor.getWoolData());
                for(int bx = x; bx < x+scale; bx++){
                    for(int bz = z; bz < z+scale; bz++) {
                        level.setBlock(new Vector3(bx, y, bz), woolBlock, false, false);
                    }
                }
            }
        }

        // Update player inventories.
        this.getSessionHandler().getGameScheduler().registerGameTask(this::updateInventoryTask);
    }

    /**
     * Game task to remove all wool block that are not the winning colour.
     */
    public void removeFloorTask() {
        Level level = this.getSessionHandler().getPrimaryMap();
        MapRegion platformRegion = this.getSessionHandler().getPrimaryMapID().getRegions().get("platform");

        BlockVector3 lowerPos = platformRegion.getPosLesser();
        BlockVector3 higherPos = platformRegion.getPosGreater();

        int y = lowerPos.y; // Y is constant between lowerPos and higherPos.

        int dyeColorId = this.getWinningColor().getWoolData();

        for (int x = lowerPos.x; x <= higherPos.x; x++) {
            for (int z = lowerPos.z; z <= higherPos.z; z++) {
                Block block = level.getBlock(new Vector3(x, y, z));
                if (block.getDamage() != dyeColorId) {
                    level.setBlock(new Vector3(x, y, z), new BlockAir());
                }
            }
        }
        for(Player player: this.getSessionHandler().getPlayers()){
            this.getSessionHandler().getPrimaryMap().addSound(player.getPosition(), Sound.MOB_ENDERDRAGON_FLAP, 0.8f, 0.9f, player);
        }
    }

    /**
     * Game task to update player inventories to the next wool colour.
     */
    public void updateInventoryTask() {
        for(Team team : this.getSessionHandler().getTeams().values()) {
            for (Player player : team.getPlayers()) {
                String colourText = "" + BlockSwapConstants.POSSIBLE_COLORS.getOrDefault(this.getWinningColor(), TextFormat.WHITE) + TextFormat.BOLD + this.getWinningColor().getName();
                player.sendTitle(colourText, TextFormat.GOLD+"- Run to the colour! -", 4 , 16, 4);
                player.sendMessage(Utility.generateServerMessage("COLOUR", TextFormat.DARK_PURPLE, "The next colour is: "+colourText+"!"));
                if(team.isActiveGameTeam()) {
                    Item block = new ItemBlock(new BlockWool());
                    block.setDamage(this.getWinningColor().getWoolData());

                    if (this.hasPowerUp(player)) {
                        block.setCustomName(BlockSwapUtility.getPowerUpItemName(this.getPowerUp(player)));
                        CompoundTag tag = block.getNamedTag();
                        tag.putList(new ListTag<>("ench"));
                        tag.putString("ability", "power_up");
                        block.setCompoundTag(tag);
                    }

                    PlayerInventory inventory = player.getInventory();
                    for (int i = 0; i < 9; i++) {
                        int itemId = inventory.getItem(i).getId();
                        if (itemId == Block.WOOL || itemId == Block.AIR) {
                            // Ensures we aren't modifying a kit item.
                            inventory.setItem(i, block);
                        }
                    }
                    inventory.sendContents(player);
                }
            }
        }
    }

    /**
     * Game task to update the XP bar.
     */
    public void updateXPBarTask() {
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

    public void spawnPowerUpTask() {

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

    public void rotatePowerUpsTask() {
        for (Entity entity : this.getPowerUpEntities()) {
            double newYaw = entity.getYaw() + 10 >= 360 ? 0 : entity.getYaw() + 10;
            entity.setRotation(newYaw, entity.getPitch());
        }
    }

    public Set<Entity> getPowerUpEntities() {
        return this.powerUpEntities;
    }

    public boolean isPowerUpEntity(Entity entity) {
        return this.powerUpEntities.contains(entity);
    }

    public void addPowerUpEntity(Entity entity) {
        this.powerUpEntities.add(entity);
    }

    public void removePowerUpEntity(Entity entity) {
        this.powerUpEntities.remove(entity);
    }

    public boolean hasPowerUp(Player player) {
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
     * @return Rounds that have been completed.
     */
    public int getCompletedRounds() {
        return this.completedRounds;
    }

    /**
     * Sets the amount of rounds that have been completed.
     * The higher this is, the lower the winning color will be generated.
     * @param rounds
     */
    public void setCompletedRounds(int rounds) {
        this.completedRounds = rounds;
    }

    /**
     * Retrieve all the colors that are used in platform generation.
     * @return All the colors that are used in the platform.
     */
    public List<DyeColor> getColors() {
        return this.colors;
    }

    /**
     * Set the colors that will be used for platform generation.
     * @param colors
     */
    public void setColors(List<DyeColor> colors) {
        this.colors = colors;
    }

    /**
     * Retrieve the color that will not be destroyed.
     * @return The color that will not be destroyed.
     */
    public DyeColor getWinningColor() {
        return this.winningColor;
    }

    /**
     * Set the color that will not be destroyed.
     * @param color
     */
    public void setWinningColor(DyeColor color) {
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
     * @param ticks
     */
    public void setRoundTime(int ticks) {
        this.roundTime = ticks;
        this.roundTimeLeft = ticks;
        updateTimeScoreboard();
    }

    /**
     * Set the amount of time remaining for the round in ticks.
     * @param ticks
     */
    public void setRoundTimeLeft(int ticks) {
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
     * @param ticks
     */
    public void setPowerUpSpawnDelay(int ticks) {
        this.powerUpSpawnDelay = ticks;
    }

    /**
     * Get the amount of ticks in-between each power up spawning attempt.
     * @return
     */
    public int getPowerUpSpawnDelay() {
        return this.powerUpSpawnDelay;
    }

    protected void updateTimeScoreboard() {
        double time = this.roundTimeLeft / 20d;
        for (Player player : this.getSessionHandler().getPlayers()) {
            this.getSessionHandler().getScoreboardManager().setLine(player, BlockSwapConstants.SCOREBOARD_TIME_INDEX, String.format("%s %ss", Utility.ResourcePackCharacters.TIME, this.roundTime > 40 ? (int)time : time));
        }
    }

    protected void updatePlayersRemainingScoreboard() {
        int activePlayers = (int)this.getSessionHandler().getPlayers().stream().filter(player -> this.getSessionHandler().getPlayerTeam(player).filter(Team::isActiveGameTeam).isPresent()).count();
        for (Player player : this.getSessionHandler().getPlayers()) {
            this.getSessionHandler().getScoreboardManager().setLine(player, BlockSwapConstants.SCOREBOARD_PLAYERS_INDEX, String.format("%s %d", Utility.ResourcePackCharacters.MORE_PEOPLE, activePlayers));
        }
    }

}
