package org.madblock.blockswap.generator.builtin;

import org.madblock.blockswap.generator.BSwapGenerator;
import org.madblock.blockswap.generator.util.Axis;
import org.madblock.blockswap.generator.util.ContextKeys;
import org.madblock.blockswap.generator.util.OpenSimplex2F;
import org.madblock.lib.commons.data.store.settings.ControlledSettings;

/** Generates a random selection of blocks using Random#nextInt(int). */
public class BSGNoiseStriped extends BSwapGenerator {

    private final OpenSimplex2F simplexGen;

    private Axis axis;

    private double scaleX;
    private double scaleY;
    private double scaleZ;

    public BSGNoiseStriped() {
        this.simplexGen = new OpenSimplex2F();

        this.axis = Axis.X;
        this.scaleX = 0.01d;
        this.scaleY = 0.01d;
        this.scaleZ = 0.01d;
    }

    @Override
    public String getGeneratorID() {
        return "blockswap:striped";
    }

    @Override
    protected int generateColourIndex(int x, int y, int z, int maxColours) {
        switch (axis) {
            case Y:
                // This one is literally just layers... Y axis should never be picked randomly
                return mapDoubleToIndex(simplexGen.noise2(y * scaleX, 0), maxColours); // Only use the x axis of the texture.
            case Z:
                return mapDoubleToIndex(simplexGen.noise2(z * scaleZ, 0), maxColours); // Only use the x axis of the texture.
            default:
                // X is the default axis.
                return mapDoubleToIndex(simplexGen.noise2(x * scaleY, 0), maxColours); // Only use the x axis of the texture.
        }
    }

    @Override
    protected int getRawMaxColours() {
        return 100;
    }

    @Override
    public void setContext(ControlledSettings context) {
        super.setContext(context);
        this.axis = getContext().getOrElse(ContextKeys.AXIS, Axis.X);
        this.scaleX = getContext().getOrElse(ContextKeys.NOISE_SCALE_X, 0.001d);
        this.scaleY = getContext().getOrElse(ContextKeys.NOISE_SCALE_Y, 0.001d);
        this.scaleZ = getContext().getOrElse(ContextKeys.NOISE_SCALE_Z, 0.001d);
    }
}
