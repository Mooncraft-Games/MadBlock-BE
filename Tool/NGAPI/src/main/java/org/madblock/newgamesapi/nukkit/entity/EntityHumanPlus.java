package org.madblock.newgamesapi.nukkit.entity;

import cn.nukkit.Player;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.scheduler.Task;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.nukkit.packet.AnimateEntityPacket;

public class EntityHumanPlus extends EntityHuman {

    public static final String NBT_SPAWN_ANIM_ID = "spawn_animation_identifier";
    public static final String NBT_SPAWN_ANIM_CONTROL = "spawn_animation_controller";

    protected String spawnAnimationID;
    protected String spawnAnimationController;

    protected EntityHumanPlus thiss() { return this; }
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

        if(player.locallyInitialized) {
            fixEverythingPlsThankYou(player);

        } else {
            Task fix2Task = new Task() {

                @Override
                public void onRun(int currentTick) {
                    if(thiss().getViewers().containsKey(player.getLoaderId())) {
                        if(player.locallyInitialized) {
                            fixEverythingPlsThankYou(player);
                            this.cancel();
                        }
                        return;
                    }

                    this.cancel();
                }

            };

            Task fix1Task = new Task() {

                @Override
                public void onRun(int currentTick) {
                    if(thiss().getViewers().containsKey(player.getLoaderId())) {
                        if(player.locallyInitialized) {
                            if(spawnAnimationID != null && spawnAnimationController != null) {
                                AnimateEntityPacket dataPacket = new AnimateEntityPacket();
                                dataPacket.eid = thiss().getId();
                                dataPacket.animation = "animation.humanoid.base_pose";
                                dataPacket.controller = spawnAnimationController;
                                player.dataPacket(dataPacket);

                                NewGamesAPI1.get().getServer().getScheduler().scheduleDelayedRepeatingTask(fix2Task, 3, 3);
                            }
                            this.cancel();
                        }
                        return;
                    }

                    this.cancel();
                }

            };

            NewGamesAPI1.get().getServer().getScheduler().scheduleRepeatingTask(fix1Task, 3);
        }

    }

    public String getSpawnAnimationID() {
        return spawnAnimationID;
    }

    public String getSpawnAnimationController() {
        return spawnAnimationController;
    }

    protected void fixEverythingPlsThankYou(Player player) {
        NewGamesAPI1.get().getServer().getScheduler().scheduleDelayedTask(NewGamesAPI1.get(), () -> {
            this.teleport(new Vector3(this.x, this.y, this.z), null);

            if(spawnAnimationID != null && spawnAnimationController != null) {
                AnimateEntityPacket dataPacket = new AnimateEntityPacket();
                dataPacket.eid = this.getId();
                dataPacket.animation = spawnAnimationID;
                dataPacket.controller = spawnAnimationController;
                player.dataPacket(dataPacket);
            }
        }, 5);
    }
}
