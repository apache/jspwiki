
package com.ecyrd.jspwiki;

import java.util.Properties;
import org.apache.log4j.PropertyConfigurator;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.*;

public class AllTests extends TestCase
{
    //
    //  Ensure everything runs properly and that we can locate all necessary
    //  thingies.
    //
    static
    {
        Properties props = new Properties();
        try
        {
            InputStream pin = AllTests.class.getClassLoader().getResourceAsStream("/jspwiki.properties");
            if( pin == null )
            {
                fail( "No property file found!" );
            }
            props.load( pin );
            PropertyConfigurator.configure(props);
        }
        catch( IOException e ) 
        {
        }
    }

    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite();

        suite.addTest( FileUtilTest.suite() );
        suite.addTest( PageManagerTest.suite() );
        suite.addTest( TextUtilTest.suite() );
        suite.addTest( TranslatorReaderTest.suite() );
        suite.addTest( WikiEngineTest.suite() );
        suite.addTest( com.ecyrd.jspwiki.plugin.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.xmlrpc.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.providers.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.auth.AllTests.suite() );

        return suite;
    }
}
