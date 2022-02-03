package org.madblock.madbot;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.madblock.lib.commons.style.Check;
import org.madblock.madbot.command.BotCommand;
import org.madblock.madbot.command.BotCommandRegistry;
import org.madblock.madbot.command.account.CommandLink;
import org.madblock.madbot.link.MadBotLinker;

import java.io.File;

public class MadBot extends PluginBase {

    private static MadBot madBotInstance;

    public static final long BRAIN_GUILD = 549066921945858050L;


    protected Config config = null;
    protected GatewayDiscordClient gatewayClient = null;
    protected long applicationID = 0;

    protected BotCommandRegistry botCommandRegistry;
    protected MadBotLinker madBotLinker;

    protected long[] commandGuildIDs = new long[] {
            //937695968843943976L, // MadBlock Brain
            //879854387265171526L // Quibble Dev
            549066921945858050L  // 5 Frame Studios
    };

    @Override
    public void onEnable() {
        MadBot.madBotInstance = this;

        File configPath = new File(this.getDataFolder(), "config.json");
        this.config = new Config(configPath, Config.JSON, new ConfigSection("token", ""));

        String token = this.config.getString("token");


        if(Check.isStringEmpty(token)) {
            this.getLogger().error("No token found! Check the configs.");
            MadBot.madBotInstance = null;
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.gatewayClient = DiscordClientBuilder.create(token).build().login().block();

        if(this.gatewayClient == null) {
            this.getLogger().error("Gateway Client was null!");
            MadBot.madBotInstance = null;
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.applicationID = this.gatewayClient.getRestClient().getApplicationId().block();

        this.botCommandRegistry = new BotCommandRegistry();
        this.botCommandRegistry.setAsMain();
        this.registerCommands();

        this.madBotLinker = new MadBotLinker();

        this.gatewayClient.on(ApplicationCommandInteractionEvent.class).subscribe(MadBotEventCore::handleAppCommandExecuted);
        this.gatewayClient.on(ButtonInteractionEvent.class).subscribe(MadBotEventCore::handleAppButtonPushed);

        this.gatewayClient.on(ButtonInteractionEvent.class).subscribe(this.madBotLinker::handleAppButtonPushed);
    }

    @Override
    public void onDisable() {
        if(this.gatewayClient != null)
            this.gatewayClient.logout().block();
    }

    protected void registerCommands() {
        this.getBotCommandRegistry().assign(new CommandLink());

        if(this.commandGuildIDs.length > 0)
            this.getLogger().warning("Using guild commands for testing! Remove all guilds to make them global.");

        for(String id: this.getBotCommandRegistry().getCommands()) {
            try {
                BotCommand command = this.getBotCommandRegistry().get(id).get();
                ApplicationCommandRequest r = ApplicationCommandRequest.builder()
                        .name(command.getName())
                        .description(command.getDescription())
                        .addAllOptions(command.getParameters())
                        .build();

                this.publishCommand(r);

            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }

    private void publishCommand(ApplicationCommandRequest request) {
        // Use guild commands
        if(this.commandGuildIDs.length > 0) {
            for(long id: this.commandGuildIDs)
                this.getGatewayClient()
                        .getRestClient()
                        .getApplicationService()
                        .createGuildApplicationCommand(this.applicationID, id, request)
                        .block();

        // Use globals
        } else {
            this.getGatewayClient().getRestClient()
                    .getApplicationService()
                    .createGlobalApplicationCommand(this.applicationID, request)
                    .block();
        }
    }


    @Override
    public Config getConfig() {
        return this.config;
    }

    public GatewayDiscordClient getGatewayClient() {
        return this.gatewayClient;
    }

    public BotCommandRegistry getBotCommandRegistry() {
        return this.botCommandRegistry;
    }

    public MadBotLinker getMadBotLinker() {
        return madBotLinker;
    }

    public GuildEmoji getEmoji(Snowflake guild, Snowflake emoji) {
        return MadBot.get().getGatewayClient().getGuildEmojiById(guild, emoji).block();
    }


    public static MadBot get() {
        return MadBot.madBotInstance;
    }
}
