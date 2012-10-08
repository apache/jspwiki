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
package org.apache.wiki;

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
            rel = Release.VERSION+"."+Release.REVISION+"."+(Release.MINORREVISION-1)+"-cvs";
        else
            rel = Release.VERSION+"."+(Release.REVISION-1)+".9999"+"-cvs";            
        
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
        else
            rel = Release.VERSION+"."+(Release.REVISION-1)+".9999"+"-cvs";   
        
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
