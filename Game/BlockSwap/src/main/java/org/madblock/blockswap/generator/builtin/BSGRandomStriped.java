package org.madblock.blockswap.generator.builtin;

import org.madblock.blockswap.generator.BSwapGenerator;
import org.madblock.blockswap.generator.util.Axis;
import org.madblock.blockswap.generator.util.ContextKeys;
import org.madblock.lib.commons.data.store.settings.ControlledSettings;

import java.util.Random;

/** Generates a random selection of blocks using Random#nextInt(int). */
public class BSGRandomStriped extends BSwapGenerator {

    private Axis axis;

    private int shiftX;
    private int shiftY;
    private int shiftZ;

    private int scaleX;
    private int scaleY;
    private int scaleZ;

    public BSGRandomStriped() {
        this.axis = Axis.X;

        Random random = new Random();
        this.shiftX = random.nextInt(1000000);
        this.shiftY = random.nextInt(1000000);
        this.shiftZ = random.nextInt(1000000);

        this.scaleX = 1;
        this.scaleY = 1;
        this.scaleZ = 1;
    }

    @Override
    public String getGeneratorID() {
        return "blockswap:striped";
    }

    @Override
    protected int generateColourIndex(int x, int y, int z, int maxColours) {
        Random r;
        switch (axis) {
            case Y:
                // This one is literally just layers... Y axis should never be picked randomly
                r = new Random(Math.floorDiv(y, scaleY) + shiftY);
                return r.nextInt(maxColours);
            case Z:
                r = new Random(Math.floorDiv(z, scaleZ) + shiftZ);
                return r.nextInt(maxColours);
            default:
                r = new Random(Math.floorDiv(x, scaleX) + shiftX); // Default is X
                return r.nextInt(maxColours);
        }
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

        this.axis = getContext().getOrElse(ContextKeys.AXIS, Axis.X);

        this.scaleX = getContext().getOrElse(ContextKeys.RANDOM_SCALE_X, 1);
        this.scaleY = getContext().getOrElse(ContextKeys.RANDOM_SCALE_Y, 1);
        this.scaleZ = getContext().getOrElse(ContextKeys.RANDOM_SCALE_Z, 1);
    }
}
