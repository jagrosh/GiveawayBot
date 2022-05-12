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

import com.jagrosh.giveawaybot.util.GiveawayUtil;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class RandomTest
{
    
    @Test
    public void distributionTest()
    {
        runTrials(2, 1000, 1);
        runTrials(10, 1000, 1);
        runTrials(10, 1000, 3);
    }
    
    private void runTrials(int numEntrants, int numTrials, int numWinners)
    {
        // construct initial structures
        System.out.println(String.format("Running: %d entrants, %d trials, %d winners", numEntrants, numTrials, numWinners));
        List<Integer> entries = new ArrayList<>();
        int[] wins = new int[numEntrants];
        for(int i = 0; i < numEntrants; i++)
            entries.add(i);
        
        for(int i = 0; i < numTrials; i++)
        {
            GiveawayUtil.selectWinners(entries, numWinners).forEach(e -> wins[e]++);
        }
        
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < wins.length; i++)
            sb.append(String.format("%d: %d (%f%%)\n", i, wins[i], (double)(wins[i])/numTrials*100.0));
        System.out.println(sb.toString());
    }
}
