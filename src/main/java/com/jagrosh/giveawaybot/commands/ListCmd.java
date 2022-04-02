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
import com.jagrosh.giveawaybot.GiveawayManager;
import com.jagrosh.giveawaybot.data.Database;
import com.jagrosh.giveawaybot.data.Giveaway;
import com.jagrosh.giveawaybot.entities.LocalizedMessage;
import com.jagrosh.giveawaybot.util.FormatUtil;
import com.jagrosh.interactions.command.ApplicationCommand;
import com.jagrosh.interactions.entities.SentMessage;
import com.jagrosh.interactions.receive.Interaction;
import com.jagrosh.interactions.responses.InteractionResponse;
import com.jagrosh.interactions.responses.MessageCallback;
import java.awt.Color;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class ListCmd extends GBCommand
{
    public ListCmd(GiveawayBot bot)
    {
        super(bot);
        this.app = new ApplicationCommand.Builder()
                .setType(ApplicationCommand.Type.CHAT_INPUT)
                .setName(bot.getCommandPrefix() + "list")
                .setDescription("show active giveaways")
                .build();
    }
    
    @Override
    protected InteractionResponse gbExecute(Interaction interaction) throws GiveawayException
    {
        bot.getGiveawayManager().checkPermission(interaction.getMember(), interaction.getGuildId());
        
        List<Giveaway> list = bot.getDatabase().getGiveawaysByGuild(interaction.getGuildId());
        if(list.isEmpty())
            return respondError(LocalizedMessage.WARNING_NO_GIVEAWAYS.getLocalizedMessage(interaction.getEffectiveLocale()));
        //Color c = bot.getDatabase().getSettings(interaction.getGuildId()).getColor();
        StringBuilder sb = new StringBuilder("**Active Giveaways**\n");
        list.forEach(giv -> 
        {
            sb.append("\n[`").append(giv.getMessageId()).append("`](").append(giv.getJumpLink()).append(") | <#").append(giv.getChannelId()).append("> | **").append(giv.getWinners())
                .append("** ").append(FormatUtil.pluralise(giv.getWinners(), "winner", "winners")).append(" | ")
                .append(giv.getPrize() == null || giv.getPrize().isEmpty() ? "No prize specified" : "Prize: **" + giv.getPrize() + "**").append(" | ");
            //switch(giv.status)
            //{
            //    case RUN:
                    sb.append("Ends in ").append(FormatUtil.secondsToTime(Instant.now().until(giv.getEndInstant(), ChronoUnit.SECONDS)));
            //        break;
            //    case ENDING:
            //        sb.append("Ending **soon**");
            //        break;
            //    case ENDNOW:
            //        sb.append("Ending **now**");
            //        break;
            //    case SCHEDULED:
            //        sb.append("Hasn't started");
            //        break;
            //    default:
            //        sb.append("Unknown status");
            //}
            
        });
        return new MessageCallback(new SentMessage.Builder()
                .setContent(sb.toString())
                //.addEmbed(new Embed.Builder().setTitle("Active Giveaways", null).setColor(c).setDescription(sb.toString()).build())
                .build());
    }
}
