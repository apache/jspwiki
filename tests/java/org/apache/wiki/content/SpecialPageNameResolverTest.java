package org.apache.wiki.content;

import java.net.URI;
import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiEngine;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SpecialPageNameResolverTest extends TestCase
{
    private SpecialPageNameResolver resolver;
    
    public void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        WikiEngine engine = new TestEngine( props );
        
        resolver = new SpecialPageNameResolver(engine);
    }
    
    public void testSpecialPageReference()
    {
        URI uri;
        uri = resolver.getSpecialPageURI( "RecentChanges" );
        assertEquals( "/RecentChanges.jsp", uri.toString() );
        
        uri = resolver.getSpecialPageURI( "FindPage" );
        assertEquals( "/Search.jsp", uri.toString() );
        
        // UserPrefs doesn't exist in our test properties
        uri = resolver.getSpecialPageURI( "UserPrefs" );
        assertNull( uri );
    }

    public static Test suite()
    {
        return new TestSuite(SpecialPageNameResolverTest.class);
    }

}
