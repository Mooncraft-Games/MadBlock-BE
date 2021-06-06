package org.madblock.newgamesapi.kits.hub;

import cn.nukkit.Player;
import cn.nukkit.block.BlockAir;
import cn.nukkit.block.BlockChest;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.entity.EntityArmorChangeEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.inventory.InventoryMoveItemEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerToggleSprintEvent;
import cn.nukkit.inventory.InventoryHolder;
import cn.nukkit.item.*;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import cn.nukkit.potion.Effect;
import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.utils.DyeColor;
import cn.nukkit.utils.TextFormat;
import org.madblock.lib.stattrack.statistic.StatisticCollection;
import org.madblock.lib.stattrack.statistic.StatisticEntitiesList;
import org.madblock.lib.stattrack.statistic.StatisticWatcher;
import org.madblock.lib.stattrack.util.Util;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.book.BookConfiguration;
import org.madblock.newgamesapi.game.NavigationManager;
import org.madblock.newgamesapi.kits.ExtendedKit;
import org.madblock.newgamesapi.kits.Kit;
import org.madblock.newgamesapi.registry.LibraryRegistry;

import java.util.Optional;

public class KitHub extends Kit {

    public static final float VANILLA_BASE_SPEED = 0.1f;
    public static final float VANILLA_BASE_SPRINT_MULTIPLIER = 1.3f;
    public static final float SPEED_MULTIPLIER = 2f;

    @Override public String getKitID() {
        return "hub_default";
    }
    @Override public String getKitDisplayName() {
        return "Player";
    }
    @Override public String getKitDescription() {
        return "Don't know how you got here but yeah, here's the internal hub kit.";
    }

    @Override public Item[] getHotbarItems() {
        // Hub selector (Configurable!
        Item hubselector = new ItemCompass()
                .setCustomName(String.format("%s%sHub Selector", TextFormat.BOLD, TextFormat.YELLOW));
        hubselector.getNamedTag().putBoolean("isItemHubSelector", true);

        // Hub selector (Configurable!
        Item leap = new ItemFeather()
                .setCustomName(String.format("%s%sLeap", TextFormat.BOLD, TextFormat.AQUA));
        leap.getNamedTag().putBoolean("isLeapFeather", true);



        // Friends menu item
        Item friends = new ItemCake()
                .setCustomName(String.format("%s%sFriends", TextFormat.BOLD, TextFormat.RED));
        friends.getNamedTag().putBoolean("isFriendsMenu", true);

        // Crates menu item
        Item crates = new BlockChest().toItem()
                .setCustomName(String.format("%s%sCrates", TextFormat.BOLD, TextFormat.LIGHT_PURPLE));
        crates.getNamedTag().putBoolean("isCrateMenu", true);

        // Wardrobe menu item
        Item wardrobe = new ItemChestplateLeather().setColor(DyeColor.GREEN)
                .setCustomName(String.format("%s%sCosmetics", TextFormat.BOLD, TextFormat.BLUE));
        wardrobe.getNamedTag().putBoolean("isWardrobeMenu", true);

        // Changelog book (Configurable!)
        ItemBookWritten changelog = (ItemBookWritten) new ItemBookWritten()
                .setCustomName(String.format("%s%sChangelog", TextFormat.BOLD, TextFormat.GOLD));
        Optional<BookConfiguration> changelogBook = LibraryRegistry.get().getBook("changelog");

        if(changelogBook.isPresent()){
            changelogBook.get().applyBookBlueprint(changelog);

        } else {
            changelog.setTitle(String.format("%s%sBroken Changelog :)", TextFormat.BOLD, TextFormat.GOLD));
            changelog.setAuthor("MadBlock Dev Team");
            changelog.setPageText(0, TextFormat.RED+"Error! It appears someone broke our changelog!\n\n- MadBlock Dev Team\n:)");
        }
        changelog.addEnchantment(Enchantment.get(Enchantment.ID_BOW_INFINITY));
        changelog.getNamedTag().putBoolean("isChangelog", true);

        // Discord link item
        Item discord = new ItemDye(DyeColor.PURPLE)
                .setCustomName(String.format("%s%sDiscord", TextFormat.BOLD, TextFormat.BLUE));
        discord.getNamedTag().putBoolean("isDiscordLink", true);


        return new Item[]{
                hubselector,
                leap, // Leap feather
                new BlockAir().toItem(),
                friends,
                crates,
                wardrobe,
                new BlockAir().toItem(),
                changelog,
                discord
        };
    }

    @Override
    public void onKitEquip(Player player) {
        // player.addEffect(Effect.getEffect(Effect.JUMP).setAmplifier(2).setDuration(100000).setVisible(false)); no more jump boost!
        if(player.isSprinting()){
            player.setMovementSpeed((VANILLA_BASE_SPEED * VANILLA_BASE_SPRINT_MULTIPLIER) * SPEED_MULTIPLIER); //Vanilla is a 30% increase
        } else {
            player.setMovementSpeed(VANILLA_BASE_SPEED * SPEED_MULTIPLIER);
        }
    }

    @Override
    public void onKitUnequip(Player player) {
        player.setMovementSpeed(0.1f);
        player.removeAllEffects();
    }

    @Override
    public Optional<Class<? extends ExtendedKit>> getExtendedKitFeatures() {
        return Optional.of(ExtendedKitHub.class);
    }



    public static class ExtendedKitHub extends ExtendedKit {

        protected boolean itemCooldown = false;
        protected int bonusJumpCount = 0;

        @Override
        protected void onPrepareExtendedKit() {

            gameHandler.getGameScheduler().registerGameTask(() -> {
                if(target.isOnGround() && (bonusJumpCount >= 10)) {
                    int count = bonusJumpCount;
                    this.bonusJumpCount = 0;

                    NewGamesAPI1.get().getServer().getScheduler().scheduleAsyncTask(NewGamesAPI1.get(), new AsyncTask() {
                        @Override
                        public void onRun() {
                            StatisticCollection pStat = StatisticEntitiesList.get().createCollection(Util.getPlayerEntityID(target));
                            StatisticWatcher w = pStat.createStatistic("lobby_leap_streak", false);
                            w.fetchRemote();

                            if(w.getValueDelta() < count) {
                                w.resetLocal();
                                w.modify(count); // Ensure no old streak is saved
                            }
                            if(w.getValueRemote() < count) {
                                w.resetRemote();
                                w.pushRemote();
                            }
                        }
                    });
                }
            }, 1, 1);
        }

        @EventHandler
        public void onSprintChange(PlayerToggleSprintEvent event){
            if(checkEventIsForTargetPlayer(event.getPlayer())){
                if(event.isSprinting()){
                    event.getPlayer().setMovementSpeed((VANILLA_BASE_SPEED * VANILLA_BASE_SPRINT_MULTIPLIER) * SPEED_MULTIPLIER); //Vanilla is a 30% increase
                } else {
                    event.getPlayer().setMovementSpeed(VANILLA_BASE_SPEED * SPEED_MULTIPLIER);
                }
            }
        }

        @EventHandler
        public void onEquipArmor(EntityArmorChangeEvent event) {
            if (event.getEntity().equals(target) && isCorrectItem(event.getNewItem(), "isWardrobeMenu")) {
                event.setCancelled();
            }
        }

        @EventHandler
        public void onItemInteract(PlayerInteractEvent event){

            if(checkEventIsForTargetPlayer(event.getPlayer()) && !itemCooldown) {
                StatisticCollection pStat = StatisticEntitiesList.get().createCollection(Util.getPlayerEntityID(event.getPlayer()));

                itemCooldown = true;
                getGameHandler().getGameScheduler().registerGameTask(() -> { itemCooldown = false; }, 10);
                Item item = event.getItem();

                if(isCorrectItem(item, "isItemHubSelector")) {
                    event.setCancelled();
                    NavigationManager.get().openQuickLobbyMenu(target);
                }

                if(isCorrectItem(item, "isLeapFeather")) {

                    if(!event.getPlayer().isOnGround()) {
                        bonusJumpCount++;

                        if(bonusJumpCount > 1) {
                            String text = Utility.generateServerMessage("SECRET", TextFormat.GOLD, "Leap 'bug' streak of: "+TextFormat.GOLD+TextFormat.BOLD+String.valueOf(bonusJumpCount));
                            event.getPlayer().sendMessage(text);
                            event.getPlayer().clearTitle();
                            event.getPlayer().sendActionBar(text);
                        }

                        if(bonusJumpCount == 10) {
                            event.getPlayer().getLevel().addSound(event.getPlayer().getPosition(), Sound.RANDOM_LEVELUP);
                            String text = Utility.generateServerMessage("SECRET", TextFormat.GOLD, "AMAZING!");
                            event.getPlayer().sendMessage(text);
                            event.getPlayer().clearTitle();
                            event.getPlayer().sendActionBar(text);
                        }

                        if(bonusJumpCount == 100) {
                            event.getPlayer().getLevel().addSound(event.getPlayer().getPosition(), Sound.RANDOM_EXPLODE);
                            event.getPlayer().getLevel().addParticleEffect(event.getPlayer().getPosition(), ParticleEffect.HUGE_EXPLOSION_LEVEL);
                            String text = Utility.generateServerMessage("SECRET", TextFormat.GOLD, "That's dedication, well done! Keep going :)");
                            event.getPlayer().sendMessage(text);
                            event.getPlayer().clearTitle();
                            event.getPlayer().sendActionBar(text);
                        }

                        if(bonusJumpCount == 1000) {
                            String t = Utility.generateServerMessage("SECRET", TextFormat.LIGHT_PURPLE, String.format("Congrats to %s%s%s%s%s for reaching a jump 'bug' streak of %s%s1000!",
                                    TextFormat.LIGHT_PURPLE,
                                    TextFormat.BOLD,
                                    event.getPlayer().getName(),
                                    TextFormat.RESET,
                                    TextFormat.DARK_PURPLE,
                                    TextFormat.LIGHT_PURPLE,
                                    TextFormat.BOLD

                                    ), TextFormat.DARK_PURPLE);

                            for(Player player: NewGamesAPI1.get().getServer().getOnlinePlayers().values()) {
                                player.sendMessage(t);
                                player.getLevel().addSound(player.getPosition().add(8f), Sound.MOB_ENDERDRAGON_GROWL, 0.6f, 0.9f);
                            }
                            String text = Utility.generateServerMessage("SECRET", TextFormat.LIGHT_PURPLE, "You're actually crazy, send us a screenshot for some XP!");
                            event.getPlayer().getLevel().addParticleEffect(event.getPlayer().getPosition(), ParticleEffect.HUGE_EXPLOSION_LEVEL);
                            event.getPlayer().sendMessage(text);
                            event.getPlayer().clearTitle();
                            event.getPlayer().sendActionBar(text);
                        }
                    }

                    event.setCancelled();
                    event.getPlayer().getLevel().addSound(event.getPlayer().getPosition(), Sound.BLOCK_BEEHIVE_ENTER);
                    Vector3 dir = event.getPlayer().getDirectionVector();
                    float tweak = dir.y < 0 ? 0.65f : 1f;
                    event.getPlayer().setMotion(new Vector3(dir.x, Math.abs(dir.y * tweak), dir.z).multiply(1.8f)); // 1.62 taken from SumoX
                    pStat.createStatistic("lobby_leap_total").increment();
                }


                if(isCorrectItem(item, "isFriendsMenu")) {
                    event.setCancelled();
                    NewGamesAPI1.get().getServer().getCommandMap().dispatch(event.getPlayer(), "friend");
                }

                if(isCorrectItem(item, "isCrateMenu")) {
                    event.setCancelled();
                    event.getPlayer().sendMessage(Utility.generateServerMessage("Crates", TextFormat.LIGHT_PURPLE, "Crates are coming soonTM! Keep an eye out for updates on discord!"));
                    event.getPlayer().clearTitle();
                    event.getPlayer().sendActionBar(TextFormat.LIGHT_PURPLE+"Crates are coming soonTM!");
                }

                if(isCorrectItem(item, "isWardrobeMenu")) {
                    event.setCancelled();
                    event.getPlayer().sendMessage(Utility.generateServerMessage("Cosmetics", TextFormat.DARK_AQUA, "Cosmetics are coming soonTM! Keep an eye out for updates on discord!"));
                    event.getPlayer().clearTitle();
                    event.getPlayer().sendActionBar(TextFormat.DARK_AQUA+"Cosmetics are coming soonTM!");
                }

                // No extra behaviour for the changelog.

                if(isCorrectItem(item, "isDiscordLink")) {
                    event.setCancelled();
                    event.getPlayer().sendMessage(Utility.generateServerMessage("Discord", TextFormat.DARK_PURPLE, "You can join our discord at: "+TextFormat.LIGHT_PURPLE+"https://discord.gg/nqNcvpU"));
                    event.getPlayer().clearTitle();
                    event.getPlayer().sendActionBar(TextFormat.GRAY+"Join the discord today at: "+TextFormat.LIGHT_PURPLE+" https://discord.gg/nqNcvpU");
                }

                if(isCorrectItem(item, "isStoreMenu")) {
                    event.setCancelled();
                    event.getPlayer().sendMessage(Utility.generateServerMessage("Store", TextFormat.DARK_GREEN, "You can view our store at: "+TextFormat.GREEN+"https://mooncraftgames-bedrock.tebex.io/"));
                    event.getPlayer().clearTitle();
                    event.getPlayer().sendActionBar(TextFormat.GRAY+"View our store: "+TextFormat.GREEN+"https://mooncraftgames-bedrock.tebex.io/");
                }
            }
        }

        @EventHandler
        public void onInventoryTransaction(EntityArmorChangeEvent event) {

            if(checkEventIsForTargetPlayer(event.getEntity())){

                if(isCorrectItem(event.getNewItem(), "isWardrobeMenu")){
                    event.setCancelled(true);
                }
            }
        }

        @EventHandler
        public void onInventoryTransaction(InventoryMoveItemEvent event) {
            InventoryHolder sourceHolder = event.getSource();
            InventoryHolder destHolder = event.getTargetInventory().getHolder();
            if (sourceHolder instanceof Player){
                if (checkEventIsForTargetPlayer((Player) sourceHolder)) {
                    event.setCancelled(true);
                    return;
                }
            }
            if (destHolder instanceof Player){
                if (checkEventIsForTargetPlayer((Player) destHolder)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }


        public static boolean isCorrectItem(Item item, String tag){
            return item != null
                    && item.hasCompoundTag()
                    && item.getNamedTag().exist(tag)
                    && item.getNamedTag().getBoolean(tag);
        }
    }
}
