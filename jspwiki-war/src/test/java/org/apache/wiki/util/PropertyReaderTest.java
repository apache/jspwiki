/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); fyou may not use this file except in compliance
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

import junit.framework.TestCase;

/**
 * Unit test for PropertyReader.
 */
public class PropertyReaderTest extends TestCase {

    public void testLocateClassPathResource() throws Exception {
        assertEquals("/ini/jspwiki.properties", PropertyReader.createResourceLocation("ini", "jspwiki.properties"));
        assertEquals("/ini/jspwiki.properties", PropertyReader.createResourceLocation(null, "ini/jspwiki.properties"));
        assertEquals("/ini/jspwiki.properties", PropertyReader.createResourceLocation(null, "/ini/jspwiki.properties"));
        assertEquals("/jspwiki-custom.properties", PropertyReader.createResourceLocation(null, "/jspwiki-custom.properties"));
        assertEquals("/jspwiki.custom.cascade.1.ini", PropertyReader.createResourceLocation(null, "jspwiki.custom.cascade.1.ini"));
        assertEquals("/WEB-INF/classes/jspwiki-custom.properties", PropertyReader.createResourceLocation("WEB-INF/classes", PropertyReader.CUSTOM_JSPWIKI_CONFIG));
        assertEquals("/WEB-INF/classes/jspwiki-custom.properties", PropertyReader.createResourceLocation("/WEB-INF/classes", PropertyReader.CUSTOM_JSPWIKI_CONFIG));
        assertEquals("/WEB-INF/classes/jspwiki-custom.properties", PropertyReader.createResourceLocation("/WEB-INF/classes/", PropertyReader.CUSTOM_JSPWIKI_CONFIG));
    }
}
