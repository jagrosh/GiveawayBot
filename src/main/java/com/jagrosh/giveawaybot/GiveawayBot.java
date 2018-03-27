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
package com.jagrosh.giveawaybot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * This bot is designed to simplify giveaways. It is very easy to start a timed
 * giveaway, and easy for users to enter as well. This bot also automatically 
 * picks a winner, and the winner can be re-rolled if necessary.
 * 
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class GiveawayBot {
    
    private final static Logger LOG = LoggerFactory.getLogger("Init");
    
    /**
     * Main execution
     * @param args - run type and other info
     */
    public static void main(String[] args)
    {
        if(args.length==0)
        {
            LOG.error("Must include command line arguments");
        }
        else try
        {
            switch(args[0])
            {
                case "updater":
                    Updater.main();
                    break;
                case "bot":
                    Bot.main(Integer.parseInt(args[1]), Integer.parseInt(args[2]), args.length>3 ? Integer.parseInt(args[3]) : 16);
                    break;
                case "website":
                    Website.main(new String[0]);
                    break;
                case "none":
                    break;
                default:
                    LOG.error(String.format("Invalid startup type '%s'",args[0]));
            }
        }
        catch (Exception e)
        {
            LOG.error(""+e);
            e.printStackTrace();
        }
    }
}
