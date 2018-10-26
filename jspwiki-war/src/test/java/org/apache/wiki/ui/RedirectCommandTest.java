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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class RedirectCommandTest
{

    @Test
    public void testStaticCommand()
    {
        Command a = RedirectCommand.REDIRECT;
        Assertions.assertEquals( "", a.getRequestContext() );
        Assertions.assertEquals( "", a.getJSP() );
        Assertions.assertEquals( "%u%n", a.getURLPattern() );
        Assertions.assertNull( a.getContentTemplate() );
        Assertions.assertNull( a.getTarget() );
        Assertions.assertEquals( a, RedirectCommand.REDIRECT );
    }

    @Test
    public void testTargetedCommand()
    {
        Command a = RedirectCommand.REDIRECT;

        // Test with local JSP
        Command b = a.targetedCommand( "%uTestPage.jsp" );
        Assertions.assertEquals( "", b.getRequestContext() );
        Assertions.assertEquals( "TestPage.jsp", b.getJSP() );
        Assertions.assertEquals( "%uTestPage.jsp", b.getURLPattern() );
        Assertions.assertNull( b.getContentTemplate() );
        Assertions.assertEquals( "%uTestPage.jsp", b.getTarget() );
        Assertions.assertNotSame( RedirectCommand.REDIRECT, b );

        // Test with non-local URL
        b = a.targetedCommand( "http://www.yahoo.com" );
        Assertions.assertEquals( "", b.getRequestContext() );
        Assertions.assertEquals( "http://www.yahoo.com", b.getJSP() );
        Assertions.assertEquals( "http://www.yahoo.com", b.getURLPattern() );
        Assertions.assertNull( b.getContentTemplate() );
        Assertions.assertEquals( "http://www.yahoo.com", b.getTarget() );
        Assertions.assertNotSame( RedirectCommand.REDIRECT, b );
    }

}