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
package com.jagrosh.giveawaybot.commands;

import com.jagrosh.giveawaybot.GiveawayBot;
import com.jagrosh.giveawaybot.entities.Giveaway;
import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class EndCommand extends Command {
    private final GiveawayBot bot;

    public EndCommand(GiveawayBot bot) {
        this.bot = bot;
        name = "end";
        help = "ends (picks a winner for) the specified or latest giveaway in the current channel";
        arguments = "[messageId]";
        category = GiveawayBot.GIVEAWAY;
        guildOnly = true;
        botPermissions = new Permission[]{Permission.MESSAGE_HISTORY};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getArgs().isEmpty()) {
            Giveaway giveaway = null;
            for (Giveaway g : bot.getGiveaways()) {
                if (g.getMessage().getChannel().equals(event.getChannel())) {
                    if (giveaway == null || g.getMessage().getCreationTime().isAfter(giveaway.getMessage().getCreationTime()))
                        giveaway = g;
                }
            }
            if (giveaway != null) {
                giveaway.end();
                return;
            }
            event.getChannel().getHistory().retrievePast(100).queue(messages -> {
                Message m = messages.stream().filter(msg -> msg.getAuthor().equals(event.getSelfUser()) && !msg.getEmbeds().isEmpty() && msg.getEmbeds().get(0).getColor().getRGB() != 1
                        && msg.getReactions().stream().anyMatch(mr -> mr.getEmote().getName().equals(GiveawayBot.TADA) && mr.getCount() > 0)).findFirst().orElse(null);
                if (m == null)
                    event.replyWarning("I couldn't find any recent giveaways in this channel.");
                else {
                    Giveaway g = Giveaway.fromMessage(m, bot);
                    if (g == null)
                        event.replyWarning("I couldn't find any recent giveaways in this channel.");
                    else
                        g.end();
                }
            }, v -> event.replyError("I failed to retrieve message history"));
        } else if (event.getArgs().matches("\\d{17,20}")) {
            Giveaway giveaway = bot.getGiveaways().stream().filter(g -> g.getMessage().getId().equals(event.getArgs())).findAny().orElse(null);
            if (giveaway == null) {
                event.getChannel().getMessageById(event.getArgs()).queue(m -> {
                    Giveaway g = Giveaway.fromMessage(m, bot);
                    if (g == null)
                        event.replyWarning("I couldn't find any recent giveaways in this channel.");
                    else
                        g.end();
                }, v -> event.replyError("I failed to retrieve that message."));
            } else
                giveaway.end();
        } else {
            event.replyError("That is not a valid message ID! Try running without an ID to use the most recent giveaway in a channel.");
        }
    }

}
