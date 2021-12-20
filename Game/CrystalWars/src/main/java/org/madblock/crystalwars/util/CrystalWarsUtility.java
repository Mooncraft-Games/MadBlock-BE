package org.madblock.crystalwars.util;

import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.DyeColor;
import org.madblock.newgamesapi.util.Utility;
import org.madblock.newgamesapi.team.Team;

public class CrystalWarsUtility {
    public static Item makeUnbreakable(Item item) {
        CompoundTag tag = item.hasCompoundTag() ? item.getNamedTag() : new CompoundTag();
        tag.putBoolean("Unbreakable", true);
        item.setCompoundTag(tag);
        return item;
    }

    public static DyeColor resolveTeamColor(Team team) {
        DyeColor dyeColor;
        switch (team.getId()) {
            case "red":
                dyeColor = DyeColor.RED;
                break;
            case "blue":
                dyeColor = DyeColor.BLUE;
                break;
            case "green":
                dyeColor = DyeColor.GREEN;
                break;
            case "yellow":
                dyeColor = DyeColor.YELLOW;
                break;
            default:
                dyeColor = DyeColor.WHITE;
                break;
        }
        return dyeColor;
    }

    public static String generateCrystalTeamIcon(Team team, boolean hasCrystal) {
        String icon;
        switch (team.getId()) {
            case "red":
                icon = hasCrystal ? Utility.ResourcePackCharacters.LARGE_SQUARE_RED
                        : Utility.ResourcePackCharacters.NO_SIGN_RED;
                break;
            case "blue":
                icon = hasCrystal ? Utility.ResourcePackCharacters.LARGE_SQUARE_BLUE
                        : Utility.ResourcePackCharacters.NO_SIGN_BLUE;
                break;
            case "green":
                icon = hasCrystal ? Utility.ResourcePackCharacters.LARGE_SQUARE_GREEN
                        : Utility.ResourcePackCharacters.NO_SIGN_GREEN;
                break;
            case "yellow":
                icon = hasCrystal ? Utility.ResourcePackCharacters.LARGE_SQUARE_YELLOW
                        : Utility.ResourcePackCharacters.NO_SIGN_YELLOW;
                break;
            default:
                icon = hasCrystal ? Utility.ResourcePackCharacters.LARGE_SQUARE_WHITE
                        : Utility.ResourcePackCharacters.NO_SIGN_WHITE;
                break;
        }
        return icon;
    }
}