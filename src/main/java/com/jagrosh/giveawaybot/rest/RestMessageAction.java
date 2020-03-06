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

import com.jagrosh.giveawaybot.util.FormatUtil;
import java.util.List;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.requests.Request;
import net.dv8tion.jda.api.requests.Response;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.requests.restaction.MessageActionImpl;

/**
 * Changes the MessageActionImpl to filter outgoing messages, and not try to build incoming message entities
 * 
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class RestMessageAction extends MessageActionImpl
{
    private static final String CONTENT_TOO_BIG = String.format("A message may not exceed %d characters. Please limit your input!", Message.MAX_CONTENT_LENGTH);
    
    public RestMessageAction(JDA api, Route.CompiledRoute route, MessageChannel channel)
    {
        super(api, route, channel);
    }

    @Override
    protected void handleSuccess(Response response, Request<Message> request) 
    {
        request.onSuccess(null);
    }
    
    @Nonnull
    @Override
    @CheckReturnValue
    public RestMessageAction apply(final Message message)
    {
        if (message == null || message.getType() != MessageType.DEFAULT)
            return this;
        final List<MessageEmbed> embeds = message.getEmbeds();
        if (embeds != null && !embeds.isEmpty())
            embed(embeds.get(0));
        files.clear();

        return content(FormatUtil.filter(message.getContentRaw())).tts(message.isTTS());
    }
    
    @Nonnull
    @Override
    @CheckReturnValue
    public RestMessageAction tts(final boolean isTTS)
    {
        this.tts = isTTS;
        return this;
    }
    
    @Nonnull
    @Override
    @CheckReturnValue
    public RestMessageAction content(final String content)
    {
        if (content == null || content.isEmpty())
            this.content.setLength(0);
        else if (content.length() <= Message.MAX_CONTENT_LENGTH)
            this.content.replace(0, this.content.length(), content);
        else
            throw new IllegalArgumentException(CONTENT_TOO_BIG);
        return this;
    }
}
