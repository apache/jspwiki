
package com.ecyrd.jspwiki.plugin;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiContext;

public class GroupsTest extends TestCase
{
    Properties props = new Properties();
    TestEngine testEngine;
    WikiContext context;
    PluginManager manager;
    
    public GroupsTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( TestEngine.findTestProperties() );

        testEngine = new TestEngine(props);
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
        
        testEngine.deletePage( "Test" );
    }
    
    public void testTag() throws Exception
    {
        String src="[{Groups}]";
        
        testEngine.saveText( "Test", src );
        
        String res = testEngine.getHTML( "Test" );
        
        assertEquals( "<a href=\"/Group.jsp?group=Admin\">Admin</a>, " 
                + "<a href=\"/Group.jsp?group=Art\">Art</a>, "
                + "<a href=\"/Group.jsp?group=Literature\">Literature</a>, "
                + "<a href=\"/Group.jsp?group=TV\">TV</a>\n"
                , res );
    }

    public static Test suite()
    {
        return new TestSuite( GroupsTest.class );
    }
}
