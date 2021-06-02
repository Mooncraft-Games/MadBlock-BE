package org.madblock.skywars.powerups;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityArmorChangeEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Position;
import cn.nukkit.level.Sound;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.team.Team;
import org.madblock.skywars.SkywarsPlugin;

import java.nio.charset.StandardCharsets;

public class DelusionalPowerUp extends PowerUp implements Listener {

    protected Player caster;

    protected EntityHuman delusion;

    public DelusionalPowerUp(GameBehavior behaviour) {
        super(behaviour);
    }

    @Override
    public String getName() {
        return "Delusion";
    }

    @Override
    public String getDescription() {
        return "Tap to summon a clone of yourself!";
    }

    @Override
    public int getItemId() {
        return Item.ENDER_EYE;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getPlayer().equals(caster)) {
            delusion.setPositionAndRotation(delusion.getPosition(), caster.getYaw(), caster.getPitch());
        }
    }

    @EventHandler
    public void onArmorEquip(EntityArmorChangeEvent event) {
        if (event.getEntity().equals(caster)) {
            delusion.getInventory().setItem(event.getSlot(), event.getNewItem());
        }
    }

    @EventHandler
    public void onDelusionAttacked(EntityDamageByEntityEvent event) {
        if (event.getEntity().equals(delusion) && event.getDamager() instanceof Player && behaviour.getSessionHandler().getPlayerTeam((Player)event.getDamager()).filter(Team::isActiveGameTeam).isPresent()) {
            ((Player)event.getDamager()).sendMessage("" + TextFormat.GRAY + TextFormat.ITALIC + "You attacked an illusion! Where's the real player?!");
            onDeath();
        }
    }

    @Override
    public void use(Player player) {
        caster = player;
        delusion = spawnDelusion();

        // TODO: Rework powerups so that you only register a listener once. Much like PointEntityTypes.
        SkywarsPlugin.getInstance().getServer().getPluginManager().registerEvents(this, SkywarsPlugin.getInstance());
        SkywarsPlugin.getInstance().getServer().getScheduler().scheduleDelayedTask(SkywarsPlugin.getInstance(), () -> {
            HandlerList.unregisterAll(this);
            if (delusion != null) {
                onDeath();
            }
        }, 20 * 30);
    }

    protected void onDeath() {
        HandlerList.unregisterAll(this);
        delusion.despawnFromAll();
        delusion.getLevel().addParticleEffect(delusion.getPosition(), ParticleEffect.CAMPFIRE_SMOKE);
        delusion.getLevel().addSound(delusion.getPosition(), Sound.RANDOM_FIZZ);
    }

    protected EntityHuman spawnDelusion() {

        Position position = caster.getLevel().getBlock(caster.getPosition());
        while (position.getFloorY() > 0 && !position.subtract(0, 1, 0).getLevelBlock().isSolid()) {
            position = position.subtract(0, 1, 0);
        }
        position = position.floor();
        Skin skin = caster.getSkin();
        // -- Taken From https://github.com/Nukkit-coders/NPC/blob/master/src/main/java/idk/plugin/npc/NPC.java --
        CompoundTag nbt = new CompoundTag()
                .putList(new ListTag<>("Pos")
                        .add(new DoubleTag("", position.getX()))
                        .add(new DoubleTag("", position.getY()))
                        .add(new DoubleTag("", position.getZ())))
                .putList(new ListTag<DoubleTag>("Motion")
                        .add(new DoubleTag("", 0))
                        .add(new DoubleTag("", 0))
                        .add(new DoubleTag("", 0)))
                .putList(new ListTag<FloatTag>("Rotation")
                        .add(new FloatTag("", (float)caster.getYaw()))
                        .add(new FloatTag("", (float)caster.getPitch())))
                .putBoolean("npc", true)
                .putFloat("scale", caster.getScale());
        CompoundTag skinDataTag = new CompoundTag()
                .putByteArray("Data", skin.getSkinData().data)
                .putInt("SkinImageWidth", skin.getSkinData().width)
                .putInt("SkinImageHeight", skin.getSkinData().height)
                .putString("ModelId", skin.getSkinId())
                .putString("CapeId", skin.getCapeId())
                .putByteArray("CapeData", skin.getCapeData().data)
                .putInt("CapeImageWidth", skin.getCapeData().width)
                .putInt("CapeImageHeight", skin.getCapeData().height)
                .putByteArray("SkinResourcePatch", skin.getSkinResourcePatch().getBytes(StandardCharsets.UTF_8))
                .putByteArray("GeometryData", skin.getGeometryData().getBytes(StandardCharsets.UTF_8))
                .putByteArray("AnimationData", skin.getAnimationData().getBytes(StandardCharsets.UTF_8))
                .putBoolean("PremiumSkin", skin.isPremium())
                .putBoolean("PersonaSkin", skin.isPersona())
                .putBoolean("CapeOnClassicSkin", skin.isCapeOnClassic());
        nbt.putCompound("Skin", skinDataTag);
        nbt.putBoolean("ishuman", true);
        // -- END snippet --
        EntityHuman delusion = new EntityHuman(position.getChunk(), nbt);
        delusion.setPositionAndRotation(position, caster.getYaw(), caster.getPitch());
        delusion.setNameTagAlwaysVisible(true);
        delusion.setNameTagVisible(true);
        delusion.setNameTag(caster.getNameTag());
        delusion.setSkin(caster.getSkin());
        delusion.setScale(caster.getScale());

        delusion.getInventory().setHelmet(caster.getInventory().getHelmet());
        delusion.getInventory().setChestplate(caster.getInventory().getChestplate());
        delusion.getInventory().setLeggings(caster.getInventory().getLeggings());
        delusion.getInventory().setBoots(caster.getInventory().getBoots());
        delusion.spawnToAll();
        return delusion;

    }

}
