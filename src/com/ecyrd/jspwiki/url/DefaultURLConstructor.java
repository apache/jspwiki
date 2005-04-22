package com.ecyrd.jspwiki.url;

import java.util.Properties;
import java.io.UnsupportedEncodingException;
import javax.servlet.http.HttpServletRequest;

import com.ecyrd.jspwiki.InternalWikiException;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;

public class DefaultURLConstructor
    implements URLConstructor
{
    protected WikiEngine m_engine;
    private String m_viewURLPattern = "%uWiki.jsp?page=%n";

    /** Are URL styles relative or absolute? */
    protected boolean          m_useRelativeURLStyle = true;

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
            return "%uWiki.jsp?page=%n";
        }
        else if( context.equals(WikiContext.EDIT) )
        {
            return "%uEdit.jsp?page=%n";
        }
        else if( context.equals(WikiContext.ATTACH) )
        {
            return "%uattach/%n";
        }
        else if( context.equals(WikiContext.INFO) )
        {
            return "%uPageInfo.jsp?page=%n";
        }
        else if( context.equals(WikiContext.DIFF) )
        {
            return "%uDiff.jsp?page=%n";
        }
        else if( context.equals(WikiContext.NONE) )
        {
            return "%u%n";
        }
        else if( context.equals(WikiContext.UPLOAD) )
        {
            return "%uUpload.jsp?page=%n"; 
        }
        else if( context.equals(WikiContext.COMMENT) )
        {
            return "%uComment.jsp?page=%n"; 
        }
        else if( context.equals(WikiContext.ERROR) )
        {
            return "%uError.jsp";
        }
        throw new InternalWikiException("Requested unsupported context "+context);
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