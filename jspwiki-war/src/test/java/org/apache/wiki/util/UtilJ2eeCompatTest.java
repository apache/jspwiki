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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.wiki.util.UtilJ2eeCompat;

public class UtilJ2eeCompatTest extends TestCase
{

    public void setUp() 
    {
        UtilJ2eeCompat.useOutputStreamValue = null;
    }
    
    public void testOracle()
    {
        assertTrue( UtilJ2eeCompat.useOutputStream( "Oracle Containers for J2EE 10g(10.1.3.1.0 )", true ) );
        // Do not reinitialize
        assertTrue( UtilJ2eeCompat.useOutputStream( "Apache Tomcat/7.0.39" ) );
        // Do not reinitialize
        assertTrue( UtilJ2eeCompat.useOutputStream( "Sun Java System Application Server 9.1_02" ) );
    }

    public void testGlassfish3()
    {
        assertFalse(UtilJ2eeCompat.useOutputStream("Oracle GlassFish Server 3.1.2", true));
        // Do not reinitialize
        assertFalse(UtilJ2eeCompat.useOutputStream("Oracle GlassFish Server 3.1.2"));
    }

    public void testGlassfish2() {
        assertFalse(UtilJ2eeCompat.useOutputStream("GlassFish Server Open Source Edition  4.1", true));
        // Do not reinitialize
        assertFalse(UtilJ2eeCompat.useOutputStream("GlassFish Server Open Source Edition  4.1"));
    }

    public void testGlassfish1() {
        assertFalse(UtilJ2eeCompat.useOutputStream("Sun Java System Application Server 9.1_02", true));
        // Do not reinitialize
        assertFalse(UtilJ2eeCompat.useOutputStream("Sun Java System Application Server 9.1_02"));
    }

    public void testTomcat()
    {
        assertTrue( UtilJ2eeCompat.useOutputStream( "Apache Tomcat/7.0.39", true ) );
        // Reinitialize
        assertTrue( UtilJ2eeCompat.useOutputStream( "Apache Tomcat/7.0.39", true ) );
    }

    public void testWebSphere()
    {
        assertFalse( UtilJ2eeCompat.useOutputStream( "IBM WebSphere Application Server/8.5", true ) );
        // Reinitialize
        assertFalse( UtilJ2eeCompat.useOutputStream( "IBM WebSphere Application Server/8.5", true ) );
    }

    public static Test suite()
    {
        return new TestSuite( UtilJ2eeCompatTest.class );
    }
}
