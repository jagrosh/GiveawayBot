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
import java.time.OffsetDateTime;
import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import net.dv8tion.jda.core.Permission;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class StartCommand extends Command {

    private final GiveawayBot bot;
    public StartCommand(GiveawayBot bot) {
        this.bot = bot;
        name = "start";
        help = "starts a giveaway; time can be in seconds (Ex: __40__ or __40s__) or minutes (Ex: __3m__)";
        arguments = "<time> [prize]";
        category = GiveawayBot.GIVEAWAY;
        guildOnly = true;
        botPermissions = new Permission[]{Permission.MESSAGE_HISTORY,Permission.MESSAGE_ADD_REACTION,Permission.MESSAGE_EMBED_LINKS};
    }
    
    @Override
    protected void execute(CommandEvent event) {
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
            if(value<10 || value>60*60*24*7)
            {
                event.reply(event.getClient().getWarning()+" Time must be at least 10 seconds and can't be too long!");
                return;
            }
            OffsetDateTime end = OffsetDateTime.now().plusSeconds(value);
            String item = parts.length==1 ? null : parts[1];
            new Giveaway(bot, end, event.getMessage(), item, null).start();
        } catch(NumberFormatException e) {
            event.reply(event.getClient().getWarning()+" Failed to parse "+(minutes ? "minutes" : "seconds")+" from `"+parts[0]+"`.");
        }
    }
    
}
