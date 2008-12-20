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

package com.ecyrd.jspwiki.providers;

import junit.framework.*;
import java.io.*;
import java.util.*;
import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.util.FileUtil;

/**
 *  Tests the RCSFileProvider.  If you are getting strange errors, please check that you
 *  actually <i>have</i> RCS installed and in your path...
 * 
 *
 *  @since forever
 */
public class RCSFileProviderTest extends TestCase
{
    public static final String NAME1 = "Test1";

    Properties props = new Properties();

    TestEngine engine;

    public RCSFileProviderTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( TestEngine.findTestProperties("/jspwiki_rcs.properties") );

        engine = new TestEngine(props);
    }

    /**
     *  Remove NAME1 + all RCS directories for it.
     */
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
        int    maxver = 100; // Save 100 versions.

        for( int i = 0; i < maxver; i++ )
        {
            text = text + ".";
            engine.saveText( name, text );
        }

        WikiPage pageinfo = engine.getPage( NAME1 );

        assertEquals( "wrong version", maxver, pageinfo.getVersion() );
        // +2 comes from \r\n at the end of each file.
        assertEquals( "wrong text", maxver+2, engine.getText(NAME1).length() );
    }

    /**
     *  Checks if migration from FileSystemProvider to VersioningFileProvider
     *  works by creating a dummy file without corresponding content in OLD/
     */
    public void testMigration()
        throws IOException
    {
        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );
        
        File f = new File( files, NAME1+FileSystemProvider.FILE_EXT );

        Writer out = new FileWriter( f );
        FileUtil.copyContents( new StringReader("foobar"), out );
        out.close();

        String res = engine.getText( NAME1 );

        assertEquals( "latest did not work", "foobar", res );

        res = engine.getText( NAME1, 1 ); // Should be the first version.

        assertEquals( "fetch by direct version did not work", "foobar", res );
    }

    public void testGetByVersion()
        throws Exception
    {
        String text = "diddo\r\n";

        engine.saveText( NAME1, text );

        WikiPage page = engine.getPage( NAME1, 1 );
       
        assertEquals( "name", NAME1, page.getName() );
        assertEquals( "version", 1, page.getVersion() );
    }

    public void testGetByLatestVersion()
        throws Exception
    {
        String text = "diddo\r\n";

        engine.saveText( NAME1, text );

        WikiPage page = engine.getPage( NAME1, WikiProvider.LATEST_VERSION );
       
        assertEquals( "name", NAME1, page.getName() );
        assertEquals( "version", 1, page.getVersion() );
    }

    public void testDelete()
        throws Exception
    {
        engine.saveText( NAME1, "v1" );
        engine.saveText( NAME1, "v2" );
        engine.saveText( NAME1, "v3" );

        PageManager mgr = engine.getPageManager();
        WikiPageProvider provider = mgr.getProvider();

        provider.deletePage( NAME1 );

        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );

        File f = new File( files, NAME1+FileSystemProvider.FILE_EXT );

        assertFalse( "file exists", f.exists() );

        f = new File( files+File.separator+"RCS", NAME1+FileSystemProvider.FILE_EXT+",v" );

        assertFalse( "RCS file exists", f.exists() );
    }

    public void testDeleteVersion()
        throws Exception
    {
        engine.saveText( NAME1, "v1\r\n" );
        engine.saveText( NAME1, "v2\r\n" );
        engine.saveText( NAME1, "v3\r\n" );

        PageManager mgr = engine.getPageManager();
        WikiPageProvider provider = mgr.getProvider();

        provider.deleteVersion( NAME1, 2 );

        List l = provider.getVersionHistory( NAME1 );

        assertEquals( "wrong # of versions", 2, l.size() );

        assertEquals( "v1", "v1\r\n", provider.getPageText( NAME1, 1 ) );
        assertEquals( "v3", "v3\r\n", provider.getPageText( NAME1, 3 ) );

        try
        {
            provider.getPageText( NAME1, 2 );
            fail( "v2" );
        }
        catch( NoSuchVersionException e )
        {
            // This is expected
        }
    }

    public void testChangeNote()
        throws Exception
    {
        WikiPage p = engine.createPage( NAME1 );
        p.setAttribute(WikiPage.CHANGENOTE, "Test change" );
        WikiContext context = engine.getWikiContextFactory().newViewContext(null, null, p);
        
        engine.saveText( context, "test" );
        
        WikiPage p2 = engine.getPage( NAME1 );
        
        assertEquals( "Test change", p2.getAttribute(WikiPage.CHANGENOTE) );
    }

    public void testChangeNoteOldVersion()
        throws Exception
    {
        WikiPage p = engine.createPage( NAME1 );
        WikiContext context = engine.getWikiContextFactory().newViewContext(null, null, p);

        context.getPage().setAttribute(WikiPage.CHANGENOTE, "Test change" );
        engine.saveText( context, "test" );
        
        context.getPage().setAttribute(WikiPage.CHANGENOTE, "Change 2" );
        engine.saveText( context, "test2" );

        WikiPage p2 = engine.getPage( NAME1, 1 );
        
        assertEquals( "Test change", p2.getAttribute(WikiPage.CHANGENOTE) );

        WikiPage p3 = engine.getPage( NAME1, 2 );
        
        assertEquals( "Change 2", p3.getAttribute(WikiPage.CHANGENOTE) );
    }
    public static Test suite()
    {
        return new TestSuite( RCSFileProviderTest.class );
    }
}
