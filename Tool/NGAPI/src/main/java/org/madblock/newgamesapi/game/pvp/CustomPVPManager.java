package org.madblock.newgamesapi.game.pvp;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemBlock;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.level.GameRule;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.math.Vector3;
import cn.nukkit.network.protocol.EntityEventPacket;
import cn.nukkit.network.protocol.SetEntityMotionPacket;
import org.madblock.newgamesapi.game.GameHandler;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class CustomPVPManager implements Listener {

    private final GameHandler handler;


    public CustomPVPManager(GameHandler handler) {
        this.handler = handler;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerVsPlayerDamage(EntityDamageByEntityEvent event) {
        CustomGamePVPSettings pvpSettings = handler.getGameID().getGameProperties().getCustomPvpSettings();
        if (
                pvpSettings.isEnabled() &&
                        event.getDamager() instanceof Player &&
                        event.getEntity() instanceof Player &&
                        handler.getPlayers().containsAll(Arrays.asList((Player)event.getDamager(), (Player)event.getEntity())) &&
                        event.getEntity().getLevel().getGameRules().getBoolean(GameRule.PVP)
        ) {
            Player attacker = (Player)event.getDamager();
            Player victim = (Player)event.getEntity();

            // Adjust damage according to settings
            if (!pvpSettings.areCriticalsAllowed() && !attacker.onGround) {
                event.setDamage(event.getDamage() / 1.5f);  // TODO: This isn't working. What is the criteria for a crit?
            }
            event.setDamage(event.getDamage() * pvpSettings.getDamageMultiplier());
            victim.noDamageTicks = pvpSettings.getNoHitTicks();

            // Apply damage modifications and call death manager before we apply kb
            this.updatePVPDamage(event);
            handler.getDeathManager().onGamePlayerDeath(event);
            if (event.isCancelled()) {
                return; // DeathManager doesn't want us to damage the player. Do not continue
            }
            event.setCancelled();   // We implement our own kb and damage. We don't want them to override our motion.

            // Damage the victim
            float damage = Math.max(event.getFinalDamage(), 0);
            float absorptionDamage = Math.min(damage, victim.getAbsorption());
            damage -= absorptionDamage;
            victim.setAbsorption(victim.getAbsorption() - absorptionDamage);
            victim.setHealth(victim.getHealth() - damage);

            // Break the victim's armor durability
            // From EntityHumanType.attack
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

            // Because we cancel the event at the end, we need to break down the weapon and apply enchants
            // so that everything goes as planned in Nukkit's original logic.
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


            // Apply custom KB to the victim
            Vector3 knockbackVector = pvpSettings.getKnockbackVector()
                    .multiply(event.getKnockBack());

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
    }

    /**
     * Replicates internal PVP logic to calculate new damage since we prevent it with our PVP listener
     * @param event
     */
    private void updatePVPDamage(EntityDamageByEntityEvent event) {
        Player victim = (Player)event.getEntity();

        EntityDamageEvent.DamageCause source = event.getCause();
        System.out.println(source);
        if (source == EntityDamageEvent.DamageCause.ENTITY_ATTACK || source == EntityDamageEvent.DamageCause.PROJECTILE) {
            // From EntityHumanType.attack
            int armorPoints = 0;
            int epf = 0;

            for (Item armor : victim.getInventory().getArmorContents()) {
                armorPoints += armor.getArmorPoints();
                for (Enchantment ench : armor.getEnchantments()) {
                    epf += ench.getProtectionFactor(event);
                }
            }

            event.setDamage(-event.getFinalDamage() * armorPoints * 0.08f, EntityDamageEvent.DamageModifier.ARMOR);
            System.out.println("armor = " + event.getDamage(EntityDamageEvent.DamageModifier.ARMOR));
            event.setDamage(-event.getFinalDamage() * Math.min(NukkitMath.ceilFloat(Math.min(epf, 25) * ((float) ThreadLocalRandom.current().nextInt(50, 100) / 100)), 20) * 0.08f,
                    EntityDamageEvent.DamageModifier.ARMOR_ENCHANTMENTS);
            System.out.println("armor = " + event.getDamage(EntityDamageEvent.DamageModifier.ARMOR_ENCHANTMENTS));
            System.out.println(event.getFinalDamage() + " final damage");
        }
    }


}
