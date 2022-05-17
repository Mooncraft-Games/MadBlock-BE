package org.madblock.newgamesapi.map.pointentities.defaults;

import cn.nukkit.Player;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntitySpawnEvent;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector2;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.utils.TextFormat;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.pointentities.PointEntityCallData;
import org.madblock.newgamesapi.map.pointentities.PointEntityType;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.newgamesapi.nukkit.entity.EntityHumanPlus;
import org.madblock.newgamesapi.nukkit.packet.AnimateEntityPacket;
import org.madblock.newgamesapi.util.SkinUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

public abstract class PointEntityTypeNPC extends PointEntityType implements Listener {

    protected BiMap<PointEntity, UUID> npcIDs;
    protected HashMap<PointEntity, EntityHumanPlus> lastInstances;

    public PointEntityTypeNPC(String id, GameHandler gameHandler) {
        super(id, gameHandler);
        npcIDs = HashBiMap.create();
        lastInstances = new HashMap<>();
    }

    public abstract String getPersistentUuidNbtLocation();

    @Override
    public void onRegister() {
        NewGamesAPI1.get().getServer().getPluginManager().registerEvents(this, NewGamesAPI1.get());
        this.addFunction("animate", this::animate);
    }

    @Override
    public void onUnregister() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void onAddPointEntity(PointEntity entity) {
        super.onAddPointEntity(entity);
        if(getParentManager().getLevelLookup().containsKey(entity)) {
            Level npcLevel = getParentManager().getLevelLookup().get(entity);

            if (lastInstances.containsKey(entity)) {
                EntityHumanPlus npc = lastInstances.get(entity);
                npc.getLevel().removeEntity(npc);
            }

            UUID uuid = UUID.randomUUID();
            Vector3 position = entity.positionToVector3().add(!entity.isAccuratePosition() ? 0.5d : 0, 0, !entity.isAccuratePosition() ? 0.5d : 0);
            Vector2 rotation = entity.rotationToVector2();
            FullChunk chunk = npcLevel.getChunk((int) Math.floor(position.getX()) >> 4, (int) Math.floor(position.getZ()) >> 4, true);
            Optional<Skin> sk = getSkin(entity);

            if(!sk.isPresent()) {
                getParentManager().removePointEntity(entity);
                NewGamesAPI1.getPlgLogger().warning("Skipped invalid NPCHuman entity.");
                return;
            }
            Skin skin = sk.get();
            String fullName = getDisplayName(entity);
            float scale = entity.getFloatProperties().getOrDefault("scale", 1f);

            // -- Taken From https://github.com/Nukkit-coders/NPC/blob/master/src/main/java/idk/plugin/npc/NPC.java --
            CompoundTag nbt = new CompoundTag()
                    .putList(new ListTag<>("Pos")
                            .add(new DoubleTag("", position.getX()))
                            .add(new DoubleTag("", position.getY()))
                            .add(new DoubleTag("", position.getZ())))
                    .putList(new ListTag<DoubleTag>("Motion")
                            .add(new DoubleTag("", 0))
                            .add(new DoubleTag("", 0))
                            .add(new DoubleTag("", 0)))
                    .putList(new ListTag<FloatTag>("Rotation")
                            .add(new FloatTag("", (float) rotation.getY()))
                            .add(new FloatTag("", (float) rotation.getX())))
                    .putBoolean("npc", true)
                    .putFloat("scale", scale)
                    .putString(getPersistentUuidNbtLocation(), uuid.toString());
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
            nbt.putCompound("Skin", skinDataTag);
            nbt.putBoolean("ishuman", true);
            // -- END snippet --
            EntityHumanPlus newHuman = new EntityHumanPlus(chunk, nbt)
                    .setClientSpawnAnimation(
                            entity.getStringProperties().get("spawn_anim"),
                            entity.getStringProperties().get("spawn_anim_controller")
                    );

            //newHuman.setPositionAndRotation(position, entity.getYaw(), entity.getPitch());
            newHuman.setImmobile(true);
            newHuman.setSneaking(entity.getBooleanProperties().getOrDefault("sneaking", false));
            newHuman.setNameTagAlwaysVisible(true);
            newHuman.setNameTagVisible(true);
            newHuman.setNameTag(fullName);
            sk.ifPresent(newHuman::setSkin);
            newHuman.setScale(scale);

            npcIDs.put(entity, uuid);
            lastInstances.put(entity, newHuman);
        }
    }

    // Notes for parsing a skin.
    // - skin.setGeometryName() must be your skin's complete name within the model file (including the "geometry" at the start)
    protected Optional<Skin> getSkin(PointEntity entity) {
        Skin skin = new Skin();
        Optional<BufferedImage> skinImage = SkinUtils.getSkinFile(entity.getStringProperties().get("skin_file"));
        Optional<String> modelData = SkinUtils.getModelFile(entity.getStringProperties().get("model_file"));
        if(skinImage.isPresent()) {
            skin.setSkinData(skinImage.get());
            skin.setTrusted(true);
            skin.setArmSize(entity.getStringProperties().getOrDefault("arm_size", "wide"));
            skin.setSkinId(UUID.randomUUID() + entity.getId());

            if(modelData.isPresent() && entity.getStringProperties().containsKey("model_id")) {
                skin.setGeometryName(entity.getStringProperties().get("model_id"));
                skin.setGeometryData(modelData.get());
            }

            return skin.isValid() ? Optional.of(skin) : Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    protected String getDisplayName(PointEntity entity) {
        return formatNameFromColourCodes(entity.getStringProperties().getOrDefault("name", ""));
    }

    protected String formatNameFromColourCodes(String unformattedName){
        return TextFormat.colorize(unformattedName);
    }



    protected void animate(PointEntityCallData pointEntityCallData) {
        String animation = pointEntityCallData.getParameters().get("animation");
        String controller = pointEntityCallData.getParameters().get("controller");

        EntityHumanPlus entity = this.lastInstances.get(pointEntityCallData.getPointEntity());

        if(animation == null) animation = entity.getSpawnAnimationID();
        if(controller == null) controller = entity.getSpawnAnimationController();


        if(animation != null && controller != null) {

            AnimateEntityPacket dataPacket = new AnimateEntityPacket();
            dataPacket.eid = entity.getId();
            dataPacket.animation = animation;
            dataPacket.controller = controller;
            dataPacket.stopExpression = "query.any_animation_finished";

            for(Player player : entity.getViewers().values()) {
                player.dataPacket(dataPacket);
            }
        }
    }



    @EventHandler
    public void onDamage(EntityDamageEvent event){
        if(event.getEntity() instanceof EntityHumanPlus){
            EntityHumanPlus human = (EntityHumanPlus) event.getEntity();
            String uuidloc = human.namedTag.getString(getPersistentUuidNbtLocation());
            if(!uuidloc.equals("")) {
                UUID uuid = UUID.fromString(uuidloc);
                if (npcIDs.containsValue(uuid)) {
                    if (npcIDs.inverse().get(uuid).getBooleanProperties().getOrDefault("invincible", true)) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    // If the entity is respawned we need should update the last instance of it
    @EventHandler
    public void onNPCSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof EntityHumanPlus) {
            EntityHumanPlus human = (EntityHumanPlus)event.getEntity();
            if (human.namedTag.contains(getPersistentUuidNbtLocation())) {
                UUID uuid = UUID.fromString(human.namedTag.getString(getPersistentUuidNbtLocation()));
                lastInstances.put(npcIDs.inverse().get(uuid), human);
            }
        }
    }

}
