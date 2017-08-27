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

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 * @param <T>
 */
public abstract class SQLColumn<T> {
    
    public final String name;
    public final boolean nullable;
    public final T defaultValue;
    public final boolean primaryKey;
    
    protected SQLColumn(String name, boolean nullable, T defaultValue)
    {
        this(name, nullable, defaultValue, false);
    }
    
    protected SQLColumn(String name, boolean nullable, T defaultValue, boolean primaryKey)
    {
        this.name = name;
        this.nullable = nullable;
        this.defaultValue = defaultValue;
        this.primaryKey = primaryKey;
    }

    protected String nullable()
    {
        return nullable ? "" : " NOT NULL";
    }
    
    public String is(String value)
    {
        return name+" = "+value;
    }
    
    public String is(long value)
    {
        return name+" = "+value;
    }
    
    public String isLessThan(long value)
    {
        return name+" < "+value;
    }
    
    @Override
    public String toString()
    {
        return name;
    }
    
    public abstract String getDataDescription();
    
    public abstract T getValue(ResultSet results) throws SQLException;
    
    public abstract void updateValue(ResultSet results, T newValue) throws SQLException;
}
