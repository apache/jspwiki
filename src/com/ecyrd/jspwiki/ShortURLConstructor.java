package com.ecyrd.jspwiki;

import java.util.Properties;
import java.io.UnsupportedEncodingException;
import javax.servlet.http.HttpServletRequest;

public class ShortURLConstructor
    extends DefaultURLConstructor
{
    public void initialize( WikiEngine engine, 
                            Properties properties )
    {
        super.initialize( engine, properties );
    }

    /**
     *  Constructs the actual URL based on the context.
     */
    private String makeURL( String context,
                            String name,
                            boolean absolute )
    {
        String viewurl = "/wiki/%n";

        if( absolute ) 
            viewurl = "%uwiki/%n";

        if( context.equals(WikiContext.VIEW) )
        {
            if( name == null ) return makeURL("%u","",absolute); // FIXME
            return doReplacement( viewurl, name, absolute );
        }
        else if( context.equals(WikiContext.EDIT) )
        {
            return doReplacement( viewurl+"?do=Edit", name, absolute );
        }
        else if( context.equals(WikiContext.ATTACH) )
        {
            return doReplacement( "%Uattach/%n", name, absolute );
        }
        else if( context.equals(WikiContext.INFO) )
        {
            return doReplacement( viewurl+"?do=PageInfo", name, absolute );
        }
        else if( context.equals(WikiContext.DIFF) )
        {
            return doReplacement( viewurl+"?do=Diff", name, absolute );
        }
        else if( context.equals(WikiContext.NONE) )
        {
            return doReplacement( "%U%n", name, absolute );
        }
        else if( context.equals(WikiContext.UPLOAD) )
        {
            return doReplacement( viewurl+"?do=Upload", name, absolute ); 
        }
        else if( context.equals(WikiContext.COMMENT) )
        {
            return doReplacement( viewurl+"?do=Comment", name, absolute ); 
        }
        else if( context.equals(WikiContext.ERROR) )
        {
            return doReplacement( "%UError.jsp", name, absolute );
        }
        throw new InternalWikiException("Requested unsupported context "+context);
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
            if( context.equals(WikiContext.ATTACH) || context.equals(WikiContext.VIEW) )
            {
                parameters = "?"+parameters;
            }
            parameters = "&amp;"+parameters;
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

        if( pagereq == null )
        {
            pagereq = parsePageFromURL( request, encoding );

            if( pagereq != null ) pagereq = TextUtil.urlDecodeUTF8(pagereq);
        }

        return pagereq;
    }
}