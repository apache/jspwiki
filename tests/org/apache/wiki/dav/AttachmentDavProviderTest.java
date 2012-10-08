/* 
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
package org.apache.wiki.dav;

import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.dav.items.DavItem;
import org.apache.wiki.dav.items.DirectoryItem;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AttachmentDavProviderTest extends TestCase
{
    Properties props = new Properties();

    TestEngine engine;

    AttachmentDavProvider m_provider;
    
    protected void setUp() throws Exception
    {
        props.load( TestEngine.findTestProperties() );

        engine = new TestEngine(props);

        m_provider = new AttachmentDavProvider(engine);
    }

    protected void tearDown() throws Exception
    {
        TestEngine.deleteAttachments( "TestPage" );
        TestEngine.deleteTestPage("TestPage");
    }

    public void testGetPageURL()
        throws Exception
    {
        engine.saveText("TestPage", "foobar");
        Attachment att = new Attachment(engine,"TestPage","deceit of the tribbles.txt");
        
        engine.getAttachmentManager().storeAttachment( att, engine.makeAttachmentFile() );
        
        DavItem di = m_provider.getItem( new DavPath("TestPage/deceit of the tribbles.txt") );
        
        assertNotNull( "No di", di );
        assertEquals("URL", "http://localhost/attach/TestPage/deceit%20of%20the%20tribbles.txt", 
                     di.getHref() );
    }

    public void testDirURL()
        throws Exception
    {
        engine.saveText("TestPage", "foobar");
    
        DavItem di = m_provider.getItem( new DavPath("") );
    
        assertNotNull( "No di", di );
        assertTrue( "DI is of wrong type", di instanceof DirectoryItem );
        assertEquals("URL", "http://localhost/attach/", di.getHref() );
    }

    public void testDirURL2()
        throws Exception
    {
        engine.saveText("TestPage", "foobar");

        DavItem di = m_provider.getItem( new DavPath("TestPage/") );

        assertNotNull( "No di", di );
        assertTrue( "DI is of wrong type", di instanceof DirectoryItem );
        assertEquals("URL", "http://localhost/attach/TestPage/", di.getHref() );
    }

    public static Test suite()
    {
        return new TestSuite( AttachmentDavProviderTest.class );
    }


}
