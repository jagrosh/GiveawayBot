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

import com.neovisionaries.ws.client.OpeningHandshakeException;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.utils.SessionControllerAdapter;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class BlockingSessionController extends SessionControllerAdapter
{
    private final long MIN_DELAY = 10000L; // 10 seconds
    private final long MAX_DELAY = 40000L; // 40 seconds
    
    @Override
    protected void runWorker()
    {
        synchronized (lock)
        {
            if (workerHandle == null)
            {
                workerHandle = new BlockingQueueWorker(MIN_DELAY);
                workerHandle.start();
            }
        }
    }
    
    protected class BlockingQueueWorker extends QueueWorker
    {
        protected BlockingQueueWorker(long min)
        {
            super(min);
        }
        
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
                log.error("Unable to backoff", ex);
            }
            processQueue();
            synchronized (lock)
            {
                workerHandle = null;
                if (!connectQueue.isEmpty())
                    runWorker();
            }
        }
        
        @Override
        protected void processQueue()
        {
            boolean isMultiple = connectQueue.size() > 1;
            while (!connectQueue.isEmpty())
            {
                SessionConnectNode node = connectQueue.poll();
                try
                {
                    log.info("Attempting to start node: {}", node.getShardInfo().getShardId());
                    node.run(isMultiple && connectQueue.isEmpty());
                    isMultiple = true;
                    lastConnect = System.currentTimeMillis();
                    if (connectQueue.isEmpty())
                        break;
                    
                    // block until we're fully loaded
                    long total = 0;
                    while(node.getJDA().getStatus() != JDA.Status.CONNECTED 
                            && node.getJDA().getStatus() != JDA.Status.SHUTDOWN 
                            && total < MAX_DELAY)
                    {
                        total += 100;
                        Thread.sleep(100);
                    }
                    
                    if (this.delay > 0)
                        Thread.sleep(this.delay);
                    log.info("Finished with delay of {}", System.currentTimeMillis() - lastConnect);
                }
                catch (IllegalStateException e)
                {
                    Throwable t = e.getCause();
                    if (t instanceof OpeningHandshakeException)
                        log.error("Failed opening handshake, appending to queue. Message: {}", e.getMessage());
                    else
                        log.error("Failed to establish connection for a node, appending to queue", e);
                    appendSession(node);
                }
                catch (InterruptedException e)
                {
                    log.error("Failed to run node", e);
                    appendSession(node);
                    return; // caller should start a new thread
                }
            }
        }
    }
}
