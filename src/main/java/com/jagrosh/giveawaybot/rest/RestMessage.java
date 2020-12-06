/*
 * Copyright 2019 John Grosh (john.a.grosh@gmail.com).
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

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.List;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageActivity;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.pagination.ReactionPaginationAction;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.GuildImpl;
import net.dv8tion.jda.internal.entities.TextChannelImpl;
import org.apache.commons.collections4.Bag;

/**
 *
 * @author user
 */
public class RestMessage implements Message
{
    private final static String UNSUPPORTED = "Not supported.";
    
    private final JDAImpl jda;
    private final long id;
    private final long channelId;
    
    protected RestMessage(JDAImpl jda, long id, long channelId)
    {
        this.jda = jda;
        this.id = id;
        this.channelId = channelId;
    }
    
    @Override
    public JDA getJDA() 
    {
        return jda;
    }

    @Override
    public long getIdLong() 
    {
        return id;
    }
    
    
    @Override
    public MessageChannel getChannel() 
    {
        return new TextChannelImpl(channelId, new GuildImpl(jda, 0));
    }
    
    @Override
    public List<User> getMentionedUsers() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public Bag<User> getMentionedUsersBag() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public List<TextChannel> getMentionedChannels() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public Bag<TextChannel> getMentionedChannelsBag() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public List<Role> getMentionedRoles() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public Bag<Role> getMentionedRolesBag() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public List<Member> getMentionedMembers(Guild guild) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public List<Member> getMentionedMembers() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public List<IMentionable> getMentions(MentionType... mts) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public boolean isMentioned(IMentionable im, MentionType... mts) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public boolean mentionsEveryone() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public boolean isEdited() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public OffsetDateTime getTimeEdited() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public User getAuthor() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public Member getMember() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public String getJumpUrl() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public String getContentDisplay() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public String getContentRaw() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public String getContentStripped() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public List<String> getInvites() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public String getNonce() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public boolean isFromType(ChannelType ct) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public ChannelType getChannelType() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public boolean isWebhookMessage() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public PrivateChannel getPrivateChannel() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public TextChannel getTextChannel() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public Category getCategory() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public Guild getGuild() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public List<Attachment> getAttachments() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public List<MessageEmbed> getEmbeds() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public List<Emote> getEmotes() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public Bag<Emote> getEmotesBag() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public List<MessageReaction> getReactions() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public boolean isTTS() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public MessageActivity getActivity() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public MessageAction editMessage(CharSequence cs) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public MessageAction editMessage(MessageEmbed me) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public MessageAction editMessage(Message msg) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public MessageAction editMessageFormat(String string, Object... os) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public AuditableRestAction<Void> delete() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public boolean isPinned() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public RestAction<Void> pin() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public RestAction<Void> unpin() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public RestAction<Void> addReaction(Emote emote) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public RestAction<Void> addReaction(String string) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public RestAction<Void> clearReactions() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public RestAction<Void> removeReaction(Emote emote) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public RestAction<Void> removeReaction(Emote emote, User user) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public RestAction<Void> removeReaction(String string) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public RestAction<Void> removeReaction(String string, User user) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public ReactionPaginationAction retrieveReactionUsers(Emote emote) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public ReactionPaginationAction retrieveReactionUsers(String string) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public MessageReaction.ReactionEmote getReactionByUnicode(String string) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public MessageReaction.ReactionEmote getReactionById(String string) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public MessageReaction.ReactionEmote getReactionById(long l) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public MessageType getType() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public RestAction<Void> clearReactions(String arg0) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public RestAction<Void> clearReactions(Emote arg0) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public AuditableRestAction<Void> suppressEmbeds(boolean arg0) { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public boolean isSuppressedEmbeds() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public EnumSet<MessageFlag> getFlags() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public RestAction<Message> crosspost() { throw new UnsupportedOperationException(UNSUPPORTED); }

    @Override
    public Message getReferencedMessage() { throw new UnsupportedOperationException(UNSUPPORTED); }
}
