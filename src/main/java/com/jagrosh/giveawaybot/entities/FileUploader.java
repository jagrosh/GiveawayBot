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

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.WebhookCluster;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class FileUploader
{
    private final WebhookCluster cluster = new WebhookCluster();
    private final AtomicInteger index = new AtomicInteger(0);
    
    public FileUploader(List<String> urls)
    {
        for(String url: urls)
            cluster.addWebhooks(new WebhookClientBuilder(url).build());
    }
    
    public String uploadFile(String contents, String filename)
    {
        int val = index.incrementAndGet();
        try
        {
            return cluster.getWebhooks()
                    .get(val % cluster.getWebhooks().size())
                    .send(contents.getBytes(), filename)
                    .get().getAttachments().get(0).getUrl();
        }
        catch(Exception ex)
        {
            return null;
        }
    }
    
    public void shutdown()
    {
        cluster.close();
    }
}
