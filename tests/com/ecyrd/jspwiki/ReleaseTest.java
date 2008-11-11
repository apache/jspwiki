package com.ecyrd.jspwiki;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ReleaseTest extends TestCase
{
    public void testNewer1()
    {
        assertTrue( Release.isNewerOrEqual("1.0.100") );
    }

    public void testNewer2()
    {
        assertTrue( Release.isNewerOrEqual("2.0.0-alpha") );
    }
    
    public void testNewer3()
    {
        assertFalse( Release.isNewerOrEqual("10.0.0") );
    }
    
    public void testNewer4()
    {
        assertTrue( Release.isNewerOrEqual(Release.VERSTR) );
    }
    
    public void testNewer5()
    {
        String rel = Release.VERSION+"."+Release.REVISION+"."+(Release.MINORREVISION+1)+"-cvs";
        
        assertFalse( Release.isNewerOrEqual(rel) );
    }

    public void testNewer6()
    {
        String rel = null;
        
        if( Release.MINORREVISION != 0 )
        {
            rel = Release.VERSION+"."+Release.REVISION+"."+(Release.MINORREVISION-1)+"-svn";
        }
        else if( Release.REVISION != 0 )
        {
            rel = Release.VERSION+"."+(Release.REVISION-1)+".9999"+"-svn";            
        }
        else
        {
            rel = (Release.VERSION-1)+".9999.9999-svn";
        }
        
        assertTrue( Release.isNewerOrEqual(rel) );
    }

    public void testNewer7()
    {
        String rel = Release.VERSION+"."+Release.REVISION;
        
        assertTrue( Release.isNewerOrEqual(rel) );
    }

    public void testNewer8()
    {
        String rel = Release.VERSION+"";
        
        assertTrue( Release.isNewerOrEqual(rel) );
    }

    public void testOlder1()
    {
        assertFalse( Release.isOlderOrEqual("1.0.100") );
    }

    public void testOlder2()
    {
        assertFalse( Release.isOlderOrEqual("2.0.0-alpha") );
    }
    
    public void testOlder3()
    {
        assertTrue( Release.isOlderOrEqual("10.0.0") );
    }
    
    public void testOlder4()
    {
        assertTrue( Release.isOlderOrEqual(Release.VERSTR) );
    }
    
    public void testOlder5()
    {
        String rel = Release.VERSION+"."+Release.REVISION+"."+(Release.MINORREVISION+1)+"-cvs";
        
        assertTrue( Release.isOlderOrEqual(rel) );
    }

    public void testOlder6()
    {
        String rel;
        
        if( Release.MINORREVISION != 0 )
            rel = Release.VERSION+"."+Release.REVISION+"."+(Release.MINORREVISION-1)+"-cvs";
        else if( Release.REVISION != 0 )
            rel = Release.VERSION+"."+(Release.REVISION-1)+".9999"+"-cvs";   
        else
            rel = (Release.VERSION-1)+".9999.9999-svn";
        
        assertFalse( Release.isOlderOrEqual(rel) );
    }

    public void testOlder7()
    {
        String rel = Release.VERSION+"."+Release.REVISION;
        
        assertTrue( Release.isOlderOrEqual(rel) );
    }

    public void testOlder8()
    {
        String rel = Release.VERSION+"";
        
        assertTrue( Release.isOlderOrEqual(rel) );
    }

    public void testOlder9()
    {
        String rel = "";
        
        assertTrue( Release.isOlderOrEqual(rel) );
    }

    public static Test suite()
    {
        return new TestSuite( ReleaseTest.class );
    }
}
