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

package org.apache.wiki.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class CommentedPropertiesTest
{

    Properties m_props = new CommentedProperties();
    // file size of the properties test file in bytes
    private int m_propFileSize;

    @BeforeEach
    public void setUp() throws IOException
    {
        InputStream in = CommentedPropertiesTest.class.getClassLoader().getResourceAsStream( "test.properties" );
        m_props.load( in );
        // CommentedProperties always internally uses \n as EOL, as opposed to a File which uses, well, the given EOL of the File.  
        // Thus, executing this test when test.properties has another EOL (like f.ex when git cloning having core.autocrlf=true on
        // windows) using File.length() would fail this test. 
        m_propFileSize = m_props.toString().length();
        in.close();
    }

    @Test
    public void testLoadProperties()
    {
        Assertions.assertEquals( 5, m_props.keySet().size() );
        Assertions.assertEquals( "Foo", m_props.get( "testProp1" ) );
        Assertions.assertEquals( "Bar", m_props.get( "testProp2" ) );
        Assertions.assertEquals( "", m_props.get( "testProp3" ) );
        Assertions.assertEquals( "FooAgain", m_props.get( "testProp4" ) );
        Assertions.assertEquals( "BarAgain", m_props.get( "testProp5" ) );
        Assertions.assertNull( m_props.get( "testProp6" ) );

        // String we read in, including comments is c_stringOffset bytes
        Assertions.assertEquals( m_propFileSize, m_props.toString().length() );
    }

    @Test
    public void testSetProperty()
    {
        m_props.setProperty( "testProp1", "newValue" );

        // Length of stored string should now be 5 bytes more
        Assertions.assertEquals( m_propFileSize+5, m_props.toString().length() );
        Assertions.assertTrue( m_props.toString().indexOf( "newValue" ) != -1 );

        // Create new property; should add 21 (1+7+3+9+1) bytes
        m_props.setProperty( "newProp", "newValue2" );
        m_props.containsKey( "newProp" );
        m_props.containsValue( "newValue2" );
        Assertions.assertEquals( m_propFileSize+5+21, m_props.toString().length() );
        Assertions.assertTrue( m_props.toString().indexOf( "newProp = newValue2" ) != -1 );
    }

    @Test
    public void testRemove()
    {
        // Remove prop 1; length of stored string should be 14 (1+9+1+3) bytes less
        m_props.remove( "testProp1" );
        Assertions.assertFalse( m_props.containsKey( "testProp1" ) );
        Assertions.assertEquals( m_propFileSize-14, m_props.toString().length() );

        // Remove prop 2; length of stored string should be 15 (1+9+2+3) bytes less
        m_props.remove( "testProp2" );
        Assertions.assertFalse( m_props.containsKey( "testProp2" ) );
        Assertions.assertEquals( m_propFileSize-14-15, m_props.toString().length() );

        // Remove prop 3; length of stored string should be 11 (1+9+1) bytes less
        m_props.remove( "testProp3" );
        Assertions.assertFalse( m_props.containsKey( "testProp3" ) );
        Assertions.assertEquals( m_propFileSize-14-15-11, m_props.toString().length() );

        // Remove prop 4; length of stored string should be 19 (1+9+1+8) bytes less
        m_props.remove( "testProp4" );
        Assertions.assertFalse( m_props.containsKey( "testProp4" ) );
        Assertions.assertEquals( m_propFileSize-14-15-11-19, m_props.toString().length() );

        // Remove prop 5; length of stored string should be 19 (1+9+1+8) bytes less
        m_props.remove( "testProp5" );
        Assertions.assertFalse( m_props.containsKey( "testProp5" ) );
        Assertions.assertEquals( m_propFileSize-14-15-11-19-19, m_props.toString().length() );
    }

    @Test
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
        Assertions.assertEquals( m_props.toString(), props2.toString() );

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
        Assertions.assertNotSame( m_props.toString(), props3.toString() );
        Assertions.assertFalse( props3.containsKey( "testProp1" ) );
        Assertions.assertFalse( props3.containsKey( "testProp2" ) );
        Assertions.assertFalse( props3.containsKey( "testProp3" ) );
        Assertions.assertTrue( props3.containsKey( "testProp4" ) );
        Assertions.assertTrue( props3.containsKey( "testProp5" ) );

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
        File testFile = new File( new URI(url.toString()) );
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
            file = new File( new URI(url.toString()) );
        }
        catch (URISyntaxException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return file;
    }

}
