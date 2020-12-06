/*
 * Copyright 2020 John Grosh (john.a.grosh@gmail.com).
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class DistributeCommand extends GiveawayCommand
{
    private final static String EXAMPLE = "\nExample usage: `!gstart-distributed #channel1 #channel2 30m 3w Your Prize Here`";
    private final static Pattern CHANNEL = Pattern.compile("\\s*<?#?(\\d{17,20})>?");
    
    public DistributeCommand(Bot bot)
    {
        super(bot);
        name = "start-distributed";
        help = "starts a giveaway, distributed across channels";
        arguments = "<channels> <time> [winners]w [prize]";
        hidden = true;
        cooldown = 10;
        needsPremium = true;
    }
    
    @Override
    protected void execute(CommandEvent event)
    {
        // get all channels
        Matcher m = CHANNEL.matcher(event.getArgs());
        List<Long> list = new ArrayList<>();
        int index = 0;
        while(m.find())
        {
            list.add(Long.parseLong(m.group(1)));
            index = m.end();
        }
        if(list.isEmpty())
        {
            event.replyError("Please include at least one text channel!");
            return;
        }
        String args = event.getArgs().substring(index);
        
        // check for valid text channels
        List<TextChannel> tcs = new ArrayList<>();
        for(long id: list)
        {
            TextChannel tc = event.getGuild().getTextChannelById(id);
            if(tc == null)
            {
                event.replyError("`" + id + "` is not a valid channel in this guild!");
                return;
            }
            tcs.add(tc);
        }
        
        if(tcs.size() < 2)
        {
            event.replyError("Please include at least 2 channels for a distributed giveaway!");
            return;
        }
        
        // check permissions
        TextChannel noperms = tcs.stream().filter(tc -> !Constants.canSendGiveaway(tc)).findFirst().orElse(null);
        if(noperms != null)
        {
            event.replyError("I cannot start a giveaway in " + noperms.getAsMention() + "; please make sure I have the following permissions:\n\n"+Constants.PERMS);
            return;
        }
        
        // check channel type
        TextChannel isnews = tcs.stream().filter(tc -> tc.isNews()).findFirst().orElse(null);
        if(isnews != null)
        {
            event.replyError("Giveaways cannot be created in announcements channels (" + isnews.getAsMention() + ")!");
            return;
        }
        
        // check for arguments
        if(event.getArgs().isEmpty())
        {
            event.replyError("Please include a length of time, and optionally a number of winners and a prize!"+EXAMPLE);
            return;
        }
        
        // parse and check length of time
        String[] parts = args.trim().split("\\s+", 2);
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
        
        // check for too many giveaways runnning
        List<Giveaway> existing = level.perChannelMaxGiveaways 
                ? bot.getDatabase().giveaways.getGiveaways(tcs.get(0)) 
                : bot.getDatabase().giveaways.getGiveaways(event.getGuild());
        if(existing == null)
        {
            event.replyError("An error occurred when trying to start giveaway.");
            return;
        }
        else if(existing.size() >= level.maxGiveaways)
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
        bot.startGiveaway(tcs.get(0), tcs.subList(1, tcs.size()), event.getAuthor(), now, seconds, winners, item);
    }
}
