package org.madblock.newgamesapi.nukkit.entity;

import cn.nukkit.Nukkit;
import cn.nukkit.Player;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.MovePlayerPacket;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.nukkit.packet.AnimateEntityPacket;

public class EntityHumanPlus extends EntityHuman {

    public static final String NBT_SPAWN_ANIM_ID = "spawn_animation_identifier";
    public static final String NBT_SPAWN_ANIM_CONTROL = "spawn_animation_controller";

    protected String spawnAnimationID;
    protected String spawnAnimationController;

    public EntityHumanPlus(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);

        String animID = this.namedTag.getString(NBT_SPAWN_ANIM_ID);
        String animControl = this.namedTag.getString(NBT_SPAWN_ANIM_CONTROL);

        this.spawnAnimationID = animID.length() == 0 ? "" : animID;
        this.spawnAnimationController = animControl.length() == 0 ? "" : animControl;
    }


    public EntityHumanPlus setClientSpawnAnimation(String animID, String animController) {
        this.spawnAnimationID = animID;
        this.spawnAnimationController = animController;

        if(animID == null) this.namedTag.remove(NBT_SPAWN_ANIM_ID);
        else this.namedTag.putString(NBT_SPAWN_ANIM_ID, animID);

        if(animController == null) this.namedTag.remove(NBT_SPAWN_ANIM_CONTROL);
        else this.namedTag.putString(NBT_SPAWN_ANIM_CONTROL, animController);

        return this;
    }

    @Override
    public void spawnTo(Player player) {
        super.spawnTo(player);

        MovePlayerPacket movePacket = new MovePlayerPacket();
        movePacket.eid = this.id;
        movePacket.x = (float) this.x;
        movePacket.y = (float) this.y + (this.getHeight() * scale / 2);
        movePacket.z = (float) this.z;
        movePacket.yaw = (float) this.getYaw();
        movePacket.headYaw = (float) this.getYaw();
        movePacket.pitch = (float) this.getPitch();
        movePacket.mode = MovePlayerPacket.MODE_TELEPORT;
        movePacket.onGround = false;
        movePacket.ridingEid = this.getRiding() == null ? 0 : this.getRiding().getId();
        movePacket.int1 = 4;
        movePacket.int2 = 0;
        //player.dataPacket(movePacket);


        NewGamesAPI1.get().getServer().getScheduler().scheduleDelayedTask(NewGamesAPI1.get(), () -> {
            if(spawnAnimationID != null && spawnAnimationController != null) {
                AnimateEntityPacket dataPacket = new AnimateEntityPacket();
                dataPacket.eid = this.getId();
                dataPacket.animation = spawnAnimationID;
                dataPacket.controller = spawnAnimationController;
                player.dataPacket(dataPacket);
            }
        }, 20);

    }
}
