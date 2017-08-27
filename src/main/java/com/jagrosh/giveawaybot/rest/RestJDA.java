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

import java.util.List;
import javax.annotation.CheckReturnValue;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.impl.JDAImpl;
import net.dv8tion.jda.core.entities.impl.MessageImpl;
import net.dv8tion.jda.core.requests.Request;
import net.dv8tion.jda.core.requests.Response;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.requests.Route;
import net.dv8tion.jda.core.utils.Checks;
import okhttp3.OkHttpClient;
import org.json.JSONObject;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class RestJDA {
    
    private final JDAImpl fakeJDA = new JDAImpl(AccountType.BOT, new OkHttpClient.Builder(), null, false, false, false, false, 2, 900);
    
    public RestJDA(String token)
    {
        fakeJDA.setToken(token);
    }
    
    @CheckReturnValue
    public RestAction<MessageJson> editMessage(String channelId, String messageId, Message newContent)
    {
        Checks.notNull(newContent, "message");
        if (!newContent.getEmbeds().isEmpty())
        {
            MessageEmbed embed = newContent.getEmbeds().get(0);
            Checks.check(embed.isSendable(AccountType.BOT),
                    "Provided Message contains an embed with a length greater than %d characters, which is the max for %s accounts!",
                    MessageEmbed.EMBED_MAX_LENGTH_BOT, AccountType.BOT);
        }
        JSONObject json = ((MessageImpl) newContent).toJSONObject();
        Route.CompiledRoute route = net.dv8tion.jda.core.requests.Route.Messages.EDIT_MESSAGE.compile(channelId, messageId);
        return new RestAction<MessageJson>(fakeJDA, route, json)
        {
            @Override
            protected void handleResponse(Response response, Request<MessageJson> request)
            {
                if (response.isOk())
                {
                    request.onSuccess(new MessageJson(response.getObject()));
                }
                else
                {
                    request.onFailure(response);
                }
            }
        };
    }
    
    @CheckReturnValue
    public RestAction<MessageJson> sendMessage(String channelId, String msg)
    {
        return sendMessage(channelId, new MessageBuilder().append(msg).build());
    }
    
    @CheckReturnValue
    public RestAction<MessageJson> sendMessage(String channelId, Message msg)
    {
        Checks.notNull(msg, "Message");

        if (!msg.getEmbeds().isEmpty())
        {
            MessageEmbed embed = msg.getEmbeds().get(0);
            Checks.check(embed.isSendable(AccountType.BOT),
                "Provided Message contains an embed with a length greater than %d characters, which is the max for %s accounts!",
                    MessageEmbed.EMBED_MAX_LENGTH_BOT, AccountType.BOT);
        }

        Route.CompiledRoute route = Route.Messages.SEND_MESSAGE.compile(channelId);
        JSONObject json = ((MessageImpl) msg).toJSONObject();
        return new RestAction<MessageJson>(fakeJDA, route, json)
        {
            @Override
            protected void handleResponse(Response response, Request<MessageJson> request)
            {
                if (response.isOk())
                {
                    request.onSuccess(new MessageJson(response.getObject()));
                }
                else
                {
                    request.onFailure(response);
                }
            }
        };
    }
    
    @CheckReturnValue
    public RestAction<MessageJson> getMessageById(String channelId, String messageId)
    {
        Checks.notEmpty(messageId, "Provided messageId");

        Route.CompiledRoute route = Route.Messages.GET_MESSAGE.compile(channelId, messageId);
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
