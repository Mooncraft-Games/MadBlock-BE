package org.madblock.newgamesapi.map.pointentities.defaults;

import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class PointEntityTypeHologram extends PointEntityType implements Listener {

    protected BiMap<PointEntity, UUID> npcIDs;
    protected HashMap<PointEntity, EntityHuman> lastInstances;

    public PointEntityTypeHologram(GameHandler gameHandler) {
        super("hologram", gameHandler);
        npcIDs = HashBiMap.create();
        lastInstances = new HashMap<>();
    }

    public String getPersistentUuidNbtLocation () {
        return "hologram_npc";
    }

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
                EntityHuman npc = lastInstances.get(entity);
                npc.getLevel().removeEntity(npc);
            }

            UUID uuid = UUID.randomUUID();
            Vector3 position = entity.positionToVector3().add(!entity.isAccuratePosition() ? 0.5d : 0, 0, !entity.isAccuratePosition() ? 0.5d : 0);
            FullChunk chunk = npcLevel.getChunk((int) Math.floor(position.getX() / 16), (int) Math.floor(position.getZ() / 16), true);
            Optional<Skin> sk = getSkin(entity);
            if(!sk.isPresent()) {
                getParentManager().removePointEntity(entity);
                NewGamesAPI1.getPlgLogger().warning("Skipped invalid NPCHuman entity.");
                return;
            }
            Skin skin = sk.get();
            String fullName = formatNameFromColourCodes(entity.getStringProperties().getOrDefault("text", ""));

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
                    .add(new FloatTag("", 0f))
                    .add(new FloatTag("", 0f)))
                    .putBoolean("npc", true)
                    .putString(getPersistentUuidNbtLocation(), uuid.toString());
            CompoundTag skinDataTag = new CompoundTag()
                    .putBoolean("Transparent", true)
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
            EntityHuman newHuman = new EntityHuman(chunk, nbt);
            newHuman.setImmobile(true);
            newHuman.setNameTagAlwaysVisible(true);
            newHuman.setNameTagVisible(true);
            newHuman.setNameTag(fullName);
            //sk.ifPresent(newHuman::setSkin);


            npcIDs.put(entity, uuid);
            lastInstances.put(entity, newHuman);
        }
    }

    protected Optional<Skin> getSkin(PointEntity entity) {
        Skin skin = new Skin();
        File skinFile = new File(NewGamesAPI1.get().getServer().getDataPath()+"/skins/invisible.png");
        try {
            BufferedImage image = ImageIO.read(skinFile);
            skin.setSkinData(image);
            skin.setTrusted(true);
            skin.setArmSize(entity.getStringProperties().getOrDefault("arm_size", "wide"));
            skin.setSkinId(UUID.randomUUID().toString());
            return skin.isValid() ? Optional.of(skin) : Optional.empty();
        }  catch (Exception err) {
            return Optional.empty();
        }
    }

    protected String formatNameFromColourCodes(String unformattedName){
        return TextFormat.colorize(unformattedName);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event){
        if(event.getEntity() instanceof EntityHuman){
            EntityHuman human = (EntityHuman) event.getEntity();
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

}
