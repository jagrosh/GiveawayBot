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

import com.jagrosh.giveawaybot.Bot;
import com.jagrosh.giveawaybot.Constants;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.JDAUtilitiesInfo;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AboutCommand extends Command {

    private final Bot bot;
    public AboutCommand(Bot bot) {
        this.bot = bot;
        name = "about";
        aliases = new String[]{"info"};
        help = "shows info about the bot";
        guildOnly = false;
        botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }
    
    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        MessageBuilder mb = new MessageBuilder();
        mb.append(Constants.YAY+" All about **GiveawayBot** "+Constants.YAY);
        //eb.setThumbnail("http://i.imgur.com/sCEbmKa.png");
        eb.setTitle("Hold giveaways quickly and easily!");
        eb.setDescription("Hello! I'm **GiveawayBot**, and I'm here to make it as easy as possible to hold "
                + "giveaways on your Discord server! I was created by [**jagrosh**#4824](http://jagrosh.com) "
                + "(<@113156185389092864>) using the [JDA]("+JDAInfo.GITHUB+") library ("+JDAInfo.VERSION+") and "
                + "[JDA-Utilities]("+JDAUtilitiesInfo.GITHUB+") ("+JDAUtilitiesInfo.VERSION+"). Check out my "
                + "commands by typing `!ghelp`, and checkout my website at **http://giveawaybot.party**.");
        eb.addField("\uD83D\uDCCA Stats", event.getClient().getTotalGuilds()+" servers\n"+event.getJDA().getShardInfo().getShardTotal()+" shards", true);
        eb.addField("\uD83C\uDF89 Giveaways", bot.getGiveaways().size()+" right now!", true);
        eb.addField("\uD83C\uDF10 Links", "[Website]("+Constants.WEBSITE+")\n[Invite]("+Constants.INVITE+")\n[Support](https://discord.gg/0p9LSGoRLu6Pet0k)", true);
        eb.setFooter("Last restart", null);
        eb.setTimestamp(Constants.STARTUP);
        eb.setColor(Constants.BLURPLE);
        mb.setEmbed(eb.build());
        event.getChannel().sendMessage(mb.build()).queue();
    }
    
}
