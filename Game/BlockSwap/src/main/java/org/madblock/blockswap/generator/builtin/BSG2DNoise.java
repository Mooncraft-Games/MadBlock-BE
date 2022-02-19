package org.madblock.blockswap.generator.builtin;

import org.madblock.blockswap.BlockSwapPlugin;
import org.madblock.blockswap.generator.BSwapGenerator;
import org.madblock.blockswap.generator.util.ContextKeys;
import org.madblock.blockswap.generator.util.OpenSimplex2F;
import org.madblock.lib.commons.data.store.settings.ControlledSettings;

import java.util.Arrays;
import java.util.Random;

/** Generates a random selection of blocks using Random#nextInt(int). */
public class BSG2DNoise extends BSwapGenerator {

    private OpenSimplex2F simplexGen;

    private double scaleX;
    private double scaleZ;

    private int shiftX;
    private int shiftZ;

    private double shiftValues;

    public BSG2DNoise() {
        this.simplexGen = new OpenSimplex2F();

        Random random = new Random();
        this.shiftX = random.nextInt(1000000) - 500000;
        this.shiftZ = random.nextInt(1000000) - 500000;

        this.scaleX = 1d;
        this.scaleZ = 1d;

        this.shiftValues = 0.1d;
    }

    @Override
    public String getGeneratorID() {
        return "blockswap:2d_noise";
    }

    @Override
    protected int generateColourIndex(int x, int y, int z, int maxColours) {
        double noise = simplexGen.noise2((x + shiftX) * scaleX, (z + shiftZ) * scaleZ);
        return mapDoubleToIndex(noise, maxColours);
    }

    @Override
    protected int getRawMaxColours() {
        return 4;
    }

    @Override
    public void setContext(ControlledSettings context) {
        super.setContext(context);

        Random random = new Random();
        this.shiftX = random.nextInt(1000000);
        this.shiftZ = random.nextInt(1000000);

        this.scaleX = getContext().getOrElse(ContextKeys.NOISE_SCALE_X, 0.0005d);
        this.scaleZ = getContext().getOrElse(ContextKeys.NOISE_SCALE_Z, 0.0005d);
        BlockSwapPlugin.getLog().info(Arrays.toString(context.get(ContextKeys.BLOCKSWAP_GAME).getColors().toArray()));
    }

    @Override
    public int getWeight() {
        return 1;
    }
}
