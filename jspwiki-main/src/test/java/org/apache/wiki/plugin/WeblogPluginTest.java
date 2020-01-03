package org.apache.wiki.plugin;

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.exceptions.WikiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class WeblogPluginTest {

    TestEngine testEngine = TestEngine.build();

    @AfterEach
    public void tearDown() throws Exception {
        testEngine.getPageManager().deletePage( "Test" );
    }

    @Test
    public void testWeblogEmpty() throws WikiException {
        final String src="[{WeblogPlugin days='90' allowComments='true'}]";
        testEngine.saveText( "Test1", src );

        final String res = testEngine.getI18nHTML( "Test1" );
        Assertions.assertEquals( "<div class=\"weblog\">\n</div>\n\n", res );
    }

    @Test
    public void testWeblogEntries() throws WikiException {
        final WeblogEntryPlugin wep = new WeblogEntryPlugin();
        testEngine.saveText( wep.getNewEntryPage( testEngine, "Test2" ), "My first blog entry, W00t!" );
        final String src="[{WeblogPlugin days='90' allowComments='true'}]";
        testEngine.saveText( "Test2", src );

        final String res = testEngine.getI18nHTML( "Test2" );
        Assertions.assertTrue( res.startsWith( "<div class=\"weblog\">\n<div class=\"weblogentry\">\n<div class=\"weblogentryheading\">\n" ), res );
        Assertions.assertTrue( res.contains( "<div class=\"weblogentrybody\">\nMy first blog entry, W00t!\n</div>\n" ), res );
    }

    @Test
    public void testWeblogEntriesWithPreviewAndNoTrimming() throws WikiException {
        final WeblogEntryPlugin wep = new WeblogEntryPlugin();
        // preview mantains complete paragraphs, as odds are that should help rendering a more well-formed html
        final String blogEntryPage = wep.getNewEntryPage( testEngine, "Test3" );
        testEngine.saveText( blogEntryPage, "My first blog entry, W00t!" );
        final String src="[{WeblogPlugin days='90' allowComments='true' preview='5'}]";
        testEngine.saveText( "Test3", src );

        final String res = testEngine.getI18nHTML( "Test3" );
        Assertions.assertTrue( res.startsWith( "<div class=\"weblog\">\n<div class=\"weblogentry\">\n<div class=\"weblogentryheading\">\n" ), res );
        Assertions.assertTrue( res.contains( "<div class=\"weblogentrybody\">\nMy first blog entry, W00t! <a href=\"/test/Wiki.jsp?page="+ blogEntryPage + "\">(more)</a>\n</div>\n" ), res );
    }

    @Test
    public void testWeblogEntriesWithPreviewAndTrimming() throws WikiException {
        final WeblogEntryPlugin wep = new WeblogEntryPlugin();
        final String blogEntryPage = wep.getNewEntryPage( testEngine, "Test4" );
        testEngine.saveText( blogEntryPage, "Another blog entry \n \n this time about some serious stuff!" );
        final String src="[{WeblogPlugin days='90' allowComments='true' preview='5'}]";
        testEngine.saveText( "Test4", src );

        final String res = testEngine.getI18nHTML( "Test4" );
        Assertions.assertTrue( res.startsWith( "<div class=\"weblog\">\n<div class=\"weblogentry\">\n<div class=\"weblogentryheading\">\n" ), res );
        Assertions.assertTrue( res.contains( "<div class=\"weblogentrybody\">\nAnother blog entry  <a href=\"/test/Wiki.jsp?page="+ blogEntryPage + "\">(more)</a>\n</div>\n" ), res );
    }

}
