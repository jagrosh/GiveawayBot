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

import java.time.Instant;
import javax.persistence.*;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
@Entity
@Table(name = "GIVEAWAYS")
@NamedQueries({
    @NamedQuery(name = "Giveaway.countAll", query = "SELECT COUNT(g) FROM Giveaway g"),
    @NamedQuery(name = "Giveaway.countAllFromChannel", query = "SELECT COUNT(g) FROM Giveaway g WHERE g.channelId = :channelId"),
    @NamedQuery(name = "Giveaway.countAllFromGuild", query = "SELECT COUNT(g) FROM Giveaway g WHERE g.guildId = :guildId"),
    @NamedQuery(name = "Giveaway.getAllFromChannel", query = "SELECT g FROM Giveaway g WHERE g.channelId = :channelId"),
    @NamedQuery(name = "Giveaway.getAllFromGuild", query = "SELECT g FROM Giveaway g WHERE g.guildId = :guildId"),
    @NamedQuery(name = "Giveaway.getAllEndingBefore", query = "SELECT g FROM Giveaway g WHERE g.endTime < :endTime")
})
public class Giveaway
{
    @Id
    @Column(name = "MESSAGE_ID")
    private long messageId;
    
    @Column(name = "CHANNEL_ID")
    private long channelId;
    
    @Column(name = "GUILD_ID")
    private long guildId;
    
    @Column(name = "USER_ID")
    private long userId;
    
    @Column(name = "END_TIME")
    private long endTime;
    
    @Column(name = "WINNERS")
    private int winners;
    
    @Column(name = "PRIZE")
    private String prize;

    public Giveaway() {}
    
    public Giveaway(long userId, Instant endTime, int winners, String prize)
    {
        this.userId = userId;
        this.endTime = endTime.getEpochSecond();
        this.winners = winners;
        this.prize = prize;
    }
    
    public long getMessageId()
    {
        return messageId;
    }

    public void setMessageId(long messageId)
    {
        this.messageId = messageId;
    }

    public long getChannelId()
    {
        return channelId;
    }

    public void setChannelId(long channelId)
    {
        this.channelId = channelId;
    }

    public long getGuildId()
    {
        return guildId;
    }

    public void setGuildId(long guildId)
    {
        this.guildId = guildId;
    }

    public long getUserId()
    {
        return userId;
    }

    public void setUserId(long userId)
    {
        this.userId = userId;
    }

    public long getEndTime()
    {
        return endTime;
    }

    public void setEndTime(long endTime)
    {
        this.endTime = endTime;
    }
    
    public Instant getEndInstant()
    {
        return Instant.ofEpochSecond(endTime);
    }
    
    public void setEndInstant(Instant endTime)
    {
        this.endTime = endTime.getEpochSecond();
    }

    public String getPrize()
    {
        return prize;
    }

    public void setPrize(String prize)
    {
        this.prize = prize;
    }

    public int getWinners()
    {
        return winners;
    }

    public void setWinners(int winners)
    {
        this.winners = winners;
    }
    
    public String getJumpLink()
    {
        return String.format("https://discord.com/channels/%d/%d/%d", guildId, channelId, messageId);
    }
}
