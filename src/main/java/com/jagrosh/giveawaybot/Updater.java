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
import com.jagrosh.giveawaybot.entities.Status;
import com.jagrosh.giveawaybot.rest.RestJDA;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
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
        
        // load tokens from a file
        // 0 - bot token
        // 1 - database host
        // 2 - database username
        // 3 - database pass
        List<String> tokens = Files.readAllLines(Paths.get("updater.txt"));
        
        // connects to the database
        Database database = new Database(tokens.get(1), tokens.get(2), tokens.get(3));
        
        // migrate the old giveaways if the file exists
        //migrateGiveaways(database);
        
        // make a 'JDA' rest client
        RestJDA restJDA = new RestJDA(tokens.get(0));
        
        // make a schedule to run the update loop and a pool for ending giveaways
        ScheduledExecutorService schedule = Executors.newSingleThreadScheduledExecutor();
        ExecutorService pool = Executors.newCachedThreadPool();
        
        // create an index to track time
        AtomicLong index = new AtomicLong(0);
        
        schedule.scheduleWithFixedDelay(() -> {
            // set vars for this iteration
            long current = index.getAndIncrement();
            Instant now = Instant.now();
            
            // end giveaways with end status
            database.giveaways.getGiveaways(Status.ENDNOW).forEach(giveaway -> 
            {
                database.giveaways.deleteGiveaway(giveaway.messageId);
                pool.submit(() -> giveaway.end(restJDA));
            });
            
            // end giveaways that have run out of time
            database.giveaways.getGiveawaysEndingBefore(now.plusMillis(1900)).forEach(giveaway -> 
            {
                database.giveaways.deleteGiveaway(giveaway.messageId);
                pool.submit(() -> giveaway.end(restJDA));
            });
            
            if(current%300==0)
            {
                // update all giveaways
                database.giveaways.getGiveaways().forEach(giveaway -> giveaway.update(restJDA, database, now));
            }
            else if(current%60==0)
            {
                // update giveaways within 1 hour of ending
                database.giveaways.getGiveawaysEndingBefore(now.plusSeconds(60*60)).forEach(giveaway -> giveaway.update(restJDA, database, now));
            }
            else if(current%5==0)
            {
                // update giveaways within 3 minutes of ending
                database.giveaways.getGiveawaysEndingBefore(now.plusSeconds(3*60)).forEach(giveaway -> giveaway.update(restJDA, database, now));
            }
            else
            {
                // update giveaways within 10 seconds of ending
                database.giveaways.getGiveawaysEndingBefore(now.plusSeconds(6)).forEach(giveaway -> giveaway.update(restJDA, database, now));
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}
