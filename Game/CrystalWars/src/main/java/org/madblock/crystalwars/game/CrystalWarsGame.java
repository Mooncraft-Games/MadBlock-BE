package org.madblock.crystalwars.game;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.block.BlockTNT;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.item.EntityPrimedTNT;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityExplodeEvent;
import cn.nukkit.event.inventory.InventoryMoveItemEvent;
import cn.nukkit.level.Location;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import cn.nukkit.potion.Effect;
import cn.nukkit.potion.Potion;
import cn.nukkit.utils.TextFormat;
import org.madblock.crystalwars.CrystalWarsConstants;
import org.madblock.crystalwars.game.entities.EntityHumanCrystal;
import org.madblock.crystalwars.game.pointentities.capturepoint.GoldCapturePointEntity;
import org.madblock.crystalwars.game.pointentities.capturepoint.MiddleCapturePointEntity;
import org.madblock.crystalwars.game.pointentities.shop.types.IronShopPointEntity;
import org.madblock.crystalwars.game.pointentities.shop.types.GoldShopPointEntity;
import org.madblock.crystalwars.game.pointentities.shop.types.DiamondShopPointEntity;
import org.madblock.crystalwars.game.pointentities.team.CrystalPointEntity;
import org.madblock.crystalwars.game.pointentities.team.GeneratorPointEntity;
import org.madblock.crystalwars.game.upgrades.CrystalTeamUpgrade;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.game.events.GamePlayerDeathEvent;
import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.newgamesapi.team.SpectatingTeam;
import org.madblock.newgamesapi.team.Team;
import org.madblock.newgamesapi.team.TeamPresets;
import org.madblock.newgamesapi.util.RawTextBuilder;

import java.util.*;

public class CrystalWarsGame extends GameBehavior {

    protected Set<Vector3> placedBlocks = new HashSet<>();
    protected Map<Team, Set<CrystalTeamUpgrade>> upgrades = new HashMap<>();

    protected ArrayList<MapRegion> repairRegions = new ArrayList<>();
    protected HashMap<Player, EntityHumanCrystal> carriedCrystals = new HashMap<>();
    protected ArrayList<Player> onCooldown = new ArrayList<>();
    protected Random random;


    // Try not to modify the following during a game. They're only here to
    // provide map-configurable data and aren't designed to accomodate for updates.

    protected int repairCrystal_startDelay;
    protected int repairCrystal_minDelay;
    protected int repairCrystal_maxDelay;

    protected int repairCrystal_minHeal;
    protected int repairCrystal_maxHeal;

    protected int repairCrystal_holdTime;

    protected int crystal_cooldownTicks;

    @Override
    public int onGameBegin() {
        this.random = new Random();

        // default: 80 ticks
        this.repairCrystal_startDelay = Math.max(10, getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("c_repair_time_start", 20 * 150));

        // default: 30 ticks
        this.repairCrystal_minDelay = Math.max(10, getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("c_repair_time_min", 20 * 80));

        //default: 60 ticks
        this.repairCrystal_maxDelay = Math.max(10, getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("c_repair_time_max", 20 * 120));

        this.repairCrystal_minHeal = Math.max(2, getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("c_repair_heal_min", 5));

        this.repairCrystal_maxHeal = Math.max(2, getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("c_repair_heal_max", 18));

        //default: 20 seconds
        this.repairCrystal_holdTime = Math.max(0, getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("c_repair_hold_time", 30));

        this.crystal_cooldownTicks = Math.max(0, getSessionHandler().getPrimaryMapID().getIntegers().getOrDefault("c_cooldown_ticks", 4));

        return 5;
    }

    @Override
    public void onInitialCountdownEnd() {
        getSessionHandler().getPointEntityTypeManager().registerPointEntityType(new CrystalPointEntity(getSessionHandler()));
        getSessionHandler().getPointEntityTypeManager().registerPointEntityType(new GeneratorPointEntity(this));

        getSessionHandler().getPointEntityTypeManager().registerPointEntityType(new IronShopPointEntity(this));
        getSessionHandler().getPointEntityTypeManager().registerPointEntityType(new GoldShopPointEntity(this));
        getSessionHandler().getPointEntityTypeManager().registerPointEntityType(new DiamondShopPointEntity(this));

        getSessionHandler().getPointEntityTypeManager().registerPointEntityType(new GoldCapturePointEntity(getSessionHandler()));
        getSessionHandler().getPointEntityTypeManager().registerPointEntityType(new MiddleCapturePointEntity(getSessionHandler()));

        for (Player player : getSessionHandler().getPlayers()) {
            if (!getSessionHandler().getPlayerTeam(player).isPresent())
                return;
            Team team = getSessionHandler().getPlayerTeam(player).get();
            getSessionHandler().getScoreboardManager().setLine(player, 0, String.format("%s",
                    team.getFormattedDisplayName()));
        }

        // I've given up with MapRegions as they're so utterly useless
        // in their current state. Rewrite inbound!
        // This just gathers all the current regions which spawn crystals so that a looping timer can spawn
        // repair crystals.
        for(MapRegion region: getSessionHandler().getPrimaryMapID().getRegions().values()) {
            for(String str: region.getTags()) {
                if(str.equalsIgnoreCase("repair_crystal_region")) {
                    repairRegions.add(region);
                    break;
                }
            }
        }
    }

    @Override
    public void registerGameSchedulerTasks() {
        getSessionHandler().getGameScheduler().registerGameTask(this::spawnNewRepairCrystal, repairCrystal_startDelay);
        getSessionHandler().getGameScheduler().registerGameTask(this::alignCrystals, 1, 1);
        getSessionHandler().getGameScheduler().registerGameTask(this::countdownCrystals, 20, 20);
    }

    @Override
    public void onPlayerLeaveGame(Player player) {
        player.removeAllEffects();
        handleCrystalDrop(player);
        checkWin();
    }

    @Override
    public void onRemovePlayerFromTeam(Player player, Team team) {
        checkWin();
    }

    @Override
    public void onGameDeathByBlock(GamePlayerDeathEvent event) {
        handleGameDeath(event);
    }

    @Override
    public void onGameMiscDeathEvent(GamePlayerDeathEvent event) {
        handleGameDeath(event);
    }

    @Override
    public void onGameDeathByEntity(GamePlayerDeathEvent event) {
        handleGameDeath(event);
    }

    @Override
    public void onGameDeathByPlayer(GamePlayerDeathEvent event) {
        handleGameDeath(event);
    }

    public void addUpgradeForTeam(Team team, CrystalTeamUpgrade upgrade) {
        upgrades.computeIfAbsent(team, (key) -> new HashSet<>());
        upgrades.get(team).add(upgrade);
    }

    public boolean doesTeamHaveUpgrade(Team team, CrystalTeamUpgrade upgrade) {
        return upgrades.containsKey(team) && upgrades.get(team).contains(upgrade);
    }

    @EventHandler
    private void onPlace(BlockPlaceEvent event) {
        if (getSessionHandler().getPlayers().contains(event.getPlayer())) {
            if (event.getBlock() instanceof BlockTNT) {
                Entity primedTntEntity = Entity.createEntity(EntityPrimedTNT.NETWORK_ID, event.getBlock().add(0.5, 0, 0.5));
                primedTntEntity.setMotion(new Vector3(0, 0.3, 0));
                primedTntEntity.spawnToAll();

                event.getPlayer().getInventory().removeItem(new BlockTNT().toItem());
                event.setCancelled(true);
            } else {
                placedBlocks.add(event.getBlock().getLocation());
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (getSessionHandler().getPlayers().contains(event.getPlayer())) {
            if (!placedBlocks.contains(event.getBlock().getLocation())) {
                event.setCancelled(true);
            } else {
                placedBlocks.remove(event.getBlock().getLocation());
                if (event.getPlayer().getInventory().getItemInHand().isSword()) {
                    event.setCancelled(false);
                }
            }
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (getSessionHandler().getPrimaryMap() == event.getEntity().getLevel()) {
            Iterator<Block> blockIterator = event.getBlockList().iterator();
            while (blockIterator.hasNext()) {
                Block block = blockIterator.next();
                if (block.getId() == BlockID.GLASS) {
                    blockIterator.remove();
                    continue;
                }
                if (!placedBlocks.contains(block)) {
                    blockIterator.remove();
                } else {
                    placedBlocks.remove(block);
                }
            }
        }
    }

    @EventHandler
    public void onRemoveArmor(InventoryMoveItemEvent event) {
        if (getSessionHandler().getPlayers().contains((Player)event.getSource()) && event.getItem().isArmor()) {
            event.setCancelled();
        }
    }

    @EventHandler
    public void onDamageCrystal(EntityDamageEvent e) {
        Entity victim = e.getEntity();

        if (victim instanceof EntityHumanCrystal && (victim.getLevel() == getSessionHandler().getPrimaryMap())) {
            e.setCancelled(true);

            if(e instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) e;

                Entity damager = event.getDamager();

                if (damager instanceof Player) {
                    Player player = (Player) damager;
                    EntityHumanCrystal victimCrystal = (EntityHumanCrystal) victim;

                    if(onCooldown.contains(player)) { return;
                    } else {
                        onCooldown.add(player);
                        getSessionHandler().getGameScheduler().registerGameTask(() -> {
                            onCooldown.remove(player);
                        }, crystal_cooldownTicks);
                    }



                    String crystalType = victimCrystal.namedTag.getString(CrystalWarsConstants.NBT_CRYSTAL_TYPE);
                    int crystalHealAmount = victimCrystal.namedTag.getInt(CrystalWarsConstants.NBT_HEAL_AMOUNT);
                    int crystalHealCountdown = victimCrystal.namedTag.getInt(CrystalWarsConstants.NBT_HEAL_COUNTDOWN);

                    // Use the constant first in-case it's null. Just checking that the victim crystal is definitely working.
                    if (CrystalWarsConstants.TYPE_REPAIR.equalsIgnoreCase(crystalType) && (crystalHealAmount != 0)) {

                        // If a player is already carrying a crystal, don't let them pick up another smh.
                        if (carriedCrystals.containsKey(player)) {
                            player.sendMessage(Utility.generateServerMessage("Heal",
                                    TextFormat.RED,
                                    "You cannot pick this up!",
                                    TextFormat.WHITE));
                            return;
                        }

                        victimCrystal.close();


                        // Give it to the player's team instantly.
                        if (crystalHealCountdown == 0) {
                            healTeam(player, crystalHealAmount);

                        } else {
                            String message = Utility.generateServerMessage("Heal",
                                    TextFormat.RED,
                                    String.format("%s%s has picked up a heal for %s%s%s Crystal HP.",
                                            player.getDisplayName(),
                                            TextFormat.RESET,
                                            TextFormat.RED,
                                            TextFormat.BOLD,
                                            crystalHealAmount
                                    ),
                                    TextFormat.WHITE);
                            for (Player p : getSessionHandler().getPlayers()) {
                                p.getLevel().addSound(p, Sound.RANDOM_ANVIL_LAND, 0.6f, 1.3f, p);
                                p.sendMessage(message);
                            }
                            spawnCarryCrystal(player, crystalHealAmount, crystalHealCountdown);
                        }
                    }
                }
            }
        }
    }

    public void healTeam(Player player, int amount) {
        getSessionHandler().getPlayerTeam(player).ifPresent(t -> {
            if(t instanceof SpectatingTeam) {
                player.sendMessage(Utility.generateServerMessage("Spec", TextFormat.BLUE, "Stawp it pls. <3"));
            } else {
                int leftover = getTeamPEType().healTeamCrystals(t, amount);
                int total = amount - leftover;

                String message = Utility.generateServerMessage("Heal",
                        TextFormat.RED,
                        String.format("Healed team %s's%s crystal by %s%s%sHP",
                                t.getDisplayName(),
                                TextFormat.RESET,
                                TextFormat.RED,
                                TextFormat.BOLD,
                                total
                        ),
                        TextFormat.WHITE);

                for(Player p: getSessionHandler().getPlayers()) {
                    p.sendMessage(message);
                    p.getLevel().addSound(p, Sound.BLOCK_BELL_HIT, 0.6f, 1.2f, p);
                }
            }
        });
    }


    public void spawnNewRepairCrystal() {
        if(repairRegions.size() > 0) {

            // -- Spawn the crystal entity
            int zoneIndex = repairRegions.size() == 1 ? 0 : random.nextInt(repairRegions.size());
            MapRegion zone = repairRegions.get(zoneIndex);

            // Randomise position: Snap to the lowest y.
            int y = zone.getPosLesser().getY();
            int dX = zone.getPosGreater().getX() - zone.getPosLesser().getX();
            int dZ = zone.getPosGreater().getZ() - zone.getPosLesser().getZ();

            float x = zone.getPosLesser().getX() + (dX < 1 ? 0 : random.nextInt(dX + 1)) + 0.5f;
            float z = zone.getPosLesser().getZ() + (dZ < 1 ? 0 : random.nextInt(dZ + 1)) + 0.5f;

            int dH = repairCrystal_maxHeal - repairCrystal_minHeal;
            int healAmount = repairCrystal_minHeal + (dH < 1 ? 0 : random.nextInt(dH + 1));

            spawnRepairCrystal(x, y, z, healAmount, repairCrystal_holdTime);

            // -- Start the next spawn cycle.
            // check delay isn't the same or less than random bounds.
            int delta = repairCrystal_maxDelay - repairCrystal_minDelay;
            int delay = delta <= 0
                    ? repairCrystal_minDelay
                    : repairCrystal_minDelay + random.nextInt( delta + 1);
            // schedule again in [delay] ticks time.
            getSessionHandler().getGameScheduler().registerGameTask(this::spawnNewRepairCrystal, delay);
        }
    }

    public void spawnRepairCrystal(double x, double y, double z, int healAmount, int timer) {
        Location location = new Location(x, y, z, 0, 0, getSessionHandler().getPrimaryMap());
        EntityHumanCrystal repair = EntityHumanCrystal.getNewCrystal(location, "green");
        repair.namedTag.putString(CrystalWarsConstants.NBT_CRYSTAL_TYPE, CrystalWarsConstants.TYPE_REPAIR);
        repair.namedTag.putInt(CrystalWarsConstants.NBT_HEAL_AMOUNT, healAmount);
        repair.namedTag.putInt(CrystalWarsConstants.NBT_HEAL_COUNTDOWN, timer);
        repair.setScale(1.5f);
        repair.setImmobile(true);
        repair.setNameTagAlwaysVisible(true);
        repair.setNameTagVisible(true);
        repair.setNameTag(
                new RawTextBuilder("HEAL TEAM CRYSTAL")
                        .setBold(true)
                        .setColor(TextFormat.GREEN)
                        .append(
                                new RawTextBuilder(String.format(" | + %s %s", healAmount, Utility.ResourcePackCharacters.HEART_ABSORB_FULL))
                                        .setColor(TextFormat.GOLD)
                        )
                        .append(
                                new RawTextBuilder(String.format(" | %s %s", timer, Utility.ResourcePackCharacters.TIMER))
                                        .setColor(TextFormat.WHITE)
                        )
                        .toString()
        );
        repair.spawnToAll();
        String message = Utility.generateServerMessage("Game",
                TextFormat.BLUE,
                String.format("Dropped a crystal repair for %s%s%s Crystal HP.",
                        TextFormat.RED,
                        TextFormat.BOLD,
                        healAmount
                ));

        for(Player p: getSessionHandler().getPlayers()) {
            p.sendMessage(message);
            p.getLevel().addSound(p, Sound.RANDOM_ANVIL_LAND, 0.6f, 1.3f, p);
        }
    }

    public void spawnCarryCrystal(Player player, int healAmount, int timer) {
        player.addEffect(Effect.getEffect(Effect.SLOWNESS).setAmbient(true).setDuration(100000));
        Location location = new Location(player.x, player.y + 2, player.z, 0, 0, getSessionHandler().getPrimaryMap());
        EntityHumanCrystal carry = EntityHumanCrystal.getNewCrystal(location, "green");
        carry.namedTag.putString(CrystalWarsConstants.NBT_CRYSTAL_TYPE, CrystalWarsConstants.TYPE_HOLDING);
        carry.namedTag.putInt(CrystalWarsConstants.NBT_HEAL_AMOUNT, healAmount);
        carry.namedTag.putInt(CrystalWarsConstants.NBT_HEAL_COUNTDOWN, timer);
        carry.setScale(0.6f);
        carry.setMotion(player.getMotion());
        carry.setNameTagAlwaysVisible(true);
        carry.setNameTagVisible(true);
        carry.setNameTag(
                new RawTextBuilder("KILL TO STEAL")
                        .setBold(true)
                        .setColor(TextFormat.RED)
                        .append(
                                new RawTextBuilder(String.format(" | + %s %s", healAmount, Utility.ResourcePackCharacters.HEART_FULL))
                                        .setColor(TextFormat.GOLD)
                        )
                        .append(
                                new RawTextBuilder(String.format(" | %s %s", timer, Utility.ResourcePackCharacters.TIMER))
                                        .setColor(TextFormat.WHITE)
                        )
                        .toString()
        );
        carriedCrystals.put(player, carry);
        carry.spawnToAll();
    }

    protected void handleGameDeath(GamePlayerDeathEvent event) {
        handleCrystalDrop(event.getDeathCause().getVictim());

        if (crystalExistsForTeam(event.getDeathCause().getVictimTeam())) {
            event.setDeathState(GamePlayerDeathEvent.DeathState.TIMED_RESPAWN);
            event.setRespawnSeconds(5);
        } else {
            checkWin();
        }
    }

    protected void handleCrystalDrop(Player player) {
        if(carriedCrystals.containsKey(player)) {
            EntityHumanCrystal c = carriedCrystals.remove(player);
            int crystalHealAmount = c.namedTag.getInt(CrystalWarsConstants.NBT_HEAL_AMOUNT);
            int crystalHealCountdown = c.namedTag.getInt(CrystalWarsConstants.NBT_HEAL_COUNTDOWN);
            player.removeEffect(Effect.SLOWNESS);
            c.close();

            spawnRepairCrystal(player.getX(), player.getY(), player.getZ(), crystalHealAmount, crystalHealCountdown);
        }
    }


    protected void checkWin() {
        Team aliveTeam = null;
        for (Team team : getSessionHandler().getTeams().values()) {
            if (team.isActiveGameTeam() && (team.getPlayers().size() > 0 || crystalExistsForTeam(team))) {
                if (aliveTeam != null)
                    return;
                aliveTeam = team;
            }
        }
        if (aliveTeam != null) {
            getSessionHandler().declareVictoryForTeam(aliveTeam);
        }
    }

    protected void alignCrystals() {
        for(Map.Entry<Player, EntityHumanCrystal> e: carriedCrystals.entrySet()) {
            Player p = e.getKey();
            EntityHumanCrystal c = e.getValue();

            c.setPositionAndRotation(p.getNextPosition().add(0, 2, 0), p.getYaw(), p.getPitch());
            c.setMotion(p.getMotion());
        }
    }

    protected void countdownCrystals() {
        for(Map.Entry<Player, EntityHumanCrystal> e: new HashMap<>(carriedCrystals).entrySet()) {
            Player p = e.getKey();
            EntityHumanCrystal c = e.getValue();

            int countdown = c.namedTag.getInt(CrystalWarsConstants.NBT_HEAL_COUNTDOWN);
            int healAmount = c.namedTag.getInt(CrystalWarsConstants.NBT_HEAL_AMOUNT);

            if(countdown == 0) {
                if(healAmount > 0) healTeam(p, healAmount);

                carriedCrystals.remove(p);
                p.removeEffect(Effect.SLOWNESS);
                c.close();
                continue;
            }

            c.namedTag.putInt(CrystalWarsConstants.NBT_HEAL_COUNTDOWN, countdown - 1);
            c.setNameTag(new RawTextBuilder("KILL THIS PLAYER")
                    .setBold(true)
                    .setColor(TextFormat.RED)
                    .append(
                            new RawTextBuilder(String.format(" | + %s %s", healAmount, Utility.ResourcePackCharacters.HEART_FULL))
                                    .setColor(TextFormat.GOLD)
                    )
                    .append(
                            new RawTextBuilder(String.format(" | %s %s", countdown - 1, Utility.ResourcePackCharacters.TIMER))
                                    .setColor(TextFormat.WHITE)
                    )
                    .toString());
        }
    }

    protected boolean crystalExistsForTeam(Team team) {
        return getTeamPEType().getTeamAliveCrystalCount(team) > 0;
    }

    protected CrystalPointEntity getTeamPEType() {
        return (CrystalPointEntity) getSessionHandler()
                .getPointEntityTypeManager()
                .getRegisteredTypes()
                .get(CrystalPointEntity.ID);
    }

    @Override
    public Team.GenericTeamBuilder[] getTeams() {
        return TeamPresets.FOUR_TEAMS;
    }
}