package org.madblock.punishments.listeners;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.utils.TextFormat;
import org.madblock.playerregistry.PlayerRegistry;
import org.madblock.punishments.PunishmentsPlugin;
import org.madblock.punishments.api.PunishmentManager;
import org.madblock.punishments.builders.PunishmentFormDataBuilder;
import org.madblock.punishments.forms.PunishmentFormManager;
import org.madblock.punishments.forms.additionalfunctions.AdditionalReasonFunction;
import org.madblock.punishments.forms.additionalfunctions.GamemodeFunction;
import org.madblock.punishments.list.PunishmentCategory;
import org.madblock.punishments.list.PunishmentOffense;
import org.madblock.punishments.list.PunishmentOffenseListItem;
import org.madblock.punishments.list.SubPunishmentOffense;
import org.madblock.punishments.utils.Utility;
import org.madblock.ranks.api.RankManager;
import org.madblock.ranks.api.RankProfile;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class PunishmentFormListener implements Listener {

    private final PunishmentsPlugin plugin;

    public PunishmentFormListener (PunishmentsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFormInteraction (PlayerFormRespondedEvent event) {
        Optional<PunishmentFormManager.PunishmentFormData> data = PunishmentManager.getInstance().getFormManager().getPunishmentFormData(event.getPlayer());

        if (data.isPresent() && data.get().getFormId() == event.getFormID()) {

            if (event.wasClosed()) {
                PunishmentManager.getInstance().getFormManager().deletePunishmentFormData(event.getPlayer());
                return;
            }

            PunishmentCategory category = PunishmentManager.getInstance().getCategory(data.get().getOffenseType()).get();

            if (data.get().getParameter("function").isPresent()) {

                FormResponseCustom response = (FormResponseCustom)event.getResponse();
                PunishmentOffense.AdditionalFunction functionType = PunishmentOffense.AdditionalFunction.valueOf(data.get().getParameter("function").get());

                String additionalPunishmentData = this.extractAdditionalFunctionData(response, functionType);

                // Addition function window. Parse the response and finish the punishment.

                this.executePunishment(data.get().getTarget(), category, data.get().getParameter("offense_name").get() + additionalPunishmentData, event.getPlayer());

            } else {

                FormResponseSimple response = (FormResponseSimple)event.getResponse();

                int index = response.getClickedButtonId();
                if (data.get().getParameter("parent_offense_index").isPresent()) {

                    // Sub reason.
                    List<SubPunishmentOffense> offenses = category.getOffenses().get(Integer.parseInt(data.get().getParameter("parent_offense_index").get())).getSubReasons();
                    SubPunishmentOffense offense = offenses.get(index);

                    // Is there a function for this offense?

                    if (offense.getAdditionalFunction().isPresent()) {

                        if (offense.getAdditionalFunction().get().equals(PunishmentOffense.AdditionalFunction.CHANGE_AND_APPEAL)) {

                            // We can punish with offense.getName() just add [Change and Appeal]
                            this.executePunishment(data.get().getTarget(), category, offense.getName() + " [Change and Appeal]", event.getPlayer());


                        } else {
                            PunishmentOffense.AdditionalFunction functionType = offense.getAdditionalFunction().get();

                            FormWindowCustom formWindow = this.createAdditionalFunctionWindow(offense);

                            int formId = event.getPlayer().showFormWindow(formWindow);

                            PunishmentManager.getInstance().getFormManager().setPunishmentFormData(
                                    event.getPlayer(),
                                    new PunishmentFormDataBuilder()
                                            .setParameter("function", functionType.toString())
                                            .setOffenseType(data.get().getOffenseType())
                                            .setTarget(data.get().getTarget())
                                            .setParameter("offense_name", offense.getName())
                                            .setFormId(formId)
                                            .build()
                            );
                        }

                    } else {

                        // We can punish with offense.getName()
                        this.executePunishment(data.get().getTarget(), category, offense.getName(), event.getPlayer());

                    }


                } else {

                    // This is not a sub reason option.

                    PunishmentOffense offense = category.getOffenses().get(index);
                    if (offense.hasSubReasons()) {

                        // Display sub reasons.

                        List<SubPunishmentOffense> offenses = offense.getSubReasons();

                        FormWindowSimple formWindow = new FormWindowSimple("Punishment", "");

                        for (SubPunishmentOffense subOffense : offenses) {
                            formWindow.addButton(new ElementButton(subOffense.getName()));
                        }

                        int formId = event.getPlayer().showFormWindow(formWindow);
                        PunishmentManager.getInstance().getFormManager().setPunishmentFormData(
                                event.getPlayer(),
                                new PunishmentFormDataBuilder()
                                        .setFormId(formId)
                                        .setOffenseType(data.get().getOffenseType())
                                        .setParameter("parent_offense_index", Integer.toString(index))
                                        .setTarget(data.get().getTarget())
                                        .build()
                        );

                    } else {

                        // Is there a function for this offense?

                        if (offense.getAdditionalFunction().isPresent()) {

                            if (offense.getAdditionalFunction().get().equals(PunishmentOffense.AdditionalFunction.CHANGE_AND_APPEAL)) {

                                // We can just punish with offense.getName() just add [Change and Appeal]
                                this.executePunishment(data.get().getTarget(), category, offense.getName() + " [Change and Appeal]", event.getPlayer());

                            } else {

                                PunishmentOffense.AdditionalFunction functionType = offense.getAdditionalFunction().get();

                                FormWindowCustom formWindow = this.createAdditionalFunctionWindow(offense);

                                int formId = event.getPlayer().showFormWindow(formWindow);

                                PunishmentManager.getInstance().getFormManager().setPunishmentFormData(
                                        event.getPlayer(),
                                        new PunishmentFormDataBuilder()
                                                .setParameter("function", functionType.toString())
                                                .setOffenseType(data.get().getOffenseType())
                                                .setTarget(data.get().getTarget())
                                                .setParameter("offense_name", offense.getName())
                                                .setFormId(formId)
                                                .build()
                                );

                            }


                        } else {

                            // We can punish with offense.getName()
                            this.executePunishment(data.get().getTarget(), category, offense.getName(), event.getPlayer());

                        }

                    }

                }


            }


        }
    }

    private void executePunishment (String target, PunishmentCategory category, String reason, Player staffMember) {
        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
            Optional<String> xuid;
            try {
                xuid = PlayerRegistry.getPlayerXuidByName(target);
            } catch (SQLException exception) {
                exception.printStackTrace();
                plugin.getServer().getScheduler().scheduleTask(plugin, () -> staffMember.sendMessage(
                    Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Internal Server Error", TextFormat.RED)
                ));
                return;
            }

            if (!xuid.isPresent()) {
                plugin.getServer().getScheduler().scheduleTask(plugin, () -> staffMember.sendMessage(
                        Utility.generateServerMessage("PUNISHMENTS", TextFormat.DARK_RED, "The player is not registered in our database.", TextFormat.RED)
                ));
                return;
            }

            Optional<RankProfile> rankProfile = RankManager.getInstance().getRankProfile(staffMember);

            // A bit disgusting.
            PunishmentCategory actualCategory = category;
            if (category.hasLevels() && category.getSeverity() != 1 && !rankProfile.get().hasPermission("punishments.issue.full")) {
                actualCategory = PunishmentManager.getInstance().getCategory(category.getCategory() + "1").get();
            }

            try {
                PunishmentManager.getInstance().punish(xuid.get(), actualCategory, reason, staffMember);
            } catch (SQLException exception) {
                exception.printStackTrace();
                plugin.getServer().getScheduler().scheduleTask(plugin, () -> staffMember.sendMessage(
                        Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Internal Server Error", TextFormat.RED)
                ));
                return;
            }

            plugin.getServer().getScheduler().scheduleTask(plugin, () -> staffMember.sendMessage(
                    Utility.generateServerMessage("PUNISHMENTS", TextFormat.DARK_RED, "Punished " + target + " for " + TextFormat.YELLOW + category.getCode(), TextFormat.GREEN)
            ));

        }, true);
    }

    private FormWindowCustom createAdditionalFunctionWindow (PunishmentOffenseListItem offense) {

        PunishmentOffense.AdditionalFunction functionType = offense.getAdditionalFunction().get();

        FormWindowCustom formWindow = new FormWindowCustom(offense.getName());

        switch (functionType) {
            case REASON_PROMPT:
                AdditionalReasonFunction.addElements(formWindow);
                break;
            case GAME_PROMPT:
                GamemodeFunction.addElements(formWindow);
                break;
        }

        return formWindow;
    }

    private String extractAdditionalFunctionData (FormResponseCustom formResponse, PunishmentOffense.AdditionalFunction functionType) {

        String additionalPunishmentData;
        switch (functionType) {
            case REASON_PROMPT:
                additionalPunishmentData = AdditionalReasonFunction.parseResponse(formResponse);
                break;
            case GAME_PROMPT:
                additionalPunishmentData = GamemodeFunction.parseResponse(formResponse);
                break;
            default:
                additionalPunishmentData = "";
                break;
        }

        return additionalPunishmentData;

    }


}
