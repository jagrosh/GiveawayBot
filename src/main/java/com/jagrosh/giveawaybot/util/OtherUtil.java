/*
 * Copyright 2018 John Grosh (john.a.grosh@gmail.com).
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

import com.jagrosh.giveawaybot.Constants;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class OtherUtil
{
    public static int parseShortTime(String timestr)
    {
        timestr = timestr.toLowerCase();
        if(!timestr.matches("\\d{1,8}[smhd]?"))
            return -1;
        int multiplier = 1;
        switch(timestr.charAt(timestr.length()-1))
        {
            case 'd':
                multiplier *= 24;
            case 'h':
                multiplier *= 60;
            case 'm':
                multiplier *= 60;
            case 's':
                timestr = timestr.substring(0, timestr.length()-1);
            default:
        }
        return multiplier * Integer.parseInt(timestr);
    }
    
    public static int parseTime(String timestr)
    {
        timestr = timestr.replaceAll("(?i)(\\s|,|and)","")
                .replaceAll("(?is)(-?\\d+|[a-z]+)", "$1 ")
                .trim();
        String[] vals = timestr.split("\\s+");
        int timeinseconds = 0;
        try
        {
            for(int j=0; j<vals.length; j+=2)
            {
                int num = Integer.parseInt(vals[j]);
                if(vals[j+1].toLowerCase().startsWith("m"))
                    num*=60;
                else if(vals[j+1].toLowerCase().startsWith("h"))
                    num*=60*60;
                else if(vals[j+1].toLowerCase().startsWith("d"))
                    num*=60*60*24;
                timeinseconds+=num;
            }
        }
        catch(Exception ex)
        {
            return 0;
        }
        return timeinseconds;
    }
    
    public static int parseWinners(String winstr)
    {
        if(!winstr.toLowerCase().matches("\\d{1,3}w"))
            return -1;
        return Integer.parseInt(winstr.substring(0, winstr.length()-1));
    }

    public static boolean validateEmoji(Message message, String rawEmoji)
    {
        if (rawEmoji == null || rawEmoji.isEmpty())
            return true;
        try
        {

            message.addReaction(rawEmoji).complete();
        }
        catch (ErrorResponseException error)
        {
            switch (error.getErrorCode())
            {
                case 90001: // REACTION_BLOCKED
                    return validateEmoji(message.getTextChannel(), rawEmoji);
                case 10014: // UNKNOWN_EMOJI
                case 50001: // MISSING_ACCESS
                case 50013: // MISSING_PERMISSION
                default:
                    return false;
            }
        }
        catch (Exception ignored) {}
        return true;
    }

    public static boolean validateEmoji(TextChannel textChannel, String rawEmoji)
    {
        if(rawEmoji == null || rawEmoji.isEmpty())
            return true;
        if(!Constants.canSendGiveaway(textChannel))
            return false;

        return textChannel.sendMessage("Emoji Validation - self destructing")
                .map(message ->
                {
                    boolean value = validateEmoji(message, rawEmoji);
                    message.delete().queue();
                    return value;
                }
        ).complete();
    }
}
