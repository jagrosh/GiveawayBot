## GiveawayBot
Hold giveaways quickly and easily on your Discord server! GiveawayBot is powered by [JDA](https://github.com/DV8FromTheWorld/JDA/) and [JDA-Utilities](https://github.com/jagrosh/JDA-Utilities).<br>
<br>
![Example](http://i.imgur.com/bMjO8UA.png)

## Invite
If you'd like to add **GiveawayBot** to your server, use the following link:<br>
🔗 **https://giveawaybot.party/invite**

## Usage
* **!ghelp** - Provides the bot's commands via Direct Message
* **!gcreate** - Interactive giveaway setup
* **!gstart \<time> [winners] [prize]** - Starts a new giveaway in the current channel. Users can react with a 🎉 to enter the giveaway. The time can be in seconds, minutes, hours, or days. Specify the time unit with an "s", "m", "h", or "d", for example `30s` or `2h`. If you include a number of winners, it must be in the form #w, for example `2w` or `5w`.
* **!greroll [messsageId]** - Re-rolls a winner. If you provided a message ID, it rerolls the giveaway at that ID. If you leave it blank, it looks in the current channel for the most recent giveaway and rerolls from that.
* **!gend [messageId]** - Ends a giveaway immediately. If you provided a message ID it will end the giveaway at that ID. If you leave it blank, it looks in the current channel for the most recent giveaway and ends that.
* **!glist** - Lists currently-running giveaways on the server.

## Suggests, Bugs, Feature Requests
If you find bugs or would like to suggest features, join my bot development server here: https://invite.gg/jagrosh

## Self-Hosting
Self-hosting your own copy of this bot is not supported; the source code is provided here so users and other bot developers can see how the bot functions. No help will be provided for editing, compiling, or building any code in this repository, and any changes must be documented as per the [license](https://github.com/jagrosh/GiveawayBot/blob/master/LICENSE).
