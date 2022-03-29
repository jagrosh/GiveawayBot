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
package com.jagrosh.giveawaybot.data;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
@Entity
@Table(name = "GIVEAWAY_ENTRIES")
public class GiveawayEntries
{
    @Id
    @Column(name = "GIVEAWAY_ID")
    private long giveawayId;
    
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<Long> users = new HashSet<>();

    public long getGiveawayId()
    {
        return giveawayId;
    }

    public void setGiveawayId(long giveawayId)
    {
        this.giveawayId = giveawayId;
    }

    public Set<Long> getUsers()
    {
        return users;
    }

    public void setUsers(Set<Long> users)
    {
        this.users = users;
    }
    
    public void addUser(long userId)
    {
        this.users.add(userId);
    }
}
