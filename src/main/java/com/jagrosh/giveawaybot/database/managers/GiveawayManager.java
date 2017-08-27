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

import com.jagrosh.giveawaybot.database.DataManager;
import com.jagrosh.giveawaybot.database.DatabaseConnector;
import com.jagrosh.giveawaybot.database.SQLColumn;
import com.jagrosh.giveawaybot.database.columns.*;
import com.jagrosh.giveawaybot.entities.Giveaway;
import com.jagrosh.giveawaybot.entities.Status;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
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
    
    public GiveawayManager(DatabaseConnector connector)
    {
        super(connector, "GIVEAWAYS");
    }
    
    public Giveaway getGiveaway(long messageId, long guildId)
    {
        try (Statement statement = getConnection().createStatement();
             ResultSet results = statement.executeQuery(selectAll(MESSAGE_ID.is(messageId))))
        {
            if(results.next() && GUILD_ID.getValue(results)==guildId)
                return new Giveaway(MESSAGE_ID.getValue(results), CHANNEL_ID.getValue(results), GUILD_ID.getValue(results), 
                        END_TIME.getValue(results), NUM_WINNERS.getValue(results), PRIZE.getValue(results));
        } catch( SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public List<Giveaway> getGiveaways()
    {
        List<Giveaway> list = new LinkedList<>();
        try (Statement statement = getConnection().createStatement();
             ResultSet results = statement.executeQuery(selectAll(null)))
        {
            while(results.next())
                list.add(new Giveaway(MESSAGE_ID.getValue(results), CHANNEL_ID.getValue(results), GUILD_ID.getValue(results), 
                        END_TIME.getValue(results), NUM_WINNERS.getValue(results), PRIZE.getValue(results)));
        } catch( SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public List<Giveaway> getGiveaways(TextChannel channel)
    {
        List<Giveaway> list = new LinkedList<>();
        try (Statement statement = getConnection().createStatement();
             ResultSet results = statement.executeQuery(selectAll(CHANNEL_ID.is(channel.getIdLong()))))
        {
            while(results.next())
                list.add(new Giveaway(MESSAGE_ID.getValue(results), CHANNEL_ID.getValue(results), GUILD_ID.getValue(results), 
                        END_TIME.getValue(results), NUM_WINNERS.getValue(results), PRIZE.getValue(results)));
        } catch( SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public List<Giveaway> getGiveaways(Guild guild)
    {
        List<Giveaway> list = new LinkedList<>();
        try (Statement statement = getConnection().createStatement();
             ResultSet results = statement.executeQuery(selectAll(GUILD_ID.is(guild.getIdLong()))))
        {
            while(results.next())
                list.add(new Giveaway(MESSAGE_ID.getValue(results), CHANNEL_ID.getValue(results), GUILD_ID.getValue(results), 
                        END_TIME.getValue(results), NUM_WINNERS.getValue(results), PRIZE.getValue(results)));
        } catch( SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public List<Giveaway> getGiveaways(Status status)
    {
        List<Giveaway> list = new LinkedList<>();
        try (Statement statement = getConnection().createStatement();
             ResultSet results = statement.executeQuery(selectAll(STATUS.is(status.ordinal()))))
        {
            while(results.next())
                list.add(new Giveaway(MESSAGE_ID.getValue(results), CHANNEL_ID.getValue(results), GUILD_ID.getValue(results), 
                        END_TIME.getValue(results), NUM_WINNERS.getValue(results), PRIZE.getValue(results)));
        } catch( SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public List<Giveaway> getGiveawaysEndingBefore(Instant end)
    {
        List<Giveaway> list = new LinkedList<>();
        try (Statement statement = getConnection().createStatement();
             ResultSet results = statement.executeQuery(selectAll(END_TIME.isLessThan(end.getEpochSecond()))))
        {
            while(results.next())
                list.add(new Giveaway(MESSAGE_ID.getValue(results), CHANNEL_ID.getValue(results), GUILD_ID.getValue(results), 
                        END_TIME.getValue(results), NUM_WINNERS.getValue(results), PRIZE.getValue(results)));
        } catch( SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public boolean createGiveaway(Message message, Instant end, int winners, String prize)
    {
        return createGiveaway(message.getGuild().getIdLong(), message.getTextChannel().getIdLong(), message.getIdLong(), end, winners, prize);
    }
    
    public boolean createGiveaway(long guildid, long channelid, long messageid, Instant end, int winners, String prize)
    {
        try (Statement statement = getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
             ResultSet results = statement.executeQuery(selectAll(MESSAGE_ID.is(messageid)));)
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
        } catch( SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteGiveaway(long messageId)
    {
        try (Statement statement = getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
             ResultSet results = statement.executeQuery(selectAll(MESSAGE_ID.is(messageId)));)
        {
            if(results.next())
            {
                results.deleteRow();
                return true;
            }
            else
                return true;
        } catch( SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean endGiveaway(long messageId)
    {
        try (Statement statement = getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
             ResultSet results = statement.executeQuery(selectAll(MESSAGE_ID.is(messageId)));)
        {
            if(results.next())
            {
                STATUS.updateValue(results, Status.ENDNOW.ordinal());
                results.updateRow();
                return true;
            }
            else
                return false;
        } catch( SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
