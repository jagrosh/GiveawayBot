/*
 * Copyright 2019 John Grosh (john.a.grosh@gmail.com).
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

import com.jagrosh.giveawaybot.Constants;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public enum PremiumLevel
    {
        NONE   (0, 60*60*24*7*2, 20, 20, false),
        BOOST  (1, 60*60*24*7*4, 30, 25, true),
        PATRON (2, 60*60*24*7*4, 30, 25, true),
        DONATOR(3, 60*60*24*7*4, 30, 25, true);
        
        public final int level;
        public final int maxTime, maxWinners, maxGiveaways;
        public final boolean perChannelMaxGiveaways;
        
        private PremiumLevel(int level, int maxTime, int maxWinners, int maxGiveaways, boolean perChannelMaxGiveaways)
        {
            this.level = level;
            this.maxTime = maxTime;
            this.maxWinners = maxWinners;
            this.maxGiveaways = maxGiveaways;
            this.perChannelMaxGiveaways = perChannelMaxGiveaways;
        }
        
        public boolean isValidTime(int seconds)
        {
            return seconds >= Constants.MIN_TIME && seconds <= maxTime;
        }
        
        public boolean isValidWinners(int winners)
        {
            return winners >= 1 && winners <= maxWinners;
        }
        
        // static methods
        public static PremiumLevel get(int level)
        {
            for(PremiumLevel p: values())
                if(p.level == level)
                    return p;
            return NONE;
        }
    }
