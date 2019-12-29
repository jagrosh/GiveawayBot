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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

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
        if(guild==null)
            return PremiumLevel.NONE;
        return read(selectAll(USER_ID.is(guild.getOwnerIdLong())), rs -> rs.next() 
                ? PremiumLevel.get(PREMIUM_LEVEL.getValue(rs)) 
                : PremiumLevel.NONE, PremiumLevel.NONE);
    }
    
    public void updatePremiumLevels(Guild premiumGuild)
    {
        if(premiumGuild == null || !premiumGuild.isLoaded() || premiumGuild.getMemberCache().size()==0)
            return;
        // make a map of all users that have premium levels
        Map<Long, PremiumLevel> map = new HashMap<>();
        for(PremiumLevel p: PremiumLevel.values())
        {
            Role role = premiumGuild.getRoleById(p.roleId);
            if(role == null)
                continue;
            premiumGuild.getMembersWithRoles(role).forEach(m -> map.put(m.getUser().getIdLong(), p));
        }
        
        // select all existing entries
        readWrite(selectAll(), rs -> 
        {
            while(rs.next())
            {
                long userId = USER_ID.getValue(rs);
                
                // remove users that no longer have premium
                if(!map.containsKey(userId))
                    rs.deleteRow();
                
                // update users if necessary
                else if(map.get(userId).level != PREMIUM_LEVEL.getValue(rs))
                {
                    PREMIUM_LEVEL.updateValue(rs, map.get(userId).level);
                    rs.updateRow();
                }
                
                // if they are in db already, dont try to put them in again
                map.remove(userId);
            }
            
            // add new users
            for(Entry<Long, PremiumLevel> entry: map.entrySet())
            {
                rs.moveToInsertRow();
                USER_ID.updateValue(rs, entry.getKey());
                PREMIUM_LEVEL.updateValue(rs, entry.getValue().level);
                rs.insertRow();
            }
        });
    }
}
