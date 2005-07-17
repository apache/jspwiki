/* 
   JSPWiki - a JSP-based WikiWiki clone.

   Copyright (C) 2001-2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU Lesser General Public License as published by
   the Free Software Foundation; either version 2.1 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Lesser General Public License for more details.

   You should have received a copy of the GNU Lesser General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package com.ecyrd.jspwiki.url;

import java.util.Properties;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

import com.ecyrd.jspwiki.WikiEngine;

/**
 *  Provides an interface through which JSPWiki constructs URLs.
 *  JSPWiki calls the methods of this interface whenever an URL
 *  that points to any JSPWiki internals is required.  For example,
 *  if you need to find an URL to the editor page for page "TextFormattingRules",
 *  you would call makeURL( WikiContext.EDIT, "TextFormattingRules", false, null );
 *  
 *  @author Janne Jalkanen
 *  @since 2.2
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
     *  
     *  @param context The request context (@see WikiContext) that you want the URL for
     *  @param name The page name (or in case of WikiContext.NONE, the auxiliary JSP page 
     *              or resource you want to point at.  This must be URL encoded.
     *  @param absolute True, if you need an absolute URL.  False, if both relative and absolute
     *                  URLs are fine.
     *  @param parameters An URL parameter string (these must be URL-encoded, and separated with &amp;amp;)
     *  @return An URL pointing to the resource.  Must never return null - throw an InternalWikiException
     *          if something goes wrong.
     */
    public String makeURL( String context,
                           String name,
                           boolean absolute,
                           String parameters );

    /**
     *  Should parse the "page" parameter from the actual
     *  request.  This is essentially the reverse of makeURL() - whenever a request 
     *  constructed by calls to makeURL() is passed to this routine, it MUST be
     *  able to parse the resource name (WikiPage, Attachment, other resource) from
     *  the request.
     *  
     *  @param context In which request context the request was made (this should
     *                 help in parsing)
     *  @param request The HTTP request that was used when coming here
     *  @param encoding The encoding with which the request was made (UTF-8 or ISO-8859-1).
     *  @return This method must return the name of the resource.
     */
    public String parsePage( String context,
                             HttpServletRequest request,
                             String encoding )
        throws IOException;
    
    /**
     *  Returns information which JSP page should continue handling
     *  this type of request.
     *  
     * @param request The HTTP Request that was used to end up in this page.
     * @return "Wiki.jsp", "PageInfo.jsp", etc.  Just return the name,
     *         JSPWiki will figure out the page.
     */
    public String getForwardPage( HttpServletRequest request );
}