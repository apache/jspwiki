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

import org.apache.wiki.*;
import org.apache.wiki.providers.*;
import org.apache.wiki.util.FileUtil;


public final class StressTestSpeed extends TestCase
{
    private static int ITERATIONS = 100;
    public static final String NAME1 = "Test1";

    Properties props = new Properties();

    TestEngine engine;

    public StressTestSpeed( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( TestEngine.findTestProperties("/jspwiki_rcs.properties") );

        props.setProperty( "jspwiki.usePageCache", "true" );
        props.setProperty( "jspwiki.newRenderingEngine", "true" );
        
        engine = new TestEngine(props);
    }

    public void tearDown()
    {
        String files = props.getProperty( AbstractFileProvider.PROP_PAGEDIR );

        File f = new File( files, NAME1+".txt" );

        f.delete();

        f = new File( files+File.separator+"RCS", NAME1+".txt,v" );

        f.delete();

        f = new File( files, "RCS" );

        f.delete();
        
        engine.shutdown();
    }

    public void testSpeed1()
        throws Exception
    {
        InputStream is = getClass().getResourceAsStream("/TextFormattingRules.txt");
        Reader      in = new InputStreamReader( is, "ISO-8859-1" );
        StringWriter out = new StringWriter();
        Benchmark mark = new Benchmark();

        FileUtil.copyContents( in, out );

        engine.saveText( NAME1, out.toString() );

        mark.start();

        for( int i = 0; i < ITERATIONS; i++ )
        {
            String txt = engine.getHTML( NAME1 );
            assertTrue( 0 != txt.length() );
        }

        mark.stop();

        System.out.println( ITERATIONS+" pages took "+mark+" (="+
                            mark.getTime()/ITERATIONS+" ms/page)" );
    }

    public void testSpeed2()
        throws Exception
    {
        InputStream is = getClass().getResourceAsStream("/TestPlugins.txt");
        Reader      in = new InputStreamReader( is, "ISO-8859-1" );
        StringWriter out = new StringWriter();
        Benchmark mark = new Benchmark();

        FileUtil.copyContents( in, out );

        engine.saveText( NAME1, out.toString() );

        mark.start();

        for( int i = 0; i < ITERATIONS; i++ )
        {
            String txt = engine.getHTML( NAME1 );
            assertTrue( 0 != txt.length() );
        }

        mark.stop();

        System.out.println( ITERATIONS+" plugin pages took "+mark+" (="+
                            mark.getTime()/ITERATIONS+" ms/page)" );
    }

    public static Test suite()
    {
        return new TestSuite( StressTestSpeed.class );
    }
    
    public static void main( String[] argv )
    {
        junit.textui.TestRunner.run(suite());
    }
}

