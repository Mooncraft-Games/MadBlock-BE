package org.madblock.newgamesapi.map.pointentities.defaults;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.item.EntityFirework;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.item.ItemFirework;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.utils.DyeColor;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.pointentities.PointEntityCallData;
import org.madblock.newgamesapi.map.pointentities.PointEntityType;
import org.madblock.newgamesapi.map.types.PointEntity;

import java.util.*;

public class PointEntityTypeFirework extends PointEntityType implements Listener {

    public static final String ID = "point_firework";
    public static final String FUNC_SPAWN = "spawn";

    public PointEntityTypeFirework(GameHandler gameHandler) {
        super("point_firework", gameHandler);
    }

    @Override
    public void onRegister() {
        NewGamesAPI1.get().getServer().getPluginManager().registerEvents(this, NewGamesAPI1.get());
        addFunction(FUNC_SPAWN, this::spawnFirework);
    }

    @Override
    public void onUnregister() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void onAddPointEntity(PointEntity entity) { super.onAddPointEntity(entity); }


    public void spawnFirework(PointEntityCallData data) {
        PointEntity entity = data.getPointEntity();

        Vector3 position = entity.positionToVector3().add(!entity.isAccuratePosition() ? 0.5d : 0, 0, !entity.isAccuratePosition() ? 0.5d : 0);
        FullChunk chunk = data.getLevel().getChunk((int) Math.floor(position.getX() / 16), (int) Math.floor(position.getZ() / 16), true);

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
                        .add(new FloatTag("", 0f)));

        ItemFirework firework = new ItemFirework();
        Random random = new Random();

        // properties:

        int maxExplosions = Math.max(1, entity.getIntegerProperties().getOrDefault("max_explosions", 5));
        int maxExplosionColours = Math.max(1, entity.getIntegerProperties().getOrDefault("max_explosion_colours", 3));
        int maxFades = Math.max(1, entity.getIntegerProperties().getOrDefault("max_explosion_colours", 2));
        String colour = data.getParameters().getOrDefault("colour", data.getParameters().get("color")); // account for american spelling.

        if (colour == null) {
            colour = entity.getStringProperties().getOrDefault("colour", entity.getStringProperties().get("color"));
        }

        if((colour == null) || (!Palettes.getPalette(colour).isPresent())) { // Still null or doesn't exist, use fallback.
            colour = Palettes.BRIGHT;
        }

        ArrayList<ItemFirework.FireworkExplosion.ExplosionType> exTypes = new ArrayList<>();

        if(entity.getBooleanProperties().getOrDefault("small_ball", true)) exTypes.add(ItemFirework.FireworkExplosion.ExplosionType.SMALL_BALL);
        if(entity.getBooleanProperties().getOrDefault("large_ball", true)) exTypes.add(ItemFirework.FireworkExplosion.ExplosionType.LARGE_BALL);
        if(entity.getBooleanProperties().getOrDefault("star", true)) exTypes.add(ItemFirework.FireworkExplosion.ExplosionType.STAR_SHAPED);
        if(entity.getBooleanProperties().getOrDefault("burst", true)) exTypes.add(ItemFirework.FireworkExplosion.ExplosionType.BURST);
        if(entity.getBooleanProperties().getOrDefault("creeper", false)) exTypes.add(ItemFirework.FireworkExplosion.ExplosionType.CREEPER_SHAPED); // Harder to work with, don't include be default.

        // end

        int loopCount = random.nextInt(maxExplosions);
        DyeColor[] pal = Palettes.getPalette(colour).get();

        if (pal.length == 0) return;
        firework.clearExplosions();

        for(int i = 0; i < loopCount; i++) {
            firework.addExplosion(genExplosion(random, maxExplosionColours, maxFades, pal, exTypes));
        }

        EntityFirework fwEntity = (EntityFirework) Entity.createEntity("Firework", chunk, nbt);

        if(fwEntity != null) {
            fwEntity.setFirework(firework);
            fwEntity.spawnToAll();
        }
    }

    private static ItemFirework.FireworkExplosion genExplosion(Random random, int maxColours, int maxFades, DyeColor[] colourPool, ArrayList<ItemFirework.FireworkExplosion.ExplosionType> exTypes) {
        ItemFirework.FireworkExplosion ex = new ItemFirework.FireworkExplosion();

        int colourCount = random.nextInt(maxColours);
        for (int i = 0; i < colourCount; i++) {
            int randomIndex = random.nextInt(colourPool.length);
            DyeColor explosionColour = colourPool[randomIndex];

            ex.addColor(explosionColour);
        }

        int fadeCount = maxFades == 0 ? 0 : random.nextInt(maxFades);
        for (int i = 0; i < fadeCount; i++) {
            int randomIndex = random.nextInt(colourPool.length);
            DyeColor explosionColour = colourPool[randomIndex];

            ex.addFade(explosionColour);
        }

        ex.setFlicker(random.nextBoolean());
        ex.setTrail(random.nextBoolean());

        if(exTypes.size() == 0) {
            ex.type(ItemFirework.FireworkExplosion.ExplosionType.BURST);
        } else {
            int typeIndex = exTypes.size() == 1 ? 0 : random.nextInt(exTypes.size());
            ex.type(exTypes.get(typeIndex));
        }

        return ex;
    }

    public static class Palettes {

        private static HashMap<String, DyeColor[]> palettes;

        public static final String ALL = "all";
        public static final String BRIGHT = "bright";

        public static final String MINEPLEX = "infringment";
        public static final String MADBLOCK = "madblock";

        public static final String HONEY = "yellow";
        public static final String AQUATIC = "blue";
        public static final String ROSE = "red";
        public static final String RADIOACTIVE = "green";

        static {
            palettes = new HashMap<>();

            setPalette(ALL, DyeColor.BLACK, DyeColor.RED, DyeColor.GREEN, DyeColor.BROWN, DyeColor.BLUE, DyeColor.PURPLE, DyeColor.CYAN, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.PINK, DyeColor.LIME, DyeColor.YELLOW, DyeColor.LIGHT_BLUE, DyeColor.MAGENTA, DyeColor.ORANGE, DyeColor.WHITE);
            setPalette(BRIGHT, DyeColor.LIGHT_BLUE, DyeColor.LIME, DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.RED);

            setPalette(MINEPLEX, DyeColor.ORANGE, DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.WHITE, DyeColor.WHITE);
            setPalette(MADBLOCK, DyeColor.RED, DyeColor.RED, DyeColor.PINK, DyeColor.GRAY, DyeColor.BLACK);

            setPalette(HONEY, DyeColor.YELLOW, DyeColor.YELLOW, DyeColor.YELLOW, DyeColor.ORANGE, DyeColor.ORANGE, DyeColor.WHITE);
            setPalette(AQUATIC, DyeColor.LIGHT_BLUE, DyeColor.LIGHT_BLUE, DyeColor.LIGHT_BLUE, DyeColor.BLUE, DyeColor.BLUE, DyeColor.WHITE);
            setPalette(ROSE, DyeColor.RED, DyeColor.RED, DyeColor.RED, DyeColor.PINK, DyeColor.PINK, DyeColor.WHITE);
            setPalette(RADIOACTIVE, DyeColor.LIME, DyeColor.LIME, DyeColor.LIME, DyeColor.GREEN, DyeColor.GREEN, DyeColor.WHITE);
        }

        public static void setPalette(String id, DyeColor... palette) {
            palettes.put(id.toLowerCase(), palette);
        }


        public static final Optional<DyeColor[]> getPalette(String palette) {
            return Optional.ofNullable(palettes.get(palette.toLowerCase()));
        }

        public static ArrayList<String> getPalettes() {
            return new ArrayList<>(palettes.keySet());
        }
    }
}
