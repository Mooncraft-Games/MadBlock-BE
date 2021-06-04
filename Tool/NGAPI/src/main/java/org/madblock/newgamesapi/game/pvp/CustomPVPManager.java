package org.madblock.newgamesapi.game.pvp;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemBlock;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.level.GameRule;
import cn.nukkit.math.Vector3;
import cn.nukkit.network.protocol.EntityEventPacket;
import cn.nukkit.network.protocol.SetEntityMotionPacket;
import org.madblock.newgamesapi.game.GameHandler;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class CustomPVPManager implements Listener {

    private final GameHandler handler;

    private CustomGamePVPSettings settings;


    public CustomPVPManager(GameHandler handler) {
        this.handler = handler;
        this.settings = handler.getGameID().getGameProperties().getCustomPvpSettings().clone();
    }

    public CustomGamePVPSettings getSettings() {
        return settings;
    }

    public void setSettings(CustomGamePVPSettings settings) {
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerVsPlayerDamage(EntityDamageByEntityEvent event) {
        if (
                settings.isEnabled() &&
                event.getDamager() instanceof Player &&
                event.getEntity() instanceof Player &&
                handler.getPlayers().containsAll(Arrays.asList((Player)event.getDamager(), (Player)event.getEntity())) &&
                event.getEntity().getLevel().getGameRules().getBoolean(GameRule.PVP)
        ) {
            Player attacker = (Player)event.getDamager();
            Player victim = (Player)event.getEntity();

            // Adjust damage according to settings
            event.setDamage(event.getDamage() * settings.getDamageMultiplier());

            // Calculate the damage and call the DeathManager to see if this event should be prevented
            float damage = applyDamageModifiers(victim, event.getDamage());
            handler.getDeathManager().onGamePlayerDeath(new EntityDamageByEntityEvent(attacker, victim, event.getCause(), damage, event.getKnockBack()));
            if (event.isCancelled()) {
                return; // DeathManager doesn't want us to damage the player. Do not continue
            }
            event.setCancelled();   // We implement our own kb and damage. We don't want them to override our motion.

            // Damage the victim
            float absorptionDamage = Math.min(damage, victim.getAbsorption());
            damage -= absorptionDamage;
            victim.setAbsorption(victim.getAbsorption() - absorptionDamage);
            victim.setHealth(victim.getHealth() - damage);

            // Damage the items/armor interacted with
            damageVictimArmor(attacker, victim);
            damageAttackerItem(attacker, victim);

            // Apply knockback
            victim.noDamageTicks = settings.getNoHitTicks();
            knockBack(attacker, victim, event.getKnockBack());

        }
    }

    /**
     * Apply the custom KB to a player
     * @param attacker
     * @param victim
     * @param knockback knockback value provided in the EntityDamageByEntityEvent
     */
    private void knockBack(Player attacker, Player victim, double knockback) {

        Vector3 knockbackVector = settings.getKnockbackVector()
                .multiply(knockback);

        // Based off of the knockBack method for LivingEntity
        double distanceX = victim.getX() - attacker.getX();
        double distanceZ = victim.getZ() - attacker.getZ();
        double distance = Math.sqrt(distanceX * distanceX + distanceZ * distanceZ);

        double x = knockbackVector.getX() * (1 / distance) * distanceX;
        double y = knockbackVector.getY();
        double z = knockbackVector.getZ() * (1 / distance) * distanceZ;

        // We don't use setMotion b/c it was not as smooth as I would have liked. (rubberbanded)
        SetEntityMotionPacket entityMotionPacket = new SetEntityMotionPacket();
        entityMotionPacket.eid = victim.getId();
        entityMotionPacket.motionX = (float)x;
        entityMotionPacket.motionY = (float)y;
        entityMotionPacket.motionZ = (float)z;
        victim.dataPacket(entityMotionPacket);

        EntityEventPacket hurtAnimationPacket = new EntityEventPacket();
        hurtAnimationPacket.eid = victim.getId();
        hurtAnimationPacket.event = EntityEventPacket.HURT_ANIMATION;
        victim.dataPacket(hurtAnimationPacket);
        Server.broadcastPacket(victim.getViewers().values(), hurtAnimationPacket);
    }

    /**
     * Damages the durability of the victim's armor and apply any post attacks of the armor to the attacker.
     * @param attacker
     * @param victim
     */
    private static void damageVictimArmor(Player attacker, Player victim) {
        // From EntityHumanType.attack since we prevent this behavior through cancelling the event
        for (int slot = 0; slot < 4; slot++) {
            Item armor = victim.getInventory().getArmorItem(slot);

            if (armor.hasEnchantments()) {
                for (Enchantment enchantment : armor.getEnchantments()) {
                    enchantment.doPostAttack(attacker, victim);
                }

                Enchantment durability = armor.getEnchantment(Enchantment.ID_DURABILITY);
                if (durability != null && durability.getLevel() > 0 && (100 / (durability.getLevel() + 1)) <= ThreadLocalRandom.current().nextInt(100))
                    continue;
            }
            if (armor.isUnbreakable()) {
                continue;
            }
            armor.setDamage(armor.getDamage() + 1);
            if (armor.getDamage() >= armor.getMaxDurability()) {
                victim.getInventory().setArmorItem(slot, new ItemBlock(Block.get(BlockID.AIR)));
            } else {
                victim.getInventory().setArmorItem(slot, armor, true);
            }
        }
    }

    /**
     * Damages the durablity of the attacker's item and applies post attacks to the victim.
     * @param attacker
     * @param victim
     */
    private static void damageAttackerItem(Player attacker, Player victim) {
        Item itemInHand = attacker.getInventory().getItemInHand();

        for (Enchantment enchantment : itemInHand.getEnchantments()) {
            enchantment.doPostAttack(attacker, victim);
        }
        if (itemInHand.isTool() && attacker.isSurvival()) {
            if (itemInHand.useOn(victim) && itemInHand.getDamage() >= itemInHand.getMaxDurability()) {
                attacker.getInventory().setItemInHand(new ItemBlock(Block.get(BlockID.AIR)));
            } else {
                attacker.getInventory().setItemInHand(itemInHand);
            }
        }
    }

    /**
     * Return the reduced amount of damage done by an attack
     * @param victim
     * @param rawDamage
     * @return new damage
     */
    private float applyDamageModifiers(Player victim, float rawDamage) {
        int protection = 0;
        for (Item armor : victim.getInventory().getArmorContents()) {
            protection += armor.getArmorPoints();
            if (armor.hasEnchantment(Enchantment.ID_PROTECTION_ALL)) {
                protection += (armor.getEnchantment(Enchantment.ID_PROTECTION_ALL).getLevel() + 1);
            }
        }
        return Math.max(0, rawDamage - (protection * 0.2f * settings.getProtectionMultiplier()));
    }


}
