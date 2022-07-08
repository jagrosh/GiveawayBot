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

import javax.persistence.*;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
@Entity
@Table(name = "GIVEAWAY_ENTRY_TABLE")
@NamedQueries({
    @NamedQuery(name = "GiveawayEntry.getAllForGiveaway", query = "SELECT g FROM GiveawayEntry g WHERE g.entryId.giveawayId = :giveawayId")
})
public class GiveawayEntry
{
    @EmbeddedId
    @Column(name = "ENTRY_ID")
    private EntryId entryId;
    
    @Embeddable
    public static class EntryId
    {
        private long giveawayId;
        private long userId;
    }
}
