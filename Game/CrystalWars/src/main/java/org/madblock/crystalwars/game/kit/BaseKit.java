package org.madblock.crystalwars.game.kit;

import cn.nukkit.item.*;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.utils.DyeColor;
import org.madblock.crystalwars.game.CrystalWarsGame;
import org.madblock.crystalwars.game.upgrades.CrystalTeamUpgrade;
import org.madblock.crystalwars.util.CrystalWarsUtility;
import org.madblock.newgamesapi.kits.ExtendedKit;
import org.madblock.newgamesapi.kits.Kit;
import org.madblock.newgamesapi.team.Team;

import java.util.Optional;

public abstract class BaseKit extends Kit {
    @Override
    public Optional<Class<? extends ExtendedKit>> getExtendedKitFeatures() {
        return Optional.of(CWExtendedKit.class);
    }

    public static class CWExtendedKit extends ExtendedKit {
        @Override
        protected void onPrepareExtendedKit() {
            Team playerTeam = gameHandler.getPlayerTeam(getTarget()).get();

            DyeColor dyeColor = CrystalWarsUtility.resolveTeamColor(playerTeam);

            Item helmet = CrystalWarsUtility.makeUnbreakable(new ItemHelmetLeather().setColor(dyeColor));
            helmet.addEnchantment(Enchantment.get(Enchantment.ID_PROTECTION_ALL));

            Item chestplate = CrystalWarsUtility.makeUnbreakable(new ItemChestplateLeather().setColor(dyeColor));
            Item leggings = CrystalWarsUtility.makeUnbreakable(new ItemLeggingsLeather().setColor(dyeColor));
            Item boots = CrystalWarsUtility.makeUnbreakable(new ItemBootsLeather().setColor(dyeColor));

            CrystalWarsGame base = (CrystalWarsGame) gameHandler.getGameBehaviors();
            if (base.doesTeamHaveUpgrade(playerTeam, CrystalTeamUpgrade.PROTECTION_ONE)) {
                Enchantment protection = Enchantment.get(Enchantment.ID_PROTECTION_ALL);
                if (base.doesTeamHaveUpgrade(playerTeam, CrystalTeamUpgrade.PROTECTION_TWO)) {
                    protection.setLevel(2);
                }

                helmet.addEnchantment(protection);
                chestplate.addEnchantment(protection);
                leggings.addEnchantment(protection);
                boots.addEnchantment(protection);
            }

            Item sword = new ItemSwordWood();
            CrystalWarsUtility.makeUnbreakable(sword);
            if (base.doesTeamHaveUpgrade(playerTeam, CrystalTeamUpgrade.SHARPNESS_ONE)) {
                if (base.doesTeamHaveUpgrade(playerTeam, CrystalTeamUpgrade.SHARPNESS_TWO)) {
                    sword.addEnchantment(Enchantment.get(Enchantment.ID_DAMAGE_ALL).setLevel(2));
                } else {
                    sword.addEnchantment(Enchantment.get(Enchantment.ID_DAMAGE_ALL));
                }
            }

            target.getInventory().setItem(0, sword);

            target.getInventory().setHelmet(helmet);
            target.getInventory().setChestplate(chestplate);
            target.getInventory().setLeggings(leggings);
            target.getInventory().setBoots(boots);

            target.getInventory().sendContents(target);
        }
    }
}