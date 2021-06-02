package org.madblock.crystalwars.util;

import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.DyeColor;
import org.madblock.newgamesapi.team.Team;

/**
 * @author Nicholas
 */
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
}