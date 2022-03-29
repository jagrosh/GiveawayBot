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
import com.jagrosh.giveawaybot.GiveawayException;
import com.jagrosh.giveawaybot.entities.LocalizedMessage;
import com.jagrosh.interactions.command.ApplicationCommand;
import com.jagrosh.interactions.command.Command;
import com.jagrosh.interactions.entities.SentMessage;
import com.jagrosh.interactions.receive.Interaction;
import com.jagrosh.interactions.responses.InteractionResponse;
import com.jagrosh.interactions.responses.MessageCallback;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public abstract class GBCommand implements Command
{
    protected ApplicationCommand app;
    
    /*protected GBCommand(String prefix, String name, String description)
    {
        this(prefix, ApplicationCommand.Type.CHAT_INPUT, name, description, true);
    }
    
    protected GBCommand(String prefix, ApplicationCommand.Type type, String name, String description, boolean defaultPermission)
    {
        this.app = new ApplicationCommand.Builder()
                .setName(prefix + name)
                .setDescription(description)
                .setType(type)
                .setDefaultPermission(defaultPermission)
                .build();
    */
    
    @Override
    public ApplicationCommand getApplicationCommand()
    {
        return app;
    }

    @Override
    public InteractionResponse execute(Interaction interaction)
    {
        if(interaction.getGuildId() == 0L)
            return new MessageCallback(new SentMessage.Builder().setContent(LocalizedMessage.ERROR_NO_DMS.getLocalizedMessage(interaction.getEffectiveLocale())).build());
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
