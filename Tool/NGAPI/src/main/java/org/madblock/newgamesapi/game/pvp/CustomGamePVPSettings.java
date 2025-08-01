package org.madblock.newgamesapi.game.pvp;

import cn.nukkit.math.Vector3;

/**
 *  Responsible for customizations to PVP in games.
 */
public class CustomGamePVPSettings implements Cloneable {

    public static final CustomGamePVPSettings DISABLED = new CustomGamePVPSettings().setEnabled(false);

    // General
    private boolean enabled;

    // KB
    private Vector3 defaultKnockback;

    // PVP
    private float damageMultiplier;
    private float protectionMultiplier;
    private int noHitTicks;


    public CustomGamePVPSettings() {
        enabled = true;

        defaultKnockback = new Vector3(1.25d, 1.4d, 1.25d);

        damageMultiplier = 1;
        protectionMultiplier = 1;
        noHitTicks = 9;
    }

    /**
     * If custom pvp behavior is toggled.
     * @return status
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Retrieve the knockback motion to be given to players when hit
     * @return knockback vector
     */
    public Vector3 getKnockbackVector() {
        return defaultKnockback;
    }

    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    public float getProtectionMultiplier() {
        return protectionMultiplier;
    }

    /**
     * Retrieve the amount of ticks players are immune to damage for when attacked
     * @return immunity ticks
     */
    public int getNoHitTicks() {
        return noHitTicks;
    }


    /**
     * Turn the custom PVP behavior on or off.
     * Default Nukkit PVP behavior will be used if this is false.
     * This should be false if you implement your own knockback logic or PVP features.
     * @param enabled
     * @return self for chaining
     */
    public CustomGamePVPSettings setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Set the amount of motion to be given to players when hit.
     * @param knockbackVector the motion
     * @return self for chaining
     */
    public CustomGamePVPSettings setDefaultKnockback(Vector3 knockbackVector) {
        this.defaultKnockback = knockbackVector;
        return this;
    }

    public CustomGamePVPSettings setDamageMultiplier(float multiplier) {
        damageMultiplier = multiplier;
        return this;
    }

    public CustomGamePVPSettings setProtectionMultiplier(float multiplier) {
        protectionMultiplier = multiplier;
        return this;
    }

    /**
     * Set the amount of ticks that players are immune for when hit
     * @param noHitTicks immunity ticks
     * @return self for chaining
     */
    public CustomGamePVPSettings setNoHitTicks(int noHitTicks) {
        this.noHitTicks = noHitTicks;
        return this;
    }

    @Override
    public CustomGamePVPSettings clone() {
        try {
            return (CustomGamePVPSettings)super.clone();
        } catch (CloneNotSupportedException exception) {
            return new CustomGamePVPSettings()
                    .setDamageMultiplier(getDamageMultiplier())
                    .setProtectionMultiplier(getProtectionMultiplier())
                    .setDefaultKnockback(getKnockbackVector())
                    .setNoHitTicks(getNoHitTicks())
                    .setEnabled(isEnabled());
        }
    }
}
