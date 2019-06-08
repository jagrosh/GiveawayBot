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
package com.jagrosh.giveawaybot.database.managers;

import com.jagrosh.easysql.DataManager;
import com.jagrosh.easysql.DatabaseConnector;
import com.jagrosh.easysql.SQLColumn;
import com.jagrosh.easysql.columns.*;
import com.jagrosh.giveawaybot.entities.PremiumLevel;
import net.dv8tion.jda.core.entities.Guild;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class PremiumManager extends DataManager
{
    public final static SQLColumn<Long> USER_ID = new LongColumn("USER_ID", false, 0L, true);
    public final static SQLColumn<Integer> PREMIUM_LEVEL = new IntegerColumn("LEVEL", false, PremiumLevel.NONE.level);
    
    public PremiumManager(DatabaseConnector connector)
    {
        super(connector, "PREMIUM");
    }
    
    public PremiumLevel getPremiumLevel(Guild guild)
    {
        if(guild==null || guild.getOwner()==null)
            return PremiumLevel.NONE;
        return read(selectAll(USER_ID.is(guild.getOwnerIdLong())), rs -> rs.next() 
                ? PremiumLevel.get(PREMIUM_LEVEL.getValue(rs)) 
                : PremiumLevel.NONE, PremiumLevel.NONE);
    }
    
    public void updatePremiumLevels()
    {
        readWrite(selectAll(), rs -> 
        {
            
        });
    }
}
