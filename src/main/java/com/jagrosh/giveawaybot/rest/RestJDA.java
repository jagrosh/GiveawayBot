/*
 * Copyright 2018 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.giveawaybot.rest;

import javax.annotation.CheckReturnValue;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.GuildImpl;
import net.dv8tion.jda.internal.entities.TextChannelImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.config.AuthorizationConfig;
import net.dv8tion.jda.internal.utils.config.MetaConfig;
import net.dv8tion.jda.internal.utils.config.SessionConfig;
import net.dv8tion.jda.internal.utils.config.ThreadingConfig;

import java.util.concurrent.Executors;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class RestJDA 
{
    private final JDAImpl internalJDA;
    
    public RestJDA(String token)
    {
        AuthorizationConfig authConfig = new AuthorizationConfig(token);
        SessionConfig sessConfig = SessionConfig.getDefault();
        ThreadingConfig threConfig = ThreadingConfig.getDefault();
        threConfig.setRateLimitPool(Executors.newScheduledThreadPool(5, r -> { return new Thread(r, "Giveaway Message Update"); }), true);
        MetaConfig metaConfig = MetaConfig.getDefault();
        
        internalJDA = new JDAImpl(authConfig, sessConfig, threConfig, metaConfig);
    }
    
    @CheckReturnValue
    public RestMessageAction editMessage(long channelId, long messageId, Message newContent)
    {
        Checks.notNull(newContent, "message");
        Route.CompiledRoute route = Route.Messages.EDIT_MESSAGE.compile(Long.toString(channelId), Long.toString(messageId));
        return new RestMessageAction(internalJDA, route, new TextChannelImpl(channelId, new GuildImpl(internalJDA, 0))).apply(newContent);
    }
    
    @CheckReturnValue
    public RestMessageAction sendMessage(long channelId, String msg)
    {
        return sendMessage(channelId, new MessageBuilder().append(msg).build());
    }
    
    @CheckReturnValue
    public RestMessageAction sendMessage(long channelId, Message msg)
    {
        Checks.notNull(msg, "Message");
        Route.CompiledRoute route = Route.Messages.SEND_MESSAGE.compile(Long.toString(channelId));
        return new RestMessageAction(internalJDA, route, new TextChannelImpl(channelId, new GuildImpl(internalJDA, 0))).apply(msg);
    }
    
    @CheckReturnValue
    public RestReactionPaginationAction getReactionUsers(long channelId, long messageId, String code)
    {
        return new RestReactionPaginationAction(new RestMessage(internalJDA, messageId, channelId), code);
    }
}
