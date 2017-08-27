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
package com.jagrosh.giveawaybot.database.columns;

import java.sql.ResultSet;
import java.sql.SQLException;
import com.jagrosh.giveawaybot.database.SQLColumn;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class BooleanColumn extends SQLColumn<Boolean>
{
    
    public BooleanColumn(String name, boolean nullable, boolean defaultValue)
    {
        super(name, nullable, defaultValue);
    }
    
    @Override
    public String getDataDescription()
    {
        return "BOOLEAN" + (defaultValue==null ? "" : " DEFAULT "+defaultValue.toString().toUpperCase()) + nullable();
    }
    
    @Override
    public Boolean getValue(ResultSet results) throws SQLException
    {
        return results.getBoolean(name);
    }

    @Override
    public void updateValue(ResultSet results, Boolean newValue) throws SQLException
    {
        results.updateBoolean(name, newValue);
    }
}
