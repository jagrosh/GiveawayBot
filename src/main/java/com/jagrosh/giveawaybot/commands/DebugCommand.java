/*
 * Copyright 2018 John Grosh (jagrosh).
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
import java.time.OffsetDateTime;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.time.temporal.ChronoUnit;
import net.dv8tion.jda.core.JDA;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class DebugCommand extends Command
{
    private final Bot bot;
    
    public DebugCommand(Bot bot)
    {
        this.bot = bot;
        this.name = "debug";
        this.help = "shows some debug stats";
        this.ownerCommand = true;
        this.guildOnly = false;
        this.hidden = true;
    }
    
    @Override
    protected void execute(CommandEvent event)
    {
        long totalMb = Runtime.getRuntime().totalMemory()/(1024*1024);
        long usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/(1024*1024);
        StringBuilder sb = new StringBuilder("**"+event.getSelfUser().getName()+"** statistics:"
                + "\nLast Startup: "+FormatUtil.secondsToTime(Constants.STARTUP.until(OffsetDateTime.now(), ChronoUnit.SECONDS))+" ago"
                + "\nGuilds: **"+bot.getShardManager().getGuildCache().size()+"**"
                + "\nMemory: **"+usedMb+"**Mb / **"+totalMb+"**Mb"
                + "\nAverage Ping: **"+bot.getShardManager().getAveragePing()+"**ms"
                + "\nShard Total: **"+bot.getShardManager().getShardsTotal()+"**"
                + "\nShard Connectivity: ```diff");
        bot.getShardManager().getShards().forEach(jda -> sb.append("\n").append(jda.getStatus()==JDA.Status.CONNECTED ? "+ " : "- ")
                .append(jda.getShardInfo().getShardId()<10 ? "0" : "").append(jda.getShardInfo().getShardId()).append(": ").append(jda.getStatus())
                .append(" ~ ").append(jda.getGuildCache().size()).append(" guilds"));
        sb.append("\n```");
        event.reply(sb.toString().trim());
    }
}
