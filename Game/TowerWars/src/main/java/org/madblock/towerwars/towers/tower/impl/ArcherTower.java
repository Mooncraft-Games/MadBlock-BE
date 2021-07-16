package org.madblock.towerwars.towers.tower.impl;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.LongEntityData;
import cn.nukkit.item.Item;
import cn.nukkit.level.Position;
import cn.nukkit.network.protocol.MobEquipmentPacket;
import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.events.TWEventHandler;
import org.madblock.towerwars.events.tower.targets.TowerTargetMoveEvent;
import org.madblock.towerwars.events.tower.targets.TowerTargetSelectEvent;
import org.madblock.towerwars.towers.tower.Tower;
import org.madblock.towerwars.towers.tower.TowerProperties;
import org.madblock.towerwars.utils.EntityUtils;
import org.madblock.towerwars.utils.GameRegion;

public class ArcherTower extends Tower {

    public ArcherTower(TowerProperties properties, TowerWarsBehavior behavior, GameRegion gameRegion) {
        super(properties, behavior, gameRegion);
    }

    @Override
    public Entity createEntity(Position position) {
        Entity skeleton = Entity.createEntity("Skeleton", position);
        skeleton.setRotation(Math.floor(Math.random() * 360), 0);
        skeleton.spawnToAll();

        MobEquipmentPacket packet = new MobEquipmentPacket();
        packet.item = Item.get(Item.BOW);
        packet.eid = skeleton.getId();

        for (Player player : skeleton.getViewers().values()) {
            player.dataPacket(packet);
        }

        return skeleton;
    }

    @TWEventHandler
    public void onTarget(TowerTargetSelectEvent event) {
        if (this.equals(event.getTower())) {
            if (event.getTarget() == null) {
                // Bring bow back to non-attacking position
                this.getEntity().setDataProperty(new LongEntityData(Entity.DATA_TARGET_EID, 0));
                this.getEntity().setDataFlag(Entity.DATA_FLAGS, Entity.DATA_FLAG_FIRE_IMMUNE);
            } else {
                // Bring bow to attacking position
                this.getEntity().setDataProperty(new LongEntityData(Entity.DATA_TARGET_EID, event.getTarget().getEntity().getId()));
                this.getEntity().setDataFlag(Entity.DATA_FLAGS, Entity.DATA_FLAG_FACING_TARGET_TO_RANGE_ATTACK + Entity.DATA_FLAG_FIRE_IMMUNE);
            }
        }
    }

    @TWEventHandler
    public void onTargetMove(TowerTargetMoveEvent event) {
        if (this.equals(event.getTower())) {
            EntityUtils.lookAt(this.getEntity(), event.getTarget().getEntity());
        }
    }

}
