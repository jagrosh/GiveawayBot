/*
 * Copyright 2018 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.giveawaybot.util;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.utils.JDALogger;
import net.dv8tion.jda.core.utils.SessionControllerAdapter;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class BlockingSessionController extends SessionControllerAdapter
{
    private final int MAX_DELAY = 20*1000;
    
    @Override
    protected void runWorker()
    {
        synchronized (lock)
        {
            if (workerHandle == null)
            {
                workerHandle = new BlockingQueueWorker();
                workerHandle.start();
            }
        }
    }
    
    protected class BlockingQueueWorker extends QueueWorker
    {
        @Override
        public void run()
        {
            try
            {
                if (this.delay > 0)
                {
                    final long interval = System.currentTimeMillis() - lastConnect;
                    if (interval < this.delay)
                        Thread.sleep(this.delay - interval);
                }
            }
            catch (InterruptedException ex)
            {
                JDALogger.getLog(SessionControllerAdapter.class).error("Unable to backoff", ex);
            }
            while (!connectQueue.isEmpty())
            {
                SessionConnectNode node = connectQueue.poll();
                try
                {
                    node.run(connectQueue.isEmpty());
                    lastConnect = System.currentTimeMillis();
                    if (connectQueue.isEmpty())
                        break;
                    if (this.delay > 0)
                        Thread.sleep(this.delay);
                    int total = 0;
                    while(node.getJDA().getStatus() != JDA.Status.CONNECTED 
                            && node.getJDA().getStatus() != JDA.Status.SHUTDOWN 
                            && total < MAX_DELAY)
                    {
                        total += 100;
                        Thread.sleep(100);
                    }
                }
                catch (InterruptedException e)
                {
                    JDALogger.getLog(SessionControllerAdapter.class).error("Failed to run node", e);
                    appendSession(node);
                }
            }
            synchronized (lock)
            {
                workerHandle = null;
                if (!connectQueue.isEmpty())
                    runWorker();
            }
        }
    }
}
