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

import org.apache.wiki.TestEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class GroupsTest {
    TestEngine testEngine = TestEngine.build();

    @AfterEach
    public void tearDown() throws Exception {
        testEngine.getPageManager().deletePage( "Test" );
    }

    @Test
    public void testTag() throws Exception
    {
        String src="[{Groups}]";

        testEngine.saveText( "Test", src );

        String res = testEngine.getRenderingManager().getHTML( "Test" );

        Assertions.assertEquals( "<a href=\"/test/Group.jsp?group=Admin\">Admin</a>, "
                + "<a href=\"/test/Group.jsp?group=Art\">Art</a>, "
                + "<a href=\"/test/Group.jsp?group=Literature\">Literature</a>, "
                + "<a href=\"/test/Group.jsp?group=TV\">TV</a>\n"
                , res );
    }

}
