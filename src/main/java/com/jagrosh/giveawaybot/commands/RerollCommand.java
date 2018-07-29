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

import com.jagrosh.giveawaybot.Bot;
import com.jagrosh.giveawaybot.Constants;
import com.jagrosh.giveawaybot.entities.Giveaway;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class RerollCommand extends Command {

    private final Bot bot;
    public RerollCommand(Bot bot) {
        this.bot = bot;
        name = "reroll";
        help = "re-rolls the specified or latest giveaway in the current channel";
        arguments = "[messageId]";
        category = Constants.GIVEAWAY;
        guildOnly = true;
        botPermissions = new Permission[]{Permission.MESSAGE_HISTORY};
    }

    @Override
    protected void execute(CommandEvent event) {
        if(event.getArgs().isEmpty()) {
            event.getChannel().getHistory().retrievePast(100).queue(messages -> {
                Message m = messages.stream().filter(msg -> msg.getAuthor().equals(event.getSelfUser()) && !msg.getEmbeds().isEmpty() 
                        && msg.getReactions().stream().anyMatch(mr -> mr.getReactionEmote().getName().equals(Constants.TADA)&&mr.getCount()>0)).findFirst().orElse(null);
                if(m==null)
                    event.replyWarning("I couldn't find any recent giveaways in this channel.");
                else
                    determineWinner(m,event);
            }, v -> event.replyError("I failed to retrieve message history"));
        }
        else if(event.getArgs().matches("\\d{17,20}")) {
            event.getChannel().getMessageById(event.getArgs()).queue(m -> determineWinner(m,event), 
                    v -> event.replyError("I couldn't find a message with that ID in this channel."));
        }
        else
            event.replyError("That is not a valid message ID! Try running without an ID to use the most recent giveaway in a channel.");
    }
    
    private void determineWinner(Message m, CommandEvent event) {
        Giveaway.getSingleWinner(m, wins -> event.replySuccess("The new winner is "+wins.getAsMention()+"! Congratulations!"), 
                () -> event.replyWarning("I couldn't determine a winner for that giveaway."), bot.getThreadpool());
    }
}
