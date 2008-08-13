package com.ecyrd.jspwiki.action;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;

public class GroupActionBeanTest extends TestCase
{
    TestEngine m_engine;

    public void setUp()
    {
        // Start the WikiEngine, and stash reference
        Properties props = new Properties();
        try
        {
            props.load( TestEngine.findTestProperties() );
            m_engine = new TestEngine( props );
        }
        catch( Exception e )
        {
            throw new RuntimeException( "Could not set up TestEngine: " + e.getMessage() );
        }
    }

    public static Test suite()
    {
        return new TestSuite( GroupActionBeanTest.class );
    }
}
