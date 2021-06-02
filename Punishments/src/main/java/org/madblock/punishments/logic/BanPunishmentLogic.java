package org.madblock.punishments.logic;

import cn.nukkit.Player;
import cn.nukkit.utils.TextFormat;
import org.madblock.punishments.api.PunishmentEntry;
import org.madblock.punishments.enums.PunishmentType;
import org.madblock.punishments.utils.Utility;

public class BanPunishmentLogic extends PunishmentLogic {

    public BanPunishmentLogic() {
        super(PunishmentType.BAN);
    }

    @Override
    public boolean onJoin(Player player, PunishmentEntry punishment) {
        String timeLeftMessage;
        if (punishment.getExpireAt() == 0) {
            timeLeftMessage = "permanently";
        } else {
            timeLeftMessage = "for " + Utility.generateTimeLeftMessage(punishment.getTimeUntilExpiry());
        }
        player.kick(
                "" + TextFormat.RED + TextFormat.BOLD + "You have been banned from MadBlock Games " + timeLeftMessage + "!\n" +
                        TextFormat.WHITE + punishment.getReason(),
                false
        );
        return false;
    }

    @Override
    public void onPunish(Player player, PunishmentEntry punishment) {
        String timeLeftMessage;
        if (punishment.getExpireAt() == 0) {
            timeLeftMessage = "permanently";
        } else {
            timeLeftMessage = "for " + Utility.generateTimeLeftMessage(punishment.getTimeUntilExpiry());
        }
        player.kick(
                "" + TextFormat.RED + TextFormat.BOLD + "You have been banned from MadBlock Games " + timeLeftMessage + "!\n" +
                        TextFormat.WHITE + punishment.getReason(),
                false
        );
    }
}
