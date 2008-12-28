package com.ecyrd.jspwiki.plugin;

import java.util.Properties;

import org.apache.jspwiki.api.PluginException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiContext;

public class PluginIndexPluginTest extends TestCase
{
    Properties props = new Properties();

    TestEngine engine;

    WikiContext context;

    PluginManager manager;

    public PluginIndexPluginTest( String s )
    {
        super( s );
    }

    public void setUp() throws Exception
    {
        props.load( TestEngine.findTestProperties() );

        // prevent "create" links :
        props.setProperty( "jspwiki.translatorReader.camelCaseLinks", "false" );

        engine = new TestEngine( props );

        manager = new PluginManager( engine, props );

        context = engine.getWikiContextFactory().newViewContext( engine.createPage( "TestPage" ) );
    }

    public void tearDown()
    {
        TestEngine.emptyWorkDir();
    }

    public static Test suite()
    {
        return new TestSuite( PluginIndexPluginTest.class );
    }

    /**
     * Test for : PluginIndexPlugin details=false Shows only the plugin names
     * 
     * @throws PluginException
     */
    public void testDetailsFalse() throws PluginException
    {
        String expectedResult = "<p />\n<table class=\"wikitable\" border=\"1\"><tr class=\"odd\"><th>name</th></tr>\n<tr><td>BugReportHandler</td></tr>\n<tr class=\"odd\"><td>Counter</td></tr>\n<tr><td>CurrentTimePlugin</td></tr>\n<tr class=\"odd\"><td>Denounce</td></tr>\n<tr><td>FormClose</td></tr>\n<tr class=\"odd\"><td>FormInput</td></tr>\n<tr><td>FormOpen</td></tr>\n<tr class=\"odd\"><td>FormOutput</td></tr>\n<tr><td>FormSelect</td></tr>\n<tr class=\"odd\"><td>FormSet</td></tr>\n<tr><td>FormTextarea</td></tr>\n<tr class=\"odd\"><td>Groups</td></tr>\n<tr><td>IfPlugin</td></tr>\n<tr class=\"odd\"><td>Image</td></tr>\n<tr><td>IndexPlugin</td></tr>\n<tr class=\"odd\"><td>InsertPage</td></tr>\n<tr><td>JavaScriptPlugin</td></tr>\n<tr class=\"odd\"><td>ListLocksPlugin</td></tr>\n<tr><td>Note</td></tr>\n<tr class=\"odd\"><td>PluginIndexPlugin</td></tr>\n<tr><td>RPCSamplePlugin</td></tr>\n<tr class=\"odd\"><td>RecentChangesPlugin</td></tr>\n<tr><td>ReferredPagesPlugin</td></tr>\n<tr class=\"odd\"><td>ReferringPagesPlugin</td></tr>\n<tr><td>SamplePlugin</td></tr>\n<tr class=\"odd\"><td>Search</td></tr>\n<tr><td>SessionsPlugin</td></tr>\n<tr class=\"odd\"><td>TableOfContents</td></tr>\n<tr><td>UndefinedPagesPlugin</td></tr>\n<tr class=\"odd\"><td>UnusedPagesPlugin</td></tr>\n<tr><td>WeblogArchivePlugin</td></tr>\n<tr class=\"odd\"><td>WeblogEntryPlugin</td></tr>\n<tr><td>WeblogPlugin</td></tr></table>";
        String result = manager.execute( context, "{PluginIndexPlugin details=false}" );

        // now this is a very straightforward test, if a new plugin is added, a plugin is removed, this test will have to be modified 
        assertEquals( expectedResult, result );
    }

    /**
     * Test for : PluginIndexPlugin details=true Shows the plugin names
     * including all attributes
     * 
     * @throws PluginException
     */
    public void testDetailsTrue() throws PluginException
    {
        String expectedResult = "<p />\n<table class=\"wikitable\" border=\"1\"><tr class=\"odd\"><th>Name</th><th>Class Name</th><th>alias's</th><th>author</th><th>minVersion</th><th>maxVersion</th><th>adminBean Class</th></tr>\n<tr><td>BugReportHandler</td><td>com.ecyrd.jspwiki.plugin.BugReportHandler</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr class=\"odd\"><td>Counter</td><td>com.ecyrd.jspwiki.plugin.Counter</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr><td>CurrentTimePlugin</td><td>com.ecyrd.jspwiki.plugin.CurrentTimePlugin</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr class=\"odd\"><td>Denounce</td><td>com.ecyrd.jspwiki.plugin.Denounce</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr><td>FormClose</td><td>com.ecyrd.jspwiki.forms.FormClose</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr class=\"odd\"><td>FormInput</td><td>com.ecyrd.jspwiki.forms.FormInput</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr><td>FormOpen</td><td>com.ecyrd.jspwiki.forms.FormOpen</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr class=\"odd\"><td>FormOutput</td><td>com.ecyrd.jspwiki.forms.FormOutput</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr><td>FormSelect</td><td>com.ecyrd.jspwiki.forms.FormSelect</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr class=\"odd\"><td>FormSet</td><td>com.ecyrd.jspwiki.forms.FormSet</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr><td>FormTextarea</td><td>com.ecyrd.jspwiki.forms.FormTextarea</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr class=\"odd\"><td>Groups</td><td>com.ecyrd.jspwiki.plugin.Groups</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr><td>IfPlugin</td><td>com.ecyrd.jspwiki.plugin.IfPlugin</td><td>&nbsp; If</td><td>AnonymousCoward</td><td>0.0</td><td>1000000.0</td><td /></tr>\n<tr class=\"odd\"><td>Image</td><td>com.ecyrd.jspwiki.plugin.Image</td><td>&nbsp; </td><td>JSPWiki development group</td><td>0.0</td><td>1000000.0</td><td /></tr>\n<tr><td>IndexPlugin</td><td>com.ecyrd.jspwiki.plugin.IndexPlugin</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr class=\"odd\"><td>InsertPage</td><td>com.ecyrd.jspwiki.plugin.InsertPage</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr><td>JavaScriptPlugin</td><td>com.ecyrd.jspwiki.plugin.JavaScriptPlugin</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr class=\"odd\"><td>ListLocksPlugin</td><td>com.ecyrd.jspwiki.plugin.ListLocksPlugin</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr><td>Note</td><td>com.ecyrd.jspwiki.plugin.Note</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr class=\"odd\"><td>PluginIndexPlugin</td><td>com.ecyrd.jspwiki.plugin.PluginIndexPlugin</td><td>&nbsp; </td><td>Harry Metske</td><td>3.0</td><td>1000000</td><td /></tr>\n<tr><td>RPCSamplePlugin</td><td>com.ecyrd.jspwiki.plugin.RPCSamplePlugin</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr class=\"odd\"><td>RecentChangesPlugin</td><td>com.ecyrd.jspwiki.plugin.RecentChangesPlugin</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr><td>ReferredPagesPlugin</td><td>com.ecyrd.jspwiki.plugin.ReferredPagesPlugin</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr class=\"odd\"><td>ReferringPagesPlugin</td><td>com.ecyrd.jspwiki.plugin.ReferringPagesPlugin</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr><td>SamplePlugin</td><td>com.ecyrd.jspwiki.plugin.SamplePlugin</td><td>&nbsp; samplealias2 samplealias</td><td>Urgle Burgle</td><td>0.0</td><td>1000000.0</td><td /></tr>\n<tr class=\"odd\"><td>Search</td><td>com.ecyrd.jspwiki.plugin.Search</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr><td>SessionsPlugin</td><td>com.ecyrd.jspwiki.plugin.SessionsPlugin</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr class=\"odd\"><td>TableOfContents</td><td>com.ecyrd.jspwiki.plugin.TableOfContents</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr><td>UndefinedPagesPlugin</td><td>com.ecyrd.jspwiki.plugin.UndefinedPagesPlugin</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr class=\"odd\"><td>UnusedPagesPlugin</td><td>com.ecyrd.jspwiki.plugin.UnusedPagesPlugin</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr><td>WeblogArchivePlugin</td><td>com.ecyrd.jspwiki.plugin.WeblogArchivePlugin</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr class=\"odd\"><td>WeblogEntryPlugin</td><td>com.ecyrd.jspwiki.plugin.WeblogEntryPlugin</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n<tr><td>WeblogPlugin</td><td>com.ecyrd.jspwiki.plugin.WeblogPlugin</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr></table>";
        String result = manager.execute( context, "{PluginIndexPlugin details=true}" );
        
        // now this is a very straightforward test, if a new plugin is added, a plugin is removed, this test will have to be modified 
        assertEquals( expectedResult, result );
    }
}
