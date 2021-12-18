package org.madblock.newgamesapi.game;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.level.Location;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.team.Team;
import org.madblock.ranks.api.RankManager;
import org.madblock.ranks.api.RankProfile;

import java.util.*;

public class NavigationManager implements Listener {

    public static final String TYPE_QUICK_LOBBY_SELECTOR = "quicklobby";
    public static final String TYPE_SPECTATE_PLAYER = "spec_player";

    //TODO: Yeet this entirely and make a form manager.

    private static NavigationManager managerInstance;

    public HashMap<Integer, String> simpleFormType;
    public HashMap<Integer, String[]> offeredSimpleFormOptions;

    public NavigationManager(){
        this.simpleFormType = new HashMap<>();
        this.offeredSimpleFormOptions = new HashMap<>();

        NewGamesAPI1.get().getServer().getPluginManager().registerEvents(this, NewGamesAPI1.get());
    }

    public void setAsPrimaryManager(){
        if(managerInstance == null) managerInstance = this;
    }

    public void openQuickLobbyMenu(Player target){
        ArrayList<ElementButton> buttons = new ArrayList<>();
        ArrayList<String> optionKeys = new ArrayList<>();

        for(Map.Entry<String, GameID> game : HubManager.get().getHubGames().entrySet()){
            boolean hide = false;
            for(String permission: game.getValue().getGameProperties().getRequiredPermissions()){
                Optional<RankProfile> profile = RankManager.getInstance().getRankProfile(target);
                if (!profile.isPresent() || !profile.get().hasPermission(permission)) {
                    hide = true;
                    break;
                }
            }

            if(!hide){
                optionKeys.add(game.getKey());
                buttons.add(new ElementButton(TextFormat.BLUE+""+TextFormat.BOLD+game.getValue().getGameDisplayName()));
            }
        }

        FormWindowSimple form = new FormWindowSimple("Lobby Navigator", "Please select the game lobby you would like to visit.", buttons);
        int id = target.showFormWindow(form);
        simpleFormType.put(id, TYPE_QUICK_LOBBY_SELECTOR);
        offeredSimpleFormOptions.put(id, optionKeys.toArray(new String[0]));
    }

    public void openSpectateMenu(Player target){
        GameHandler game = GameManager.get().getPlayerLookup().get(target.getUniqueId());
        if(game != null) {
            ArrayList<ElementButton> buttons = new ArrayList<>();
            ArrayList<String> optionKeys = new ArrayList<>();

            for (Team team : game.getTeams().values()) {

                if(team.isActiveGameTeam()) {

                    for(Player player: team.getPlayers()) {
                        optionKeys.add(player.getUniqueId().toString());
                        buttons.add(new ElementButton(TextFormat.GOLD + "" + TextFormat.BOLD + player.getDisplayName()));
                    }
                }
            }

            FormWindowSimple form = new FormWindowSimple(TextFormat.DARK_BLUE+"Spectate Player", "Please select a player to teleport to.", buttons);
            int id = target.showFormWindow(form);
            simpleFormType.put(id, TYPE_SPECTATE_PLAYER);
            offeredSimpleFormOptions.put(id, optionKeys.toArray(new String[0]));
        } else {
            target.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You're not in a game. This is funky.", TextFormat.RED));
        }
    }

    @EventHandler
    public void onFormWindow(PlayerFormRespondedEvent event){
        if(offeredSimpleFormOptions.containsKey(event.getFormID())){
            String[] options = offeredSimpleFormOptions.remove(event.getFormID());

            if(event.getResponse() instanceof FormResponseSimple){
                FormResponseSimple response = (FormResponseSimple) event.getResponse();
                int clickedButton = response.getClickedButtonId();

                if(clickedButton < options.length && simpleFormType.containsKey(event.getFormID())) {

                    switch (simpleFormType.get(event.getFormID())){
                        case TYPE_QUICK_LOBBY_SELECTOR:
                            String lobbyType = options[clickedButton];

                            Optional<GameHandler> hub = HubManager.get().getAvailableHub(lobbyType, event.getPlayer());
                            if (hub.isPresent()) {
                                if(!hub.get().addPlayerToGame(event.getPlayer())) {
                                    event.getPlayer().sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "The hub system broke and we couldn't make a game of this lobby right now. Tell a developer pls. <3", TextFormat.RED));
                                    return;
                                }
                            }
                            break;


                        case TYPE_SPECTATE_PLAYER:
                            String playerId = options[clickedButton];
                            try {
                                UUID uuid = UUID.fromString(playerId);
                                Optional<Player> p = NewGamesAPI1.get().getServer().getPlayer(uuid);

                                if(p.isPresent()){
                                    Location loc = p.get().getLocation();

                                    if(loc.getLevel() == event.getPlayer().getLevel()){
                                        event.getPlayer().teleportImmediate(p.get().getLocation());
                                        event.getPlayer().sendMessage(Utility.generateServerMessage("SPECTATE", TextFormat.DARK_AQUA, String.format("You are now spectating %s%s", TextFormat.YELLOW, p.get().getDisplayName()), TextFormat.GRAY));
                                    } else {
                                        event.getPlayer().sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "The player is not in the same world as you. Tell a developer if this is weird. <3", TextFormat.RED));
                                    }

                                } else {
                                    event.getPlayer().sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "The player you selected is not online.", TextFormat.RED));
                                }
                            } catch (Exception err) {
                                event.getPlayer().sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Invalid player ID. Tell a developer pls. <3", TextFormat.RED));
                            }
                            break;
                    }

                } else {
                    event.getPlayer().sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "The form broke. Tell a developer pls. <3", TextFormat.RED));
                }
            }
        }
    }

    /** @return the primary instance of the Manager. */
    public static NavigationManager get() { return managerInstance; }

}
