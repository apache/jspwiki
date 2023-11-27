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
/*
 * (C) Janne Jalkanen 2005
 *
 */

package org.apache.wiki.ui;

import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockServletConfig;
import org.apache.wiki.TestEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class InstallerTest {

    private Installer installer;


    @BeforeEach
    public void setUp() throws ServletException {


        final IntallerMockHttpServletRequest req = new IntallerMockHttpServletRequest( "/JSPWiki", "/wiki/Wiki.jsp" );


        req.setParameter( "jspwiki.applicationName", "<script>alert('xss');</script>" );
        req.setParameter( "jspwiki.workDir", "C:\\Users\\Example\\Path" );
        req.setParameter( "jspwiki.fileSystemProvider.pageDir", "<script>alert2('xss');</script>" );


        final MockServletConfig config = new MockServletConfig();
        config.setServletContext( TestEngine.createServletContext( "/JSPWiki" ) );


        installer = new Installer( req, config );
    }

    @Test
    public void testSanitizeInput() {
        // Assuming there's a method in Installer that processes the request and sets properties
        installer.parseProperties();

        // Verify the property is sanitized
        String sanitizedValue = installer.getProperty( "jspwiki.applicationName" );
        assertEquals( "&lt;script&gt;alert(&#x27;xss&#x27;);&lt;&#x2F;script&gt;", sanitizedValue );
    }

    @Test
    public void testSanitizePath() {
        // Assuming there's a method in Installer that processes the request and sets properties
        installer.parseProperties();

        // Verify the path is normalized
        String normalizedPath = installer.getProperty( "jspwiki.workDir" );
        assertEquals( "C:/Users/Example/Path", normalizedPath ); // Adjust expected value based on normalization behavior
    }

    @Test
    public void testSanitizePageDir() {
        // Assuming there's a method in Installer that processes the request and sets properties
        installer.parseProperties();

        // Verify the path is normalized
        String sanitizedValue = installer.getProperty( "jspwiki.fileSystemProvider.pageDir" );
        assertEquals( "&lt;script&gt;alert2(&#x27;xss&#x27;);&lt;&#x2F;script&gt;", sanitizedValue );
    }


    static class IntallerMockHttpServletRequest extends MockHttpServletRequest {
        private final Map< String, String > parameters = new HashMap<>();

        public IntallerMockHttpServletRequest( String contextPath, String servletPath ) {
            super( contextPath, servletPath );
        }

        @Override
        public String getParameter( String name ) {
            return parameters.get( name );
        }

        public void setParameter( String name, String value ) {
            parameters.put( name, value );
        }
    }

}

