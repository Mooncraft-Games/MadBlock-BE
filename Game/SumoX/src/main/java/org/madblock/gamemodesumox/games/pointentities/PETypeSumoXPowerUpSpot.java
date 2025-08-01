package org.madblock.gamemodesumox.games.pointentities;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.mob.EntityGuardian;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.Level;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Sound;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector2;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.utils.TextFormat;
import org.madblock.gamemodesumox.SumoX;
import org.madblock.gamemodesumox.SumoXConstants;
import org.madblock.gamemodesumox.SumoXKeys;
import org.madblock.gamemodesumox.SumoXStrings;
import org.madblock.gamemodesumox.games.GBehaveSumoBase;
import org.madblock.gamemodesumox.powerup.PowerUp;
import org.madblock.gamemodesumox.powerup.PowerUpContext;
import org.madblock.newgamesapi.util.Utility;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.pointentities.PointEntityCallData;
import org.madblock.newgamesapi.map.pointentities.PointEntityType;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.newgamesapi.rewards.RewardChunk;
import org.madblock.newgamesapi.team.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;

public class PETypeSumoXPowerUpSpot extends PointEntityType implements Listener {

    protected boolean triggeredMisuseWarning = false;

    protected PowerUp[] powerUpPool;

    protected int powerUpItemCount;
    protected HashMap<String, PowerUp> pendingPowerUps;
    protected ArrayList<Player> powerUpItemCooldowns;

    protected ArrayList<Entity> powerUpEntities;

    public PETypeSumoXPowerUpSpot(GameHandler gameHandler) {
        super(SumoXKeys.PE_TYPE_POWERUP, gameHandler);

        this.powerUpPool = new PowerUp[0];

        this.powerUpItemCount = 0;
        this.pendingPowerUps = new HashMap<>();
        this.powerUpItemCooldowns = new ArrayList<>();

        this.powerUpEntities = new ArrayList<>();
    }

    @Override
    public void onRegister() {
        this.addFunction(SumoXKeys.PE_FUNC_POWERUP_SPAWN, this::spawnPowerUp);

        this.powerUpPool = new PowerUp[SumoXConstants.AVAILABLE_POWER_UPS.size()];
        for(int i = 0; i < SumoXConstants.AVAILABLE_POWER_UPS.size(); i++){
            try {
                Class<? extends PowerUp> c = SumoXConstants.AVAILABLE_POWER_UPS.get(i);
                PowerUp powerUp = c.getConstructor(GameHandler.class).newInstance(gameHandler);
                powerUpPool[i] = powerUp;
            } catch (Exception err){
                this.powerUpPool = new PowerUp[0];
                SumoX.getPlgLogger().error("Very Broken PowerUp: Constructor Fault.");
                return;
            }
        }

        gameHandler.getGameScheduler().registerGameTask(this::animatePowerUp, SumoXConstants.POWERUP_ANIMATE_TICK_INTERVAL, SumoXConstants.POWERUP_ANIMATE_TICK_INTERVAL);
        SumoX.get().getServer().getPluginManager().registerEvents(this, SumoX.get());
    }

    @Override
    public void onUnregister() {
        for(Entity entity: powerUpEntities){
            entity.close();
        }
        HandlerList.unregisterAll(this);
    }

    protected void animatePowerUp(){
        for(Entity entity: new ArrayList<>(powerUpEntities)){
            if(entity.isClosed()){
                powerUpEntities.remove(entity);
            } else {
                entity.setRotation(entity.getYaw() + SumoXConstants.POWERUP_ANIMATE_TICK_SPEED, entity.getPitch());
            }
        }
    }

    protected void spawnPowerUp(PointEntityCallData data){
        if(getGameHandler().getGameBehaviors() instanceof GBehaveSumoBase) {

            GBehaveSumoBase behaviours = (GBehaveSumoBase) getGameHandler().getGameBehaviors();
            PointEntity pe = data.getPointEntity();

            if(behaviours.getPowerUpPointCooldowns().containsKey(pe.getId())){
                int time = behaviours.getPowerUpPointCooldowns().get(pe.getId());
                if(time < 0){
                    // Power up still spawned. Keep there.
                    return;
                }

                // Decrement Timer
                time--;
                behaviours.getPowerUpPointCooldowns().put(pe.getId(), time);
                if (time == 0){
                    // Spawn Power up
                    spawnPowerUpEntity(pe).spawnToAll();
                }
            } else {
                behaviours.getPowerUpPointCooldowns().put(pe.getId(), generateNewTime(behaviours));
            }
        } else {
            if(!triggeredMisuseWarning) {
                this.triggeredMisuseWarning = true;
                SumoX.getPlgLogger().error("Misused powerup. Wrong Gamemode! //JellyBaboon was here.");
            }
        }
    }

    protected EntityGuardian spawnPowerUpEntity(PointEntity pe){
        Level level = getParentManager().getLevelLookup().get(pe);

        Vector3 position = pe.positionToVector3().add(!pe.isAccuratePosition() ? 0.5d : 0, 1, !pe.isAccuratePosition() ? 0.5d : 0);
        Vector2 rotation = pe.rotationToVector2();

        FullChunk chunk = level.getChunk((int)Math.floor(position.getX() / 16.0D), (int)Math.floor(position.getZ() / 16.0D), true);
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
                        .add(new FloatTag("", (float) rotation.getY()))
                        .add(new FloatTag("", (float) rotation.getX())))
                .putBoolean("npc", true)
                .putString(SumoXKeys.NBT_POWERUP_PE_TIE, pe.getId())
                .putFloat("scale", 2);

        EntityGuardian guardian = new EntityGuardian(chunk, nbt);
        guardian.setImmobile(true);
        guardian.setPositionAndRotation(position, rotation.getY(), rotation.getX());
        guardian.setNameTag(SumoXStrings.POWERUP_ENTITY_NAME);
        guardian.setNameTagVisible(true);
        guardian.setNameTagAlwaysVisible(true);
        guardian.setScale(2);

        level.addSound(position, Sound.RANDOM_EXPLODE, 0.7f, 1f, gameHandler.getPlayers());
        level.addParticleEffect(position, ParticleEffect.HUGE_EXPLOSION_LEVEL);

        powerUpEntities.add(guardian);
        for(Player player: getGameHandler().getPlayers()){
            player.sendMessage(Utility.generateServerMessage("POWER-UP", TextFormat.YELLOW, "A Power-Up Guardian has spawned!", TextFormat.GOLD));
        }

        return guardian;
    }

    public int generateNewTime(GBehaveSumoBase behaviours){
        Random r = new Random();
        return behaviours.getMinimumPowerUpSpawnTime() + r.nextInt(behaviours.getVariationPowerUpSpawnTime());
    }

    protected boolean runPowerUp(PowerUp powerUp, PowerUpContext context){
        String name = String.format("%s%s%s", TextFormat.DARK_PURPLE, TextFormat.BOLD, powerUp.getName());
        if(powerUp.isConsumedImmediatley()){
            boolean result = powerUp.use(context);
            if(result){
                context.getPlayer().getLevel().addSound(
                        context.getPlayer().getPosition(),
                        powerUp.useSound(),
                        powerUp.useSoundVolume(),
                        powerUp.useSoundPitch(),
                        gameHandler.getPlayers());
                context.getPlayer().getLevel().addParticleEffect(context.getPlayer().getPosition().add(0, 2.2f, 0), ParticleEffect.BASIC_CRIT);
                context.getPlayer().sendMessage(Utility.generateServerMessage("POWER-UP", TextFormat.BLUE, String.format("You just activated the %s %s%spower-up!", name, TextFormat.RESET, TextFormat.BLUE)));
                context.getPlayer().sendMessage(Utility.generateServerMessage("POWER-UP", TextFormat.BLUE, powerUp.getDescription()));
                context.getPlayer().sendMessage(Utility.generateServerMessage("POWER-UP", TextFormat.BLUE, powerUp.getUsage()));
                return true;
            }
        } else {
            Item item = getItem(powerUp.getItemID());
            CompoundTag tag = item.hasCompoundTag() ? item.getNamedTag() : new CompoundTag();
            tag.putString(SumoXKeys.NBT_POWERUP_ITEM_TIE, String.valueOf(powerUpItemCount));
            item.setCompoundTag(tag);
            item.setCustomName(String.format("%s - %s%s%s", name, TextFormat.LIGHT_PURPLE, TextFormat.BOLD, powerUp.getUsage()));
            context.getPlayer().getInventory().addItem(item);
            context.getPlayer().sendMessage(Utility.generateServerMessage("POWER-UP", TextFormat.BLUE, String.format("You just recieved the %s %s%spower-up!", name, TextFormat.RESET, TextFormat.BLUE)));
            context.getPlayer().sendMessage(Utility.generateServerMessage("POWER-UP", TextFormat.BLUE, powerUp.getDescription()));

            pendingPowerUps.put(String.valueOf(powerUpItemCount), powerUp);
            powerUpItemCount++;
            return true;
        }
        return false;
    }

    private Item getItem(Integer id){
        if(id != null && id != 0){
            if(id < 256 && Block.list[id] != null) return Item.get(id);
            if(id < 65535 && Item.list[id] != null) return Item.get(id);
        }

        return Item.get(ItemID.BEETROOT);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityHit(EntityDamageByEntityEvent event){
        if(event.getDamager() instanceof Player){
            Player attacker = (Player) event.getDamager();

            if(getGameHandler().getPlayers().contains(attacker)){
                Optional<Team> t = getGameHandler().getPlayerTeam(attacker);

                if(t.isPresent() && t.get().isActiveGameTeam()) {

                    if (event.getEntity().namedTag.contains(SumoXKeys.NBT_POWERUP_PE_TIE)) {

                        String s = event.getEntity().namedTag.getString(SumoXKeys.NBT_POWERUP_PE_TIE);

                        if (s != null) {

                            if (getGameHandler().getGameBehaviors() instanceof GBehaveSumoBase) {
                                PowerUpContext context = new PowerUpContext(attacker);
                                event.setCancelled(true);

                                getGameHandler().addRewardChunk(attacker, new RewardChunk("powerup", "Power-Up Pickup", 3, 0, 1));

                                int powerPoolWeight = 0;
                                for(PowerUp e: powerUpPool) powerPoolWeight += Math.max(e.getTotalWeight(context), 0);

                                int selection = powerPoolWeight <= 0 ? 0 : new Random().nextInt(powerPoolWeight);
                                int cumulativeWeightChecked = 0;

                                for (PowerUp entry : powerUpPool) {
                                    int weight = entry.getTotalWeight(context);

                                    if (selection < (cumulativeWeightChecked + weight)) {
                                        boolean result = runPowerUp(entry, context);

                                        if (result) {
                                            event.getEntity().getLevel().addSound(event.getEntity().getPosition(), Sound.MOB_WITHER_BREAK_BLOCK, 0.5f, 0.9f, gameHandler.getPlayers());
                                            event.getEntity().close();
                                            powerUpEntities.remove(event.getEntity());
                                            GBehaveSumoBase behaviours = (GBehaveSumoBase) getGameHandler().getGameBehaviors();
                                            behaviours.getPowerUpPointCooldowns().put(s, generateNewTime(behaviours));
                                            return;
                                        }
                                    }
                                    cumulativeWeightChecked += weight;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemUse(PlayerInteractEvent event){
        if(gameHandler.getPlayers().contains(event.getPlayer()) && event.getItem().hasCompoundTag()){
            CompoundTag nbt = event.getItem().getNamedTag();

            if(nbt.contains(SumoXKeys.NBT_POWERUP_ITEM_TIE) && !powerUpItemCooldowns.contains(event.getPlayer())){
                powerUpItemCooldowns.add(event.getPlayer());
                gameHandler.getGameScheduler().registerGameTask(() -> powerUpItemCooldowns.remove(event.getPlayer()), 20);


                String id = nbt.getString(SumoXKeys.NBT_POWERUP_ITEM_TIE);
                if(pendingPowerUps.containsKey(id)){
                    PowerUp p = pendingPowerUps.get(id);
                    if(p.use(new PowerUpContext(event.getPlayer()))){
                        event.getPlayer().getLevel().addSound(
                                event.getPlayer().getPosition(),
                                p.useSound(),
                                p.useSoundVolume(),
                                p.useSoundPitch(),
                                gameHandler.getPlayers());
                        event.getPlayer().getInventory().remove(event.getItem());
                    }
                } else {
                    event.getPlayer().sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "The powerup you just used was broken. Oops!", TextFormat.RED));
                    event.getPlayer().getInventory().remove(event.getItem());
                }
                event.setCancelled(true);
            }
        }
    }

}
