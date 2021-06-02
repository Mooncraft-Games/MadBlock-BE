package org.madblock.newgamesapi.map.types;

import cn.nukkit.level.Position;
import cn.nukkit.math.BlockVector3;
import cn.nukkit.math.Vector3;
import org.madblock.newgamesapi.Utility;

import java.util.Arrays;

public final class MapRegion {

    private transient String uniqueIdentifier;
    private Boolean isActive;

    private BlockVector3 posLesser;
    private BlockVector3 posGreater;

    private String[] tags;

    public MapRegion(String uniqueIdentifier, BlockVector3 pos1, BlockVector3 pos2, String[] tags, boolean isActive){
        this.uniqueIdentifier = uniqueIdentifier == null ? Utility.generateUniqueToken(4, 4).toLowerCase() : uniqueIdentifier.toLowerCase();
        this.posLesser = new BlockVector3(Math.min(pos1.x, pos2.x), Math.min(pos1.y, pos2.y), Math.min(pos1.z, pos2.z));
        this.posGreater = new BlockVector3(Math.max(pos1.x, pos2.x), Math.max(pos1.y, pos2.y), Math.max(pos1.z, pos2.z));
        this.tags = tags;
        this.isActive = isActive;
    }

    public void verifyIntegrityFromJson(String uniqueIdentifier){
        this.uniqueIdentifier = this.uniqueIdentifier == null ? Utility.generateUniqueToken(4, 4).toLowerCase() : uniqueIdentifier.toLowerCase();
        if(isActive == null){ isActive = true; }
        BlockVector3 less = new BlockVector3(Math.min(posLesser.x, posGreater.x), Math.min(posLesser.y, posGreater.y), Math.min(posLesser.z, posGreater.z));
        BlockVector3 great = new BlockVector3(Math.max(posLesser.x, posGreater.x), Math.max(posLesser.y, posGreater.y), Math.max(posLesser.z, posGreater.z));
        this.posLesser = less;
        this.posGreater = great;
        if(this.tags == null) tags = new String[0];
    }

    public String getUniqueIdentifier() { return uniqueIdentifier; }
    public BlockVector3 getPosLesser() { return posLesser; }
    public BlockVector3 getPosGreater() { return posGreater; }
    public String[] getTags() { return tags; }
    public boolean isActive() { return isActive; }

    public String toString(){
        return String.format("Region@[(%s,%s,%s) -> (%s,%s,%s)] + ", posLesser.x, posLesser.y, posLesser.z, posGreater.x, posGreater.y, posGreater.z) + Arrays.toString(tags);
    }

    public boolean isWithinThisRegion(Position position){ return isWithinRegion(this, (Vector3) position); }
    public boolean isWithinThisRegion(Vector3 position){ return isWithinRegion(this, position); }

    public static boolean isWithinRegion(MapRegion region, Position position){ return isWithinRegion(region, (Vector3) position); }
    public static boolean isWithinRegion(MapRegion region, Vector3 position){
        BlockVector3 pos = position.asBlockVector3();
        if((pos.x >= region.posLesser.x) && (pos.x <= region.posGreater.x)){
            if((pos.y >= region.posLesser.y) && (pos.y <= region.posGreater.y)){
                if((pos.z >= region.posLesser.z) && (pos.z <= region.posGreater.z)){
                    return true;
                }
            }
        }
        return false;
    }

}
