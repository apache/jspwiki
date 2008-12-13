package com.ecyrd.jspwiki.content;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Properties;

import junit.framework.TestCase;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.providers.ProviderException;

public class ExporterTest extends TestCase
{
    private TestEngine m_engine;
    
    protected void setUp() throws Exception
    {
        super.setUp();
        
        Properties props = new Properties();
        
        props.load( TestEngine.findTestProperties() );

        props.setProperty( WikiEngine.PROP_MATCHPLURALS, "true" );
        
        TestEngine.emptyWorkDir();
        m_engine = new TestEngine(props);  
    }

    protected void tearDown() throws ProviderException
    {
        m_engine.deletePage("FooBar");
    }
    
    public void testExport1() throws Exception
    {
        m_engine.saveText( "FooBar", "test" );

        m_engine.addAttachment( "FooBar", "test.jpg", "1234567890".getBytes() );
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        Exporter x = new Exporter(m_engine,out);
        
        x.export();
        
        String res = out.toString( "UTF-8" );
        
        System.out.println("Result is");
        System.out.println(res);
    }
}
