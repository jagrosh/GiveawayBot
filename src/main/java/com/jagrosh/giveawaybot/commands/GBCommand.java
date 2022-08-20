/*
 * Copyright 2022 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.giveawaybot.commands;

import com.jagrosh.giveawaybot.Constants;
import com.jagrosh.giveawaybot.GiveawayBot;
import com.jagrosh.giveawaybot.GiveawayException;
import com.jagrosh.giveawaybot.data.GuildSettings;
import com.jagrosh.giveawaybot.entities.LocalizedMessage;
import com.jagrosh.interactions.command.ApplicationCommand;
import com.jagrosh.interactions.command.Command;
import com.jagrosh.interactions.entities.Guild;
import com.jagrosh.interactions.entities.SentMessage;
import com.jagrosh.interactions.receive.Interaction;
import com.jagrosh.interactions.requests.Route;
import com.jagrosh.interactions.responses.InteractionResponse;
import com.jagrosh.interactions.responses.MessageCallback;
import java.time.Instant;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public abstract class GBCommand implements Command
{
    private final Logger log = LoggerFactory.getLogger(GBCommand.class);
    protected final GiveawayBot bot;
    protected ApplicationCommand app;
    
    protected GBCommand(GiveawayBot bot)
    {
        this.bot = bot;
    }
    
    @Override
    public ApplicationCommand getApplicationCommand()
    {
        return app;
    }

    @Override
    public InteractionResponse execute(Interaction interaction)
    {
        // bot cannot be used in DMs
        if(interaction.getGuildId() == 0L)
            return new MessageCallback(new SentMessage.Builder().setContent(LocalizedMessage.ERROR_NO_DMS.getLocalizedMessage(interaction.getEffectiveLocale())).build());
        
        // update cached user for interaction
        bot.getDatabase().updateUser(interaction.getUser());
        
        // update cached guild info
        GuildSettings gs = bot.getDatabase().getSettings(interaction.getGuildId());
        Instant now = Instant.now();
        if(gs.getLatestRetrieval().plusSeconds(60*20).isBefore(now))
        {
            Guild g;
            try
            {
                JSONObject gjson = bot.getRestClient().request(Route.GET_GUILD.format(interaction.getGuildId()), "").get().getBody();
                //log.info(String.format("Retrieved guild: " + gjson));
                g = new Guild(gjson);
            }
            catch(Exception ex) 
            {
                g = null;
                log.error(String.format("Failed to retrieve guild: %s, %s", ex, ex.getMessage()));
            }
            bot.getDatabase().setAutomaticGuildSettings(interaction.getGuildId(), now, g);
        }
        
        // attempt to run command
        try
        {
            return gbExecute(interaction);
        }
        catch (GiveawayException ex)
        {
            return respondError(ex.getErrorMessage().getLocalizedMessage(interaction.getEffectiveLocale(), ex.getArguments()));
        }
    }
    
    protected abstract InteractionResponse gbExecute(Interaction interaction) throws GiveawayException;
    
    public static MessageCallback respondSuccess(String content)
    {
        return respond(Constants.YAY + " " + content);
    }
    
    public static MessageCallback respondError(String content)
    {
        return respond(Constants.ERROR + " " + content);
    }
    
    public static MessageCallback respond(String content)
    {
        return new MessageCallback(new SentMessage.Builder().setContent(content).setEphemeral(true).build());
    }
}
