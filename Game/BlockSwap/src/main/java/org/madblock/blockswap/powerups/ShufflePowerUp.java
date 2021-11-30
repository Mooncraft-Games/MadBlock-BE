package org.madblock.blockswap.powerups;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.item.Item;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.DyeColor;
import org.madblock.blockswap.behaviours.BlockSwapGameBehaviour;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.map.types.MapRegion;

import java.util.List;

public class ShufflePowerUp extends PowerUp {

    private static final int RADIUS = 2;

    public ShufflePowerUp(GameBehavior behaviour, Player player) {
        super(behaviour, player);
    }

    @Override
    public String getName() {
        return "Shuffle";
    }

    @Override
    public String getDescription() {
        return "Randomize the colors around you!";
    }

    @Override
    public boolean isInstantConsumable() {
        return false;
    }

    @Override
    public int getDisplayItemID() {
        return Item.EMERALD;
    }

    @Override
    public void use() {
        Position playerPosition = this.player.getPosition();
        MapRegion platform = this.behaviour.getSessionHandler().getPrimaryMapID().getRegions().get("platform");

        int blockY = platform.getPosGreater().getY();
        List<DyeColor> colors = ((BlockSwapGameBehaviour) this.behaviour).getColors();

        for (int x = (int)Math.max(Math.floor(playerPosition.getX()) - RADIUS, platform.getPosLesser().getX()); x <= (int)Math.min(Math.floor(playerPosition.getX()) + RADIUS, platform.getPosGreater().getX()); x++) {
            for (int z = (int)Math.max(Math.floor(playerPosition.getZ()) - RADIUS, platform.getPosLesser().getZ()); z <= (int)Math.min(Math.floor(playerPosition.getZ()) + RADIUS, platform.getPosGreater().getZ()); z++) {
                Block woolBlock = Block.get(BlockID.WOOL);
                woolBlock.setDamage(colors.get((int) Math.floor(Math.random() * colors.size())).getWoolData());
                this.behaviour.getSessionHandler().getPrimaryMap().setBlock(new Vector3(x, blockY, z), woolBlock);
            }
        }

    }
}
