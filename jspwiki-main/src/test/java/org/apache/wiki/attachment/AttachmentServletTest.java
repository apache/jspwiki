/*
 * Copyright 2026 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wiki.attachment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class AttachmentServletTest {
    

    private static final String BASE_URL = "http://localhost:8080/wiki";


    /**
     * Regression tests for the {@code nextpage} redirect validation (JSPWIKI-46 hardening): scheme-relative and
     * scheme-colon URLs must not escape the same-origin check.
     */
    @Test
    public void testRelativeUrlsAreAllowed() {
        Assertions.assertTrue( AttachmentServlet.isSameOrigin( "/wiki/Wiki.jsp?page=Main", BASE_URL ) );
        Assertions.assertTrue( AttachmentServlet.isSameOrigin( "Wiki.jsp?page=Main", BASE_URL ) );
        Assertions.assertTrue( AttachmentServlet.isSameOrigin( "Wiki.jsp?page=Main:Sub", BASE_URL ) );
    }

    @Test
    public void testSameOriginAbsoluteUrlIsAllowed() {
        Assertions.assertTrue( AttachmentServlet.isSameOrigin( "http://localhost:8080/wiki/Upload.jsp?page=Main", BASE_URL ) );
        Assertions.assertTrue( AttachmentServlet.isSameOrigin( "http://localhost:8080/wiki", BASE_URL ) );
    }

    @Test
    public void testForeignAbsoluteUrlIsRejected() {
        Assertions.assertFalse( AttachmentServlet.isSameOrigin( "http://evil.example/", BASE_URL ) );
        Assertions.assertFalse( AttachmentServlet.isSameOrigin( "https://evil.example/", BASE_URL ) );
        Assertions.assertFalse( AttachmentServlet.isSameOrigin( "https:evil.example", BASE_URL ) );
        Assertions.assertFalse( AttachmentServlet.isSameOrigin( "javascript:alert(1)", BASE_URL ) );
    }

    @Test
    public void testSchemeRelativeUrlIsRejected() {
        Assertions.assertFalse( AttachmentServlet.isSameOrigin( "//evil.example", BASE_URL ) );
        Assertions.assertFalse( AttachmentServlet.isSameOrigin( "/\\evil.example", BASE_URL ) );
        Assertions.assertFalse( AttachmentServlet.isSameOrigin( "\\/evil.example", BASE_URL ) );
        Assertions.assertFalse( AttachmentServlet.isSameOrigin( "/\t/evil.example", BASE_URL ) );
        Assertions.assertFalse( AttachmentServlet.isSameOrigin( " //evil.example", BASE_URL ) );
    }

    @Test
    public void testPrefixSpoofIsRejected() {
        Assertions.assertFalse( AttachmentServlet.isSameOrigin( "http://localhost:8080/wiki.evil.example/", BASE_URL ) );
    }

}