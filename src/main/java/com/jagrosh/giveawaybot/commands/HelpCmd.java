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

import com.jagrosh.giveawaybot.GiveawayBot;
import com.jagrosh.giveawaybot.entities.LocalizedMessage;
import com.jagrosh.interactions.command.ApplicationCommand;
import com.jagrosh.interactions.entities.Embed;
import com.jagrosh.interactions.entities.SentMessage;
import com.jagrosh.interactions.entities.WebLocale;
import com.jagrosh.interactions.receive.Interaction;
import com.jagrosh.interactions.responses.InteractionResponse;
import com.jagrosh.interactions.responses.MessageCallback;
import java.awt.Color;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class HelpCmd extends GBCommand
{
    private final static String YAY = "<:yay:585696613507399692>";
    
    public HelpCmd(GiveawayBot bot)
    {
        super(bot);
        this.app = new ApplicationCommand.Builder()
                .setType(ApplicationCommand.Type.CHAT_INPUT)
                .setName(bot.getCommandPrefix() + "help")
                .setDescription("shows commands")
                .setDmPermission(false)
                .build();
    }
    
    @Override
    public InteractionResponse gbExecute(Interaction interaction)
    {
        WebLocale wl = interaction.getEffectiveLocale();
        String prefix = "/" + bot.getCommandPrefix();
        return new MessageCallback(new SentMessage.Builder()
                .setContent(YAY + " **GiveawayBot** Commands " + YAY)
                .addEmbed(new Embed.Builder()
                        .setColor(new Color(0x5865F2))
                        .addField(LocalizedMessage.INFO_HELP_GENERAL.getLocalizedMessage(wl), 
                                  "`" + prefix + "about`"
                              + "\n`" + prefix + "ping`"
                              + "\n`" + prefix + "invite`", false)
                        .addField(LocalizedMessage.INFO_HELP_CREATION.getLocalizedMessage(wl), 
                                  "`" + prefix + "start`"
                              + "\n`" + prefix + "create`", false)
                        .addField(LocalizedMessage.INFO_HELP_MANIPULATION.getLocalizedMessage(wl), 
                                  "`" + prefix + "end`"
                              + "\n`" + prefix + "delete`"
                              + "\n`" + prefix + "reroll`"
                              + "\n`" + prefix + "list`", false)
                        .addField(LocalizedMessage.INFO_HELP_SETTINGS.getLocalizedMessage(wl), 
                                  "`" + prefix + "settings show`"
                              + "\n`" + prefix + "settings set color`"
                              + "\n`" + prefix + "settings set emoji`", false)
                .build()).setEphemeral(true).build());
    }
}
