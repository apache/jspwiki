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
package com.ecyrd.jspwiki.ui.migrator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.Locale;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.ui.migrator.BundleMigrator.Bundle;
import com.ecyrd.jspwiki.util.CommentedProperties;

public class BundleMigratorTest extends TestCase
{
    private static final String[] LOCALES = { "", "en", "de", "zh_CN" };
    public static Test suite()
    {
        return new TestSuite( BundleMigratorTest.class );
    }
    private String m_source = null;

    private String m_dest = null;
    
    public void setUp() throws Exception
    {
        // Create dummy property files
        String tmpdir = System.getProperty( "java.io.tmpdir" );
        File tmp = new File( tmpdir );
        for( String locale : LOCALES )
        {
            // Create sample "to" and "from" property files
            Properties sourceProps = new CommentedProperties();
            sourceProps.put( "source.name", "Full name_" + locale );
            sourceProps.put( "source.email", "E-mail_" + locale );
            sourceProps.put( "source.password", "Password_" + locale );
            File sourcePropfile = new File( tmp, ( locale.length() == 0 ? "source" : "source_" + locale ) + ".properties" );
            sourceProps.store( new FileOutputStream( sourcePropfile), null );
            m_source = tmpdir + "/source";
            
            Properties destProps = new CommentedProperties();
            destProps.put( "dest.timeZone", "Time zone_" + locale );
            destProps.put( "dest.orientation", "Orientation_" + locale );
            File destPropfile = new File( tmp, ( locale.length() == 0 ? "dest" : "dest_" + locale ) + ".properties" );
            destProps.store( new FileOutputStream( destPropfile), null );
            m_dest = tmpdir + "/dest";
        }
    }

    public void tearDown() throws Exception
    {
        // Delete dummy property files
        File tmp = new File( System.getProperty( "java.io.tmpdir" ) );
        for( String locale : LOCALES )
        {
            // Delete sample "to" and "from" property files
            File sourcePropfile = new File( tmp, ( locale.length() == 0 ? "source" : "source_" + locale ) + ".properties" );
            //sourcePropfile.delete();
            
            File destPropfile = new File( tmp, ( locale.length() == 0 ? "dest" : "dest_" + locale ) + ".properties" );
            //destPropfile.delete();
        }
    }
    
    public void testCopyKey() throws Exception
    {
        Bundle source = new Bundle( m_source );
        BundleMigrator m = new BundleMigrator();
        m.setBundle( source );
        
        // Move a key to target
        Bundle target = new Bundle( m_dest );
        m.copy( "source.name", target );
        
        // Verify the contents were copied to target
        source = new Bundle( m_dest );
        source.load();
        Properties p;

        p = source.getProperties( null );
        assertEquals( 3, p.size() );
        assertEquals( "Full name_", p.get( "source.name" ) );

        p = source.getProperties( new Locale( "en" ) );
        assertEquals( 3, p.size() );
        assertEquals( "Full name_en", p.get( "source.name" ) );
        
        p = source.getProperties( new Locale( "de" ) );
        assertEquals( 3, p.size() );
        assertEquals( "Full name_de", p.get( "source.name" ) );
        
        p = source.getProperties( new Locale( "zh", "CN" ) );
        assertEquals( 3, p.size() );
        assertEquals( "Full name_zh_CN", p.get( "source.name" ) );
    }

    public void testMoveKey() throws Exception
    {
        Bundle source = new Bundle( m_source );
        BundleMigrator m = new BundleMigrator();
        m.setBundle( source );
        
        // Move a key to target
        Bundle target = new Bundle( m_dest );
        m.move( "source.name", target );
        
        // Verify the key was deleted from source
        source = new Bundle( m_source );
        source.load();
        Properties p;

        p = source.getProperties( null );
        assertEquals( 2, p.size() );
        assertEquals( null, p.get( "source.name" ) );

        p = source.getProperties( new Locale( "en" ) );
        assertEquals( 2, p.size() );
        assertEquals( null, p.get( "source.name" ) );
        
        p = source.getProperties( new Locale( "de" ) );
        assertEquals( 2, p.size() );
        assertEquals( null, p.get( "source.name" ) );
        
        p = source.getProperties( new Locale( "zh", "CN" ) );
        assertEquals( 2, p.size() );
        assertEquals( null, p.get( "source.name" ) );
        
        // Verify the key was copied to target
        target = new Bundle( m_dest );
        target.load();
        p = target.getProperties( null );
        assertEquals( 3, p.size() );
        assertEquals( "Full name_", p.get( "source.name" ) );

        p = target.getProperties( new Locale( "en" ) );
        assertEquals( 3, p.size() );
        assertEquals( "Full name_en", p.get( "source.name" ) );
        
        p = target.getProperties( new Locale( "de" ) );
        assertEquals( 3, p.size() );
        assertEquals( "Full name_de", p.get( "source.name" ) );
        
        p = target.getProperties( new Locale( "zh", "CN" ) );
        assertEquals( 3, p.size() );
        assertEquals( "Full name_zh_CN", p.get( "source.name" ) );
    }
    
    public void testLoadBundle() throws Exception
    {
        Bundle b = new Bundle( m_source );
        // Check that there isn't anything in there yet
        Properties p;
        
        p = b.getProperties( null );
        assertEquals( 0, p.size() );
        p = b.getProperties( new Locale( "en" ) );
        assertEquals( 0, p.size() );
        p = b.getProperties( new Locale( "de" ) );
        assertEquals( 0, p.size() );
        p = b.getProperties( new Locale( "zh", "CN" ) );
        assertEquals( 0, p.size() );
        
        // Load the files
        b.load();
        
        // Verify the contents were loaded
        p = b.getProperties( null );
        assertEquals( 3, p.size() );
        assertEquals( "Full name_", p.get( "source.name" ) );

        p = b.getProperties( new Locale( "en" ) );
        assertEquals( 3, p.size() );
        assertEquals( "Full name_en", p.get( "source.name" ) );
        
        p = b.getProperties( new Locale( "de" ) );
        assertEquals( 3, p.size() );
        assertEquals( "Full name_de", p.get( "source.name" ) );
        
        p = b.getProperties( new Locale( "zh", "CN" ) );
        assertEquals( 3, p.size() );
        assertEquals( "Full name_zh_CN", p.get( "source.name" ) );
    }

    public void testNewBundle() throws Exception
    {
        // Source: verify the base file and all other files
        Bundle bundle = new Bundle( m_source );
        assertEquals( new File( m_source + ".properties" ), bundle.getFile( null ) );
        Collection<Locale> map = bundle.getLocales();
        assertEquals( 3, map.size() );
        assertNotNull( bundle.getFile( new Locale( "en" ) ) );
        assertNotNull( bundle.getFile( new Locale( "de" ) ) );
        assertNotNull( bundle.getFile( new Locale( "zh", "CN" ) ) );
        
        // Dest: verify the base file and all other files
        bundle = new Bundle( m_dest );
        assertEquals( new File( m_dest + ".properties" ), bundle.getFile( null ) );
        assertEquals( 3, map.size() );
        assertNotNull( bundle.getFile( new Locale( "en" ) ) );
        assertNotNull( bundle.getFile( new Locale( "de" ) ) );
        assertNotNull( bundle.getFile( new Locale( "zh", "CN" ) ) );
    }
    
    public void testRemoveKey() throws Exception
    {
        Bundle b = new Bundle( m_source );
        BundleMigrator m = new BundleMigrator();
        m.setBundle( b );
        
        // Remove a key, then save
        m.remove( "source.name" );
        
        // Reload & verify key was deleted
        b = new Bundle( m_source );
        b.load();
        
        // Verify the contents were loaded
        Properties p;
        p = b.getProperties( null );
        assertEquals( 2, p.size() );

        p = b.getProperties( new Locale( "en" ) );
        assertEquals( 2, p.size() );
        
        p = b.getProperties( new Locale( "de" ) );
        assertEquals( 2, p.size() );
        
        p = b.getProperties( new Locale( "zh", "CN" ) );
        assertEquals( 2, p.size() );
    }
    
    public void testRenameKey() throws Exception
    {
        Bundle b = new Bundle( m_source );
        BundleMigrator m = new BundleMigrator();
        m.setBundle( b );
        
        // Rename a key, then save
        m.rename( "source.name", "source.rename" );
        
        // Reload & verify key was deleted
        b = new Bundle( m_source );
        b.load();
        
        // Verify the contents were loaded
        Properties p;
        p = b.getProperties( null );
        assertEquals( 3, p.size() );
        assertEquals( "Full name_", p.get( "source.rename" ) );

        p = b.getProperties( new Locale( "en" ) );
        assertEquals( 3, p.size() );
        assertEquals( "Full name_en", p.get( "source.rename" ) );
        
        p = b.getProperties( new Locale( "de" ) );
        assertEquals( 3, p.size() );
        assertEquals( "Full name_de", p.get( "source.rename" ) );
        
        p = b.getProperties( new Locale( "zh", "CN" ) );
        assertEquals( 3, p.size() );
        assertEquals( "Full name_zh_CN", p.get( "source.rename" ) );
    }

    public void testSaveBundle() throws Exception
    {
        Bundle b = new Bundle( m_source );
        b.load();
        
        // Modify the properties by adding a new key
        Properties p;
        p = b.getProperties( null );
        p.put( "new.key", "Value_" );
        p = b.getProperties( new Locale( "en" ) );
        p.put( "new.key", "Value_en" );
        p = b.getProperties( new Locale( "de" ) );
        p.put( "new.key", "Value_de" );
        p = b.getProperties( new Locale( "zh", "CN" ) );
        p.put( "new.key", "Value_zh_CN" );
        
        // Save 'em
        b.save();
        
        // Reload & verify contents were saved
        b = new Bundle( m_source );
        b.load();
        
        // Verify the contents were loaded
        p = b.getProperties( null );
        assertEquals( 4, p.size() );
        assertEquals( "Value_", p.get( "new.key" ) );

        p = b.getProperties( new Locale( "en" ) );
        assertEquals( 4, p.size() );
        assertEquals( "Value_en", p.get( "new.key" ) );
        
        p = b.getProperties( new Locale( "de" ) );
        assertEquals( 4, p.size() );
        assertEquals( "Value_de", p.get( "new.key" ) );
        
        p = b.getProperties( new Locale( "zh", "CN" ) );
        assertEquals( 4, p.size() );
        assertEquals( "Value_zh_CN", p.get( "new.key" ) );
    }

    public void testSetSource() throws Exception
    {
        BundleMigrator m = new BundleMigrator();

        // Try with a bundle base file that obviously does not exist
        try
        {
            m.setBundle( new Bundle( "src/Default" ) );
        }
        catch( FileNotFoundException e )
        {
            // Good! This is what we expect
        }

        // Try with one that does exist
        m.setBundle( new Bundle( "etc/i18n/CoreResources" ) );
    }

}
