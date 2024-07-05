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

import com.jagrosh.giveawaybot.GiveawayBot;
import com.jagrosh.giveawaybot.GiveawayException;
import com.jagrosh.giveawaybot.data.Giveaway;
import com.jagrosh.giveawaybot.entities.LocalizedMessage;
import com.jagrosh.giveawaybot.util.FormatUtil;
import com.jagrosh.interactions.command.ApplicationCommand;
import com.jagrosh.interactions.entities.*;
import com.jagrosh.interactions.receive.Interaction;
import com.jagrosh.interactions.responses.InteractionResponse;
import com.jagrosh.interactions.responses.MessageCallback;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class ListCmd extends GBCommand
{
    private final static int MAX_LENGTH = 6000;
    
    public ListCmd(GiveawayBot bot)
    {
        super(bot);
        this.app = new ApplicationCommand.Builder()
                .setType(ApplicationCommand.Type.CHAT_INPUT)
                .setName(bot.getCommandPrefix() + "list")
                .setDescription("show active giveaways")
                .setDmPermission(false)
                .setDefaultPermissions(Permission.MANAGE_GUILD)
                .build();
    }
    
    @Override
    protected InteractionResponse gbExecute(Interaction interaction) throws GiveawayException
    {
        List<Giveaway> list = bot.getDatabase().getGiveawaysByGuild(interaction.getGuildId());
        if(list.isEmpty())
            return respondError(LocalizedMessage.WARNING_NO_GIVEAWAYS.getLocalizedMessage(interaction.getEffectiveLocale()));
        
        WebLocale loc = interaction.getEffectiveLocale();
        String title = LocalizedMessage.GIVEAWAY_LIST.getLocalizedMessage(interaction.getEffectiveLocale());
        int total = title.length();
        
        Embed.Builder eb = new Embed.Builder();
        eb.setColor(bot.getDatabase().getSettings(interaction.getGuildId()).getColor());
        eb.setTitle(title, null);
        
        for(Giveaway giv: list)
        {
            String key = giv.getPrize().isEmpty() ? "Giveaway" : giv.getPrize();
            String val = renderGiveawayString(giv, loc);
            if(total + key.length() + val.length() >= MAX_LENGTH - 4)
            {
                eb.setFooter("...", null);
                break;
            }
            eb.addField(key, val, true);
            total += key.length() + val.length();
        }
        return new MessageCallback(new SentMessage.Builder()
                //.setContent(sb.toString())
                .setAllowedMentions(new AllowedMentions(true))
                .addEmbed(eb.build())
                .build());
    }
    
    private String renderGiveawayString(Giveaway giv, WebLocale loc)
    {
        return new StringBuilder()
                .append("[`").append(giv.getMessageId()).append("`](").append(giv.getJumpLink()).append(") | <#").append(giv.getChannelId()).append("> | ")
                //.append(giv.getPrize().isEmpty() ? "" : "**" + giv.getPrize() + "** | ")
                .append(LocalizedMessage.GIVEAWAY_WINNERS.getLocalizedMessage(loc)).append(": **").append(giv.getWinners()).append("**  | ")
                .append(LocalizedMessage.GIVEAWAY_HOSTED.getLocalizedMessage(loc)).append(": <@").append(giv.getUserId()).append("> | ")
                .append(LocalizedMessage.GIVEAWAY_ENDS.getLocalizedMessage(loc)).append(": ").append(FormatUtil.secondsToTime(Instant.now().until(giv.getEndInstant(), ChronoUnit.SECONDS)))
                .toString();
    }
}
