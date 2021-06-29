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
import com.jagrosh.giveawaybot.rest.RestMessageAction;
import com.jagrosh.giveawaybot.rest.RestJDA;
import com.jagrosh.giveawaybot.rest.RestReactionPaginationAction;
import com.jagrosh.giveawaybot.util.FormatUtil;
import com.jagrosh.giveawaybot.util.GiveawayUtil;
import java.awt.Color;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.internal.utils.EncodingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Giveaway 
{
    
    public static final Logger LOG = LoggerFactory.getLogger("GIVEAWAY");
    
    public final long messageId, channelId, guildId, userId;
    public final Instant end;
    public final int winners;
    public final String prize;
    public final Status status;
    public final boolean expanded;
    
    public Giveaway(long messageId, long channelId, long guildId, long userId, Instant end, int winners, String prize, Status status, boolean expanded)
    {
        this.messageId = messageId;
        this.channelId = channelId;
        this.guildId = guildId;
        this.userId = userId;
        this.end = end;
        this.winners = winners;
        this.prize = prize==null ? null : prize.isEmpty() ? null : prize;
        this.status = status;
        this.expanded = expanded;
    }
    
    public Message render(Color color, Instant now)
    {
        MessageBuilder mb = new MessageBuilder();
        boolean close = now.plusSeconds(9).isAfter(end);
        mb.append(Constants.YAY).append(close ? " **G I V E A W A Y** " : "   **GIVEAWAY**   ").append(Constants.YAY);
        EmbedBuilder eb = new EmbedBuilder();
        if(close)
            eb.setColor(Color.RED);
        else if(color==null)
            eb.setColor(Constants.BLURPLE);
        else
            eb.setColor(color);
        eb.setFooter((winners==1 ? "" : winners+" winners | ")+"Ends at",null);
        eb.setTimestamp(end);
        eb.setDescription("React with " + Constants.TADA + " to enter!"
                + "\nEnds: <t:" + end.getEpochSecond() + ":R> (<t:" + end.getEpochSecond() + ":f>"
                + "\nHosted by: <@" + userId + ">");
        if(prize!=null)
            eb.setAuthor(prize, null, null);
        if(close)
            eb.setTitle("Last chance to enter!!!", null);
        mb.setEmbed(eb.build());
        return mb.build();
    }
    
    public void update(RestJDA restJDA, Database database, Instant now)
    {
        update(restJDA, database, now, true);
    }
    
    public void update(RestJDA restJDA, Database database, Instant now, boolean queue)
    {
        Message rendered = render(database.settings.getSettings(guildId).color, now);
        RestMessageAction ra = restJDA.editMessage(channelId, messageId, rendered);
        List<RestMessageAction> additional = expanded ? database.expanded.getExpanded(messageId).entrySet()
                .stream().map(e -> restJDA.editMessage(e.getKey(), e.getValue(), rendered))
                .collect(Collectors.toList()) : Collections.EMPTY_LIST;
        if(queue)
        {
            ra.queue(m -> {}, t -> handleThrowable(t, database));
            additional.forEach(a -> a.queue(m -> {}, t -> {}));
        }
        else
        {
            try
            {
                ra.complete(false);
            }
            catch(Exception t)
            {
                handleThrowable(t, database);
            }
            for(RestMessageAction rma: additional)
            {
                try
                {
                    rma.complete(false);
                }
                catch(Exception ignored) {}
            }
        }
    }
    
    private void handleThrowable(Throwable t, Database database)
    {
        if(t instanceof ErrorResponseException)
        {
            ErrorResponseException e = (ErrorResponseException)t;
            switch(e.getErrorCode())
            {
                // delete the giveaway, since the bot wont be able to have access again
                case 10008: // message not found
                case 10003: // channel not found
                    database.giveaways.deleteGiveaway(messageId);
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
        else if (t instanceof RateLimitedException) { /* ignore */ }
        else LOG.error("RestAction failure: ["+t+"] "+t.getMessage());
    }
    
    private String messageLink()
    {
        return String.format("<https://discord.com/channels/%d/%d/%d>", guildId, channelId, messageId);
    }
    
    private List<User> getAllReactions(RestJDA restJDA, long channel, long message, String emoji)
    {
        RestReactionPaginationAction ra = restJDA.getReactionUsers(channel, message, emoji);
        List<User> list = new ArrayList<>();
        List<User> chunk;
        do
        {
            chunk = ra.getNextChunk();
            list.addAll(chunk);
        } while(chunk.size() == ra.getMaxLimit());
        return list;
    }
    
    public void end(RestJDA restJDA, Map<Long,Long> additional)
    {
        LOG.debug("Ending giveaway " + guildId + "/" + channelId + "/" + messageId + (additional.isEmpty() ? "" : " (expanded)"));
        String emoji = EncodingUtil.encodeUTF8(Constants.TADA);
        MessageBuilder mb = new MessageBuilder();
        MessageBuilder mb2 = new MessageBuilder();
        mb.append(Constants.YAY).append(" **GIVEAWAY ENDED** ").append(Constants.YAY);
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(new Color(0x36393F)); // dark theme background
        eb.setFooter((winners==1 ? "" : winners+" Winners | ") + "Ended at",null);
        eb.setTimestamp(end);
        if(prize!=null)
            eb.setAuthor(prize, null, null);
        try 
        {
            // stream over all the users that reacted (paginating as necessary
            LOG.debug("Retrieving reactions for giveaway " + messageId);
            List<User> users = getAllReactions(restJDA, channelId, messageId, emoji);
            additional.entrySet().forEach(e -> users.addAll(getAllReactions(restJDA, e.getKey(), e.getValue(), emoji)));
            Set<Long> ids = users.stream().filter(u -> !u.isBot()).map(u -> u.getIdLong()).distinct().collect(Collectors.toSet());
            LOG.debug("Retrieved " + ids.size() + " reactions for giveaway " + messageId);
            mb2.setEmbed(new EmbedBuilder()
                    .setColor(new Color(0x36393F))
                    .setDescription("**" + ids.size() + "** entrants [\u2197](" + messageLink() + ")") // ↗
                    .build());
            List<Long> wins = GiveawayUtil.selectWinners(ids, winners);
            LOG.debug("Selected " + wins.size() + " winners for giveaway " + messageId);
            if(wins.isEmpty())
            {
                eb.setDescription("Not enough entrants to determine a winner!");
                mb2.setContent("No valid entrants, so a winner could not be determined!");
            }
            else if(wins.size()==1)
            {
                eb.setDescription("Winner: <@" + wins.get(0) + ">");
                mb2.setContent("Congratulations <@" + wins.get(0) + ">! You won" + (prize==null ? "" : " the **" + prize + "**") + "!");
            }
            else
            {
                eb.setDescription("Winners:");
                wins.forEach(w -> eb.appendDescription("\n").appendDescription("<@"+w+">"));
                mb2.setContent("Congratulations <@"+wins.get(0)+">");
                for(int i=1; i<wins.size(); i++)
                    mb2.append(", <@"+wins.get(i)+">");
                mb2.append("! You won" + (prize == null ? "" : " the **" + prize + "**") + "!");
            }
            mb.setEmbed(eb.appendDescription("\nHosted by: <@" + userId + ">").build());

        } 
        catch(ErrorResponseException e)
        {
            mb.setEmbed(eb.setDescription("Could not determine a winner!\nHosted by: <@" + userId + ">").build());
            mb2.setContent("A winner could not be determined! (" + e.getMeaning() + ")");
        }
        catch(Exception e) 
        {
            mb.setEmbed(eb.setDescription("Could not determine a winner!\nHosted by: <@" + userId + ">").build());
            mb2.setContent("A winner could not be determined!");
        }
        Message msg = mb.build();
        Message msg2 = mb2.build();
        restJDA.editMessage(channelId, messageId, msg).queue(m->{}, f->{});
        restJDA.sendMessage(channelId, msg2).queue(m->{}, f->{});
        additional.forEach((chid, msgid) -> 
        {
            restJDA.editMessage(chid, msgid, msg).queue(m -> {}, f -> {});
            restJDA.sendMessage(chid, msg2).queue(m -> {}, f -> {});
        });
    }
}
