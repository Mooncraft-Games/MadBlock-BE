package org.madblock.newgamesapi.nukkit.entity;

import cn.nukkit.Player;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import org.madblock.newgamesapi.nukkit.packet.AnimateEntityPacket;

public class EntityHumanPlus extends EntityHuman {

    protected String spawnAnimationID;
    protected String spawnAnimationCrontroller;

    public EntityHumanPlus(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    public EntityHumanPlus setClientSpawnAnimation(String animID, String animController) {
        this.spawnAnimationID = animID;
        this.spawnAnimationCrontroller = animController;
        return this;
    }

    @Override
    public void spawnTo(Player player) {
        super.spawnTo(player);
        if(spawnAnimationID != null && spawnAnimationCrontroller != null) {
            AnimateEntityPacket dataPacket = new AnimateEntityPacket();
            dataPacket.eid = this.getId();
            dataPacket.animation = spawnAnimationID;
            dataPacket.controller = spawnAnimationCrontroller;
        }
    }
}
