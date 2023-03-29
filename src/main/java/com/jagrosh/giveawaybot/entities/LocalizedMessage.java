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
package com.jagrosh.giveawaybot.entities;

import com.jagrosh.interactions.entities.WebLocale;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public enum LocalizedMessage
{
    // permission checks
    ERROR_USER_PERMISSIONS("error.permissions.user"),
    ERROR_USER_PERMS_OR_ROLE("error.permissions.perms_or_role"),
    ERROR_USER_PERMS_NO_ROLE("error.permissions.perms_no_role"),
    ERROR_BOT_PERMISSIONS("error.permissions.bot"),
    
    // input validation
    ERROR_INVALID_TIME_FORMAT("error.invalid_time.format"), 
    ERROR_INVALID_TIME_MIN("error.invalid_time.min"), 
    ERROR_INVALID_TIME_MAX("error.invalid_time.max"),
    ERROR_MAXIMUM_GIVEAWAYS_GUILD("error.max_giveaways.guild"),
    ERROR_MAXIMUM_GIVEAWAYS_CHANNEL("error.max_giveaways.channel"),
    ERROR_INVALID_WINNERS_FORMAT("error.invalid_winners.format"), 
    ERROR_INVALID_WINNERS_MAX("error.invalid_winners.max"),
    ERROR_INVALID_PRIZE_LENGTH("error.invalid_prize.length"),
    ERROR_INVALID_DESCR_LENGTH("error.invalid_descr.length"),
    ERROR_INVALID_EMOJI_LENGTH("error.invalid_emoji.length"),
    ERROR_INVALID_EMOJI_CHOICE("error.invalid_emoji.choice"),
    
    // other errors
    ERROR_NO_DMS("error.no_dms"),
    ERROR_GIVEAWAY_COOLDOWN("error.giveaway_cooldown"),
    ERROR_GIVEAWAY_ENDED("error.giveaway_ended"),
    ERROR_INVALID_COLOR("error.invalid_color"),
    ERROR_INVALID_ID("error.invalid_id"),
    ERROR_MESSAGE_NOT_FOUND("error.message_not_found"),
    ERROR_INVALID_MESSAGE("error.invalid_message"),
    ERROR_GIVEAWAY_NOT_FOUND("error.giveaway_not_found"),
    ERROR_GIVEAWAY_ALREADY_ENTERED("error.giveaway_already_entered"),
    ERROR_GIVEAWAY_NOT_ENTERED("error.giveaway_not_entered"),
    ERROR_REROLL_MESSAGE_OPTION("error.reroll_message_option"),
    ERROR_GENERIC_CREATION("error.generic.creation"),
    ERROR_GENERIC_ENTER("error.generic.enter"),
    ERROR_GENERIC_ENDING("error.generic.ending"),
    ERROR_GENERIC_REROLL("error.generic.reroll"),
    ERROR_GENERIC_DELETE("error.generic.delete"),
    ERROR_GENERIC("error.generic"),
    
    // warnings
    WARNING_NO_GIVEAWAYS("warning.no_giveaways"),
    WARNING_CURRENTLY_UNSUPPORTED("warning.not_supported"),
    WARNING_NO_ENTRIES("warning.no_entries"),
    
    // successful responses
    SUCCESS_GIVEAWAY_CREATED("success.giveaway_created"),
    SUCCESS_ENTERED("success.entered"),
    SUCCESS_LEAVE("success.leave"),
    SUCCESS_WINNER("success.winner"),
    SUCCESS_SETTINGS_ROLE("success.settings.role"),
    SUCCESS_SETTINGS_COLOR("success.settings.color"),
    SUCCESS_SETTINGS_EMOJI("success.settings.emoji"),
    SUCCESS_SETTINGS_LOGCHANNEL("success.settings.logchannel"),
    SUCCESS_GIVEAWAY_ENDED("success.giveaway_ended"),
    SUCCESS_GIVEAWAY_REROLL("success.giveaway_reroll"),
    SUCCESS_GIVEAWAY_DELETE("success.giveaway_delete"),
    
    // info
    INFO_INVITE("info.invite"),
    INFO_ABOUT("info.about"),
    INFO_ABOUT_BRIEF("info.about.brief"),
    INFO_ABOUT_LONG("info.about.long"),
    INFO_ABOUT_STATS("info.about.stats"),
    INFO_ABOUT_STATS_SERVERS("info.about.stats.servers"),
    INFO_ABOUT_STATS_GIVEAWAYS("info.about.stats.giveaways"),
    INFO_ABOUT_LINKS("info.about.links"),
    INFO_ABOUT_LINKS_WEBSITE("info.about.links.website"),
    INFO_ABOUT_LINKS_INVITE("info.about.links.invite"),
    INFO_ABOUT_LINKS_SUPPORT("info.about.links.support"),
    INFO_SETTINGS("info.settings"),
    INFO_SETTINGS_OWNER("info.settings.owner"),
    INFO_SETTINGS_PREMIUM("info.settings.premium"),
    INFO_SETTINGS_EMOJI("info.settings.emoji"),
    INFO_SETTINGS_ROLE("info.settings.role"),
    INFO_SETTINGS_COLOR("info.settings.color"),
    INFO_SETTINGS_LOGCHANNEL("info.settings.logchannel"),
    INFO_SETTINGS_LOCALE("info.settings.locale"),
    INFO_SETTINGS_ETC("info.settings.etc"),
    INFO_HELP_GENERAL("info.help.general"),
    INFO_HELP_CREATION("info.help.creation"),
    INFO_HELP_MANIPULATION("info.help.manipulation"),
    INFO_HELP_SETTINGS("info.help.settings"),
    
    GIVEAWAY_LEAVE("giveaway.leave"),
    GIVEAWAY_ENDS("giveaway.ends"),
    GIVEAWAY_ENDED("giveaway.ended"),
    GIVEAWAY_HOSTED("giveaway.hosted"),
    GIVEAWAY_ENTRIES("giveaway.entries"),
    GIVEAWAY_WINNERS("giveaway.winners"),
    GIVEAWAY_SUMMARY("giveaway.summary");

    private final static String FILENAME = "localization/messages";
    private final String key;
    
    private LocalizedMessage(String key)
    {
        this.key = key;
    }
    
    public String getLocalizedMessage(WebLocale locale, Object... obj)
    {
        ResourceBundle res = ResourceBundle.getBundle(FILENAME, new Locale(locale.getCode()));
        MessageFormat form = new MessageFormat(res.getString(key));
        return form.format(obj);
    }
}
