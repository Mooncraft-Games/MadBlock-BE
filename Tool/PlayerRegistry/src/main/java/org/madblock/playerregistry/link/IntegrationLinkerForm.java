package org.madblock.playerregistry.link;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.response.FormResponseData;
import cn.nukkit.form.response.FormResponseModal;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowModal;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.utils.TextFormat;
import org.madblock.playerregistry.PlayerRegistry;
import org.madblock.lib.commons.text.*;
import org.madblock.playerregistry.PlayerRegistryReturns;
import org.madblock.util.DatabaseReturn;

import java.util.HashMap;

public class IntegrationLinkerForm implements Listener {

    protected HashMap<Integer, Player> playerLinkCodeForms;
    protected HashMap<Integer, Player> playerLinkCommandForms;

    public IntegrationLinkerForm() {
        this.playerLinkCodeForms = new HashMap<>();
        this.playerLinkCommandForms = new HashMap<>();
    }

    public void sendLinkCommandFormTo(Player player) {
        FormWindowModal formWindowModal = new FormWindowModal(
                "MadBlock Account Link",
                String.format(
                            "Welcome to the account linker! Do you already have %sa code from another platform? %sIf so click %s'Enter Code'. %sElse, click %s'Get Code'",
                            TextFormat.BLUE, TextFormat.RESET, TextFormat.BLUE, TextFormat.RESET, TextFormat.BLUE
                        ),
                "Get Code", "Enter Code");

        int id = player.showFormWindow(formWindowModal);
        this.playerLinkCommandForms.put(id, player);
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
        this.playerLinkCodeForms.put(id, player);
    }

    @EventHandler
    public void onFormResponse(PlayerFormRespondedEvent e) {
        int id = e.getFormID();
        Player player = e.getPlayer();

        // Player has ran /link
        if(this.playerLinkCommandForms.containsKey(id)) {
            this.playerLinkCommandForms.remove(id);

            if(e.wasClosed()) {
                IntegrationLinkerForm.linkInfo(player, "Cancelled account link.");
                return;
            }

            FormResponseModal formIn = (FormResponseModal) e.getResponse();

            // Left Button (Get Code)
            if(formIn.getClickedButtonId() == 0) {
                IntegrationLinkerForm.linkInfo(player, "Generating code...");

                PlayerRegistry.get().getServer().getScheduler().scheduleTask(PlayerRegistry.get(), () -> {
                    DatabaseReturn<String> code = IntegrationLinker.linkFromPlatform(KnownLinkSources.MINECRAFT, player.getLoginChainData().getXUID());

                    if(code.isPresent()) {
                        // It's in character form rather than graphic form. Convert it.
                        char[] encodedCode = code.get().toCharArray();
                        StringBuilder newCode = new StringBuilder();

                        for (char c : encodedCode) {
                            String convert = CodeGeneratorSymbols.getGraphicFromCode(c);

                            // If a string of "" is returned, it's not recognised. Use a "-" in that case.
                            newCode.append(convert.length() == 0 ? "-" : convert);
                        }

                        PlayerRegistry.get().getServer().getScheduler().scheduleTask(PlayerRegistry.get(), () -> {
                            FormWindowSimple codeDisplay = new FormWindowSimple("account_link_generated_code", newCode.toString());
                            IntegrationLinkerForm.linkInfoFromSync(player, "Reminder, your code is: "+newCode.toString());

                            player.showFormWindow(codeDisplay);
                        });

                    } else {
                        switch (code.getStatus()) {
                            // Host being dumb
                            case DATABASE_OFFLINE:
                                IntegrationLinkerForm.linkServerError(player, "D0");
                                break;

                            // User being dumb
                            case FAILURE:
                                switch (code.getFailureDescription()) {

                                    case PlayerRegistryReturns.INTEGRATION_ALREADY_EXISTS:
                                        IntegrationLinkerForm.linkError(player, "This account is already linked! Please open a support ticket if this is incorrect."); // Should not be possible!
                                        break;

                                    case PlayerRegistryReturns.INTEGRATION_ALL_LINK_CODES_DUPES:
                                        IntegrationLinkerForm.linkError(player, "It seems we have a lot of accounts being linked! Please try again later."); // Should not be possible!
                                        break;

                                    default:
                                        IntegrationLinkerForm.linkError(player, "Unable to generate a link code - Please try again later...");
                                        break;
                                }
                                break;

                            // Us being dumb
                            case ERROR:
                                switch (code.getFailureDescription()) {
                                    case PlayerRegistryReturns.FAILED_TO_OBTAIN_LOCK:
                                        IntegrationLinkerForm.linkServerError(player, "DT0");
                                        break;

                                    case PlayerRegistryReturns.FAILED_TO_RELEASE_LOCK:
                                        IntegrationLinkerForm.linkServerError(player, "DT1");
                                        break;

                                    case PlayerRegistryReturns.INTEGRATION_EXISTENCE_CHECK_ERRORED:
                                        IntegrationLinkerForm.linkServerError(player, "G1");
                                        break;

                                    case PlayerRegistryReturns.INTEGRATION_CODE_GENERATION_ERRORED:
                                        IntegrationLinkerForm.linkServerError(player, "P1");
                                        break;

                                    default:
                                        IntegrationLinkerForm.linkServerError(player, "E0");
                                        break;
                                }
                                break;
                        }
                    }

                }, true);

            // Right Button (Enter Code)
            } else {
                this.sendLinkingFormTo(player);
            }

            return;
        }


        // Player has selected "enter code" in the modal and filled out the next form.
        if(this.playerLinkCodeForms.containsKey(id)) {
            this.playerLinkCodeForms.remove(id);

            // If closed, abort the operation.
            if(e.wasClosed()) {
                IntegrationLinkerForm.linkInfo(player, "Cancelled account link.");
                return;
            }

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
                    PlayerRegistry.get().getLogger().info(String.format("%s has successfully paired with '%s'", player.getDisplayName(), platformLinkedWith));

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

    private static void linkInfoFromSync(Player player, String text) {
        player.sendMessage(ServerFormat.generateServerMessage("LINK", TextFormat.BLUE, text, TextFormat.GRAY));
    }


}
