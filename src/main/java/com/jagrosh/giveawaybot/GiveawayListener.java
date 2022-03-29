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
package com.jagrosh.giveawaybot;

import com.jagrosh.giveawaybot.commands.GBCommand;
import com.jagrosh.giveawaybot.data.Database;
import com.jagrosh.giveawaybot.data.Giveaway;
import com.jagrosh.giveawaybot.entities.LocalizedMessage;
import com.jagrosh.giveawaybot.entities.PremiumLevel;
import com.jagrosh.giveawaybot.util.GiveawayUtil;
import com.jagrosh.interactions.InteractionsListener;
import com.jagrosh.interactions.command.Choice;
import com.jagrosh.interactions.components.Component;
import com.jagrosh.interactions.entities.AllowedMentions;
import com.jagrosh.interactions.entities.SentMessage;
import com.jagrosh.interactions.receive.CommandInteractionDataOption;
import com.jagrosh.interactions.receive.Interaction;
import com.jagrosh.interactions.requests.RestClient;
import com.jagrosh.interactions.requests.Route;
import com.jagrosh.interactions.responses.AutocompleteCallback;
import com.jagrosh.interactions.responses.DeferredCallback;
import com.jagrosh.interactions.responses.InteractionResponse;
import com.jagrosh.interactions.responses.MessageCallback;
import com.jagrosh.interactions.util.JsonUtil;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class GiveawayListener implements InteractionsListener
{
    private final Logger log = LoggerFactory.getLogger(GiveawayListener.class);
    private final Database database;
    private final GiveawayManager gman;
    private final RestClient rest;
    
    public GiveawayListener(Database database, GiveawayManager gman, RestClient rest)
    {
        this.database = database;
        this.gman = gman;
        this.rest = rest;
    }
    
    @Override
    public InteractionResponse onModalSubmit(Interaction interaction)
    {
        try
        {
            PremiumLevel lv = database.getPremiumLevel(interaction.getUser().getIdLong());
            // validate inputs
            Giveaway g = gman.constructGiveaway(interaction.getUser(), 
                    interaction.getComponentData().getModalValueByCustomId("time"), 
                    interaction.getComponentData().getModalValueByCustomId("winners"), 
                    interaction.getComponentData().getModalValueByCustomId("prize"), lv, interaction.getEffectiveLocale());

            // attempt giveaway creation
            long id = gman.sendGiveaway(g, interaction.getGuildId(), interaction.getChannelId());

            return new MessageCallback(new SentMessage.Builder()
                    .setContent(LocalizedMessage.SUCCESS_GIVEAWAY_CREATED.getLocalizedMessage(interaction.getEffectiveLocale(), Long.toString(id)))
                    .setEphemeral(true).build());
        }
        catch(GiveawayException ex)
        {
            return new MessageCallback(new SentMessage.Builder()
                    .setContent(ex.getErrorMessage().getLocalizedMessage(interaction.getEffectiveLocale(), ex.getArguments()))
                    .setEphemeral(true).build());
        }
    }

    @Override
    public InteractionResponse onMessageComponent(Interaction interaction)
    {
        switch(interaction.getComponentData().getType())
        {
            case BUTTON:
                return onButton(interaction);
        }
        return new DeferredCallback(false);
    }

    @Override
    public InteractionResponse onAutocomplete(Interaction interaction)
    {
        CommandInteractionDataOption op = interaction.getCommandData().getOptionByName("giveaway_id");
        if(op != null && op.isFocused())
        {
            try
            {
                gman.checkPermission(interaction.getMember(), interaction.getGuildId());
                return new AutocompleteCallback<String>(database.getGiveawaysByChannel(interaction.getChannelId())
                    .stream().limit(25).map(g -> new Choice<>(g.getPrize(), g.getMessageId()+"")).collect(Collectors.toList()));
            }
            catch(GiveawayException ex)
            {
                return new DeferredCallback(false);
            }
        }
        return new DeferredCallback(false);
    }
    
    private InteractionResponse onButton(Interaction interaction)
    {
        String customId = interaction.getComponentData().getCustomId();
        if(customId.equalsIgnoreCase(GiveawayManager.ENTER_BUTTON_ID))
        {
            long id = interaction.getMessage().getIdLong();
            Giveaway g = database.getGiveaway(id);
            if(g == null)
                return GBCommand.respondError(LocalizedMessage.ERROR_GIVEAWAY_ENDED.getLocalizedMessage(interaction.getEffectiveLocale()));
            database.addEntry(id, interaction.getUser());
            return new MessageCallback(gman.renderGiveaway(g, database.getEntryCount(id)), true);
        }
        else if(customId.toLowerCase().startsWith(GiveawayManager.REROLL_BUTTON_ID.toLowerCase()))
        {
            String summaryKey = customId.split(" ")[1];
            String url = "https://cdn.discordapp.com/attachments/" + summaryKey + "/giveaway_summary.json";
            try
            {
                RestClient.RestResponse res = rest.simpleRequest(url).get();
                List<Long> entries = JsonUtil.optArray(res.getBody(), "entries", user -> user.getLong("id"));
                List<Long> winner = GiveawayUtil.selectWinners(entries, 1);
                return new MessageCallback(new SentMessage.Builder()
                        .setAllowedMentions(new AllowedMentions(AllowedMentions.ParseType.USERS))
                        .setReferenceMessage(interaction.getMessage().getIdLong())
                        .setContent(LocalizedMessage.SUCCESS_GIVEAWAY_REROLL.getLocalizedMessage(interaction.getEffectiveLocale(), "<@" + winner.get(0) + ">"))
                        .build());
            } 
            catch(Exception ex)
            {
                return GBCommand.respondError(LocalizedMessage.ERROR_GENERIC_REROLL.getLocalizedMessage(interaction.getEffectiveLocale()));
            }
        }
        return new DeferredCallback(false);
    }
}
