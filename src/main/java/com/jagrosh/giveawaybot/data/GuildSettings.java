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

import com.jagrosh.giveawaybot.Constants;
import com.jagrosh.interactions.components.ButtonComponent;
import com.jagrosh.interactions.components.PartialEmoji;
import com.jagrosh.interactions.entities.WebLocale;
import java.awt.Color;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
@Entity
@Table(name = "SETTINGS")
public class GuildSettings
{
    @Id
    @Column(name = "GUILD_ID")
    private long guildId;
    @Column(name = "COLOR")
    private int colorVal;
    @Column(name = "EMOJI")
    private String emoji;
    @Column(name = "OWNER")
    private long ownerId;
    @Column(name = "LOG")
    private long logChannelId;
    @Column(name = "LOCALE")
    private String locale;
    @Column(name = "LAST_FETCH")
    private long latestRetrieval;
    
    public GuildSettings()
    {
        this(0L);
    }
    
    public GuildSettings(long guildId)
    {
        this.guildId = guildId;
        //this.managerRoleId = 0L;
        this.colorVal = Constants.BLURPLE.getRGB();
        this.emoji = null;
        this.ownerId = 0L;
        this.locale = null;
        this.latestRetrieval = 0L;
        this.logChannelId = 0L;
    }
    
    public long getGuildId()
    {
        return guildId;
    }

    public void setGuildId(long guildId)
    {
        this.guildId = guildId;
    }

    /*public long getManagerRoleId()
    {
        return managerRoleId;
    }

    public void setManagerRoleId(long managerRoleId)
    {
        this.managerRoleId = managerRoleId;
    }*/

    public int getColorVal()
    {
        return colorVal;
    }

    public void setColorVal(int color)
    {
        this.colorVal = color;
    }
    
    public Color getColor()
    {
        return new Color(getColorVal());
    }
    
    public void setColor(Color color)
    {
        setColorVal(color.getRGB());
    }

    public String getEmoji()
    {
        return emoji == null ? Constants.TADA : emoji;
    }

    public void setEmoji(String emoji)
    {
        this.emoji = emoji;
    }

    public long getOwnerId()
    {
        return ownerId;
    }

    public void setOwnerId(long ownerId)
    {
        this.ownerId = ownerId;
    }

    public long getLogChannelId() {
        return logChannelId;
    }

    public void setLogChannelId(long logChannelId) {
        this.logChannelId = logChannelId;
    }

    public WebLocale getLocale()
    {
        return WebLocale.of(locale);
    }

    public void setLocale(WebLocale locale)
    {
        this.locale = locale.getCode();
    }
    
    public Instant getLatestRetrieval()
    {
        return Instant.ofEpochSecond(latestRetrieval);
    }
    
    public void setLatestRetrieval(Instant time)
    {
        this.latestRetrieval = time.getEpochSecond();
    }
}
