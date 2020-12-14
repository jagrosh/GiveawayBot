/*
 * Copyright 2020 John Grosh (john.a.grosh@gmail.com).
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
import com.jagrosh.giveawaybot.util.GiveawayUtil;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class RerolldistCommand extends GiveawayCommand 
{
    private final static Pattern ARGS_PATTERN = Pattern.compile("<#(\\d{17,20})>\\s*(\\d{17,20})\\s*");
    
    public RerolldistCommand(Bot bot) 
    {
        super(bot);
        name = "reroll-distributed";
        help = "re-rolls from multiple messages";
        arguments = "<#channel messageid> ...";
        botPermissions = new Permission[]{Permission.MESSAGE_HISTORY};
        hidden = true;
        cooldown = 10;
        needsPremium = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        event.getChannel().sendTyping().queue();
        Matcher m = ARGS_PATTERN.matcher(event.getArgs());
        Map<TextChannel,Long> ids = new HashMap<>();
        while(m.find())
        {
            TextChannel tc = event.getGuild().getTextChannelById(m.group(1));
            if(tc == null)
            {
                event.replyError("`" + m.group(1) + "` is not a text channel ID in this guild.");
                return;
            }
            ids.put(tc, Long.parseLong(m.group(2)));
        }
        if(ids.isEmpty())
        {
            event.replyWarning("Please include channels and messages IDs. Ex: `" + event.getClient().getPrefix() + name 
                    + " #channel1 messageId1 #channel2 messageId2`");
            return;
        }
        // do this async
        bot.getThreadpool().submit(() -> 
        {
            try
            {
                // get all the users
                Set<User> set = new HashSet<>();
                ids.forEach((tc,mid) -> 
                {
                    Message msg = tc.retrieveMessageById(mid).complete();
                    if(msg != null)
                    {
                        MessageReaction mr = msg.getReactions().stream()
                                .filter(r -> Constants.TADA.equals(r.getReactionEmote().getName())).findFirst().orElse(null);
                        if(mr != null)
                            mr.retrieveUsers().stream().filter(u -> !u.isBot()).forEach(u -> set.add(u));
                    }
                });

                List<User> winner = GiveawayUtil.selectWinners(set, 1);
                if(winner.isEmpty())
                    event.replyWarning("A winner could not be determined!");
                else
                    event.replySuccess("From " + ids.size() + " channels and " + set.size() + " entrants... the new winner is " + winner.get(0).getAsMention() + "! Congratulations!");
            } 
            catch(Exception ex)
            {
                ex.printStackTrace();
                event.replyError("Something went wrong...");
            }
        });
    }
}
