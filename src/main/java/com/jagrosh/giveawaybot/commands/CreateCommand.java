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
import com.jagrosh.giveawaybot.entities.MessageWaiter;
import com.jagrosh.giveawaybot.entities.PremiumLevel;
import com.jagrosh.giveawaybot.util.FormatUtil;
import com.jagrosh.giveawaybot.util.OtherUtil;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class CreateCommand extends GiveawayCommand 
{
    private final static String CANCEL = "\n\n`Giveaway creation has been cancelled.`";
    private final static String CHANNEL = "\n\n`Please type the name of a channel in this server.`";
    private final static String TIME = "\n\n`Please enter the duration of the giveaway in seconds.`\n`Alternatively, enter a duration in minutes and include an M at the end, or days and include a D.`";
    private final static String WINNERS = "\n\n`Please enter a number of winners between 1 and %d.`";
    private final static String PRIZE = "\n\n`Please enter the giveaway prize. This will also begin the giveaway.`";
    
    private final static List<String> CANCEL_WORDS = Arrays.asList("cancel", "!gcancel", "g!cancel");
    
    private final Set<Long> current;
    
    public CreateCommand(Bot bot) 
    {
        super(bot);
        this.current = new HashSet<>();
        name = "create";
        help = "creates a giveaway (interactive setup)";
    }
    
    @Override
    protected void execute(CommandEvent event) 
    {
        // ignore if there's already creation running here
        if(current.contains(event.getChannel().getIdLong()))
            return;
        
        // preliminary giveaway count check
        // we use current text channel as a basis, even though this may not be the final giveaway channel
        // this might need to be changed at some point
        if(tooManyGiveaways(event, null))
            return;
        
        // get started
        current.add(event.getChannel().getIdLong());
        event.replySuccess("Alright! Let's set up your giveaway! First, what channel do you want the giveaway in?\n"
                + "You can type `cancel` at any time to cancel creation."+CHANNEL);
        waitForChannel(event, event.getMessage().getIdLong());
    }
    
    private boolean tooManyGiveaways(CommandEvent event, TextChannel tchannel)
    {
        PremiumLevel level = bot.getDatabase().premium.getPremiumLevel(event.getGuild());
        List<Giveaway> list = level.perChannelMaxGiveaways 
                ? bot.getDatabase().giveaways.getGiveaways(tchannel==null ? event.getTextChannel() : tchannel) 
                : bot.getDatabase().giveaways.getGiveaways(event.getGuild());
        if(list == null)
        {
            event.replyError("An error occurred when trying to start giveaway." + CANCEL);
            current.remove(event.getChannel().getIdLong());
            return true;
        }
        else if(list.size() >= level.maxGiveaways)
        {
            event.replyError("There are already " + level.maxGiveaways + " giveaways running in " 
                    + (tchannel == null ? "this server" : tchannel.getAsMention()) + "!" + CANCEL);
            current.remove(event.getChannel().getIdLong());
            return true;
        }
        return false;
    }
    
    private void waitForChannel(CommandEvent event, long lastMessage)
    {
        wait(event, lastMessage, e -> 
        {
            // look for the channel, handle not found and multiple
            String query = e.getMessage().getContentRaw().replace(" ", "_");
            List<TextChannel> list = FinderUtil.findTextChannels(query, event.getGuild());
            if(list.isEmpty())
            {
                event.replyWarning("Uh oh, I couldn't find any channels called '"+query+"'! Try again!" + CHANNEL);
                waitForChannel(event, e.getMessageIdLong());
                return;
            }
            if(list.size()>1)
            {
                event.replyWarning("Oh... there are multiple channels with that name. Please be more specific!" + CHANNEL);
                waitForChannel(event, e.getMessageIdLong());
                return;
            }
            TextChannel tchan = list.get(0);
            if(tchan.isNews())
            {
                event.replyWarning("Giveaways cannot be created in announcements channels! Please try a different channel!" + CHANNEL);
                waitForChannel(event, e.getMessageIdLong());
                return;
            }

            // check perms
            if(!Constants.canSendGiveaway(tchan))
            {
                event.replyWarning("Erm, I need the following permissions to start a giveaway in " + tchan.getAsMention() 
                        + ":\n" + Constants.PERMS + "\nPlease fix this and then try again." + CANCEL);
                current.remove(event.getChannel().getIdLong());
                return;
            }

            // check giveaway count again
            if(tooManyGiveaways(event, tchan))
                return;

            // channel selection successful
            event.replySuccess("Sweet! The giveaway will be in "+tchan.getAsMention()+"! Next, how long should the giveaway last?"+TIME);
            waitForTime(event, tchan, e.getMessageIdLong());
        });
    }
    
    private void waitForTime(CommandEvent event, TextChannel tchan, long lastMessage)
    {
        wait(event, lastMessage, e -> 
        {
            // see if parseable
            int seconds = OtherUtil.parseShortTime(e.getMessage().getContentRaw());
            if(seconds==-1)
            {
                event.replyWarning("Hm. I can't seem to get a number from that. Can you try again?"+TIME);
                waitForTime(event, tchan, e.getMessageIdLong());
                return;
            }

            // check for valid time
            PremiumLevel level = bot.getDatabase().premium.getPremiumLevel(event.getGuild());
            if(!level.isValidTime(seconds))
            {
                event.replyWarning("Oh! Sorry! Giveaway time must not be shorter than " + FormatUtil.secondsToTime(Constants.MIN_TIME) 
                        + " and no longer than " + FormatUtil.secondsToTime(level.maxTime) + " Mind trying again?" + TIME);
                waitForTime(event, tchan, e.getMessageIdLong());
                return;
            }

            // valid time, continue
            event.replySuccess("Neat! This giveaway will last "+FormatUtil.secondsToTime(seconds)+"! Now, how many winners should there be?"+String.format(WINNERS, level.maxWinners));
            waitForWinners(event, level, tchan, seconds, e.getMessageIdLong());
        });
    }
    
    private void waitForWinners(CommandEvent event, PremiumLevel level, TextChannel tchan, int seconds, long lastMessage)
    {
        wait(event, lastMessage, e -> 
        {
            try 
            {
                // attempt to parse
                int num = Integer.parseInt(e.getMessage().getContentRaw().trim());

                // check value
                if(!level.isValidWinners(num))
                {
                    event.replyWarning("Hey! I can only support 1 to " + level.maxWinners + " winners!" + String.format(WINNERS, level.maxWinners));
                    waitForWinners(event, level, tchan, seconds, e.getMessageIdLong());
                }
                else
                {
                    event.replySuccess("Ok! "+num+" "
                        + FormatUtil.pluralise(num, "winner", "winners")
                        + " it is! Finally, what do you want to give away?" + PRIZE);
                    waitForPrize(event, tchan, seconds, num, e.getMessageIdLong());
                }
            } 
            catch(NumberFormatException ex) 
            {
                event.replyWarning("Uh... that doesn't look like a valid number."+String.format(WINNERS, level.maxWinners));
                waitForWinners(event, level, tchan, seconds, e.getMessageIdLong());
            }
        });
    }
    
    private void waitForPrize(CommandEvent event, TextChannel tchan, int seconds, int winners, long lastMessage)
    {
        wait(event, lastMessage, e -> 
        {
            String prize = e.getMessage().getContentRaw();
            if(prize.length()>250)
            {
                event.replyWarning("Ack! That prize is too long. Can you shorten it a bit?"+PRIZE);
                waitForPrize(event, tchan, seconds, winners, e.getMessageIdLong());
                return;
            }

            // no more waiting
            current.remove(event.getChannel().getIdLong());

            // final checks
            if(tooManyGiveaways(event, tchan))
                return;
            PremiumLevel level = bot.getDatabase().premium.getPremiumLevel(event.getGuild());
            if(!level.isValidTime(seconds) || !level.isValidWinners(winners))
            {
                event.replyError("An invalid amount of time or number of winners was chosen." + CANCEL);
                return;
            }

            Instant now = Instant.now();
            if(bot.startGiveaway(tchan, event.getAuthor(), now, seconds, winners, prize))
            {
                event.replySuccess("Done! The giveaway for the `"+e.getMessage().getContentRaw()+"` is starting in "+tchan.getAsMention()+"!");
            }
            else
            {
                event.replyError("Uh oh. Something went wrong and I wasn't able to start the giveaway."+CANCEL);
            }
        });
    }
    
    private void wait(CommandEvent event, long lastMessage, Consumer<GuildMessageReceivedEvent> action)
    {
        getWaiter(event).waitForGuildMessageReceived(
                e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel()) && e.getMessageIdLong() != lastMessage, 
                e -> 
                {
                    // manual cancel
                    if(CANCEL_WORDS.contains(e.getMessage().getContentRaw().toLowerCase()))
                    {
                        event.replyWarning("Alright, I guess we're not having a giveaway after all..."+CANCEL);
                        current.remove(event.getChannel().getIdLong());
                        return;
                    }
                    // run action
                    action.accept(e);
                }, 2, TimeUnit.MINUTES, new Timeout(event));
    }
    
    private MessageWaiter getWaiter(CommandEvent event)
    {
        for(Object ev: event.getJDA().getEventManager().getRegisteredListeners())
            if(ev instanceof MessageWaiter)
                return (MessageWaiter) ev;
        return null;
    }
    
    private class Timeout implements Runnable
    {
        private final CommandEvent event;
        private boolean ran = false;
        
        private Timeout(CommandEvent event)
        {
            this.event = event;
        }
        
        @Override
        public void run() 
        {
            if(ran)
                return;
            ran = true;
            event.replyWarning("Uh oh! You took longer than 2 minutes to respond, "+event.getAuthor().getAsMention()+"!"+CANCEL);
            current.remove(event.getChannel().getIdLong());
        }
    }
}
