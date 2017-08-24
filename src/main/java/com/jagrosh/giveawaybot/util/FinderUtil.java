/*
 * Copyright 2016 jagrosh.
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
package com.jagrosh.giveawaybot.util;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.entities.impl.JDAImpl;
import net.dv8tion.jda.core.entities.impl.UserImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author jagrosh
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class FinderUtil {

    public final static String USER_MENTION = "<@!?(\\d{17,20})>";
    public final static String DISCORD_ID = "\\d{17,20}";

    public static List<User> findUsers(String query, JDA jda) {
        String id;
        String discriminator = null;
        if (query.matches(USER_MENTION)) {
            id = query.replaceAll(USER_MENTION, "$1");
            if (id.equals("1")) {
                UserImpl clyde = new UserImpl(1L, (JDAImpl) jda);
                clyde.setDiscriminator("0000");
                clyde.setBot(true);
                clyde.setName("Clyde");
                return Collections.singletonList(clyde);
            }
            User u = jda.getUserById(id);
            if (u != null)
                return Collections.singletonList(u);
        } else if (query.matches(DISCORD_ID)) {
            id = query;
            User u = jda.getUserById(id);
            if (u != null)
                return Collections.singletonList(u);
        } else if (query.matches("^.*#\\d{4}$")) {
            discriminator = query.substring(query.length() - 4);
            query = query.substring(0, query.length() - 5).trim();
        }
        ArrayList<User> exact = new ArrayList<>();
        ArrayList<User> wrongcase = new ArrayList<>();
        ArrayList<User> startswith = new ArrayList<>();
        ArrayList<User> contains = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (User u : jda.getUsers()) {
            if (discriminator != null && !u.getDiscriminator().equals(discriminator))
                continue;
            if (u.getName().equals(query))
                exact.add(u);
            else if (exact.isEmpty() && u.getName().equalsIgnoreCase(query))
                wrongcase.add(u);
            else if (wrongcase.isEmpty() && u.getName().toLowerCase().startsWith(lowerQuery))
                startswith.add(u);
            else if (startswith.isEmpty() && u.getName().toLowerCase().contains(lowerQuery))
                contains.add(u);
        }
        if (!exact.isEmpty())
            return exact;
        if (!wrongcase.isEmpty())
            return wrongcase;
        if (!startswith.isEmpty())
            return startswith;
        return contains;
    }

    public static List<User> findUsers(String query, Guild guild) {
        return findMembers(query, guild).stream().map(Member::getUser).collect(Collectors.toList());
    }

    public static List<Member> findMembers(String query, Guild guild) {
        String id;
        String discrim = null;
        if (query.matches(USER_MENTION)) {
            id = query.replaceAll(USER_MENTION, "$1");
            Member m = guild.getMemberById(id);
            if (m != null)
                return Collections.singletonList(m);
        } else if (query.matches(DISCORD_ID)) {
            id = query;
            Member m = guild.getMemberById(id);
            if (m != null)
                return Collections.singletonList(m);
        } else if (query.matches("^.*#\\d{4}$")) {
            discrim = query.substring(query.length() - 4);
            query = query.substring(0, query.length() - 5).trim();
        }
        ArrayList<Member> exact = new ArrayList<>();
        ArrayList<Member> wrongcase = new ArrayList<>();
        ArrayList<Member> startswith = new ArrayList<>();
        ArrayList<Member> contains = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (Member m : guild.getMembers()) {
            String nickname = m.getNickname();
            String name = m.getUser().getName();
            if (discrim != null && !m.getUser().getDiscriminator().equals(discrim))
                continue;
            if (name.equals(query) || (nickname != null && nickname.equals(query)))
                exact.add(m);
            else if (exact.isEmpty() && (name.equalsIgnoreCase(query) || (nickname != null && nickname.equalsIgnoreCase(query))))
                wrongcase.add(m);
            else if (wrongcase.isEmpty() && (name.toLowerCase().startsWith(lowerQuery) || (nickname != null && nickname.toLowerCase().startsWith(lowerQuery))))
                startswith.add(m);
            else if (startswith.isEmpty() && (name.toLowerCase().contains(lowerQuery) || (nickname != null && nickname.toLowerCase().contains(lowerQuery))))
                contains.add(m);
        }
        if (!exact.isEmpty())
            return exact;
        if (!wrongcase.isEmpty())
            return wrongcase;
        if (!startswith.isEmpty())
            return startswith;
        return contains;
    }

    public static List<User> findBannedUsers(String query, Guild guild) {
        List<User> bans;
        try {
            bans = guild.getBans().complete();
        } catch (Exception e) {
            return null;
        }
        String id;
        String discrim = null;
        if (query.matches(USER_MENTION)) {
            id = query.replaceAll(USER_MENTION, "$1");
            User u = guild.getJDA().getUserById(id);
            if (bans.contains(u))
                return Collections.singletonList(u);
            for (User user : bans)
                if (user.getId().equals(id))
                    return Collections.singletonList(user);
        } else if (query.matches(DISCORD_ID)) {
            id = query;
            User u = guild.getJDA().getUserById(id);
            if (u != null && bans.contains(u))
                return Collections.singletonList(u);
            for (User user : bans)
                if (user.getId().equals(id))
                    return Collections.singletonList(user);
        } else if (query.matches("^.*#\\d{4}$")) {
            discrim = query.substring(query.length() - 4);
            query = query.substring(0, query.length() - 5).trim();
        }
        ArrayList<User> exact = new ArrayList<>();
        ArrayList<User> wrongcase = new ArrayList<>();
        ArrayList<User> startswith = new ArrayList<>();
        ArrayList<User> contains = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (User u : bans) {
            if (discrim != null && !u.getDiscriminator().equals(discrim))
                continue;
            if (u.getName().equals(query))
                exact.add(u);
            else if (exact.isEmpty() && u.getName().equalsIgnoreCase(query))
                wrongcase.add(u);
            else if (wrongcase.isEmpty() && u.getName().toLowerCase().startsWith(lowerQuery))
                startswith.add(u);
            else if (startswith.isEmpty() && u.getName().toLowerCase().contains(lowerQuery))
                contains.add(u);
        }
        if (!exact.isEmpty())
            return exact;
        if (!wrongcase.isEmpty())
            return wrongcase;
        if (!startswith.isEmpty())
            return startswith;
        return contains;
    }

    public static List<TextChannel> findTextChannel(String query, Guild guild) {
        String id;
        if (query.matches("<#\\d+>")) {
            id = query.replaceAll("<#(\\d+)>", "$1");
            TextChannel tc = guild.getJDA().getTextChannelById(id);
            if (tc != null && tc.getGuild().equals(guild))
                return Collections.singletonList(tc);
        } else if (query.matches(DISCORD_ID)) {
            id = query;
            TextChannel tc = guild.getJDA().getTextChannelById(id);
            if (tc != null && tc.getGuild().equals(guild))
                return Collections.singletonList(tc);
        }
        ArrayList<TextChannel> exact = new ArrayList<>();
        ArrayList<TextChannel> wrongCase = new ArrayList<>();
        ArrayList<TextChannel> startsWith = new ArrayList<>();
        ArrayList<TextChannel> contains = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        guild.getTextChannels().forEach((tc) -> {
            if (tc.getName().equals(lowerQuery))
                exact.add(tc);
            else if (tc.getName().equalsIgnoreCase(lowerQuery) && exact.isEmpty())
                wrongCase.add(tc);
            else if (tc.getName().toLowerCase().startsWith(lowerQuery) && wrongCase.isEmpty())
                startsWith.add(tc);
            else if (tc.getName().toLowerCase().contains(lowerQuery) && startsWith.isEmpty())
                contains.add(tc);
        });
        if (!exact.isEmpty())
            return exact;
        if (!wrongCase.isEmpty())
            return wrongCase;
        if (!startsWith.isEmpty())
            return startsWith;
        return contains;
    }

    public static List<VoiceChannel> findVoiceChannel(String query, Guild guild) {
        String id;
        if (query.matches("<#\\d+>")) {
            id = query.replaceAll("<#(\\d+)>", "$1");
            VoiceChannel vc = guild.getJDA().getVoiceChannelById(id);
            if (vc != null && vc.getGuild().equals(guild))
                return Collections.singletonList(vc);
        } else if (query.matches(DISCORD_ID)) {
            id = query;
            VoiceChannel vc = guild.getJDA().getVoiceChannelById(id);
            if (vc != null && vc.getGuild().equals(guild))
                return Collections.singletonList(vc);
        }
        ArrayList<VoiceChannel> exact = new ArrayList<>();
        ArrayList<VoiceChannel> wrongCase = new ArrayList<>();
        ArrayList<VoiceChannel> startsWith = new ArrayList<>();
        ArrayList<VoiceChannel> contains = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        guild.getVoiceChannels().forEach((vc) -> {
            if (vc.getName().equals(lowerQuery))
                exact.add(vc);
            else if (vc.getName().equalsIgnoreCase(lowerQuery) && exact.isEmpty())
                wrongCase.add(vc);
            else if (vc.getName().toLowerCase().startsWith(lowerQuery) && wrongCase.isEmpty())
                startsWith.add(vc);
            else if (vc.getName().toLowerCase().contains(lowerQuery) && startsWith.isEmpty())
                contains.add(vc);
        });
        if (!exact.isEmpty())
            return exact;
        if (!wrongCase.isEmpty())
            return wrongCase;
        if (!startsWith.isEmpty())
            return startsWith;
        return contains;
    }

    public static List<Role> findRole(String query, Guild guild) {
        String id;
        if (query.matches("<@&\\d+>")) {
            id = query.replaceAll("<@&(\\d+)>", "$1");
            Role r = guild.getRoleById(id);
            if (r != null)
                return Collections.singletonList(r);
        }
        if (query.matches("[Ii][Dd]\\s*:\\s*\\d+")) {
            id = query.replaceAll("[Ii][Dd]\\s*:\\s*(\\d+)", "$1");
            Role r = guild.getRoleById(id);
            if (r != null)
                return Collections.singletonList(r);
        } else if (query.matches(DISCORD_ID)) {
            id = query;
            Role r = guild.getRoleById(id);
            if (r != null)
                return Collections.singletonList(r);
        }
        ArrayList<Role> exact = new ArrayList<>();
        ArrayList<Role> wrongcase = new ArrayList<>();
        ArrayList<Role> startswith = new ArrayList<>();
        ArrayList<Role> contains = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        guild.getRoles().forEach((role) -> {
            if (role.getName().equals(query))
                exact.add(role);
            else if (role.getName().equalsIgnoreCase(query) && exact.isEmpty())
                wrongcase.add(role);
            else if (role.getName().toLowerCase().startsWith(lowerQuery) && wrongcase.isEmpty())
                startswith.add(role);
            else if (role.getName().toLowerCase().contains(lowerQuery) && startswith.isEmpty())
                contains.add(role);
        });
        if (!exact.isEmpty())
            return exact;
        if (!wrongcase.isEmpty())
            return wrongcase;
        if (!startswith.isEmpty())
            return startswith;
        return contains;
    }

    public static List<Guild> findGuild(String query, JDA jda) {
        String id;
        if (query.matches("[Ii][Dd]\\s*:\\s*\\d+")) {
            id = query.replaceAll("[Ii][Dd]\\s*:\\s*(\\d+)", "$1");
            Guild g = jda.getGuildById(id);
            if (g != null)
                return Collections.singletonList(g);
        } else if (query.matches(DISCORD_ID)) {
            id = query;
            Guild g = jda.getGuildById(id);
            if (g != null)
                return Collections.singletonList(g);
        }
        ArrayList<Guild> exact = new ArrayList<>();
        ArrayList<Guild> wrongcase = new ArrayList<>();
        ArrayList<Guild> startswith = new ArrayList<>();
        ArrayList<Guild> contains = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        jda.getGuilds().forEach((guild) -> {
            if (guild.getName().equals(query))
                exact.add(guild);
            else if (guild.getName().equalsIgnoreCase(query) && exact.isEmpty())
                wrongcase.add(guild);
            else if (guild.getName().toLowerCase().startsWith(lowerQuery) && wrongcase.isEmpty())
                startswith.add(guild);
            else if (guild.getName().toLowerCase().contains(lowerQuery) && startswith.isEmpty())
                contains.add(guild);
        });
        if (!exact.isEmpty())
            return exact;
        if (!wrongcase.isEmpty())
            return wrongcase;
        if (!startswith.isEmpty())
            return startswith;
        return contains;
    }
}
