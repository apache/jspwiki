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

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.authorize.GroupManager;
import org.apache.wiki.auth.authorize.XMLGroupDatabase;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.render.RenderingManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class GroupsTest {
    private TestEngine testEngine;
    private File target ;
    @BeforeEach
    public void init() throws IOException {
        final Properties props = TestEngine.getTestProperties();
        target = new File("target/GroupsTest" + UUID.randomUUID().toString() + ".xml");
        FileUtils.copyFile(new File("src/test/resources/groupdatabase.xml"), target);
        props.put(XMLGroupDatabase.PROP_DATABASE, target.getAbsolutePath());
        testEngine = TestEngine.build(props);
    }
    

    

    @AfterEach
    public void tearDown() throws Exception {
        testEngine.getManager( PageManager.class ).deletePage( "Test" );
    }

    @Test
    public void testTag() throws Exception {
        final String src="[{Groups}]";

        testEngine.saveText( "Test", src );

        final String res = testEngine.getManager( RenderingManager.class ).getHTML( "Test" );

        Assertions.assertEquals( "<a href=\"/test/Group.jsp?group=Admin\">Admin</a>, "
                + "<a href=\"/test/Group.jsp?group=Art\">Art</a>, "
                + "<a href=\"/test/Group.jsp?group=Literature\">Literature</a>, "
                + "<a href=\"/test/Group.jsp?group=TV\">TV</a>\n"
                , res );
    }
    
    @Test
    public void jspwiki130part2() throws Exception {

        GroupManager usermanger = testEngine.getManager(GroupManager.class);

        Assertions.assertThrows(WikiException.class, () -> {
            usermanger.parseGroup("janne", "Al\nBob\nCookie", true);
        });
        Assertions.assertThrows(WikiException.class, () -> {
            usermanger.parseGroup("JanneJalkanen", "Al\nBob\nCookie", true);
        });

        Assertions.assertThrows(WikiException.class, () -> {
            usermanger.parseGroup("janne@ecyrd.com", "Al\nBob\nCookie", true);
        });

    }

}
