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

import net.dv8tion.jda.core.requests.Request;
import net.dv8tion.jda.core.requests.Response;
import net.dv8tion.jda.core.requests.Route;
import org.json.JSONArray;

import java.util.LinkedList;
import java.util.List;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.requests.restaction.pagination.PaginationAction;
import org.json.JSONObject;

/**
 * {@link net.dv8tion.jda.core.requests.restaction.pagination.PaginationAction PaginationAction}
 * that paginates the endpoint {@link net.dv8tion.jda.core.requests.Route.Messages#GET_REACTION_USERS Route.Messages.GET_REACTION_USERS}.
 *
 * <p><b>Must provide not-null {@link net.dv8tion.jda.core.entities.MessageReaction MessageReaction} to compile a valid
 * pagination route.</b>
 *
 * <h2>Limits:</h2>
 * Minimum - 1
 * <br>Maximum - 100
 *
 * @since  3.1
 * @author Florian Spieß
 */
public class EditedReactionPaginationAction extends PaginationAction<Long, EditedReactionPaginationAction>
{

    /**
     * Creates a new PaginationAction instance
     *
     * @param jda
     * @param code
     * @param messageId
     * @param channelId
     */
    public EditedReactionPaginationAction(JDA jda, String code, String channelId, String messageId)
    {
        super(jda, Route.Messages.GET_REACTION_USERS.compile(channelId, messageId, code), 1, 100, 100);
    }

    @Override
    protected Route.CompiledRoute finalizeRoute()
    {
        Route.CompiledRoute route = super.finalizeRoute();

        String after = null;
        String lim = String.valueOf(getLimit());
        Long las = this.last;
        if (las != null)
            after = Long.toString(las);

        route = route.withQueryParams("limit", lim);

        if (after != null)
            route = route.withQueryParams("after", after);

        return route;
    }

    @Override
    protected void handleResponse(Response response, Request<List<Long>> request)
    {
        if (!response.isOk())
        {
            request.onFailure(response);
            return;
        }
        final JSONArray array = response.getArray();
        final List<Long> users = new LinkedList<>();
        for (int i = 0; i < array.length(); i++)
        {
            JSONObject user = array.getJSONObject(i);
            long id = user.getLong("id");
            last = id;
            if(user.has("bot") && user.getBoolean("bot"))
                continue;
            users.add(id);
            if (useCache)
                cached.add(id);
        }

        request.onSuccess(users);
    }

}
