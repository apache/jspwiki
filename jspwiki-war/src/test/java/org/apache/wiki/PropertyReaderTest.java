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

import java.util.Properties;

import org.apache.wiki.util.PropertyReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PropertyReaderTest
{
    @Test
    public void testVariableExpansion()
    {
        Properties p = new Properties();

        p.put("var.basedir", "/p/mywiki");

        p.put("jspwiki.fileSystemProvider.pageDir", "$basedir/www/");
        p.put("jspwiki.basicAttachmentProvider.storageDir", "$basedir/www/");
        p.put("jspwiki.workDir", "$basedir/wrk/");
        p.put("jspwiki.xyz", "test basedir"); //don't touch this

        PropertyReader.expandVars(p);

        Assertions.assertTrue( p.getProperty("jspwiki.fileSystemProvider.pageDir").equals("/p/mywiki/www/") );
        Assertions.assertTrue( p.getProperty("jspwiki.basicAttachmentProvider.storageDir").equals("/p/mywiki/www/") );
        Assertions.assertTrue( p.getProperty("jspwiki.fileSystemProvider.pageDir").equals("/p/mywiki/www/") );
        Assertions.assertTrue( p.getProperty("jspwiki.workDir").endsWith("/p/mywiki/wrk/") );
        Assertions.assertTrue( p.getProperty("jspwiki.xyz").endsWith("test basedir") ); //don't touch this

        Assertions.assertFalse( p.getProperty("jspwiki.workDir").endsWith("$basedir/wrk/") );
    }

    @Test
    public void testVariableExpansion2()
    {
        Properties p = new Properties();

        //this time, declare the var at the end... (should overwrite this one);
        p.put("var.basedir", "xxx");

        p.put("jspwiki.fileSystemProvider.pageDir", "$basedir/www/");
        p.put("jspwiki.basicAttachmentProvider.storageDir", "$basedir/www/");
        p.put("jspwiki.workDir", "$basedir/wrk/");
        p.put("jspwiki.xyz", "test basedir"); //don't touch this
        p.put("jspwiki.abc", "test $x2"); //don't touch this

        p.put("var.basedir", " /p/mywiki"); //note that this var has a space at the beginning...
        p.put("var.x2", " wiki "); //note that this var has a space at the beginning...

        PropertyReader.expandVars(p);

        Assertions.assertTrue( p.getProperty("jspwiki.fileSystemProvider.pageDir").equals("/p/mywiki/www/") );
        Assertions.assertTrue( p.getProperty("jspwiki.basicAttachmentProvider.storageDir").equals("/p/mywiki/www/") );
        Assertions.assertTrue( p.getProperty("jspwiki.fileSystemProvider.pageDir").equals("/p/mywiki/www/") );
        Assertions.assertTrue( p.getProperty("jspwiki.workDir").endsWith("/p/mywiki/wrk/") );
        Assertions.assertTrue( p.getProperty("jspwiki.xyz").endsWith("test basedir") ); //don't touch this

        Assertions.assertFalse( p.getProperty("jspwiki.workDir").endsWith("$basedir/wrk/") );
        Assertions.assertTrue( p.getProperty("jspwiki.abc").endsWith("test wiki") );
    }



    @Test
    public void testMultipleVariableExpansion()
    {
        Properties p = new Properties();

        //this time, declare the var at the end... (should overwrite this one);
        p.put("var.x1", "a");
        p.put("var.x2", "b");

        p.put("jspwiki.x1", "$x1");
        p.put("jspwiki.x2", "$x2");
        p.put("jspwiki.x3", "$x1/$x2");

        PropertyReader.expandVars(p);

        Assertions.assertTrue( p.getProperty("jspwiki.x1").equals("a") );
        Assertions.assertTrue( p.getProperty("jspwiki.x2").equals("b") );
        Assertions.assertTrue( p.getProperty("jspwiki.x3").equals("a/b") );
    }

}