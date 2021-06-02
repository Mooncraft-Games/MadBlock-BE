package org.madblock.punishments.list;

import java.util.Optional;

public interface PunishmentOffenseListItem {
    String getName ();
    Optional<PunishmentOffense.AdditionalFunction> getAdditionalFunction ();
}
