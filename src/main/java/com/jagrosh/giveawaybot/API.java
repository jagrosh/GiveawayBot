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

import com.jagrosh.giveawaybot.entities.Giveaway;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class API {
    
    //private static SimpleLog LOG = SimpleLog.getLog("API");
    
    /**
     * Starts the REST API
     * @param token
     * @param bot
     */
    /*public static void main(String token, Bot bot)
    {
        Spark.port(8182);
        
        // Gets the guilds a user can manager
        Spark.get("/api/users/:id/guilds", (req, res) -> {
            if(!isAuth(req,token))
                return noAuth(res);
            long id;
            try {
                id = Long.parseLong(req.params("id"));
            } catch(NumberFormatException ex) {
                return error(res, "Invalid ID '"+req.params("id")+"'");
            }
            List<Guild> guilds = bot.getManagedGuildsForUser(id);
            JSONArray array = new JSONArray();
            guilds.stream().map((guild) -> {
                JSONObject g = new JSONObject();
                g.put("id", guild.getId());
                g.put("name",guild.getName());
                g.put("icon",guild.getIconId()==null ? JSONObject.NULL : guild.getIconId());
                return g;
            }).forEachOrdered((g) -> {
                array.put(g);
            });
            res.status(200);
            res.header("Content-Type", "application/json");
            res.body(array.toString());
            return res.body();
        });
        
        // Gets the information for a guild's dashboard
        Spark.get("/api/guilds/:id/dashboard", (req, res) -> {
            if(!isAuth(req,token))
                return noAuth(res);
            long id;
            try {
                id = Long.parseLong(req.params("id"));
            } catch(NumberFormatException ex) {
                return error(res, "Invalid ID '"+req.params("id")+"'");
            }
            Guild guild = bot.getGuildById(id);
            if(guild==null)
                return error(res, "Guild Not Found");
            JSONObject obj = new JSONObject();
            JSONObject g = new JSONObject();
            g.put("id", guild.getId());
            g.put("name",guild.getName());
            g.put("icon",guild.getIconId()==null ? JSONObject.NULL : guild.getIconId());
            obj.put("guild", g);
            JSONArray array = new JSONArray();
            bot.getDatabase().giveaways.getGiveaways(guild).forEach(giveaway -> {
                JSONObject give = new JSONObject();
                give.put("prize", giveaway.prize==null || giveaway.prize.isEmpty() ? "No prize listed" : giveaway.prize);
                TextChannel tc = guild.getTextChannelById(giveaway.channelId);
                give.put("channel", tc==null ? JSONObject.NULL : tc.getName());
                give.put("id", Long.toString(giveaway.messageId));
                give.put("winners", giveaway.winners);
                give.put("end", giveaway.end.atZone(ZoneId.of("Z")).format(DateTimeFormatter.RFC_1123_DATE_TIME));
                array.put(give);
            });
            obj.put("giveaways", array);
            JSONArray array2 = new JSONArray();
            guild.getTextChannels().stream().filter(tc -> Constants.canSendGiveaway(tc)).forEach(tc -> {
                JSONObject c = new JSONObject();
                c.put("name",tc.getName());
                c.put("id", tc.getId());
            });
            obj.put("channels", array2);
            res.status(200);
            res.header("Content-Type", "application/json");
            res.body(obj.toString());
            return res.body();
        });
        
        // Gets global stats
        Spark.get("/api/stats", (req, res) -> {
            if(!isAuth(req,token))
                return noAuth(res);
            JSONObject obj = new JSONObject();
            obj.put("total",bot.getDatabase().giveaways.getGiveaways().size());
            res.status(200);
            res.header("Content-Type", "application/json");
            res.body(obj.toString());
            return res.body();
        });
        
        // Starts a giveaway
        Spark.post("/api/giveaways", (req, res) -> {
            if(!isAuth(req,token))
                return noAuth(res);
            res.header("Content-Type", "application/json");
            Instant now = Instant.now();
            JSONObject obj = new JSONObject(req.body());
            long userId = Long.parseLong(obj.getString("userid"));
            long channelId = Long.parseLong(obj.getString("channelid"));
            TextChannel tc = bot.getTextChannelById(channelId);
            if(tc==null)
            {
                res.status(400);
                res.body(new JSONObject().put("message", "Channel does not exist").toString());
                return res.body();
            }
            Member m = tc.getGuild().getMemberById(userId);
            if(m==null)
            {
                res.status(400);
                res.body(new JSONObject().put("message", "Member does not exist").toString());
                return res.body();
            }
            if(!Constants.canGiveaway(m))
            {
                res.status(400);
                res.body(new JSONObject().put("message", "Member cannot start giveaway").toString());
                return res.body();
            }
            int seconds = obj.getInt("seconds");
            int winners = obj.getInt("winners");
            String prize = obj.getString("prize");
            if(prize.length()>Constants.PRIZE_MAX)
                prize = prize.substring(0, Constants.PRIZE_MAX);
            if(bot.startGiveaway(tc, now, seconds, winners, prize))
            {
                res.status(200);
                res.body(new JSONObject().put("channel", tc.getName()).toString());
                return res.body();
            }
            else
            {
                res.status(400);
                res.body(new JSONObject().put("message", "Could not start giveaway").toString());
                return res.body();
            }
        });
        
        // Ends a giveaway
        Spark.post("/api/giveaways/:id/end", (req, res) -> {
            if(!isAuth(req,token))
                return noAuth(res);
            JSONObject obj = new JSONObject(req.body());
            long userId = Long.parseLong(obj.getString("userid"));
            long guildId = Long.parseLong(obj.getString("guildid"));
            Guild g = bot.getGuildById(guildId);
            if(g==null)
            {
                res.status(400);
                res.body(new JSONObject().put("message", "Guild does not exist").toString());
                return res.body();
            }
            Member m = g.getMemberById(userId);
            if(m==null)
            {
                res.status(400);
                res.body(new JSONObject().put("message", "Member does not exist").toString());
                return res.body();
            }
            if(!Constants.canGiveaway(m))
            {
                res.status(400);
                res.body(new JSONObject().put("message", "Member cannot start giveaway").toString());
                return res.body();
            }
            long messageId = Long.parseLong(req.params("id"));
            Giveaway gi = bot.getDatabase().giveaways.getGiveaway(messageId, guildId);
            if(gi==null)
            {
                res.status(400);
                res.body(new JSONObject().put("message", "Giveaway does not exist").toString());
                return res.body();
            }
            if(bot.getDatabase().giveaways.endGiveaway(messageId))
            {
                res.status(200);
                res.body(new JSONObject().put("message", "Ended giveaway").toString());
                return res.body();
            }
            else
            {
                res.status(400);
                res.body(new JSONObject().put("message", "Could not end giveaway").toString());
                return res.body();
            }
        });
        
        // Deletes a giveaway
        Spark.delete("/api/giveaways/:id", (req, res) -> {
            if(!isAuth(req,token))
                return noAuth(res);
            JSONObject obj = new JSONObject(req.body());
            long userId = Long.parseLong(obj.getString("userid"));
            long guildId = Long.parseLong(obj.getString("guildid"));
            Guild g = bot.getGuildById(guildId);
            if(g==null)
            {
                res.status(400);
                res.body(new JSONObject().put("message", "Guild does not exist").toString());
                return res.body();
            }
            Member m = g.getMemberById(userId);
            if(m==null)
            {
                res.status(400);
                res.body(new JSONObject().put("message", "Member does not exist").toString());
                return res.body();
            }
            if(!Constants.canGiveaway(m))
            {
                res.status(400);
                res.body(new JSONObject().put("message", "Member cannot start giveaway").toString());
                return res.body();
            }
            long messageId = Long.parseLong(req.params("id"));
            Giveaway gi = bot.getDatabase().giveaways.getGiveaway(messageId, guildId);
            if(gi==null)
            {
                res.status(400);
                res.body(new JSONObject().put("message", "Giveaway does not exist").toString());
                return res.body();
            }
            if(bot.deleteGiveaway(gi.channelId, messageId))
            {
                res.status(200);
                res.body(new JSONObject().put("message", "Deleted giveaway").toString());
                return res.body();
            }
            else
            {
                res.status(400);
                res.body(new JSONObject().put("message", "Could not delete giveaway").toString());
                return res.body();
            }
        });
        
        LOG.info("Spark API started");
    }
    
    private static boolean isAuth(Request req, String token)
    {
        return req.headers("Authorization")!=null && req.headers("Authorization").equals(token);
    }
    
    private static String noAuth(Response res)
    {
        res.status(401);
        res.body(new JSONObject().put("message", "Invalid giveaway auth token").toString());
        return res.body();
    }
    
    private static String error(Response res, String message)
    {
        res.status(400);
        res.body(new JSONObject().put("message", message).toString());
        return res.body();
    }//*/
}
