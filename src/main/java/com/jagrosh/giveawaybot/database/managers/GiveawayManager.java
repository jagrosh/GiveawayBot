/*
 * Copyright 2017 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.giveawaybot.database.managers;

import com.jagrosh.easysql.DataManager;
import com.jagrosh.easysql.SQLColumn;
import com.jagrosh.easysql.columns.*;
import com.jagrosh.giveawaybot.database.Database;
import com.jagrosh.giveawaybot.entities.Giveaway;
import com.jagrosh.giveawaybot.entities.Status;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class GiveawayManager extends DataManager
{
    public final static SQLColumn<Long>    GUILD_ID    = new LongColumn   ("GUILD_ID",    false, 0L);
    public final static SQLColumn<Long>    CHANNEL_ID  = new LongColumn   ("CHANNEL_ID",  false, 0L);
    public final static SQLColumn<Long>    MESSAGE_ID  = new LongColumn   ("MESSAGE_ID",  false, 0L, true);
    public final static SQLColumn<Instant> END_TIME    = new InstantColumn("END_TIME",    false, Instant.MIN);
    public final static SQLColumn<Integer> NUM_WINNERS = new IntegerColumn("NUM_WINNERS", false, 1);
    public final static SQLColumn<String>  PRIZE       = new StringColumn ("PRIZE",       true,  null, 250);
    public final static SQLColumn<Integer> STATUS      = new IntegerColumn("STATUS",      false, Status.RUN.ordinal());
    
    public GiveawayManager(Database connector)
    {
        super(connector, "GIVEAWAYS");
    }
    
    public Giveaway getGiveaway(long messageId, long guildId)
    {
        return read(selectAll(MESSAGE_ID.is(messageId)), results -> 
        {
            if(results.next() && GUILD_ID.getValue(results)==guildId)
                return giveaway(results);
            return null;
        });
    }
    
    public List<Giveaway> getGiveaways()
    {
        return getGiveaways(selectAll());
    }
    
    public List<Giveaway> getGiveaways(TextChannel channel)
    {
        return getGiveaways(selectAll(CHANNEL_ID.is(channel.getIdLong())));
    }
    
    public List<Giveaway> getGiveaways(Guild guild)
    {
        return getGiveaways(selectAll(GUILD_ID.is(guild.getIdLong())));
    }
    
    public List<Giveaway> getGiveaways(Status status)
    {
        return getGiveaways(selectAll(STATUS.is(status.ordinal())));
    }
    
    public List<Giveaway> getGiveawaysEndingBefore(Instant end)
    {
        return getGiveaways(selectAll(END_TIME.isLessThan(end.getEpochSecond())));
    }
    
    private List<Giveaway> getGiveaways(String selection)
    {
        return read(selection, results -> 
        {
            List<Giveaway> list = new LinkedList<>();
            while(results.next())
                list.add(giveaway(results));
            return list;
        }, Collections.EMPTY_LIST);
    }
    
    public boolean createGiveaway(Message message, Instant end, int winners, String prize)
    {
        return createGiveaway(message.getGuild().getIdLong(), message.getTextChannel().getIdLong(), message.getIdLong(), end, winners, prize);
    }
    
    public boolean createGiveaway(long guildid, long channelid, long messageid, Instant end, int winners, String prize)
    {
        return readWrite(selectAll(MESSAGE_ID.is(messageid)), results -> 
        {
            if(results.next())
            {
                GUILD_ID.updateValue(results, guildid);
                CHANNEL_ID.updateValue(results, channelid);
                MESSAGE_ID.updateValue(results, messageid);
                END_TIME.updateValue(results, end);
                NUM_WINNERS.updateValue(results, winners);
                PRIZE.updateValue(results, prize);
                STATUS.updateValue(results, Status.INIT.ordinal());
                results.updateRow();
                return true;
            }
            else
            {
                results.moveToInsertRow();
                GUILD_ID.updateValue(results, guildid);
                CHANNEL_ID.updateValue(results, channelid);
                MESSAGE_ID.updateValue(results, messageid);
                END_TIME.updateValue(results, end);
                NUM_WINNERS.updateValue(results, winners);
                PRIZE.updateValue(results, prize);
                STATUS.updateValue(results, Status.INIT.ordinal());
                results.insertRow();
                return true;
            }
        }, false);
    }
    
    public boolean deleteGiveaway(long messageId)
    {
        return readWrite(selectAll(MESSAGE_ID.is(messageId)), results -> 
        {
            if(results.next())
            {
                results.deleteRow();
                return true;
            }
            else
                return true;
        }, false);
    }
    
    public boolean endGiveaway(long messageId)
    {
        return readWrite(selectAll(MESSAGE_ID.is(messageId)), results -> 
        {
            if(results.next())
            {
                STATUS.updateValue(results, Status.ENDNOW.ordinal());
                results.updateRow();
                return true;
            }
            else
                return false;
        }, false);
    }
    
    private static Giveaway giveaway(ResultSet results) throws SQLException
    {
        return new Giveaway(MESSAGE_ID.getValue(results), CHANNEL_ID.getValue(results), GUILD_ID.getValue(results), 
                        END_TIME.getValue(results), NUM_WINNERS.getValue(results), PRIZE.getValue(results));
    }
}
