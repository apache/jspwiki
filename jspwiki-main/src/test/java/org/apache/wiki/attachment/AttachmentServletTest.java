/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

    @Test
    public void testActiveContentTypesAreNotInlineRenderSafe() {
        Assertions.assertFalse( AttachmentServlet.isInlineRenderSafe( "text/html" ) );
        Assertions.assertFalse( AttachmentServlet.isInlineRenderSafe( "application/xhtml+xml" ) );
        Assertions.assertFalse( AttachmentServlet.isInlineRenderSafe( "image/svg+xml" ) );
        Assertions.assertFalse( AttachmentServlet.isInlineRenderSafe( "application/xml" ) );
        Assertions.assertFalse( AttachmentServlet.isInlineRenderSafe( "text/xml" ) );
        Assertions.assertFalse( AttachmentServlet.isInlineRenderSafe( "application/pdf" ) );
        Assertions.assertFalse( AttachmentServlet.isInlineRenderSafe( "application/javascript" ) );
        Assertions.assertFalse( AttachmentServlet.isInlineRenderSafe( "application/binary" ) );
        Assertions.assertFalse( AttachmentServlet.isInlineRenderSafe( null ) );
    }

    @Test
    public void testPassiveContentTypesAreInlineRenderSafe() {
        Assertions.assertTrue( AttachmentServlet.isInlineRenderSafe( "image/png" ) );
        Assertions.assertTrue( AttachmentServlet.isInlineRenderSafe( "image/jpeg" ) );
        Assertions.assertTrue( AttachmentServlet.isInlineRenderSafe( "image/gif" ) );
        Assertions.assertTrue( AttachmentServlet.isInlineRenderSafe( "text/plain" ) );
        Assertions.assertTrue( AttachmentServlet.isInlineRenderSafe( "audio/mpeg" ) );
        Assertions.assertTrue( AttachmentServlet.isInlineRenderSafe( "video/mp4" ) );
        Assertions.assertTrue( AttachmentServlet.isInlineRenderSafe( "IMAGE/PNG" ) );
    }

}
