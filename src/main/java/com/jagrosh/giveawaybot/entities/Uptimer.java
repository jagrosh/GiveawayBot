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
package com.jagrosh.giveawaybot.entities;

import com.jagrosh.giveawaybot.Bot;
import com.jagrosh.giveawaybot.util.FormatUtil;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.JDA;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public abstract class Uptimer
{
    protected final Bot bot;
    private final int delay;
    
    private boolean started = false;
    
    private Uptimer(Bot bot, int delay)
    {
        this.bot = bot;
        this.delay = delay;
    }
    
    public synchronized void start(ScheduledExecutorService threadpool)
    {
        if(started)
            return;
        started = true;
        threadpool.scheduleWithFixedDelay(() -> check(), delay, delay, TimeUnit.SECONDS);
    }
    
    protected abstract void check();
    
    public static class DatabaseUptimer extends Uptimer
    {
        private int failures = 0;
    
        public DatabaseUptimer(Bot bot)
        {
            super(bot, 60);
        }
        
        @Override
        protected void check()
        {
            if(!bot.getDatabase().databaseCheck())
            {
                failures++;
                if(failures < 3)
                    bot.getWebhookLog().send(WebhookLog.Level.ERROR, "Failed a database check (" + failures + ")!");
                else
                {
                    bot.getWebhookLog().send(WebhookLog.Level.ERROR, "Failed a database check (" + failures + ")! Restarting...");
                    System.exit(0);
                }
            }
            else
                failures = 0;
        }
    }
    
    public static class StatusUptimer extends Uptimer
    {
        private enum BotStatus { LOADING, ONLINE, PARTIAL_OUTAGE, OFFLINE }
        
        private BotStatus status = BotStatus.LOADING;
        private Instant lastChange = Instant.now();
        private boolean attemptedFix = false;
    
        public StatusUptimer(Bot bot)
        {
            super(bot, 30);
        }
        
        @Override
        protected void check()
        {
            long onlineCount = bot.getShardManager().getShardCache().stream().filter(jda -> jda.getStatus() == JDA.Status.CONNECTED).count();
            BotStatus curr = onlineCount == bot.getShardManager().getShardCache().size() ? BotStatus.ONLINE 
                    : status == BotStatus.LOADING ? BotStatus.LOADING 
                    : onlineCount == 0 ? BotStatus.OFFLINE 
                    : BotStatus.PARTIAL_OUTAGE;
            
            if(curr != status) // log if it changed
            {
                bot.getWebhookLog().send(WebhookLog.Level.INFO, "Status changed from `" + status + "` to `" + curr + "`: " 
                        + FormatUtil.formatShardStatuses(bot.getShardManager().getShards()));
                lastChange = Instant.now();
                status = curr;
                if(status == BotStatus.ONLINE)
                    attemptedFix = false; // if we're fully online, reset status of an outage
            }
            else // if it didn't change, maybe take action
            {
                if(status == BotStatus.PARTIAL_OUTAGE || status == BotStatus.OFFLINE)
                {
                    int minutes = (int) lastChange.until(Instant.now(), ChronoUnit.MINUTES);
                    if(minutes > 10 && !attemptedFix)
                    {
                        List<Integer> down = bot.getShardManager().getShardCache().stream()
                                .filter(jda -> jda.getStatus() != JDA.Status.CONNECTED)
                                .map(jda -> jda.getShardInfo().getShardId())
                                .collect(Collectors.toList());
                        bot.getWebhookLog().send(WebhookLog.Level.WARNING, "Attempting to restart some shards: `" + down + "`");
                        down.forEach(i -> bot.getShardManager().restart(i));
                        attemptedFix = true;
                    }
                    else if(minutes > 20)
                    {
                        try
                        {
                            bot.getWebhookLog().sendBlocking(WebhookLog.Level.ERROR, "Extended outage, restarting...");
                        }
                        catch(InterruptedException | ExecutionException ex) {}
                        Runtime.getRuntime().halt(0);
                    }
                }
            }
        }
    }
}
