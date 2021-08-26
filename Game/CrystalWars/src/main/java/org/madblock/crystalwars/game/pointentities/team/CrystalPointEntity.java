package org.madblock.crystalwars.game.pointentities.team;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.level.Location;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.TextFormat;
import org.madblock.crystalwars.CrystalWarsPlugin;
import org.madblock.crystalwars.game.entities.EntityHumanCrystal;
import org.madblock.crystalwars.util.CrystalWarsUtility;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.pointentities.PointEntityCallData;
import org.madblock.newgamesapi.map.pointentities.PointEntityType;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.newgamesapi.team.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CrystalPointEntity extends PointEntityType implements Listener {
    public static final String ID = "madblock_crystalwars_crystal";

    public static final String TEAM_ID_PROPERTY = "team";

    protected final Map<Team, PointEntity> teamPointEntities = new HashMap<>();
    protected final Map<Vector3, Integer> crystalHealth = new HashMap<>();
    protected final Map<Vector3, EntityHumanCrystal> crystals = new HashMap<>();

    protected final Map<Team, Long> lastCrystalAttackNotification = new HashMap<>();

    public CrystalPointEntity(GameHandler gameHandler) {
        super(ID, gameHandler);
    }

    @Override
    public void onRegister() {
        addFunction("spawn_crystal", this::spawnCrystalFunction);
        CrystalWarsPlugin.getInstance().getServer().getPluginManager().registerEvents(this, CrystalWarsPlugin.getInstance());
        getGameHandler().getGameScheduler().registerGameTask(this::updateActionBar, 0, 20);
        getGameHandler().getGameScheduler().registerGameTask(this::preventCrystalCamping, 0, 20);
    }

    @Override
    public void onUnregister() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void onAddPointEntity(PointEntity entity) {
        super.onAddPointEntity(entity);
        executeFunction("spawn_crystal", entity, manager.getLevelLookup().get(entity), new HashMap<>());
    }

    @Override
    public void onRemovePointEntity(PointEntity entity) {
        super.onRemovePointEntity(entity);
        teamPointEntities.remove(gameHandler.getTeams().get(entity.getStringProperties().get(TEAM_ID_PROPERTY)));
        crystalHealth.remove(new Vector3(entity.getX(), entity.getY(), entity.getZ()));
        crystals.remove(new Vector3(entity.getX(), entity.getY(), entity.getZ()));
        lastCrystalAttackNotification.clear();
    }

    public boolean isCrystalDestroyed(PointEntity entity) {
        if (!entity.getType().equals(ID)) {
            return false;
        }
        Entity theEntity = null;
        for (Entity worldEntity : gameHandler.getPrimaryMap().getEntities()) {
            if (!(worldEntity instanceof EntityHumanCrystal))
                continue;
            if (worldEntity.getLocation().x == entity.getX() && worldEntity.getLocation().y == entity.getY() &&
                    worldEntity.getLocation().z == entity.getZ()) {
                theEntity = worldEntity;
                break;
            }
        }
        return theEntity == null;
    }

    protected void spawnCrystalFunction(PointEntityCallData data) {
        if (!data.getPointEntity().getStringProperties().containsKey(TEAM_ID_PROPERTY)) {
            CrystalWarsPlugin.getInstance().getLogger().error(String.format("Missing %s property for the MapID of %s (PointEntity: %s)", TEAM_ID_PROPERTY, gameHandler.getPrimaryMapID().getId(), data.getPointEntity().getId()));
            return;
        }
        if (!gameHandler.getTeams().containsKey(data.getPointEntity().getStringProperties().get(TEAM_ID_PROPERTY))) {
            CrystalWarsPlugin.getInstance().getLogger().error(String.format("Invalid %s property for the MapID of %s (PointEntity: %s)", TEAM_ID_PROPERTY, gameHandler.getPrimaryMapID().getId(), data.getPointEntity().getId()));
            return;
        }

        Team team = gameHandler.getTeams().get(data.getPointEntity().getStringProperties().get(TEAM_ID_PROPERTY));
        if (!team.isActiveGameTeam()) {
            CrystalWarsPlugin.getInstance().getLogger().error(String.format("Invalid %s property for the MapID of %s (PointEntity: %s)", TEAM_ID_PROPERTY, gameHandler.getPrimaryMapID().getId(), data.getPointEntity().getId()));
            return;
        }

        if (team.getPlayers().size() == 0)
            return;

        PointEntity entity = data.getPointEntity();

        Vector3 position = new Vector3(entity.getX(), entity.getY(), entity.getZ());
        Location loc = new Location(entity.getX(), entity.getY(), entity.getZ(), entity.getYaw(), 0, gameHandler.getPrimaryMap());

        EntityHumanCrystal endCrystal = EntityHumanCrystal.getNewCrystal(loc, "purple");
        endCrystal.spawnToAll();

        teamPointEntities.put(team, entity);
        crystals.put(position, endCrystal);
        crystalHealth.put(position, 100);
    }

    @EventHandler
    private void onCrystalDamage(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        if (victim instanceof EntityHumanCrystal) {
            event.setCancelled();
            Entity damager = event.getDamager();
            if (damager instanceof Player)
                updateCrystal(((Player) damager), victim.getLocation());
        }
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!gameHandler.getPlayers().contains(player))
            return;
        if (!gameHandler.getPlayerTeam(player).isPresent())
            return;
        Team team = gameHandler.getPlayerTeam(player).get();
        PointEntity entity = teamPointEntities.get(team);
        Vector3 position = new Vector3(entity.getX(), entity.getY(), entity.getZ());
        if (team.getPlayers().size() > 1 || crystals.get(position) == null)
            return;
        crystals.get(position).kill();
        crystals.get(position).despawnFromAll();
        crystals.remove(position);
        crystalHealth.remove(position);
        teamPointEntities.remove(team);
        for (Player gamePlayer : gameHandler.getPlayers()) {
            gamePlayer.sendMessage(Utility.generateServerMessage("Game", TextFormat.BLUE, String.format("%s's crystal has " +
                    "been destroyed. All players of that team have left.", team.getFormattedDisplayName())));
        }

    }

    protected void updateCrystal(Player player, Location location) {
        Optional<Team> playerTeam = gameHandler.getPlayerTeam(player);
        if (!playerTeam.isPresent() || !playerTeam.get().isActiveGameTeam()) {
            return;
        }

        PointEntity targetCrystalPointEntity = null;
        for (Team team : teamPointEntities.keySet()) {
            PointEntity entity = teamPointEntities.get(team);
            if (entity.getX() == (double) location.getFloorX() + 0.5 && entity.getY() == location.getFloorY() &&
                    entity.getZ() == (double) location.getFloorZ() + 0.5) {
                targetCrystalPointEntity = entity;
                if (entity.getStringProperties().get(TEAM_ID_PROPERTY).equals(playerTeam.get().getId())) {
                    player.sendMessage(Utility.generateServerMessage("Game", TextFormat.BLUE, "You cannot destroy " +
                            "your own crystal!", TextFormat.RED));
                    return;
                }
                break;
            }
        }

        if (targetCrystalPointEntity == null) {
            return;
        }

        Vector3 position = new Vector3(location.getX(), location.getY(), location.getZ());
        if (crystals.containsKey(position)) {
            Integer health = crystalHealth.get(position);
            crystalHealth.put(position, health - 1);
            player.getLevel().addSound(player.getPosition(), Sound.HIT_CHAIN, 0.25f, 1);
            Team victimTeam = gameHandler.getTeams().get(targetCrystalPointEntity.getStringProperties().get(TEAM_ID_PROPERTY));
            if (System.currentTimeMillis() - lastCrystalAttackNotification.getOrDefault(victimTeam, 0L) >= 10000L) {
                lastCrystalAttackNotification.put(victimTeam, System.currentTimeMillis());
                for (Player victimPlayer : victimTeam.getPlayers())
                    victimPlayer.sendTitle("", TextFormat.RED + "Your crystal is under attack!", 0, 80, 0);
            }
            if (health - 1 <= 0) {
                crystals.get(position).kill();
                crystals.get(position).despawnFromAll();
                crystals.remove(position);
                crystalHealth.remove(position);
                for (Player gamePlayer : gameHandler.getPlayers()) {
                    gamePlayer.sendMessage(Utility.generateServerMessage("Game", TextFormat.BLUE, String.format("%s's " +
                            "crystal was destroyed by " + playerTeam.get().getColour().getColourString() + player.getName() +
                            TextFormat.WHITE + "! They will no longer respawn!", victimTeam.getFormattedDisplayName())));
                }
                for (Player victimPlayer : victimTeam.getPlayers()) {
                    victimPlayer.sendTitle(TextFormat.RED + "CRYSTAL DESTROYED", TextFormat.RED +
                            "You will no longer respawn!", 20, 60, 20);
                    victimPlayer.getLevel().addSound(victimPlayer.getPosition(), Sound.MOB_ENDERDRAGON_GROWL, 0.25f, 1);
                }
            }
        }
    }

    protected void preventCrystalCamping() {
        for (Player player : gameHandler.getPlayers()) {
            for (Map.Entry<Vector3, EntityHumanCrystal> entry : crystals.entrySet()) {
                if (player.distance(entry.getKey()) < 0.5) {
                    player.sendMessage(Utility.generateServerMessage("Game", TextFormat.BLUE, "Don't block the crystal!", TextFormat.RED));
                    player.attack(1f);
                }
            }
        }
    }

    protected void updateActionBar() {
        StringBuilder textToDisplay = new StringBuilder();
        for (Team team : gameHandler.getTeams().values()) {
            if (!team.isActiveGameTeam() || teamPointEntities.get(team) == null)
                continue;
            boolean crystalExists = crystalExistsForTeam(team);
            int playerCount = team.getPlayers().size();
            if (crystalExists || playerCount > 0) {
                textToDisplay.append(String.format("%s %s[%s] ", CrystalWarsUtility.generateCrystalTeamIcon(team,
                        crystalExistsForTeam(team)),
                        team.getColour().getColourString(), (getTeamCrystalHealth(team) > 0 ? getTeamCrystalHealth(team) +
                                Utility.ResourcePackCharacters.HEART_FULL : team.getPlayers().size() +
                                Utility.ResourcePackCharacters.MORE_PEOPLE)));
            }
        }

        for (Player player : gameHandler.getPlayers()) {
            player.sendActionBar(textToDisplay.toString().trim(), 0, 1, 0);
        }
    }

    protected boolean crystalExistsForTeam(Team team) {
        return !isCrystalDestroyed(teamPointEntities.get(team));
    }

    protected int getTeamCrystalHealth(Team team) {
        PointEntity entity = teamPointEntities.get(team);
        return crystalHealth.getOrDefault(new Vector3(entity.getX(), entity.getY(), entity.getZ()), 0);
    }
}