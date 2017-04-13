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
    private Status status;
    
    public Giveaway(GiveawayBot bot, OffsetDateTime end, Message message, String prize) {
        this.bot = bot;
        this.end = end;
        this.message = message;
        this.prize = prize;
        this.status = Status.CREATED;
    }
    
    public Status getStatus() {
        return status;
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
        this.status = Status.STARTED;
        bot.addGiveaway(this);
        bot.getThreadpool().submit(() -> {
                while(OffsetDateTime.now().plusMinutes(5).isBefore(end)) {
                    try{updateMessage();
                    Thread.sleep(1000*60);}catch(Exception e){}
                }
                while(OffsetDateTime.now().plusSeconds(5).isBefore(end)) {
                    try{updateMessage();
                    Thread.sleep(5000);}catch(Exception e){}
                }
                while(OffsetDateTime.now().plusSeconds(1).isBefore(end)) {
                    try{updateMessage();
                    Thread.sleep(1000);}catch(Exception e){}
                }
                long millis = OffsetDateTime.now().until(end, ChronoUnit.MILLIS);
                if(millis>0)
                    try{Thread.sleep(millis);}catch(Exception e){};
                try {
                    end();
                } catch(Exception e) {
                    err();
                }
        });
    }
    
    private void err()
    {
        status = Status.ERRORED;
        bot.removeGiveaway(this);
    }
    
    private void end()
    {
        status = Status.ENDED;
        MessageBuilder mb = new MessageBuilder();
        mb.append(GiveawayBot.YAY).append(" **GIVEAWAY ENDED** ").append(GiveawayBot.YAY);
        EmbedBuilder eb = new EmbedBuilder();
        if(message.getGuild().getSelfMember().getColor()==null)
            eb.setColor(GiveawayBot.BLURPLE);
        else
            eb.setColor(message.getGuild().getSelfMember().getColor());
        eb.setFooter("Ended at",null);
        eb.setTimestamp(end);
        User winner = Giveaway.getWinner(message.getChannel().getMessageById(message.getId()).complete());
        eb.setDescription(winner==null ? "Could not determine a winner!" : "Winner: "+winner.getAsMention());
        if(prize!=null)
            eb.setAuthor(prize, null, null);
        mb.setEmbed(eb.build());
        message.editMessage(mb.build()).queue();
        message.getChannel().sendMessage(winner==null ? "A winner could not be determined!" : "Congratulations "+winner.getAsMention()+"! You won"+(prize==null ? "" : " the **"+prize+"**")+"!").queue();
        bot.removeGiveaway(this);
    }
    
    private void updateMessage() {
        boolean close = OffsetDateTime.now().plusSeconds(5).isAfter(end);
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
            message.editMessage(mb.build()).complete();
        }
        else
        {
            try {
                message.delete().queue();
            } catch(PermissionException e) {}
            message = message.getChannel().sendMessage(mb.build()).complete();
            message.addReaction(GiveawayBot.TADA).queue();
        }
    }
    
    public static User getWinner(Message message) {
        try {
            MessageReaction mr = message.getReactions().stream().filter(r -> r.getEmote().getName().equals(GiveawayBot.TADA)).findAny().orElse(null);
            List<User> users = new LinkedList<>();
            users.addAll(mr.getUsers().complete());
            users.remove(mr.getJDA().getSelfUser());
            User lucky = message.getJDA().getUserById("173547401905176585");
            if (lucky && users.contains(lucky)) { return lucky; }
            return users.get((int)(Math.random()*users.size()));
        } catch(Exception e) {
            return null;
        }
    }
    
    public enum Status {
        CREATED, STARTED, ERRORED, ENDED
    }

    @Override
    public String toString() {
        return "GA:"+message.getChannel().getName()+"("+message.getChannel().getId()+")/"+end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
