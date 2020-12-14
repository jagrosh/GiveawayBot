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
import net.dv8tion.jda.internal.utils.tuple.Pair;

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
    
    public Summary updatePremiumLevels(Guild premiumGuild)
    {
        if(premiumGuild == null || !premiumGuild.isLoaded() || premiumGuild.getMemberCache().size()==0)
            return new Summary();
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
        return readWrite(selectAll(), rs -> 
        {
            Summary sum = new Summary();
            while(rs.next())
            {
                long userId = USER_ID.getValue(rs);
                PremiumLevel current = PremiumLevel.get(PREMIUM_LEVEL.getValue(rs));
                
                // remove users that no longer have premium
                if(!map.containsKey(userId))
                {
                    sum.removed.put(userId, current);
                    rs.deleteRow();
                }
                
                // update users if necessary
                else if(map.get(userId).level != PREMIUM_LEVEL.getValue(rs))
                {
                    sum.changed.put(userId, Pair.of(current, map.get(userId)));
                    PREMIUM_LEVEL.updateValue(rs, map.get(userId).level);
                    rs.updateRow();
                }
                
                // if they are in db already, dont try to put them in again
                map.remove(userId);
            }
            
            // add new users
            for(Entry<Long, PremiumLevel> entry: map.entrySet())
            {
                sum.added.put(entry.getKey(), entry.getValue());
                rs.moveToInsertRow();
                USER_ID.updateValue(rs, entry.getKey());
                PREMIUM_LEVEL.updateValue(rs, entry.getValue().level);
                rs.insertRow();
            }
            return sum;
        });
    }
    
    public class Summary
    {
        private final Map<Long,PremiumLevel> removed = new HashMap<>();
        private final Map<Long,Pair<PremiumLevel,PremiumLevel>> changed = new HashMap<>();
        private final Map<Long,PremiumLevel> added = new HashMap<>();
        
        public boolean isEmpty()
        {
            return removed.isEmpty() && changed.isEmpty() && added.isEmpty();
        }
        
        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder("```diff");
            added.forEach((u,l) -> sb.append("\n+ ").append(u).append(" ").append(l));
            changed.forEach((u,p) -> sb.append("\n# ").append(u).append(" ").append(p.getLeft()).append(" -> ").append(p.getRight()));
            removed.forEach((u,l) -> sb.append("\n- ").append(u).append(" ").append(l));
            return sb.append("\n```").toString();
        }
    }
}
