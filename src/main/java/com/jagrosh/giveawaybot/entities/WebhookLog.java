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

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class WebhookLog
{
    public enum Level
    { 
        INFO("\u2139"), WARNING("\u26A0"), ERROR("\uD83D\uDE31"); // ℹ, ⚠, 😱
        
        private final String emoji;
        
        private Level(String emoji)
        {
            this.emoji = emoji;
        }
    }
    
    private final WebhookClient client;
    private final String logname;
    
    public WebhookLog(String webhookUrl, String logname)
    {
        this.client = new WebhookClientBuilder(webhookUrl).build();
        this.logname = logname;
    }
    
    public void send(Level level, String message)
    {
        client.send(level.emoji + " `[" + logname + "]` " + message);
    }
    
    public void sendBlocking(Level level, String message) throws InterruptedException, ExecutionException
    {
        client.send(level.emoji + " `[" + logname + "]` " + message).get();
    }
}
