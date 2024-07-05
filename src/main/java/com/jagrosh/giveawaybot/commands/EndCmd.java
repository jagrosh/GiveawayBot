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
import com.jagrosh.interactions.command.ApplicationCommand;
import com.jagrosh.interactions.command.ApplicationCommandOption;
import com.jagrosh.interactions.entities.Permission;
import com.jagrosh.interactions.receive.Interaction;
import com.jagrosh.interactions.responses.InteractionResponse;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class EndCmd extends GBCommand
{
    public EndCmd(GiveawayBot bot)
    {
        super(bot);
        this.app = new ApplicationCommand.Builder()
                .setType(ApplicationCommand.Type.CHAT_INPUT)
                .setName(bot.getCommandPrefix() + "end")
                .setDescription("end a giveaway")
                .addOptions(new ApplicationCommandOption(ApplicationCommandOption.Type.STRING, "giveaway_id", "ID of giveaway to end now", true, null, null, true))
                .setDmPermission(false)
                .setDefaultPermissions(Permission.MANAGE_GUILD)
                .build();
    }
    
    @Override
    public InteractionResponse gbExecute(Interaction interaction) throws GiveawayException
    {
        //bot.getGiveawayManager().checkPermission(interaction.getMember(), interaction.getGuildId());
        
        String sid = interaction.getCommandData().getOptionByName("giveaway_id").getStringValue().split("~")[0].trim();
        long id = -1;
        try
        {
            id = Long.parseLong(sid);
        }
        catch(NumberFormatException ignore) {}
        if(id < 0)
            return respondError(LocalizedMessage.ERROR_INVALID_ID.getLocalizedMessage(interaction.getEffectiveLocale(), sid));
        
        Giveaway g = bot.getDatabase().getGiveaway(id);
        if(g == null || g.getGuildId() != interaction.getGuildId())
            return respondError(LocalizedMessage.ERROR_GIVEAWAY_NOT_FOUND.getLocalizedMessage(interaction.getEffectiveLocale(), id+""));
        
        boolean success = bot.getGiveawayManager().endGiveaway(g, true, interaction.getUser().getId());
        
        if(success)
            return respondSuccess(LocalizedMessage.SUCCESS_GIVEAWAY_ENDED.getLocalizedMessage(interaction.getEffectiveLocale(), id+""));
        else
            return respondError(LocalizedMessage.ERROR_GENERIC_ENDING.getLocalizedMessage(interaction.getEffectiveLocale()));
    }
}
