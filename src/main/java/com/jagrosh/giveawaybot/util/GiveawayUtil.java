/*
 * Copyright 2019 John Grosh (john.a.grosh@gmail.com).
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

import com.jagrosh.giveawaybot.Constants;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class GiveawayUtil
{
    public static <T> List<T> selectWinners(Set<T> set, int winners)
    {
        return selectWinners(new ArrayList<>(set), winners);
    }
    
    public static <T> List<T> selectWinners(List<T> list, int winners)
    {
        List<T> winlist = new LinkedList<>();
        List<T> pullist = new LinkedList<>(list);
        for(int i=0; i<winners && !pullist.isEmpty(); i++)
        {
            winlist.add(pullist.remove((int)(Math.random()*pullist.size())));
        }
        return winlist;
    }
    
    public static void getSingleWinner(Message message, Consumer<User> success, Runnable failure, ExecutorService threadpool)
    {
        threadpool.submit(() -> {
            try {
                MessageReaction mr = message.getReactions().stream().filter(r -> r.getReactionEmote().getName().equals(Constants.TADA)).findAny().orElse(null);
                List<User> users = new LinkedList<>();
                mr.retrieveUsers().stream().distinct().filter(u -> !u.isBot()).forEach(u -> users.add(u));
                if(users.isEmpty())
                    failure.run();
                else
                    success.accept(users.get((int)(Math.random()*users.size())));
            } catch(Exception e) {
                failure.run();
            }
        });
    }
}
