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

import net.dv8tion.jda.core.utils.SessionControllerAdapter;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class BlockingSessionController extends SessionControllerAdapter
{
    private final long MIN_DELAY = 10000L; // 10 seconds
    
    @Override
    protected void runWorker()
    {
        synchronized (lock)
        {
            if (workerHandle == null)
            {
                workerHandle = new QueueWorker(MIN_DELAY);
                workerHandle.start();
            }
        }
    }
}
