/*
 * Copyright 2019 John Grosh (john.a.grosh@gmail.com).
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
import com.jagrosh.giveawaybot.entities.PremiumLevel;
import com.jagrosh.jdautilities.command.Command;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public abstract class GiveawayCommand extends Command
{
    protected final Bot bot;
    protected boolean needsPremium = false;
    
    protected GiveawayCommand(Bot bot)
    {
        this.bot = bot;
        this.guildOnly = true;
        this.category = new Command.Category("Giveaway", event -> 
        {
            if(!event.isFromType(ChannelType.TEXT))
            {
                event.replyError("This command cannot be used in Direct Messages!");
                return false;
            }
            if(needsPremium)
            {
                PremiumLevel level = bot.getDatabase().premium.getPremiumLevel(event.getGuild());
                if(level == null || level == PremiumLevel.NONE)
                {
                    event.replyError("This server must have a premium level to use this command!");
                    return false;
                }
            }
            if(event.getMember().hasPermission(Permission.MANAGE_SERVER) || 
               event.getMember().getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("giveaways")))
                return true;
            event.replyError("You must have the Manage Server permission, or a role called \"Giveaways\", to use this command!");
            return false;
        });
    }
}
