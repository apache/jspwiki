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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * Unit test for PropertyReader.
 */
public class PropertyReaderTest {

    @Test
    public void testLocateClassPathResource() {
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
    public void testVariableExpansion() {
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
    public void testVariableExpansion2() {
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
    public void testMultipleVariableExpansion() {
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
    public void testCollectPropertiesFrom() {
        final Map< String, String > sut = new HashMap<>();
        sut.put( "jspwiki_frontPage", "Main" );
        sut.put( "secretEnv", "asdasd" );

        final Map< String, String > test = PropertyReader.collectPropertiesFrom( sut );

        Assertions.assertEquals( "Main", test.get( "jspwiki.frontPage" ) );
        Assertions.assertNull( test.get( "secretEnv" ) );
    }

}
