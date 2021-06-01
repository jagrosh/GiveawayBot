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
package com.jagrosh.giveawaybot;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import com.jagrosh.giveawaybot.database.Database;
import com.jagrosh.giveawaybot.entities.Giveaway;
import com.jagrosh.giveawaybot.entities.Status;
import com.jagrosh.giveawaybot.rest.RestJDA;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Updater 
{
    /**
     * Runs the application as a giveaway updater
     * @throws Exception 
     */
    public static void main() throws Exception
    {
        Logger log = LoggerFactory.getLogger("Updater");
        log.info("Updater starting.");
        
        Config config = ConfigFactory.load();
        
        // connects to the database
        Database database = new Database(config.getString("database.host"), 
                                       config.getString("database.username"), 
                                       config.getString("database.password"));
        
        WebhookClient webhook = new WebhookClientBuilder(config.getString("webhook")).build();
        
        List<Giveaway> list = database.giveaways.getGiveaways();
        webhook.send(Constants.TADA + " Starting updater... `" + (list == null ? "unknown" : list.size()) + "` giveways in database");
        
        // end any giveaways that didn't get deleted before last restart
        database.giveaways.getGiveaways(Status.ENDING).forEach(giveaway -> database.giveaways.setStatus(giveaway.messageId, Status.ENDNOW));
        
        // make a 'JDA' rest client
        RestJDA restJDA = new RestJDA(config.getString("bot-token"));
        
        // make a schedule to run the update loop and a pool for ending giveaways
        ScheduledExecutorService schedule = Executors.newScheduledThreadPool(3);
        ExecutorService pool = Executors.newFixedThreadPool(15);
        
        // create an index to track time
        AtomicLong index = new AtomicLong(1);
        
        // primary loop that ends giveaways
        schedule.scheduleWithFixedDelay(() -> 
        {
            try
            {
                // set vars for this iteration
                long current = index.getAndIncrement();
                Instant now = Instant.now();

                // end giveaways with endnow status
                database.giveaways.getGiveaways(Status.ENDNOW).forEach(giveaway -> 
                {
                    database.giveaways.setStatus(giveaway.messageId, Status.ENDING);
                    pool.submit(() -> 
                    {
                        giveaway.end(restJDA, giveaway.expanded ? database.expanded.getExpanded(giveaway.messageId) : Collections.EMPTY_MAP);
                        database.giveaways.deleteGiveaway(giveaway.messageId);
                        if(giveaway.expanded)
                            database.expanded.deleteExpanded(giveaway.messageId);
                    });
                });

                // end giveaways that have run out of time
                database.giveaways.getGiveawaysEndingBefore(now.plusMillis(1900)).forEach(giveaway -> 
                {
                    database.giveaways.setStatus(giveaway.messageId, Status.ENDING);
                    pool.submit(() -> 
                    {
                        giveaway.end(restJDA, giveaway.expanded ? database.expanded.getExpanded(giveaway.messageId) : Collections.EMPTY_MAP);
                        database.giveaways.deleteGiveaway(giveaway.messageId);
                        if(giveaway.expanded)
                            database.expanded.deleteExpanded(giveaway.messageId);
                    });
                });
            }
            catch(Exception ex)
            {
                log.error("Exception in primary update loop: ", ex);
            }
        }, 0, 1, TimeUnit.SECONDS);
        
        if(config.hasPath("end-only") && config.getBoolean("end-only"))
            webhook.send(Constants.TADA + " Updater running in end-only mode");
        else
        {
            // secondary update loop that updates upcoming giveaways more frequently
            schedule.scheduleWithFixedDelay(() -> 
            {
                try
                {
                    Instant now = Instant.now();
                    database.giveaways.getGiveawaysEndingBefore(now.plusSeconds(30)).forEach(giveaway -> giveaway.update(restJDA, database, now));
                }
                catch(Exception ex)
                {
                    log.error("Exception in secondary update loop: ", ex);
                }
            }, 1, 5, TimeUnit.SECONDS);

            // tertiary update loop that updates all giveaways
            schedule.scheduleWithFixedDelay(() -> 
            {
                try
                {
                    for(Giveaway giveaway: database.giveaways.getGiveaways(Status.RUN))
                    {
                        if(Instant.now().until(giveaway.end, ChronoUnit.MINUTES)>30)
                        {
                            giveaway.update(restJDA, database, Instant.now(), false);
                            try{Thread.sleep(100);}catch(Exception ignore){} // stop hitting global ratelimits...
                        }
                    }
                }
                catch(Exception ex)
                {
                    log.error("Exception in tertiary update loop: ", ex);
                }
            }, 1, 1, TimeUnit.MINUTES);
        }
        
        int[] dbfailures = {0};
        schedule.scheduleWithFixedDelay(()->
        {
            if(!database.databaseCheck())
            {
                dbfailures[0]++;
                if(dbfailures[0] < 3)
                    webhook.send("\uD83D\uDE31 `Updater` has failed a database check ("+dbfailures[0]+")!"); // 😱
                else
                {
                    webhook.send("\uD83D\uDE31 `Updater` has failed a database check ("+dbfailures[0]+")! Restarting..."); // 😱
                    System.exit(0);
                }
            }
            else
                dbfailures[0] = 0;
        }, 5, 5, TimeUnit.MINUTES);
    }
}
