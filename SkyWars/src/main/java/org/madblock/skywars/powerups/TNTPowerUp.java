package org.madblock.skywars.powerups;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.item.Item;
import cn.nukkit.math.Vector3;
import org.madblock.newgamesapi.game.GameBehavior;

public class TNTPowerUp extends PowerUp {

    public TNTPowerUp(GameBehavior behaviour) {
        super(behaviour);
    }

    @Override
    public String getName() {
        return "TNT";
    }

    @Override
    public String getDescription() {
        return "Tap to throw it!";
    }

    @Override
    public int getItemId() {
        return Item.TNT;
    }

    @Override
    public void use(Player user) {

        Entity tntEntity = Entity.createEntity(
                "PrimedTnt",
                user.getPosition().add(new Vector3(0, 2, 0))
        );

        tntEntity.setMotion(user.getDirectionVector().multiply(1.5f));
        tntEntity.setRotation(user.getYaw(), user.getPitch());
        tntEntity.spawnToAll();

    }
}
