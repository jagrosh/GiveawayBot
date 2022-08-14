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
import com.jagrosh.giveawaybot.data.Giveaway;
import com.jagrosh.giveawaybot.entities.LocalizedMessage;
import com.jagrosh.giveawaybot.entities.PremiumLevel;
import com.jagrosh.giveawaybot.util.FormatUtil;
import com.jagrosh.interactions.InteractionsListener;
import com.jagrosh.interactions.command.Choice;
import com.jagrosh.interactions.components.*;
import com.jagrosh.interactions.entities.SentMessage;
import com.jagrosh.interactions.receive.CommandInteractionDataOption;
import com.jagrosh.interactions.receive.Interaction;
import com.jagrosh.interactions.responses.AutocompleteCallback;
import com.jagrosh.interactions.responses.DeferredCallback;
import com.jagrosh.interactions.responses.InteractionResponse;
import com.jagrosh.interactions.responses.MessageCallback;
import java.lang.management.ManagementFactory;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class GiveawayListener implements InteractionsListener
{
    private final Logger log = LoggerFactory.getLogger(GiveawayListener.class);
    private final GiveawayBot bot;
    
    public GiveawayListener(GiveawayBot bot)
    {
        this.bot = bot;
    }
    
    @Override
    public InteractionResponse onModalSubmit(Interaction interaction)
    {
        try
        {
            PremiumLevel lv = bot.getDatabase().getPremiumLevel(interaction.getGuildId(), interaction.getUser().getIdLong());
            // validate inputs
            Giveaway g = bot.getGiveawayManager().constructGiveaway(interaction.getUser(), 
                    interaction.getComponentData().getModalValueByCustomId("time"), 
                    interaction.getComponentData().getModalValueByCustomId("winners"), 
                    interaction.getComponentData().getModalValueByCustomId("prize"), 
                    interaction.getComponentData().getModalValueByCustomId("description"), 
                    lv, interaction.getEffectiveLocale());

            // attempt giveaway creation
            long id = bot.getGiveawayManager().sendGiveaway(g, interaction.getGuildId(), interaction.getChannelId());

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
            return new AutocompleteCallback<>(bot.getDatabase().getGiveawaysByChannel(interaction.getChannelId())
                    .stream().limit(25).map(g -> new Choice<>(g.getPrize(), g.getMessageId()+"")).collect(Collectors.toList()));
        }
        return new DeferredCallback(false);
    }
    
    private InteractionResponse onButton(Interaction interaction)
    {
        String customId = interaction.getComponentData().getCustomId();
        if(customId.equalsIgnoreCase(GiveawayManager.ENTER_BUTTON_ID))
        {
            long id = interaction.getMessage().getIdLong();
            Giveaway g = bot.getDatabase().getGiveaway(id);
            if(g == null)
                return GBCommand.respondError(LocalizedMessage.ERROR_GIVEAWAY_ENDED.getLocalizedMessage(interaction.getEffectiveLocale()));
            
            // sanity check
            if(g.getGuildId() != interaction.getGuildId() || g.getChannelId() != interaction.getChannelId())
                log.debug(String.format("Giveaway guild/channel ids don't match for giveaway %d! Giveaway: %d/%d Interaction: %d/%d", g.getMessageId(), g.getGuildId(), g.getChannelId(), interaction.getGuildId(), interaction.getChannelId()));
            
            int entered = bot.getDatabase().addEntry(id, interaction.getUser());
            if(entered >= 0)
                return new MessageCallback(bot.getGiveawayManager().renderGiveaway(g, entered), true);
            else
                return new MessageCallback(new SentMessage.Builder()
                        .setReferenceMessage(id)
                        .setContent(LocalizedMessage.ERROR_GIVEAWAY_ALREADY_ENTERED.getLocalizedMessage(interaction.getEffectiveLocale()))
                        .addComponent(new ActionRowComponent(new ButtonComponent(ButtonComponent.Style.DANGER, 
                                LocalizedMessage.GIVEAWAY_LEAVE.getLocalizedMessage(interaction.getEffectiveLocale()), 
                                GiveawayManager.LEAVE_BUTTON_ID + ":" + id)))
                        .setEphemeral(true).build());
        }
        else if(customId.toLowerCase().startsWith(GiveawayManager.LEAVE_BUTTON_ID.toLowerCase()))
        {
            try
            {
                long id = Long.parseLong(customId.split(":")[1]);
                Giveaway g = bot.getDatabase().getGiveaway(id);
                return new MessageCallback(new SentMessage.Builder()
                        .setContent(g == null ? Constants.ERROR + " " + LocalizedMessage.ERROR_GIVEAWAY_ENDED.getLocalizedMessage(interaction.getEffectiveLocale()) 
                                    : bot.getDatabase().removeEntry(id, interaction.getUser()) ? Constants.YAY + " " + LocalizedMessage.SUCCESS_LEAVE.getLocalizedMessage(interaction.getEffectiveLocale()) 
                                    : Constants.ERROR + " " + LocalizedMessage.ERROR_GIVEAWAY_NOT_ENTERED.getLocalizedMessage(interaction.getEffectiveLocale()))
                        .removeComponents().setEphemeral(true).build(), true);
            }
            catch(ArrayIndexOutOfBoundsException | NumberFormatException ex){}
        }
        else if(interaction.getChannelId() == bot.getControlChannel())
        {
            switch(customId.toLowerCase())
            {
                case "view-statistics":
                    long total = Runtime.getRuntime().totalMemory() / 1024 / 1024;
                    long used = total - (Runtime.getRuntime().freeMemory() / 1024 / 1024);
                    return new MessageCallback(new SentMessage.Builder()
                            .setContent("```css"
                                    + "\nUptime: " + FormatUtil.secondsToTime(ManagementFactory.getRuntimeMXBean().getUptime() / 1000).replace("*", "")
                                    + "\nMemory: " + used + "mb / " + total + "mb"
                                    + "\nGiveaways: " + bot.getDatabase().countAllGiveaways()
                                    + "\nAvg Req: " + (interaction.getClient().getMetrics().getOrDefault("TotalTime", 0L) / interaction.getClient().getMetrics().getOrDefault("TotalRequests", 1L) * 1e-9)
                                    + "\nMetrics: " + interaction.getClient().getMetrics()
                                    + "\n```")
                            .setEphemeral(true).build());
                case "shutdown":
                    return new MessageCallback(new SentMessage.Builder().setContent("This will shut down the bot. Are you sure?")
                            .addComponent(new ActionRowComponent(new ButtonComponent(ButtonComponent.Style.DANGER, "Shut Down", "actual-shutdown")))
                            .setEphemeral(true).build());
                case "actual-shutdown":
                    bot.shutdown(interaction.getUser().getUsername() + "#" + interaction.getUser().getDiscriminator() + " (" + interaction.getUser().getIdLong() + ")" );
                    return new MessageCallback(new SentMessage.Builder().setContent("Attempting shutdown...").setEphemeral(true).removeComponents().build(), true);
                    
            }
        }
        return GBCommand.respondError(LocalizedMessage.ERROR_GENERIC.getLocalizedMessage(interaction.getEffectiveLocale()));
    }
}
