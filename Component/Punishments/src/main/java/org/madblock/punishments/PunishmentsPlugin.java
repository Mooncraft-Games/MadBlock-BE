package org.madblock.punishments;

import cn.nukkit.plugin.PluginBase;
import org.madblock.punishments.api.PunishmentManager;
import org.madblock.punishments.commands.IssueCommand;
import org.madblock.punishments.commands.IssueDetailCommand;
import org.madblock.punishments.commands.IssuesCommand;
import org.madblock.punishments.commands.RemoveIssueCommand;
import org.madblock.punishments.listeners.PlayerListener;
import org.madblock.punishments.listeners.PunishmentFormListener;

public class PunishmentsPlugin extends PluginBase {

    @Override
    public void onEnable () {

        PunishmentManager.setInstance(new PunishmentManager(this));

        this.getServer().getCommandMap().register("issue", new IssueCommand(this));
        this.getServer().getCommandMap().register("issues", new IssuesCommand(this));
        this.getServer().getCommandMap().register("isremove", new RemoveIssueCommand(this));
        this.getServer().getCommandMap().register("isdetail", new IssueDetailCommand(this));

        this.getServer().getPluginManager().registerEvents(new PunishmentFormListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

    }

}
