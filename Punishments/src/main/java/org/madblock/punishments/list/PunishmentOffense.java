package org.madblock.punishments.list;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PunishmentOffense implements PunishmentOffenseListItem {

    private List<SubPunishmentOffense> subPunishmentOffenses = new ArrayList<>();

    private String name;

    private Optional<AdditionalFunction> additionalFunction = Optional.empty();

    public PunishmentOffense(String name) {
        this.name = name;
    }

    public PunishmentOffense(String name, AdditionalFunction additionalFunction) {
        this.name = name;
        this.additionalFunction = Optional.of(additionalFunction);
    }

    public PunishmentOffense addSubReason (SubPunishmentOffense offense) {
        this.subPunishmentOffenses.add(
                offense
        );
        return this;
    }

    public boolean hasSubReasons () {
        return this.subPunishmentOffenses.size() > 0;
    }

    public List<SubPunishmentOffense> getSubReasons () {
        return this.subPunishmentOffenses;
    }

    public String getName () {
        return this.name;
    }

    public Optional<AdditionalFunction> getAdditionalFunction () {
        return this.additionalFunction;
    }

    public enum AdditionalFunction {
        GAME_PROMPT,
        REASON_PROMPT,
        CHANGE_AND_APPEAL
    }

}
