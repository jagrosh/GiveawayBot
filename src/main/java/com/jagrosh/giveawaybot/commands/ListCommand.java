/*
 * Copyright 2018 John Grosh (john.a.grosh@gmail.com).
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
import com.jagrosh.giveawaybot.entities.Giveaway;
import com.jagrosh.giveawaybot.util.FormatUtil;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import net.dv8tion.jda.core.Permission;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class ListCommand extends Command
{
    private final Bot bot;
    
    public ListCommand(Bot bot) 
    {
        this.bot = bot;
        name = "list";
        help = "lists active giveaways on the server";
        category = Constants.GIVEAWAY;
        guildOnly = true;
        botPermissions = new Permission[]{Permission.MESSAGE_HISTORY};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        List<Giveaway> list = bot.getDatabase().giveaways.getGiveaways(event.getGuild());
        if(list.isEmpty())
        {
            event.replyWarning("There are no giveaways running on the server!");
            return;
        }
        StringBuilder sb = new StringBuilder().append("__Active Giveaways on **").append(event.getGuild().getName()).append("**__:\n");
        list.forEach(giv -> sb.append("\n`").append(giv.messageId).append("` | <#").append(giv.channelId).append("> | **").append(giv.winners)
                .append("** winner(s) | ").append(giv.prize==null||giv.prize.isEmpty() ? "No prize specified" : "Prize: **"+giv.prize+"**").append(" | Ends in ")
                .append(FormatUtil.secondsToTime(Instant.now().until(giv.end, ChronoUnit.SECONDS))));
        event.replySuccess(sb.toString());
    }
}
