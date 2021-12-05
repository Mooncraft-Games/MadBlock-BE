package org.madblock.blockswap.powerups;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.level.Position;
import cn.nukkit.level.Sound;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.TextFormat;
import org.madblock.blockswap.BlockSwapPlugin;
import org.madblock.blockswap.behaviours.BlockSwapGameBehaviour;
import org.madblock.blockswap.utils.BlockSwapUtility;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.util.SkinUtils;

import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
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
                p.sendActionBar(String.format("%s%s used %s%s%s%s%s!", player.getDisplayName(), TextFormat.GRAY, TextFormat.BOLD, TextFormat.YELLOW, powerUp.getName(), TextFormat.RESET, TextFormat.GRAY ));
            }
        }
    }

    public Set<Entity> getEntities() {
        return new HashSet<>(this.powerUpEntities);
    }

    public void spawnAt(Position position) {
        Entity lightning = Entity.createEntity("Lightning", position);
        lightning.spawnToAll();

        CompoundTag tag = Entity.getDefaultNBT(position);
        tag.putCompound("Skin", getPowerUpSkinTag());

        Entity powerUpEntity = new EntityBlockSwapPowerUp(this.behaviour, position.getChunk(), tag);
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
            player.sendActionBar(String.format("%s%s", TextFormat.RED, "You must use a power up before you can get another!"));
            return;
        }

        PowerUp powerUp = this.giveRandomPowerUp(player, powerUpSlot);

        for (Player p : this.behaviour.getSessionHandler().getPlayers()) {
            p.sendActionBar(String.format("%s%s has received the %s%s%s%s%s power up!", player.getDisplayName(), TextFormat.GRAY, TextFormat.BOLD, TextFormat.YELLOW, powerUp.getName(), TextFormat.RESET, TextFormat.GRAY));
        }
        player.sendMessage(Utility.generateServerMessage("POWERUP", TextFormat.YELLOW, String.format("%s%s", TextFormat.AQUA, powerUp.getDescription())));

        powerUpEntity.getLevel().addSound(powerUpEntity.getLocation(), Sound.FIREWORK_SHOOT, 1, 1, player);
        powerUpEntity.close();

        this.setPowerUp(player, powerUp, powerUpSlot);

        if (powerUp.isInstantConsumable()) {
            this.usePowerUp(player, powerUpSlot);
        }

        this.powerUpEntities.remove(powerUpEntity);
    }

    private static CompoundTag getPowerUpSkinTag() {
        Optional<String> modelJSON = SkinUtils.getModelFile("prop/lucky/prop_lucky_block.json");
        Optional<BufferedImage> skinData = SkinUtils.getSkinFile("prop/lucky/prop.lucky_block.png");

        if (!(modelJSON.isPresent() && skinData.isPresent())) {
            BlockSwapPlugin.getLog().error("Unable to find powerup skin");
            return new CompoundTag();
        }

        Skin skin = new Skin();
        skin.setGeometryName("geometry.prop.lucky_block");
        skin.setGeometryData(modelJSON.get());
        skin.setSkinData(skinData.get());
        skin.setTrusted(true);

        return new CompoundTag()
                .putByteArray("Data", skin.getSkinData().data)
                .putInt("SkinImageWidth", skin.getSkinData().width)
                .putInt("SkinImageHeight", skin.getSkinData().height)
                .putString("ModelId", skin.getSkinId())
                .putString("CapeId", skin.getCapeId())
                .putByteArray("CapeData", skin.getCapeData().data)
                .putInt("CapeImageWidth", skin.getCapeData().width)
                .putInt("CapeImageHeight", skin.getCapeData().height)
                .putByteArray("SkinResourcePatch", skin.getSkinResourcePatch().getBytes(StandardCharsets.UTF_8))
                .putByteArray("GeometryData", skin.getGeometryData().getBytes(StandardCharsets.UTF_8))
                .putByteArray("AnimationData", skin.getAnimationData().getBytes(StandardCharsets.UTF_8))
                .putBoolean("PremiumSkin", skin.isPremium())
                .putBoolean("PersonaSkin", skin.isPersona())
                .putBoolean("CapeOnClassicSkin", skin.isCapeOnClassic());
    }

}
