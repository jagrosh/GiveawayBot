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
import com.jagrosh.giveawaybot.entities.FileUploader;
import com.jagrosh.giveawaybot.entities.PremiumChecker;
import com.jagrosh.giveawaybot.entities.WebhookLog;
import com.jagrosh.interactions.Interactions;
import com.jagrosh.interactions.InteractionsClient;
import com.jagrosh.interactions.command.Command;
import com.jagrosh.interactions.requests.RestClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;


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
    /**
     * Main execution
     * @param args - run type and other info
     */
    public static void main(String[] args) throws Exception
    {
        // load config and configure gb core
        Config config = ConfigFactory.load();
        GiveawayBot bot = new GiveawayBot(config);
        
        // instantiate commands
        Command[] commands = 
        {
            new AboutCmd(bot),
            new PingCmd(bot),
            new InviteCmd(bot),
            
            new StartCmd(bot),
            new CreateCmd(bot),
            new ListCmd(bot),
            new EndCmd(bot),
            new SettingsCmd(bot)
        };
        
        // instantiate interactions client
        InteractionsClient client = new InteractionsClient.Builder()
                .setAppId(config.getLong("app-id"))
                .setRestClient(bot.getRestClient())
                .addCommands(commands)
                .setListener(bot.getGiveawayListener())
                .build();
        
        // update commands if necessary
        if(config.hasPath("update-commands") && config.getBoolean("update-commands"))
            client.updateGlobalCommands();
        
        // set up static values for the webserver
        Interactions.InteractionsConfig ic = new Interactions.InteractionsConfig();
        ic.keystore = config.hasPath("keystore") ? config.getString("keystore") : null;
        ic.keystorePass = config.hasPath("keystore-pass") ? config.getString("keystore-pass") : null;
        ic.publicKey = config.getString("public-key");
        ic.port = config.getInt("port");
        
        // go!
        bot.getGiveawayManager().start();
        bot.getPremiumChecker().start();
        Interactions.start(client, ic);
    }
    
    private final String cmdPrefix;
    private final Database database;
    private final RestClient restClient;
    private final GiveawayManager manager;
    private final GiveawayListener listener;
    private final PremiumChecker premium;
    
    private GiveawayBot(Config config)
    {
        // send startup message
        WebhookLog webhook = new WebhookLog(config.getString("webhook.url"), config.getString("webhook.name"));
        webhook.send(WebhookLog.Level.INFO, "GiveawayBot is starting!");
        
        // get the prefix for commands
        cmdPrefix = config.getString("cmd-prefix");
        
        // connect to the database
        database = new Database(config.getString("database.host"), config.getString("database.user"), config.getString("database.pass"));
        webhook.send(WebhookLog.Level.INFO, String.format("Database contains `%d` giveaways", database.countAllGiveaways()));
        
        // instantiate the remaing components
        FileUploader uploader = new FileUploader(config.getStringList("file-uploader"));
        premium = new PremiumChecker(database, webhook, config.getString("checker-token"));
        restClient = new RestClient(config.getString("bot-token"));
        manager = new GiveawayManager(database, restClient, uploader);
        listener = new GiveawayListener(database, manager, restClient);
    }
    
    public String getCommandPrefix()
    {
        return cmdPrefix;
    }
    
    public Database getDatabase()
    {
        return database;
    }

    public PremiumChecker getPremiumChecker()
    {
        return premium;
    }
    
    public RestClient getRestClient()
    {
        return restClient;
    }
    
    public GiveawayManager getGiveawayManager()
    {
        return manager;
    }
    
    public GiveawayListener getGiveawayListener()
    {
        return listener;
    }
}
