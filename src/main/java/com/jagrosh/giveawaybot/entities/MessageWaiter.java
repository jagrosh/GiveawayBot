/*
 * Copyright 2020 John Grosh (john.a.grosh@gmail.com).
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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class MessageWaiter implements EventListener
{
    private final Set<WaitingEvent> set = new HashSet<>();
    private final ScheduledExecutorService threadpool = Executors.newSingleThreadScheduledExecutor();
    
    @Override
    public void onEvent(GenericEvent event)
    {
        if(event instanceof GuildMessageReceivedEvent)
            onGuildMessageReceived((GuildMessageReceivedEvent) event);
    }
    
    public synchronized void waitForGuildMessageReceived(Predicate<GuildMessageReceivedEvent> condition, Consumer<GuildMessageReceivedEvent> action,
                                               long timeout, TimeUnit unit, Runnable timeoutAction)
    {
        WaitingEvent we = new WaitingEvent(condition, action);
        set.add(we);

        if(timeout > 0 && unit != null)
        {
            threadpool.schedule(() ->
            {
                if(set.remove(we) && timeoutAction != null)
                    timeoutAction.run();
            }, timeout, unit);
        }
    }
    
    public synchronized void onGuildMessageReceived(GuildMessageReceivedEvent event)
    {
        WaitingEvent[] toRemove = set.toArray(new WaitingEvent[set.size()]);

        // WaitingEvent#attempt invocations that return true have passed their condition tests
        // and executed the action. We filter the ones that return false out of the toRemove and
        // remove them all from the set.
        set.removeAll(Stream.of(toRemove).filter(i -> i.attempt(event)).collect(Collectors.toSet()));
    }
    
    private class WaitingEvent
    {
        final Predicate<GuildMessageReceivedEvent> condition;
        final Consumer<GuildMessageReceivedEvent> action;
        
        WaitingEvent(Predicate<GuildMessageReceivedEvent> condition, Consumer<GuildMessageReceivedEvent> action)
        {
            this.condition = condition;
            this.action = action;
        }
        
        boolean attempt(GuildMessageReceivedEvent event)
        {
            if(condition.test(event))
            {
                action.accept(event);
                return true;
            }
            return false;
        }
    }
}
