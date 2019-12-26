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

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.List;
import javax.annotation.CheckReturnValue;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageActivity;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.Request;
import net.dv8tion.jda.api.requests.Response;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.pagination.ReactionPaginationAction;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.AbstractMessage;
import net.dv8tion.jda.internal.entities.DataMessage;
import net.dv8tion.jda.internal.entities.GuildImpl;
import net.dv8tion.jda.internal.entities.ReceivedMessage;
import net.dv8tion.jda.internal.entities.TextChannelImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.config.AuthorizationConfig;
import net.dv8tion.jda.internal.utils.config.MetaConfig;
import net.dv8tion.jda.internal.utils.config.SessionConfig;
import net.dv8tion.jda.internal.utils.config.ThreadingConfig;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.Bag;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class RestJDA 
{
    private final JDAImpl internalJDA;
    
    public RestJDA(String token)
    {
        AuthorizationConfig authConfig = new AuthorizationConfig(AccountType.BOT, token);
        SessionConfig sessConfig = null;
        ThreadingConfig threConfig = null;
        MetaConfig metaConfig = null;
        
        internalJDA = new JDAImpl(authConfig, sessConfig, threConfig, metaConfig);
        
        /*fakeJDA = new JDAImpl(AccountType.BOT, // AccountType accountType
                token, // String token
                null, // SessionController controller
                new OkHttpClient.Builder().build(), // OkHttpClient httpClient
                null, // WebSocketFactory wsFactory
                null, // ScheduledThreadPoolExecutor rateLimitPool
                null, // ScheduledExecutorService gatewayPool
                null, // ExecutorService callbackPool
                false, // boolean autoReconnect
                false, // boolean audioEnabled
                false, // boolean useShutdownHook
                false, // boolean bulkDeleteSplittingEnabled
                true, // boolean retryOnTimeout
                false, // boolean enableMDC
                true, // boolean shutdownRateLimitPool
                true, // boolean shutdownGatewayPool
                true, // boolean shutdownCallbackPool
                5, // int poolSize
                900, // int maxReconnectDelay
                null, // ConcurrentMap<String, String> contextMap
                EnumSet.allOf(CacheFlag.class)); // EnumSet<CacheFlag> cacheFlags*/
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
    
    /*@CheckReturnValue
    public RestAction<MessageJson> getMessageById(long channelId, long messageId)
    {
        Route.CompiledRoute route = Route.Messages.GET_MESSAGE.compile(Long.toString(channelId), Long.toString(messageId));
        return new RestAction<MessageJson>(internalJDA, route)
        {
            @Override
            protected void handleResponse(Response response, Request<MessageJson> request)
            {
                if (response.isOk())
                    request.onSuccess(new MessageJson(response.getObject()));
                else
                    request.onFailure(response);
            }
        };
    }*/
    
    @CheckReturnValue
    public RestReactionPaginationAction getReactionUsers(long channelId, long messageId, String code)
    {
        return new RestReactionPaginationAction(new RestMessage(internalJDA, messageId, channelId), code);
    }
}
