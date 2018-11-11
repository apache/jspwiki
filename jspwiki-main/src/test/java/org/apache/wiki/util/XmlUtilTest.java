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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.jdom2.Element;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class XmlUtilTest {

    @Test
    public void testParseFromClasspath() {
        List< Element > elements = XmlUtil.parse( "ini/jspwiki_module.xml", "/modules/plugin" );
        Assertions.assertEquals( 4, elements.size() ); // 2 on src/main/resources, another 2 on src/test/resources

        elements = XmlUtil.parse( "ini/jspwiki_module.xml", "/modules/filter" );
        Assertions.assertEquals( 1, elements.size() );

        elements = XmlUtil.parse( "ini/jspwiki_module.xml", "/modules/editor" );
        Assertions.assertEquals( 2, elements.size() );

        elements = XmlUtil.parse( "ini/jspwiki_module.xml", "/modules/heck" );
        Assertions.assertEquals( 0, elements.size() );

        elements = XmlUtil.parse( "doesnt/exist.this", "/modules/editor" );
        Assertions.assertEquals( 0, elements.size() );

        elements = XmlUtil.parse( ( String )null, "/modules/editor" );
        Assertions.assertEquals( 0, elements.size() );

        elements = XmlUtil.parse( "ini/jspwiki_module.xml", null );
        Assertions.assertEquals( 0, elements.size() );

        elements = XmlUtil.parse( ClassUtil.MAPPINGS, "/classmappings/mapping" );
        Assertions.assertEquals( 19, elements.size() );
    }

    @Test
    public void testParseFromStream() throws FileNotFoundException {
        InputStream is = new FileInputStream( new File ("./src/test/resources/ini/jspwiki_module.xml" ) );
        List< Element > elements = XmlUtil.parse( is, "/modules/plugin" );
        Assertions.assertEquals( 2, elements.size() );

        elements = XmlUtil.parse( is, "/modules/filter" );
        Assertions.assertEquals( 0, elements.size() );

        elements = XmlUtil.parse( is, "/modules/editor" );
        Assertions.assertEquals( 0, elements.size() );

        elements = XmlUtil.parse( is, "/modules/heck" );
        Assertions.assertEquals( 0, elements.size() );

        elements = XmlUtil.parse( ( InputStream )null, "/modules/editor" );
        Assertions.assertEquals( 0, elements.size() );

        elements = XmlUtil.parse( is, null );
        Assertions.assertEquals( 0, elements.size() );

        IOUtils.closeQuietly( is );
    }

}
