package org.madblock.madbot.text;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.GuildEmoji;
import org.madblock.madbot.MadBot;

public enum Emoji {

    LINK_PIG(Snowflake.of(MadBot.BRAIN_GUILD), Snowflake.of(938515077311639622L)),
    LINK_COW(Snowflake.of(MadBot.BRAIN_GUILD), Snowflake.of(938515076971917352L)),
    LINK_SQUID(Snowflake.of(MadBot.BRAIN_GUILD), Snowflake.of(938515077034815538L)),
    LINK_VILLAGER(Snowflake.of(MadBot.BRAIN_GUILD), Snowflake.of(938515077156446258L)),
    LINK_SHEEP(Snowflake.of(MadBot.BRAIN_GUILD), Snowflake.of(938515077026443274L)),

    LINK_DIAMOND(Snowflake.of(MadBot.BRAIN_GUILD), Snowflake.of(938515077148057610L)),
    LINK_EMERALD(Snowflake.of(MadBot.BRAIN_GUILD), Snowflake.of(938515076837670963L)),
    LINK_GOLD(Snowflake.of(MadBot.BRAIN_GUILD), Snowflake.of(938515077064187935L)),
    LINK_IRON(Snowflake.of(MadBot.BRAIN_GUILD), Snowflake.of(938515077030633522L)),
    LINK_COAL(Snowflake.of(MadBot.BRAIN_GUILD), Snowflake.of(938515077022220308L)),

    LINK_DASH(Snowflake.of(MadBot.BRAIN_GUILD), Snowflake.of(938515077072572476L))

    ;


    private Snowflake guild;
    private Snowflake emoji;

    Emoji(Snowflake guild, Snowflake emoji) {
        this.guild = guild;
        this.emoji = emoji;
    }

    public Snowflake getGuildID() {
        return guild;
    }

    public Snowflake getEmojiID() {
        return emoji;
    }

    public GuildEmoji get() {
        return MadBot.get().getEmoji(this.guild, this.emoji);
    }

    public static String codeToEmojiFormatted(char code) {
        switch (code) {
            case 'p': return Emoji.LINK_PIG.get().asFormat();
            case 'c': return Emoji.LINK_COW.get().asFormat();
            case 'S': return Emoji.LINK_SQUID.get().asFormat();
            case 'v': return Emoji.LINK_VILLAGER.get().asFormat();
            case 's': return Emoji.LINK_SHEEP.get().asFormat();

            case 'd': return Emoji.LINK_DIAMOND.get().asFormat();
            case 'e': return Emoji.LINK_EMERALD.get().asFormat();
            case 'g': return Emoji.LINK_GOLD.get().asFormat();
            case 'i': return Emoji.LINK_IRON.get().asFormat();
            case 'C': return Emoji.LINK_COAL.get().asFormat();

            default: return Emoji.LINK_DASH.get().asFormat();
        }
    }
}
