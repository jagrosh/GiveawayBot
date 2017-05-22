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

import com.jagrosh.giveawaybot.GiveawayBot;
import com.jagrosh.giveawaybot.util.FormatUtil;
import java.awt.Color;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageReaction;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.PermissionException;

/**
 *
 * This class represents a Giveaway
 * 
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Giveaway {
    
    private final GiveawayBot bot;
    private final OffsetDateTime end;
    private Message message;
    private final String prize;
    private final String[] dmprizes;
    
    public Giveaway(GiveawayBot bot, OffsetDateTime end, Message message, String prize, String[] dmprizes) {
        this.bot = bot;
        this.end = end;
        this.message = message;
        this.prize = prize;
        this.dmprizes = dmprizes;
    }
    
    public OffsetDateTime getEnd() {
        return end;
    }
    
    public Message getMessage() {
        return message;
    }
    
    public String getPrize() {
        return prize;
    }
    
    public void start()
    {
        bot.getGiveaways().add(this);
        updateMessage();
    }
    
    public void end()
    {
        bot.getGiveaways().remove(this);
        if(message==null)
            return;
        MessageBuilder mb = new MessageBuilder();
        mb.append(GiveawayBot.YAY).append(" **GIVEAWAY ENDED** ").append(GiveawayBot.YAY);
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(new Color(1));
        eb.setFooter("Ended at",null);
        eb.setTimestamp(end);
        try {
            message = message.getChannel().getMessageById(message.getIdLong()).complete();
            User winner = Giveaway.getWinner(message);
            eb.setDescription(winner==null ? "Could not determine a winner!" : "Winner: "+winner.getAsMention());
            if(prize!=null)
                eb.setAuthor(prize, null, null);
            mb.setEmbed(eb.build());
            message.editMessage(mb.build()).queue();
            message.getChannel().sendMessage(winner==null ? "A winner could not be determined!" : "Congratulations "+winner.getAsMention()+"! You won"+(prize==null ? "" : " the **"+prize+"**")+"!").queue();
        } catch(Exception e) {
        }
    }
    
    public void updateMessage() {
        try {
            boolean close = OffsetDateTime.now().plusSeconds(6).isAfter(end);
            MessageBuilder mb = new MessageBuilder();
            mb.append(GiveawayBot.YAY).append(close ? " **G I V E A W A Y** " : "   **GIVEAWAY**   ").append(GiveawayBot.YAY);
            EmbedBuilder eb = new EmbedBuilder();
            if(close)
                eb.setColor(Color.RED);
            else if(message.getGuild().getSelfMember().getColor()==null)
                eb.setColor(GiveawayBot.BLURPLE);
            else
                eb.setColor(message.getGuild().getSelfMember().getColor());
            eb.setFooter("Ends at",null);
            eb.setTimestamp(end);
            eb.setDescription("React with "+GiveawayBot.TADA+" to enter!\nTime remaining: "+FormatUtil.secondsToTime(OffsetDateTime.now().until(end, ChronoUnit.SECONDS)));
            if(prize!=null)
                eb.setAuthor(prize, null, null);
            if(close)
                eb.setTitle("Last chance to enter!!!", null);
            mb.setEmbed(eb.build());
            if(message.getAuthor().equals(message.getJDA().getSelfUser()))
            {
                message.editMessage(mb.build()).queue(m -> {}, f -> {});
            }
            else
            {
                try {
                    message.delete().queue();
                } catch(PermissionException e) {}
                message.getChannel().sendMessage(mb.build()).queue(m -> {
                        message = m;
                        message.addReaction(GiveawayBot.TADA).queue();
                }, f -> {
                        message = null;
                });
            }
        } catch(Exception e) {
        }
    }
    
    public static User getWinner(Message message) {
        try {
            MessageReaction mr = message.getReactions().stream().filter(r -> r.getEmote().getName().equals(GiveawayBot.TADA)).findAny().orElse(null);
            List<User> users = new LinkedList<>();
            users.addAll(mr.getUsers().complete());
            users.remove(mr.getJDA().getSelfUser());
            return users.get((int)(Math.random()*users.size()));
        } catch(Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "GA:"+message.getChannel().getName()+"("+message.getChannel().getId()+")/"+end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    
    public static Giveaway fromMessage(Message m, GiveawayBot bot) {
        try {
            return new Giveaway(bot, m.getEmbeds().get(0).getTimestamp(), m, m.getEmbeds().get(0).getAuthor().getName(), null);
        } catch(Exception e) {
            return null;
        }
    }
}
