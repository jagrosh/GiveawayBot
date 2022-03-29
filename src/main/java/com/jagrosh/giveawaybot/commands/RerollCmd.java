/*
 * Copyright 2022 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.giveawaybot.commands;

import com.jagrosh.giveawaybot.GiveawayException;
import com.jagrosh.giveawaybot.GiveawayManager;
import com.jagrosh.interactions.command.ApplicationCommand;
import com.jagrosh.interactions.receive.Interaction;
import com.jagrosh.interactions.responses.InteractionResponse;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class RerollCmd extends GBCommand
{
    private final GiveawayManager gman;
    
    public RerollCmd(String prefix, GiveawayManager gman)
    {
        this.gman = gman;
        this.app = new ApplicationCommand.Builder()
                .setType(ApplicationCommand.Type.MESSAGE)
                .setName(prefix + "reroll")
                .setDescription("rerolls one new winner from a giveaway")
                .build();
    }
    
    @Override
    public InteractionResponse gbExecute(Interaction interaction) throws GiveawayException
    {
        gman.checkPermission(interaction.getMember(), interaction.getGuildId());
        
        //interaction.getMessage().get
        return respondSuccess("Not supported");
    }
}
