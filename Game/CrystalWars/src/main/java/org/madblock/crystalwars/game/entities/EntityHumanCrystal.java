package org.madblock.crystalwars.game.entities;

import cn.nukkit.Player;
import cn.nukkit.block.BlockEnchantingTable;
import cn.nukkit.block.BlockEnderChest;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.level.Location;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Sound;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.utils.TextFormat;
import org.madblock.crystalwars.CrystalWarsConstants;
import org.madblock.crystalwars.CrystalWarsPlugin;
import org.madblock.crystalwars.game.pointentities.team.CrystalPointEntity;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.nukkit.entity.EntityHumanPlus;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class EntityHumanCrystal extends EntityHumanPlus {

    protected static int ticker = 0;
    protected Random random;

    public EntityHumanCrystal(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.random = new Random();
    }

    @Override
    public boolean entityBaseTick() {

        if(random.nextInt(25) == 1) {
            float pitch = 0.75f + (random.nextFloat() / 2);

            getLevel().addSound(getPosition(), Sound.CHIME_AMETHYST_BLOCK, 0.8f, pitch, getViewers().values());
            //getLevel().addParticleEffect(getPosition().add(0, 2, 0), new BlockEnderChest(), -1, getLevel().getDimension(), getViewers().values());
        }

        return super.entityBaseTick();
    }

    public static EntityHumanCrystal getNewCrystal(Location location, String texture) {
        if(location == null) throw new IllegalArgumentException("Location must not be null.");
        if(location.getLevel() == null) throw new IllegalArgumentException("Location's level must not be null.");

        FullChunk chunk = location.getLevel().getChunk((int) Math.floor(location.getX()) >> 4, (int) Math.floor(location.getZ()) >> 4, true);
        Skin skin = getCrystalSkin(texture);


        CompoundTag nbt = new CompoundTag()
                .putList(new ListTag<>("Pos")
                        .add(new DoubleTag("", location.getX()))
                        .add(new DoubleTag("", location.getY()))
                        .add(new DoubleTag("", location.getZ())))
                .putList(new ListTag<DoubleTag>("Motion")
                        .add(new DoubleTag("", 0))
                        .add(new DoubleTag("", 0))
                        .add(new DoubleTag("", 0)))
                .putList(new ListTag<FloatTag>("Rotation")
                        .add(new FloatTag("", (float) location.getY()))
                        .add(new FloatTag("", (float) location.getX())))
                .putBoolean("npc", true)
                .putFloat("scale", 1)
                .putBoolean("ishuman", true);

        if(skin != null) {
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
        } else {
            CrystalWarsPlugin.getInstance().getLogger().warning("Missing skin for crystal wars.");
        }

        EntityHumanCrystal entityHumanCrystal = new EntityHumanCrystal(chunk, nbt);
        entityHumanCrystal.setClientSpawnAnimation(
                CrystalWarsConstants.ANIM_CRYSTAL_SPIN_ID,
                CrystalWarsConstants.ANIM_CRYSTAL_CONTROLLER
        );


        if(skin != null) entityHumanCrystal.setSkin(skin);
        else {
            CrystalWarsPlugin.getInstance().getLogger().warning("Missing skin for crystal wars.");
        }

        return entityHumanCrystal;
    }



    // -- I've moved the skin stuff to the bottom for now. :)

    // Hardcoded for convenience. We'll forget to move over the skin some day.
    // Once I add a way of loading skins to Utility rather than a set point entity,
    // I'll move back to loading files.
    protected static String geometryID = "geometry.steve";
    protected static String geometry = "";
    protected static Map<String, Skin> crystalSkins = new HashMap<>();

    public static Skin getCrystalSkin(String type) {
        if(crystalSkins.containsKey(type)) return crystalSkins.get(type);

        Skin skin = new Skin();
        InputStream gStr = CrystalWarsPlugin.getInstance().getResource("crystal/crystal.geo.json");
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(gStr));
            StringBuilder b = new StringBuilder();
            Iterator<String> lines = r.lines().iterator();

            while (lines.hasNext()) {
                b.append(lines.next());
                b.append("\n");
            }

            geometry = b.length() > 0 ? b.toString() : "";
            geometryID = "geometry.madblock.crystal";

        } catch (Exception err) {
            CrystalWarsPlugin.getInstance().getLogger().warning("Error loading custom skin model data for CrystalWars Repair Crystal skin.");
        }

        BufferedImage skinBaseData;
        InputStream sStr = CrystalWarsPlugin.getInstance().getResource("crystal/crystal."+type+".png");
        try {
            skinBaseData = ImageIO.read(sStr);
        } catch (Exception err) {
            CrystalWarsPlugin.getInstance().getLogger().warning("Error loading custom skin data for CrystalWars "+type+" Crystal skin.");
            return null;
        }
        if(geometry.length() == 0) return null;

        skin.setGeometryData(geometry);
        skin.setGeometryName(geometryID);
        skin.setSkinData(skinBaseData);
        skin.setTrusted(true);
        skin.setSkinId(UUID.randomUUID() + String.valueOf(ticker++));
        crystalSkins.put(type, skin);
        CrystalWarsPlugin.getInstance().getLogger().info(TextFormat.GREEN + "Successfully created CW Crystal skin of type: " + type);
        return skin;
    }
}
