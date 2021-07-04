package com.jagrosh.giveawaybot.entities;

import com.jagrosh.giveawaybot.Constants;

public class Emoji
{

    private final String raw;

    public Emoji(final String value)
    {
        this.raw = value;
    }

    public String getRaw()
    {
        return raw;
    }

    public boolean isSet()
    {
        return raw != null;
    }

    public String getReaction()
    {
        return raw == null ? Constants.TADA : raw;
    }

    public String getDisplay()
    {
        if (raw == null)
            return Constants.TADA;
        if (raw.contains(":"))
            return "<" + raw + ">";
        return raw;
    }
}
