package org.madblock.blockswap.powerups.impl;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.item.Item;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.DyeColor;
import org.madblock.blockswap.behaviours.BlockSwapGameBehaviour;
import org.madblock.blockswap.powerups.PowerUp;
import org.madblock.blockswap.utils.BlockSwapConstants;
import org.madblock.newgamesapi.game.GameBehavior;

import java.util.Arrays;
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
        List<DyeColor> colors = ((BlockSwapGameBehaviour) this.behaviour).getColors();

        this.behaviour.getSessionHandler().getPrimaryMapID().getRegions().values().forEach(region -> {
            if (Arrays.asList(region.getTags()).contains(BlockSwapConstants.FUNCTION_TAG_GENERATE_PLATFORM)) {
                int y = region.getPosLesser().getY();
                int minX = (int)Math.max(Math.floor(playerPosition.getX()) - RADIUS, region.getPosLesser().getX());
                int maxX = (int)Math.min(Math.floor(playerPosition.getX()) + RADIUS, region.getPosGreater().getX());
                int minZ = (int)Math.max(Math.floor(playerPosition.getZ()) - RADIUS, region.getPosLesser().getZ());
                int maxZ = (int)Math.min(Math.floor(playerPosition.getZ()) + RADIUS, region.getPosGreater().getZ());
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Block concreteBlock = Block.get(BlockID.CONCRETE);
                        concreteBlock.setDamage(colors.get((int) Math.floor(Math.random() * colors.size())).getWoolData());
                        this.behaviour.getSessionHandler().getPrimaryMap().setBlock(new Vector3(x, y, z), concreteBlock);
                    }
                }

            }
        });

    }
}
