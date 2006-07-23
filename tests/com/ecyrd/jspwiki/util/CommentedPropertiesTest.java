
package com.ecyrd.jspwiki.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class CommentedPropertiesTest extends TestCase
{
    Properties m_props = new CommentedProperties();
    
    public void setUp() throws IOException
    {
        InputStream in = CommentedPropertiesTest.class.getClassLoader().getResourceAsStream( "test.properties" );
        m_props.load( in );
        in.close();
    }
    
    public void testLoadProperties()
    {
        assertEquals( 5, m_props.keySet().size() );
        assertEquals( "Foo", m_props.get( "testProp1" ) );
        assertEquals( "Bar", m_props.get( "testProp2" ) );
        assertEquals( "", m_props.get( "testProp3" ) );
        assertEquals( "FooAgain", m_props.get( "testProp4" ) );
        assertEquals( "BarAgain", m_props.get( "testProp5" ) );
        assertNull( m_props.get( "testProp6" ) );
        
        // String we read in, including comments is 208 bytes
        assertEquals( 208, m_props.toString().length() );
    }
    
    public void testSetProperty()
    {
        m_props.setProperty( "testProp1", "newValue" );
        
        // Length of stored string should now be 5 bytes more
        assertEquals( 208+5, m_props.toString().length() );
        assertTrue( m_props.toString().contains( "newValue" ) );
        
        // Create new property; should add 21 (1+7+3+9+1) bytes
        m_props.setProperty( "newProp", "newValue2" );
        m_props.containsKey( "newProp" );
        m_props.containsValue( "newValue2" );
        assertEquals( 208+5+21, m_props.toString().length() );
        assertTrue( m_props.toString().contains( "newProp = newValue2" ) );
    }
    
    public void testRemove()
    {
        // Remove prop 1; length of stored string should be 14 (1+9+1+3) bytes less
        m_props.remove( "testProp1" );
        assertFalse( m_props.containsKey( "testProp1" ) );
        assertEquals( 208-14, m_props.toString().length() );
        
        // Remove prop 2; length of stored string should be 15 (1+9+2+3) bytes less
        m_props.remove( "testProp2" );
        assertFalse( m_props.containsKey( "testProp2" ) );
        assertEquals( 208-14-15, m_props.toString().length() );
        
        // Remove prop 3; length of stored string should be 11 (1+9+1) bytes less
        m_props.remove( "testProp3" );
        assertFalse( m_props.containsKey( "testProp3" ) );
        assertEquals( 208-14-15-11, m_props.toString().length() );
        
        // Remove prop 4; length of stored string should be 19 (1+9+1+8) bytes less
        m_props.remove( "testProp4" );
        assertFalse( m_props.containsKey( "testProp4" ) );
        assertEquals( 208-14-15-11-19, m_props.toString().length() );
        
        // Remove prop 5; length of stored string should be 19 (1+9+1+8) bytes less
        m_props.remove( "testProp5" );
        assertFalse( m_props.containsKey( "testProp5" ) );
        assertEquals( 208-14-15-11-19-19, m_props.toString().length() );
    }
    
    public void testStore() throws Exception
    {
        // Write results to a new file
        File outFile = createFile( "test2.properties" );
        OutputStream out = new FileOutputStream( outFile );
        m_props.store( out, null );
        
        // Load the file into new props object; should return identical strings
        Properties props2 = new CommentedProperties();
        InputStream in = CommentedPropertiesTest.class.getClassLoader().getResourceAsStream( "test2.properties" );
        props2.load( in );
        in.close();
        assertEquals( m_props.toString(), props2.toString() );
        
        // Remove props1, 2, 3 & resave props to new file
        m_props.remove( "testProp1" );
        m_props.remove( "testProp2" );
        m_props.remove( "testProp3" );
        outFile = createFile( "test3.properties" );
        out = new FileOutputStream( outFile );
        m_props.store( out, null );
        
        // Load the new file; should not have props1/2/3 & is shorter
        Properties props3 = new CommentedProperties();
        in = CommentedPropertiesTest.class.getClassLoader().getResourceAsStream( "test3.properties" );
        props3.load( in );
        in.close();
        assertNotSame( m_props.toString(), props3.toString() );
        assertFalse( props3.containsKey( "testProp1" ) );
        assertFalse( props3.containsKey( "testProp2" ) );
        assertFalse( props3.containsKey( "testProp3" ) );
        assertTrue( props3.containsKey( "testProp4" ) );
        assertTrue( props3.containsKey( "testProp5" ) );
        
        // Clean up
        File file = getFile( "test2.properties" );
        if ( file != null && file.exists() )
        {
            file.delete();
        }
        file = getFile( "test3.properties" );
        if ( file != null && file.exists() )
        {
            file.delete();
        }
    }

    private File createFile( String file ) throws URISyntaxException
    {
        // Get the test.properties file
        URL url = CommentedPropertiesTest.class.getClassLoader().getResource( "test.properties" );
        if ( url == null )
        {
            throw new IllegalStateException( "Very odd. We can't find test.properties!" );
        }
        
        // Construct new file in same directory
        File testFile = new File( url.toURI() );
        File dir = testFile.getParentFile();
        return new File( dir, file );
    }
    
    private File getFile( String name )
    {
        // Get the test.properties file
        URL url = CommentedPropertiesTest.class.getClassLoader().getResource( name );
        if ( url == null )
        {
            throw new IllegalStateException( "Very odd. We can't find test.properties!" );
        }
        // Return the file
        File file = null;
        try
        {
            file = new File( url.toURI() );
        }
        catch ( URISyntaxException e )
        {
            // No worries; just return null
        }
        return file;
    }
        
    public static Test suite()
    {
        return new TestSuite( CommentedPropertiesTest.class );
    }
}


