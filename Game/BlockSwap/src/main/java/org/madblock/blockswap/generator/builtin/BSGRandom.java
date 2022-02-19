package org.madblock.blockswap.generator.builtin;

import org.madblock.blockswap.generator.BSwapGenerator;
import org.madblock.blockswap.generator.util.ContextKeys;
import org.madblock.lib.commons.data.store.settings.ControlledSettings;

import java.util.Random;

/** Generates a random selection of blocks using Random#nextInt(int). */
public class BSGRandom extends BSwapGenerator {

    private long shiftX;
    private long shiftY;
    private long shiftZ;

    private int scaleX;
    private int scaleY;
    private int scaleZ;

    public BSGRandom() {
        Random random = new Random();
        this.shiftX = random.nextInt(1000);
        this.shiftY = random.nextInt(1000);
        this.shiftZ = random.nextInt(1000);

        this.scaleX = 1;
        this.scaleY = 1;
        this.scaleZ = 1;
    }

    @Override
    public String getGeneratorID() {
        return "blockswap:random";
    }

    @Override
    protected int generateColourIndex(int x, int y, int z, int maxColours) {
        long xTile = Math.floorDiv(x, scaleX) + shiftX;
        long yTile = Math.floorDiv(y, scaleY) + shiftY;
        long zTile = Math.floorDiv(z, scaleZ) + shiftZ;

        Random r = new Random((xTile + (yTile * (shiftX + shiftY)) + (zTile * (shiftX + shiftY + shiftZ))));
        return r.nextInt(maxColours);
    }

    @Override
    protected int getRawMaxColours() {
        return 100;
    }

    @Override
    public void setContext(ControlledSettings context) {
        super.setContext(context);

        Random random = new Random();
        this.shiftX = random.nextInt(1000000);
        this.shiftY = random.nextInt(1000000);
        this.shiftZ = random.nextInt(1000000);

        this.scaleX = getContext().getOrElse(ContextKeys.RANDOM_SCALE_X, 1);
        this.scaleY = getContext().getOrElse(ContextKeys.RANDOM_SCALE_Y, 1);
        this.scaleZ = getContext().getOrElse(ContextKeys.RANDOM_SCALE_Z, 1);
    }

    @Override
    public int getWeight() {
        return 4;
    }
}
