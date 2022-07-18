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
import com.jagrosh.giveawaybot.data.Database;
import com.jagrosh.giveawaybot.entities.EmojiParser;
import com.jagrosh.giveawaybot.entities.FileUploader;
import com.jagrosh.giveawaybot.entities.PremiumChecker;
import com.jagrosh.giveawaybot.entities.WebhookLog;
import com.jagrosh.interactions.InteractionsClient;
import com.jagrosh.interactions.command.Command;
import com.jagrosh.interactions.components.*;
import com.jagrosh.interactions.entities.SentMessage;
import com.jagrosh.interactions.requests.RestClient;
import com.jagrosh.interactions.requests.Route;
import com.typesafe.config.Config;
import java.util.concurrent.ExecutionException;


/**
 *
 * This bot is designed to simplify giveaways. It is very easy to start a timed
 * giveaway, and easy for users to enter as well. This bot also automatically 
 * picks a winner, and the winner can be re-rolled if necessary.
 * 
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class GiveawayBot 
{
    private final String cmdPrefix, serverCountOverride;
    private final long botId, controlChannel;
    
    private final WebhookLog webhook;
    private final FileUploader uploader;
    private final RestClient restClient;
    private final Database database;
    private final InteractionsClient interClient;
    private final GiveawayManager manager;
    private final PremiumChecker premium;
    
    protected GiveawayBot(Config config)
    {
        // send startup message
        webhook = new WebhookLog(config.getString("webhook.url"), config.getString("webhook.name"));
        webhook.send(WebhookLog.Level.INFO, "GiveawayBot is starting!");
        
        // get some static values
        cmdPrefix = config.getString("cmd-prefix");
        botId = config.hasPath("bot-id") ? config.getLong("bot-id") : config.getLong("app-id");
        controlChannel = config.hasPath("control-channel") ? config.getLong("control-channel") : 0L;
        serverCountOverride = config.hasPath("server-count") ? config.getString("server-count") : "N/A";
        
        // connect to the database
        database = new Database(config.getString("database.host"), config.getString("database.user"), config.getString("database.pass"));
        webhook.send(WebhookLog.Level.INFO, String.format("Database contains `%d` giveaways", database.countAllGiveaways()));
        
        // instantiate the remaing components
        uploader = new FileUploader(config.getStringList("file-uploader"));
        GiveawayListener listener = new GiveawayListener(this);
        EmojiParser emojis = new EmojiParser(config.getConfig("emojis").getStringList("free"));
        restClient = new RestClient(config.getString("bot-token"));
        premium = new PremiumChecker(database, webhook, config.getString("checker-token"));
        manager = new GiveawayManager(database, restClient, uploader, emojis, botId);
        
        // instantiate commands
        Command[] commands = 
        {
            new HelpCmd(this),
            new AboutCmd(this),
            new PingCmd(this),
            new InviteCmd(this),
            
            new StartCmd(this),
            new CreateCmd(this),
            new ListCmd(this),
            new DeleteCmd(this),
            new EndCmd(this),
            new RerollCmd(this),
            new RerollMessageCmd(this),
            new SettingsCmd(this)
        };
        
        // instantiate interactions client
        interClient = new InteractionsClient.Builder()
                .setAppId(config.getLong("app-id"))
                .setPublicKey(config.getString("public-key"))
                .setKeystore(config.hasPath("keystore") ? config.getString("keystore") : null)
                .setKeystorePass(config.hasPath("keystore-pass") ? config.getString("keystore-pass") : null)
                .setPath("/")
                .setPort(config.getInt("port"))
                .setThreads(config.hasPath("threads") ? config.getInt("threads") : 250)
                .setRestClient(restClient)
                .addCommands(commands)
                .setListener(listener)
                .build();

        // update commands if necessary
        if(config.hasPath("update-commands") && config.getBoolean("update-commands"))
            interClient.updateGlobalCommands();
        
        // send control message if necessary
        if(config.hasPath("send-message") && config.getBoolean("send-message"))
            restClient.request(Route.POST_MESSAGE.format(config.getLong("control-channel")), new SentMessage.Builder()
                    .addComponent(new ActionRowComponent(
                            new ButtonComponent(ButtonComponent.Style.SUCCESS, "View Statistics", "view-statistics"), 
                            new ButtonComponent(ButtonComponent.Style.DANGER, "Shut Down", "shutdown"))).build().toJson());
    }
    
    public void start() throws Exception
    {
        interClient.start();
        manager.start();
        premium.start();
    }
    
    public void shutdown()
    {
        shutdown("N/A");
    }
    
    public void shutdown(String reason)
    {
        new Thread(() ->
        {
            try
            {
                Thread.sleep(500);
                interClient.shutdown();
                premium.shutdown();
                manager.shutdown();
                uploader.shutdown();
                webhook.sendBlocking(WebhookLog.Level.INFO, "Shutting down...  `" + reason + "`");
                webhook.shutdown();
                Thread.sleep(500);
                database.shutdown();
            }
            catch(InterruptedException | ExecutionException ex)
            {
                ex.printStackTrace();
            }
        }).start();
    }
    
    public String getCommandPrefix()
    {
        return cmdPrefix;
    }
    
    public long getBotId()
    {
        return botId;
    }
    
    public String getServerCountOverride()
    {
        return serverCountOverride;
    }
    
    public long getControlChannel()
    {
        return controlChannel;
    }
    
    public RestClient getRestClient()
    {
        return restClient;
    }
    
    public Database getDatabase()
    {
        return database;
    }

    public PremiumChecker getPremiumChecker()
    {
        return premium;
    }
    
    public GiveawayManager getGiveawayManager()
    {
        return manager;
    }
}
