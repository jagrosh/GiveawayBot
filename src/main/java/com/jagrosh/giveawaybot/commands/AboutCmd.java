/*
 * Copyright 2022 John Grosh (john.a.grosh@gmail.com).
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

import com.jagrosh.giveawaybot.Constants;
import com.jagrosh.giveawaybot.GiveawayBot;
import com.jagrosh.giveawaybot.entities.LocalizedMessage;
import com.jagrosh.interactions.command.ApplicationCommand;
import com.jagrosh.interactions.entities.Embed;
import com.jagrosh.interactions.entities.SentMessage;
import com.jagrosh.interactions.entities.WebLocale;
import com.jagrosh.interactions.receive.Interaction;
import com.jagrosh.interactions.responses.InteractionResponse;
import com.jagrosh.interactions.responses.MessageCallback;
import java.awt.Color;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AboutCmd extends GBCommand
{
    private final static String YAY = "<:yay:585696613507399692>";
    private final static String STATS = "\uD83D\uDCCA "; // 📊
    private final static String LINKS = "\uD83C\uDF10 "; // 🌐
    
    public AboutCmd(GiveawayBot bot)
    {
        super(bot);
        this.app = new ApplicationCommand.Builder()
                .setType(ApplicationCommand.Type.CHAT_INPUT)
                .setName(bot.getCommandPrefix() + "about")
                .setDescription("show information about the bot")
                .setDmPermission(false)
                .build();
    }
    
    @Override
    public InteractionResponse gbExecute(Interaction interaction)
    {
        WebLocale wl = interaction.getEffectiveLocale();
        return new MessageCallback(new SentMessage.Builder()
                .setContent(YAY + " " + LocalizedMessage.INFO_ABOUT.getLocalizedMessage(wl, "**GiveawayBot**") + " " + YAY)
                .addEmbed(new Embed.Builder()
                        .setTitle(LocalizedMessage.INFO_ABOUT_BRIEF.getLocalizedMessage(wl), null)
                        .setColor(new Color(0x5865F2))
                        .setDescription(LocalizedMessage.INFO_ABOUT_LONG.getLocalizedMessage(wl))
                        .addField(STATS + LocalizedMessage.INFO_ABOUT_STATS.getLocalizedMessage(wl), 
                                      LocalizedMessage.INFO_ABOUT_STATS_GIVEAWAYS.getLocalizedMessage(wl, bot.getDatabase().countAllGiveaways()) 
                                 + "\n" + LocalizedMessage.INFO_ABOUT_STATS_SERVERS.getLocalizedMessage(wl, bot.getServerCount()), true)
                        .addField(LINKS + LocalizedMessage.INFO_ABOUT_LINKS.getLocalizedMessage(wl), 
                                "[" + LocalizedMessage.INFO_ABOUT_LINKS_WEBSITE.getLocalizedMessage(wl) + "](" + Constants.WEBSITE 
                                + ")\n[" + LocalizedMessage.INFO_ABOUT_LINKS_INVITE.getLocalizedMessage(wl) + "](" + Constants.INVITE 
                                + ")\n[" + LocalizedMessage.INFO_ABOUT_LINKS_SUPPORT.getLocalizedMessage(wl) + "](" + Constants.SUPPORT + ")", true)
                .build()).setEphemeral(true).build());
    }
}
