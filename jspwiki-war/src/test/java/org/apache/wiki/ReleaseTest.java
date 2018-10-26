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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class ReleaseTest
{
    @Test
    public void testNewer1()
    {
        Assertions.assertTrue( Release.isNewerOrEqual("1.0.100") );
    }

    @Test
    public void testNewer2()
    {
        Assertions.assertTrue( Release.isNewerOrEqual("2.0.0-alpha") );
    }

    @Test
    public void testNewer3()
    {
        Assertions.assertFalse( Release.isNewerOrEqual("10.0.0") );
    }

    @Test
    public void testNewer4()
    {
        Assertions.assertTrue( Release.isNewerOrEqual(Release.VERSTR) );
    }

    @Test
    public void testNewer5()
    {
        String rel = Release.VERSION+"."+Release.REVISION+"."+(Release.MINORREVISION+1)+"-cvs";

        Assertions.assertFalse( Release.isNewerOrEqual(rel) );
    }

    @Test
    public void testNewer6()
    {
        String rel = null;

        if( Release.MINORREVISION != 0 )
            rel = Release.VERSION+"."+Release.REVISION+"."+(Release.MINORREVISION-1)+"-cvs";
        else
            rel = Release.VERSION+"."+(Release.REVISION-1)+".9999"+"-cvs";

        Assertions.assertTrue( Release.isNewerOrEqual(rel) );
    }

    @Test
    public void testNewer7()
    {
        String rel = Release.VERSION+"."+Release.REVISION;

        Assertions.assertTrue( Release.isNewerOrEqual(rel) );
    }

    @Test
    public void testNewer8()
    {
        String rel = Release.VERSION+"";

        Assertions.assertTrue( Release.isNewerOrEqual(rel) );
    }

    @Test
    public void testOlder1()
    {
        Assertions.assertFalse( Release.isOlderOrEqual("1.0.100") );
    }

    @Test
    public void testOlder2()
    {
        Assertions.assertFalse( Release.isOlderOrEqual("2.0.0-alpha") );
    }

    @Test
    public void testOlder3()
    {
        Assertions.assertTrue( Release.isOlderOrEqual("10.0.0") );
    }

    @Test
    public void testOlder4()
    {
        Assertions.assertTrue( Release.isOlderOrEqual(Release.VERSTR) );
    }

    @Test
    public void testOlder5()
    {
        String rel = Release.VERSION+"."+Release.REVISION+"."+(Release.MINORREVISION+1)+"-cvs";

        Assertions.assertTrue( Release.isOlderOrEqual(rel) );
    }

    @Test
    public void testOlder6()
    {
        String rel;

        if( Release.MINORREVISION != 0 )
            rel = Release.VERSION+"."+Release.REVISION+"."+(Release.MINORREVISION-1)+"-cvs";
        else
            rel = Release.VERSION+"."+(Release.REVISION-1)+".9999"+"-cvs";

        Assertions.assertFalse( Release.isOlderOrEqual(rel) );
    }

    @Test
    public void testOlder7()
    {
        String rel = Release.VERSION+"."+Release.REVISION;

        Assertions.assertTrue( Release.isOlderOrEqual(rel) );
    }

    @Test
    public void testOlder8()
    {
        String rel = Release.VERSION+"";

        Assertions.assertTrue( Release.isOlderOrEqual(rel) );
    }

    @Test
    public void testOlder9()
    {
        String rel = "";

        Assertions.assertTrue( Release.isOlderOrEqual(rel) );
    }

}
