package org.madblock.punishments.commands;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.utils.TextFormat;
import org.madblock.punishments.PunishmentsPlugin;
import org.madblock.punishments.api.PunishmentManager;
import org.madblock.punishments.builders.PunishmentFormDataBuilder;
import org.madblock.punishments.list.PunishmentCategory;
import org.madblock.punishments.list.PunishmentOffense;
import org.madblock.punishments.utils.Utility;
import org.madblock.ranks.api.RankManager;
import org.madblock.ranks.api.RankProfile;

import java.util.List;
import java.util.Optional;

public class IssueCommand extends PluginCommand<PunishmentsPlugin> {

    public IssueCommand(PunishmentsPlugin plugin) {
        super("issue", plugin);
        this.setDescription("Issue a punishment to another player.");
        this.setUsage("/issue <player name> <code>");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {

        if (!sender.isPlayer()) {
            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Sorry, this command can only be executed as a player.", TextFormat.RED));
            return true;
        }

        Optional<RankProfile> rankProfile = RankManager.getInstance().getRankProfile((Player)sender);

        if (!rankProfile.isPresent() || !(rankProfile.get().hasPermission("punishments.issue.full") || rankProfile.get().hasPermission("punishments.issue.limited"))) {
            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You do not have permission to execute this command.", TextFormat.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Utility.generateServerMessage("PUNISHMENTS", TextFormat.DARK_RED, this.getUsage(), TextFormat.RED));
            return true;
        }

        String targetName = args[0];
        String code = args[args.length - 1].toLowerCase();
        for (int i = 1; i < args.length - 1; i++) {
            targetName += " " + args[i];
        }

        Optional<PunishmentCategory> category = PunishmentManager.getInstance().getCategory(code);
        if (!category.isPresent()) {
            sender.sendMessage(Utility.generateServerMessage("PUNISHMENTS", TextFormat.DARK_RED, "That category does not exist.", TextFormat.RED));
            return true;
        }

        if (category.get().getPermissionRequired().isPresent() && !rankProfile.get().hasPermission(category.get().getPermissionRequired().get())) {
            sender.sendMessage(Utility.generateServerMessage("PUNISHMENTS", TextFormat.DARK_RED, "You do not have permission to punish for this.", TextFormat.RED));
            return true;
        }

        List<PunishmentOffense> offenses = category.get().getOffenses();

        FormWindowSimple formWindow = new FormWindowSimple("Punishment - " + targetName, "");
        for (PunishmentOffense offense : offenses) {
            formWindow.addButton(new ElementButton(offense.getName()));
        }

        int formId = ((Player)sender).showFormWindow(formWindow);
        PunishmentManager.getInstance().getFormManager().setPunishmentFormData(
                (Player)sender,
                new PunishmentFormDataBuilder()
                    .setFormId(formId)
                    .setOffenseType(code)
                    .setTarget(targetName)
                    .build()
        );

        return true;
    }
}
