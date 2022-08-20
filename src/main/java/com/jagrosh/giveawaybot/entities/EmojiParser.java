/*
 * Copyright 2022 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.giveawaybot.entities;

import com.jagrosh.giveawaybot.Constants;
import com.vdurmont.emoji.EmojiManager;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class EmojiParser
{
    private final static Pattern PATTERN = Pattern.compile("^<(a?):([A-Za-z0-9_-]{2,32}):(\\d{17,22})>(.*)$");
    private final List<String> freeEmoji;
    
    public EmojiParser(List<String> freeEmoji)
    {
        this.freeEmoji = freeEmoji;
    }
    
    public List<String> getFreeEmoji()
    {
        return freeEmoji;
    }
    
    public ParsedEntryButton parse(String text)
    {
        if(text == null)
            return new ParsedEntryButton(Constants.TADA);
        
        for(int i = text.length(); i>0; i--)
        {
            String e = text.substring(0, i);
            if(EmojiManager.isEmoji(e))
            {
                String remaining = text.substring(i).trim();
                return remaining.isEmpty() ? new ParsedEntryButton(e) : new ParsedEntryButton(e, remaining);
            }
        }
        Matcher m = PATTERN.matcher(text);
        if(m.find()) try
        {
            return new ParsedEntryButton(m.group(2), Long.parseLong(m.group(3)), m.group(1).equals("a"), m.group(4).trim());
        }
        catch(NumberFormatException | NullPointerException | ArrayIndexOutOfBoundsException ignore) {}
        return new ParsedEntryButton(null, 0L, false, text);
    }
    
    public class ParsedEntryButton
    {
        public final String name, text;
        public final long id;
        public final boolean animated;
        
        private ParsedEntryButton(String name)
        {
            this(name, null);
        }
        
        private ParsedEntryButton(String name, String extra)
        {
            this(name, 0L, false, extra);
        }
        
        private ParsedEntryButton(String name, long id, boolean animated, String text)
        {
            this.name = name;
            this.id = id;
            this.animated = animated;
            this.text = text == null || text.isEmpty() ? null : text;
        }
        
        public boolean isFree()
        {
            return text == null && id == 0L && animated == false && freeEmoji.contains(name);
        }
        
        public String render()
        {
            return ((id == 0L 
                    ? (name == null ? "" : name) 
                    : ("<" + (animated ? "a" : "") + ":" + name + ":" + id + ">")) 
                    + " " + (text == null ? "" : text)).trim();
        }
    }
}
