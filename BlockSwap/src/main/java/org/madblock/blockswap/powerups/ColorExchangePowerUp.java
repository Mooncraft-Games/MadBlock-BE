package org.madblock.blockswap.powerups;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockWool;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import org.madblock.blockswap.behaviours.BlockSwapGameBehaviour;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.map.types.MapRegion;

public class ColorExchangePowerUp extends PowerUp {

    private static final int RADIUS = 2;

    public ColorExchangePowerUp(GameBehavior behaviour, Player player) {
        super(behaviour, player);
    }

    @Override
    public String getName() {
        return "Color Exchange";
    }

    @Override
    public String getDescription() {
        return "Bend the laws of time and space and change the ground around you to the required color.";
    }

    @Override
    public boolean isInstantConsumable() {
        return false;
    }

    @Override
    public void use() {
        Position pos = this.player.getPosition();
        MapRegion platform = this.behaviour.getSessionHandler().getPrimaryMapID().getRegions().get("platform");

        int y = platform.getPosGreater().getY();

        Block woolBlock = new BlockWool();
        woolBlock.setDamage(((BlockSwapGameBehaviour)this.behaviour).getWinningColor().getWoolData());

        for (int x = (int)Math.max(Math.floor(pos.getX()) - RADIUS, platform.getPosLesser().getX()); x <= (int)Math.min(Math.floor(pos.getX()) + RADIUS, platform.getPosGreater().getX()); x++) {
            for (int z = (int)Math.max(Math.floor(pos.getZ()) - RADIUS, platform.getPosLesser().getZ()); z <= (int)Math.min(Math.floor(pos.getZ()) + RADIUS, platform.getPosGreater().getZ()); z++) {
                this.behaviour.getSessionHandler().getPrimaryMap().setBlock(new Vector3(x, y, z), woolBlock);
            }
        }

    }
}
