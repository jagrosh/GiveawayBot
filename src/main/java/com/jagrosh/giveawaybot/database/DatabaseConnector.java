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
package com.jagrosh.giveawaybot.database;

import com.jagrosh.giveawaybot.database.managers.*;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import net.dv8tion.jda.core.utils.SimpleLog;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class DatabaseConnector {
    
    private final Connection connection;
    public final static SimpleLog LOG = SimpleLog.getLog("SQL");
    
    public final GiveawayManager giveaways;
    public final GuildSettingsManager settings;
    
    public DatabaseConnector (String host, String user, String pass) throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        Class.forName("org.h2.Driver").newInstance();
        connection = DriverManager.getConnection("jdbc:h2:"+host, user, pass);
        LOG.info("Connected to database!");
        
        this.giveaways = new GiveawayManager(this);
        this.settings = new GuildSettingsManager(this);
        
        try {
            for(Field field: this.getClass().getFields())
            {
                if(field.get(this).getClass().getSuperclass() == DataManager.class)
                {
                    DataManager manager = DataManager.class.cast(field.get(this));
                    if(!connection.getMetaData().getTables(null, null, manager.getTableName(), null).next())
                    {
                        LOG.info("Creating table: "+manager.getTableName());
                        Statement s = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        s.closeOnCompletion();
                        String str = manager.getColumns()[0].name+" "+manager.getColumns()[0].getDataDescription();
                        for(int i=1; i<manager.getColumns().length; i++)
                            str+=", "+manager.getColumns()[i].name+" "+manager.getColumns()[i].getDataDescription();
                        s.execute("CREATE TABLE "+manager.getTableName()+"("+str+")");
                    }
                    for(SQLColumn col: manager.getColumns())
                    {
                        if(!connection.getMetaData().getColumns(null, null, manager.getTableName(), col.name).next())
                        {
                            LOG.info("Creating column '"+col.name+"` in "+manager.getTableName());
                            Statement st = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                            st.closeOnCompletion();
                            st.execute("ALTER TABLE "+manager.getTableName()+" ADD "+col.name+" "+col.getDataDescription());
                        }
                    }
                }
            }
        } catch( SQLException | IllegalAccessException e) {
            LOG.fatal(e);
        }
    }
    
    public final Connection getConnection()
    {
        return connection;
    }
    
    public void shutdown()
    {
        try {
            connection.close();
        } catch(SQLException ex) {
            LOG.fatal("Failed to close connection: "+ex);
        }
    }
}
