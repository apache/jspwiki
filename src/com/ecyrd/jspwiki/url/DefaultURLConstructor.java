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
import java.io.UnsupportedEncodingException;
import javax.servlet.http.HttpServletRequest;

import com.ecyrd.jspwiki.InternalWikiException;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;

/**
 *  Implements the default URL constructor using links directly to the
 *  JSP pages.  This is what JSPWiki by default is using.  For example,
 *  WikiContext.VIEW points at "Wiki.jsp", etc.
 *  
 *  @author Janne Jalkanen
 *  @since 2.2
 */
public class DefaultURLConstructor
    implements URLConstructor
{
    protected WikiEngine m_engine;

    /** Are URL styles relative or absolute? */
    protected boolean          m_useRelativeURLStyle = true;

    /**
     *  These are the patterns for each different request context.
     */
    private static String[] c_patterns = 
    {
     WikiContext.VIEW,   "%uWiki.jsp?page=%n",
     WikiContext.EDIT,   "%uEdit.jsp?page=%n",
     WikiContext.ATTACH, "%uattach/%n",
     WikiContext.INFO,   "%uPageInfo.jsp?page=%n",
     WikiContext.DIFF,   "%uDiff.jsp?page=%n",
     WikiContext.NONE,   "%u%n",
     WikiContext.UPLOAD, "%uUpload.jsp?page=%n",
     WikiContext.COMMENT,"%uComment.jsp?page=%n",
     WikiContext.LOGIN,  "%uLogin.jsp?page=%n",
     WikiContext.ERROR,  "%uError.jsp"
    };
                                      
    private static Properties c_patternList = TextUtil.createProperties(c_patterns);

    public void initialize( WikiEngine engine, 
                            Properties properties )
    {
        m_engine = engine;

        m_useRelativeURLStyle = "relative".equals( properties.getProperty( WikiEngine.PROP_REFSTYLE,
                                                                           "relative" ) );
    }

    protected final String doReplacement( String baseptrn, String name, boolean absolute )
    {
        String baseurl = "";

        if( absolute || !m_useRelativeURLStyle ) baseurl = m_engine.getBaseURL();

        baseptrn = TextUtil.replaceString( baseptrn, "%u", baseurl );
        baseptrn = TextUtil.replaceString( baseptrn, "%U", m_engine.getBaseURL() );
        baseptrn = TextUtil.replaceString( baseptrn, "%n", m_engine.encodeName(name) );

        return baseptrn;
    }

    
    /**
     *   Returns the pattern used for each URL style.
     * 
     * @param context
     * @param name
     * @return A pattern for replacement.
     */
    public static String getURLPattern( String context, String name )
    {
        if( context.equals(WikiContext.VIEW) )
        {
            if( name == null ) return "%uWiki.jsp"; // FIXME
        }
        
        String ptrn = c_patternList.getProperty(context);

        if( ptrn == null )
            throw new InternalWikiException("Requested unsupported context "+context);
        
        return ptrn;
    }
    
    /**
     *  Constructs the actual URL based on the context.
     */
    private String makeURL( String context,
                            String name,
                            boolean absolute )
    {
        if( context.equals(WikiContext.VIEW) )
        {
            if( name == null ) return makeURL("%uWiki.jsp","",absolute); // FIXME
        }
        
        return doReplacement( getURLPattern(context,name), name, absolute );
    }

    /**
     *  Constructs the URL with a bunch of parameters.
     *  @param parameters If null or empty, no parameters are added.
     */
    public String makeURL( String context,
                           String name,
                           boolean absolute,
                           String parameters )
    {
        if( parameters != null && parameters.length() > 0 )
        {            
            if( context.equals(WikiContext.ATTACH) )
            {
                parameters = "?"+parameters;
            }
            else
            {
                parameters = "&amp;"+parameters;
            }
        }
        else
        {
            parameters = "";
        }
        return makeURL( context, name, absolute )+parameters;
    }

    /**
     *  Should parse the "page" parameter from the actual
     *  request.
     */
    public String parsePage( String context,
                             HttpServletRequest request,
                             String encoding )
        throws UnsupportedEncodingException
    {
        String pagereq = m_engine.safeGetParameter( request, "page" );

        if( context.equals(WikiContext.ATTACH) )
        {
            pagereq = parsePageFromURL( request, encoding );
        }

        return pagereq;
    }

    /**
     *  Takes the name of the page from the request URI.
     *  The initial slash is also removed.  If there is no page,
     *  returns null.
     */
    public static String parsePageFromURL( HttpServletRequest request,
                                           String encoding )
        throws UnsupportedEncodingException
    {
        String name = request.getPathInfo();

        if( name == null || name.length() <= 1 )
        {
            return null;
        }
        else if( name.charAt(0) == '/' )
        {
            name = name.substring(1);
        }
       
        //
        //  This is required, because by default all URLs are handled
        //  as Latin1, even if they are really UTF-8.
        //
        
        name = TextUtil.urlDecode( name, encoding );
        
        return name;
    }

    
    /**
     *  This method is not needed for the DefaultURLConstructor.
     *  
     *  @author jalkanen
     *
     *  @since
     */
    public String getForwardPage( HttpServletRequest request )
    {
        return request.getPathInfo();
    }
}