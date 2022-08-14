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
import static com.jagrosh.giveawaybot.commands.GBCommand.respondError;
import com.jagrosh.giveawaybot.entities.LocalizedMessage;
import com.jagrosh.interactions.command.ApplicationCommand;
import com.jagrosh.interactions.command.ApplicationCommandOption;
import com.jagrosh.interactions.entities.Permission;
import com.jagrosh.interactions.entities.ReceivedMessage;
import com.jagrosh.interactions.receive.CommandInteractionDataOption;
import com.jagrosh.interactions.receive.Interaction;
import com.jagrosh.interactions.requests.RestClient.RestResponse;
import com.jagrosh.interactions.requests.Route;
import com.jagrosh.interactions.responses.InteractionResponse;
import java.util.concurrent.ExecutionException;
import org.json.JSONObject;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class RerollCmd extends RerollMessageCmd
{
    public RerollCmd(GiveawayBot bot)
    {
        super(bot);
        this.app = new ApplicationCommand.Builder()
                .setType(ApplicationCommand.Type.CHAT_INPUT)
                .setName(bot.getCommandPrefix() + "reroll")
                .setDescription("rerolls one new winner from a giveaway")
                .addOptions(new ApplicationCommandOption(ApplicationCommandOption.Type.STRING, "giveaway_id", "ID of giveaway to reroll", true))
                .addOptions(new ApplicationCommandOption(ApplicationCommandOption.Type.INTEGER, "count", "number of new winners to pick", false, 1, 10, false))
                .setDmPermission(false)
                .setDefaultPermissions(Permission.MANAGE_GUILD)
                .build();
    }
    
    @Override
    public InteractionResponse gbExecute(Interaction interaction) throws GiveawayException
    {
        long msgId = interaction.getCommandData().getOptionByName("giveaway_id").getIdValue();
        CommandInteractionDataOption countOpt = interaction.getCommandData().getOptionByName("count");
        int count = countOpt == null ? 1 : countOpt.getIntValue();
        String tip = " " + LocalizedMessage.ERROR_REROLL_MESSAGE_OPTION.getLocalizedMessage(interaction.getEffectiveLocale());
        if(msgId < 150000000000000000L)
            return respondError(LocalizedMessage.ERROR_INVALID_ID.getLocalizedMessage(interaction.getEffectiveLocale(), interaction.getCommandData().getOptionByName("giveaway_id").getStringValue()) + tip);
        if(!interaction.appHasPermission(Permission.READ_MESSAGE_HISTORY))
            return respondError(LocalizedMessage.ERROR_BOT_PERMISSIONS.getLocalizedMessage(interaction.getEffectiveLocale(), bot.getGiveawayManager().getPermsLink(interaction.getGuildId())));
        try
        {
            RestResponse res = bot.getRestClient().request(Route.GET_MESSAGE.format(interaction.getChannelId(), msgId)).get();
            if(!res.isSuccess())
                return respondError(LocalizedMessage.ERROR_MESSAGE_NOT_FOUND.getLocalizedMessage(interaction.getEffectiveLocale(), msgId) + tip);
            JSONObject json = res.getBody();
            return rerollGiveaway(interaction, new ReceivedMessage(json), count);
        }
        catch(ExecutionException | InterruptedException ex)
        {
            return respondError(LocalizedMessage.ERROR_MESSAGE_NOT_FOUND.getLocalizedMessage(interaction.getEffectiveLocale(), msgId) + tip);
        }
    }
}
