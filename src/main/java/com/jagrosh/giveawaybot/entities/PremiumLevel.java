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
        NONE   (0, "None",          60*60*24*7*2, 20, 20, false, 0L),
        BOOST  (1, "Nitro Booster", 60*60*24*7*5, 30, 25, true,  585981877396045826L),
        PATRON (2, "Patron",        60*60*24*7*5, 30, 25, true,  585689274565918721L),
        DONATOR(3, "Donator",       60*60*24*7*5, 30, 25, true,  585708901270421504L),
        DISCORD(4, "Discord",       60*60*24*7*5, 50, 25, true,  778420722673778748L);
        
        public final int level;
        public final String name;
        public final int maxTime, maxWinners, maxGiveaways;
        public final boolean perChannelMaxGiveaways;
        public final long roleId;
        
        private PremiumLevel(int level, String name, int maxTime, int maxWinners, 
                int maxGiveaways, boolean perChannelMaxGiveaways, long roleId)
        {
            this.level = level;
            this.name = name;
            this.maxTime = maxTime;
            this.maxWinners = maxWinners;
            this.maxGiveaways = maxGiveaways;
            this.perChannelMaxGiveaways = perChannelMaxGiveaways;
            this.roleId = roleId;
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
