/*
 * Copyright 2022 John Grosh (john.a.grosh@gmail.com).
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

import com.jagrosh.giveawaybot.GiveawayBot;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Uptimer
{
    private final ScheduledExecutorService schedule = Executors.newSingleThreadScheduledExecutor();
    private final GiveawayBot bot;
    
    private boolean started = false;
    
    public Uptimer(GiveawayBot bot)
    {
        this.bot = bot;
    }
    
    public void start()
    {
        if(started)
            return;
        started = true;
        schedule.scheduleWithFixedDelay(() -> check(), 300, 30, TimeUnit.SECONDS);
    }
    
    public void shutdown()
    {
        schedule.shutdown();
    }
    
    private void check()
    {
        double req = bot.getInteractionsClient().getMetrics().getOrDefault("TotalTime", 0L) / bot.getInteractionsClient().getMetrics().getOrDefault("TotalRequests", 1L) * 1e-9;
        if(req > 1.5)
            bot.shutdown("AUTOMATIC: avg req = " + req);
    }
}
