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
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class CreateCommand extends Command {

    private final static String CANCEL = "\n\n`Giveaway creation has been cancelled.`";
    private final static String CHANNEL = "\n\n`Please type the name of a channel in this server.`";
    private final static String TIME = "\n\n`Please enter the duration of the giveaway in seconds.`\n`Alternatively, enter a duration in minutes and include an M at the end, or days and include a D.`";
    private final static String WINNERS = "\n\n`Please enter a number of winners between 1 and 15.`";
    private final static String PRIZE = "\n\n`Please enter the giveaway prize. This will also begin the giveaway.`";
    private final Bot bot;
    private final EventWaiter waiter;
    public CreateCommand(Bot bot, EventWaiter waiter) {
        this.bot = bot;
        this.waiter = waiter;
        name = "create";
        help = "creates a giveaway (interactive setup)";
        category = Constants.GIVEAWAY;
        guildOnly = true;
        //botPermissions = new Permission[]{Permission.MESSAGE_HISTORY,Permission.MESSAGE_ADD_REACTION,Permission.MESSAGE_EMBED_LINKS};
    }
    
    @Override
    protected void execute(CommandEvent event) {
        if(bot.getDatabase().giveaways.getGiveaways(event.getGuild()).size() >= Constants.MAX_GIVEAWAYS)
        {
            event.replyError("There are already "+Constants.MAX_GIVEAWAYS+" running on this server!");
            return;
        }
        event.replySuccess("Alright! Let's set up your giveaway! First, what channel do you want the giveaway in?\n"
                + "You can type `cancel` at any time to cancel creation."+CHANNEL);
        waitForChannel(event);
    }
    
    private void waitForChannel(CommandEvent event)
    {
        waiter.waitForEvent(GuildMessageReceivedEvent.class, 
                e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel()), 
                e -> {
                    if(e.getMessage().getContentRaw().equalsIgnoreCase("cancel"))
                    {
                        event.replyWarning("Alright, I guess we're not having a giveaway after all..."+CANCEL);
                    }
                    else
                    {
                        String query = e.getMessage().getContentRaw().replace(" ", "_");
                        List<TextChannel> list = FinderUtil.findTextChannels(query, event.getGuild());
                        if(list.isEmpty())
                        {
                            event.replyWarning("Uh oh, I couldn't find any channels called '"+query+"'! Try again!"+CHANNEL);
                            waitForChannel(event);
                        }
                        else if(list.size()>1)
                        {
                            event.replyWarning("Oh... there are multiple channels with that name. Please be more specific!"+CHANNEL);
                            waitForChannel(event);
                        }
                        else
                        {
                            TextChannel tchan = list.get(0);
                            if(!Constants.canSendGiveaway(tchan))
                            {
                                event.replyWarning("Erm, I need the following permissions to start a giveaway in "+tchan.getAsMention()+":\n"+Constants.PERMS+"\nPlease fix this and then try again."+CANCEL);
                            }
                            else
                            {
                                event.replySuccess("Sweet! The giveaway will be in "+tchan.getAsMention()+"! Next, how long should the giveaway last?"+TIME);
                                waitForTime(event, tchan);
                            }
                        }
                    }
                }, 
                2, TimeUnit.MINUTES, () -> event.replyWarning("Uh oh! You took longer than 2 minutes to respond, "+event.getAuthor().getAsMention()+"!"+CANCEL));
    }
    
    private void waitForTime(CommandEvent event, TextChannel tchan)
    {
        waiter.waitForEvent(GuildMessageReceivedEvent.class, 
                e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel()), 
                e -> {
                    if(e.getMessage().getContentRaw().equalsIgnoreCase("cancel"))
                    {
                        event.replyWarning("Alright, I guess we're not having a giveaway after all..."+CANCEL);
                    }
                    else
                    {
                        int seconds = OtherUtil.parseShortTime(e.getMessage().getContentRaw());
                        if(seconds==-1)
                        {
                            event.replyWarning("Hm. I can't seem to get a number from that. Can you try again?"+TIME);
                            waitForTime(event, tchan);
                        }
                        else if(!OtherUtil.validTime(seconds))
                        {
                            event.replyWarning("Oh! Sorry! "+Constants.TIME_MSG+" Mind trying again?"+TIME);
                            waitForTime(event, tchan);
                        }
                        else
                        {
                            event.replySuccess("Neat! This giveaway will last "+FormatUtil.secondsToTime(seconds)+"! Now, how many winners should there be?"+WINNERS);
                            waitForWinners(event, tchan, seconds);
                        }
                    }
                }, 
                2, TimeUnit.MINUTES, () -> event.replyWarning("Uh oh! You took longer than 2 minutes to respond, "+event.getAuthor().getAsMention()+"!"+CANCEL));
    }
    
    private void waitForWinners(CommandEvent event, TextChannel tchan, int seconds)
    {
        waiter.waitForEvent(GuildMessageReceivedEvent.class, 
                e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel()), 
                e -> {
                    if(e.getMessage().getContentRaw().equalsIgnoreCase("cancel"))
                    {
                        event.replyWarning("Alright, I guess we're not having a giveaway after all..."+CANCEL);
                    }
                    else
                    {
                        try {
                            int num = Integer.parseInt(e.getMessage().getContentRaw().trim());
                            if(num<1 || num>15)
                            {
                                event.replyWarning("Hey! I can only support 1 to 15 winners!"+WINNERS);
                                waitForWinners(event, tchan, seconds);
                            }
                            else
                            {
                                event.replySuccess("Ok! "+num+" winners it is! Finally, what do you want to give away?"+PRIZE);
                                waitForPrize(event, tchan, seconds, num);
                            }
                        } catch(NumberFormatException ex) {
                            event.replyWarning("Uh... that doesn't look like a valid number."+WINNERS);
                            waitForWinners(event, tchan, seconds);
                        }
                    }
                }, 
                2, TimeUnit.MINUTES, () -> event.replyWarning("Uh oh! You took longer than 2 minutes to respond, "+event.getAuthor().getAsMention()+"!"+CANCEL));
    }
    
    private void waitForPrize(CommandEvent event, TextChannel tchan, int seconds, int winners)
    {
        waiter.waitForEvent(GuildMessageReceivedEvent.class, 
                e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel()), 
                e -> {
                    if(e.getMessage().getContentRaw().equalsIgnoreCase("cancel"))
                    {
                        event.replyWarning("Alright, I guess we're not having a giveaway after all..."+CANCEL);
                    }
                    else
                    {
                        String prize = e.getMessage().getContentRaw();
                        if(prize.length()>250)
                        {
                            event.replyWarning("Ack! That prize is too long. Can you shorten it a bit?"+PRIZE);
                            waitForPrize(event, tchan, seconds, winners);
                        }
                        else
                        {
                            if(bot.getDatabase().giveaways.getGiveaways(event.getGuild()).size() >= Constants.MAX_GIVEAWAYS)
                            {
                                event.replyError("There are already "+Constants.MAX_GIVEAWAYS+" running on this server!");
                                return;
                            }
                            Instant now = Instant.now();
                            if(bot.startGiveaway(tchan, now, seconds, winners, prize))
                            {
                                event.replySuccess("Done! The giveaway for the `"+e.getMessage().getContentRaw()+"` is starting in "+tchan.getAsMention()+"!");
                            }
                            else
                            {
                                event.replyError("Uh oh. Something went wrong and I wasn't able to start the giveaway."+CANCEL);
                            }
                        }
                    }
                }, 
                2, TimeUnit.MINUTES, () -> event.replyWarning("Uh oh! You took longer than 2 minutes to respond, "+event.getAuthor().getAsMention()+"!"+CANCEL));
    }
}
