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
package com.jagrosh.giveawaybot.commands;

import com.jagrosh.giveawaybot.Constants;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class InviteCommand extends Command {

    public InviteCommand() {
        name = "invite";
        help = "shows how to invite the bot";
        guildOnly = false;
    }
    
    @Override
    protected void execute(CommandEvent event) {
        event.reply(Constants.YAY+" Hello! I'm **GiveawayBot**! I help to make giveaways quick and easy!\n"
                + "You can add me to your server with this link:\n\n"
                + "\uD83D\uDD17 **<"+Constants.INVITE+">**\n\n"
                + "Check out my commands by typing `!ghelp`");
    }
    
}
