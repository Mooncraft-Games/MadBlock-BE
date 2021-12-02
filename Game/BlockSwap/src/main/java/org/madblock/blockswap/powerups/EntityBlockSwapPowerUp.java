package org.madblock.blockswap.powerups;

import cn.nukkit.Player;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.passive.EntityChicken;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import org.madblock.blockswap.behaviours.BlockSwapGameBehaviour;
import org.madblock.newgamesapi.team.TeamPresets;

public class EntityBlockSwapPowerUp extends EntityHuman {

    private static final int TARGET_SCALE_SIZE = 2;
    private static final int ROTATION_SPEED = 3;

    protected final BlockSwapGameBehaviour behaviour;

    public EntityBlockSwapPowerUp(BlockSwapGameBehaviour behaviour, FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.behaviour = behaviour;

        this.setScale(0.5f);
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        // Rotate the power up
        double rotationSpeed = 3 / this.getScale() * ROTATION_SPEED;
        double newYaw = this.getYaw() + rotationSpeed >= 360 ? 0 : this.getYaw() + rotationSpeed;
        this.setRotation(newYaw, this.getPitch());

        if (this.getScale() - TARGET_SCALE_SIZE <= 0) {
            this.setScale(Math.min(TARGET_SCALE_SIZE, this.getScale() * 1.1f));
        }

        return super.entityBaseTick(tickDiff);
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (source instanceof EntityDamageByEntityEvent && ((EntityDamageByEntityEvent) source).getDamager() instanceof Player) {
            Player player = (Player) ((EntityDamageByEntityEvent) source).getDamager();
            boolean canPlayerInteract = this.behaviour.getSessionHandler().getPlayers().contains(player)
                    && !this.behaviour.getSessionHandler().getTeams().get(TeamPresets.DEAD_TEAM_ID).getPlayers().contains(player);

            if (canPlayerInteract) {
                this.behaviour.getPowerUpManager().onEntityHit(player, this);
                return false;
            }
        } else if (source.getCause() == EntityDamageEvent.DamageCause.LIGHTNING
                || source.getCause() == EntityDamageEvent.DamageCause.FIRE
                || source.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
            source.getEntity().extinguish();
            return false;
        }

        return super.attack(source);
    }

}
