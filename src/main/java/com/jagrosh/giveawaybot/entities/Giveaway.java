/*
 * Copyright 2017 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.giveawaybot.entities;

import com.jagrosh.giveawaybot.Constants;
import com.jagrosh.giveawaybot.database.Database;
import com.jagrosh.giveawaybot.rest.RestJDA;
import com.jagrosh.giveawaybot.util.FormatUtil;
import java.awt.Color;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageReaction;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.utils.MiscUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Giveaway {
    
    public static final Logger LOG = LoggerFactory.getLogger("REST");
    
    public final long messageId;
    public final long channelId;
    public final long guildId;
    public final Instant end;
    public final int winners;
    public final String prize;
    
    public Giveaway(long messageId, long channelId, long guildId, Instant end, int winners, String prize)
    {
        this.messageId = messageId;
        this.channelId = channelId;
        this.guildId = guildId;
        this.end = end;
        this.winners = winners;
        this.prize = prize==null ? null : prize.isEmpty() ? null : prize;
    }
    
    public Message render(Color color, Instant now)
    {
        MessageBuilder mb = new MessageBuilder();
        boolean close = now.plusSeconds(6).isAfter(end);
        mb.append(Constants.YAY).append(close ? " **G I V E A W A Y** " : "   **GIVEAWAY**   ").append(Constants.YAY);
        EmbedBuilder eb = new EmbedBuilder();
        if(close)
            eb.setColor(Color.RED);
        else if(color==null)
            eb.setColor(Constants.BLURPLE);
        else
            eb.setColor(color);
        eb.setFooter((winners==1 ? "" : winners+" Winners | ")+"Ends at",null);
        eb.setTimestamp(end);
        eb.setDescription("React with "+Constants.TADA+" to enter!\nTime remaining: "+FormatUtil.secondsToTime(now.until(end, ChronoUnit.SECONDS)));
        if(prize!=null)
            eb.setAuthor(prize, null, null);
        if(close)
            eb.setTitle("Last chance to enter!!!", null);
        mb.setEmbed(eb.build());
        return mb.build();
    }
    
    public void update(RestJDA restJDA, Database connector, Instant now)
    {
        restJDA.editMessage(channelId, messageId, render(connector.settings.getSettings(guildId).color, now)).queue(m -> {}, t -> {
            if(t instanceof ErrorResponseException)
            {
                ErrorResponseException e = (ErrorResponseException)t;
                switch(e.getErrorCode())
                {
                    // delete the giveaway, since the bot wont be able to have access again
                    case 10008: // message not found
                    case 10003: // channel not found
                        connector.giveaways.deleteGiveaway(messageId);
                        break;
                        
                    // for now, just keep chugging, maybe we'll get perms back
                    case 50001: // missing access
                    case 50013: // missing permissions
                        break;
                     
                    // anything else, print it out
                    default:
                        LOG.warn("RestAction returned error: "+e.getErrorCode()+": "+e.getMeaning());
                }
            }
            else
                LOG.error("RestAction failure: ["+t+"] "+t.getMessage());
        });
    }
    
    public void end(RestJDA restJDA)
    {
        MessageBuilder mb = new MessageBuilder();
        mb.append(Constants.YAY).append(" **GIVEAWAY ENDED** ").append(Constants.YAY);
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(new Color(1));
        eb.setFooter((winners==1 ? "" : winners+" Winners | ")+"Ended at",null);
        eb.setTimestamp(end);
        if(prize!=null)
            eb.setAuthor(prize, null, null);
        try {
            List<Long> ids = restJDA.getReactionUsers(Long.toString(channelId), Long.toString(messageId), MiscUtil.encodeUTF8(Constants.TADA))
                    .cache(true).stream().distinct().collect(Collectors.toList());
            List<Long> wins = selectWinners(ids, winners);
            String toSend;
            if(wins.isEmpty())
            {
                eb.setDescription("Could not determine a winner!");
                toSend = "A winner could not be determined!";
            }
            else if(wins.size()==1)
            {
                eb.setDescription("Winner: <@"+wins.get(0)+">");
                toSend = "Congratulations <@"+wins.get(0)+">! You won"+(prize==null ? "" : " the **"+prize+"**")+"!";
            }
            else
            {
                eb.setDescription("Winners:");
                wins.forEach(w -> eb.appendDescription("\n").appendDescription("<@"+w+">"));
                toSend = "Congratulations <@"+wins.get(0)+">";
                for(int i=1; i<wins.size(); i++)
                    toSend+=", <@"+wins.get(i)+">";
                toSend+="! You won"+(prize==null ? "" : " the **"+prize+"**")+"!";
            }
            mb.setEmbed(eb.build());
            restJDA.editMessage(channelId, messageId, mb.build()).queue();
            restJDA.sendMessage(channelId, toSend).queue();
        } catch(Exception e) {
            eb.setDescription("Could not determine a winner!");
            mb.setEmbed(eb.build());
            restJDA.editMessage(channelId, messageId, mb.build()).queue();
            restJDA.sendMessage(channelId, "A winner could not be determined!").queue();
        }
    }
    
    public static <T> List<T> selectWinners(List<T> list, int winners)
    {
        List<T> winlist = new LinkedList<>();
        List<T> pullist = new LinkedList<>(list);
        for(int i=0; i<winners && !pullist.isEmpty(); i++)
        {
            winlist.add(pullist.remove((int)(Math.random()*pullist.size())));
        }
        return winlist;
    }
    
    public static void getSingleWinner(Message message, Consumer<User> success, Runnable failure, ExecutorService threadpool)
    {
        threadpool.submit(() -> {
            try {
                MessageReaction mr = message.getReactions().stream().filter(r -> r.getReactionEmote().getName().equals(Constants.TADA)).findAny().orElse(null);
                List<User> users = new LinkedList<>();
                mr.getUsers().stream().distinct().filter(u -> !u.isBot()).forEach(u -> users.add(u));
                if(users.isEmpty())
                    failure.run();
                else
                    success.accept(users.get((int)(Math.random()*users.size())));
            } catch(Exception e) {
                failure.run();
            }
        });
    }
}
