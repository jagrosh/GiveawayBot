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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class MiscTest
{
    @Test
    public void distributionTest()
    {
        String text = "giveawaybot";
        for(int i = text.length(); i>0; i--)
        {
            String e = text.substring(0, i);
            String f = text.substring(i).trim();
            System.out.println(i + ": " + e + " " + f);
        }
    }
    
    @Test
    public void mappingTest()
    {
        Config config = ConfigFactory.parseString("{ map: { a : a_value , b : b_value } }");
        Assert.assertEquals("a_value", config.getConfig("map").getString("a"));
        Map<String,String> map = config.getConfig("map").entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().unwrapped().toString()));
        System.out.println(map);
        Assert.assertEquals(2, map.entrySet().size());
        Assert.assertEquals("b_value", map.get("b"));
    }
}
