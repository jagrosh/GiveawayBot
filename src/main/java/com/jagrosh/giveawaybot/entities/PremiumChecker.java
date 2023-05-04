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
package com.jagrosh.giveawaybot.entities;

import com.jagrosh.giveawaybot.Constants;
import com.jagrosh.giveawaybot.data.CachedUser;
import com.jagrosh.giveawaybot.data.Database;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class PremiumChecker
{
    private final Logger log = LoggerFactory.getLogger(PremiumChecker.class);
    private final String botToken;
    private final Database database;
    private final WebhookLog webhook;
    private final ScheduledExecutorService schedule;
    
    private JDA jda;
    
    public PremiumChecker(Database database, WebhookLog webhook, String botToken)
    {
        this.database = database;
        this.webhook = webhook;
        this.botToken = botToken;
        this.schedule = Executors.newSingleThreadScheduledExecutor();
    }
    
    public void start() throws LoginException, InterruptedException
    {
        webhook.send(WebhookLog.Level.INFO, Constants.TADA + " Starting premium checker...");
        jda = JDABuilder.createDefault(botToken, GatewayIntent.GUILD_MEMBERS)
                .setStatus(OnlineStatus.ONLINE)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setChunkingFilter(ChunkingFilter.ALL)
                .disableCache(EnumSet.of(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, 
                        CacheFlag.EMOJI, CacheFlag.VOICE_STATE))
                .build();
        jda.addEventListener(new EventListener()
        {
            @Override
            public void onEvent(GenericEvent event)
            {
                if(event instanceof ReadyEvent)
                {
                    webhook.send(WebhookLog.Level.INFO, Constants.TADA + " Checker ready! `" + jda.getGuildById(PremiumLevel.SERVER_ID).getMemberCache().size() + "`");
                    schedule.scheduleWithFixedDelay(() -> update(), 0, 10, TimeUnit.MINUTES);
                }
            }
        });
    }
    
    public void shutdown()
    {
        schedule.shutdown();
        jda.shutdown();
    }
    
    private void update()
    {
        try
        {
            log.debug("Updating premium users...");

            // summary of changes
            StringBuilder sb = new StringBuilder();

            // make a map of all users that have premium levels
            Map<Long, Pair<Member, PremiumLevel>> added = new HashMap<>();

            // to start, assume that all users need to be added
            for(PremiumLevel p: PremiumLevel.values())
            {
                Role role = jda.getGuildById(PremiumLevel.SERVER_ID).getRoleById(p.roleId);
                if(role == null)
                    continue;
                jda.getGuildById(PremiumLevel.SERVER_ID).getMembersWithRoles(role).forEach(m -> added.put(m.getUser().getIdLong(), Pair.of(m, p)));
            }

            // now, update the removed and changed lists
            List<CachedUser> premiumUsers = database.getAllPremiumUsers();
            for(CachedUser old: premiumUsers)
            {
                long userId = old.getId();
                PremiumLevel oldLevel = old.getPremiumLevel();
                Pair<Member, PremiumLevel> newLevel = added.get(userId);

                // if the member no longer has premium, add to removed list and update db
                if(newLevel == null)
                {
                    database.removePremium(userId);
                    sb.append("\n- ").append(userId).append(" ").append(oldLevel);
                    log.info(String.format("Removed %d from %s", userId, oldLevel));
                }

                // if the member needs premium level changed, add to changed list and update db
                else if(newLevel.getRight() != oldLevel)
                {
                    net.dv8tion.jda.api.entities.User u = newLevel.getLeft().getUser();
                    database.updatePremiumLevel(userId, u.getName(), u.getDiscriminator(), u.getAvatarId(), newLevel.getRight());
                    sb.append("\n# ").append(userId).append(" ").append(oldLevel).append(" -> ").append(newLevel.getRight());
                    log.info(String.format("Changed %d from %s to %s", userId, oldLevel, newLevel.getRight()));
                }

                //else if the level has not channged, then we do nothing except remove from 'added'
                //  do nothing

                // remove from map to find the members that are newly getting premium
                added.remove(userId);
            }

            // for any remaining additions in the map, update
            for(Pair<Member, PremiumLevel> pair: added.values())
            {
                net.dv8tion.jda.api.entities.User u = pair.getLeft().getUser();
                database.updatePremiumLevel(u.getIdLong(), u.getName(), u.getDiscriminator(), u.getAvatarId(), pair.getRight());
                sb.append("\n+ ").append(u.getId()).append(" ").append(pair.getRight());
                log.info(String.format("Added %d to %s", u.getIdLong(), pair.getRight()));
            }

            // send to webhook, if anything changed
            if(sb.length() > 1)
                webhook.send(WebhookLog.Level.INFO, "**Premium Update** ```diff" + sb.toString() + "\n```");
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
