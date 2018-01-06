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

import com.jagrosh.giveawaybot.commands.AboutCommand;
import com.jagrosh.giveawaybot.commands.CreateCommand;
import com.jagrosh.giveawaybot.commands.EndCommand;
import com.jagrosh.giveawaybot.commands.EvalCommand;
import com.jagrosh.giveawaybot.commands.InviteCommand;
import com.jagrosh.giveawaybot.commands.RerollCommand;
import com.jagrosh.giveawaybot.commands.ShutdownCommand;
import com.jagrosh.giveawaybot.commands.StartCommand;
import com.jagrosh.giveawaybot.database.Database;
import com.jagrosh.giveawaybot.entities.Giveaway;
import com.jagrosh.giveawaybot.util.FormatUtil;
import com.jagrosh.jdautilities.commandclient.CommandClient;
import com.jagrosh.jdautilities.commandclient.CommandClientBuilder;
import com.jagrosh.jdautilities.commandclient.examples.PingCommand;
import com.jagrosh.jdautilities.waiter.EventWaiter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.core.events.role.update.RoleUpdateColorEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.Request;
import net.dv8tion.jda.core.requests.Response;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.requests.Route;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Bot extends ListenerAdapter
{
    
    private final List<JDA> shards; // list of all logins the bot has
    private final ScheduledExecutorService threadpool; // threadpool to use for timings
    private final Database database; // database
    private final static Logger LOG = LoggerFactory.getLogger("Bot");
    
    private Bot(Database database)
    {
        this.database = database;
        shards = new LinkedList<>();
        threadpool = Executors.newScheduledThreadPool(20);
    }
    
    // protected methods
    protected void addShard(JDA shard)
    {
        shards.add(shard);
    }
    
    // public getters
    public List<JDA> getShards()
    {
        return shards;
    }
    
    public TextChannel getTextChannelById(long id)
    {
        for(JDA shard: shards)
        {
            TextChannel tc = shard.getTextChannelById(id);
            if(tc!=null)
                return tc;
        }
        return null;
    }
    
    public Guild getGuildById(long id)
    {
        for(JDA shard: shards)
        {
            Guild g = shard.getGuildById(id);
            if(g!=null)
                return g;
        }
        return null;
    }
    
    public ScheduledExecutorService getThreadpool()
    {
        return threadpool;
    }
    
    public Database getDatabase()
    {
        return database;
    }
    
    public List<Guild> getManagedGuildsForUser(long userId)
    {
        List<Guild> guilds = new LinkedList<>();
        for(JDA shard: shards)
        {
            for(Guild g: shard.getGuilds())
            {
                Member m = g.getMemberById(userId);
                if(m!=null && Constants.canGiveaway(m))
                    guilds.add(g);
            }
        }
        return guilds;
    }
    
    public List<Giveaway> getGiveaways()
    {
        return database.giveaways.getGiveaways();
    }
    
    // public methods
    public void shutdown()
    {
        threadpool.shutdown();
        shards.forEach(jda -> jda.shutdown());
        database.shutdown();
    }
    
    public boolean startGiveaway(TextChannel channel, Instant now, int seconds, int winners, String prize)
    {
        if(!Constants.canSendGiveaway(channel))
            return false;
        database.settings.updateColor(channel.getGuild());
        Instant end = now.plusSeconds(seconds);
        Message msg = new Giveaway(0, channel.getIdLong(), channel.getGuild().getIdLong(), end, winners, prize).render(channel.getGuild().getSelfMember().getColor(), now);
        channel.sendMessage(msg).queue(m -> {
            m.addReaction(Constants.TADA).queue();
            database.giveaways.createGiveaway(m, end, winners, prize);
        }, v -> LOG.warn("Unable to start giveaway: "+v));
        return true;
    }
    
    public boolean deleteGiveaway(long channelId, long messageId)
    {
        TextChannel channel = getTextChannelById(channelId);
        try {
            channel.deleteMessageById(messageId).queue();
        } catch(Exception e) {
        }
        return database.giveaways.deleteGiveaway(messageId);
    }
    
    // events
    @Override
    public void onRoleUpdateColor(RoleUpdateColorEvent event) {
        if(event.getGuild().getSelfMember().getRoles().contains(event.getRole()))
            database.settings.updateColor(event.getGuild());
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        if(event.getMember().equals(event.getGuild().getSelfMember()))
            database.settings.updateColor(event.getGuild());
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        if(event.getMember().equals(event.getGuild().getSelfMember()))
            database.settings.updateColor(event.getGuild());
    }
    
    
    /**
     * Starts the application in Bot mode
     * @param shards 
     * @throws java.lang.Exception 
     */
    public static void main(int shards) throws Exception
    {
        // load tokens from a file
        // 0 - bot token
        // 1 - dbots key
        // 2 - database host
        // 3 - database username
        // 4 - database pass
        // 5 - rest token
        List<String> tokens = Files.readAllLines(Paths.get("config.txt"));
        
        // instantiate a bot with a database connector
        Bot bot = new Bot(new Database(tokens.get(2), tokens.get(3), tokens.get(4)));
        
        // instantiate an event waiter
        EventWaiter waiter = new EventWaiter();
        
        // build the client to deal with commands
        CommandClient client = new CommandClientBuilder()
                .setPrefix("!g")
                .setAlternativePrefix("g!")
                .setOwnerId("113156185389092864")
                .setGame(Game.playing(Constants.WEBSITE+" | Type !ghelp"))
                .setEmojis(Constants.TADA, "\uD83D\uDCA5", "\uD83D\uDCA5")
                //.setServerInvite("https://discordapp.com/invite/0p9LSGoRLu6Pet0k")
                .setHelpFunction(event -> FormatUtil.formatHelp(event))
                .setDiscordBotsKey(tokens.get(1))
                .setCarbonitexKey(tokens.get(5))
                .setDiscordBotListKey(tokens.get(6))
                .addCommands(
                        new AboutCommand(bot),
                        new InviteCommand(),
                        new PingCommand(),
                        
                        new CreateCommand(bot,waiter),
                        new StartCommand(bot),
                        new EndCommand(bot),
                        new RerollCommand(bot),
                        
                        new EvalCommand(bot),
                        new ShutdownCommand(bot)
                ).build();
        
        // set up log
        Route.CompiledRoute route = Route.Messages.SEND_MESSAGE.compile("196692840602927104");
        
        // start up each shard
        for(int i=0; i<shards; i++)
        {
            JDABuilder builder = new JDABuilder(AccountType.BOT)
                    .setToken(tokens.get(0))
                    .setAudioEnabled(false)
                    .setGame(Game.playing("loading..."))
                    .setStatus(OnlineStatus.DO_NOT_DISTURB)
                    .addEventListener(client)
                    .addEventListener(waiter)
                    .addEventListener(bot);
            if(shards>1)
                builder.useSharding(i, shards);
            long time = System.currentTimeMillis();
            JDA tmp;
            try
            {
                LOG.info("Starting shard "+i);
                tmp = builder.buildBlocking();
            } catch(Exception e)
            {
                Thread.sleep(5000);
                LOG.info("Retrying shard "+i);
                tmp = builder.buildBlocking();
            }
            bot.addShard(tmp);
            System.gc();
            new RestAction<Void>(tmp, route, new JSONObject().put("content", "\uD83D\uDCE1 Shard `"+(i+1)+"/"
                    +shards+"` has connected. Guilds: `"+tmp.getGuilds().size()+"` Users: `"+tmp.getUsers().size()+"`")){
                @Override
                protected void handleResponse(Response rspns, Request<Void> rqst) {}
            }.queue();
            long diff = System.currentTimeMillis()-time;
            if(diff<6000)
                try{Thread.sleep(6000-diff);}catch(InterruptedException ex){}
        }
    }
}
