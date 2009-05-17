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

package org.apache.wiki.plugin;

import java.util.Properties;

import org.apache.wiki.TestEngine;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class GroupsTest extends TestCase
{
    Properties m_props = new Properties();
    TestEngine m_testEngine;
    
    public GroupsTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        m_props.load( TestEngine.findTestProperties() );

        m_testEngine = new TestEngine(m_props);
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
        
        m_testEngine.deletePage( "Test" );
    }
    
    public void testTag() throws Exception
    {
        String src="[{Groups}]";
        
        m_testEngine.saveText( "Test", src );
        
        String res = m_testEngine.getHTML( "Test" );
        
        assertEquals( "<a href=\"/Group.jsp?group=Admin\">Admin</a>, " 
                + "<a href=\"/Group.jsp?group=Art\">Art</a>, "
                + "<a href=\"/Group.jsp?group=Literature\">Literature</a>, "
                + "<a href=\"/Group.jsp?group=TV\">TV</a>\n"
                , res );
    }

    public static Test suite()
    {
        return new TestSuite( GroupsTest.class );
    }
}
