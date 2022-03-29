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
package com.jagrosh.giveawaybot.data;

import com.jagrosh.giveawaybot.entities.PremiumLevel;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
@Entity
@Table(name = "PREMIUM")
public class PremiumStatus
{
    @Id
    @Column(name = "USER_ID")
    private long userId;
    
    @Column(name = "LEVEL")
    private int premiumLevel;

    public long getUserId()
    {
        return userId;
    }

    public void setUserId(long userId)
    {
        this.userId = userId;
    }

    public int getPremiumLevel()
    {
        return premiumLevel;
    }

    public void setPremiumLevel(int premiumLevel)
    {
        this.premiumLevel = premiumLevel;
    }
    
    public PremiumLevel getPremium()
    {
        return PremiumLevel.get(premiumLevel);
    }
    
    public void setPremium(PremiumLevel level)
    {
        this.premiumLevel = level.level;
    }
}
