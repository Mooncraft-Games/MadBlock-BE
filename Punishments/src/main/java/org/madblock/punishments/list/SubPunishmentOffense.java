package org.madblock.punishments.list;

import java.util.Optional;

public class SubPunishmentOffense implements PunishmentOffenseListItem {

    private String name;

    private Optional<PunishmentOffense.AdditionalFunction> additionalFunction = Optional.empty();

    public SubPunishmentOffense (String name) {
        this.name = name;
    }

    public SubPunishmentOffense (String name, PunishmentOffense.AdditionalFunction additionalFunction) {
        this.name = name;
        this.additionalFunction = Optional.of(additionalFunction);
    }

    public String getName () {
        return this.name;
    }

    public Optional<PunishmentOffense.AdditionalFunction> getAdditionalFunction () {
        return this.additionalFunction;
    }

}
