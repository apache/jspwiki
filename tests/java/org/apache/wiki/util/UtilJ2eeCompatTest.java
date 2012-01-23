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
package org.apache.wiki.util;

import org.apache.wiki.util.UtilJ2eeCompat;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class UtilJ2eeCompatTest extends TestCase
{

    public void testOracle()
    {
        assertTrue( UtilJ2eeCompat.useOutputStream( "Oracle Containers for J2EE 10g(10.1.3.1.0 )", true ) );
        // Do not reinitialize
        assertTrue( UtilJ2eeCompat.useOutputStream( "Apache Tomcat/5.5.20" ) );
    }

    public void testGlassfish()
    {
        assertTrue( UtilJ2eeCompat.useOutputStream( "Sun Java System Application Server 9.1_02" ) );
    }

    public void testTomcat()
    {
        // Reinitialize
        assertFalse( UtilJ2eeCompat.useOutputStream( "Apache Tomcat/5.5.20", true ) );
    }

    public static Test suite()
    {
        return new TestSuite( UtilJ2eeCompatTest.class );
    }
}
