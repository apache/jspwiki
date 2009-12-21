/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.    
 */

package org.apache.wiki.content.inspect;

import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.content.inspect.AsirraCaptcha.Challenge;

/**
 */
public class AsirraCaptchaTest extends TestCase
{
    public static Test suite()
    {
        return new TestSuite( AsirraCaptchaTest.class );
    }
    
    private String m_response = "asirraState.NewSessionComplete(3, " +
    		"\"2009352/ec2.8/33f46dd1299db9de910869db5a493ed2\");" +
    		"asirraState.GetChallengeComplete(" +
    		"[ImgRec(\"401906c8e5fb9117ee4e4666758a03af\",\"//s3.amazonaws.com/Asirra/PhotoDB/401906c8e5fb9117ee4e4666758a03af.jpg\",250,250)," +
    		"ImgRec(\"0838a84351d6afbe0b981f0852394c19\",\"//s3.amazonaws.com/Asirra/PhotoDB/0838a84351d6afbe0b981f0852394c19.jpg\",250,250)," +
    		"ImgRec(\"f1753dc76175f85261a612099eeec772\",\"//s3.amazonaws.com/Asirra/PhotoDB/f1753dc76175f85261a612099eeec772.jpg\",250,250)," +
    		"ImgRec(\"c330de41625bcee3730c04293d181016\",\"//s3.amazonaws.com/Asirra/PhotoDB/c330de41625bcee3730c04293d181016.jpg\",250,250)," +
    		"ImgRec(\"8308841fded6bf328f6cae1bbca818ce\",\"//s3.amazonaws.com/Asirra/PhotoDB/8308841fded6bf328f6cae1bbca818ce.jpg\",250,250)," +
    		"ImgRec(\"678953ff8adab8bad067e3faec24ac90\",\"//s3.amazonaws.com/Asirra/PhotoDB/678953ff8adab8bad067e3faec24ac90.jpg\",250,250)," +
    		"ImgRec(\"5682da66d19d4f39b2d4cdc4618273fd\",\"//s3.amazonaws.com/Asirra/PhotoDB/5682da66d19d4f39b2d4cdc4618273fd.jpg\",250,250)," +
    		"ImgRec(\"b1f6d4d4234ba49a6adda94e698cfa1a\",\"//s3.amazonaws.com/Asirra/PhotoDB/b1f6d4d4234ba49a6adda94e698cfa1a.jpg\",250,250)," +
    		"ImgRec(\"667cd40b3ace67bcb5bc5b0df1af384c\",\"//s3.amazonaws.com/Asirra/PhotoDB/667cd40b3ace67bcb5bc5b0df1af384c.jpg\",250,250)," +
    		"ImgRec(\"892a31875c881eab6a37715107ede991\",\"//s3.amazonaws.com/Asirra/PhotoDB/892a31875c881eab6a37715107ede991.jpg\",250,250)," +
    		"ImgRec(\"b1eac4117ec55150a6de55bc6e486d5c\",\"//s3.amazonaws.com/Asirra/PhotoDB/b1eac4117ec55150a6de55bc6e486d5c.jpg\",250,250)," +
    		"ImgRec(\"ceae10e7729b37ba77133e006bab9253\",\"//s3.amazonaws.com/Asirra/PhotoDB/ceae10e7729b37ba77133e006bab9253.jpg\",250,250)]\");";
    
    public void testExtractChallenges()
    {
        List<Challenge> challenges = AsirraCaptcha.extractChallenges( m_response );
        assertEquals( 12, challenges.size() );
        assertEquals( "401906c8e5fb9117ee4e4666758a03af", challenges.get( 0 ).id() );
        assertEquals( "//s3.amazonaws.com/Asirra/PhotoDB/401906c8e5fb9117ee4e4666758a03af.jpg", challenges.get( 0 ).url() );
        assertEquals( "c330de41625bcee3730c04293d181016", challenges.get( 3 ).id() );
        assertEquals( "//s3.amazonaws.com/Asirra/PhotoDB/c330de41625bcee3730c04293d181016.jpg", challenges.get( 3 ).url() );
        assertEquals( "5682da66d19d4f39b2d4cdc4618273fd", challenges.get( 6 ).id() );
        assertEquals( "//s3.amazonaws.com/Asirra/PhotoDB/5682da66d19d4f39b2d4cdc4618273fd.jpg", challenges.get( 6 ).url() );
        assertEquals( "892a31875c881eab6a37715107ede991", challenges.get( 9 ).id() );
        assertEquals( "//s3.amazonaws.com/Asirra/PhotoDB/892a31875c881eab6a37715107ede991.jpg", challenges.get( 9 ).url() );
        assertEquals( "ceae10e7729b37ba77133e006bab9253", challenges.get( 11 ).id() );
        assertEquals( "//s3.amazonaws.com/Asirra/PhotoDB/ceae10e7729b37ba77133e006bab9253.jpg", challenges.get( 11 ).url() );
    }
    
    public void testExtractSession()
    {
        assertEquals( "2009352/ec2.8/33f46dd1299db9de910869db5a493ed2", AsirraCaptcha.extractSessionId( m_response ) );
    }

    /**
     * Does a live HTTP GET to Asirra and confirms that we can still extract valid session IDs and challenges.
     * @throws Exception
     */
    public void testGetChallengeResponse() throws Exception
    {
        String challengeResponse = AsirraCaptcha.getChallengeResponse();
        String sessionId = AsirraCaptcha.extractSessionId( challengeResponse );
        List<Challenge> challenges = AsirraCaptcha.extractChallenges( challengeResponse );
        assertNotNull( sessionId );
        assertNotNull( challenges );
        assertEquals( 12, challenges.size() );
    }
    
}
