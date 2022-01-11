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
package org.apache.wiki.ui.admin.beans;

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.api.spi.Wiki;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.management.NotCompliantMBeanException;
import java.util.Properties;


public class PluginBeanTest {
    
    Properties props = TestEngine.getTestProperties();

    TestEngine testEngine;
    
    @Test
    public void testDoGet() throws WikiException, NotCompliantMBeanException {
        testEngine = new TestEngine( props );
        final Context context = Wiki.context().create( testEngine, Wiki.contents().page( testEngine, "TestPage01" ) );
        final PluginBean pb = new PluginBean( testEngine );
        final String expectedHtml = "<div>" +
                                      "<h4>Plugins</h4>" +
                                      "<table border=\"1\">" +
                                        "<tr><th>Name</th><th>Alias</th><th>Author</th><th>Notes</th></tr>" +
                                        "<tr><td>IfPlugin</td><td>If</td><td>Janne Jalkanen</td><td></td></tr>" +
                                        "<tr><td>Note</td><td></td><td>Janne Jalkanen</td><td></td></tr>" +
                                        "<tr><td>SamplePlugin</td><td>samplealias</td><td>Janne Jalkanen</td><td></td></tr>" +
                                        "<tr><td>SamplePlugin2</td><td>samplealias2</td><td>Janne Jalkanen</td><td></td></tr>" +
                                      "</table>" +
                                    "</div>";
        Assertions.assertEquals( expectedHtml, pb.doGet( context ) );
    }

}
