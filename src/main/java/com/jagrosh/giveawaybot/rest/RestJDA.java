/*
 * Copyright 2017 John Grosh (john.a.grosh@gmail.com).
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

import java.util.EnumSet;
import java.util.concurrent.Executors;
import javax.annotation.CheckReturnValue;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.impl.GuildImpl;
import net.dv8tion.jda.core.entities.impl.JDAImpl;
import net.dv8tion.jda.core.entities.impl.TextChannelImpl;
import net.dv8tion.jda.core.requests.Request;
import net.dv8tion.jda.core.requests.Response;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.requests.Route;
import net.dv8tion.jda.core.requests.restaction.MessageAction;
import net.dv8tion.jda.core.utils.Checks;
import net.dv8tion.jda.core.utils.cache.CacheFlag;
import okhttp3.OkHttpClient;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class RestJDA {
    
    private final JDAImpl fakeJDA;
    
    public RestJDA(String token)
    {
        fakeJDA = new JDAImpl(AccountType.BOT, // AccountType accountType
                token, // String token
                null, // SessionController controller
                new OkHttpClient.Builder().build(), // OkHttpClient httpClient
                null, // WebSocketFactory wsFactory
                null, // ScheduledThreadPoolExecutor rateLimitPool
                null, // ExecutorService callbackPool
                false, // boolean autoReconnect
                false, // boolean audioEnabled
                false, // boolean useShutdownHook
                false, // boolean bulkDeleteSplittingEnabled
                true, // boolean retryOnTimeout
                false, // boolean enableMDC
                true, // boolean shutdownRateLimitPool
                true, // boolean shutdownCallbackPool
                5, // int poolSize
                900, // int maxReconnectDelay
                null, // ConcurrentMap<String, String> contextMap
                EnumSet.allOf(CacheFlag.class)); // EnumSet<CacheFlag> cacheFlags
    }
    
    @CheckReturnValue
    public MessageAction editMessage(long channelId, long messageId, Message newContent)
    {
        Checks.notNull(newContent, "message");
        Route.CompiledRoute route = Route.Messages.EDIT_MESSAGE.compile(Long.toString(channelId), Long.toString(messageId));
        return new MessageAction(fakeJDA, route, new TextChannelImpl(channelId, new GuildImpl(fakeJDA, 0))).apply(newContent);
    }
    
    @CheckReturnValue
    public MessageAction sendMessage(long channelId, String msg)
    {
        return sendMessage(channelId, new MessageBuilder().append(msg).build());
    }
    
    @CheckReturnValue
    public MessageAction sendMessage(long channelId, Message msg)
    {
        Checks.notNull(msg, "Message");
        Route.CompiledRoute route = Route.Messages.SEND_MESSAGE.compile(Long.toString(channelId));
        return new MessageAction(fakeJDA, route, new TextChannelImpl(channelId, new GuildImpl(fakeJDA, 0))).apply(msg);
    }
    
    @CheckReturnValue
    public RestAction<MessageJson> getMessageById(long channelId, long messageId)
    {
        Route.CompiledRoute route = Route.Messages.GET_MESSAGE.compile(Long.toString(channelId), Long.toString(messageId));
        return new RestAction<MessageJson>(fakeJDA, route)
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
    }
    
    @CheckReturnValue
    public EditedReactionPaginationAction getReactionUsers(String channelId, String messageId, String code)
    {
        return new EditedReactionPaginationAction(fakeJDA, code, channelId, messageId);
    }
}
