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

package org.apache.wiki.filters;

import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.action.TestActionBean;

public class SpamFilterTest extends TestCase
{
    public SpamFilterTest( String s )
    {
        super( s );
    }

    public void testGetBeanProperties() throws Exception
    {
        TestActionBean bean = new TestActionBean();
        bean.setAcl( "ACL" );
        bean.setText( "Sample text" );
        Map<String, Object> map = SpamFilter.getBeanProperties( bean, new String[] { "text", "acl", "nonExistentProperty" } );
        assertEquals( 2, map.size() );
        Object value = map.get( "text" );
        assertEquals( "Sample text", value );
        value = map.get( "acl" );
        assertEquals( "ACL", value );
    }

    public static Test suite()
    {
        return new TestSuite( SpamFilterTest.class );
    }

}
