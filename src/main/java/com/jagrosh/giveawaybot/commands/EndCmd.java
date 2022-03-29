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

import com.jagrosh.giveawaybot.GiveawayException;
import com.jagrosh.giveawaybot.GiveawayManager;
import com.jagrosh.giveawaybot.data.Database;
import com.jagrosh.giveawaybot.data.Giveaway;
import com.jagrosh.giveawaybot.entities.LocalizedMessage;
import com.jagrosh.interactions.command.ApplicationCommand;
import com.jagrosh.interactions.command.ApplicationCommandOption;
import com.jagrosh.interactions.receive.Interaction;
import com.jagrosh.interactions.responses.InteractionResponse;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class EndCmd extends GBCommand
{
    private final Database database;
    private final GiveawayManager gman;
    
    public EndCmd(String prefix, Database database, GiveawayManager gman)
    {
        this.database = database;
        this.gman = gman;
        this.app = new ApplicationCommand.Builder()
                .setType(ApplicationCommand.Type.CHAT_INPUT)
                .setName(prefix + "end")
                .setDescription("end a giveaway")
                .addOptions(new ApplicationCommandOption(ApplicationCommandOption.Type.STRING, "giveaway_id", "ends a giveaway", true, null, null, true))
                .build();
    }
    
    @Override
    public InteractionResponse gbExecute(Interaction interaction) throws GiveawayException
    {
        gman.checkPermission(interaction.getMember(), interaction.getGuildId());
        
        String sid = interaction.getCommandData().getOptionByName("giveaway_id").getStringValue().split("~")[0].trim();
        long id = -1;
        try
        {
            id = Long.parseLong(sid);
        }
        catch(NumberFormatException ex) {}
        if(id < 0)
            return respondError(LocalizedMessage.ERROR_INVALID_ID.getLocalizedMessage(interaction.getEffectiveLocale(), sid));
        
        Giveaway g = database.getGiveaway(id);
        if(g == null || g.getGuildId() != interaction.getGuildId())
            return respondError(LocalizedMessage.ERROR_GIVEWAY_NOT_FOUND.getLocalizedMessage(interaction.getEffectiveLocale(), id+""));
        
        boolean success = gman.endGiveaway(g);
        
        if(success)
            return respondSuccess(LocalizedMessage.SUCCESS_GIVEAWAY_ENDED.getLocalizedMessage(interaction.getEffectiveLocale(), id+""));
        else
            return respondError(LocalizedMessage.ERROR_GENERIC_ENDING.getLocalizedMessage(interaction.getEffectiveLocale()));
    }
}
