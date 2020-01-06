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
package org.apache.wiki.stress;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.providers.FileSystemProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.util.Collection;
import java.util.Properties;


public class StressTestVersioningProvider {
    public static final String NAME1 = "Test1";

    Properties props = TestEngine.getTestProperties("/jspwiki-vers-custom.properties");
    TestEngine engine = TestEngine.build( props );

    @AfterEach
    public void tearDown()
    {
        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );

        // Remove file
        File f = new File( files, NAME1+FileSystemProvider.FILE_EXT );
        f.delete();

        f = new File( files, "OLD" );

        TestEngine.deleteAll(f);
    }

    public void testMillionChanges()
        throws Exception
    {
        String text = "";
        String name = NAME1;
        int    maxver = 2000; // Save 2000 versions.
        Benchmark mark = new Benchmark();

        mark.start();
        for( int i = 0; i < maxver; i++ )
        {
            text = text + ".";
            engine.saveText( name, text );
        }

        mark.stop();

        System.out.println("Benchmark: "+mark.toString(2000)+" pages/second");
        WikiPage pageinfo = engine.getPageManager().getPage( NAME1 );

        Assertions.assertEquals( maxver, pageinfo.getVersion(), "wrong version" );

        // +2 comes from \r\n.
        Assertions.assertEquals( maxver+2, engine.getPageManager().getText(NAME1).length(), "wrong text" );
    }

    private void runMassiveFileTest(int maxpages)
        throws Exception
    {
        String text = "Testing, 1, 2, 3: ";
        String name = NAME1;
        Benchmark mark = new Benchmark();

        System.out.println("Building a massive repository of "+maxpages+" pages...");

        mark.start();
        for( int i = 0; i < maxpages; i++ )
        {
            engine.saveText( name+i, text+i );
        }
        mark.stop();

        System.out.println("Total time to save "+maxpages+" pages was "+mark.toString() );
        System.out.println("Saved "+mark.toString(maxpages)+" pages/second");

        mark.reset();

        mark.start();
        Collection< WikiPage > pages = engine.getPageManager().getAllPages();
        mark.stop();

        System.out.println("Got a list of all pages in "+mark);

        mark.reset();
        mark.start();

        for( WikiPage page : pages ) {
            String foo = engine.getPageManager().getPureText( page );
            Assertions.assertNotNull( foo );
        }
        mark.stop();

        System.out.println("Read through all of the pages in "+mark);
        System.out.println("which is "+mark.toString(maxpages)+" pages/second");
    }

    public void testMillionFiles1() throws Exception
    {
        runMassiveFileTest(100);
    }

    public void testMillionFiles2() throws Exception
    {
        runMassiveFileTest(1000);
    }

    public void testMillionFiles3() throws Exception
    {
        runMassiveFileTest(10000);
    }
    /*
    public void testMillionFiles4()throws Exception
    {
        runMassiveFileTest(100000);
    }
    */

}
