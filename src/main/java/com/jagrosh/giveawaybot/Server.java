/*
 * Copyright 2021 John Grosh (john.a.grosh@gmail.com).
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
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SignatureException;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.Utils;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Server
{
    public static void main(String[] args) throws Exception
    {
        Logger log = LoggerFactory.getLogger("Main");
        Config config = ConfigFactory.load();
        
        // used to verify that we're getting messages from Discord
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
        EdDSAPublicKeySpec pubKey = new EdDSAPublicKeySpec(Utils.hexToBytes(config.getString("public-key")), spec);
        EdDSAEngine sgr = new EdDSAEngine(MessageDigest.getInstance(spec.getHashAlgorithm()));
        sgr.initVerify(new EdDSAPublicKey(pubKey));
        
        // set the port
        Spark.port(config.getInt("port"));
        
        // verify that the request is from discord for every request
        Spark.before((req, res) ->
        {
            sgr.update((req.headers("x-signature-timestamp") + req.body()).getBytes(Charset.forName("UTF-8")));
            boolean verified = false;
            try
            {
                verified = sgr.verify(Utils.hexToBytes(req.headers("x-signature-ed25519")));
            }
            catch (SignatureException ex) {}
            if(!verified)
            {
                log.info(String.format("Unverified request from %s (%s)", req.host(), req.headers("x-signature-ed25519")));
                Spark.halt(400);
            }
        });
        
        // handle posts to the integration
        Spark.post("/", (req, res) -> 
        {
            JSONObject body = new JSONObject(req.body());
            
            return null;
        });
    }
}
