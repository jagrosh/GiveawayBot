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
import me.jagrosh.jdautilities.commandclient.Command;
import me.jagrosh.jdautilities.commandclient.CommandEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class RerollCommand extends Command {

    public RerollCommand() {
        name = "reroll";
        help = "re-rolls the specified or latest giveaway in the current channel";
        arguments = "[messageId]";
        category = GiveawayBot.GIVEAWAY;
        guildOnly = true;
        botPermissions = new Permission[]{Permission.MESSAGE_HISTORY};
    }

    @Override
    protected void execute(CommandEvent event) {
        if(event.getArgs().isEmpty()) {
            event.getChannel().getHistory().retrievePast(100).queue(messages -> {
                Message m = messages.stream().filter(msg -> msg.getAuthor().equals(event.getSelfUser()) && !msg.getEmbeds().isEmpty() 
                        && msg.getReactions().stream().anyMatch(mr -> mr.getEmote().getName().equals(GiveawayBot.TADA)&&mr.getCount()>0)).findFirst().orElse(null);
                if(m==null)
                    event.reply(event.getClient().getWarning()+" I couldn't find any recent giveaways in this channel.");
                else
                {
                    User winner = Giveaway.getWinner(m);
                    if(winner==null)
                        event.reply(event.getClient().getWarning()+" I couldn't determine a winner for that message.");
                    else
                        event.reply(event.getClient().getSuccess()+" The new winner is "+winner.getAsMention()+"! Congratulations!");
                }
            }, v -> event.reply(event.getClient().getError()+" I failed to retrieve message history"));
        }
        else if(event.getArgs().matches("\\d{17,22}")) {
            event.getChannel().getMessageById(event.getArgs()).queue(m -> {
                User winner = Giveaway.getWinner(m);
                if(winner==null)
                    event.reply(event.getClient().getWarning()+" I couldn't determine a winner for that message.");
                else
                    event.reply(event.getClient().getSuccess()+" The new winner is "+winner.getAsMention()+"! Congratulations!");
            }, v -> event.reply(event.getClient().getError()+" I couldn't find a message with that ID in this channel."));
        }
        else
            event.reply(event.getClient().getError()+" That is not a valid message ID! Try running without an ID to use the most recent giveaway in a channel.");
    }
    
}
