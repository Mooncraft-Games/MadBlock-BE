package org.madblock.gamemodesumox.powerup;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDeathEvent;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import org.madblock.gamemodesumox.SumoUtil;
import org.madblock.gamemodesumox.SumoX;
import org.madblock.gamemodesumox.SumoXConstants;
import org.madblock.gamemodesumox.SumoXKeys;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.kits.Kit;

import java.util.ArrayList;
import java.util.HashMap;

public class PowerUpRecall extends PowerUp implements Listener {

    protected HashMap<Player, ArrayList<Vector3>> recallLists;

    public PowerUpRecall(GameHandler gameHandler) {
        super(gameHandler);

        this.recallLists = new HashMap<>();

        SumoX.get().getServer().getPluginManager().registerEvents(this, SumoX.get());
        gameHandler.getGameScheduler().registerGameTask(this::onSecondTick, 0 , SumoXConstants.POWERUP_RECALL_TICK_FRAMES);
    }

    @Override
    public String getName() {
        return "Recall";
    }

    @Override
    public String getDescription() {
        return "Allows you to leap in a specific direction.";
    }

    @Override
    public String getUsage() {
        return "Face a direction and tap on the ground. You'll go 'weeeee'";
    }

    @Override
    public Sound useSound() {
        return Sound.BLOCK_BEEHIVE_ENTER;
    }

    @Override
    public float useSoundPitch() {
        return 1f;
    }

    @Override
    public int getWeight() {
        return 100;
    }

    @Override
    public int getBonusWeight(PowerUpContext context) {
        Kit kit = gameHandler.getAppliedSessionKits().get(context.getPlayer());
        if(kit != null){
            return SumoUtil.StringToInt(kit.getProperty(SumoXKeys.KIT_PROP_RECALL_BONUS_WEIGHT).orElse(null)).orElse(0);
        }
        return 0;
    }

    @Override
    public Integer getItemID() {
        return ItemID.ENDER_PEARL;
    }

    @Override
    public boolean isConsumedImmediatley() {
        return false;
    }

    @Override
    public boolean use(PowerUpContext context) {
        if(recallLists.containsKey(context.getPlayer())){
            ArrayList<Vector3> posHistory = new ArrayList<>(recallLists.get(context.getPlayer()));
            int recallAmount = Math.min(SumoXConstants.POWERUP_RECALL_MAX_HISTORY, posHistory.size());

            for (int i = 0; i < recallAmount; i++) {
                int waitTicks = i * SumoXConstants.POWERUP_RECALL_REWIND_SPEED;
                int fI = i; // needs to be effectivley final for lambda
                boolean finalTick = i == (recallAmount - 1);

                context.getPlayer().setImmobile(true);

                gameHandler.getGameScheduler().registerGameTask(() -> {
                    context.getPlayer().setPosition(posHistory.get(fI));
                    context.getPlayer().sendPosition(posHistory.get(fI));

                    if(finalTick) {
                        context.getPlayer().teleport(posHistory.get(fI));
                        context.getPlayer().setImmobile(false);
                        context.getPlayer().getLevel().addSound(context.getPlayer().getPosition(), Sound.BEACON_DEACTIVATE, 0.6f, 0.7f);
                        context.getPlayer().getLevel().addParticleEffect(context.getPlayer().getPosition(), ParticleEffect.HUGE_EXPLOSION_LEVEL);

                    } else {
                        context.getPlayer().getLevel().addSound(context.getPlayer().getPosition(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.6f, 0.8f);
                    }
                }, waitTicks);
            }
        }
        return true;
    }

    @Override
    public void cleanUp() {
        HandlerList.unregisterAll(this);
    }

    public void onSecondTick(){
        for(Player player: gameHandler.getPlayers()){

            if (!recallLists.containsKey(player)) {
                recallLists.put(player, new ArrayList<>());
            }
            ArrayList<Vector3> positions = recallLists.get(player);
            positions.add(0, player.getPosition());

            int originalSize = positions.size();
            int historySize = SumoXConstants.POWERUP_RECALL_MAX_HISTORY;

            Kit kit = gameHandler.getAppliedSessionKits().get(player);

            if (kit != null) {
                historySize = SumoUtil.StringToInt(kit.getProperty(SumoXKeys.KIT_PROP_RECALL_TIME).orElse(null)).orElse(SumoXConstants.POWERUP_RECALL_MAX_HISTORY);
            }

            for (int i = historySize; i < originalSize; i++) {
                positions.remove(historySize);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(EntityDeathEvent event) {
        if(event.getEntity() instanceof Player){
            Player p = (Player) event.getEntity();

            if(recallLists.containsKey(p)) {
                recallLists.put(p, new ArrayList<>());
            }
        }
    }

}
