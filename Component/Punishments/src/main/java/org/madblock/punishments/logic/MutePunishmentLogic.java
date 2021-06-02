package org.madblock.punishments.logic;

import cn.nukkit.Player;
import cn.nukkit.utils.TextFormat;
import org.madblock.punishments.api.PunishmentEntry;
import org.madblock.punishments.enums.PunishmentType;
import org.madblock.punishments.utils.Utility;

public class MutePunishmentLogic extends PunishmentLogic {

    public MutePunishmentLogic() {
        super(PunishmentType.MUTE);
    }

    @Override
    public boolean onChat(Player player, PunishmentEntry punishment) {
        String timeLeftMessage;
        if (punishment.getExpireAt() == 0) {
            timeLeftMessage = "permanently";
        } else {
            timeLeftMessage = "for " + Utility.generateTimeLeftMessage(punishment.getTimeUntilExpiry());
        }
        player.sendMessage(Utility.generateServerMessage("MUTE", TextFormat.DARK_RED, "You are currently muted " + timeLeftMessage + ". " + punishment.getReason(), TextFormat.GRAY));
        return false;
    }
}
