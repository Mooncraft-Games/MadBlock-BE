package org.madblock.blockswap.powerups;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.Position;
import cn.nukkit.level.Sound;
import cn.nukkit.utils.TextFormat;
import org.madblock.blockswap.behaviours.BlockSwapGameBehaviour;
import org.madblock.blockswap.utils.BlockSwapUtility;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameBehavior;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class PowerUpManager {

    public static final int POWERUP_SLOTS = 3;

    protected final BlockSwapGameBehaviour behaviour;
    protected final Set<Entity> powerUpEntities = new HashSet<>();

    protected final Map<UUID, PowerUp[]> playerPowerUps = new HashMap<>();


    public PowerUpManager(BlockSwapGameBehaviour behavior) {
        this.behaviour = behavior;
    }

    public int getAvailablePowerUpSlot(Player player) {
        this.ensureSlotsExists(player);

        PowerUp[] powerUps = this.playerPowerUps.get(player.getUniqueId());
        for (int slot = 0; slot < POWERUP_SLOTS; slot++) {
            if (powerUps[slot] == null) {
                return slot;
            }
        }
        return -1;
    }

    public Optional<PowerUp> getPowerUp(Player player, int slot) {
        this.ensureSlotsExists(player);
        return Optional.ofNullable(this.playerPowerUps.get(player.getUniqueId())[slot]);
    }

    public PowerUp giveRandomPowerUp(Player player, int slot) {
        Class<? extends PowerUp> powerUpClass = BlockSwapUtility.getRandomPowerUp();
        PowerUp powerUp;
        try {
            powerUp = powerUpClass.getConstructor(GameBehavior.class, Player.class).newInstance(this.behaviour, player);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            throw new AssertionError("Misconfigured power up found", exception);
        }

        this.setPowerUp(player, powerUp, slot);
        return powerUp;
    }

    public void setPowerUp(Player player, PowerUp powerUp, int slot) {
        this.ensureSlotsExists(player);

        this.playerPowerUps.get(player.getUniqueId())[slot] = powerUp;

        this.behaviour.updatePlayerInventory(player);
    }

    private void ensureSlotsExists(Player player) {
        this.playerPowerUps.putIfAbsent(player.getUniqueId(), new PowerUp[POWERUP_SLOTS]);
    }

    public void usePowerUp(Player player, int slot) {
        if (this.getPowerUp(player, slot).isPresent()) {
            PowerUp powerUp = this.getPowerUp(player, slot).get();
            powerUp.use();

            this.playerPowerUps.get(player.getUniqueId())[slot] = null;

            this.behaviour.updatePlayerInventory(player);

            for (Player p : this.behaviour.getSessionHandler().getPlayers()) {
                p.sendMessage(Utility.generateServerMessage("POWERUP", TextFormat.YELLOW, String.format("%s%s used %s%s%s%s%s!", player.getDisplayName(), TextFormat.GRAY, TextFormat.BOLD, TextFormat.YELLOW, powerUp.getName(), TextFormat.RESET, TextFormat.GRAY )));
            }
        }
    }

    public Set<Entity> getEntities() {
        return new HashSet<>(this.powerUpEntities);
    }

    public void spawnAt(Position position) {
        Entity lightning = Entity.createEntity("Lightning", position);
        lightning.spawnToAll();

        Entity powerUpEntity = new EntityBlockSwapPowerUp(this.behaviour, position.getChunk(), Entity.getDefaultNBT(position));
        powerUpEntity.spawnToAll();

        this.powerUpEntities.add(powerUpEntity);
    }

    /**
     * Called internally when a powerup entity is hit.
     * @param player player who hit the powerup entity
     * @param powerUpEntity the powerup entity hit
     */
    void onEntityHit(Player player, Entity powerUpEntity) {
        // Verify the player can hold onto another power up
        int powerUpSlot = this.getAvailablePowerUpSlot(player);
        if (powerUpSlot == -1) {
            player.sendMessage(Utility.generateServerMessage("POWERUP", TextFormat.YELLOW, String.format("%s%s", TextFormat.RED, "You must use a power up before you can get another!")));
            return;
        }

        PowerUp powerUp = this.giveRandomPowerUp(player, powerUpSlot);

        for (Player p : this.behaviour.getSessionHandler().getPlayers()) {
            p.sendMessage(Utility.generateServerMessage("POWERUP", TextFormat.YELLOW, String.format("%s%s has received the %s%s%s%s%s power up!", player.getDisplayName(), TextFormat.GRAY, TextFormat.BOLD, TextFormat.YELLOW, powerUp.getName(), TextFormat.RESET, TextFormat.GRAY)));
        }
        player.sendMessage(Utility.generateServerMessage("POWERUP", TextFormat.YELLOW, String.format("%s%s", TextFormat.AQUA, powerUp.getDescription())));

        powerUpEntity.getLevel().addSound(powerUpEntity.getLocation(), Sound.FIREWORK_SHOOT, 1, 1, player);
        powerUpEntity.despawnFromAll();

        this.setPowerUp(player, powerUp, powerUpSlot);

        if (powerUp.isInstantConsumable()) {
            this.usePowerUp(player, powerUpSlot);
        }

        this.powerUpEntities.remove(powerUpEntity);
    }

}
