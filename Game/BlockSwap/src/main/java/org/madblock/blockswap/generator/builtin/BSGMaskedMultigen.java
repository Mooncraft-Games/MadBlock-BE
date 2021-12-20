package org.madblock.blockswap.generator.builtin;

import org.madblock.blockswap.generator.BSwapGenerator;
import org.madblock.blockswap.generator.BSwapGeneratorManager;
import org.madblock.lib.commons.data.store.settings.ControlledSettings;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;

/**
 * Combines together some generators and selects them based on a mask generator.
 * Required the use of setContext() to properly work.
 */
public class BSGMaskedMultigen extends BSwapGenerator {

    public static final int MULTI_MIN = 2;
    public static final int MULTI_MAX = 3;

    protected BSwapGenerator maskGenerator;
    protected BSwapGenerator[] subGenerators;

    protected Random random;

    public BSGMaskedMultigen() {
        // Fallback gen, 2x more inefficient than the usual BSGRandom! :D
        this.maskGenerator = new BSGRandom();
        this.subGenerators = new BSwapGenerator[] { new BSGRandom() };

        this.random = new Random();
    }

    @Override
    public String getGeneratorID() {
        return "blockswap:maskedmulti";
    }

    @Override
    protected int generateColourIndex(int x, int y, int z, int maxColours) {
        int generatorIndex = maskGenerator.getColourIndex(x, y, z, maxColours);
        BSwapGenerator g = subGenerators[generatorIndex];
        return g.getColourIndex(x, y, z, maxColours);
    }

    @Override
    protected int getRawMaxColours() {
        return 3;
    }


    @Override
    public void setContext(ControlledSettings context) {
        super.setContext(context);

        ArrayList<String> pool = BSwapGeneratorManager.get().getGeneratorList();
        pool.remove(this.getGeneratorID()); // Ensure we don't cause an infinite loop.

        if(pool.size() > 1) {
            int maskIndex = random.nextInt(pool.size());
            Optional<BSwapGenerator> gen = BSwapGeneratorManager.get().getGenerator(pool.get(maskIndex)); // Should be present

            if(!gen.isPresent()) throw new IllegalStateException("Generator present in list doesn't exist? Something is wrong.");
            this.maskGenerator = gen.get();

            //TODO: Use context settings
            int genCount = MULTI_MIN + random.nextInt(MULTI_MAX - MULTI_MIN); // There's always two so this is safe :D
            this.subGenerators = new BSwapGenerator[genCount];

            for(int i = 0; i < genCount; i++) {
                int genIndex = random.nextInt(pool.size());
                Optional<BSwapGenerator> subGen = BSwapGeneratorManager.get().getGenerator(pool.get(genIndex)); // Should be present
                pool.remove(genIndex);

                if(!subGen.isPresent()) throw new IllegalStateException("Generator present in list doesn't exist? Something is wrong.");
                subGenerators[i] = subGen.get();
            }

        } else {
            throw new IllegalStateException("BSGMaskedMultigen requires at least 3 BSwapGenerator types to be registered on the server.");
        }
    }
}
