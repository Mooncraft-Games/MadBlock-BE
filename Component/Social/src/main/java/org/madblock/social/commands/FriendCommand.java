package org.madblock.social.commands;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.command.data.CommandEnum;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.utils.TextFormat;
import org.madblock.playerregistry.PlayerRegistry;
import org.madblock.social.SocialAPI;
import org.madblock.social.Utility;
import org.madblock.social.friends.Friend;
import org.madblock.social.friends.comparators.FriendComparator;
import org.madblock.social.friends.FriendsManager;

import java.sql.SQLException;
import java.util.*;

public class FriendCommand extends PluginCommand<SocialAPI> {

    // How many items do we want to list for commands that send a list of players in chat?
    private final int ITEMS_TO_SHOW = 10;

    public FriendCommand(SocialAPI plugin) {
        super("friend", plugin);
        setAliases(new String[]{ "friends", "f" });
        setDescription("Add other players to your friend lists");
        setUsage(
                "/friend - View GUI\n" +
                "/friend add <username> - Send a friend request to a player\n" +
                "/friend remove <username> - Remove a player from your friends list\n" +
                "/friend accept <username> - Accept a incoming friend request\n" +
                "/friend deny <username> - Reject a incoming friend request\n" +
                "/friend cancel <username> - Cancel a outgoing friend request\n" +
                "/friend teleport <username> - Teleport to the server of a player\n" +
                "/friend list [page] - Retrieve a list of your friends"
        );

        getCommandParameters().clear();
        getCommandParameters().put("add", new CommandParameter[]{
                CommandParameter.newEnum("add", new CommandEnum("add")),
                CommandParameter.newType("username", CommandParamType.TARGET)
        });
        getCommandParameters().put("remove", new CommandParameter[]{
                CommandParameter.newEnum("remove", new CommandEnum("remove")),
                CommandParameter.newType("username", CommandParamType.TARGET)
        });
        getCommandParameters().put("accept", new CommandParameter[]{
                CommandParameter.newEnum("accept", new CommandEnum("accept")),
                CommandParameter.newType("username", CommandParamType.TARGET)
        });
        getCommandParameters().put("deny", new CommandParameter[]{
                CommandParameter.newEnum("deny", new CommandEnum("deny")),
                CommandParameter.newType("username", CommandParamType.TARGET)
        });
        getCommandParameters().put("cancel", new CommandParameter[]{
                CommandParameter.newEnum("cancel", new CommandEnum("cancel")),
                CommandParameter.newType("username", CommandParamType.TARGET)
        });
        getCommandParameters().put("teleport", new CommandParameter[]{
                CommandParameter.newEnum("teleport", new CommandEnum("teleport")),
                CommandParameter.newType("username", CommandParamType.TARGET)
        });
        getCommandParameters().put("list", new CommandParameter[]{
                CommandParameter.newEnum("list", new CommandEnum("list")),
                CommandParameter.newType("page", CommandParamType.INT)
        });
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {

        if (!sender.isPlayer()) {
            sender.sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, "You can only execute this command as a player.", TextFormat.RED));
            return true;
        }
        Player player = (Player)sender;
        if (args.length == 0) {
            SocialAPI.getFriendManager().openFriendsMenu(player);
            return true;
        }
        switch (args[0].toLowerCase()) {

            case "add": {
                if (args.length < 2) {
                    sender.sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, "Usage: /friend add <username>", TextFormat.RED));
                    return true;
                }
                String friendRequestTargetName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                if (friendRequestTargetName.equalsIgnoreCase(player.getLoginChainData().getUsername())) {
                    sender.sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, "You can't send friend requests to yourself!", TextFormat.RED));
                    return true;
                }

                getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
                    Optional<String> targetXuid;
                    try {
                        targetXuid = PlayerRegistry.getPlayerXuidByName(friendRequestTargetName);
                    } catch (SQLException exception) {
                        getPlugin().getLogger().error("Failed to retrieve xuid of target when adding friend via /friend");
                        getPlugin().getLogger().error(exception.toString());
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> player.sendMessage(
                                Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("An error occurred while adding \"%s\" as a friend.", friendRequestTargetName), TextFormat.RED)
                        ));
                        return;
                    }

                    if (!targetXuid.isPresent()) {
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> player.sendMessage(
                                Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("No player by the name of \"%s\" could be found.", friendRequestTargetName), TextFormat.RED))
                        );
                        return;
                    }

                    boolean success;
                    try {
                        success = SocialAPI.getFriendManager().sendFriendRequest(player.getLoginChainData().getXUID(), targetXuid.get());
                    } catch (SQLException exception) {
                        getPlugin().getLogger().error("Failed to send a friend request to a player via /friend");
                        getPlugin().getLogger().error(exception.toString());
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> player.sendMessage(
                                Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("An error occurred while adding \"%s\" as a friend.", friendRequestTargetName), TextFormat.RED)
                        ));
                        return;
                    }

                    getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
                        if (!success) {
                            player.sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, "You have already sent or have a incoming friend request from this player!", TextFormat.RED));
                            return;
                        }
                        player.sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, "Friend request sent!", TextFormat.GREEN));
                    });

                }, true);
            }
            break;

            case "remove": {
                if (args.length < 2) {
                    sender.sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, "Usage: /friend remove <username>", TextFormat.RED));
                    return true;
                }

                String removeFriendName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {

                    Optional<String> targetXuid;
                    try {
                        targetXuid = PlayerRegistry.getPlayerXuidByName(removeFriendName);
                    } catch (SQLException exception) {
                        getPlugin().getLogger().error("Failed to retrieve xuid of target when removing friend via /friend");
                        getPlugin().getLogger().error(exception.toString());
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> player.sendMessage(
                                Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("An error occurred while removing \"%s\" from your friends list.", removeFriendName), TextFormat.RED)
                        ));
                        return;
                    }

                    if (!targetXuid.isPresent()) {
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> player.sendMessage(
                                Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("No player by the name of \"%s\" could be found.", removeFriendName), TextFormat.RED)
                        ));
                        return;
                    }

                    boolean success;
                    try {
                        success = SocialAPI.getFriendManager().removeFriend(player.getLoginChainData().getXUID(), targetXuid.get());
                    } catch (SQLException exception) {
                        getPlugin().getLogger().error("Failed to remove a player as a friend via /friend");
                        getPlugin().getLogger().error(exception.toString());
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> player.sendMessage(
                                Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("An error occurred while removing \"%s\" from your friends list.", removeFriendName), TextFormat.RED)
                        ));
                        return;
                    }

                    getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
                        if (!success) {
                            player.sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, "Unable to remove friend. Are you sure they're on your friends list?", TextFormat.RED));
                        }
                    });

                }, true);
            }
            break;

            case "accept": {
                if (args.length < 2) {
                    sender.sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, "Usage: /friend accept <username>", TextFormat.RED));
                    return true;
                }

                String acceptFriendRequestName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
                    Optional<String> targetXuid;
                    try {
                        targetXuid = PlayerRegistry.getPlayerXuidByName(acceptFriendRequestName);
                    } catch (SQLException exception) {
                        getPlugin().getLogger().error("Failed to retrieve xuid of target when accepting friend request via /friend");
                        getPlugin().getLogger().error(exception.toString());
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> player.sendMessage(
                                Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("An error occurred while accepting your friend request with \"%s\"", acceptFriendRequestName), TextFormat.RED)
                        ));
                        return;
                    }

                    if (!targetXuid.isPresent()) {
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> player.sendMessage(
                                Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("No player by the name of \"%s\" could be found.", acceptFriendRequestName), TextFormat.RED)
                        ));
                        return;
                    }

                    boolean success;
                    try {
                        success = SocialAPI.getFriendManager().acceptFriendRequest(player.getLoginChainData().getXUID(), targetXuid.get());
                    } catch (SQLException exception) {
                        getPlugin().getLogger().error("Failed to add a player as a friend via /friend");
                        getPlugin().getLogger().error(exception.toString());
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> player.sendMessage(
                                Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("An error occurred while adding \"%s\" as a friend.", acceptFriendRequestName), TextFormat.RED)
                        ));
                        return;
                    }

                    getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
                        if (!success) {
                            player.sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, "Unable to add friend, are you sure they sent you a request?", TextFormat.RED));
                        }
                    });

                }, true);
            }
            break;

            case "deny": {
                if (args.length < 2) {
                    sender.sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, "Usage: /friend deny <username>", TextFormat.RED));
                    return true;
                }

                String denyFriendRequestName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
                    Optional<String> targetXuid;
                    try {
                        targetXuid = PlayerRegistry.getPlayerXuidByName(denyFriendRequestName);
                    } catch (SQLException exception) {
                        getPlugin().getLogger().error("Failed to retrieve xuid of target when denying friend request.");
                        getPlugin().getLogger().error(exception.toString());
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> player.sendMessage(
                                Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("An error occurred while denying your friend request with \"%s\"", denyFriendRequestName), TextFormat.RED)
                        ));
                        return;
                    }

                    if (!targetXuid.isPresent()) {
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> player.sendMessage(
                                Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("No player by the name of \"%s\" could be found.", denyFriendRequestName), TextFormat.RED)
                        ));
                        return;
                    }

                    boolean success;
                    try {
                        success = SocialAPI.getFriendManager().rejectFriendRequest(player.getLoginChainData().getXUID(), targetXuid.get());
                    } catch (SQLException exception) {
                        getPlugin().getLogger().error("Failed to reject friend request.");
                        getPlugin().getLogger().error(exception.toString());
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> player.sendMessage(
                                Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("An error occurred while rejecting \"%s\"'s friend request.", denyFriendRequestName), TextFormat.RED)
                        ));
                        return;
                    }

                    getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
                        if (!success) {
                            player.sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, "Unable to reject friend request, are you sure they sent you a request?", TextFormat.RED));
                            return;
                        }
                        player.sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("Rejected %s's request", denyFriendRequestName), TextFormat.GREEN));
                    });

                }, true);
            }
            break;

            case "cancel": {
                if (args.length < 2) {
                    sender.sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, "Usage: /friend cancel <username>", TextFormat.RED));
                    return true;
                }

                String cancelFriendRequestName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {

                    Optional<String> targetXuid;
                    try {
                        targetXuid = PlayerRegistry.getPlayerXuidByName(cancelFriendRequestName);
                    } catch (SQLException exception) {
                        getPlugin().getLogger().error("Failed to retrieve xuid of target when cancelling friend request via /friend");
                        getPlugin().getLogger().error(exception.toString());
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> player.sendMessage(
                                Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("An error occurred while cancelling your friend request to \"%s\".", cancelFriendRequestName), TextFormat.RED)
                        ));
                        return;
                    }

                    if (!targetXuid.isPresent()) {
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> player.sendMessage(
                                Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("No player by the name of \"%s\" could be found.", cancelFriendRequestName), TextFormat.RED)
                        ));
                        return;
                    }

                    boolean success;
                    try {
                        success = SocialAPI.getFriendManager().cancelFriendRequest(player.getLoginChainData().getXUID(), targetXuid.get());
                    } catch (SQLException exception) {
                        getPlugin().getLogger().error("Failed to cancel a friend request via /friend");
                        getPlugin().getLogger().error(exception.toString());
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> player.sendMessage(
                                Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("An error occurred while cancelling your outgoing friend request to \"%s\".", cancelFriendRequestName), TextFormat.RED)
                        ));
                        return;
                    }

                    getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
                        if (!success) {
                            player.sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, "Unable to cancel request. Are you sure you sent them one?", TextFormat.RED));
                            return;
                        }
                        player.sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("Cancelled your outgoing friend request to %s", cancelFriendRequestName), TextFormat.GREEN));
                    });

                }, true);
            }
            break;

            case "teleport": {

                String teleportToFriendName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
                    Collection<Friend> friends;
                    try {
                        friends = SocialAPI.getFriendManager().getFriends(player.getLoginChainData().getXUID());
                    } catch (SQLException exception) {
                        getPlugin().getLogger().error("An error has occurred while trying to get retrieve the player's friends via /friend");
                        getPlugin().getLogger().error(exception.toString());
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> player.sendMessage(
                                Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An internal error has occurred.", TextFormat.RED)
                        ));
                        return;
                    }

                    Optional<Friend> friend = friends.stream().filter(f -> f.getUsername().equalsIgnoreCase(teleportToFriendName)).findAny();
                    getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
                        if (!friend.isPresent()) {
                            player.sendMessage(
                                    Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("%s is not online", teleportToFriendName), TextFormat.RED)
                            );
                            return;
                        }
                        if (!FriendsManager.get().teleportToFriend(player, friend.get())) {
                            player.sendMessage(
                                    Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, "Unable to teleport you to their server!", TextFormat.RED)
                            );
                        }
                    });

                }, true);

            }
            break;

            case "list": {
                getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
                    Collection<Friend> friends;
                    try {
                        friends = SocialAPI.getFriendManager().getFriends(player.getLoginChainData().getXUID());
                    } catch (SQLException exception) {
                        getPlugin().getLogger().error("An error has occurred while trying to show the list of friends.");
                        getPlugin().getLogger().error(exception.toString());
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> player.sendMessage(
                                Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An internal error has occurred.", TextFormat.RED)
                        ));
                        return;
                    }

                    List<Friend> friendList = new ArrayList<>(friends);
                    friendList.sort(new FriendComparator(getPlugin()));

                    int listPage = 1;
                    if (args.length == 2) {
                        try {
                            listPage = Integer.parseInt(args[1]);
                            if (listPage < 1 || (listPage - 1) * ITEMS_TO_SHOW > friendList.size()) {
                                listPage = 1;
                            }
                        } catch (NumberFormatException exception) {
                            listPage = 1;
                        }
                    }

                    int minIndex = (listPage - 1) * ITEMS_TO_SHOW; // 10 friend request
                    int maxIndex = listPage * ITEMS_TO_SHOW;       // 10 friend request

                    StringBuilder listMessage = new StringBuilder(String.format("Viewing page %d of your friends", listPage));
                    for (int i = minIndex; i < Math.min(maxIndex, friendList.size()); i++) {
                        boolean isOnline = friendList.get(i).isOnline();
                        listMessage.append(String.format("\n%s%s%s - %s", TextFormat.YELLOW, friendList.get(i).getUsername(), TextFormat.GRAY, isOnline ? String.format("%sOnline", TextFormat.GREEN) : String.format("%sOffline", TextFormat.RED)));
                    }
                    getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> player.sendMessage(
                            Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, listMessage.toString())
                    ));

                }, true);
            }
            break;

            default:
                sender.sendMessage(Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("Incorrect usage:\n%s", getUsage()), TextFormat.RED));
                break;

        }

        return true;
    }
}
