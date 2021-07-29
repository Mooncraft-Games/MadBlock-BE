package org.madblock.newgamesapi.map.pointentities.defaults;

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
import org.madblock.newgamesapi.map.pointentities.PointEntityType;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.newgamesapi.nukkit.entity.EntityHumanPlus;

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

    public abstract String getPersistentUuidNbtLocation ();

    @Override
    public void onRegister() {
        NewGamesAPI1.get().getServer().getPluginManager().registerEvents(this, NewGamesAPI1.get());
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
            FullChunk chunk = npcLevel.getChunk((int) Math.floor(position.getX() / 16), (int) Math.floor(position.getZ() / 16), true);
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

            newHuman.setPositionAndRotation(position, entity.getYaw(), entity.getPitch());
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
    // - Skin files must be in the "1.8.0" format. This is the same as Blockbench's Legacy Bedrock Model format (except it
    //   needs changing from "1.10.0")
    // - There's no other needs as long as the above criteria are met afaik
    protected Optional<Skin> getSkin(PointEntity entity) {
        Skin skin = new Skin();
        Optional<BufferedImage> skinImage = loadSkinFile(entity.getStringProperties().get("skin_file"));
        Optional<String> modelData = loadModelFile(entity.getStringProperties().get("model_file"));
        if(skinImage.isPresent()) {
            skin.setSkinData(skinImage.get());
            skin.setTrusted(true);
            skin.setArmSize(entity.getStringProperties().getOrDefault("arm_size", "wide"));
            skin.generateSkinId(entity.getId());

            if(modelData.isPresent() && entity.getStringProperties().containsKey("model_id")) {
                skin.setGeometryName(entity.getStringProperties().get("model_id"));
                skin.setGeometryData(modelData.get());
            }

            return skin.isValid() ? Optional.of(skin) : Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    protected Optional<BufferedImage> loadSkinFile(String path) {
        if (path != null) {
            File skinfile = new File(NewGamesAPI1.get().getServer().getDataPath()+"/skins/"+path);
            try {
                BufferedImage image = ImageIO.read(skinfile);
                return Optional.of(image);
            } catch (Exception err) {
                NewGamesAPI1.getPlgLogger().warning("Error loading custom skin data for NPCHuman skin.");
            }
        }
        File fallback = new File(NewGamesAPI1.get().getServer().getDataPath()+"/skins/default.png");
        try {
            BufferedImage image = ImageIO.read(fallback);
            return Optional.of(image);
        } catch (Exception err) {
            NewGamesAPI1.getPlgLogger().warning("Error loading fallback skin data for NPCHuman skin.");
        }
        return Optional.empty();
    }

    protected Optional<String> loadModelFile(String path) {

        if (path != null) {
            File skinfile = new File(NewGamesAPI1.get().getServer().getDataPath()+"/skins/"+path);

            try {
                BufferedReader r = new BufferedReader(new FileReader(skinfile));
                StringBuilder b = new StringBuilder();
                Iterator<String> lines = r.lines().iterator();

                while (lines.hasNext()) {
                    b.append(lines.next());
                    b.append("\n");
                }

                return Optional.of(b.toString());

            } catch (Exception err) {
                NewGamesAPI1.getPlgLogger().warning("Error loading custom skin model data for NPCHuman skin.");
            }
        }
        return Optional.empty();
    }

    protected String getDisplayName(PointEntity entity) {
        return formatNameFromColourCodes(entity.getStringProperties().getOrDefault("name", ""));
    }

    protected String formatNameFromColourCodes(String unformattedName){
        return TextFormat.colorize(unformattedName);
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
