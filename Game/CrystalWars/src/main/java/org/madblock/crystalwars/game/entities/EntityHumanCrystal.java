package org.madblock.crystalwars.game.entities;

import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import org.madblock.newgamesapi.nukkit.entity.EntityHumanPlus;

public class EntityHumanCrystal extends EntityHumanPlus {



    public EntityHumanCrystal(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }


    protected static final String GEO_ID = "";
    protected static final String GEO = "{\n" +
            "\t\"format_version\": \"1.12.0\",\n" +
            "\t\"minecraft:geometry\": [\n" +
            "\t\t{\n" +
            "\t\t\t\"description\": {\n" +
            "\t\t\t\t\"identifier\": \""+GEO_ID+"\",\n" +
            "\t\t\t\t\"texture_width\": 128,\n" +
            "\t\t\t\t\"texture_height\": 128,\n" +
            "\t\t\t\t\"visible_bounds_width\": 3,\n" +
            "\t\t\t\t\"visible_bounds_height\": 3.5,\n" +
            "\t\t\t\t\"visible_bounds_offset\": [0, 1.25, 0]\n" +
            "\t\t\t},\n" +
            "\t\t\t\"bones\": [\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"name\": \"root\",\n" +
            "\t\t\t\t\t\"pivot\": [0, 0, 0]\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"name\": \"head\",\n" +
            "\t\t\t\t\t\"parent\": \"root\",\n" +
            "\t\t\t\t\t\"pivot\": [0, 24, 0],\n" +
            "\t\t\t\t\t\"cubes\": [\n" +
            "\t\t\t\t\t\t{\"origin\": [-4, 24, -4], \"size\": [8, 8, 8], \"uv\": [0, 0]},\n" +
            "\t\t\t\t\t\t{\"origin\": [-11, 30, 1], \"size\": [2, 3, 2], \"inflate\": -0.5, \"pivot\": [0, 0, 0], \"rotation\": [10, 0, 22.5], \"uv\": [34, 56]},\n" +
            "\t\t\t\t\t\t{\"origin\": [-4, 29, 11], \"size\": [2, 4, 2], \"inflate\": -0.5, \"pivot\": [0, 0, 0], \"rotation\": [20, 0, 0], \"uv\": [16, 55]},\n" +
            "\t\t\t\t\t\t{\"origin\": [-1, 28, -13], \"size\": [2, 5, 2], \"inflate\": -0.5, \"pivot\": [0, 0, 0], \"rotation\": [-17.5, 0, 2.5], \"uv\": [0, 55]},\n" +
            "\t\t\t\t\t\t{\"origin\": [-1, 29, 8], \"size\": [2, 5, 2], \"inflate\": -0.5, \"pivot\": [0, 0, 0], \"rotation\": [15, 0, 2.5], \"uv\": [0, 0]},\n" +
            "\t\t\t\t\t\t{\"origin\": [-15, 25, -9], \"size\": [2, 5, 2], \"inflate\": -0.5, \"pivot\": [0, 0, 0], \"rotation\": [-17.5, 0, 30], \"uv\": [54, 14]},\n" +
            "\t\t\t\t\t\t{\"origin\": [4, 29, 7], \"size\": [2, 4, 2], \"inflate\": -0.5, \"pivot\": [0, 0, 0], \"rotation\": [20, 0, -10], \"uv\": [8, 55]},\n" +
            "\t\t\t\t\t\t{\"origin\": [3, 30, 4], \"size\": [2, 4, 2], \"inflate\": -0.5, \"pivot\": [0, 0, 0], \"rotation\": [10, 0, -12.5], \"uv\": [54, 25]},\n" +
            "\t\t\t\t\t\t{\"origin\": [-3, 30, -7], \"size\": [3, 4, 3], \"inflate\": -0.5, \"pivot\": [0, 0, 0], \"rotation\": [-7.5, 0, 7.5], \"uv\": [24, 48]},\n" +
            "\t\t\t\t\t\t{\"origin\": [-6, 30, -6], \"size\": [3, 3, 3], \"inflate\": -0.5, \"pivot\": [0, 0, 0], \"rotation\": [-12.5, 0, 12.5], \"uv\": [48, 50]},\n" +
            "\t\t\t\t\t\t{\"origin\": [3, 31, 3], \"size\": [3, 3, 3], \"inflate\": -0.5, \"pivot\": [0, 0, 0], \"rotation\": [12.5, 0, -12.5], \"uv\": [36, 50]},\n" +
            "\t\t\t\t\t\t{\"origin\": [0, 30, -7], \"size\": [3, 5, 3], \"inflate\": -0.5, \"pivot\": [0, 0, 0], \"rotation\": [-15, 0, -7.5], \"uv\": [44, 42]},\n" +
            "\t\t\t\t\t\t{\"origin\": [-10, 28, -7], \"size\": [3, 5, 3], \"inflate\": -0.5, \"pivot\": [0, 0, 0], \"rotation\": [-15, 0, 15], \"uv\": [44, 34]},\n" +
            "\t\t\t\t\t\t{\"origin\": [-17, 27, -2], \"size\": [3, 3, 3], \"inflate\": -0.5, \"pivot\": [0, 0, 0], \"rotation\": [0, 0, 27.5], \"uv\": [53, 39]}\n" +
            "\t\t\t\t\t]\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"name\": \"body\",\n" +
            "\t\t\t\t\t\"parent\": \"root\",\n" +
            "\t\t\t\t\t\"pivot\": [0, 18, 0],\n" +
            "\t\t\t\t\t\"cubes\": [\n" +
            "\t\t\t\t\t\t{\"origin\": [-4, 12, -2], \"size\": [8, 12, 4], \"uv\": [0, 16]}\n" +
            "\t\t\t\t\t]\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"name\": \"arm_l\",\n" +
            "\t\t\t\t\t\"parent\": \"root\",\n" +
            "\t\t\t\t\t\"pivot\": [4, 24, 0],\n" +
            "\t\t\t\t\t\"cubes\": [\n" +
            "\t\t\t\t\t\t{\"origin\": [4, 12, -2], \"size\": [3, 12, 4], \"uv\": [30, 32]}\n" +
            "\t\t\t\t\t]\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"name\": \"pad_l\",\n" +
            "\t\t\t\t\t\"parent\": \"arm_l\",\n" +
            "\t\t\t\t\t\"pivot\": [4, 24, 0],\n" +
            "\t\t\t\t\t\"rotation\": [0, 0, 10],\n" +
            "\t\t\t\t\t\"cubes\": [\n" +
            "\t\t\t\t\t\t{\"origin\": [4, 22, -3], \"size\": [4, 3, 6], \"uv\": [40, 16]},\n" +
            "\t\t\t\t\t\t{\"origin\": [5, 21, 8], \"size\": [3, 4, 3], \"inflate\": -0.5, \"pivot\": [0, 0, 0], \"rotation\": [22.5, 0, 0], \"uv\": [0, 48]},\n" +
            "\t\t\t\t\t\t{\"origin\": [4, 24, 0], \"size\": [3, 4, 3], \"inflate\": -0.5, \"uv\": [46, 0]}\n" +
            "\t\t\t\t\t]\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"name\": \"arm_r\",\n" +
            "\t\t\t\t\t\"parent\": \"root\",\n" +
            "\t\t\t\t\t\"pivot\": [-4, 24, 0],\n" +
            "\t\t\t\t\t\"cubes\": [\n" +
            "\t\t\t\t\t\t{\"origin\": [-7, 12, -2], \"size\": [3, 12, 4], \"uv\": [16, 32]}\n" +
            "\t\t\t\t\t]\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"name\": \"pad_r\",\n" +
            "\t\t\t\t\t\"parent\": \"arm_r\",\n" +
            "\t\t\t\t\t\"pivot\": [-4, 24, 0],\n" +
            "\t\t\t\t\t\"rotation\": [0, 0, -10],\n" +
            "\t\t\t\t\t\"cubes\": [\n" +
            "\t\t\t\t\t\t{\"origin\": [-8, 22, -3], \"size\": [4, 3, 6], \"uv\": [40, 25]},\n" +
            "\t\t\t\t\t\t{\"origin\": [-7, 24, 0], \"size\": [3, 4, 3], \"inflate\": -0.5, \"uv\": [46, 0]},\n" +
            "\t\t\t\t\t\t{\"origin\": [-8, 21, 8], \"size\": [3, 4, 3], \"inflate\": -0.5, \"pivot\": [0, 0, 0], \"rotation\": [22.5, 0, 0], \"uv\": [0, 48]}\n" +
            "\t\t\t\t\t]\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"name\": \"leg_l\",\n" +
            "\t\t\t\t\t\"parent\": \"root\",\n" +
            "\t\t\t\t\t\"pivot\": [2, 12, 0],\n" +
            "\t\t\t\t\t\"cubes\": [\n" +
            "\t\t\t\t\t\t{\"origin\": [0, 5, -3], \"size\": [5, 2, 6], \"uv\": [24, 0]},\n" +
            "\t\t\t\t\t\t{\"origin\": [0, 0, -3], \"size\": [4, 3, 1], \"uv\": [36, 16]},\n" +
            "\t\t\t\t\t\t{\"origin\": [0, 0, -2], \"size\": [4, 12, 4], \"uv\": [24, 16]}\n" +
            "\t\t\t\t\t]\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"name\": \"leg_r\",\n" +
            "\t\t\t\t\t\"parent\": \"root\",\n" +
            "\t\t\t\t\t\"pivot\": [-2, 12, 0],\n" +
            "\t\t\t\t\t\"cubes\": [\n" +
            "\t\t\t\t\t\t{\"origin\": [-5, 5, -3], \"size\": [5, 2, 6], \"uv\": [32, 8]},\n" +
            "\t\t\t\t\t\t{\"origin\": [-4, 0, -2], \"size\": [4, 12, 4], \"uv\": [0, 32]},\n" +
            "\t\t\t\t\t\t{\"origin\": [-4, 0, -3], \"size\": [4, 3, 1], \"uv\": [24, 55]}\n" +
            "\t\t\t\t\t]\n" +
            "\t\t\t\t}\n" +
            "\t\t\t]\n" +
            "\t\t}\n" +
            "\t]\n" +
            "}";
}
