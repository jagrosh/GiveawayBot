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
package com.jagrosh.giveawaybot;

import com.jagrosh.giveawaybot.data.Database;
import com.jagrosh.giveawaybot.data.Giveaway;
import java.time.Instant;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class DatabaseTest
{
    private final static long USER = 1L, GUILD = 2L, CHANNEL = 3L, MESSAGE = 4L;
    //Database db;
    
    @Before
    public void initialize()
    {
        //Database db = new Database("", "testuser", "testpass");
    }
    
    @Test
    public void testGiveawayCreation()
    {
        Giveaway g = new Giveaway(USER, Instant.now(), 1, "prize", null);
        g.setMessageId(MESSAGE);
        g.setGuildId(GUILD);
        g.setChannelId(CHANNEL);
        //db.createGiveaway(g);
    }
}
