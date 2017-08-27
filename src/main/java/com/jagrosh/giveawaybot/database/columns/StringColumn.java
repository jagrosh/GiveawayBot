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
public class StringColumn extends SQLColumn<String>
{
    private final int maxLength;
    
    public StringColumn(String name, boolean nullable, String defaultValue, int maxLength)
    {
        this(name, nullable, defaultValue, false, maxLength);
    }
    
    public StringColumn(String name, boolean nullable, String defaultValue, boolean publicKey, int maxLength)
    {
        super(name, nullable, defaultValue, publicKey);
        this.maxLength = maxLength;
    }
    
    @Override
    public String getDataDescription()
    {
        return "VARCHAR(" + maxLength + ")" + (defaultValue==null ? "" : " DEFAULT "+defaultValue) + nullable() + (primaryKey ? " PRIMARY KEY" : "");
    }
    
    @Override
    public String getValue(ResultSet results) throws SQLException
    {
        return results.getString(name);
    }

    @Override
    public void updateValue(ResultSet results, String newValue) throws SQLException
    {
        results.updateString(name, newValue);
    }
}
