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
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageReaction;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.PermissionException;

import java.awt.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * This class represents a Giveaway
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Giveaway {

    private final GiveawayBot bot;
    private final OffsetDateTime end;
    private final String prize;
    private final int winners;
    private Message message;

    public Giveaway(GiveawayBot bot, OffsetDateTime end, Message message, String prize, int winners) {
        this.bot = bot;
        this.end = end;
        this.message = message;
        this.prize = prize;
        this.winners = winners;
    }

    public static void getWinners(Message message, Consumer<List<User>> success, Runnable failure) {
        try {
            MessageReaction mr = message.getReactions().stream().filter(r -> r.getEmote().getName().equals(GiveawayBot.TADA)).findAny().orElse(null);
            //noinspection ConstantConditions
            mr.getUsers(100).queue(u -> {
                List<User> users = new LinkedList<>();
                users.addAll(u);
                users.remove(mr.getJDA().getSelfUser());
                if (users.isEmpty())
                    failure.run();
                else {
                    int wincount;
                    String[] split = message.getEmbeds().get(0).getFooter().getText().split(" ");
                    try {
                        wincount = Integer.parseInt(split[0]);
                    } catch (NumberFormatException e) {
                        wincount = 1;
                    }
                    List<User> wins = new LinkedList<>();
                    for (int i = 0; i < wincount && !users.isEmpty(); i++) {
                        wins.add(users.remove((int) (Math.random() * users.size())));
                    }
                    success.accept(wins);
                }
            }, f -> failure.run());
        } catch (Exception e) {
            failure.run();
        }
    }

    public static Giveaway fromMessage(Message m, GiveawayBot bot) {
        try {
            String[] split = m.getEmbeds().get(0).getFooter().getText().split(" ");
            int winners;
            try {
                winners = Integer.parseInt(split[0]);
            } catch (NumberFormatException e) {
                winners = 1;
            }
            return new Giveaway(bot, m.getEmbeds().get(0).getTimestamp(), m, m.getEmbeds().get(0).getAuthor().getName(), winners);
        } catch (Exception e) {
            return null;
        }
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

    public int getWinners() {
        return winners;
    }

    public void start() {
        bot.getGiveaways().add(this);
        updateMessage();
    }

    public void end() {
        bot.getGiveaways().remove(this);
        if (message == null)
            return;
        MessageBuilder mb = new MessageBuilder();
        mb.append(GiveawayBot.YAY).append(" **GIVEAWAY ENDED** ").append(GiveawayBot.YAY);
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(new Color(1));
        eb.setFooter((winners == 1 ? "" : winners + " Winners | ") + "Ended at", null);
        eb.setTimestamp(end);
        if (prize != null)
            eb.setAuthor(prize, null, null);
        try {
            message.getChannel().getMessageById(message.getIdLong()).queue(m -> Giveaway.getWinners(m, wins -> {
                StringBuilder str = new StringBuilder(wins.get(0).getAsMention());
                if (wins.size() == 1) {
                    eb.setDescription("Winner: " + wins.get(0).getAsMention());
                } else {
                    eb.setDescription("Winners:");
                    wins.forEach(w -> eb.appendDescription("\n").appendDescription(w.getAsMention()));
                    for (int i = 1; i < wins.size(); i++)
                        str.append(", ").append(wins.get(i).getAsMention());
                }
                mb.setEmbed(eb.build());
                m.editMessage(mb.build()).queue();
                m.getChannel().sendMessage("Congratulations " + str + "! You won" + (prize == null ? "" : " the **" + prize + "**") + "!").queue();
            }, () -> {
                eb.setDescription("Could not determine a winner!");
                mb.setEmbed(eb.build());
                m.editMessage(mb.build()).queue();
                m.getChannel().sendMessage("A winner could not be determined!").queue();
            }), v -> {
            });
        } catch (Exception ignored) {
        }
    }

    public void updateMessage() {
        try {
            boolean close = OffsetDateTime.now().plusSeconds(6).isAfter(end);
            MessageBuilder mb = new MessageBuilder();
            mb.append(GiveawayBot.YAY).append(close ? " **G I V E A W A Y** " : "   **GIVEAWAY**   ").append(GiveawayBot.YAY);
            EmbedBuilder eb = new EmbedBuilder();
            if (close)
                eb.setColor(Color.RED);
            else if (message.getGuild().getSelfMember().getColor() == null)
                eb.setColor(GiveawayBot.BLURPLE);
            else
                eb.setColor(message.getGuild().getSelfMember().getColor());
            eb.setFooter((winners == 1 ? "" : winners + " Winners | ") + "Ends at", null);
            eb.setTimestamp(end);
            eb.setDescription("React with " + GiveawayBot.TADA + " to enter!\nTime remaining: " + FormatUtil.secondsToTime(OffsetDateTime.now().until(end, ChronoUnit.SECONDS)));
            if (prize != null)
                eb.setAuthor(prize, null, null);
            if (close)
                eb.setTitle("Last chance to enter!!!", null);
            mb.setEmbed(eb.build());
            if (message.getAuthor().equals(message.getJDA().getSelfUser())) {
                message.editMessage(mb.build()).queue(m -> {
                }, f -> {
                });
            } else {
                try {
                    message.delete().queue();
                } catch (PermissionException ignored) {
                }
                message.getChannel().sendMessage(mb.build()).queue(m -> {
                    message = m;
                    message.addReaction(GiveawayBot.TADA).queue();
                }, f -> message = null);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public String toString() {
        return "GA:" + message.getChannel().getName() + "(" + message.getChannel().getId() + ")/" + end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
