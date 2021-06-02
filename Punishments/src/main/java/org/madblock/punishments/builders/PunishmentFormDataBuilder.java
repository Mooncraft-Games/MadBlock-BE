package org.madblock.punishments.builders;

import org.madblock.punishments.forms.PunishmentFormManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PunishmentFormDataBuilder {

    private int formId;

    private String offenseType;

    private String target;

    private Map<String, String> parameters = new HashMap<>();

    private Optional<Integer> parentOffenseIndex = Optional.empty();

    public PunishmentFormDataBuilder setFormId (int id) {
        this.formId = id;
        return this;
    }

    public PunishmentFormDataBuilder setOffenseType (String offenseType) {
        this.offenseType = offenseType;
        return this;
    }

    public PunishmentFormDataBuilder setTarget(String playerName) {
        this.target = playerName;
        return this;
    }

    public PunishmentFormDataBuilder setParameter (String key, String val) {
        this.parameters.put(key, val);
        return this;
    }

    public PunishmentFormManager.PunishmentFormData build () {
        return new PunishmentFormManager.PunishmentFormData(this.target, this.formId, this.offenseType, this.parameters);
    }

}
