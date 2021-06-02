package org.madblock.skywars.pointentities;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.entity.item.EntityItem;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.HugeExplodeParticle;
import cn.nukkit.level.particle.Particle;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.utils.TextFormat;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.pointentities.PointEntityCallData;
import org.madblock.newgamesapi.map.pointentities.PointEntityType;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.newgamesapi.team.Team;
import org.madblock.newgamesapi.team.TeamPresets;
import org.madblock.skywars.SkywarsPlugin;
import org.madblock.skywars.behaviours.SkywarsGameBehaviour;
import org.madblock.skywars.powerups.PowerUp;
import org.madblock.skywars.utils.Constants;
import org.madblock.skywars.utils.SkywarsUtils;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class PointEntityTypePowerUp extends PointEntityType implements Listener {

    public static final String ID = "resources/powerup";

    private static final String GEOMETRY_NAME = "geometry.skywars_powerup";

    private static final String GEOMETRY_DATA;
    private static final BufferedImage SKIN_DATA;

    static {

        String geometryFile = null;
        try {
            geometryFile = IOUtils.toString(
                    SkywarsPlugin.getInstance().getClass().getResourceAsStream("/resources/powerup/skywars_powerup.geometry.json"),
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            SkywarsPlugin.getInstance().getLogger().error("Failed to load powerup geometry file.");
            exception.printStackTrace();
        }
        GEOMETRY_DATA = geometryFile;

        BufferedImage skinFile = null;
        try {
            skinFile = ImageIO.read(
                    SkywarsPlugin.getInstance().getClass().getResourceAsStream("/resources/powerup/skywars_powerup.png")
            );
        } catch (IOException exception) {
            SkywarsPlugin.getInstance().getLogger().error("Failed to load powerup skin file.");
            exception.printStackTrace();
        }
        SKIN_DATA = skinFile;

    }

    private final BiMap<PointEntity, Entity> powerUpEntities;

    public PointEntityTypePowerUp(GameHandler gameHandler) {
        super(ID, gameHandler);
        this.powerUpEntities = HashBiMap.create();
    }

    public void onRegister() {
        SkywarsPlugin.getInstance().getServer().getPluginManager().registerEvents(this, SkywarsPlugin.getInstance());
        this.addFunction("give_powerup", this::givePowerUpFunction);
        this.gameHandler.getGameScheduler().registerGameTask(this::rotatePowerUpEntitiesTask, 0, 1);
    }

    public void onUnregister() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void onAddPointEntity (PointEntity entity) {
        super.onAddPointEntity(entity);
        Level powerUpLevel = this.manager.getLevelLookup().get(entity);
        Vector3 position = entity.positionToVector3().add(!entity.isAccuratePosition() ? 0.5d : 0, 0, !entity.isAccuratePosition() ? 0.5d : 0);

        Entity powerUpEntity = getNewPowerUpEntity(entity, powerUpLevel, position);
        powerUpEntity.spawnToAll();

        this.powerUpEntities.put(entity, powerUpEntity);
    }

    @Override
    public void onRemovePointEntity (PointEntity entity) {
        super.onRemovePointEntity(entity);
        if (this.powerUpEntities.containsKey(entity)) {
            this.powerUpEntities.get(entity).despawnFromAll();
            this.powerUpEntities.remove(entity);
        }
    }

    @EventHandler
    public void onDamage (EntityDamageEvent event) {
        if (this.powerUpEntities.containsValue(event.getEntity())) {
            event.setCancelled();
        }
    }

    @EventHandler(
            priority = EventPriority.LOW
    )
    public void onEntityAttack (EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof EntityHuman && this.powerUpEntities.containsValue(event.getEntity())) {

            Team team = this.getGameHandler().getTeams().get(TeamPresets.DEAD_TEAM_ID);

            if (team.getPlayers().contains(event.getDamager())) {
                return;
            }

            PointEntity pointEntity = this.powerUpEntities.inverse().get(event.getEntity());

            Particle largeExplosion = new HugeExplodeParticle(event.getEntity().getPosition());
            for (Player player : this.getGameHandler().getPlayers()) {
                player.getLevel().addParticle(largeExplosion, player);
            }

            event.getEntity().getLevel().removeEntity(event.getEntity());

            this.powerUpEntities.remove(pointEntity);

            this.getGameHandler().getGameScheduler().registerGameTask(() -> {

                for (Player p : this.getGameHandler().getPlayers()) {
                    p.sendMessage(Utility.generateServerMessage("GAME", TextFormat.DARK_AQUA, "A power up has spawned!"));
                }

                Vector3 position = pointEntity.positionToVector3().add(!pointEntity.isAccuratePosition() ? 0.5d : 0, 0, !pointEntity.isAccuratePosition() ? 0.5d : 0);

                Entity powerUpEntity = getNewPowerUpEntity(pointEntity, event.getEntity().getLevel(), position);
                powerUpEntity.spawnToAll();

                this.powerUpEntities.put(pointEntity, powerUpEntity);

            }, 20 * Constants.POWERUP_SPAWN_COOLDOWN);

            Player player = (Player)event.getDamager();

            HashMap<String, String> params = new HashMap<>();
            params.put("player_uuid", player.getUniqueId().toString());
            this.getParentManager().getRegisteredTypes().get(ID).executeFunction("give_powerup", pointEntity, event.getEntity().getLevel(), params);
        }
    }

    private void givePowerUpFunction (PointEntityCallData callData) {
        UUID uuid = UUID.fromString(callData.getParameters().get("player_uuid"));
        Optional<Player> possiblePlayer = callData.getLevel().getServer().getPlayer(uuid);
        SkywarsGameBehaviour behaviour = (SkywarsGameBehaviour)this.getGameHandler().getGameBehaviors();
        if (possiblePlayer.isPresent()) { // We could just use .get() but in case executeFunction changes to have a delay or anything, this is safer.

            Player player = possiblePlayer.get();

            Class<? extends PowerUp> powerUpClass = SkywarsUtils.getRandomPowerUp();
            PowerUp powerUp;
            try {
                powerUp = powerUpClass.getConstructor(GameBehavior.class).newInstance(behaviour);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException exception) {
                SkywarsPlugin.getInstance().getLogger().error(String.format("[PowerUps] Could not create %s.\n%s", powerUpClass.getName(), exception.toString()));
                return;
            }

            behaviour.addPowerUp(powerUp);

            PointEntity pointEntity = callData.getPointEntity();

            Item powerUpItem = new Item(powerUp.getItemId(), 0, 1);
            CompoundTag namedTag = new CompoundTag();

            namedTag.putInt(Constants.POWERUP_ITEM_NBT_ID, powerUp.getId())
                    .putList(new ListTag<>("ench"));
            powerUpItem.setNamedTag(namedTag);
            powerUpItem.setCustomName(String.format("%s%s%s %s- TAP TO USE", TextFormat.BOLD, TextFormat.AQUA, powerUp.getName(), TextFormat.GREEN));
            powerUpItem.setLore(powerUp.getDescription());

            if (!player.getInventory().canAddItem(powerUpItem)) {
                EntityItem powerUpItemEntity = (EntityItem)Entity.createEntity(
                        "Item",
                        new Position(pointEntity.getX(), pointEntity.getY(), pointEntity.getZ(), gameHandler.getPrimaryMap())
                );

                powerUpItemEntity.namedTag.putCompound("Item", NBTIO.putItemHelper(powerUpItem));
                powerUpItemEntity.spawnToAll();
                player.sendMessage(String.format("%sThe power up dropped on the ground because you did not have enough inventory space!", TextFormat.RED));
                return;
            }

            player.sendMessage(Utility.generateServerMessage(
                    "POWERUP",
                    TextFormat.YELLOW,
                    String.format("%sYou got the %s power up! %s", TextFormat.AQUA, powerUp.getName(), powerUp.getDescription())
            ));

            player.getInventory().addItem(powerUpItem);
            player.getInventory().sendContents(player);

        }
    }

    private void rotatePowerUpEntitiesTask () {
        for (Entity entity : this.powerUpEntities.values()) {
            double newYaw = entity.getYaw() + 10 >= 360 ? 0 : entity.getYaw() + 10;
            entity.setRotation(newYaw, entity.getPitch());
        }
    }

    private Entity getNewPowerUpEntity (PointEntity pointEntity, Level level, Vector3 position) {

        Skin skin = new Skin();
        skin.setGeometryName(GEOMETRY_NAME);
        skin.setGeometryData(GEOMETRY_DATA);
        skin.setSkinData(SKIN_DATA);
        skin.setTrusted(true);
        skin.generateSkinId(pointEntity.getId());


        CompoundTag skinDataTag = new CompoundTag()
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

        CompoundTag nbt = new CompoundTag()
                .putList(
                        new ListTag<DoubleTag>("Pos")
                            .add(new DoubleTag("", position.getX()))
                            .add(new DoubleTag("", position.getY()))
                            .add(new DoubleTag("", position.getZ()))
                ).putList(
                        new ListTag<DoubleTag>("Motion")
                            .add(new DoubleTag("", 0.0D))
                            .add(new DoubleTag("", 0.0D))
                            .add(new DoubleTag("", 0.0D))
                ).putList(
                        new ListTag<FloatTag>("Rotation")
                            .add(new FloatTag("", (float)0))
                            .add(new FloatTag("", (float)0))
                )
                .putBoolean("npc", true)
                .putFloat("scale", 2)
                .putCompound("Skin", skinDataTag)
                .putBoolean("ishuman", true);

        FullChunk chunk = level.getChunk((int)Math.floor(position.getX() / 16.0D), (int)Math.floor(position.getZ() / 16.0D), true);
        Entity powerUpEntity = new EntityHuman(chunk, nbt);
        powerUpEntity.setScale(2);
        powerUpEntity.setNameTag(String.format("%s%sTAP TO GET POWERUP", TextFormat.BOLD, TextFormat.GREEN));
        powerUpEntity.setNameTagAlwaysVisible(true);

        return powerUpEntity;

    }




}
