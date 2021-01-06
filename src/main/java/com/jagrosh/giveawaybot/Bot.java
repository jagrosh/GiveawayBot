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
import com.jagrosh.giveawaybot.entities.*;
import com.jagrosh.giveawaybot.util.FormatUtil;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.examples.command.PingCommand;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdateColorEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Bot extends ListenerAdapter
{
    private ShardManager shards; // list of all logins the bot has
    
    private final WebhookLog webhook;
    private final ScheduledExecutorService threadpool; // threadpool to use for timings
    private final Database database; // database
    private final Logger LOG = LoggerFactory.getLogger("Bot");
    
    private Bot(Database database, String webhookUrl)
    {
        this.database = database;
        this.threadpool = Executors.newScheduledThreadPool(20);
        this.webhook = new WebhookLog(webhookUrl, System.getProperty("logname"));
        
        new Uptimer.DatabaseUptimer(this).start(this.threadpool);
        new Uptimer.StatusUptimer(this).start(this.threadpool);
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
    
    public WebhookLog getWebhookLog()
    {
        return webhook;
    }
    
    public Database getDatabase()
    {
        return database;
    }
    
    // public methods
    public void shutdown()
    {
        threadpool.shutdown();
        shards.shutdown();
        database.shutdown();
    }
    
    public boolean startGiveaway(TextChannel channel, User creator, Instant now, int seconds, int winners, String prize)
    {
        if(!Constants.canSendGiveaway(channel))
            return false;
        database.settings.updateColor(channel.getGuild());
        Instant end = now.plusSeconds(seconds);
        Message msg = new Giveaway(0, channel.getIdLong(), channel.getGuild().getIdLong(), creator.getIdLong(), end, winners, prize, Status.RUN, false)
                .render(channel.getGuild().getSelfMember().getColor(), now);
        channel.sendMessage(msg).queue(m -> 
        {
            m.addReaction(Constants.TADA).queue();
            database.giveaways.createGiveaway(m, creator, end, winners, prize, false);
        }, v -> LOG.warn("Unable to start giveaway: "+v));
        return true;
    }
    
    public boolean startGiveaway(TextChannel channel, List<TextChannel> additional, User creator, Instant now, int seconds, int winners, String prize)
    {
        if(!Constants.canSendGiveaway(channel))
            return false;
        if(additional.stream().anyMatch(c -> !Constants.canSendGiveaway(c)))
            return false;
        database.settings.updateColor(channel.getGuild());
        Instant end = now.plusSeconds(seconds);
        Message msg = new Giveaway(0, channel.getIdLong(), channel.getGuild().getIdLong(), creator.getIdLong(), end, winners, prize, Status.RUN, false)
                .render(channel.getGuild().getSelfMember().getColor(), now);
        Map<Long,Long> map = additional.stream()
                .map(c -> 
                { 
                    Message m = c.sendMessage(msg).complete();
                    m.addReaction(Constants.TADA).queue();
                    return m;
                })
                .collect(Collectors.toMap(m -> m.getChannel().getIdLong(), m -> m.getIdLong()));
        channel.sendMessage(msg).queue(m -> 
        {
            m.addReaction(Constants.TADA).queue();
            database.expanded.createExpanded(m.getIdLong(), map);
            database.giveaways.createGiveaway(m, creator, end, winners, prize, true);
        }, v -> LOG.warn("Unable to start giveaway: "+v));
        return true;
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

    /*@Override
    public void onReady(ReadyEvent event)
    {
        webhook.send(Constants.TADA + " Shard `"+(event.getJDA().getShardInfo().getShardId()+1)+"/"
                +event.getJDA().getShardInfo().getShardTotal()+"` has connected. Guilds: `"
                +event.getJDA().getGuilds().size() + "`");// + " Users: `"+event.getJDA().getUsers().size()+"`");
    }//*/
    
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
        
        // build the client to deal with commands
        CommandClient client = new CommandClientBuilder()
                .setPrefix(config.getString("prefix"))
                .setAlternativePrefix(config.getString("altprefix"))
                .setOwnerId("113156185389092864")
                .setActivity(Activity.playing(Constants.TADA+" "+Constants.WEBSITE+" "+Constants.TADA+" Type !ghelp "+Constants.TADA))
                .setEmojis(Constants.TADA, Constants.WARNING, Constants.ERROR)
                .setHelpConsumer(event -> event.replyInDm(FormatUtil.formatHelp(event), 
                        m-> {try{event.getMessage().addReaction(Constants.REACTION).queue(s->{},f->{});}catch(PermissionException ignored){}}, 
                        f-> event.replyWarning("Help could not be sent because you are blocking Direct Messages")))
                .setDiscordBotsKey(config.getString("listing.discord-bots"))
                .setCarbonitexKey(config.getString("listing.carbon"))
                .addCommands(
                        new AboutCommand(bot),
                        new InviteCommand(),
                        new PingCommand(),
                        
                        new CreateCommand(bot),
                        new StartCommand(bot),
                        new EndCommand(bot),
                        new RerollCommand(bot),
                        new ListCommand(bot),
                        new SettingsCommand(bot),
                        
                        new DistributeCommand(bot),
                        new RerolldistCommand(bot),
                        
                        new DebugCommand(bot),
                        new EvalCommand(bot),
                        new ShutdownCommand(bot)
                ).build();
        
        bot.webhook.send(WebhookLog.Level.INFO, "Starting shards `"+(shardSetId*shardSetSize + 1) + " - " + ((shardSetId+1)*shardSetSize) + "` of `"+shardTotal+"`...");
        
        MessageAction.setDefaultMentions(Arrays.asList(MentionType.CHANNEL, MentionType.EMOTE, MentionType.USER));
        
        // start logging in
        bot.shards = DefaultShardManagerBuilder
                .createLight(config.getString("bot-token"), GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGES/*, GatewayIntent.GUILD_MEMBERS*/) // I guess we just dont get role changes? what the heck discord
                .setCallbackPool(Executors.newScheduledThreadPool(100, r -> new Thread(r, "gbcallback")))
                .setRateLimitPool(Executors.newScheduledThreadPool(100, r -> new Thread(r, "gbratelimit")))
                .setEventPool(Executors.newScheduledThreadPool(100, r -> new Thread(r, "gbevent")))
                //.setGatewayPool(Executors.newScheduledThreadPool(100, r -> new Thread("gbgateway")))
                .setShardsTotal(shardTotal)
                .setShards(shardSetId*shardSetSize, (shardSetId+1)*shardSetSize-1)
                .setActivity(Activity.playing("loading..."))
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .addEventListeners(client, bot, new MessageWaiter())
                .enableCache(CacheFlag.MEMBER_OVERRIDES)
                .setChunkingFilter(ChunkingFilter.NONE)
                .build();
    }
}
