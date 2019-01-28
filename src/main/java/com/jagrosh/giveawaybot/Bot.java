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

import com.jagrosh.giveawaybot.commands.*;
import com.jagrosh.giveawaybot.database.Database;
import com.jagrosh.giveawaybot.entities.Giveaway;
import com.jagrosh.giveawaybot.entities.Status;
import com.jagrosh.giveawaybot.util.BlockingSessionController;
import com.jagrosh.giveawaybot.util.FormatUtil;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.examples.command.PingCommand;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.core.events.role.update.RoleUpdateColorEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.utils.cache.CacheFlag;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Bot extends ListenerAdapter
{
    
    private ShardManager shards; // list of all logins the bot has
    private final ScheduledExecutorService threadpool; // threadpool to use for timings
    private final Database database; // database
    private final WebhookClient webhook;
    private final static Logger LOG = LoggerFactory.getLogger("Bot");
    
    private Bot(Database database, String webhookUrl)
    {
        this.database = database;
        threadpool = Executors.newScheduledThreadPool(20);
        webhook = new WebhookClientBuilder(webhookUrl).build();
    }
    
    // protected methods
    protected void setShardManager(ShardManager shards)
    {
        this.shards = shards;
    }
    
    // public getters
    public ShardManager getShardManager()
    {
        return shards;
    }
    
    public ScheduledExecutorService getThreadpool()
    {
        return threadpool;
    }
    
    public WebhookClient getWebhook()
    {
        return webhook;
    }
    
    public Database getDatabase()
    {
        return database;
    }
    
    /*public List<Guild> getManagedGuildsForUser(long userId)
    {
        List<Guild> guilds = new LinkedList<>();
        for(JDA shard: shards.getShards())
        {
            for(Guild g: shard.getGuilds())
            {
                Member m = g.getMemberById(userId);
                if(m!=null && Constants.canGiveaway(m))
                    guilds.add(g);
            }
        }
        return guilds;
    }//*/
    
    // public methods
    public void shutdown()
    {
        threadpool.shutdown();
        shards.shutdown();
        database.shutdown();
    }
    
    public boolean startGiveaway(TextChannel channel, Instant now, int seconds, int winners, String prize)
    {
        if(!Constants.canSendGiveaway(channel))
            return false;
        database.settings.updateColor(channel.getGuild());
        Instant end = now.plusSeconds(seconds);
        Message msg = new Giveaway(0, channel.getIdLong(), channel.getGuild().getIdLong(), end, winners, prize, Status.RUN).render(channel.getGuild().getSelfMember().getColor(), now);
        channel.sendMessage(msg).queue(m -> {
            m.addReaction(Constants.TADA).queue();
            database.giveaways.createGiveaway(m, end, winners, prize);
        }, v -> LOG.warn("Unable to start giveaway: "+v));
        return true;
    }
    
    public boolean deleteGiveaway(long channelId, long messageId)
    {
        TextChannel channel = shards.getTextChannelById(channelId);
        try 
        {
            channel.deleteMessageById(messageId).queue();
        } 
        catch(Exception ignore) {}
        return database.giveaways.deleteGiveaway(messageId);
    }
    
    // events
    @Override
    public void onRoleUpdateColor(RoleUpdateColorEvent event)
    {
        if(event.getGuild().getSelfMember().getRoles().contains(event.getRole()))
            database.settings.updateColor(event.getGuild());
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event)
    {
        if(event.getMember().equals(event.getGuild().getSelfMember()))
            database.settings.updateColor(event.getGuild());
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event)
    {
        if(event.getMember().equals(event.getGuild().getSelfMember()))
            database.settings.updateColor(event.getGuild());
    }

    @Override
    public void onReady(ReadyEvent event)
    {
        webhook.send(Constants.TADA + " Shard `"+(event.getJDA().getShardInfo().getShardId()+1)+"/"
                +event.getJDA().getShardInfo().getShardTotal()+"` has connected. Guilds: `"
                +event.getJDA().getGuilds().size()+"` Users: `"+event.getJDA().getUsers().size()+"`");
    }
    
    /**
     * Starts the application in Bot mode
     * @param shardTotal 
     * @param shardSetId 
     * @param shardSetSize 
     * @throws java.lang.Exception 
     */
    public static void main(int shardTotal, int shardSetId, int shardSetSize) throws Exception
    {
        Config config = ConfigFactory.load();
        
        // instantiate a bot with a database connector
        Bot bot = new Bot(new Database(config.getString("database.host"), 
                                       config.getString("database.username"), 
                                       config.getString("database.password")), 
                          config.getString("webhook"));
        
        // instantiate an event waiter
        EventWaiter waiter = new EventWaiter(Executors.newSingleThreadScheduledExecutor(), false);
        
        // build the client to deal with commands
        CommandClient client = new CommandClientBuilder()
                .setPrefix("!g")
                .setAlternativePrefix("g!")
                .setOwnerId("113156185389092864")
                .setGame(Game.playing(Constants.TADA+" "+Constants.WEBSITE+" "+Constants.TADA+" Type !ghelp "+Constants.TADA))
                .setEmojis(Constants.TADA, Constants.WARNING, Constants.ERROR)
                //.setServerInvite("https://discordapp.com/invite/0p9LSGoRLu6Pet0k")
                .setHelpConsumer(event -> event.replyInDm(FormatUtil.formatHelp(event), 
                        m-> event.getMessage().addReaction(Constants.REACTION).queue(s->{},f->{}), 
                        f-> event.replyWarning("Help could not be sent because you are blocking Direct Messages")))
                .setDiscordBotsKey(config.getString("listing.discord-bots"))
                .setCarbonitexKey(config.getString("listing.carbon"))
                //.setDiscordBotListKey(tokens.get(6))
                .addCommands(
                        new AboutCommand(bot),
                        new InviteCommand(),
                        new PingCommand(),
                        
                        new CreateCommand(bot,waiter),
                        new StartCommand(bot),
                        new EndCommand(bot),
                        new RerollCommand(bot),
                        new ListCommand(bot),
                        
                        new DebugCommand(bot),
                        new EvalCommand(bot),
                        new ShutdownCommand(bot)
                ).build();
        
        bot.getWebhook().send(Constants.TADA + " Starting shards `"+(shardSetId*shardSetSize + 1) + " - " + ((shardSetId+1)*shardSetSize) + "` of `"+shardTotal+"`...");
        
        // start logging in
        bot.setShardManager(new DefaultShardManagerBuilder()
                .setShardsTotal(shardTotal)
                .setShards(shardSetId*shardSetSize, (shardSetId+1)*shardSetSize-1)
                .setToken(config.getString("bot-token"))
                .setAudioEnabled(false)
                .setGame(Game.playing("loading..."))
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .addEventListeners(client, waiter, bot)
                .setSessionController(new BlockingSessionController())
                .setDisabledCacheFlags(EnumSet.of(CacheFlag.VOICE_STATE, CacheFlag.GAME))
                .build());
    }
}
