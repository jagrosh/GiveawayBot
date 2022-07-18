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
import com.jagrosh.giveawaybot.entities.LocalizedMessage;
import com.jagrosh.giveawaybot.util.GiveawayUtil;
import com.jagrosh.interactions.command.ApplicationCommand;
import com.jagrosh.interactions.components.ActionRowComponent;
import com.jagrosh.interactions.components.ButtonComponent;
import com.jagrosh.interactions.entities.AllowedMentions;
import com.jagrosh.interactions.entities.Permission;
import com.jagrosh.interactions.entities.ReceivedMessage;
import com.jagrosh.interactions.entities.SentMessage;
import com.jagrosh.interactions.receive.Interaction;
import com.jagrosh.interactions.requests.RestClient;
import com.jagrosh.interactions.responses.InteractionResponse;
import com.jagrosh.interactions.responses.MessageCallback;
import com.jagrosh.interactions.util.JsonUtil;
import java.util.List;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class RerollMessageCmd extends GBCommand
{
    private final static String JUMP_LINK = "https://discord.com/channels/%d/%d/%d";
    private final static String KEY = "#giveaway=";
    
    public RerollMessageCmd(GiveawayBot bot)
    {
        super(bot);
        this.app = new ApplicationCommand.Builder()
                .setType(ApplicationCommand.Type.MESSAGE)
                .setName("Reroll Giveaway")
                //.setDescription("rerolls one new winner from a giveaway")
                .setDmPermission(false)
                .setDefaultPermissions(Permission.MANAGE_GUILD)
                .build();
    }
    
    @Override
    public InteractionResponse gbExecute(Interaction interaction) throws GiveawayException
    {
        ReceivedMessage msg = interaction.getCommandData().getResolvedData().getMessages().get(interaction.getCommandData().getTargetId());
        return rerollGiveaway(interaction, msg, 1);
    }
    
    protected InteractionResponse rerollGiveaway(Interaction interaction, ReceivedMessage msg, int count)
    {
        String summaryKey;
        try
        {
            // check if the message is from the bot
            if(msg.getAuthor().getIdLong() != bot.getBotId())
                return GBCommand.respondError(LocalizedMessage.ERROR_INVALID_MESSAGE.getLocalizedMessage(interaction.getEffectiveLocale()));

            // check if the message is a giveaway by attempting to get the reroll key
            ActionRowComponent arc = (ActionRowComponent) msg.getComponents().get(0);
            String url = arc.getComponents().stream()
                    .map(c -> (ButtonComponent) c)
                    .filter(b -> b.getUrl() != null)
                    .map(b -> b.getUrl())
                    .findFirst().orElse(null);
            summaryKey = url.substring(url.lastIndexOf(KEY) + KEY.length());
        }
        catch(Exception ex)
        {
            return GBCommand.respondError(LocalizedMessage.ERROR_INVALID_MESSAGE.getLocalizedMessage(interaction.getEffectiveLocale()));
        }
        
        // reroll
        String url = "https://cdn.discordapp.com/attachments/" + summaryKey + "/giveaway_summary.json";
        try
        {
            RestClient.RestResponse res = bot.getRestClient().simpleRequest(url).get();
            List<Long> entries = JsonUtil.optArray(res.getBody(), "entries", user -> user.getLong("id"));
            List<Long> winner = GiveawayUtil.selectWinners(entries, count);
            if(winner.isEmpty())
                return GBCommand.respondError(LocalizedMessage.ERROR_GENERIC_REROLL.getLocalizedMessage(interaction.getEffectiveLocale()));
            StringBuilder winStr = new StringBuilder();
            winner.forEach(w -> winStr.append(", <@").append(w).append(">"));
            return new MessageCallback(new SentMessage.Builder()
                    .setAllowedMentions(new AllowedMentions(AllowedMentions.ParseType.USERS))
                    .setReferenceMessage(msg.getIdLong())
                    .setContent(LocalizedMessage.SUCCESS_GIVEAWAY_REROLL.getLocalizedMessage(interaction.getEffectiveLocale(), "<@" + interaction.getUser().getIdLong() + ">", winStr.substring(2)) 
                            + " [\u2197](" + String.format(JUMP_LINK, interaction.getGuildId(), interaction.getChannelId(), msg.getIdLong()) + ")")
                    .build());
        } 
        catch(Exception ex)
        {
            ex.printStackTrace();
            return GBCommand.respondError(LocalizedMessage.ERROR_GENERIC_REROLL.getLocalizedMessage(interaction.getEffectiveLocale()));
        }
    }
}
