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
import com.jagrosh.giveawaybot.util.FormatUtil;
import com.jagrosh.giveawaybot.util.OtherUtil;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.time.Instant;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.exceptions.PermissionException;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class StartCommand extends Command
{
    private final static String EXAMPLE = "\nExample usage: `!gstart 30m 5w Awesome T-Shirt`";
    private final Bot bot;
    public StartCommand(Bot bot)
    {
        this.bot = bot;
        name = "start";
        help = "starts a giveaway (quick setup)";
        arguments = "<time> [winners]w [prize]";
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
        if(event.getArgs().isEmpty())
        {
            event.replyError("Please include a length of time, and optionally a number of winners and a prize!"+EXAMPLE);
            return;
        }
        String[] parts = event.getArgs().split("\\s+", 2);
        int seconds = OtherUtil.parseShortTime(parts[0]);
        if(seconds==-1)
        {
            event.replyWarning("Failed to parse time from `"+parts[0]+"`"+EXAMPLE);
            return;
        }
        if(!OtherUtil.validTime(seconds))
        {
            event.replyWarning(Constants.TIME_MSG);
            return;
        }
        int winners = 1;
        String item = null;
        if(parts.length>1)
        {
            String[] parts2 = parts[1].split("\\s+", 2);
            winners = OtherUtil.parseWinners(parts2[0]);
            if(winners==-1)
            {
                winners = 1;
                item = parts[1];
            }
            else
            {
                item = parts2.length>1 ? parts2[1] : null;
            }
        }
        if(!OtherUtil.validWinners(winners))
        {
            event.replyWarning("Number of winners must be at least 1 and no larger than "+Constants.MAX_WINNERS+EXAMPLE);
            return;
        }
        try{ event.getMessage().delete().queue(); }catch(PermissionException ex){}
        Instant now = event.getMessage().getCreationTime().toInstant();
        if(bot.getDatabase().giveaways.getGiveaways(event.getGuild()).size() >= Constants.MAX_GIVEAWAYS)
        {
            event.replyError("There are already "+Constants.MAX_GIVEAWAYS+" running on this server!");
            return;
        }
        bot.startGiveaway(event.getTextChannel(), now, seconds, winners, item);
    }
    
}
