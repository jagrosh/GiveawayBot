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
package com.jagrosh.giveawaybot.entities;

import com.jagrosh.giveawaybot.GiveawayBot;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.utils.SimpleLog;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class DataPersistence extends ListenerAdapter {
    
    public final GiveawayBot bot;
    private final static String FILENAME = "giveaways_restart.txt";
    
    public DataPersistence(GiveawayBot bot) {
        this.bot = bot;
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        bot.shutdown();
        Set<Giveaway> giveaways = bot.getGiveaways();
        StringBuilder builder = new StringBuilder();
        giveaways.forEach(g -> {
            builder.append(g.getMessage().getChannel().getId()).append("  ").append(g.getMessage().getId()).append("  ")
                    .append(g.getEnd().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).append("  ").append(g.getPrize()).append("\n");
        });
        try {
            Files.write(Paths.get(FILENAME), builder.toString().trim().getBytes());
        }catch(Exception e) {
            SimpleLog.getLog("Saving").fatal(e);
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        try {
            Files.readAllLines(Paths.get(FILENAME)).forEach(str -> {
                String[] parts = str.split("  ",4);
                try {
                    Message m = event.getJDA().getTextChannelById(parts[0]).getMessageById(parts[1]).complete();
                    OffsetDateTime end = OffsetDateTime.parse(parts[2]);
                    String prize = parts.length<4||parts[3]==null||parts[3].equals("null") ? null : parts[3];
                    new Giveaway(bot, end, m, prize).start();
                } catch(Exception e) {
                    SimpleLog.getLog("Loading").warn(e);
                }
            });
        } catch(Exception e) {
            SimpleLog.getLog("Loading").fatal(e);
        }
    }
    
}
