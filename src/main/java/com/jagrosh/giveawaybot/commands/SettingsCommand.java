/*
 * Copyright 2019 John Grosh (john.a.grosh@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.giveawaybot.commands;

import com.jagrosh.giveawaybot.Bot;
import com.jagrosh.giveawaybot.Constants;
import com.jagrosh.giveawaybot.database.managers.GuildSettingsManager.GuildSettings;
import com.jagrosh.giveawaybot.entities.PremiumLevel;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.vdurmont.emoji.EmojiManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Arrays;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class SettingsCommand extends Command
{
    private final Bot bot;
    private final String EMOTE_REGEX = "<a?:([a-zA-Z0-9_]+):([0-9]+)>";
    private final String[] CLEAR_ALIAS = new String[]{"reset", "clear"};
    
    public SettingsCommand(Bot bot)
    {
        this.bot = bot;
        this.name = "settings";
        this.aliases = new String[]{"settings"};
        this.help = "shows server settings";
        this.hidden = true;
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }
    
    @Override
    protected void execute(CommandEvent event)
    {
        GuildSettings settings = bot.getDatabase().settings.getSettings(event.getGuild().getIdLong());
        PremiumLevel level = bot.getDatabase().premium.getPremiumLevel(event.getGuild());

        String[] args = event.getArgs().split("\\s+", 2);

        switch (args[0])
        {
            case "emoji":
                emojiBlock(event, level, args);
                break;
            case "clear":
            case "reset":
                resetBlock(event.getGuild());
                break;
            case "":
            default:
                defaultBlock(event, new EmbedBuilder(), settings, level);
        }


    }

    private void emojiBlock(CommandEvent event, PremiumLevel level, String[] args)
    {
        if (args.length != 2)
        {
            defaultBlock(event, new EmbedBuilder(), bot.getDatabase().settings.getSettings(event.getGuild().getIdLong()), level);
            return;
        }
        if (Arrays.stream(CLEAR_ALIAS).anyMatch(it -> args[1].equalsIgnoreCase(it)))
        {
            resetBlock(event.getGuild());
            return;
        }

        if (!level.canSetEmoji())
        {
            event.replyError("Sorry you must have a premium level for setting a custom emoji.");
            return;
        }

        if (args[1].length() > 60)
        {
            event.replyError("Emoji too long");
            return;
        }

        String extracted;

        if (args[1].matches(EMOTE_REGEX))
        {
            extracted = args[1].substring(2, args[1].length() - 1);
        }
        else if (EmojiManager.isEmoji(args[1]))
        {
            extracted = args[1];
        }
        else
        {
            event.replyError("The provided emoji is invalid.");
            return;
        }

        final String finalExtracted = extracted; // because of lambda expression
        event.getMessage().addReaction(finalExtracted)
                .map((success) -> {
                    bot.getDatabase().settings.updateEmoji(event.getGuild(), finalExtracted);
                    event.replySuccess("Successfully set " + args[1] + " as the new servers reaction emoji.");
                    event.getMessage().removeReaction(finalExtracted, event.getSelfUser()).queue();
                    return null;
                })
                .onErrorMap((error) -> {
                    event.replyError("The provided emoji is not accessible for me. Please use a different one.");
                    return null;
                }).queue();
    }

    private void resetBlock(Guild guild) {
        if (bot.getDatabase().settings.getSettings(guild.getIdLong()).getEmojiDisplay().equals(Constants.TADA))
            return; // might be redundant check, will remove if desired
        bot.getDatabase().settings.updateEmoji(guild, null);
    }

    private void defaultBlock(CommandEvent event, EmbedBuilder eb, GuildSettings settings, PremiumLevel level)
    {
        eb.setColor(settings.color);
        eb.appendDescription("Premium Level: **" + level.name + "**\n");
        eb.appendDescription("\nReaction: " + settings.getEmojiDisplay());
        eb.setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl());
        event.reply(new MessageBuilder()
                .setContent(Constants.YAY + " **" + event.getSelfUser().getName() + "** settings: ")
                .setEmbed(eb.build())
                .build());
    }
}
