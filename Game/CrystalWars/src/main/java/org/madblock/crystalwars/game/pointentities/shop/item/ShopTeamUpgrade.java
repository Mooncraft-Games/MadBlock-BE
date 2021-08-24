package org.madblock.crystalwars.game.pointentities.shop.item;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.utils.TextFormat;
import org.madblock.crystalwars.game.CrystalWarsGame;
import org.madblock.crystalwars.game.upgrades.CrystalTeamUpgrade;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.team.Team;

import java.util.Optional;
import java.util.function.Consumer;

public class ShopTeamUpgrade implements IShopData {
    protected CrystalWarsGame gameBehavior;
    protected CrystalTeamUpgrade upgrade;
    protected Item soldItem = null;
    protected String image = null;
    protected Optional<Consumer<Player>> purchaseCallback = Optional.empty();

    public ShopTeamUpgrade(CrystalTeamUpgrade givenUpgrade, Item cost, String imageLink, CrystalWarsGame base) {
        gameBehavior = base;
        upgrade = givenUpgrade;
        soldItem = cost;
        image = imageLink;
    }

    public ShopTeamUpgrade(CrystalTeamUpgrade givenUpgrade, Item cost, CrystalWarsGame base) {
        gameBehavior = base;
        upgrade = givenUpgrade;
        soldItem = cost;
    }

    public ShopTeamUpgrade(CrystalTeamUpgrade givenUpgrade, CrystalWarsGame base) {
        gameBehavior = base;
        upgrade = givenUpgrade;
    }

    @Override
    public boolean onQuery(Player player) {
        return soldItem != null && player.getInventory().contains(soldItem) && !gameBehavior.getSessionHandler().getPlayerTeam(player).filter(t -> gameBehavior.doesTeamHaveUpgrade(t, upgrade)).isPresent();
    }

    @Override
    public void onPurchase(Player player) {
        Optional<Team> team = gameBehavior.getSessionHandler().getPlayerTeam(player);
        if (team.isPresent() && team.get().isActiveGameTeam()) {
            gameBehavior.addUpgradeForTeam(team.get(), upgrade);
            player.getInventory().removeItem(soldItem);
            player.getInventory().sendContents(player);
            for (Player teammate : team.get().getPlayers()) {
                if (teammate != player) {
                    teammate.sendMessage(getPurchaseMessage(player));
                }
            }
            purchaseCallback.ifPresent(playerConsumer -> playerConsumer.accept(player));
        }
    }

    @Override
    public String getLabel() {
        StringBuilder label = new StringBuilder(upgrade.getName()).append('\n');
        label.append(TextFormat.RESET);
        String character = null;

        if (soldItem != null) {
            switch (soldItem.getId()) {
                case ItemID.GOLD_INGOT:
                    character = Utility.ResourcePackCharacters.GOLD_INGOT;
                    break;
                case ItemID.IRON_INGOT:
                    character = Utility.ResourcePackCharacters.IRON_INGOT;
                    break;
                case ItemID.DIAMOND:
                    character = Utility.ResourcePackCharacters.DIAMOND;
                    break;
            }
            label.append(soldItem.getCount()).append(" ").append(character);
        } else {
            label.append("Max Reached");
        }
        return label.toString();
    }

    @Override
    public String getPurchaseMessage(Player player) {
        return Utility.generateServerMessage("Game", TextFormat.BLUE, TextFormat.YELLOW +
                TextFormat.clean(upgrade.getName()) + Utility.DEFAULT_TEXT_COLOUR + " has been purchased for the team.");
    }

    @Override
    public String getFailedToPurchaseMessage(Player player) {
        if (soldItem != null && !player.getInventory().contains(soldItem)) {
            return Utility.generateServerMessage("Game", TextFormat.BLUE, "You do not have enough resources to " +
                    "buy this upgrade!", TextFormat.RED);
        } else {
            return Utility.generateServerMessage("Game", TextFormat.BLUE, "Your team already owns this upgrade!",
                    TextFormat.RED);
        }
    }

    @Override
    public String getImage() {
        return image;
    }

    public void setPurchaseCallback(Consumer<Player> callback) {
        purchaseCallback = Optional.of(callback);
    }

}