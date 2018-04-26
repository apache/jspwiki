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

package org.apache.wiki;

import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


@RunWith( Suite.class )
@Suite.SuiteClasses( { PageManagerTest.class,
                       PageSorterTest.class,
                       ReferenceManagerTest.class,
                       ReleaseTest.class,
                       VariableManagerTest.class,
                       WikiEngineTest.class,
                       WikiSessionTest.class,
                       org.apache.wiki.attachment.AllTests.class,
                       org.apache.wiki.auth.AllTests.class,
                       org.apache.wiki.content.AllTests.class,
                       org.apache.wiki.diff.AllTests.class,
                       org.apache.wiki.filters.AllTests.class,
                       org.apache.wiki.htmltowiki.AllTests.class,
                       org.apache.wiki.parser.AllTests.class,
                       org.apache.wiki.plugin.AllTests.class,
                       org.apache.wiki.providers.AllTests.class,
                       org.apache.wiki.render.AllTests.class,
                       org.apache.wiki.rss.AllTests.class,
                       org.apache.wiki.search.AllTests.class,
                       org.apache.wiki.ui.AllTests.class,
                       org.apache.wiki.url.AllTests.class,
                       org.apache.wiki.util.AllTests.class,
                       // org.apache.wiki.web.AllTests.class // These are not runnable without a running tomcat
                       org.apache.wiki.workflow.AllTests.class,
                       org.apache.wiki.xmlrpc.AllTests.class } )
public class AllTests
{
    //
    //  Ensure everything runs properly and that we can locate all necessary thingies.
    //
    static {
        Properties props = TestEngine.getTestProperties();
        if( props == null ) {
            Assert.fail( "No property file found!" );
        }
        PropertyConfigurator.configure( props );
    }

}
