package org.madblock.playerregistry.link;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.response.FormResponseData;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.utils.TextFormat;
import org.madblock.playerregistry.PlayerRegistry;
import org.madblock.lib.commons.text.*;
import org.madblock.playerregistry.PlayerRegistryReturns;
import org.madblock.util.DatabaseReturn;

import java.util.HashMap;

public class IntegrationLinkerForm implements Listener {

    protected HashMap<Integer, Player> playerForms;

    public IntegrationLinkerForm() {
        this.playerForms = new HashMap<>();
    }

    public void sendLinkingFormTo(Player player) {
        FormWindowCustom formWindowCustom = new FormWindowCustom("account_linking");
        formWindowCustom.addElement(new ElementDropdown("1", CodeGeneratorSymbols.getGraphics()));
        formWindowCustom.addElement(new ElementDropdown("2", CodeGeneratorSymbols.getGraphics()));
        formWindowCustom.addElement(new ElementDropdown("3", CodeGeneratorSymbols.getGraphics()));
        formWindowCustom.addElement(new ElementDropdown("4", CodeGeneratorSymbols.getGraphics()));
        formWindowCustom.addElement(new ElementLabel("-"));
        formWindowCustom.addElement(new ElementDropdown("5", CodeGeneratorSymbols.getGraphics()));
        formWindowCustom.addElement(new ElementDropdown("6", CodeGeneratorSymbols.getGraphics()));
        formWindowCustom.addElement(new ElementDropdown("7", CodeGeneratorSymbols.getGraphics()));
        formWindowCustom.addElement(new ElementDropdown("8", CodeGeneratorSymbols.getGraphics()));

        int id = player.showFormWindow(formWindowCustom);
        this.playerForms.put(id, player);
    }

    @EventHandler
    public void onFormResponse(PlayerFormRespondedEvent e) {
        int id = e.getFormID();

        if(playerForms.containsKey(id)) {
            playerForms.remove(id);
            Player player = e.getPlayer();

            // If closed, abort the operation.
            if(!e.wasClosed()) {
                FormResponseCustom formIn = (FormResponseCustom) e.getResponse();
                StringBuilder assemble = new StringBuilder();

                // Go through each element, chaining responses together.
                for(int i = 0; i < formIn.getResponses().size(); i++) {
                    Object element = formIn.getResponses().get(i);

                    if(element instanceof FormResponseData) {
                        FormResponseData dropdown = (FormResponseData) element;
                        String response = dropdown.getElementContent();
                        Character symbol = CodeGeneratorSymbols.getCodeFromGraphic(response);
                        assemble.append(symbol);

                    } else {
                        assemble.append("-");
                    }
                }

                String code = assemble.toString();
                PlayerRegistry.get().getLogger().info(String.format("%s has attempted to integrate an account using the code '%s'", player.getDisplayName(), code));
                IntegrationLinkerForm.linkInfo(player, "Attempting to link accounts...");


                PlayerRegistry.get().getServer().getScheduler().scheduleTask(PlayerRegistry.get(), () -> {
                    DatabaseReturn<String> linkResult = IntegrationLinker.redeemLink(code, KnownLinkSources.MINECRAFT, player.getLoginChainData().getXUID());

                    // If successful, it's returned a result.
                    if(linkResult.getStatus().isSuccess()) {
                        String platformLinkedWith = linkResult.get();
                        IntegrationLinkerForm.linkInfo(player, String.format("Successfully paired your %s account!", platformLinkedWith));
                        PlayerRegistry.get().getLogger().info(String.format("%s has successfully paired with ''", player.getDisplayName(), player));

                    } else {

                        // There's an error. Split it into the 3 main characters and go through specific codes.
                        switch (linkResult.getStatus()) {

                            // Host being dumb
                            case DATABASE_OFFLINE:
                                IntegrationLinkerForm.linkServerError(player, "D0");
                                break;

                            // User being dumb
                            case FAILURE:
                                switch (linkResult.getFailureDescription()) {
                                    case PlayerRegistryReturns.LINK_SAME_PLATFORM:
                                        IntegrationLinkerForm.linkError(player, "This code was generated here! Enter the code on another platform (Discord?)");
                                        break;

                                    case PlayerRegistryReturns.LINK_NONE_FOUND:
                                        IntegrationLinkerForm.linkError(player, "This code is not valid (Has it expired? Is it correct?)");
                                        break;

                                    case PlayerRegistryReturns.LINK_INCOMPATIBLE_PLATFORM:
                                        IntegrationLinkerForm.linkError(player, "You cannot use that code on this platform.");
                                        break;

                                    default:
                                        IntegrationLinkerForm.linkError(player, "Unable to link accounts.");
                                        break;
                                }
                                break;

                            // Us being dumb
                            case ERROR:
                                switch (linkResult.getFailureDescription()) {
                                    case PlayerRegistryReturns.FAILED_TO_OBTAIN_LOCK:
                                        IntegrationLinkerForm.linkServerError(player, "DT0");
                                        break;

                                    case PlayerRegistryReturns.FAILED_TO_RELEASE_LOCK:
                                        IntegrationLinkerForm.linkServerError(player, "DT1");
                                        break;

                                    case PlayerRegistryReturns.LINK_FETCH_DETAILS_FROM_CODE_ERRORED:
                                        IntegrationLinkerForm.linkServerError(player, "G0");
                                        break;

                                    case PlayerRegistryReturns.LINK_CREATE_PAIRING_ERRORED:
                                        IntegrationLinkerForm.linkServerError(player, "P0");
                                        break;

                                    default:
                                        IntegrationLinkerForm.linkServerError(player, "E0");
                                        break;
                                }
                                break;


                        }
                    }

                }, true);

            } else {
                IntegrationLinkerForm.linkInfo(player, "Successfully cancelled account link.");
            }
        }
    }

    private static void linkError(Player player, String text) {
        PlayerRegistry.get().getServer().getScheduler().scheduleTask(PlayerRegistry.get(), () -> {
            player.sendMessage(ServerFormat.generateServerMessage("LINK", TextFormat.DARK_RED, text, TextFormat.RED));
        });
    }

    private static void linkServerError(Player player, String code) {
        IntegrationLinkerForm.linkError(player, String.format("Internal Server Error (%s) - Please try again later...", code));
    }

    private static void linkInfo(Player player, String text) {
        PlayerRegistry.get().getServer().getScheduler().scheduleTask(PlayerRegistry.get(), () -> {
            player.sendMessage(ServerFormat.generateServerMessage("LINK", TextFormat.BLUE, text, TextFormat.GRAY));
        });
    }


}
