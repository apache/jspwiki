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
package stress;

import junit.framework.*;
import java.io.*;
import java.util.*;

import org.apache.jspwiki.api.WikiPage;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.providers.*;

/**
 *  Does stress testing on the RCSProvider.
 */
public class StressTestRCSProvider extends TestCase
{
    public static final String NAME1 = "Test1";

    Properties props = new Properties();

    TestEngine engine;

    public StressTestRCSProvider( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( TestEngine.findTestProperties("/jspwiki_rcs.properties") );
        props.setProperty( CachingProvider.PROP_CACHECAPACITY, "10000" );
        engine = new TestEngine(props);
    }

    public void tearDown()
    {
        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );

        File f = new File( files, NAME1+FileSystemProvider.FILE_EXT );

        f.delete();

        f = new File( files+File.separator+"RCS", NAME1+FileSystemProvider.FILE_EXT+",v" );

        f.delete();

        f = new File( files, "RCS" );

        f.delete();
    }

    /**
     *  Bug report by Anon: RCS under Windows 2k:
     * <PRE>
     * In getPageInfo of RCSFileProvider:
     *
     * Problem:
     *
     * With a longer rlog result, the break clause in the last "else if" 
     * breaks out of the reading loop before all the lines in the full 
     * rlog have been read in. This causes the process.wait() to hang.
     *
     * Suggested quick fix:
     *
     * Always read all the contents of the rlog, even if it is slower.
     * </PRE>
     *
     */

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
        WikiPage pageinfo = engine.getPage( NAME1 );

        assertEquals( "wrong version", maxver, pageinfo.getVersion() );
        // +2 comes from \r\n at the end of each file.
        assertEquals( "wrong text", maxver+2, engine.getText(NAME1).length() );
    }

    private void runMassiveFileTest(int maxpages) throws Exception
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
        Collection pages = engine.getPageManager().getAllPages();
        mark.stop();
    
        System.out.println("Got a list of all pages in "+mark);
    
        mark.reset();
        mark.start();
    
        for( Iterator i = pages.iterator(); i.hasNext(); )
        {
            String foo = engine.getPureText( (WikiPage)i.next() );
        
            assertNotNull( foo );
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
    
    public static Test suite()
    {
        return new TestSuite( StressTestRCSProvider.class );
    }
}
