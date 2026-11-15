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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

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

    @Test
    public void testPlainAsciiFileNameIsWrittenAsItIs() {
        Assertions.assertEquals( "report.txt\";", AttachmentServlet.encodeFileName( "report.txt" ) );
        Assertions.assertEquals( "my file (1).txt\";", AttachmentServlet.encodeFileName( "my file (1).txt" ) );
        Assertions.assertEquals( "it's 100%.txt\";", AttachmentServlet.encodeFileName( "it's 100%.txt" ) );
    }

    @Test
    public void testLatin1FileNameGetsAnAsciiFallbackAndAnEncodedName() {
        Assertions.assertEquals( "T_st.txt\"; filename*=UTF-8''T%C3%A4st.txt;", AttachmentServlet.encodeFileName( "T\u00e4st.txt" ) );
    }

    @Test
    public void testNonLatin1FileNameGetsAnAsciiFallbackAndAnEncodedName() {
        Assertions.assertEquals( "__.txt\"; filename*=UTF-8''%E7%B6%AD%E5%9F%BA.txt;", AttachmentServlet.encodeFileName( "\u7dad\u57fa.txt" ) );
    }

    @Test
    public void testQuoteAndBackslashCannotEndTheQuotedString() {
        Assertions.assertEquals( "a_b.txt\"; filename*=UTF-8''a%22b.txt;", AttachmentServlet.encodeFileName( "a\"b.txt" ) );
        Assertions.assertEquals( "a_b.txt\"; filename*=UTF-8''a%5Cb.txt;", AttachmentServlet.encodeFileName( "a\\b.txt" ) );
    }

    @Test
    public void testSpecialCharactersAreEncodedInTheExtendedName() {
        Assertions.assertEquals( "_ 100%'*+.txt\"; filename*=UTF-8''%C3%9C%20100%25%27%2A+.txt;",
                                 AttachmentServlet.encodeFileName( "\u00dc 100%'*+.txt" ) );
    }

    @Test
    public void testControlCharactersAreNotWrittenInTheFallback() {
        final String header = AttachmentServlet.encodeFileName( "a\r\nb.txt" );
        Assertions.assertEquals( "a__b.txt\"; filename*=UTF-8''a%0D%0Ab.txt;", header );
    }

    @Test
    public void testHeaderOfANonAsciiNameHasOnlyAsciiCharacters() {
        for( final String name : new String[] { "T\u00e4st.txt", "Gr\u00f6\u00dfe \u00dcbung.pdf", "\u7dad\u57fa.txt", "\ud83d\ude00.png" } ) {
            final String header = AttachmentServlet.encodeFileName( name );
            for( int i = 0; i < header.length(); i++ ) {
                Assertions.assertTrue( header.charAt( i ) >= 0x20 && header.charAt( i ) <= 0x7e, header );
            }
        }
    }

    @Test
    public void testEncodedNameDecodesToTheOriginal() throws Exception {
        for( final String name : new String[] { "T\u00e4st.txt", "Gr\u00f6\u00dfe \u00dcbung.pdf", "\u7dad\u57fa.txt", "\ud83d\ude00.png", "a\"b'c%d.txt" } ) {
            final String header = AttachmentServlet.encodeFileName( name );
            final String marker = "filename*=UTF-8''";
            final String value = header.substring( header.indexOf( marker ) + marker.length(), header.length() - 1 );
            Assertions.assertEquals( name, URLDecoder.decode( value, StandardCharsets.UTF_8 ), header );
        }
    }

}
