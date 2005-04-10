package com.ecyrd.jspwiki.auth.modules;

import junit.framework.*;
import java.util.*;
import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.auth.*;
import com.ecyrd.jspwiki.auth.permissions.*;

public class PageAuthorizerTest
    extends TestCase
{
    TestEngine m_engine;

    public PageAuthorizerTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        
        m_engine = new TestEngine(props);

        String text1 = "Foobar.\n\n[{SET defaultpermissions='ALLOW EDIT Charlie;DENY VIEW Bob'}]\n\nBlood.";
        String text2 = "Foo";

        m_engine.saveText( "DefaultPermissions", text1 );
        m_engine.saveText( "TestPage", text2 );
    }

    public void tearDown()
    {
        TestEngine.deleteTestPage( "DefaultPermissions" );
        TestEngine.deleteTestPage( "TestPage" );
    }

     // TODO: Fix this test
    public void testDefaultPermissions()
    {
        AuthorizationManager mgr = m_engine.getAuthorizationManager();

        UserProfile wup = new UserProfile();
        wup.setName( "Charlie" );
        wup.setLoginStatus( UserProfile.PASSWORD );

        assertTrue( "Charlie", mgr.checkPermission( m_engine.getPage( "TestPage" ),
                                                    wup,
                                                    WikiPermission.newInstance( "edit" ) ) );

        /*
        wup.setName( "Bob" );
        assertTrue( "Bob", mgr.checkPermission( m_engine.getPage( "TestPage" ),
                                                wup,
                                                WikiPermission.newInstance( "view" ) ) );
         */                                                    
    }

    public static Test suite()
    {
        return new TestSuite( PageAuthorizerTest.class );
    }

}
