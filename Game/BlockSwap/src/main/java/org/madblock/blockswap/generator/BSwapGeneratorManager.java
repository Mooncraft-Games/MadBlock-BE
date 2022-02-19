package org.madblock.blockswap.generator;

import org.madblock.blockswap.BlockSwapPlugin;
import org.madblock.blockswap.generator.builtin.BSGRandom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;

public final class BSwapGeneratorManager {

    private static BSwapGeneratorManager managerInstance;

    private Random random;
    private HashMap<String, BSwapGenerator> generators;


    public BSwapGeneratorManager() {
        this.random = new Random();
        this.generators = new HashMap<>();
    }

    /** Sets this to the primary instance of this manager if not already set.*/
    public void setAsPrimaryManager(){
        if(managerInstance == null) managerInstance = this;
    }

    /**
     * Registers the provided generator against the ID provided by
     * BSwapGenerator#getGeneratorID() - If a generator is already registered with the
     * same ID, the generator provided is not registered.
     * @param generator the generator provided to be registered.
     * @return true if the generator is registered.
     */
    public boolean registerGenerator(BSwapGenerator generator) {
        String id = generator.getGeneratorID();

        if (id == null || id.trim().length() == 0) throw new IllegalStateException("Invalid generator ID, must not be null or have a length of 0");
        String fullID = id.trim().toLowerCase();

        if(!this.generators.containsKey(fullID)) {
            this.generators.put(id.trim().toLowerCase(), generator);
            return true;
        }
        return false;
    }



    /** @return an optional containing a BSwapGenerator for the provided id if present, else an empty optional. */
    public Optional<BSwapGenerator> getGenerator(String id) {
        return Optional.ofNullable(generators.get(id.trim().toLowerCase()));
    }

    public BSwapGenerator getRandomGenerator() {
        ArrayList<BSwapGenerator> gen = new ArrayList<>(generators.values());
        ArrayList<BSwapGenerator> pool = new ArrayList<>();

        //TODO: Terribly inefficient for large weights, redo this!
        for(BSwapGenerator generator: gen) {
            for(int i = 0; i < generator.getWeight(); i++) {
                pool.add(generator);
            }
        }

        if(pool.size() > 0) {
            int genIndex = generators.size() == 1 ? 0 : random.nextInt(pool.size());
            return pool.get(genIndex);

        } else {
            BlockSwapPlugin.getLog().warning("No generators detected! Something is very wrong! Falling back to BSGRandom :^)");
            return new BSGRandom(); // Fallback just in-case someone really breaks it.
        }
    }

    /** @return a list of all the generator ids registered. */
    public ArrayList<String> getGeneratorList() {
        return new ArrayList<>(generators.keySet());
    }


    /** @return the primary instance of the Manager. */
    public static BSwapGeneratorManager get() { return managerInstance; }

}
