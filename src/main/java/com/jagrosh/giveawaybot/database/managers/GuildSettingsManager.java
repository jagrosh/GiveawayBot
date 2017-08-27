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

import com.jagrosh.giveawaybot.Constants;
import com.jagrosh.giveawaybot.database.DataManager;
import com.jagrosh.giveawaybot.database.DatabaseConnector;
import com.jagrosh.giveawaybot.database.SQLColumn;
import com.jagrosh.giveawaybot.database.columns.*;
import java.awt.Color;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import net.dv8tion.jda.core.entities.Guild;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class GuildSettingsManager extends DataManager {
    
    public final static SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID", false, 0, true);
    public final static SQLColumn<Integer> COLOR = new IntegerColumn("COLOR", false, Constants.BLURPLE.getRGB());
    
    public GuildSettingsManager(DatabaseConnector connector)
    {
        super(connector, "GUILD_SETTINGS");
    }
    
    public void updateColor(Guild guild)
    {
        int color = guild.getSelfMember().getColor()==null ? Constants.BLURPLE.getRGB() : guild.getSelfMember().getColor().getRGB();
        try (Statement statement = getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
             ResultSet results = statement.executeQuery(selectAll(GUILD_ID.is(guild.getIdLong())));)
        {
            if(results.next())
            {
                COLOR.updateValue(results, color);
                results.updateRow();
            }
            else
            {
                results.moveToInsertRow();
                GUILD_ID.updateValue(results, guild.getIdLong());
                COLOR.updateValue(results, color);
                results.insertRow();
            }
        } catch( SQLException e) {
            e.printStackTrace();
        }
    }
    
    public GuildSettings getSettings(long guildid)
    {
        try (Statement statement = getConnection().createStatement();
             ResultSet results = statement.executeQuery(selectAll(GUILD_ID.is(guildid)));)
        {
            if(results.next())
                return new GuildSettings(COLOR.getValue(results));
            else
                return new GuildSettings();
        } catch( SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public class GuildSettings {
        public final Color color;
        
        private GuildSettings()
        {
            this(Constants.BLURPLE.getRGB());
        }
        
        private GuildSettings(int color)
        {
            this.color = new Color(color);
        }
    }
}
