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

import com.jagrosh.giveawaybot.database.Database;
import com.jagrosh.giveawaybot.entities.Giveaway;
import com.jagrosh.giveawaybot.entities.Status;
import com.jagrosh.giveawaybot.rest.RestJDA;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Updater {
    
    /**
     * Runs the application as a giveaway updater
     * @throws Exception 
     */
    public static void main() throws Exception
    {
        LoggerFactory.getLogger("Updater").info("Updater starting.");
        
        Config config = ConfigFactory.load();
        
        // connects to the database
        Database database = new Database(config.getString("database.host"), 
                                       config.getString("database.username"), 
                                       config.getString("database.password"));
        
        // migrate the old giveaways if the file exists
        //migrateGiveaways(database);
        
        // end any giveaways that didn't get deleted before last restart
        database.giveaways.getGiveaways(Status.ENDING).forEach(giveaway -> database.giveaways.setStatus(giveaway.messageId, Status.ENDNOW));
        
        // make a 'JDA' rest client
        RestJDA restJDA = new RestJDA(config.getString("bot-token"));
        
        // make a schedule to run the update loop and a pool for ending giveaways
        ScheduledExecutorService schedule = Executors.newScheduledThreadPool(2);
        ExecutorService pool = Executors.newFixedThreadPool(15);
        
        // create an index to track time
        AtomicLong index = new AtomicLong(1);
        
        // main updating loop
        schedule.scheduleWithFixedDelay(() -> 
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
                    giveaway.end(restJDA);
                    database.giveaways.deleteGiveaway(giveaway.messageId);
                });
            });
            
            // end giveaways that have run out of time
            database.giveaways.getGiveawaysEndingBefore(now.plusMillis(1900)).forEach(giveaway -> 
            {
                database.giveaways.setStatus(giveaway.messageId, Status.ENDING);
                pool.submit(() -> 
                {
                    giveaway.end(restJDA);
                    database.giveaways.deleteGiveaway(giveaway.messageId);
                });
            });
            
            if(current%120==0)
            {
                // update giveaways within 1 hour of ending
                database.giveaways.getGiveawaysEndingBefore(now.plusSeconds(60*60)).forEach(giveaway -> giveaway.update(restJDA, database, now));
            }
            else if(current%15==0)
            {
                // update giveaways within 3 minutes of ending
                database.giveaways.getGiveawaysEndingBefore(now.plusSeconds(3*60)).forEach(giveaway -> giveaway.update(restJDA, database, now));
            }
            else
            {
                // update giveaways within 5 seconds of ending
                database.giveaways.getGiveawaysEndingBefore(now.plusSeconds(5)).forEach(giveaway -> giveaway.update(restJDA, database, now));
            }
        }, 0, 1, TimeUnit.SECONDS);
        
        // secondary update loop that updates all giveaways
        schedule.scheduleWithFixedDelay(() -> 
        {
            for(Giveaway giveaway: database.giveaways.getGiveaways(Status.RUN))
            {
                if(Instant.now().until(giveaway.end, ChronoUnit.MINUTES)>60)
                {
                    giveaway.update(restJDA, database, Instant.now(), false);
                    try{Thread.sleep(100);}catch(Exception ignore){} // stop hitting global ratelimits...
                }
            }
        }, 1, 1, TimeUnit.MINUTES);
    }
}
