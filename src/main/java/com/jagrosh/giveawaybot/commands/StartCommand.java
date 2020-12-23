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
import com.jagrosh.giveawaybot.entities.PremiumLevel;
import com.jagrosh.giveawaybot.util.FormatUtil;
import com.jagrosh.giveawaybot.util.OtherUtil;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.time.Instant;
import java.util.List;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.exceptions.PermissionException;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class StartCommand extends GiveawayCommand
{
    private final static String EXAMPLE = "\nExample usage: `!gstart 30m 5w Awesome T-Shirt`";
    
    public StartCommand(Bot bot)
    {
        super(bot);
        name = "start";
        help = "starts a giveaway (quick setup)";
        arguments = "<time> [winners]w [prize]";
        botPermissions = new Permission[]{Permission.MESSAGE_HISTORY,Permission.MESSAGE_ADD_REACTION,Permission.MESSAGE_EMBED_LINKS};
    }
    
    @Override
    protected void execute(CommandEvent event) 
    {
        // check permissions
        if(!Constants.canSendGiveaway(event.getTextChannel()))
        {
            event.replyError("I cannot start a giveaway here; please make sure I have the following permissions:\n\n"+Constants.PERMS);
            return;
        }
        
        // check channel type
        if(event.getTextChannel().isNews())
        {
            event.replyError("Giveaways cannot be created in announcements channels!");
            return;
        }
        
        // check for arguments
        if(event.getArgs().isEmpty())
        {
            event.replyError("Please include a length of time, and optionally a number of winners and a prize!"+EXAMPLE);
            return;
        }
        
        // parse and check length of time
        String[] parts = event.getArgs().split("\\s+", 2);
        int seconds = OtherUtil.parseShortTime(parts[0]);
        if(seconds==-1)
        {
            event.replyWarning("Failed to parse time from `"+parts[0]+"`"+EXAMPLE);
            return;
        }
        PremiumLevel level = bot.getDatabase().premium.getPremiumLevel(event.getGuild());
        if(!level.isValidTime(seconds))
        {
            event.replyError("Giveaway time must not be shorter than " + FormatUtil.secondsToTime(Constants.MIN_TIME) 
                    + " and no longer than " + FormatUtil.secondsToTime(level.maxTime));
            return;
        }
        
        // parse and check number of winners
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
        if(!level.isValidWinners(winners))
        {
            event.replyError("Number of winners must be at least 1 and no larger than " + level.maxWinners);
            return;
        }
        
        if(item != null && item.length()>250)
        {
            event.replyWarning("Ack! That prize is too long. Can you shorten it a bit?");
            return;
        }
        
        // check for too many giveaways runnning
        List<Giveaway> list = level.perChannelMaxGiveaways 
                ? bot.getDatabase().giveaways.getGiveaways(event.getTextChannel()) 
                : bot.getDatabase().giveaways.getGiveaways(event.getGuild());
        if(list == null)
        {
            event.replyError("An error occurred when trying to start giveaway.");
            return;
        }
        else if(list.size() >= level.maxGiveaways)
        {
            event.replyError("There are already " + level.maxGiveaways + " giveaways running in this " 
                    + (level.perChannelMaxGiveaways ? "channel" : "server") + "!");
            return;
        }
        
        // try to delete the command if possible
        try
        { 
            event.getMessage().delete().queue(); 
        }
        catch(PermissionException ignore) {}
        
        // start the giveaway
        Instant now = event.getMessage().getTimeCreated().toInstant();
        bot.startGiveaway(event.getTextChannel(), event.getAuthor(), now, seconds, winners, item);
    }
    
}
