
package com.ecyrd.jspwiki;
import java.util.Properties;
import javax.servlet.*;

/**
 *  Simple test engine that always assumes pages are found.
 */
public class TestEngine extends WikiEngine
{
    public TestEngine( Properties props )
        throws NoRequiredPropertyException,
               ServletException
    {
        super( props );
    }

    public boolean pageExists( String page )
    {
        return true;
    }
}
