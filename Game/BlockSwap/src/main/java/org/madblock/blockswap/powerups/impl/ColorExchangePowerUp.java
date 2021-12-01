package org.madblock.blockswap.powerups.impl;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockWool;
import cn.nukkit.item.Item;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import org.madblock.blockswap.behaviours.BlockSwapGameBehaviour;
import org.madblock.blockswap.powerups.PowerUp;
import org.madblock.blockswap.utils.BlockSwapConstants;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.map.types.MapRegion;

import java.util.Arrays;

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
    public int getDisplayItemID() {
        return Item.DIAMOND;
    }

    @Override
    public void use() {
        Position playerPosition = this.player.getPosition();

        this.behaviour.getSessionHandler().getPrimaryMapID().getRegions().values().forEach(region -> {
            if (Arrays.asList(region.getTags()).contains(BlockSwapConstants.FUNCTION_TAG_GENERATE_PLATFORM)) {
                Block concreteBlock = Block.get(Block.CONCRETE);
                concreteBlock.setDamage(((BlockSwapGameBehaviour)this.behaviour).getWinningColor().getWoolData());

                int y = region.getPosLesser().getY();
                int minX = (int)Math.max(Math.floor(playerPosition.getX()) - RADIUS, region.getPosLesser().getX());
                int maxX = (int)Math.min(Math.floor(playerPosition.getX()) + RADIUS, region.getPosGreater().getX());
                int minZ = (int)Math.max(Math.floor(playerPosition.getZ()) - RADIUS, region.getPosLesser().getZ());
                int maxZ = (int)Math.min(Math.floor(playerPosition.getZ()) + RADIUS, region.getPosGreater().getZ());
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        this.behaviour.getSessionHandler().getPrimaryMap().setBlock(new Vector3(x, y, z), concreteBlock);
                    }
                }

            }
        });
    }
}
