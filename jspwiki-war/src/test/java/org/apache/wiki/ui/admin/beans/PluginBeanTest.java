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

import org.junit.Test;

import java.util.Properties;

import javax.management.NotCompliantMBeanException;

import org.junit.Assert;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.WikiException;


public class PluginBeanTest {
    
    Properties props = TestEngine.getTestProperties();

    TestEngine testEngine;
    
    @Test
    public void testDoGet() throws WikiException, NotCompliantMBeanException {
        testEngine = new TestEngine( props );
        WikiContext context = new WikiContext( testEngine, new WikiPage( testEngine, "TestPage01" ) );
        PluginBean pb = new PluginBean( testEngine );
        String expectedHtml = "<div>" +
                                "<h4>Plugins</h4>" +
                                "<table border=\"1\">" +
                                  "<tr><th>Name</th><th>Alias</th><th>Author</th><th>Notes</th></tr>" +
                                  "<tr><td>IfPlugin</td><td>If</td><td>Janne Jalkanen</td><td></td></tr>" +
                                  "<tr><td>Note</td><td></td><td>Janne Jalkanen</td><td></td></tr>" +
                                  "<tr><td>SamplePlugin</td><td>samplealias</td><td>Janne Jalkanen</td><td></td></tr>" +
                                  "<tr><td>SamplePlugin2</td><td>samplealias2</td><td>Janne Jalkanen</td><td></td></tr>" +
                                "</table>" +
                              "</div>";
        Assert.assertEquals( expectedHtml, pb.doGet( context ) );
    }

}
