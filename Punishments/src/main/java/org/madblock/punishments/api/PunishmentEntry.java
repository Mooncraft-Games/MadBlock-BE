package org.madblock.punishments.api;

import org.madblock.punishments.enums.PunishmentType;

import java.util.Date;

/**
 * Represents a punishment currently in the database.
 */
public class PunishmentEntry {

    private final int id;

    private volatile long expiresAt;

    private final long issuedAt;

    private final String reason;

    private volatile String removedReason;

    private volatile String removedBy;

    private final String issuedBy;

    private final PunishmentType type;

    private final String code;

    public PunishmentEntry(int id, String reason, long issuedAt, long expiresAt, PunishmentType type, String code, String removedBy, String removedReason, String issuedBy) {
        this.id = id;
        this.reason = reason;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.type = type;
        this.removedBy = removedBy;
        this.removedReason = removedReason;
        this.issuedBy = issuedBy;
        this.code = code;
    }

    public int getId () {
        return this.id;
    }

    public long getExpireAt () {
        return this.expiresAt;
    }

    public long getIssuedAt () {
        return this.issuedAt;
    }

    public String getRemovedBy () {
        return this.removedBy;
    }

    public String getCode () {
        return this.code;
    }

    public String getIssuedBy () {
        return this.issuedBy;
    }

    public String getRemovedReason () {
        return this.removedReason;
    }

    public long getTimeUntilExpiry () {
        long currentTime = new Date().getTime();
        if (currentTime > this.expiresAt) {
            return 0;
        } else {
            return this.expiresAt - currentTime;
        }
    }

    public boolean isExpired () {
        return this.getTimeUntilExpiry() == 0 && this.expiresAt != 0;
    }
    
    public boolean isRemoved () {
        return this.expiresAt == -1;
    }

    public boolean isPermanent () {
        return !this.isExpired() && this.expiresAt == 0;
    }

    public String getReason () {
        return this.reason;
    }

    public PunishmentType getType () {
        return this.type;
    }

    /**
     * Called internally to update fields to mark this punishment as removed.
     */
    public void remove (String removedBy, String removedReason) {
        this.removedBy = removedBy;
        this.removedReason = removedReason;
        this.expiresAt = -1;
    }

}
