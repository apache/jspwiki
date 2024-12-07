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

import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


class WikiServletTest {
    
    @Test
    void testDoGet() throws Exception {
        final HttpServletRequest req = HttpMockFactory.createHttpRequest( "/JSPWiki", "/wiki/Wiki.jsp" );
        final HttpServletResponse res = HttpMockFactory.createHttpResponse();
        final WikiServlet wikiServlet = new WikiServlet();
        final ServletConfig config = HttpMockFactory.createServletConfig( "/JSPWiki" );

        wikiServlet.init( config );
        wikiServlet.doGet( req, res );
        wikiServlet.destroy();

        Mockito.verify( req ).getRequestDispatcher( "/Wiki.jsp?page=Main&null" );
    }
    
    @Test
    void testNastyDoPost() throws Exception {
        final HttpServletRequest req = HttpMockFactory.createHttpRequest( "/JSPWiki", "/wiki/Edit.jsp" );
        final HttpServletResponse res = HttpMockFactory.createHttpResponse();
        final WikiServlet wikiServlet = new WikiServlet();
        final ServletConfig config = HttpMockFactory.createServletConfig( "/JSPWiki" );
        
        wikiServlet.init( config );
        wikiServlet.doPost( req, res );
        wikiServlet.destroy();

        Mockito.verify( req ).getRequestDispatcher( "/Wiki.jsp?page=Main&null" );
    }

}
