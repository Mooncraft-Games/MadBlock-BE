package org.madblock.blockswap.generator.util;

import org.madblock.blockswap.behaviours.BlockSwapGameBehaviour;
import org.madblock.newgamesapi.data.keyvalue.Key;

public final class ContextKeys {

    public static final Key<BlockSwapGameBehaviour> BLOCKSWAP_GAME = new Key<>("bswap_game");

    public static final Key<Axis> AXIS = new Key<>("axis");

    public static final Key<Double> NOISE_SCALE_X = new Key<>("scale_noise_x");
    public static final Key<Double> NOISE_SCALE_Y = new Key<>("scale_noise_y");
    public static final Key<Double> NOISE_SCALE_Z = new Key<>("scale_noise_z");

    public static final Key<Integer> RANDOM_SCALE_X = new Key<>("scale_random_x");
    public static final Key<Integer> RANDOM_SCALE_Y = new Key<>("scale_random_y");
    public static final Key<Integer> RANDOM_SCALE_Z = new Key<>("scale_random_z");

}
