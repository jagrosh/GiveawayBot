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
import com.jagrosh.giveawaybot.data.Giveaway;
import com.jagrosh.giveawaybot.entities.LocalizedMessage;
import com.jagrosh.giveawaybot.entities.PremiumLevel;
import com.jagrosh.interactions.command.ApplicationCommand;
import com.jagrosh.interactions.command.ApplicationCommandOption;
import com.jagrosh.interactions.entities.Permission;
import com.jagrosh.interactions.entities.SentMessage;
import com.jagrosh.interactions.receive.Interaction;
import com.jagrosh.interactions.responses.InteractionResponse;
import com.jagrosh.interactions.responses.MessageCallback;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class StartCmd extends GBCommand
{
    public StartCmd(GiveawayBot bot)
    {
        super(bot);
        this.app = new ApplicationCommand.Builder()
                .setType(ApplicationCommand.Type.CHAT_INPUT)
                .setName(bot.getCommandPrefix() + "start")
                .setDescription("starts a giveaway")
                .addOptions(new ApplicationCommandOption(ApplicationCommandOption.Type.STRING, "duration", "duration of the giveaway", true),
                            new ApplicationCommandOption(ApplicationCommandOption.Type.INTEGER, "winners", "number of winners", true, 1, 50, false),
                            new ApplicationCommandOption(ApplicationCommandOption.Type.STRING, "prize", "the prize being given away", true),
                            new ApplicationCommandOption(ApplicationCommandOption.Type.STRING, "description", "a description of your giveaway", false))
                .setDmPermission(false)
                .setDefaultPermissions(Permission.MANAGE_GUILD)
                .build();
    }
    
    @Override
    public InteractionResponse gbExecute(Interaction interaction) throws GiveawayException
    {
        PremiumLevel pl = bot.getDatabase().getPremiumLevel(interaction.getGuildId(), interaction.getMember().getIdLong());
        
        // check availability
        bot.getGiveawayManager().checkAvailability(interaction, pl);
        
        // validate inputs
        Giveaway g = bot.getGiveawayManager().constructGiveaway(interaction.getUser(), 
                interaction.getCommandData().getOptionByName("duration").getStringValue(), 
                interaction.getCommandData().getOptionByName("winners").getIntValue() + "", 
                interaction.getCommandData().getOptionByName("prize").getStringValue(),
                interaction.getCommandData().getOptionByName("description") == null ? null :
                        interaction.getCommandData().getOptionByName("description").getStringValue(),
                pl, interaction.getEffectiveLocale());
        
        // attempt giveaway creation
        long id = bot.getGiveawayManager().sendGiveaway(g, interaction.getGuildId(), interaction.getChannelId());
        
        return new MessageCallback(new SentMessage.Builder()
                .setContent(LocalizedMessage.SUCCESS_GIVEAWAY_CREATED.getLocalizedMessage(interaction.getEffectiveLocale(), Long.toString(id)))
                .setEphemeral(true).build());
    }
}
