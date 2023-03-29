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

import com.jagrosh.giveawaybot.data.CachedUser;
import com.jagrosh.giveawaybot.data.Database;
import com.jagrosh.giveawaybot.data.Giveaway;
import com.jagrosh.giveawaybot.data.GuildSettings;
import com.jagrosh.giveawaybot.entities.EmojiParser;
import com.jagrosh.giveawaybot.entities.FileUploader;
import com.jagrosh.giveawaybot.entities.LocalizedMessage;
import com.jagrosh.giveawaybot.entities.PremiumLevel;
import com.jagrosh.giveawaybot.util.FormatUtil;
import com.jagrosh.giveawaybot.util.GiveawayUtil;
import com.jagrosh.giveawaybot.util.OtherUtil;
import com.jagrosh.interactions.components.ActionRowComponent;
import com.jagrosh.interactions.components.ButtonComponent;
import com.jagrosh.interactions.components.PartialEmoji;
import com.jagrosh.interactions.entities.*;
import com.jagrosh.interactions.receive.Interaction;
import com.jagrosh.interactions.requests.RestClient;
import com.jagrosh.interactions.requests.RestClient.RestResponse;
import com.jagrosh.interactions.requests.Route;
import com.jagrosh.interactions.util.JsonUtil;
import java.awt.Color;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class GiveawayManager
{
    public final static String ENTER_BUTTON_ID = "enter-giveaway",
                               LEAVE_BUTTON_ID = "leave-giveaway";
    private final static int MINIMUM_SECONDS = 10,
                             MAX_PRIZE_LENGTH = 250,
                             MAX_DESCR_LENGTH = 1000,
                             FAILURE_COOLDOWN_TIME = 30;
    private final static Color ENDED_COLOR = new Color(0x2F3136);
    private final static Permission[] REQUIRED_PERMS = { Permission.SEND_MESSAGES, Permission.VIEW_CHANNEL, 
        Permission.READ_MESSAGE_HISTORY, Permission.EMBED_LINKS };
    
    private final Logger log = LoggerFactory.getLogger(GiveawayManager.class);
    private final ScheduledExecutorService schedule = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Map<Long,Instant> latestFailure = new HashMap<>();
    private final Database database;
    private final RestClient rest;
    private final FileUploader uploader;
    private final EmojiParser emojis;
    private final long clientId;
    
    public GiveawayManager(Database database, RestClient rest, FileUploader uploader, EmojiParser emojis, long clientId)
    {
        this.database = database;
        this.rest = rest;
        this.uploader = uploader;
        this.clientId = clientId;
        this.emojis = emojis;
    }
    
    public void start()
    {
        schedule.scheduleWithFixedDelay(() -> 
        {
            try
            {
                // end giveaways that have run out of time
                database.getGiveawaysEndingBefore(Instant.now().plusMillis(500))
                        .stream().parallel()
                        .forEach(giveaway -> endGiveaway(giveaway));
            }
            catch(Exception ex)
            {
                log.error("Exception in ending giveaways: ", ex);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
    
    public void shutdown()
    {
        schedule.shutdown();
        pool.shutdown();
    }
    
    public EmojiParser getEmojiManager()
    {
        return emojis;
    }
    
    public String getPermsLink(long guildId)
    {
        return String.format(Constants.ADMIN, Long.toString(clientId), Long.toString(guildId));
    }
    
    public boolean deleteGiveaway(Giveaway giveaway)
    {
        database.removeGiveaway(giveaway.getMessageId());
        try
        {
            RestResponse res = rest.request(Route.DELETE_MESSAGE.format(giveaway.getChannelId(), giveaway.getMessageId())).get();
            return res.isSuccess();
        }
        catch(ExecutionException | InterruptedException ex)
        {
            return false;
        }
    }
    
    public boolean endGiveaway(Giveaway giveaway)
    {
        List<CachedUser> entries = database.getEntriesList(giveaway.getMessageId());
        database.removeGiveaway(giveaway.getMessageId());
        List<CachedUser> all = new ArrayList<>(entries);
        List<CachedUser> winners = GiveawayUtil.selectWinners(all, giveaway.getWinners());
        CachedUser host = database.getUser(giveaway.getUserId());
        try
        {
            JSONObject summary = createGiveawaySummary(giveaway, host, entries, winners);
            String url = uploader.uploadFile(summary.toString(), "giveaway_summary.json");
            String summaryKey = url == null ? null : url.replaceAll(".*/(\\d+/\\d+)/.*", "$1");
            rest.request(Route.PATCH_MESSAGE.format(giveaway.getChannelId(), giveaway.getMessageId()), renderGiveaway(giveaway, entries.size(), winners, summaryKey).toJson()).get();
            rest.request(Route.POST_MESSAGE.format(giveaway.getChannelId()), renderWinnerMessage(giveaway, winners).toJson()).get();
        }
        catch(ExecutionException | InterruptedException ex)
        {
            return false;
        }
        return true;
    }
    
    public void checkAvailability(Interaction interaction, PremiumLevel level) throws GiveawayException
    {
        // apply cooldown when giveaway creation fails
        Instant latest = latestFailure.get(interaction.getGuildId());
        if(latest != null && latest.until(Instant.now(), ChronoUnit.SECONDS) < FAILURE_COOLDOWN_TIME)
            throw new GiveawayException(LocalizedMessage.ERROR_GIVEAWAY_COOLDOWN);
        
        // check bot permissions
        for(Permission p: REQUIRED_PERMS)
            if(!interaction.appHasPermission(p))
                throw new GiveawayException(LocalizedMessage.ERROR_BOT_PERMISSIONS, getPermsLink(interaction.getGuildId()));
        
        // check if the maximum number of giveaways has been reached
        long currentGiveaways = level.perChannelMaxGiveaways ? database.countGiveawaysByChannel(interaction.getChannelId()) : database.countGiveawaysByGuild(interaction.getGuildId());
        if(currentGiveaways >= level.maxGiveaways)
            throw new GiveawayException(LocalizedMessage.ERROR_MAXIMUM_GIVEAWAYS_GUILD, currentGiveaways, level.perChannelMaxGiveaways);
    }
    
    public Giveaway constructGiveaway(User user, String time, String winners, String prize, String description, PremiumLevel level, WebLocale locale) throws GiveawayException
    {
        // validate time
        int seconds = OtherUtil.parseTime(time);
        if(seconds <= 0)
            throw new GiveawayException(LocalizedMessage.ERROR_INVALID_TIME_FORMAT, time);
        if(seconds < MINIMUM_SECONDS)
            throw new GiveawayException(LocalizedMessage.ERROR_INVALID_TIME_MIN, seconds, FormatUtil.secondsToTime(MINIMUM_SECONDS));
        if(seconds > level.maxTime)
            throw new GiveawayException(LocalizedMessage.ERROR_INVALID_TIME_MAX, seconds, FormatUtil.secondsToTime(level.maxTime));
        
        // validate number of winners
        int wins = 0;
        try
        {
            wins = Integer.parseInt(winners);
        }
        catch(NumberFormatException ex)
        {
            throw new GiveawayException(LocalizedMessage.ERROR_INVALID_WINNERS_FORMAT, winners);
        }
        if(wins < 1 || wins > level.maxWinners)
            throw new GiveawayException(LocalizedMessage.ERROR_INVALID_WINNERS_MAX, wins, level.maxWinners);
        
        // validate prize and description
        if(prize.length() > MAX_PRIZE_LENGTH)
            throw new GiveawayException(LocalizedMessage.ERROR_INVALID_PRIZE_LENGTH, prize, MAX_PRIZE_LENGTH);
        if(description != null && description.length() > MAX_DESCR_LENGTH)
            throw new GiveawayException(LocalizedMessage.ERROR_INVALID_PRIZE_LENGTH, prize, MAX_PRIZE_LENGTH);
        
        return new Giveaway(user.getIdLong(), Instant.now().plusSeconds(seconds), wins, prize, description);
    }
    
    public long sendGiveaway(Giveaway giveaway, long guildId, long channelId) throws GiveawayException
    {
        try
        {
            giveaway.setGuildId(guildId);
            giveaway.setChannelId(channelId);
            SentMessage sm = renderGiveaway(giveaway, 0);
            log.debug("Attempting giveaway creation in " + guildId + ", json: " + sm.toJson());
            RestResponse res = rest.request(Route.POST_MESSAGE.format(channelId), sm.toJson()).get();
            log.debug("Attempted to create giveaway, response: " + res.getStatus() + ", " + res.getBody());
            if(!res.isSuccess())
            {
                latestFailure.put(guildId, Instant.now());
                if(res.getErrorSpecific() == 50013 || res.getErrorSpecific() == 50001)
                    throw new GiveawayException(LocalizedMessage.ERROR_BOT_PERMISSIONS, String.format(Constants.ADMIN, Long.toString(clientId), Long.toString(guildId)));
                throw new GiveawayException(LocalizedMessage.ERROR_GENERIC_CREATION);
            }
            ReceivedMessage rm = new ReceivedMessage(res.getBody());
            giveaway.setMessageId(rm.getIdLong());
            
            // sanity checks
            if(/*rm.getGuildId() != guildId ||*/ rm.getChannelId() != channelId)
            {
                log.error(String.format("There's a mismatch in data! Interaction: %d/%d Message: %d/%d", guildId, channelId, rm.getGuildId(), rm.getChannelId()));
                //throw new GiveawayException(LocalizedMessage.ERROR_GENERIC_CREATION);
            }
            if(guildId > channelId)
            {
                log.error(String.format("Odd data received; channel is older than guild! G: %d  C:%d", guildId, channelId));
            }
            
            database.createGiveaway(giveaway);
            return giveaway.getMessageId();
        }
        catch(InterruptedException | ExecutionException ex)
        {
            latestFailure.put(guildId, Instant.now());
            throw new GiveawayException(LocalizedMessage.ERROR_GENERIC_CREATION);
        }
    }
    
    public SentMessage renderGiveaway(Giveaway giveaway, int numEntries)
    {
        return renderGiveaway(giveaway, numEntries, null, null);
    }
    
    public SentMessage renderGiveaway(Giveaway giveaway, int numEntries, List<CachedUser> winners, String summaryKey)
    {
        GuildSettings gs = database.getSettings(giveaway.getGuildId());
        String message = (giveaway.getDescription() == null || giveaway.getDescription().isEmpty() ? "" : giveaway.getDescription() + "\n\n")
                + (winners == null ? LocalizedMessage.GIVEAWAY_ENDS.getLocalizedMessage(gs.getLocale()) : LocalizedMessage.GIVEAWAY_ENDED.getLocalizedMessage(gs.getLocale())) 
                    + ": <t:" + giveaway.getEndInstant().getEpochSecond() + ":R> (<t:" + giveaway.getEndInstant().getEpochSecond() + ":f>)"
                + "\n" + LocalizedMessage.GIVEAWAY_HOSTED.getLocalizedMessage(gs.getLocale()) + ": <@" + giveaway.getUserId() + ">"
                + "\n" + LocalizedMessage.GIVEAWAY_ENTRIES.getLocalizedMessage(gs.getLocale()) + ": **" + numEntries + "**"
                + "\n" + LocalizedMessage.GIVEAWAY_WINNERS.getLocalizedMessage(gs.getLocale()) + ": " + (winners == null ? "**" + giveaway.getWinners() + "**" : renderWinners(winners));
        SentMessage.Builder sb = new SentMessage.Builder()
                .addEmbed(new Embed.Builder()
                        .setTitle(giveaway.getPrize(), null)
                        .setColor(winners == null ? gs.getColor() : ENDED_COLOR)
                        .setTimestamp(giveaway.getEndInstant())
                        .setDescription(message).build());
        if(winners == null)
            sb.addComponent(new ActionRowComponent(createEntryButton(emojis.parse(gs.getEmoji()))));
        else if(summaryKey != null)
            sb.addComponent(new ActionRowComponent(new ButtonComponent(LocalizedMessage.GIVEAWAY_SUMMARY.getLocalizedMessage(gs.getLocale()), Constants.SUMMARY + "#giveaway=" + summaryKey)));
        else
            sb.removeComponents();
        return sb.build();
    }
    
    public SentMessage renderWinnerMessage(Giveaway giveaway, List<CachedUser> winners)
    {
        return new SentMessage.Builder()
                .setContent(winners.isEmpty() 
                        ? LocalizedMessage.WARNING_NO_ENTRIES.getLocalizedMessage(WebLocale.ENGLISH_US) 
                        : LocalizedMessage.SUCCESS_WINNER.getLocalizedMessage(WebLocale.ENGLISH_US, renderWinners(winners), giveaway.getPrize()))
                .setReferenceMessage(giveaway.getMessageId())
                .setAllowedMentions(new AllowedMentions(AllowedMentions.ParseType.USERS))
                .build();
    }
    
    public String renderWinners(List<CachedUser> winners)
    {
        if(winners.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for(CachedUser u: winners)
            sb.append(", <@").append(u.getId()).append(">");
        return sb.toString().substring(2);
    }
    
    private JSONObject createGiveawaySummary(Giveaway giveaway, CachedUser host, List<CachedUser> entries, List<CachedUser> winners)
    {
        return new JSONObject()
                .put("giveaway", new JSONObject()
                    .put("id", Long.toString(giveaway.getMessageId()))
                    .put("prize", giveaway.getPrize())
                    .put("desc", giveaway.getDescription())
                    .put("num_winners", giveaway.getWinners())
                    .put("host", host.toJson())
                    .put("end", giveaway.getEndTime()))
                .put("winners", JsonUtil.buildArray(winners))
                .put("entries", JsonUtil.buildArray(entries));
    }
    
    private ButtonComponent createEntryButton(EmojiParser.ParsedEntryButton pe)
    {
        return new ButtonComponent(ButtonComponent.Style.PRIMARY, pe.text, 
                    pe.hasEmoji() ? new PartialEmoji(pe.name, pe.id, pe.animated) : null, 
                    ENTER_BUTTON_ID, null, false);
    }
}
