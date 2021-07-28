package org.madblock.crystalwars.game.pointentities.shop;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.passive.EntityVillager;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.event.player.PlayerInteractEntityEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementButtonImageData;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.level.Position;
import cn.nukkit.utils.TextFormat;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.madblock.crystalwars.CrystalWarsPlugin;
import org.madblock.crystalwars.game.CrystalWarsGame;
import org.madblock.crystalwars.game.pointentities.shop.item.IShopData;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.pointentities.PointEntityType;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.newgamesapi.team.Team;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * @author Nicholas
 */
public abstract class ShopPointEntity extends PointEntityType implements Listener {
    public static final String SHOP_DIRECTION_PROPERTY = "direction";
    public static final String NBT_SHOP_ID = "crystalwars_shop_id";

    protected final String entityTypeId;

    protected final BiMap<PointEntity, String> shopEntities = HashBiMap.create();

    protected final Set<Integer> openPlayerGuis = new HashSet<>();

    protected final CrystalWarsGame gameBehavior;

    public ShopPointEntity(String id, GameHandler handler, CrystalWarsGame base) {
        super(id, handler);
        entityTypeId = id;
        gameBehavior = base;
    }

    @Override
    public void onRegister() {
        CrystalWarsPlugin.getInstance().getServer().getPluginManager().registerEvents(this, CrystalWarsPlugin.getInstance());
    }

    @Override
    public void onUnregister() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void onAddPointEntity(PointEntity entity) {
        super.onAddPointEntity(entity);

        String shopId = UUID.randomUUID().toString();
        Entity shopEntity = Entity.createEntity(EntityVillager.NETWORK_ID, new Position(entity.getX(), entity.getY(), entity.getZ(), gameHandler.getPrimaryMap()));
        shopEntity.setNameTagAlwaysVisible(true);
        shopEntity.setNameTag(getShopNameHeader() + "\n" + getShopNameFooter());
        shopEntity.namedTag.putString(NBT_SHOP_ID, shopId);
        int yaw;
        switch (entity.getStringProperties().getOrDefault(SHOP_DIRECTION_PROPERTY, "north").toLowerCase()) {
            case "north":
                yaw = 0;
                break;
            case "south":
                yaw = 180;
                break;
            case "east":
                yaw = 270;
                break;
            case "west":
                yaw = 90;
                break;
            default:
                CrystalWarsPlugin.getInstance().getLogger().error(String.format("Invalid %s property for shop entity in MapID %s",
                        SHOP_DIRECTION_PROPERTY, gameHandler.getPrimaryMapID().getId()));
                return;
        }
        shopEntity.setRotation(yaw, 0);
        shopEntity.spawnToAll();
        shopEntities.put(entity, shopId);
    }

    @Override
    public void onRemovePointEntity(PointEntity entity) {
        super.onRemovePointEntity(entity);
    }

    @EventHandler
    public void onInteract(EntityDamageByEntityEvent event) {
        if (shouldOpenShopOnInteraction(event.getDamager(), event.getEntity())) {
            event.setCancelled(true);
            if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                openShopGUI((Player)event.getDamager());
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (shopEntities.inverse().containsKey(event.getEntity().namedTag.getString(NBT_SHOP_ID)))
            event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (shouldOpenShopOnInteraction(event.getPlayer(), event.getEntity())) {
            event.setCancelled(true);
            openShopGUI(event.getPlayer());
        }
    }

    @EventHandler
    public void onShopPurchase(PlayerFormRespondedEvent event) {
        if (openPlayerGuis.contains(event.getFormID())) {
            openPlayerGuis.remove(event.getFormID());
            if (!event.getWindow().wasClosed()) {

                FormResponseSimple response = (FormResponseSimple)event.getWindow().getResponse();
                IShopData chosenButton = getShopItems(event.getPlayer())[response.getClickedButtonId()];

                if (chosenButton.onQuery(event.getPlayer())) {
                    chosenButton.onPurchase(event.getPlayer());
                    event.getPlayer().sendMessage(chosenButton.getPurchaseMessage(event.getPlayer()));
                } else {
                    event.getPlayer().sendMessage(chosenButton.getFailedToPurchaseMessage(event.getPlayer()));
                }

                if (reopenOnQuery()) {
                    openShopGUI(event.getPlayer());
                }

            }
        }
    }

    protected abstract boolean reopenOnQuery();

    protected boolean shouldOpenShopOnInteraction(Entity playerEntity, Entity shopEntity) {
        if (playerEntity instanceof Player) {
            Optional<Team> playerTeam = gameHandler.getPlayerTeam((Player)playerEntity);
            return playerTeam.filter(Team::isActiveGameTeam).isPresent() &&
                    gameHandler.getPlayers().contains(playerEntity) &&
                    shopEntities.inverse().containsKey(shopEntity.namedTag.getString(NBT_SHOP_ID));
        } else {
            return false;
        }
    }

    protected void openShopGUI(Player player) {

        FormWindowSimple form = new FormWindowSimple(getTitle(player), "");
        for (IShopData item : getShopItems(player)) {
            if (item.getImage() != null) {
                form.addButton(new ElementButton(item.getLabel(), new ElementButtonImageData(ElementButtonImageData.IMAGE_DATA_TYPE_PATH, item.getImage())));
            } else {
                form.addButton(new ElementButton(item.getLabel()));
            }
        }

        int formId = player.showFormWindow(form);
        openPlayerGuis.add(formId);
    }

    protected abstract IShopData[] getShopItems(Player player);

    protected abstract String getTitle(Player player);

    protected abstract String getShopNameHeader();

    protected String getShopNameFooter() {
        return "" + TextFormat.RED + TextFormat.BOLD + "TAP TO OPEN";
    }
}