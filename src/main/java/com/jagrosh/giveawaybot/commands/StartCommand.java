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
import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import java.time.Instant;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.exceptions.PermissionException;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class StartCommand extends Command {

    private final Bot bot;
    public StartCommand(Bot bot) {
        this.bot = bot;
        name = "start";
        help = "starts a giveaway; time can be in seconds (Ex: __40__ or __40s__) or minutes (Ex: __3m__)";
        arguments = "<time> [prize]";
        category = Constants.GIVEAWAY;
        guildOnly = true;
        botPermissions = new Permission[]{Permission.MESSAGE_HISTORY,Permission.MESSAGE_ADD_REACTION,Permission.MESSAGE_EMBED_LINKS};
    }
    
    @Override
    protected void execute(CommandEvent event) {
        if(!Constants.canSendGiveaway(event.getTextChannel()))
        {
            event.replyError("I cannot start a giveaway here; please make sure I have the following permissions:\n\n"+Constants.PERMS);
            return;
        }
        String[] parts = event.getArgs().split("\\s+",2);
        boolean minutes;
        if(parts[0].toLowerCase().endsWith("m")) {
            parts[0] = parts[0].substring(0,parts[0].length()-1);
            minutes = true;
        } else if (parts[0].toLowerCase().endsWith("s")) {
            parts[0] = parts[0].substring(0,parts[0].length()-1);
            minutes = false;
        } else
            minutes = false;
        try {
            int value = (int)(Double.parseDouble(parts[0]) * (minutes ? 60 : 1) );
            if(!Constants.validTime(value))
            {
                event.replyWarning("Time must be at least 10 seconds and can't be longer than a week!");
                return;
            }
            try{ event.getMessage().delete().queue(); }catch(PermissionException ex){}
            Instant now = Instant.now();
            String item = parts.length==1 ? null : (parts[1].length()>Constants.PRIZE_MAX ? parts[1].substring(0,Constants.PRIZE_MAX) : parts[1]);
            bot.startGiveaway(event.getTextChannel(), now, value, 1, item);
        } catch(NumberFormatException e) {
            event.replyWarning("Failed to parse "+(minutes ? "minutes" : "seconds")+" from `"+parts[0]+"`.");
        }
    }
    
}
