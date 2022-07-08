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
package com.jagrosh.giveawaybot;

import java.awt.Color;
import java.time.OffsetDateTime;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Constants 
{
    public static final OffsetDateTime STARTUP = OffsetDateTime.now();
    public static final long  SERVER_ID = 585687812548853760L;
    public static final String TADA     = "\uD83C\uDF89"; // 🎉
    public static final String WARNING  = "\uD83D\uDCA5"; // 💥
    public static final String ERROR    = "\uD83D\uDCA5"; // 💥
    public static final String YAY      = "<:yay:585696613507399692>";//"<:yay:440620097543864320>";
    public static final String REACTION = "yay:585696613507399692";//"yay:440620097543864320";
    public static final Color  BLURPLE  = new Color(0x5865F2);
    public static final Color  GRAY     = new Color(0x2F3136);
    public static final String WEBSITE  = "https://giveawaybot.party";
    public static final String INVITE   = WEBSITE + "/invite";
    public static final String DONATE   = WEBSITE + "/donate";
    public static final String SUPPORT  = WEBSITE + "/support";
    public static final String SUMMARY  = WEBSITE + "/summary";
    public static final String GITHUB   = "https://github.com/jagrosh/GiveawayBot";
    public static final String ADMIN    = "https://discord.com/oauth2/authorize?permissions=8&scope=bot+applications.commands&client_id=%s&guild_id=%s";
    
    public static final int    MIN_TIME = 10;
    public static final String OWNER    = "**jagrosh**#4824";
    public static final String VERSION  = "3.0";
}
