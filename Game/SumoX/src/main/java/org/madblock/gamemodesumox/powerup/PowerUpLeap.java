package org.madblock.gamemodesumox.powerup;

import cn.nukkit.Player;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import org.madblock.gamemodesumox.SumoUtil;
import org.madblock.gamemodesumox.SumoXConstants;
import org.madblock.gamemodesumox.SumoXKeys;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.kits.Kit;

public class PowerUpLeap extends PowerUp {

    public PowerUpLeap(GameHandler gameHandler) {
        super(gameHandler);
    }

    @Override
    public String getName() {
        return "Leap";
    }

    @Override
    public String getDescription() {
        return "Allows you to leap in a specific direction.";
    }

    @Override
    public String getUsage() {
        return "Face a direction and tap on the ground. You'll go 'weeeee'";
    }

    @Override
    public Sound useSound() {
        return Sound.BLOCK_BEEHIVE_ENTER;
    }

    @Override
    public float useSoundPitch() {
        return 1f;
    }

    @Override
    public int getWeight() {
        return 100;
    }

    @Override
    public int getBonusWeight(PowerUpContext context) {
        Kit kit = gameHandler.getAppliedSessionKits().get(context.getPlayer());
        if(kit != null){
            return SumoUtil.StringToInt(kit.getProperty(SumoXKeys.KIT_PROP_LEAP_BONUS_WEIGHT).orElse(null)).orElse(0);
        }
        return 0;
    }

    @Override
    public Integer getItemID() {
        return ItemID.FEATHER;
    }

    @Override
    public boolean isConsumedImmediatley() {
        return false;
    }

    @Override
    public boolean use(PowerUpContext context) {
        Player p = context.getPlayer();
        Vector3 dir = p.getDirectionVector();
        p.setMotion(new Vector3(dir.x, Math.abs(dir.y), dir.z).multiply(SumoXConstants.POWERUP_LEAP_STRENGTH));
        return true;
    }

    @Override
    public void cleanUp() { }

}
