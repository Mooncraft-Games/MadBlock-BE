package org.madblock.crystalwars.game.pointentities.capturepoint;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.level.Level;
import cn.nukkit.level.Sound;
import cn.nukkit.level.particle.DestroyBlockParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.DyeColor;
import cn.nukkit.utils.TextFormat;
import org.madblock.crystalwars.CrystalWarsPlugin;
import org.madblock.crystalwars.util.CrystalWarsUtility;
import org.madblock.newgamesapi.util.Utility;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.pointentities.PointEntityType;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.newgamesapi.team.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class CapturePointEntity extends PointEntityType implements Listener {
    protected static final int CAPTURE_BLOCK_RANGE = 3;

    protected final Map<PointEntity, Optional<Team>> ownershipByEntity = new HashMap<>();
    protected final Map<PointEntity, Integer> progressByEntity = new HashMap<>();

    private final String id;

    public CapturePointEntity(String id, GameHandler gameHandler) {
        super(id, gameHandler);
        this.id = id;
    }

    public Optional<Team> getTeam(PointEntity entity) {
        return ownershipByEntity.get(entity);
    }

    @Override
    public void onRegister() {
        CrystalWarsPlugin.getInstance().getServer().getPluginManager().registerEvents(this, CrystalWarsPlugin.getInstance());
    }

    @Override
    public void onUnregister() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void onAddPointEntity(PointEntity entity) {
        super.onAddPointEntity(entity);
        ownershipByEntity.put(entity, Optional.empty());
        getGameHandler().getGameScheduler().registerGameTask(this::updateCapturePoints, 0, 7);
    }

    @Override
    public void onRemovePointEntity(PointEntity entity) {
        super.onRemovePointEntity(entity);
        ownershipByEntity.remove(entity);
    }

    @EventHandler
    public void onBlockPlaced(BlockPlaceEvent event) {
        for (PointEntity entity : manager.getTypeLookup().get(id)) {
            if (event.getBlock().distance(new Vector3(entity.getX(), event.getBlock().getY(), entity.getZ())) <= CAPTURE_BLOCK_RANGE + 1) {
                event.setCancelled();
                return;
            }
        }
    }

    public abstract String getName();

    protected void updateCapturePoints() {
        for (PointEntity entity : manager.getTypeLookup().get(id)) {

            // Are there players on the point?
            List<Player> nearbyPlayers = gameHandler.getPlayers()
                    .stream()
                    .filter(p -> gameHandler.getPlayerTeam(p).filter(Team::isActiveGameTeam).isPresent())
                    .filter(p -> p.getPosition().distance(new Vector3(entity.getX(), p.getY(), entity.getZ())) < CAPTURE_BLOCK_RANGE)
                    .collect(Collectors.toList());

            Team teamOnPoint = null;
            boolean teamExists = false;
            for (Player player : nearbyPlayers) {

                if (teamOnPoint == null) {
                    teamOnPoint = gameHandler.getPlayerTeam(player).get();
                    teamExists = true;

                } else if (!teamOnPoint.getId().equals(gameHandler.getPlayerTeam(player).get().getId())) {
                    teamOnPoint = null; // There isn't a clear team that's dominant.
                    break;
                }
            }

            if (teamOnPoint == null && !teamExists) {

                // Nobody is contesting for this.
                if (progressByEntity.containsKey(entity)) {
                    updateCapturePointBlocks(entity, Optional.empty());

                    int newProgress = progressByEntity.get(entity) - 1;
                    progressByEntity.put(entity, newProgress);

                    if (newProgress <= -1) progressByEntity.remove(entity);
                }

            } else if (teamOnPoint != null) {

                // We should contest for this point.
                // Who currently owns this capture point?
                Optional<Team> currentTeam = ownershipByEntity.get(entity);
                if (!currentTeam.isPresent() || !currentTeam.get().getId().equals(teamOnPoint.getId())) {

                    // Our team does not own this point!
                    int newProgress = progressByEntity.getOrDefault(entity, -1) + 1;
                    progressByEntity.put(entity, newProgress);
                    updateCapturePointBlocks(entity, Optional.of(teamOnPoint));

                    //for(Player player: gameHandler.getPlayers()) {
                    //    player.getLevel().addSound(entity.positionToVector3(), Sound.FIRE_IGNITE, 0.7f, 0.8f, player);
                    //}


                    if (newProgress >= 11) {
                        progressByEntity.remove(entity);
                        for(Player player: gameHandler.getPlayers()) {
                            player.getLevel().addSound(player, Sound.BEACON_ACTIVATE, 0.6f, 1f, player);
                        }

                        if (currentTeam.isPresent()) {
                            for (Player player : gameHandler.getPlayers()) {
                                player.sendMessage(Utility.generateServerMessage("Game", TextFormat.BLUE, String.format("%s has lost %s Point!", currentTeam.get().getFormattedDisplayName(), getName())));
                            }
                            ownershipByEntity.put(entity, Optional.empty());

                        } else {
                            ownershipByEntity.put(entity, Optional.of(teamOnPoint));
                            for (Player player : gameHandler.getPlayers()) {
                                player.sendMessage(Utility.generateServerMessage("Game", TextFormat.BLUE, String.format("%s has taken control of %s Point!", teamOnPoint.getFormattedDisplayName(), getName())));
                            }
                        }
                    }

                }

            }

        }
    }

    protected void updateCapturePointBlocks(PointEntity entity, Optional<Team> contestingTeam) {
        DyeColor color;
        if (contestingTeam.isPresent()) {
            if (ownershipByEntity.get(entity).isPresent()) {
                color = DyeColor.WHITE;
            } else {
                color = CrystalWarsUtility.resolveTeamColor(contestingTeam.get());
            }
        } else {
            if (ownershipByEntity.get(entity).isPresent()) {
                color = CrystalWarsUtility.resolveTeamColor(ownershipByEntity.get(entity).get());
            } else {
                color = DyeColor.WHITE;
            }
        }

        Vector3 centerPoint = new Vector3((int)entity.getX(), (int)entity.getY(), (int)entity.getZ());
        Level level = manager.getLevelLookup().get(entity);

        // Capture animation
        switch (progressByEntity.get(entity)) {
            case 0:
                // 3
                setBlockColor(centerPoint.add(3, 0, 1), level, color);
                setBlockColor(centerPoint.add(1, 0, 1), level, color);
                setBlockColor(centerPoint.add(-1, 0, 0), level, color);
                break;
            case 1:
                // 3
                setBlockColor(centerPoint.add(3, 0, -1), level, color);
                setBlockColor(centerPoint.add(-2, 0, 1), level, color);
                setBlockColor(centerPoint.add(0, 0, 2), level, color);
                break;
            case 2:
                // 3
                setBlockColor(centerPoint.add(2, 0, 0), level, color);
                setBlockColor(centerPoint.add(-2, 0, -2), level, color);
                setBlockColor(centerPoint.add(-1, 0, 2), level, color);
                break;
            case 3:
                // 3
                setBlockColor(centerPoint.add(1, 0, -1), level, color);
                setBlockColor(centerPoint.add(-1, 0, 1), level, color);
                setBlockColor(centerPoint.add(2, 0, 2), level, color);
                break;
            case 4:
                // 4
                setBlockColor(centerPoint.add(0, 0, 3), level, color);
                setBlockColor(centerPoint.add(-1, 0, -1), level, color);
                setBlockColor(centerPoint.add(0, 0, -2), level, color);
                setBlockColor(centerPoint.add(-3, 0, -1), level, color);
                break;
            case 5:
                // 3
                setBlockColor(centerPoint.add(1, 0, 3), level, color);
                setBlockColor(centerPoint.add(2, 0, -1), level, color);
                setBlockColor(centerPoint.add(-1, 0, -3), level, color);
                break;
            case 6:
                // 3
                setBlockColor(centerPoint.add(-2, 0, 2), level, color);
                setBlockColor(centerPoint.add(-3, 0, 0), level, color);
                setBlockColor(centerPoint.add(1, 0, 0), level, color);
                break;
            case 7:
                // 3
                setBlockColor(centerPoint.add(1, 0, -3), level, color);
                setBlockColor(centerPoint.add(-1, 0, -2), level, color);
                setBlockColor(centerPoint.add(2, 0, 1), level, color);
                break;
            case 8:
                // 3
                setBlockColor(centerPoint.add(2, 0, -2), level, color);
                setBlockColor(centerPoint.add(-1, 0, 3), level, color);
                setBlockColor(centerPoint.add(-2, 0, 0), level, color);
                break;
            case 9:
                // 3
                setBlockColor(centerPoint.add(0, 0, -1), level, color);
                setBlockColor(centerPoint.add(1, 0, 2), level, color);
                setBlockColor(centerPoint.add(0, 0, -3), level, color);
                break;
            case 10:
                // 3
                setBlockColor(centerPoint.add(3, 0, 0), level, color);
                setBlockColor(centerPoint.add(0, 0, 1), level, color);
                setBlockColor(centerPoint.add(-2, 0, -1), level, color);
                break;
            case 11:
                setBlockColor(centerPoint, level, color);
                setBlockColor(centerPoint.add(1, 0, -2), level, color);
                setBlockColor(centerPoint.add(-3, 0, 1), level, color);
                break;
        }
    }

    private void setBlockColor(Vector3 position, Level level, DyeColor color) {
        Block glassBlock = level.getBlock(position);
        glassBlock.setDamage(color.getWoolData());
        level.setBlock(position, glassBlock);

        Block woolBlock = level.getBlock(position.add(0, -1, 0));
        if (woolBlock.getId() == Block.WOOL) {
            woolBlock.setDamage(color.getWoolData());
            level.setBlock(position.add(0, -1, 0), woolBlock);
            level.addParticle(new DestroyBlockParticle(position.add(0.5, 0.5, 0.5), woolBlock), gameHandler.getPlayers());
        }
    }
}