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
        // load config and send startup message
        Config config = ConfigFactory.load();
        WebhookLog webhook = new WebhookLog(config.getString("webhook.url"), config.getString("webhook.name"));
        webhook.send(WebhookLog.Level.INFO, "GiveawayBot is starting!");
        
        // connect to the database
        Database database = new Database(config.getString("database.host"), config.getString("database.user"), config.getString("database.pass"));
        webhook.send(WebhookLog.Level.INFO, String.format("Database contains `%d` giveaways", database.countAllGiveaways()));
        
        // instantiate the rest client and start the giveaway manager
        RestClient restClient = new RestClient(config.getString("bot-token"));
        FileUploader uploader = new FileUploader(config.getStringList("file-uploader"));
        GiveawayManager givMan = new GiveawayManager(database, restClient, uploader);
        givMan.start();
        
        // instantiate commands
        String cmdPrefix = config.getString("cmd-prefix");
        Command[] commands = 
        {
            new AboutCmd(cmdPrefix, database),
            new PingCmd(cmdPrefix),
            new InviteCmd(cmdPrefix),
            
            new StartCmd(cmdPrefix, givMan),
            new CreateCmd(cmdPrefix, givMan),
            new ListCmd(cmdPrefix, database, givMan),
            new EndCmd(cmdPrefix, database, givMan),
            new SettingsCmd(cmdPrefix, database)
        };
        
        // instantiate interactions client
        InteractionsClient client = new InteractionsClient.Builder()
                .setAppId(config.getLong("app-id"))
                .setRestClient(restClient)
                .addCommands(commands)
                .setListener(new GiveawayListener(database, givMan, restClient))
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
        Interactions.start(client, ic);
    }
}
