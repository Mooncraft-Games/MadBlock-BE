package org.madblock.newgamesapi.game.deaths;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.entity.EntityDamageEvent;
import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.newgamesapi.team.Team;

import java.util.Optional;

public class DeathCause {

    protected Player victim;
    protected Team victimTeam;
    protected DeathCategory deathCategory;
    protected DeathSubCategory deathSubCategory;
    protected float finalDamage;

    protected Player killerPlayer;
    protected Entity killerEntity;
    protected Block killerBlock;

    protected MapRegion killingRegion;
    protected PointEntity killingPointEntity;

    protected EntityDamageEvent rawEvent;

    public DeathCause (Player victim, Team victimTeam, DeathCategory deathCategory, DeathSubCategory deathSubCategory, float finalDamage) {
        this.victim = victim;
        this.victimTeam = victimTeam;
        this.deathCategory = deathCategory;
        this.deathSubCategory = deathSubCategory;
        this.finalDamage = finalDamage;
    }

    public Player getVictim() { return victim; }
    public Team getVictimTeam() { return victimTeam; }
    public DeathCategory getDeathCategory() { return deathCategory; }
    public DeathSubCategory getDeathSubCategory() { return deathSubCategory; }
    public float getFinalDamage() { return finalDamage; }

    public Optional<Player> getKillerPlayer() { return Optional.ofNullable(killerPlayer); }
    public Optional<Entity> getKillerEntity() { return Optional.ofNullable(killerEntity); }
    public Optional<Block> getKillerBlock() { return Optional.ofNullable(killerBlock); }
    public Optional<MapRegion> getKillingRegion() { return Optional.ofNullable(killingRegion); }
    public Optional<PointEntity> getKillingPointEntity() { return Optional.ofNullable(killingPointEntity); }
    public Optional<EntityDamageEvent> getRawEvent() { return Optional.ofNullable(rawEvent); }

    public static DeathCauseBuilder builder(Player victim, Team victimTeam, DeathCategory deathCategory, DeathSubCategory subCategory, float finalDamage){
        return new DeathCauseBuilder(victim, victimTeam, deathCategory, subCategory, finalDamage);
    }

    public static class DeathCauseBuilder {
        protected Player victim;
        protected Team victimTeam;
        protected DeathCategory deathCategory;
        protected DeathSubCategory deathSubCategory;
        protected float finalDamage;

        protected Player killerPlayer;
        protected Entity killerEntity;
        protected Block killerBlock;

        protected MapRegion killingRegion;
        protected PointEntity killingPointEntity;

        protected EntityDamageEvent rawEvent;

        public DeathCauseBuilder(Player victim, Team victimTeam, DeathCategory deathCategory, DeathSubCategory subCategory, float finalDamage){
            this.victim = victim;
            this.victimTeam = victimTeam;
            this.deathCategory = deathCategory;
            this.deathSubCategory = subCategory;
            this.finalDamage = finalDamage;

            this.killerPlayer = null;
            this.killerEntity = null;
            this.killerBlock = null;
            this.killingRegion = null;
            this.killingPointEntity = null;
            this.rawEvent = null;
        }

        public DeathCause build() {
            DeathCause c = new DeathCause(victim, victimTeam, deathCategory, deathSubCategory, finalDamage);
            c.killerPlayer = killerPlayer;
            c.killerEntity = killerEntity;
            c.killerBlock = killerBlock;
            c.killingRegion = killingRegion;
            c.killingPointEntity = killingPointEntity;
            c.rawEvent = rawEvent;
            return c;
        }

        public DeathCauseBuilder setKillerPlayer(Player killerPlayer) { this.killerPlayer = killerPlayer; this.killerEntity = killerPlayer; return this; }
        public DeathCauseBuilder setKillerEntity(Entity killerEntity) { this.killerEntity = killerEntity; return this; }
        public DeathCauseBuilder setKillerBlock(Block killerBlock) { this.killerBlock = killerBlock; return this; }
        public DeathCauseBuilder setKillingRegion(MapRegion killingRegion) { this.killingRegion = killingRegion; return this; }
        public DeathCauseBuilder setKillingPointEntity(PointEntity killingPointEntity) { this.killingPointEntity = killingPointEntity; return this; }
        public DeathCauseBuilder setRawEvent(EntityDamageEvent rawEvent) { this.rawEvent = rawEvent; return this; }

    }

}
