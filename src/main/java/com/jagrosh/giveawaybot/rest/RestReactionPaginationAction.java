/*
 *     Copyright 2015-2017 Austin Keener & Michael Ritter & Florian Spieß
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jagrosh.giveawaybot.rest;

import net.dv8tion.jda.api.requests.Request;
import net.dv8tion.jda.api.requests.Response;

import java.util.LinkedList;
import java.util.List;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ParsingException;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.entities.UserImpl;
import net.dv8tion.jda.internal.requests.restaction.pagination.ReactionPaginationActionImpl;

/**
 * Edits the ReactionPaginationActionImpl to not try to build entities
 * 
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class RestReactionPaginationAction extends ReactionPaginationActionImpl
{
    public RestReactionPaginationAction(Message message, String code) 
    {
        super(message, code);
    }
    
    @Override
    protected void handleSuccess(Response response, Request<List<User>> request)
    {
        final DataArray array = response.getArray();
        final List<User> users = new LinkedList<>();
        for (int i = 0; i < array.length(); i++)
        {
            try
            {
                final User user = createFakeUser(array.getObject(i));
                if(!user.isBot())
                {
                    users.add(user);
                    if (useCache)
                        cached.add(user);
                }
                last = user;
                lastKey = last.getIdLong();
            }
            catch (ParsingException | NullPointerException e)
            {
                LOG.warn("Encountered exception in ReactionPagination", e);
            }
        }

        request.onSuccess(users);
    }
    
    private User createFakeUser(DataObject object)
    {
        UserImpl u = new UserImpl(object.getLong("id"), null);
        u.setBot(object.hasKey("bot") && object.getBoolean("bot"));
        u.setFake(true);
        return u;
    }
}
