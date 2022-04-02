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
import com.jagrosh.interactions.interfaces.IJson;
import javax.persistence.*;
import org.json.JSONObject;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
@Entity
@Table(name = "USERS")
@NamedQuery(name = "CachedUser.findAllWithPremium", query = "SELECT u FROM CachedUser u WHERE u.premiumLevel > 0")
public class CachedUser implements IJson
{
    @Id
    @Column(name = "ID")
    private long id;
    
    @Column(name = "USERNAME")
    private String username;

    @Column(name = "DISCRIM")
    private String discriminator;

    @Column(name = "AVATAR")
    private String avatar;
    
    @Column(name = "PREMIUM")
    private int premiumLevel;
    
    public CachedUser()
    {
        this.premiumLevel = 0;
    }
    
    public long getId()
    {
        return id;
    }
    
    public void setId(long id)
    {
        this.id = id;
    }
    
    public String getUsername()
    {
        return username;
    }
    
    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public void setAvatar(String avatar)
    {
        this.avatar = avatar;
    }
    
    public String getDiscriminator()
    {
        return discriminator;
    }

    public void setDiscriminator(String discriminator)
    {
        this.discriminator = discriminator;
    }

    public PremiumLevel getPremiumLevel()
    {
        return PremiumLevel.get(premiumLevel);
    }

    public void setPremiumLevel(PremiumLevel premiumLevel)
    {
        this.premiumLevel = premiumLevel.level;
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject j = new JSONObject()
                .put("username", username)
                .put("id", Long.toString(id))
                .put("discrim", discriminator)
                .put("avatar", avatar);
        if(premiumLevel > 0)
            j.put("premium", premiumLevel);
        return j;
    }
}
