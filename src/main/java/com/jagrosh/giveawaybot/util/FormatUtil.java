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
package com.jagrosh.giveawaybot.util;

import java.util.Collection;
import java.util.HashMap;
import net.dv8tion.jda.api.JDA;

/**
 *
 * This file contains utility methods to help with formatting output.
 * 
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class FormatUtil 
{
    public static String filter(String input)
    {
        return input.replace("\u202E","") // RTL override
                .replace("@everyone","@\u0435veryone") // cyrillic e
                .replace("@here","@h\u0435re") // cyrillic e
                .trim();
    }
    
    public static String formatShardStatuses(Collection<JDA> shards)
    {
        HashMap<JDA.Status, String> map = new HashMap<>();
        shards.forEach(jda -> map.put(jda.getStatus(), map.getOrDefault(jda.getStatus(), "") + " " + jda.getShardInfo().getShardId()));
        StringBuilder sb = new StringBuilder("```diff");
        map.entrySet().forEach(entry -> sb.append("\n").append(entry.getKey()==JDA.Status.CONNECTED ? "+ " : "- ")
                .append(entry.getKey()).append(":").append(entry.getValue()));
        return sb.append(" ```").toString();
    }
    
    public static String pluralise(long x, String singular, String plural) 
    {
        return x == 1 ? singular : plural;
    }
    
    public static String secondsToTime(long timeseconds)
    {
        StringBuilder builder = new StringBuilder();
        int years = (int)(timeseconds / (60*60*24*365));
        if(years>0){
            builder.append("**").append(years).append("** ").append(pluralise(years, "year", "years")).append(", ");
            timeseconds = timeseconds % (60*60*24*365);
        }
        int weeks = (int)(timeseconds / (60*60*24*7));
        if(weeks>0){
            builder.append("**").append(weeks).append("** ").append(pluralise(weeks, "week", "weeks")).append(", ");
            timeseconds = timeseconds % (60*60*24*7);
        }
        int days = (int)(timeseconds / (60*60*24));
        if(days>0){
            builder.append("**").append(days).append("** ").append(pluralise(days, "day", "days")).append(", ");
            timeseconds = timeseconds % (60*60*24);
        }
        int hours = (int)(timeseconds / (60*60));
        if(hours>0){
            builder.append("**").append(hours).append("** ").append(pluralise(hours, "hour", "hours")).append(", ");
            timeseconds = timeseconds % (60*60);
        }
        int minutes = (int)(timeseconds / (60));
        if(minutes>0){
            builder.append("**").append(minutes).append("** ").append(pluralise(minutes, "minute", "minutes")).append(", ");
            timeseconds = timeseconds % (60);
        }
        if(timeseconds>0) {
            builder.append("**").append(timeseconds).append("** ").append(pluralise(timeseconds, "second", "seconds"));
        }
        String str = builder.toString();
        if(str.endsWith(", "))
            str = str.substring(0,str.length()-2);
        if(str.equals(""))
            str="**No time**";
        return str;
    }
}
