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

import com.jagrosh.giveawaybot.Constants;
import com.jagrosh.giveawaybot.GiveawayBot;
import com.jagrosh.giveawaybot.GiveawayException;
import com.jagrosh.giveawaybot.data.GuildSettings;
import com.jagrosh.giveawaybot.entities.EmojiParser;
import com.jagrosh.giveawaybot.entities.LocalizedMessage;
import com.jagrosh.giveawaybot.entities.PremiumLevel;
import com.jagrosh.giveawaybot.util.OtherUtil;
import com.jagrosh.interactions.command.ApplicationCommand;
import com.jagrosh.interactions.command.ApplicationCommandOption;
import com.jagrosh.interactions.entities.Embed;
import com.jagrosh.interactions.entities.Permission;
import com.jagrosh.interactions.entities.SentMessage;
import com.jagrosh.interactions.entities.WebLocale;
import com.jagrosh.interactions.receive.CommandInteractionDataOption;
import com.jagrosh.interactions.receive.Interaction;
import com.jagrosh.interactions.responses.InteractionResponse;
import com.jagrosh.interactions.responses.MessageCallback;
import java.awt.Color;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class SettingsCmd extends GBCommand
{
    private final static int MAX_BUTTON_TEXT_LENGTH = 32;
    
    public SettingsCmd(GiveawayBot bot)
    {
        super(bot);
        ApplicationCommandOption showCmd = new ApplicationCommandOption(ApplicationCommandOption.Type.SUB_COMMAND, "show", "show current settings", false);
        ApplicationCommandOption group = new ApplicationCommandOption(ApplicationCommandOption.Type.SUB_COMMAND_GROUP, "set", "set settings", false);
        ApplicationCommandOption colorCmd = new ApplicationCommandOption(ApplicationCommandOption.Type.SUB_COMMAND, "color", "set giveaway embed color", false);
        colorCmd.addOptions(new ApplicationCommandOption(ApplicationCommandOption.Type.STRING, "hex", "hex code or standard color name", true));
        ApplicationCommandOption logCmd = new ApplicationCommandOption(ApplicationCommandOption.Type.SUB_COMMAND, "logchannel", "sets the log channel", false);
        logCmd.addOptions(new ApplicationCommandOption(ApplicationCommandOption.Type.CHANNEL, "channel", "channel to log to", true));
        ApplicationCommandOption buttonCmd = new ApplicationCommandOption(ApplicationCommandOption.Type.SUB_COMMAND, "emoji", "set giveaway button emoji and text", false);
        buttonCmd.addOptions(new ApplicationCommandOption(ApplicationCommandOption.Type.STRING, "emoji", "emoji or button text", true, 1, 64, false));
        //ApplicationCommandOption roleCmd = new ApplicationCommandOption(ApplicationCommandOption.Type.SUB_COMMAND, "role", "set giveaway manager role", false);
        //roleCmd.addOptions(new ApplicationCommandOption(ApplicationCommandOption.Type.ROLE, "role", "role that can create and manage giveaways", true));
        group.addOptions(colorCmd, buttonCmd/*, roleCmd*/, logCmd);
        this.app = new ApplicationCommand.Builder()
                .setType(ApplicationCommand.Type.CHAT_INPUT)
                .setName(bot.getCommandPrefix() + "settings")
                .setDescription("show or modify settings")
                .addOptions(showCmd, group)
                .setDmPermission(false)
                .setDefaultPermissions(Permission.MANAGE_GUILD)
                .build();
    }
    
    @Override
    protected InteractionResponse gbExecute(Interaction interaction) throws GiveawayException
    {
        // permission check
        // this one is intentionally different from standard check for creating giveaways
        if(!interaction.getMember().hasPermission(Permission.MANAGE_GUILD))
            throw new GiveawayException(LocalizedMessage.ERROR_USER_PERMISSIONS);
        
        WebLocale wl = interaction.getEffectiveLocale();
        
        CommandInteractionDataOption group = interaction.getCommandData().getOptions().get(0);
        switch(group.getName())
        {
            case "set":
                CommandInteractionDataOption cmd = group.getOptions().get(0);
                switch(cmd.getName())
                {
                    //case "role":
                        //long id = cmd.getOptionByName("role").getIdValue();
                        //bot.getDatabase().setGuildManager(interaction.getGuildId(), id);
                        //return respondSuccess(LocalizedMessage.SUCCESS_SETTINGS_ROLE.getLocalizedMessage(wl, "<@&" + id + ">"));
                    case "color":
                        String col = cmd.getOptionByName("hex").getStringValue();
                        Color color = OtherUtil.parseColor(col);
                        if(color == null)
                            return respondError(LocalizedMessage.ERROR_INVALID_COLOR.getLocalizedMessage(wl, col));
                        bot.getDatabase().setGuildColor(interaction.getGuildId(), color);
                        return respondSuccess(LocalizedMessage.SUCCESS_SETTINGS_COLOR.getLocalizedMessage(wl, "#" + Integer.toHexString(color.getRGB() & 0xFFFFFF).toUpperCase()));
                    case "emoji":
                        String em = cmd.getOptionByName("emoji").getStringValue();
                        EmojiParser.ParsedEntryButton pe = bot.getGiveawayManager().getEmojiManager().parse(em);
                        if(pe.isFree() || bot.getDatabase().getPremiumLevel(interaction.getGuildId()).customEmojis)
                        {
                            if(pe.text != null && pe.text.length() > MAX_BUTTON_TEXT_LENGTH)
                                return respondError(LocalizedMessage.ERROR_INVALID_EMOJI_LENGTH.getLocalizedMessage(wl, MAX_BUTTON_TEXT_LENGTH));
                            bot.getDatabase().setGuildEmoji(interaction.getGuildId(), pe.render());
                            return respondSuccess(LocalizedMessage.SUCCESS_SETTINGS_EMOJI.getLocalizedMessage(wl, pe.render()));
                        }
                        else return respondError(LocalizedMessage.ERROR_INVALID_EMOJI_CHOICE.getLocalizedMessage(wl, bot.getGiveawayManager().getEmojiManager().getFreeEmoji()));
                    case "logchannel":
                        long channel = cmd.getOptionByName("channel").getIdValue();
                        bot.getDatabase().setGuildLogChannel(interaction.getGuildId(), channel);
                        return respondSuccess(LocalizedMessage.SUCCESS_SETTINGS_LOGCHANNEL.getLocalizedMessage(wl, "<#" + channel + ">"));
                    default:
                        return respondError("Unknown settings command.");
                }
            case "show":
                GuildSettings gs = bot.getDatabase().getSettings(interaction.getGuildId());
                String text = LocalizedMessage.INFO_SETTINGS_OWNER.getLocalizedMessage(wl) + ": <@" + gs.getOwnerId() + ">\n" 
                        + LocalizedMessage.INFO_SETTINGS_PREMIUM.getLocalizedMessage(wl) + ": **" + bot.getDatabase().getPremiumLevel(gs.getGuildId(), gs.getOwnerId()) + "**\n" 
                        //+ LocalizedMessage.INFO_SETTINGS_ROLE.getLocalizedMessage(wl) + ": " + (gs.getManagerRoleId() == 0L ? "N/A" : "<@&" + gs.getManagerRoleId() + ">") + "\n"
                        + LocalizedMessage.INFO_SETTINGS_EMOJI.getLocalizedMessage(wl) + ": " + gs.getEmoji() + "\n"
                        + LocalizedMessage.INFO_SETTINGS_LOGCHANNEL.getLocalizedMessage(wl) + ": <#" + gs.getLogChannelId() + ">\n"
                        + LocalizedMessage.INFO_SETTINGS_LOCALE.getLocalizedMessage(wl) + ": " + gs.getLocale().getTextualName() + "\n\n"
                        + LocalizedMessage.INFO_SETTINGS_ETC.getLocalizedMessage(wl);
                return new MessageCallback(new SentMessage.Builder()
                        .setContent(Constants.YAY + " **GiveawayBot** " + LocalizedMessage.INFO_SETTINGS.getLocalizedMessage(wl))
                        .addEmbed(new Embed.Builder()
                                .setColor(gs.getColor())
                                .setDescription(text).build()).build());
            default:
                return respondError("Unknown settings command.");
        }
    }
    
}
