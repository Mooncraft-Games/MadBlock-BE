package org.madblock.crystalwars.game.entities;

import cn.nukkit.Player;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Sound;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import org.madblock.newgamesapi.nukkit.entity.EntityHumanPlus;

import java.util.*;
import java.util.stream.Collectors;

public class EntityHumanCrystal extends EntityHumanPlus {

    protected Random random;

    public EntityHumanCrystal(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.random = new Random();
    }

    @Override
    public boolean entityBaseTick() {

        if(random.nextInt(25) == 1) {
            float pitch = 0.75f + (random.nextFloat() / 2);

            Set<Player> nearby = getViewers().values().stream().filter(player -> distance(player) > 20).collect(Collectors.toSet());

            getLevel().addSound(getPosition(), Sound.CHIME_AMETHYST_BLOCK, 0.5f, pitch, nearby);
            getLevel().addParticleEffect(getPosition().add(0, 2, 0), ParticleEffect.ENCHANTING_TABLE_PARTICLE, -1, getLevel().getDimension(), getViewers().values());
        }

        return super.entityBaseTick();
    }

    protected static final String GEO_ID = "";
    protected static final String GEO = "";
}
