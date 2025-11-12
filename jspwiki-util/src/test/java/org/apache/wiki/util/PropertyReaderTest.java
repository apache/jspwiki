/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); fyou may not use this file except in compliance
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.servlet.ServletContext;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.mockito.Mockito.mock;


/**
 * Unit test for PropertyReader.
 */
class PropertyReaderTest {

    @Test
    void testLocateClassPathResource() {
        Assertions.assertEquals( "/ini/jspwiki.properties", PropertyReader.createResourceLocation( "ini", "jspwiki.properties" ) );
        Assertions.assertEquals( "/ini/jspwiki.properties", PropertyReader.createResourceLocation( null, "ini/jspwiki.properties" ) );
        Assertions.assertEquals( "/ini/jspwiki.properties", PropertyReader.createResourceLocation( null, "/ini/jspwiki.properties" ) );
        Assertions.assertEquals( "/jspwiki-custom.properties", PropertyReader.createResourceLocation( null, "/jspwiki-custom.properties" ) );
        Assertions.assertEquals( "/jspwiki.custom.cascade.1.ini", PropertyReader.createResourceLocation( null, "jspwiki.custom.cascade.1.ini" ) );
        Assertions.assertEquals( "/WEB-INF/classes/jspwiki-custom.properties", PropertyReader.createResourceLocation( "WEB-INF/classes", PropertyReader.CUSTOM_JSPWIKI_CONFIG ) );
        Assertions.assertEquals( "/WEB-INF/classes/jspwiki-custom.properties", PropertyReader.createResourceLocation( "/WEB-INF/classes", PropertyReader.CUSTOM_JSPWIKI_CONFIG ) );
        Assertions.assertEquals( "/WEB-INF/classes/jspwiki-custom.properties", PropertyReader.createResourceLocation( "/WEB-INF/classes/", PropertyReader.CUSTOM_JSPWIKI_CONFIG ) );
    }

    @Test
    void testVariableExpansion() {
        final Properties p = new Properties();
        p.put( "var.basedir", "/p/mywiki" );
        p.put( "jspwiki.fileSystemProvider.pageDir", "$basedir/www/" );
        p.put( "jspwiki.basicAttachmentProvider.storageDir", "$basedir/www/" );
        p.put( "jspwiki.workDir", "$basedir/wrk/" );
        p.put( "jspwiki.xyz", "test basedir" ); //don't touch this

        PropertyReader.expandVars( p );

        Assertions.assertEquals( "/p/mywiki/www/", p.getProperty( "jspwiki.fileSystemProvider.pageDir" ) );
        Assertions.assertEquals( "/p/mywiki/www/", p.getProperty( "jspwiki.basicAttachmentProvider.storageDir" ) );
        Assertions.assertEquals( "/p/mywiki/www/", p.getProperty( "jspwiki.fileSystemProvider.pageDir" ) );
        Assertions.assertTrue( p.getProperty( "jspwiki.workDir" ).endsWith( "/p/mywiki/wrk/" ) );
        Assertions.assertTrue( p.getProperty( "jspwiki.xyz" ).endsWith( "test basedir" ) ); //don't touch this
        Assertions.assertFalse( p.getProperty( "jspwiki.workDir" ).endsWith( "$basedir/wrk/" ) );
    }

    @Test
    void testVariableExpansion2() {
        final Properties p = new Properties();

        //this time, declare the var at the end... (should overwrite this one);
        p.put( "var.basedir", "xxx" );

        p.put( "jspwiki.fileSystemProvider.pageDir", "$basedir/www/" );
        p.put( "jspwiki.basicAttachmentProvider.storageDir", "$basedir/www/" );
        p.put( "jspwiki.workDir", "$basedir/wrk/" );
        p.put( "jspwiki.xyz", "test basedir" ); //don't touch this
        p.put( "jspwiki.abc", "test $x2" ); //don't touch this

        p.put( "var.basedir", " /p/mywiki" ); //note that this var has a space at the beginning...
        p.put( "var.x2", " wiki " ); //note that this var has a space at the beginning...

        PropertyReader.expandVars( p );

        Assertions.assertEquals( "/p/mywiki/www/", p.getProperty( "jspwiki.fileSystemProvider.pageDir" ) );
        Assertions.assertEquals( "/p/mywiki/www/", p.getProperty( "jspwiki.basicAttachmentProvider.storageDir" ) );
        Assertions.assertEquals( "/p/mywiki/www/", p.getProperty( "jspwiki.fileSystemProvider.pageDir" ) );
        Assertions.assertTrue( p.getProperty( "jspwiki.workDir" ).endsWith( "/p/mywiki/wrk/" ) );
        Assertions.assertTrue( p.getProperty( "jspwiki.xyz" ).endsWith( "test basedir" ) ); //don't touch this
        Assertions.assertFalse( p.getProperty( "jspwiki.workDir" ).endsWith( "$basedir/wrk/" ) );
        Assertions.assertTrue( p.getProperty( "jspwiki.abc" ).endsWith( "test wiki" ) );
    }

    @Test
    void testMultipleVariableExpansion() {
        final Properties p = new Properties();

        //this time, declare the var at the end... (should overwrite this one);
        p.put( "var.x1", "a" );
        p.put( "var.x2", "b" );

        p.put( "jspwiki.x1", "$x1" );
        p.put( "jspwiki.x2", "$x2" );
        p.put( "jspwiki.x3", "$x1/$x2" );

        PropertyReader.expandVars( p );

        Assertions.assertEquals( "a", p.getProperty( "jspwiki.x1" ) );
        Assertions.assertEquals( "b", p.getProperty( "jspwiki.x2" ) );
        Assertions.assertEquals( "a/b", p.getProperty( "jspwiki.x3" ) );
    }

    @Test
    void testCollectPropertiesFrom() {
        final Map< String, String > sut = new HashMap<>();
        sut.put( "jspwiki_frontPage", "Main" );
        sut.put( "secretEnv", "asdasd" );

        final Map< String, String > test = PropertyReader.collectPropertiesFrom( sut );

        Assertions.assertEquals( "Main", test.get( "jspwiki.frontPage" ) );
        Assertions.assertNull( test.get( "secretEnv" ) );
    }

    @Test
    void testSetWorkDir() {
        final Properties properties = new Properties();
        final ServletContext servletContext = mock(ServletContext.class);
        final File tmp = new File( "/tmp" );
        Mockito.when(servletContext.getAttribute( "jakarta.servlet.context.tempdir" ) ).thenReturn( tmp );

        PropertyReader.setWorkDir( servletContext, properties );

        // Test when the "jspwiki.workDir" is not set, it should get set to servlet's temporary directory
        PropertyReader.setWorkDir(servletContext, properties);
        String workDir = properties.getProperty("jspwiki.workDir");
        Assertions.assertEquals(tmp.getAbsolutePath(), workDir);

        // Test when the "jspwiki.workDir" is set, it should remain as it is
        properties.setProperty("jspwiki.workDir", "/custom/dir");
        PropertyReader.setWorkDir(servletContext, properties);
        workDir = properties.getProperty("jspwiki.workDir");
        Assertions.assertEquals("/custom/dir", workDir);

        // Test when the servlet's temporary directory is null, it should get set to system's temporary directory
        Mockito.when( servletContext.getAttribute( "jakarta.servlet.context.tempdir" ) ).thenReturn( null );
        properties.remove( "jspwiki.workDir" );
        PropertyReader.setWorkDir( servletContext, properties );
        workDir = properties.getProperty( "jspwiki.workDir" );
        Assertions.assertEquals( System.getProperty( "java.io.tmpdir" ), workDir );
    }

    @Test
    void testSystemPropertyExpansion() {
        try {
            System.setProperty( "FOO", "BAR" );
            System.setProperty( "TEST", "VAL" );
            final Properties p = new Properties();
            p.put( "jspwiki.fileSystemProvider.pageDir", "${FOO}/www/" );
            p.put( "jspwiki.fileSystemProvider.workDir", "${FOO}/www/${TEST}" );
            p.put( "jspwiki.fileSystemProvider.badVal1", "${FOO/www/${TEST}" );
            p.put( "jspwiki.fileSystemProvider.badVal2", "}${FOO/www/${TEST}" );
            p.put( "jspwiki.fileSystemProvider.badVal3", "${NONEXISTANTPROP}" );
            p.put( "jspwiki.fileSystemProvider.badVal4", "${NONEXISTANTPROP}/${FOO}" );
            PropertyReader.propertyExpansion( p );
            Assertions.assertEquals( "BAR/www/", p.getProperty( "jspwiki.fileSystemProvider.pageDir" ) );
            Assertions.assertEquals( "BAR/www/VAL", p.getProperty( "jspwiki.fileSystemProvider.workDir" ) );
            Assertions.assertEquals( "${FOO/www/${TEST}", p.getProperty( "jspwiki.fileSystemProvider.badVal1" ) );
            Assertions.assertEquals( "}${FOO/www/${TEST}", p.getProperty( "jspwiki.fileSystemProvider.badVal2" ) );
            Assertions.assertEquals( "${NONEXISTANTPROP}", p.getProperty( "jspwiki.fileSystemProvider.badVal3" ) );
            Assertions.assertEquals( "${NONEXISTANTPROP}/${FOO}", p.getProperty( "jspwiki.fileSystemProvider.badVal4" ) );
        } finally {
            System.setProperty( "FOO", "" );
            System.setProperty( "TEST", "" );
        }
    }

}
