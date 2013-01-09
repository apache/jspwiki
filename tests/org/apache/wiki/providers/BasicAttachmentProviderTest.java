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

package org.apache.wiki.providers;

import junit.framework.*;
import java.io.*;
import java.util.*;

import org.apache.wiki.*;
import org.apache.wiki.attachment.*;

public class BasicAttachmentProviderTest extends TestCase
{
    public static final String NAME1 = "TestPage";
    public static final String NAME2 = "TestPageToo";

    Properties props = new Properties();

    TestEngine m_engine;

    BasicAttachmentProvider m_provider;

    /**
     *  This is the sound of my head hitting the keyboard.
     */
    private static final String c_fileContents = "gy th tgyhgthygyth tgyfgftrfgvtgfgtr";

    public BasicAttachmentProviderTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( TestEngine.findTestProperties() );

        m_engine  = new TestEngine(props);

        TestEngine.deleteAll( new File(TextUtil.getRequiredProperty( props, BasicAttachmentProvider.PROP_STORAGEDIR )) );
        
        m_provider = new BasicAttachmentProvider();
        m_provider.initialize( m_engine, props );

        m_engine.saveText( NAME1, "Foobar" );
        m_engine.saveText( NAME2, "Foobar2" );
    }

    private File makeAttachmentFile()
        throws Exception
    {
        File tmpFile = File.createTempFile("test","txt");
        tmpFile.deleteOnExit();

        FileWriter out = new FileWriter( tmpFile );
        
        FileUtil.copyContents( new StringReader( c_fileContents ), out );

        out.close();
        
        return tmpFile;
    }

    private File makeExtraFile( File directory, String name )
        throws Exception
    {
        File tmpFile = new File( directory, name );
        FileWriter out = new FileWriter( tmpFile );
        
        FileUtil.copyContents( new StringReader( c_fileContents ), out );

        out.close();
        
        return tmpFile;
    }


    public void tearDown()
    {
        m_engine.deleteTestPage( NAME1 );
        m_engine.deleteTestPage( NAME2 );

        String tmpfiles = props.getProperty( BasicAttachmentProvider.PROP_STORAGEDIR );

        File f = new File( tmpfiles, NAME1+BasicAttachmentProvider.DIR_EXTENSION );

        TestEngine.deleteAll( f );

        f = new File( tmpfiles, NAME2+BasicAttachmentProvider.DIR_EXTENSION );

        TestEngine.deleteAll( f );
        
        TestEngine.emptyWorkDir();
    }

    public void testExtension()
    {
        String s = "test.png";

        assertEquals( BasicAttachmentProvider.getFileExtension(s), "png" );
    }

    public void testExtension2()
    {
        String s = ".foo";

        assertEquals( "foo", BasicAttachmentProvider.getFileExtension(s) );
    }

    public void testExtension3()
    {
        String s = "test.png.3";

        assertEquals( "3", BasicAttachmentProvider.getFileExtension(s) );
    }

    public void testExtension4()
    {
        String s = "testpng";

        assertEquals( "bin", BasicAttachmentProvider.getFileExtension(s) );
    }


    public void testExtension5()
    {
        String s = "test.";

        assertEquals( "bin", BasicAttachmentProvider.getFileExtension(s) );
    }

    public void testExtension6()
    {
        String s = "test.a";

        assertEquals( "a", BasicAttachmentProvider.getFileExtension(s) );
    }

    /**
     *  Can we save attachments with names in UTF-8 range?
     */
    public void testPutAttachmentUTF8()
        throws Exception
    {
        File in = makeAttachmentFile();

        Attachment att = new Attachment( m_engine, NAME1, "\u3072\u3048\u308b\u00e5\u00e4\u00f6test.f\u00fc\u00fc" );

        m_provider.putAttachmentData( att, new FileInputStream(in) );

        List res = m_provider.listAllChanged( new Date(0L) );

        Attachment a0 = (Attachment) res.get(0);
        
        assertEquals( "name", att.getName(), a0.getName() );
    }

    public void testListAll()
        throws Exception
    {
        File in = makeAttachmentFile();

        Attachment att = new Attachment( m_engine, NAME1, "test1.txt" );

        m_provider.putAttachmentData( att, new FileInputStream(in) );

        Thread.sleep( 2000L ); // So that we get a bit of granularity.

        Attachment att2 = new Attachment( m_engine, NAME2, "test2.txt" );

        m_provider.putAttachmentData( att2, new FileInputStream(in) );
        
        List res = m_provider.listAllChanged( new Date(0L) );

        assertEquals( "list size", 2, res.size() );

        Attachment a2 = (Attachment) res.get(0);  // Most recently changed
        Attachment a1 = (Attachment) res.get(1);  // Least recently changed

        assertEquals( "a1 name", att.getName(), a1.getName() );
        assertEquals( "a2 name", att2.getName(), a2.getName() );
    }


    /**
     *  Check that the system does not fail if there are extra files in the directory.
     */
    public void testListAllExtrafile()
        throws Exception
    {
        File in = makeAttachmentFile();

        File sDir = new File(m_engine.getWikiProperties().getProperty( BasicAttachmentProvider.PROP_STORAGEDIR ));
        File extrafile = makeExtraFile( sDir, "foobar.blob" );

        try
        {
            Attachment att = new Attachment( m_engine, NAME1, "test1.txt" );

            m_provider.putAttachmentData( att, new FileInputStream(in) );

            Thread.sleep( 2000L ); // So that we get a bit of granularity.

            Attachment att2 = new Attachment( m_engine, NAME2, "test2.txt" );

            m_provider.putAttachmentData( att2, new FileInputStream(in) );
        
            List res = m_provider.listAllChanged( new Date(0L) );

            assertEquals( "list size", 2, res.size() );

            Attachment a2 = (Attachment) res.get(0);  // Most recently changed
            Attachment a1 = (Attachment) res.get(1);  // Least recently changed

            assertEquals( "a1 name", att.getName(), a1.getName() );
            assertEquals( "a2 name", att2.getName(), a2.getName() );
        }
        finally
        {
            extrafile.delete();
        }
    }

    /**
     *  Check that the system does not fail if there are extra files in the
     *  attachment directory.
     */
    public void testListAllExtrafileInAttachmentDir()
        throws Exception
    {
        File in = makeAttachmentFile();

        File sDir = new File(m_engine.getWikiProperties().getProperty( BasicAttachmentProvider.PROP_STORAGEDIR ));
        File attDir = new File( sDir, NAME1+"-att" );


        Attachment att = new Attachment( m_engine, NAME1, "test1.txt" );

        m_provider.putAttachmentData( att, new FileInputStream(in) );
        
        File extrafile = makeExtraFile( attDir, "ping.pong" );
        
        try
        {
            Thread.sleep( 2000L ); // So that we get a bit of granularity.

            Attachment att2 = new Attachment( m_engine, NAME2, "test2.txt" );

            m_provider.putAttachmentData( att2, new FileInputStream(in) );
        
            List res = m_provider.listAllChanged( new Date(0L) );

            assertEquals( "list size", 2, res.size() );

            Attachment a2 = (Attachment) res.get(0);  // Most recently changed
            Attachment a1 = (Attachment) res.get(1);  // Least recently changed

            assertEquals( "a1 name", att.getName(), a1.getName() );
            assertEquals( "a2 name", att2.getName(), a2.getName() );
        }
        finally
        {
            extrafile.delete();
        }
    }

    /**
     *  Check that the system does not fail if there are extra dirs in the
     *  attachment directory.
     */
    public void testListAllExtradirInAttachmentDir()
        throws Exception
    {
        File in = makeAttachmentFile();

        File sDir = new File(m_engine.getWikiProperties().getProperty( BasicAttachmentProvider.PROP_STORAGEDIR ));
        File attDir = new File( sDir, NAME1+"-att" );

        Attachment att = new Attachment( m_engine, NAME1, "test1.txt" );

        m_provider.putAttachmentData( att, new FileInputStream(in) );
        
        // This is our extraneous directory. 
        File extrafile = new File( attDir, "ping.pong" );
        extrafile.mkdir();
        
        try
        {
            Thread.sleep( 2000L ); // So that we get a bit of granularity.

            Attachment att2 = new Attachment( m_engine, NAME2, "test2.txt" );

            m_provider.putAttachmentData( att2, new FileInputStream(in) );
        
            List res = m_provider.listAllChanged( new Date(0L) );

            assertEquals( "list size", 2, res.size() );

            Attachment a2 = (Attachment) res.get(0);  // Most recently changed
            Attachment a1 = (Attachment) res.get(1);  // Least recently changed

            assertEquals( "a1 name", att.getName(), a1.getName() );
            assertEquals( "a2 name", att2.getName(), a2.getName() );
        }
        finally
        {
            extrafile.delete();
        }
    }

    public void testListAllNoExtension()
        throws Exception
    {
        File in = makeAttachmentFile();
        
        Attachment att = new Attachment( m_engine, NAME1, "test1." );

        m_provider.putAttachmentData( att, new FileInputStream(in) );

        Thread.sleep( 2000L ); // So that we get a bit of granularity.

        Attachment att2 = new Attachment( m_engine, NAME2, "test2." );

        m_provider.putAttachmentData( att2, new FileInputStream(in) );
        
        List res = m_provider.listAllChanged( new Date(0L) );

        assertEquals( "list size", 2, res.size() );

        Attachment a2 = (Attachment) res.get(0);  // Most recently changed
        Attachment a1 = (Attachment) res.get(1);  // Least recently changed

        assertEquals( "a1 name", att.getName(), a1.getName() );
        assertEquals( "a2 name", att2.getName(), a2.getName() );        
    }

    public static Test suite()
    {
        return new TestSuite( BasicAttachmentProviderTest.class );
    }


}
