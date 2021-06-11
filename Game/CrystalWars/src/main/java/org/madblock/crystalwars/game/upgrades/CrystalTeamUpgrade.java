package org.madblock.crystalwars.game.upgrades;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Nicholas
 */
@AllArgsConstructor @Getter
public enum CrystalTeamUpgrade {
    SHARPNESS_ONE("Sharpness 1"),
    SHARPNESS_TWO("Sharpness 2"),
    PROTECTION_ONE("Protection 1"),
    PROTECTION_TWO("Protection 2"),
    RESOURCES_ONE("Resources 1"),
    RESOURCES_TWO("Resources 2");

    private final String name;
}