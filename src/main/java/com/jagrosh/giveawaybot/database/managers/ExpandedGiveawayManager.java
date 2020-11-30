/*
 * Copyright 2020 John Grosh (john.a.grosh@gmail.com).
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
import com.jagrosh.easysql.columns.LongColumn;
import com.jagrosh.giveawaybot.database.Database;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class ExpandedGiveawayManager extends DataManager
{
    public final static SQLColumn<Long> GIVEAWAY_ID  = new LongColumn("GIVEAWAY_ID",  false, 0L);
    public final static SQLColumn<Long> CHANNEL_ID  = new LongColumn("CHANNEL_ID",  false, 0L);
    public final static SQLColumn<Long> MESSAGE_ID  = new LongColumn("MESSAGE_ID",  false, 0L, true);
    
    public ExpandedGiveawayManager(Database connector)
    {
        super(connector, "EXPANDEDGIVEAWAYS");
    }
    
    public void createExpanded(long giveawayId, Map<Long,Long> expanded)
    {
        readWrite(selectAll(GIVEAWAY_ID.is(giveawayId)), rs -> 
        {
            for(Entry<Long,Long> e :expanded.entrySet())
            {
                rs.moveToInsertRow();
                GIVEAWAY_ID.updateValue(rs, giveawayId);
                CHANNEL_ID.updateValue(rs, e.getKey());
                MESSAGE_ID.updateValue(rs, e.getValue());
                rs.insertRow();
            }
        });
    }
    
    public Map<Long,Long> getExpanded(long giveawayId)
    {
        return read(selectAll(GIVEAWAY_ID.is(giveawayId)), rs -> 
        {
            Map<Long,Long> map = new HashMap<>();
            while(rs.next())
                map.put(CHANNEL_ID.getValue(rs), MESSAGE_ID.getValue(rs));
            return map;
        });
    }
    
    public void deleteExpanded(long giveawayId)
    {
        readWrite(selectAll(GIVEAWAY_ID.is(giveawayId)), rs -> 
        {
            while(rs.next())
                rs.deleteRow();
        });
    }
}
