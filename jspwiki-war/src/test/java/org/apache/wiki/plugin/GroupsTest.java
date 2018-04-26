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

package org.apache.wiki.plugin;

import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class GroupsTest
{
    Properties props = TestEngine.getTestProperties();
    TestEngine testEngine;

    @Before
    public void setUp()
        throws Exception
    {
        testEngine = new TestEngine(props);
    }

    @After
    public void tearDown() throws Exception
    {
        testEngine.deletePage( "Test" );
    }

    @Test
    public void testTag() throws Exception
    {
        String src="[{Groups}]";

        testEngine.saveText( "Test", src );

        String res = testEngine.getHTML( "Test" );

        Assert.assertEquals( "<a href=\"/test/Group.jsp?group=Admin\">Admin</a>, "
                + "<a href=\"/test/Group.jsp?group=Art\">Art</a>, "
                + "<a href=\"/test/Group.jsp?group=Literature\">Literature</a>, "
                + "<a href=\"/test/Group.jsp?group=TV\">TV</a>\n"
                , res );
    }

}
