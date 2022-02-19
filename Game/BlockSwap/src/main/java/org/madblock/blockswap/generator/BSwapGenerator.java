package org.madblock.blockswap.generator;

import org.madblock.blockswap.utils.BlockSwapConstants;
import org.madblock.lib.commons.data.store.settings.ControlledSettings;

public abstract class BSwapGenerator {

    private ControlledSettings context;

    public BSwapGenerator () {
        this.context = null;
    }

    /** @return the ID of the generator. */
    public abstract String getGeneratorID();

    /**
     * Generates an index which can be used to extract a colour from an array at
     * a given block position. Values generated between 0 and 1 can be converted
     * to an index using the utility method BSwapGenerator#mapFloatToIndex
     * @param x X coordinate of block.
     * @param y Y coordinate of block.
     * @param z Y coordinate of block.
     * @param maxColours the max amount of colour in the palette array
     * @return an index which should be between 0 and (maxColours - 1)
     */
    protected abstract int generateColourIndex(int x, int y, int z, int maxColours);

    /** The maximum amount of colours that can be present per round of blockswap. */
    protected abstract int getRawMaxColours();



    /**
     * Generates an index which can be used to extract a colour from an array at
     * a given block position.
     * @param x X coordinate of block.
     * @param y Y coordinate of block.
     * @param z Y coordinate of block.
     * @param maxColours the max amount of colour in the palette array
     * @return an index between 0 and (maxColours - 1)
     */
    public final int getColourIndex(int x, int y, int z, int maxColours) {
        if (maxColours < 1) throw new IllegalStateException("Max colour count must be greater than 1");

        // Keep it between 0 and maxColours - 1
        return Math.min(Math.max(generateColourIndex(x, y, z, maxColours), 0), (maxColours - 1));
    }

    /** The maximum amount of colours that can be present per round of blockswap. */
    public final int getMaxColours() {
        return Math.min(BlockSwapConstants.POSSIBLE_COLORS.size(), getRawMaxColours());
    }



    /** @return the last BlockSwapGameBehaviour to properly use this generator. Can be null. */
    public ControlledSettings getContext() {
        return context;
    }

    /** Sets the last BlockSwapGameBehaviour to use this generator. */
    public void setContext(ControlledSettings context) {
        this.context = context.lock();
    }

    /** Sets the context to null. */
    public void resetContext() {
        this.context = new ControlledSettings().lock();
    }



    /**
     * Maps a float with a value between 0 and 1 (Numbers out of those bounds are
     * clamped to them) to a colour index for use in BSwapGenerator#generateColourIndex(...)
     * @param value the float between 0 and 1
     * @return a colour index.
     */
    protected static int mapFloatToIndex(float value, int maxColours) {
        return mapDoubleToIndex(value, maxColours);
    }

    /**
     * Maps a double with a value between 0 and 1 (Numbers out of those bounds are
     * clamped to them) to a colour index for use in BSwapGenerator#generateColourIndex(...)
     * @param value the double between 0 and 1
     * @return a colour index.
     */
    protected static int mapDoubleToIndex(double value, int maxColours) {
        if(value < 0) return 0;  // Snap to zero
        if(value > 1d) return maxColours - 1;  // Snap to the last index (equivalent of 1)

        double section = 1f / maxColours;
        double remainder = value % section; // Remainders mean a value is within the section

        return (int) ((value - remainder) / section); // Calculate section by removing remainder to get a whole number.
    }


    public abstract int getWeight();
}
