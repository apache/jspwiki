package org.apache.wiki.url;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

class UrlConstructorTest {

    @Test
    void testParsePageFromURLUsingPathInfo() {
        final HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
        Mockito.doReturn( "MyPage" ).when( req ).getPathInfo();
        Assertions.assertEquals( "MyPage", URLConstructor.parsePageFromURL( req, StandardCharsets.UTF_8 ) );
    }

    @Test
    void testParsePageFromURLUsingRequestParam() {
        final HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
        Mockito.doReturn( null ).when( req ).getPathInfo();
        Mockito.doReturn( "MyPage", ( String )null ).when( req ).getParameter( "page" );
        Assertions.assertEquals( "MyPage", URLConstructor.parsePageFromURL( req, StandardCharsets.UTF_8 ) );
        Assertions.assertNull( URLConstructor.parsePageFromURL( req, StandardCharsets.UTF_8 ) ); // no req parameter should return null
    }

}
