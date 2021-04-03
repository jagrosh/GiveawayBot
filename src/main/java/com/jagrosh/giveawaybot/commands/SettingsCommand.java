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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class SettingsCommand extends Command
{
    private final Bot bot;
    
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
        EmbedBuilder eb = new EmbedBuilder();
        GuildSettings settings = bot.getDatabase().settings.getSettings(event.getGuild().getIdLong());
        PremiumLevel level = bot.getDatabase().premium.getPremiumLevel(event.getGuild());

        String[] args = event.getArgs().split("\\s+", 2);

        switch (args[0])
        {
            case "emoji":
            {
                if (args.length == 2)
                {
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

                    event.getMessage().addReaction(args[1])
                            .map((success) -> {
                                bot.getDatabase().settings.updateEmoji(event.getGuild(), args[1]);
                                event.reply("new emoji set");
                                return null;
                            })
                            .onErrorMap((error) -> {
                                event.replyError("The provided emoji is invalid or not accessible for me. Please use a different one.");
                                return null;
                            }).queue();
                }
            }
            case "":
            default:
                defaultBlock(event, eb, settings, level);
        }


    }

    private void emojiBlock()
    {
        // TODO Tidy switch up and move stuff here
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
