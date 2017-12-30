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
/*
 * (C) Janne Jalkanen 2005
 *
 */
package org.apache.wiki.ui;

import org.junit.Assert;
import org.junit.Test;


public class RedirectCommandTest
{

    @Test
    public void testStaticCommand()
    {
        Command a = RedirectCommand.REDIRECT;
        Assert.assertEquals( "", a.getRequestContext() );
        Assert.assertEquals( "", a.getJSP() );
        Assert.assertEquals( "%u%n", a.getURLPattern() );
        Assert.assertNull( a.getContentTemplate() );
        Assert.assertNull( a.getTarget() );
        Assert.assertEquals( a, RedirectCommand.REDIRECT );
    }

    @Test
    public void testTargetedCommand()
    {
        Command a = RedirectCommand.REDIRECT;

        // Test with local JSP
        Command b = a.targetedCommand( "%uTestPage.jsp" );
        Assert.assertEquals( "", b.getRequestContext() );
        Assert.assertEquals( "TestPage.jsp", b.getJSP() );
        Assert.assertEquals( "%uTestPage.jsp", b.getURLPattern() );
        Assert.assertNull( b.getContentTemplate() );
        Assert.assertEquals( "%uTestPage.jsp", b.getTarget() );
        Assert.assertNotSame( RedirectCommand.REDIRECT, b );

        // Test with non-local URL
        b = a.targetedCommand( "http://www.yahoo.com" );
        Assert.assertEquals( "", b.getRequestContext() );
        Assert.assertEquals( "http://www.yahoo.com", b.getJSP() );
        Assert.assertEquals( "http://www.yahoo.com", b.getURLPattern() );
        Assert.assertNull( b.getContentTemplate() );
        Assert.assertEquals( "http://www.yahoo.com", b.getTarget() );
        Assert.assertNotSame( RedirectCommand.REDIRECT, b );
    }

}