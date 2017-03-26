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
import com.jagrosh.giveawaybot.entities.Giveaway;
import com.jagrosh.giveawaybot.util.FormatUtil;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.security.auth.login.LoginException;
import me.jagrosh.jdautilities.commandclient.Command.Category;
import me.jagrosh.jdautilities.commandclient.CommandClient;
import me.jagrosh.jdautilities.commandclient.CommandClientBuilder;
import me.jagrosh.jdautilities.commandclient.examples.PingCommand;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.utils.PermissionUtil;

/**
 *
 * This bot is designed to simplify giveaways. It is very easy to start a timed
 * giveaway, and easy for users to enter as well. This bot also automatically 
 * picks a winner, and the winner can be re-rolled if necessary.
 * 
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class GiveawayBot {
    
    public static final String TADA = "\uD83C\uDF89";
    public static final String YAY = "<:yay:294906617378504704>";
    public static final Color BLURPLE = Color.decode("#7289DA");
    
    // list of all logins the bot has
    private final List<JDA> shards;
    
    // threadpool to use for timings
    private final ExecutorService threadpool;
    
    // all current giveaways
    private final Set<Giveaway> current;
    
    public static Category GIVEAWAY = new Category("Giveaway", event -> {
        if(PermissionUtil.checkPermission(event.getGuild(), event.getMember(), Permission.MANAGE_SERVER) 
                || event.getMember().getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("giveaways")))
            return true;
        event.reply(event.getClient().getError()+" You must have the Manage Server permission, or a role called \"Giveaways\", to use this command!");
        return false;
    });
    
    public GiveawayBot() {
        shards = new LinkedList<>();
        threadpool = Executors.newCachedThreadPool();
        current = new HashSet<>();
    }
    
    private void addShard(JDA shard) {
        shards.add(shard);
    }
    
    public ExecutorService getThreadpool(){
        return threadpool;
    }
    
    public void addGiveaway(Giveaway giveaway) {
        current.add(giveaway);
    }
    
    public void removeGiveaway(Giveaway giveaway) {
        current.remove(giveaway);
    }
    
    public Set<Giveaway> getGiveaways() {
        return current;
    }
    
    public static void main(String[] args) throws IOException, LoginException, IllegalArgumentException, RateLimitedException {
        
        // determine the number of shards
        int shards = args.length==0 ? 1 : Integer.parseInt(args[0]);
        
        // load tokens from a file
        List<String> tokens = Files.readAllLines(Paths.get("config.txt"));
        
        // instantiate a bot
        GiveawayBot bot = new GiveawayBot();
        
        // build the client to deal with commands
        CommandClient client = new CommandClientBuilder()
                .setPrefix("!g")
                .setOwnerId("113156185389092864")
                .setGame(Game.of("Giveaways! | !ghelp"))
                .setEmojis(TADA, "\uD83D\uDCA5", "\uD83D\uDCA5")
                //.setServerInvite("https://discordapp.com/invite/0p9LSGoRLu6Pet0k")
                .setHelpFunction(event -> FormatUtil.formatHelp(event))
                .setDiscordBotsKey(tokens.get(1))
                .addCommands(
                        new InviteCommand(),
                        new PingCommand(),
                        new StartCommand(bot),
                        new RerollCommand(),
                        new EvalCommand(bot)
                ).build();
        
        for(int i=0; i<shards; i++)
        {
            JDABuilder builder = new JDABuilder(AccountType.BOT)
                    .setToken(tokens.get(0))
                    .setAudioEnabled(false)
                    .setGame(Game.of("loading..."))
                    .setStatus(OnlineStatus.DO_NOT_DISTURB)
                    .addListener(client);
            if(shards>1)
                builder.useSharding(i, shards);
            bot.addShard(builder.buildAsync());
        }
    }
}
