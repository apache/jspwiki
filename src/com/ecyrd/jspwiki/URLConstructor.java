package com.ecyrd.jspwiki;

import java.util.Properties;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

/**
 *  An utility class for creating URLs for different purposes.
 */
public interface URLConstructor
{
    /**
     *  Initializes.  Note that the engine is not fully initialized
     *  at this point, so don't do anything fancy here - use lazy
     *  init, if you have to.
     */
    public void initialize( WikiEngine engine, 
                            Properties properties );

    /**
     *  Constructs the URL with a bunch of parameters.
     */
    public String makeURL( String context,
                           String name,
                           boolean absolute,
                           String parameters );

    /**
     *  Should parse the "page" parameter from the actual
     *  request.
     */
    public String parsePage( String context,
                             HttpServletRequest request,
                             String encoding )
        throws IOException;
}