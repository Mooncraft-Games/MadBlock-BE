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
import org.madblock.crystalwars.CrystalWarsConstants;
import org.madblock.crystalwars.CrystalWarsPlugin;
import org.madblock.crystalwars.game.CrystalWarsGame;
import org.madblock.crystalwars.game.entities.EntityHumanCrystal;
import org.madblock.crystalwars.util.CrystalWarsUtility;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.pointentities.PointEntityCallData;
import org.madblock.newgamesapi.map.pointentities.PointEntityType;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.newgamesapi.rewards.RewardChunk;
import org.madblock.newgamesapi.team.Team;
import org.madblock.newgamesapi.util.HealthbarUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CrystalPointEntity extends PointEntityType implements Listener {

    public static final String ID = "madblock_crystalwars_crystal";

    public static final String TEAM_ID_PROPERTY = "team";

    protected final Map<Team, ArrayList<PointEntity>> teamPointEntities = new HashMap<>();
    protected final Map<String, Integer> crystalHealth = new HashMap<>();
    protected final Map<String, EntityHumanCrystal> crystals = new HashMap<>();
    protected final Map<String, Vector3> crystalOrigins = new HashMap<>();

    protected final Map<Team, Long> lastCrystalAttackNotification = new HashMap<>();

    protected ArrayList<Player> onCooldown = new ArrayList<>();
    protected int maxHealth;
    protected int crystal_cooldownTicks;


    public CrystalPointEntity(GameHandler gameHandler) {
        super(ID, gameHandler);
    }



    @Override
    public void onRegister() {
        addFunction("spawn_crystal", this::spawnCrystalFunction);

        CrystalWarsPlugin.getInstance().getServer().getPluginManager().registerEvents(this, CrystalWarsPlugin.getInstance());
        getGameHandler().getGameScheduler().registerGameTask(this::updateActionBar, 0, 20);
        getGameHandler().getGameScheduler().registerGameTask(this::preventCrystalCamping, 0, 20);

        this.maxHealth = Math.max(1, getGameHandler().getPrimaryMapID().getIntegers().getOrDefault("crystal_health", 40));
        this.crystal_cooldownTicks = Math.max(0, getGameHandler().getPrimaryMapID().getIntegers().getOrDefault("c_cooldown_ticks", 10));
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

        ArrayList<PointEntity> tpes = teamPointEntities.get(gameHandler.getTeams().get(entity.getStringProperties().get(TEAM_ID_PROPERTY)));
        if(tpes != null) tpes.remove(entity);

        crystalHealth.remove(entity.getId());
        crystals.remove(entity.getId());
        lastCrystalAttackNotification.clear();
    }


    public int getTeamMaxCrystalCount(Team team) {
        ArrayList<PointEntity> crystalList = teamPointEntities.get(team);
        return crystalList == null ? 0 : crystalList.size();
    }

    public int getTeamAliveCrystalCount(Team team) {
        ArrayList<PointEntity> crystalList = teamPointEntities.get(team);
        if(crystalList == null) return 0;

        ArrayList<String> teamCrystalIDs = new ArrayList<>();
        for(PointEntity e: crystalList) teamCrystalIDs.add(e.getId());
        int count = 0;

        for(Entity entity: gameHandler.getPrimaryMap().getEntities()) {
            if(entity instanceof EntityHumanCrystal) {
                String crystalType = entity.namedTag.getString(CrystalWarsConstants.NBT_CRYSTAL_TYPE);
                String crystalID = entity.namedTag.getString(CrystalWarsConstants.NBT_CRYSTAL_ID);

                // Use the constant first in-case it's null. Just checking that the victim crystal is definitely working.
                if(CrystalWarsConstants.TYPE_TEAM.equalsIgnoreCase(crystalType) && (crystalID != null)) {
                    if(teamCrystalIDs.contains(crystalID)) count++;
                }
            }
        }

        return count;
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

        if (team.getPlayers().size() == 0) return;

        PointEntity entity = data.getPointEntity();
        String id = entity.getId();
        Location loc = new Location(entity.getX(), entity.getY(), entity.getZ(), entity.getYaw(), 0, gameHandler.getPrimaryMap());


        EntityHumanCrystal endCrystal = EntityHumanCrystal.getNewCrystal(loc, "purple");
        endCrystal.namedTag.putString(CrystalWarsConstants.NBT_CRYSTAL_TYPE, CrystalWarsConstants.TYPE_TEAM);
        endCrystal.namedTag.putString(CrystalWarsConstants.NBT_CRYSTAL_ID, id);
        endCrystal.setNameTagAlwaysVisible(true);
        endCrystal.setNameTagVisible(true);
        endCrystal.setNameTag(HealthbarUtility.getHealthText(HealthbarUtility.HealthbarType.BAR_DUO, maxHealth, maxHealth));
        endCrystal.setScale(0.9f);
        endCrystal.spawnToAll();

        if(!teamPointEntities.containsKey(team)) {
            teamPointEntities.put(team, new ArrayList<>());
        }

        teamPointEntities.get(team).add(entity);
        crystals.put(id, endCrystal);
        crystalHealth.put(id, maxHealth);
        crystalOrigins.put(id, entity.positionToVector3());
    }

    @EventHandler
    private void onCrystalDamage(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();

        if (victim instanceof EntityHumanCrystal) {
            event.setCancelled();
            Entity damager = event.getDamager();

            if (damager instanceof Player) {
                Player player = (Player) damager;
                EntityHumanCrystal crystal = (EntityHumanCrystal) victim;

                if(onCooldown.contains(player)) { return;
                } else {
                    onCooldown.add(player);
                    getGameHandler().getGameScheduler().registerGameTask(() -> {
                        onCooldown.remove(player);
                    }, crystal_cooldownTicks);
                }

                updateCrystal(player, crystal);
            }
        }
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (!gameHandler.getPlayers().contains(player)) return;
        if (!gameHandler.getPlayerTeam(player).isPresent()) return;

        Team team = gameHandler.getPlayerTeam(player).get();
        ArrayList<PointEntity> entities = teamPointEntities.get(team);

        if (team.getPlayers().size() > 1) return;

        if(entities != null) {
            boolean deadTeamAnnounce = true;
            for (PointEntity entity : entities) {
                String id = entity.getId();

                if (crystals.get(id) != null) {
                    crystals.get(id).kill();
                    crystals.get(id).despawnFromAll();
                    crystals.remove(id);
                    crystalHealth.remove(id);
                    teamPointEntities.remove(team);


                    // A crystal doesn't exist for the team so it never existed anyway.
                } else deadTeamAnnounce = false;

            }

            if (deadTeamAnnounce) {
                for (Player gamePlayer : gameHandler.getPlayers()) {
                    gamePlayer.sendMessage(Utility.generateServerMessage("Game", TextFormat.BLUE, String.format("%s's crystal has " +
                            "been destroyed. All players of that team have left.", team.getFormattedDisplayName())));
                }
            }
        }

    }

    protected void updateCrystal(Player player, EntityHumanCrystal victimCrystal) {
        Optional<Team> playerTeam = gameHandler.getPlayerTeam(player);
        if (!playerTeam.isPresent() || !playerTeam.get().isActiveGameTeam()) {
            return;
        }

        String crystalType = victimCrystal.namedTag.getString(CrystalWarsConstants.NBT_CRYSTAL_TYPE);
        String crystalID = victimCrystal.namedTag.getString(CrystalWarsConstants.NBT_CRYSTAL_ID);

        // Use the constant first in-case it's null. Just checking that the victim crystal is definitely working.
        if(CrystalWarsConstants.TYPE_TEAM.equalsIgnoreCase(crystalType) && (crystalID != null)) {
            PointEntity targetCrystalPointEntity = null;

            for (Team team : teamPointEntities.keySet()) {
                ArrayList<PointEntity> entities = teamPointEntities.get(team);

                for (PointEntity entity : entities) {
                    if (entity.getId().equals(crystalID)) {
                        targetCrystalPointEntity = entity;

                        if (entity.getStringProperties().get(TEAM_ID_PROPERTY).equals(playerTeam.get().getId())) {
                            player.sendMessage(Utility.generateServerMessage("Game", TextFormat.BLUE, "You cannot destroy " +
                                    "your own crystal!", TextFormat.RED));
                            return;
                        }
                        break;
                    }
                }
            }

            if (targetCrystalPointEntity == null) return;



            if (crystals.containsKey(crystalID)) {
                EntityHumanCrystal entity = crystals.get(crystalID);
                Integer health = crystalHealth.get(crystalID);
                crystalHealth.put(crystalID, health - 1);
                player.getLevel().addSound(player.getPosition(), Sound.HIT_CHAIN, 0.6f, 1);
                Team victimTeam = gameHandler.getTeams().get(targetCrystalPointEntity.getStringProperties().get(TEAM_ID_PROPERTY));

                entity.setNameTag(
                        HealthbarUtility.getHealthText(HealthbarUtility.HealthbarType.BAR_DUO, health - 1, maxHealth)
                );

                if (System.currentTimeMillis() - lastCrystalAttackNotification.getOrDefault(victimTeam, 0L) >= 10000L) {
                    lastCrystalAttackNotification.put(victimTeam, System.currentTimeMillis());

                    for (Player victimPlayer : victimTeam.getPlayers()) {
                        victimPlayer.sendTitle("", TextFormat.RED + "Your crystal is under attack!", 0, 80, 0);
                    }
                }

                if (health - 1 <= 0) {
                    entity.kill();
                    entity.despawnFromAll();
                    crystals.remove(crystalID);
                    crystalHealth.remove(crystalID);
                    getGameHandler().addRewardChunk(player, new RewardChunk("destroy_crystal", "Crystal Kill", 10, 4, 3));

                    if(getTeamAliveCrystalCount(victimTeam) > 0) {
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
        }
    }

    protected void preventCrystalCamping() {
        for (Player player : gameHandler.getPlayers()) {
            for (Map.Entry<String, Vector3> entry : crystalOrigins.entrySet()) {

                if (player.distance(entry.getValue()) < 0.5) {
                    player.sendMessage(Utility.generateServerMessage("Game", TextFormat.BLUE, "Don't block the crystal!", TextFormat.RED));
                    player.attack(1f);
                }
            }
        }
    }

    protected void updateActionBar() {
        StringBuilder textToDisplay = new StringBuilder();

        for (Team team : gameHandler.getTeams().values()) {
            if (!team.isActiveGameTeam() || teamPointEntities.get(team) == null) continue;

            boolean crystalExists = doCrystalsExistForTeam(team);
            int playerCount = team.getPlayers().size();

            if (crystalExists || playerCount > 0) {

                // TODO: Add a crystal character. Then maps with multiple crystals can
                //       show which ones are destroyed.
                //int count = getTeamAliveCrystalCount(team);
                //int countMax = getTeamMaxCrystalCount(team);

                textToDisplay.append(String.format("%s %s[%s] ",
                        CrystalWarsUtility.generateCrystalTeamIcon(team, doCrystalsExistForTeam(team)),
                        team.getColour().getColourString(),
                        (getTeamTotalHealth(team) > 0 ? getTeamTotalHealth(team) +
                                Utility.ResourcePackCharacters.HEART_FULL : team.getPlayers().size() +
                                Utility.ResourcePackCharacters.MORE_PEOPLE)));
            }
        }

        for (Player player : gameHandler.getPlayers()) {
            player.sendActionBar(textToDisplay.toString().trim(), 0, 1, 0);
        }
    }

    public boolean doCrystalsExistForTeam(Team team) {
        return getTeamAliveCrystalCount(team) > 0;
    }

    public int getTeamTotalHealth(Team team) {
        ArrayList<PointEntity> entities = teamPointEntities.get(team);
        int health = 0;

        for(PointEntity entity: entities) {
            String id = entity.getId();
            if(crystalHealth.containsKey(id)) {
                health += crystalHealth.get(id);
            }
        }

        return health;
    }

    public int healTeamCrystals(Team team, int health) {
        int pool = health;

        for(PointEntity entity: teamPointEntities.get(team)) {
            String id = entity.getId();

            if(crystalHealth.containsKey(id)) {
                int cryH = crystalHealth.get(id);
                int healDelta = Math.min(pool, maxHealth - cryH);

                pool -= healDelta;
                int newHealth = cryH + healDelta;
                crystalHealth.put(id, newHealth);

                EntityHumanCrystal c = crystals.get(id);
                if(c != null) {
                    c.setNameTag(
                            HealthbarUtility.getHealthText(HealthbarUtility.HealthbarType.BAR_DUO, health - 1, maxHealth)
                    );
                }

                if(pool == 0) break;
            }
        }

        return pool;
    }
}