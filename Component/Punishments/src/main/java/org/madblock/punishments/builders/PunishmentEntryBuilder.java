package org.madblock.punishments.builders;

import org.madblock.punishments.api.PunishmentEntry;
import org.madblock.punishments.enums.PunishmentType;

public class PunishmentEntryBuilder {

    private int id;

    private Long expiresAt = null;

    private long issuedAt;

    private String reason;

    private String removedReason;

    private String removedBy;

    private String issuedBy;

    private String code;

    private PunishmentType type;

    public PunishmentEntryBuilder setId (int id) {
        this.id = id;
        return this;
    }

    public PunishmentEntryBuilder setExpiresAt (long expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public PunishmentEntryBuilder setIssuedAt (long issuedAt) {
        this.issuedAt = issuedAt;
        return this;
    }

    public PunishmentEntryBuilder setIssuedBy (String issuedBy) {
        this.issuedBy = issuedBy;
        return this;
    }

    public PunishmentEntryBuilder setCode (String code) {
        this.code = code;
        return this;
    }

    public PunishmentEntryBuilder setReason (String reason) {
        this.reason = reason;
        return this;
    }

    public PunishmentEntryBuilder setRemovedReason (String removedReason) {
        this.removedReason = removedReason;
        return this;
    }

    public PunishmentEntryBuilder setRemovedBy (String playerName) {
        this.removedBy = playerName;
        return this;
    }

    public PunishmentEntryBuilder setType (PunishmentType type) {
        this.type = type;
        return this;
    }

    public PunishmentEntry build () {
        return new PunishmentEntry(this.id, this.reason, this.issuedAt, this.expiresAt, this.type, this.code, this.removedBy, this.removedReason, this.issuedBy);
    }

}
