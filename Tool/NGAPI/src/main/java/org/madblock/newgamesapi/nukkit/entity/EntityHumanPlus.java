package org.madblock.newgamesapi.nukkit.entity;

import cn.nukkit.Nukkit;
import cn.nukkit.Player;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import org.madblock.newgamesapi.nukkit.packet.AnimateEntityPacket;

public class EntityHumanPlus extends EntityHuman {

    protected String spawnAnimationID;
    protected String spawnAnimationController;

    public EntityHumanPlus(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    public EntityHumanPlus setClientSpawnAnimation(String animID, String animController) {
        this.spawnAnimationID = animID;
        this.spawnAnimationController = animController;
        return this;
    }

    @Override
    public void spawnTo(Player player) {
        super.spawnTo(player);
        if(spawnAnimationID != null && spawnAnimationController != null) {
            AnimateEntityPacket dataPacket = new AnimateEntityPacket();
            dataPacket.eid = this.getId();
            dataPacket.animation = spawnAnimationID;
            dataPacket.controller = spawnAnimationController;
            player.dataPacket(dataPacket);
        }
    }
}
