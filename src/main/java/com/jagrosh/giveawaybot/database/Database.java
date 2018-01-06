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

import com.jagrosh.easysql.DatabaseConnector;
import com.jagrosh.giveawaybot.database.managers.*;
import java.sql.SQLException;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Database extends DatabaseConnector
{
    public final GiveawayManager giveaways;
    public final GuildSettingsManager settings;
    
    public Database (String host, String user, String pass) throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        super(host, user, pass);
        
        this.giveaways = new GiveawayManager(this);
        this.settings = new GuildSettingsManager(this);
        
        init();
    }
}
