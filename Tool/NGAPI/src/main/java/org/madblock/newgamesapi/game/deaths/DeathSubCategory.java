package org.madblock.newgamesapi.game.deaths;

import cn.nukkit.event.entity.EntityDamageEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum DeathSubCategory {
    FIRE,
    LAVA,
    LIGHTNING,
    FALL,
    EXPLOSION,
    MAGIC,
    PROJECTILE,
    ATTACK,
    HUNGER,
    DROWNING,
    SUFFOCATING,
    PLUGIN,
    GAME,
    BORDER,
    MISC;

    static {
        HashMap<EntityDamageEvent.DamageCause, DeathSubCategory> setup = new HashMap<>();

        setup.put(EntityDamageEvent.DamageCause.FIRE, FIRE);
        setup.put(EntityDamageEvent.DamageCause.FIRE_TICK, FIRE);
        setup.put(EntityDamageEvent.DamageCause.LAVA, LAVA);
        setup.put(EntityDamageEvent.DamageCause.LIGHTNING, LIGHTNING);
        setup.put(EntityDamageEvent.DamageCause.FALL, FALL);
        setup.put(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION, EXPLOSION);
        setup.put(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, EXPLOSION);
        setup.put(EntityDamageEvent.DamageCause.MAGIC, MAGIC);
        setup.put(EntityDamageEvent.DamageCause.PROJECTILE, PROJECTILE);
        setup.put(EntityDamageEvent.DamageCause.ENTITY_ATTACK, ATTACK);
        setup.put(EntityDamageEvent.DamageCause.HUNGER, HUNGER);
        setup.put(EntityDamageEvent.DamageCause.DROWNING, DROWNING);
        setup.put(EntityDamageEvent.DamageCause.SUFFOCATION, SUFFOCATING);
        setup.put(EntityDamageEvent.DamageCause.CUSTOM, PLUGIN);

        convertTable = Collections.unmodifiableMap(setup);
    }

    private static final Map<EntityDamageEvent.DamageCause, DeathSubCategory> convertTable;

    public static DeathSubCategory getCategoryFromDamageCause(EntityDamageEvent.DamageCause damageCause){
        return convertTable.getOrDefault(damageCause, MISC);
    }
}
