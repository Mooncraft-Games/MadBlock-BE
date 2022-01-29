package org.madblock.gamemodesumox.powerup;

import cn.nukkit.Player;
import cn.nukkit.block.BlockAir;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.item.*;
import cn.nukkit.level.Sound;
import cn.nukkit.scheduler.Task;
import cn.nukkit.scheduler.TaskHandler;
import org.madblock.gamemodesumox.SumoX;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.kits.Kit;
import org.madblock.newgamesapi.util.Utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class PowerUpImmunity extends PowerUp implements Listener {

    private HashMap<Player, AtomicInteger> immunityPowerUps;
    private final TaskHandler armourTask;

    public static final int IMMUNITY_LENGTH = 7;

    public PowerUpImmunity(GameHandler gameHandler) {
        super(gameHandler);
        this.immunityPowerUps = new HashMap<>();

        SumoX.get().getServer().getPluginManager().registerEvents(this, SumoX.get());

        this.armourTask = this.gameHandler.getGameScheduler().registerGameTask(() -> {
            for(Player player: new ArrayList<>(this.immunityPowerUps.keySet())) {
                int newCounter = this.immunityPowerUps.get(player).decrementAndGet();

                if (newCounter < 1) {
                    Kit kit = this.gameHandler.getAppliedSessionKits().get(player);

                    if (kit != null) {
                        player.getInventory().setArmorContents(
                                new Item[]{
                                        kit.getKitHelmet().orElse(new BlockAir().toItem()),
                                        kit.getKitChestplate().orElse(new BlockAir().toItem()),
                                        kit.getKitLeggings().orElse(new BlockAir().toItem()),
                                        kit.getKitBoots().orElse(new BlockAir().toItem()),
                                });

                    } else {
                        player.getInventory().setArmorContents(
                                new Item[]{
                                        new BlockAir().toItem(),
                                        new BlockAir().toItem(),
                                        new BlockAir().toItem(),
                                        new BlockAir().toItem()
                                });
                    }

                    this.immunityPowerUps.remove(player);
                    this.gameHandler.getScoreboardManager().setLine(player, 2, null);

                } else {
                    this.gameHandler.getScoreboardManager().setLine(player, 2,
                            String.format("%s%s %s",
                                    Utility.ResourcePackCharacters.ARMOUR_FULL1,
                                    Utility.ResourcePackCharacters.TIMER,
                                    newCounter
                            )
                    );
                }
            }
        }, 20,20);
    }

    @Override
    public String getName() {
        return "Immunity";
    }

    @Override
    public String getDescription() {
        return "Makes you immune to all damage for "+IMMUNITY_LENGTH+" seconds.";
    }

    @Override
    public String getUsage() {
        return "You are now immune for "+IMMUNITY_LENGTH+" seconds.";
    }

    @Override
    public Sound useSound() {
        return Sound.HIT_ANVIL;
    }

    @Override
    public float useSoundPitch() {
        return 0.9f;
    }

    @Override
    public int getWeight() {
        return 100;
    }

    @Override
    public Integer getItemID() {
        return ItemID.IRON_INGOT;
    }

    @Override
    public boolean isConsumedImmediatley() {
        return true;
    }

    @Override
    public boolean use(PowerUpContext context) {
        if(immunityPowerUps.containsKey(context.getPlayer()))
            immunityPowerUps.get(context.getPlayer()).addAndGet(IMMUNITY_LENGTH);
        else
            immunityPowerUps.put(context.getPlayer(), new AtomicInteger(IMMUNITY_LENGTH));

        context.getPlayer().getInventory().setArmorContents(
                new Item[]{
                        new ItemHelmetIron(),
                        new ItemChestplateIron(),
                        new ItemLeggingsIron(),
                        new ItemBootsIron()
                });

        return true;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void getJellyBaboon(EntityDamageByEntityEvent event){
        if(event.getEntity() instanceof Player && event.getDamager() instanceof Player){
            Player player = (Player) event.getEntity();
            Player attacker = (Player) event.getDamager();

            if(gameHandler.getPlayers().contains(player)){
                if(immunityPowerUps.containsKey(player) && immunityPowerUps.get(player).get() > 0) {
                    player.getLevel().addSound(player.getPosition(), Sound.RANDOM_ANVIL_LAND, 0.4f, 1.2f, player, attacker);
                    attacker.sendTitle(Utility.ResourcePackCharacters.ARMOUR_FULL1, "* KB Immune *", 3, 10, 7);
                    event.setCancelled(true);
                }
            }
        }
    }



    @Override
    public void cleanUp() {
        HandlerList.unregisterAll(this);
        this.armourTask.cancel();
    }
}
