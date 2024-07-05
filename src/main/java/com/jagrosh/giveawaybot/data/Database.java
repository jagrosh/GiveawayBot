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
package com.jagrosh.giveawaybot.data;

import com.jagrosh.giveawaybot.entities.PremiumLevel;
import com.jagrosh.giveawaybot.util.OtherUtil;
import com.jagrosh.interactions.entities.Guild;
import com.jagrosh.interactions.entities.User;
import com.jagrosh.interactions.entities.WebLocale;
import java.awt.Color;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Database
{
    private final EntityManagerFactory emf;
    private final EntityManager em;
    private final Map<Long, GiveawayEntries> cachedEntries = new HashMap<>();
    private final Map<Long, Giveaway> cachedGiveawaysReadonly = new HashMap<>();
    private final ScheduledExecutorService cacheCombiner = Executors.newSingleThreadScheduledExecutor();
    
    public Database(String host, String user, String pass)
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("javax.persistence.jdbc.user", user);
        properties.put("javax.persistence.jdbc.password", pass);
        emf = Persistence.createEntityManagerFactory(host, properties);
        em = emf.createEntityManager();
        em.getMetamodel().managedType(CachedUser.class);
        em.getMetamodel().managedType(Giveaway.class);
        em.getMetamodel().managedType(GiveawayEntries.class);
        em.getMetamodel().managedType(GuildSettings.class);
        cacheCombiner.scheduleWithFixedDelay(() -> syncEntries(), 60, 60, TimeUnit.SECONDS);
    }
    
    public void shutdown()
    {
        cacheCombiner.shutdown();
        syncEntries();
        em.close();
        emf.close();
    }
    
    // guild settings
    public GuildSettings getSettings(long guildId)
    {
        GuildSettings gs = em.find(GuildSettings.class, guildId);
        return gs == null ? new GuildSettings(guildId) : gs;
    }
    
    public synchronized void setAutomaticGuildSettings(long guildId, Instant now, Guild guild)
    {
        GuildSettings gs = em.find(GuildSettings.class, guildId);
        em.getTransaction().begin();
        if(gs == null)
        {
            gs = new GuildSettings();
            gs.setGuildId(guildId);
            gs.setLocale(WebLocale.UNKNOWN);
            em.persist(gs);
        }
        gs.setLatestRetrieval(now);
        if(guild != null)
        {
            gs.setOwnerId(guild.getOwnerId());
            if(guild.getPreferredLocale() != null && guild.getPreferredLocale() != WebLocale.UNKNOWN)
                gs.setLocale(guild.getPreferredLocale());
            /*if(gs.getLocale() == null || gs.getLocale() == WebLocale.UNKNOWN)
            {
                gs.setLocale(guild.getPreferredLocale());
            }
            /*if(gs.getManagerRoleId() == 0L)
            {
                GuildRole legacy = guild.getRoles().stream()
                        .filter(r -> r.getName().equalsIgnoreCase("giveaways"))
                        .findFirst().orElse(null);
                if(legacy != null)
                    gs.setManagerRoleId(legacy.getIdLong());
            }*/
        }
        em.getTransaction().commit();
    }
    
    public synchronized void setGuildColor(long guildId, Color color)
    {
        GuildSettings gs = em.find(GuildSettings.class, guildId);
        em.getTransaction().begin();
        if(gs == null)
        {
            gs = new GuildSettings();
            gs.setGuildId(guildId);
            em.persist(gs);
        }
        gs.setColor(color);
        em.getTransaction().commit();
    }

    public synchronized void setGuildLogChannel(long guildId, long logChannel) {
        GuildSettings gs = em.find(GuildSettings.class, guildId);
        em.getTransaction().begin();
        if(gs == null) {
            gs = new GuildSettings();
            gs.setGuildId(guildId);
            em.persist(gs);
        }
        gs.setLogChannelId(logChannel);
        em.getTransaction().commit();

    }
    
    public synchronized void setGuildEmoji(long guildId, String emoji)
    {
        GuildSettings gs = em.find(GuildSettings.class, guildId);
        em.getTransaction().begin();
        if(gs == null)
        {
            gs = new GuildSettings();
            gs.setGuildId(guildId);
            em.persist(gs);
        }
        gs.setEmoji(emoji);
        em.getTransaction().commit();
    }
    
    // giveaways
    public Giveaway getGiveaway(long id)
    {
        if(cachedGiveawaysReadonly.containsKey(id))
            return cachedGiveawaysReadonly.get(id);
        Giveaway g = em.find(Giveaway.class, id);
        cachedGiveawaysReadonly.put(id, g);
        return g;
        //return em.find(Giveaway.class, id);
    }
    
    public List<Giveaway> getGiveawaysByGuild(long guildId)
    {
        return em.createNamedQuery("Giveaway.getAllFromGuild", Giveaway.class).setParameter("guildId", guildId).getResultList();
    }
    
    public List<Giveaway> getGiveawaysByChannel(long channelId)
    {
        return em.createNamedQuery("Giveaway.getAllFromChannel", Giveaway.class).setParameter("channelId", channelId).getResultList();
    }
    
    public long countGiveawaysByChannel(long channelId)
    {
        return em.createNamedQuery("Giveaway.countAllFromChannel", Long.class).setParameter("channelId", channelId).getSingleResult();
    }
    
    public long countGiveawaysByGuild(long guildId)
    {
        return em.createNamedQuery("Giveaway.countAllFromGuild", Long.class).setParameter("guildId", guildId).getSingleResult();
    }
    
    public long countAllGiveaways()
    {
        return em.createNamedQuery("Giveaway.countAll", Long.class).getSingleResult();
    }
    
    public List<Giveaway> getGiveawaysEndingBefore(Instant time)
    {
        return em.createNamedQuery("Giveaway.getAllEndingBefore", Giveaway.class).setParameter("endTime", time.getEpochSecond()).getResultList();
    }
    
    public synchronized void createGiveaway(Giveaway giveaway)
    {
        em.getTransaction().begin();
        em.persist(giveaway);
        em.getTransaction().commit();
    }
    
    public synchronized void removeGiveaway(long id)
    {
        cachedGiveawaysReadonly.remove(id);
        Giveaway g = em.find(Giveaway.class, id);
        if(g != null)
        {
            em.getTransaction().begin();
            em.remove(g);
            em.getTransaction().commit();
        }
        GiveawayEntries ge = em.find(GiveawayEntries.class, id);
        if(ge != null)
        {
            em.getTransaction().begin();
            em.remove(ge);
            em.getTransaction().commit();
        }
    }
    
    
    // entries
    public synchronized void updateUser(User user)
    {
        // update cached user
        CachedUser u = em.find(CachedUser.class, user.getIdLong());
        
        // short circuit if data is up to date
        if(u != null
            && OtherUtil.strEquals(user.getUsername(), u.getUsername()) 
            && OtherUtil.strEquals(user.getDiscriminator(), u.getDiscriminator()) 
            && OtherUtil.strEquals(user.getAvatar(), u.getAvatar()))
            return;
        
        em.getTransaction().begin();
        if(u == null)
        {
            u = new CachedUser();
            u.setId(user.getIdLong());
            em.persist(u);
        }
        u.setUsername(user.getUsername());
        u.setDiscriminator(user.getDiscriminator());
        u.setAvatar(user.getAvatar());
        em.getTransaction().commit();
    }
    
    public CachedUser getUser(long userId)
    {
        return em.find(CachedUser.class, userId);
    }
    
    public synchronized int addEntry(long giveawayId, User user)
    {
        // update user
        updateUser(user);
        
        // get entries
        GiveawayEntries ge = getEntries(giveawayId);
        
        // short circuit if user has already entered
        if(ge != null && ge.getUsers().contains(user.getIdLong()))
            return -1;
        
        if(ge == null)
        {
            ge = new GiveawayEntries();
            ge.setGiveawayId(giveawayId);
            em.getTransaction().begin();
            em.persist(ge);
            em.getTransaction().commit();
        }
        cachedEntries.put(giveawayId, ge);
        ge.addUser(user.getIdLong());
        return ge.getUsers().size();
    }
    
    public synchronized boolean removeEntry(long giveawayId, User user)
    {
        // update user
        updateUser(user);
        
        // update entries
        GiveawayEntries ge = getEntries(giveawayId);
        
        // short circuit if user is not already entered
        if(ge == null || !ge.getUsers().contains(user.getIdLong()))
            return false;
        
        //em.getTransaction().begin();
        cachedEntries.put(giveawayId, ge);
        ge.removeUser(user.getIdLong());
        //em.getTransaction().commit();
        return true;
    }
    
    public synchronized void syncEntries()
    {
        em.getTransaction().begin();
        cachedEntries.values().forEach(e -> em.merge(e));
        em.getTransaction().commit();
        cachedEntries.clear();
    }
    
    public List<CachedUser> getEntriesList(long giveawayId)
    {
        GiveawayEntries ge = getEntries(giveawayId);
        if(ge == null)
            return Collections.emptyList();
        return ge.getUsers().stream()
                .map(u -> em.find(CachedUser.class, u))
                .collect(Collectors.toList());
    }
    
    private GiveawayEntries getEntries(long giveawayId)
    {
        return cachedEntries.containsKey(giveawayId) ? cachedEntries.get(giveawayId) : em.find(GiveawayEntries.class, giveawayId);
    }
    
    
    // premium
    public PremiumLevel getPremiumLevel(long guildId)
    {
        return getPremiumLevel(guildId, 0L);
    }
    
    public PremiumLevel getPremiumLevel(long guildId, long userId)
    {
        // get premium level of user
        CachedUser user = em.find(CachedUser.class, userId);
        PremiumLevel userPremium = user == null ? PremiumLevel.NONE : user.getPremiumLevel();
        
        // get premium level of guild
        GuildSettings guild = em.find(GuildSettings.class, guildId);
        long ownerId = guild == null ? 0L : guild.getOwnerId();
        CachedUser owner = em.find(CachedUser.class, ownerId);
        PremiumLevel guildPremium = owner == null ? PremiumLevel.NONE : owner.getPremiumLevel();
                
        return userPremium.level > guildPremium.level ? userPremium : guildPremium;
    }
    
    public synchronized void updatePremiumLevel(long userId, String username, String discrim, String avatar, PremiumLevel premium)
    {
        CachedUser u = em.find(CachedUser.class, userId);
        em.getTransaction().begin();
        if(u == null)
        {
            u = new CachedUser();
            u.setId(userId);
            em.persist(u);
        }
        u.setUsername(username);
        u.setDiscriminator(discrim);
        u.setAvatar(avatar);
        u.setPremiumLevel(premium);
        em.getTransaction().commit();
    }
    
    public synchronized void removePremium(long userId)
    {
        CachedUser u = em.find(CachedUser.class, userId);
        em.getTransaction().begin();
        if(u == null)
        {
            u = new CachedUser();
            u.setId(userId);
            em.persist(u);
        }
        u.setPremiumLevel(PremiumLevel.NONE);
        em.getTransaction().commit();
    }
    
    public List<CachedUser> getAllPremiumUsers()
    {
        return em.createNamedQuery("CachedUser.findAllWithPremium", CachedUser.class).getResultList();
    }
}
