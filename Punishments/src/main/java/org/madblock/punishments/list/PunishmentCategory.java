package org.madblock.punishments.list;

import org.madblock.punishments.enums.PunishmentType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PunishmentCategory {

    private final long duration;

    private final PunishmentType type;

    private final String code;

    private final int severity;

    private final String category;

    private final String permissionRequired;

    private List<PunishmentOffense> offenses = new ArrayList<>();

    public PunishmentCategory (String category, int severity, long duration, PunishmentType type)  {
        this.duration = duration;
        this.type = type;
        this.code = category + severity;
        this.category = category;
        this.severity = severity;
        this.permissionRequired = null;
    }

    public PunishmentCategory (String category, int severity, long duration, PunishmentType type, String permission) {
        this.duration = duration;
        this.type = type;
        this.code = category + severity;
        this.category = category;
        this.severity = severity;
        this.permissionRequired = permission;
    }

    public PunishmentCategory (String code, long duration, PunishmentType type) {
        this.duration = duration;
        this.type = type;
        this.code = code;
        this.severity = 0;
        this.category = code;
        this.permissionRequired = null;
    }

    public PunishmentCategory (String code, long duration, PunishmentType type, String permission) {
        this.duration = duration;
        this.type = type;
        this.code = code;
        this.severity = 0;
        this.category = code;
        this.permissionRequired = permission;
    }

    public PunishmentCategory addOffense (PunishmentOffense offense) {
        this.offenses.add(offense);
        return this;
    }

    public List<PunishmentOffense> getOffenses () {
        return this.offenses;
    }

    public long getDuration () {
        return this.duration;
    }

    public boolean hasLevels () {
        return this.severity > 0;
    }

    public int getSeverity () {
        return this.severity;
    }

    public Optional<String> getPermissionRequired () {
        return Optional.ofNullable(this.permissionRequired);
    }

    public String getCategory () {
        return this.category;
    }

    public String getCode () {
        return this.code;
    }

    public PunishmentType getType() {
        return this.type;
    }

}
