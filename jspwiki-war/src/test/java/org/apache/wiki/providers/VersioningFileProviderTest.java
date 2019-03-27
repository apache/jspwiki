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

package org.apache.wiki.providers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import net.sf.ehcache.CacheManager;
import org.apache.wiki.PageManager;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.auth.Users;
import org.apache.wiki.util.FileUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

// FIXME: Should this thingy go directly to the VersioningFileProvider,
//        or should it rely on the WikiEngine API?

public class VersioningFileProviderTest
{
    public static final String NAME1 = "Test1";

    private static final String OLD_AUTHOR = "brian";
    private static final String FAKE_HISTORY =
                "#JSPWiki page properties for page " + NAME1 + "\n"
                + "#Wed Jan 01 12:27:57 GMT 2012" + "\n"
                + "author=" + OLD_AUTHOR + "\n";

    // we always use the same properties for this suite, but
    // they can be changed by our tests and also TestEngine,
    // and so must be reloaded from source for each test case
    private Properties PROPS =
            TestEngine.getTestProperties("/jspwiki-vers-custom.properties");

    // this is the testing page directory
    private String files;

    private TestEngine engine;

    @Before
    public void setUp()
        throws Exception
    {
        // make sure that the reference manager cache is cleaned first
        TestEngine.emptyWorkDir(null);

        engine = new TestEngine(PROPS);
        files = PROPS.getProperty( AbstractFileProvider.PROP_PAGEDIR );
    }

    @After
    public void tearDown()
    {
        // Remove all/any files and subdirs left in test page directory
        TestEngine.deleteAll( new File(files) );

        // jspwiki always uses a singleton CacheManager, so
        // clear the cache at the end of every test case to avoid
        // polluting another test case
        CacheManager.getInstance().removeAllCaches();
    }

    /*
     * Checks if a page created or last modified by FileSystemProvider
     * will be seen by VersioningFileProvider as the "first" version.
     */
    @Test
    public void testMigrationInfoAvailable()
        throws IOException
    {
        // we cannot switch PageProviders within a single test, so the
        // initial FileSystemProvider wiki page must be faked.
        final String fakeWikiPage = "foobar";
        injectFile(NAME1+AbstractFileProvider.FILE_EXT, fakeWikiPage);

       // also create an associated properties file with some history
        injectFile(NAME1+FileSystemProvider.PROP_EXT, FAKE_HISTORY);

        String res = engine.getText( NAME1 );
        Assert.assertEquals( "fetch latest should work", fakeWikiPage, res );

        WikiPage page = engine.getPage( NAME1, 1 );
        Assert.assertEquals( "original version expected", 1, page.getVersion() );
        Assert.assertEquals( "original author", OLD_AUTHOR, page.getAuthor() );
    }

    /*
     * Checks if migration from FileSystemProvider to VersioningFileProvider
     * works when a simple text file (without associated properties) exists,
     * but there is not yet any corresponding history content in OLD/
     */
    @Test
    public void testMigrationSimple()
        throws IOException
    {
        // we cannot switch PageProviders within a single test, so the
        // initial FileSystemProvider wiki page must be faked.
        injectFile(NAME1+AbstractFileProvider.FILE_EXT, "foobar");

        String res = engine.getText( NAME1 );
        Assert.assertEquals( "fetch latest did not work", "foobar", res );

        res = engine.getText( NAME1, 1 ); // Should be the first version.
        Assert.assertEquals( "fetch by direct version did not work", "foobar", res );

        WikiPage page = engine.getPage( NAME1 );
        Assert.assertEquals( "original version expected", 1, page.getVersion() );
        Assert.assertNull( "original author not expected", page.getAuthor() );
    }

    /*
     * Checks if migration from FileSystemProvider to VersioningFileProvider
     * works when a simple text file and its associated properties exist, but
     * when there is not yet any corresponding history content in OLD/
     */
    @Test
    public void testMigrationWithSimpleHistory()
        throws IOException
    {
        // we cannot switch PageProviders within a single test, so the
        // initial FileSystemProvider wiki page must be faked.
        final String fakeWikiPage = "foobar";
        injectFile(NAME1+AbstractFileProvider.FILE_EXT, fakeWikiPage);

       // now create the associated properties file with some history
        injectFile(NAME1+FileSystemProvider.PROP_EXT, FAKE_HISTORY);

        String res = engine.getText( NAME1 );
        Assert.assertEquals( "fetch latest did not work", fakeWikiPage, res );

        res = engine.getText( NAME1, 1 ); // Should be the first version.
        Assert.assertEquals( "fetch by direct version did not work", fakeWikiPage, res );

        WikiPage page = engine.getPage( NAME1, 1 );
        Assert.assertEquals( "original version expected", 1, page.getVersion() );
        Assert.assertEquals( "original author", OLD_AUTHOR, page.getAuthor() );
    }

    /*
     * Checks if migration from FileSystemProvider to VersioningFileProvider
     * works when a simple text file and its associated properties exist, but
     * when there is not yet any corresponding history content in OLD/.
     * Update the wiki page and confirm the original simple history was
     * assimilated into the newly-created properties.
     */
    @Test
    public void testMigrationChangesHistory()
        throws Exception
    {
        // we cannot switch PageProviders within a single test, so the
        // initial FileSystemProvider wiki page must be faked.
        final String fakeWikiPage = "foobar";
        injectFile(NAME1+AbstractFileProvider.FILE_EXT, fakeWikiPage);

       // also create an associated properties file with some history
        injectFile(NAME1+FileSystemProvider.PROP_EXT, FAKE_HISTORY);

        String result1 = engine.getText( NAME1 );
        Assert.assertEquals( "latest should be initial", fakeWikiPage, result1 );

        // now update the wiki page to create a new version
        final String text = "diddo\r\n";
        engine.saveText( NAME1, text );

        // confirm the right number of versions have been recorded
        Collection versionHistory = engine.getVersionHistory(NAME1);
        Assert.assertEquals( "number of versions", 2, versionHistory.size() );

        // fetch the updated page
        String result2 = engine.getText( NAME1 );
        Assert.assertEquals( "latest should be new version", text, result2 );
        String result3 = engine.getText( NAME1, 2 ); // Should be the 2nd version.
        Assert.assertEquals( "fetch new by version did not work", text, result3 );

        // now confirm the original page has been archived
        String result4 = engine.getText( NAME1, 1 );
        Assert.assertEquals( "fetch original by version Assert.failed", fakeWikiPage, result4 );

        WikiPage pageNew = engine.getPage( NAME1, 2 );
        Assert.assertEquals( "new version", 2, pageNew.getVersion() );
        Assert.assertEquals( "new author", "Guest", pageNew.getAuthor() );

        WikiPage pageOld = engine.getPage( NAME1, 1 );
        Assert.assertEquals( "old version", 1, pageOld.getVersion() );
        Assert.assertEquals( "old author", OLD_AUTHOR, pageOld.getAuthor() );
    }

    /*
     * Checks migration from FileSystemProvider to VersioningFileProvider
     * works after multiple updates to a page with existing properties.
     */
    @Test
    public void testMigrationMultiChangesHistory()
        throws Exception
    {
        // we cannot switch PageProviders within a single test, so the
        // initial FileSystemProvider wiki page must be faked.
        final String fakeWikiPage = "foobar";
        injectFile(NAME1+AbstractFileProvider.FILE_EXT, fakeWikiPage);

       // also create an associated properties file with some history
        injectFile(NAME1+FileSystemProvider.PROP_EXT, FAKE_HISTORY);

        // next update the wiki page to create a version number 2
        // with a different user name
        final String text2 = "diddo\r\n";
        engine.saveTextAsJanne( NAME1, text2 );

        // finally update the wiki page to create a version number 3
        final String text3 = "whateverNext\r\n";
        engine.saveText( NAME1, text3 );

        // confirm the right number of versions have been recorded
        Collection versionHistory = engine.getVersionHistory(NAME1);
        Assert.assertEquals( "number of versions", 3, versionHistory.size() );

        // fetch the latest version of the page
        String result = engine.getText( NAME1 );
        Assert.assertEquals( "latest should be newest version", text3, result );
        String result2 = engine.getText( NAME1, 3 );
        Assert.assertEquals( "fetch new by version did not work", text3, result2 );

        // confirm the original page was archived
        String result3 = engine.getText( NAME1, 1 );
        Assert.assertEquals( "fetch original by version Assert.failed", fakeWikiPage, result3 );

        // confirm the first update was archived
        String result4 = engine.getText( NAME1, 2 );
        Assert.assertEquals( "fetch original by version Assert.failed", text2, result4 );

        WikiPage pageNew = engine.getPage( NAME1 );
        Assert.assertEquals( "newest version", 3, pageNew.getVersion() );
        Assert.assertEquals( "newest author", "Guest", pageNew.getAuthor() );

        WikiPage pageMiddle = engine.getPage( NAME1, 2 );
        Assert.assertEquals( "middle version", 2, pageMiddle.getVersion() );
        Assert.assertEquals( "middle author", Users.JANNE, pageMiddle.getAuthor() );

        WikiPage pageOld = engine.getPage( NAME1, 1 );
        Assert.assertEquals( "old version", 1, pageOld.getVersion() );
        Assert.assertEquals( "old author", OLD_AUTHOR, pageOld.getAuthor() );
    }

    /*
     * A variation of testMigrationMultiChangesHistory when caching
     * is disabled.
     */
    @Test
    public void testMigrationMultiChangesNoCache()
        throws Exception
    {
        // discard the default engine, and get another with different properties
        // note: the originating properties file is unchanged.
        String cacheState = PROPS.getProperty( PageManager.PROP_USECACHE );
        Assert.assertEquals( "should cache", "true", cacheState );
        cacheState = "false";
        PROPS.setProperty( PageManager.PROP_USECACHE, cacheState );
        engine = new TestEngine(PROPS);

        // the new TestEngine will have assigned a new page directory
        files = PROPS.getProperty( AbstractFileProvider.PROP_PAGEDIR );

        // we cannot switch PageProviders within a single test, so the
        // initial FileSystemProvider wiki page must be faked.
        final String fakeWikiPage = "foobar";
        injectFile(NAME1+AbstractFileProvider.FILE_EXT, fakeWikiPage);

       // also create an associated properties file with some history
        injectFile(NAME1+FileSystemProvider.PROP_EXT, FAKE_HISTORY);

        // next update the wiki page to create a version number 2
        // with a different user name
        final String text2 = "diddo\r\n";
        engine.saveTextAsJanne( NAME1, text2 );

        // finally update the wiki page to create a version number 3
        final String text3 = "whateverNext\r\n";
        engine.saveText( NAME1, text3 );

        // confirm the right number of versions have been recorded
        Collection versionHistory = engine.getVersionHistory(NAME1);
        Assert.assertEquals( "number of versions", 3, versionHistory.size() );

        // fetch the latest version of the page
        String result = engine.getText( NAME1 );
        Assert.assertEquals( "latest should be newest version", text3, result );
        String result2 = engine.getText( NAME1, 3 );
        Assert.assertEquals( "fetch new by version did not work", text3, result2 );

        // confirm the original page was archived
        String result3 = engine.getText( NAME1, 1 );
        Assert.assertEquals( "fetch original by version Assert.failed", fakeWikiPage, result3 );

        // confirm the first update was archived
        String result4 = engine.getText( NAME1, 2 );
        Assert.assertEquals( "fetch original by version Assert.failed", text2, result4 );

        WikiPage pageNew = engine.getPage( NAME1 );
        Assert.assertEquals( "newest version", 3, pageNew.getVersion() );
        Assert.assertEquals( "newest author", "Guest", pageNew.getAuthor() );

        WikiPage pageMiddle = engine.getPage( NAME1, 2 );
        Assert.assertEquals( "middle version", 2, pageMiddle.getVersion() );
        Assert.assertEquals( "middle author", Users.JANNE, pageMiddle.getAuthor() );

        WikiPage pageOld = engine.getPage( NAME1, 1 );
        Assert.assertEquals( "old version", 1, pageOld.getVersion() );
        Assert.assertEquals( "old author", OLD_AUTHOR, pageOld.getAuthor() );
    }

    @Test
    public void testMillionChanges()
        throws Exception
    {
        String text = "";
        String name = NAME1;
        int maxver = 100;           // Save 100 versions.

        for( int i = 0; i < maxver; i++ )
        {
            text = text + ".";
            engine.saveText( name, text );
        }

        WikiPage pageinfo = engine.getPage( NAME1 );

        Assert.assertEquals( "wrong version", maxver, pageinfo.getVersion() );

        // +2 comes from \r\n.
        Assert.assertEquals( "wrong text", maxver+2, engine.getText(NAME1).length() );
    }

    @Test
    public void testCheckin()
        throws Exception
    {
        String text = "diddo\r\n";

        engine.saveText( NAME1, text );

        String res = engine.getText(NAME1);

        Assert.assertEquals( text, res );
    }

    @Test
    public void testGetByVersion()
        throws Exception
    {
        String text = "diddo\r\n";

        engine.saveText( NAME1, text );

        WikiPage page = engine.getPage( NAME1, 1 );

        Assert.assertEquals( "name", NAME1, page.getName() );
        Assert.assertEquals( "version", 1, page.getVersion() );
    }

    @Test
    public void testPageInfo()
        throws Exception
    {
        String text = "diddo\r\n";

        engine.saveText( NAME1, text );

        WikiPage res = engine.getPage(NAME1);

        Assert.assertEquals( 1, res.getVersion() );
    }

    @Test
    public void testGetOldVersion()
        throws Exception
    {
        String text = "diddo\r\n";
        String text2 = "barbar\r\n";
        String text3 = "Barney\r\n";

        engine.saveText( NAME1, text );
        engine.saveText( NAME1, text2 );
        engine.saveText( NAME1, text3 );

        WikiPage res = engine.getPage(NAME1);

        Assert.assertEquals("wrong version", 3, res.getVersion() );

        Assert.assertEquals("ver1", text, engine.getText( NAME1, 1 ) );
        Assert.assertEquals("ver2", text2, engine.getText( NAME1, 2 ) );
        Assert.assertEquals("ver3", text3, engine.getText( NAME1, 3 ) );
    }

    @Test
    public void testGetOldVersion2()
        throws Exception
    {
        String text = "diddo\r\n";
        String text2 = "barbar\r\n";
        String text3 = "Barney\r\n";

        engine.saveText( NAME1, text );
        engine.saveText( NAME1, text2 );
        engine.saveText( NAME1, text3 );

        WikiPage res = engine.getPage(NAME1);

        Assert.assertEquals("wrong version", 3, res.getVersion() );

        Assert.assertEquals("ver1", 1, engine.getPage( NAME1, 1 ).getVersion() );
        Assert.assertEquals("ver2", 2, engine.getPage( NAME1, 2 ).getVersion() );
        Assert.assertEquals("ver3", 3, engine.getPage( NAME1, 3 ).getVersion() );
}

    /**
     *  2.0.7 and before got this wrong.
     */
    @Test
    public void testGetOldVersionUTF8()
        throws Exception
    {
        String text = "\u00e5\u00e4\u00f6\r\n";
        String text2 = "barbar\u00f6\u00f6\r\n";
        String text3 = "Barney\u00e4\u00e4\r\n";

        engine.saveText( NAME1, text );
        engine.saveText( NAME1, text2 );
        engine.saveText( NAME1, text3 );

        WikiPage res = engine.getPage(NAME1);

        Assert.assertEquals("wrong version", 3, res.getVersion() );

        Assert.assertEquals("ver1", text, engine.getText( NAME1, 1 ) );
        Assert.assertEquals("ver2", text2, engine.getText( NAME1, 2 ) );
        Assert.assertEquals("ver3", text3, engine.getText( NAME1, 3 ) );
    }

    @Test
    public void testNonexistentPage()
    {
        Assert.assertNull( engine.getPage("fjewifjeiw") );
    }

    @Test
    public void testVersionHistory()
        throws Exception
    {
        String text = "diddo\r\n";
        String text2 = "barbar\r\n";
        String text3 = "Barney\r\n";

        engine.saveText( NAME1, text );
        engine.saveText( NAME1, text2 );
        engine.saveText( NAME1, text3 );

        Collection history = engine.getVersionHistory(NAME1);

        Assert.assertEquals( "size", 3, history.size() );
    }

    @Test
    public void testDelete()
        throws Exception
    {
        engine.saveText( NAME1, "v1" );
        engine.saveText( NAME1, "v2" );
        engine.saveText( NAME1, "v3" );

        PageManager mgr = engine.getPageManager();
        WikiPageProvider provider = mgr.getProvider();

		WikiPage p = new WikiPage(engine, NAME1);
		provider.deletePage(p);

        File f = new File( files, NAME1+AbstractFileProvider.FILE_EXT );
        Assert.assertFalse( "file exists", f.exists() );
    }

    @Test
    public void testDeleteVersion()
        throws Exception
    {
        engine.saveText( NAME1, "v1\r\n" );
        engine.saveText( NAME1, "v2\r\n" );
        engine.saveText( NAME1, "v3\r\n" );

        PageManager mgr = engine.getPageManager();
        WikiPageProvider provider = mgr.getProvider();

        List l = provider.getVersionHistory( NAME1 );
        Assert.assertEquals( "wrong # of versions", 3, l.size() );

		WikiPage p = new WikiPage(engine, NAME1);
		provider.deleteVersion(p, 2);

        l = provider.getVersionHistory( NAME1 );

        Assert.assertEquals( "wrong # of versions", 2, l.size() );

        Assert.assertEquals( "v1", "v1\r\n", provider.getPageText( NAME1, 1 ) );
        Assert.assertEquals( "v3", "v3\r\n", provider.getPageText( NAME1, 3 ) );

        try
        {
            provider.getPageText( NAME1, 2 );
            Assert.fail( "v2" );
        }
        catch( NoSuchVersionException e )
        {
            // This is expected
        }
    }


    @Test
    public void testChangeNote()
        throws Exception
    {
        WikiPage p = new WikiPage( engine, NAME1 );
        p.setAttribute(WikiPage.CHANGENOTE, "Test change" );
        WikiContext context = new WikiContext(engine,p);

        engine.saveText( context, "test" );

        WikiPage p2 = engine.getPage( NAME1 );

        Assert.assertEquals( "Test change", p2.getAttribute(WikiPage.CHANGENOTE) );
    }

    @Test
    public void testChangeNoteOldVersion()
        throws Exception
    {
        WikiPage p = new WikiPage( engine, NAME1 );


        WikiContext context = new WikiContext(engine,p);

        context.getPage().setAttribute(WikiPage.CHANGENOTE, "Test change" );
        engine.saveText( context, "test" );

        context.getPage().setAttribute(WikiPage.CHANGENOTE, "Change 2" );
        engine.saveText( context, "test2" );

        WikiPage p2 = engine.getPage( NAME1, 1 );

        Assert.assertEquals( "Test change", p2.getAttribute(WikiPage.CHANGENOTE) );

        WikiPage p3 = engine.getPage( NAME1, 2 );

        Assert.assertEquals( "Change 2", p3.getAttribute(WikiPage.CHANGENOTE) );
    }

    @Test
    public void testChangeNoteOldVersion2() throws Exception
    {
        WikiPage p = new WikiPage( engine, NAME1 );

        WikiContext context = new WikiContext(engine,p);

        context.getPage().setAttribute( WikiPage.CHANGENOTE, "Test change" );

        engine.saveText( context, "test" );

        for( int i = 0; i < 5; i++ )
        {
            WikiPage p2 = (WikiPage)engine.getPage( NAME1 ).clone();
            p2.removeAttribute(WikiPage.CHANGENOTE);

            context.setPage( p2 );

            engine.saveText( context, "test"+i );
        }

        WikiPage p3 = engine.getPage( NAME1, -1 );

        Assert.assertEquals( null, p3.getAttribute(WikiPage.CHANGENOTE) );
    }

    /*
     * Creates a file of the given name in the wiki page directory,
     * containing the data provided.
     */
    private void injectFile(String fileName, String fileContent)
        throws IOException
    {
        File ft = new File( files, fileName );
        Writer out = new FileWriter( ft );
        FileUtil.copyContents( new StringReader(fileContent), out );
        out.close();
    }
}
