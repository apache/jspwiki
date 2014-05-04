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
