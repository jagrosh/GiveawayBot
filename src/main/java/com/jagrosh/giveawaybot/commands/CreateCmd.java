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

import com.jagrosh.giveawaybot.GiveawayBot;
import com.jagrosh.giveawaybot.GiveawayException;
import com.jagrosh.giveawaybot.entities.PremiumLevel;
import com.jagrosh.interactions.command.ApplicationCommand;
import com.jagrosh.interactions.components.ActionRowComponent;
import com.jagrosh.interactions.components.Component;
import com.jagrosh.interactions.components.TextInputComponent;
import com.jagrosh.interactions.entities.Permission;
import com.jagrosh.interactions.receive.Interaction;
import com.jagrosh.interactions.responses.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class CreateCmd extends GBCommand
{
    private final List<Component> components = new ArrayList<>();
    
    public CreateCmd(GiveawayBot bot)
    {
        super(bot);
        this.app = new ApplicationCommand.Builder()
                .setType(ApplicationCommand.Type.CHAT_INPUT)
                .setName(bot.getCommandPrefix() + "create")
                .setDescription("starts a giveaway (interactive)")
                .setDmPermission(false)
                .setDefaultPermissions(Permission.MANAGE_GUILD)
                .build();
        components.add(new ActionRowComponent(new TextInputComponent("time", TextInputComponent.Style.SHORT, "Duration", 2, null, true, null, "Ex: 10 minutes")));
        components.add(new ActionRowComponent(new TextInputComponent("winners", TextInputComponent.Style.SHORT, "Number of Winners", 1, 2, true, "1", null)));
        components.add(new ActionRowComponent(new TextInputComponent("prize", TextInputComponent.Style.SHORT, "Prize", 1, 128, true, null, null)));
        components.add(new ActionRowComponent(new TextInputComponent("description", TextInputComponent.Style.PARAGRAPH, "Description", 0, 1000, false, null, null)));
    }
    
    @Override
    public InteractionResponse gbExecute(Interaction interaction) throws GiveawayException
    {
        PremiumLevel pl = bot.getDatabase().getPremiumLevel(interaction.getGuildId(), interaction.getMember().getIdLong());
        
        // check availability
        bot.getGiveawayManager().checkAvailability(interaction, pl);
        
        // open modal (remainder of giveaway creation is handled elsewhere
        String customId = interaction.getGuildId() + " " + interaction.getChannelId() + " " + interaction.getMember().getUser().getIdLong();
        return new ModalCallback(customId, "Create a Giveaway", components);
    }
}
