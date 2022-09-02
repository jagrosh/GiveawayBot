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
import com.jagrosh.interactions.requests.RestClient.RestResponse;
import com.jagrosh.interactions.requests.Route;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class ServerCountUpdater
{
    private final static String API_ENDPOINT = "https://botblock.org/api/count";
    
    private final Logger log = LoggerFactory.getLogger(ServerCountUpdater.class);
    private final ScheduledExecutorService schedule = Executors.newSingleThreadScheduledExecutor();
    
    private final GiveawayBot bot;
    private final Map<String,String> tokens;
    
    private int serverCount = 0;
    
    public ServerCountUpdater(GiveawayBot bot, Map<String,String> tokens)
    {
        this.bot = bot;
        this.tokens = tokens;
    }
    
    public void start()
    {
        schedule.scheduleAtFixedRate(this::updateServerCount, 0, 60, TimeUnit.MINUTES);
    }
    
    public void shutdown()
    {
        schedule.shutdown();
    }
    
    public int getServerCount()
    {
        return serverCount;
    }
    
    public void updateServerCount()
    {
        try
        {
            // get estimated guild count
            RestResponse res = bot.getRestClient().request(Route.GET_GATEWAY.format()).get();
            int guilds = res.getBody().getInt("shards") * 1000;
            if(guilds <= 0)
                throw new IllegalArgumentException("Invalid guild count");
            serverCount = guilds;
            
            // post guild count
            JSONObject json = new JSONObject()
                    .put("server_count", guilds)
                    .put("bot_id", Long.toString(bot.getBotId()));
            tokens.forEach((key,val) -> json.put(key, val));
            RestResponse res2 = bot.getRestClient().simpleRequest(API_ENDPOINT, Route.Type.POST, json.toString()).get();
            if(res2.isSuccess() && res2.getBody().has("failure"))
                res2.getBody().getJSONObject("failure").toMap().forEach((site,val) -> log.warn(String.format("Updating server count for '%s' failed: %s", site, val)));
            else
                log.warn("Updating server counts failed: " + res2.getBody().toString());
        }
        catch(Exception ex)
        {
            log.error("Exception when updating guild count: ", ex);
        }
    }
}
