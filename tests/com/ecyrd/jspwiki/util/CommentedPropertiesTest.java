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

package com.ecyrd.jspwiki.util;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class CommentedPropertiesTest extends TestCase
{
    CommentedProperties m_props = new CommentedProperties();

    public void setUp() throws IOException
    {
        InputStream in = CommentedPropertiesTest.class.getClassLoader().getResourceAsStream( "test.properties" );
        m_props.load( in );
        in.close();
    }

    public void testLoadProperties()
    {
        assertEquals( 6, m_props.keySet().size() );
        assertEquals( "Foo", m_props.get( "testProp1" ) );
        assertEquals( "Bar", m_props.get( "testProp2" ) );
        assertEquals( "", m_props.get( "testProp3" ) );
        assertEquals( "FooAgain", m_props.get( "testProp4" ) );
        assertEquals( "BarAgain", m_props.get( "testProp5" ) );
        assertNotNull( m_props.get( "testProp6" ) );
        assertNull( m_props.get( "testProp7" ) );
    }

    public void testSetProperty()
    {
        int propsLen = m_props.toString().length();
        m_props.setProperty( "testProp1", "newValue" );

        // Length of stored string should now be 5 bytes more
        assertEquals( propsLen + 5, m_props.toString().length() );
        assertTrue( m_props.toString().indexOf( "newValue" ) != -1 );

        // Create new property; should add 20 (7+3+9+1) bytes
        m_props.setProperty( "newProp", "newValue2" );
        m_props.containsKey( "newProp" );
        m_props.containsValue( "newValue2" );
        assertEquals( propsLen + 5 + 20, m_props.toString().length() );
        assertTrue( m_props.toString().indexOf( "newProp = newValue2" ) != -1 );
    }

    public void testGetComment()
    {
        String cr = System.getProperty( "line.separator" );
        assertEquals( "# This is a sample properties file with comments", m_props.getComment( "testProp1" ) );
        assertEquals( "# This is a comment" + cr + "#   with two lines", m_props.getComment( "testProp2" ) );
        assertEquals( "# This is a property with no value", m_props.getComment( "testProp3" ) );
        assertEquals( "# Two final properties", m_props.getComment( "testProp4" ) );
        assertEquals( null, m_props.getComment( "testProp5" ) );
        assertEquals( "# This is a property that spans more than 1 line", m_props.getComment( "testProp6" ) );
    }
    
    public void testSetComment()
    {
        m_props.setProperty( "testProp7", "TestValue","This is a comment" );
        assertEquals( "TestValue", m_props.getProperty( "testProp7" ) );
        assertEquals( "# This is a comment", m_props.getComment( "testProp7" ) );
        
        // Make sure it was actually added to the string returned by toString()
        assertTrue( m_props.toString().contains( "# This is a comment\ntestProp7 = TestValue" ) );
    }
    
    public void testMultilineProperties()
    {
        assertTrue( m_props.containsKey( "testProp6" ) );
        assertEquals( "Your new properties have been saved to jspwiki.properties.", m_props.get( "testProp6" ) );
    }

    public void testRemove()
    {
        int propsLen = m_props.toString().length();

        // Remove prop 1; length of stored string should be 16 (9+3+3+1) bytes
        // less for property
        // and 49 bytes less for the comment above it. Total difference: 65
        // bytes
        m_props.remove( "testProp1" );
        assertFalse( m_props.containsKey( "testProp1" ) );
        assertEquals( propsLen - 65, m_props.toString().length() );

        // Remove prop 2; length of stored string should be 55 (20+19+16) bytes
        // less
        m_props.remove( "testProp2" );
        assertFalse( m_props.containsKey( "testProp2" ) );
        assertEquals( propsLen - 65 - 55, m_props.toString().length() );

        // Remove prop 3; length of stored string should be 48 (35+13) bytes
        // less
        m_props.remove( "testProp3" );
        assertFalse( m_props.containsKey( "testProp3" ) );
        assertEquals( propsLen - 65 - 55 - 48, m_props.toString().length() );

        // Remove prop 4; length of stored string should be 44 (23+21) bytes
        // less
        m_props.remove( "testProp4" );
        assertFalse( m_props.containsKey( "testProp4" ) );
        assertEquals( propsLen - 65 - 55 - 48 - 44, m_props.toString().length() );

        // Remove prop 5; length of stored string should be 21 bytes less
        m_props.remove( "testProp5" );
        assertFalse( m_props.containsKey( "testProp5" ) );
        assertEquals( propsLen - 65 - 55 - 48 - 44 - 21, m_props.toString().length() );
    }

    public void testStore() throws Exception
    {
        // Write results to a new file
        File outFile = createFile( "test2.properties" );
        OutputStream out = new FileOutputStream( outFile );
        m_props.store( out, null );
        out.close();

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
        out.close();

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
        if( file != null && file.exists() )
        {
            file.delete();
        }
        file = getFile( "test3.properties" );
        if( file != null && file.exists() )
        {
            file.delete();
        }
    }

    private File createFile( String file ) throws URISyntaxException
    {
        // Get the test.properties file
        URL url = CommentedPropertiesTest.class.getClassLoader().getResource( "test.properties" );
        if( url == null )
        {
            throw new IllegalStateException( "Very odd. We can't find test.properties!" );
        }

        // Construct new file in same directory
        File testFile = new File( new URI( url.toString() ) );
        File dir = testFile.getParentFile();
        return new File( dir, file );
    }

    private File getFile( String name )
    {
        // Get the test.properties file
        URL url = CommentedPropertiesTest.class.getClassLoader().getResource( name );
        if( url == null )
        {
            throw new IllegalStateException( "Very odd. We can't find test.properties!" );
        }
        // Return the file
        File file = null;

        try
        {
            file = new File( new URI( url.toString() ) );
        }
        catch( URISyntaxException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return file;
    }

    public static Test suite()
    {
        return new TestSuite( CommentedPropertiesTest.class );
    }
}
