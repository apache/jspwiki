package org.apache.wiki.search.tika;

import net.sf.ehcache.CacheManager;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.search.SearchResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Properties;


public class TikaSearchProviderTest {

    private static final long SLEEP_TIME = 2_000L;
    private static final int SLEEP_COUNT = 50;
    TestEngine engine;
    Properties props;

    @BeforeEach
    void setUp() throws Exception {
        props = TestEngine.getTestProperties();
        TestEngine.emptyWorkDir( props );
        CacheManager.getInstance().removeAllCaches();

        engine = new TestEngine( props );
    }

    @Test
    void testGetAttachmentContent() throws Exception {
        engine.saveText( "test-tika", "blablablabla" );
        byte[] filePng = Files.readAllBytes( Paths.get( TikaSearchProviderTest.class.getClassLoader().getResource( "favicon.png" ).toURI() ) );
        byte[] filePdf = Files.readAllBytes( Paths.get( TikaSearchProviderTest.class.getClassLoader().getResource( "aaa-diagram.pdf" ).toURI() ) );
        engine.addAttachment( "test-tika", "aaa-diagram.pdf", filePdf );
        engine.addAttachment( "test-tika", "favicon.png", filePng );

        engine.getSearchManager().getSearchEngine().reindexPage( engine.getPage( "test-tika" ) );
        Collection< SearchResult > res = waitForIndex( "favicon.png" , "testGetAttachmentContent" );
        Assertions.assertNotNull( res );
        Assertions.assertEquals( 1, res.size(), debugSearchResults( res ) );

        res = waitForIndex( "application\\/pdf" , "testGetAttachmentContent" );
        Assertions.assertNotNull( res );
        Assertions.assertEquals( 1, res.size(), debugSearchResults( res ) );
    }

    String debugSearchResults( Collection< SearchResult > res ) {
        StringBuilder sb = new StringBuilder();
        for( SearchResult next : res ) {
            sb.append( System.lineSeparator() + "* page: " + next.getPage() );
            for( String s : next.getContexts() ) {
                sb.append( System.lineSeparator() + "** snippet: " + s );
            }
        }
        return sb.toString();
    }

    /**
     * Should cover for both index and initial delay
     */
    Collection<SearchResult> waitForIndex( String text, String testName ) throws Exception {
        Collection< SearchResult > res = null;
        for( long l = 0; l < SLEEP_COUNT; l++ ) {
            if( res == null || res.isEmpty() ) {
                Thread.sleep( SLEEP_TIME );
            } else {
                break;
            }
            MockHttpServletRequest request = engine.newHttpRequest();
            WikiContext ctx = engine.createContext( request, WikiContext.EDIT );

            res = engine.getSearchManager().findPages( text, ctx );

        }
        return res;
    }

}