/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav.methods;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.dav.DavContext;
import com.ecyrd.jspwiki.dav.DavUtil;
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public class GetMethod extends DavMethod
{

    /**
     * 
     */
    public GetMethod( WikiEngine engine )
    {
        super( engine );
    }

    private void returnMainCollection( HttpServletRequest req, HttpServletResponse res )
    throws IOException
    {
        res.getWriter().println("raw");
    }
    
    private WikiContext createContext( HttpServletRequest req, DavContext dc )
    {
        String pagename = dc.m_page;
        
        if( "raw".equals(dc.m_davcontext) )
        {
            if( pagename.endsWith(".txt") ) pagename = pagename.substring( 0, pagename.length()-4 );
        }
        else if( "html".equals(dc.m_davcontext))
        {
            if( pagename.endsWith(".html") ) pagename = pagename.substring( 0, pagename.length()-5 );
        }
            
        WikiPage page = m_engine.getPage( pagename );
        WikiContext wc = new WikiContext( m_engine, page );
        wc.setRequestContext( "dav" );
        wc.setHttpRequest( req );

        return wc;
    }
    
    public void execute( HttpServletRequest req, HttpServletResponse res )
    throws IOException
    {
        DavContext dc = new DavContext( req );
        
        WikiContext wc = createContext( req, dc );
        try
        {
            if( "".equals( dc.m_davcontext ))
            {
                returnMainCollection(req,res);
            }
            else
            {
                if( "raw".equals(dc.m_davcontext) )
                {
                    if( dc.m_page == null )
                    {
                        Collection pages = m_engine.getPageManager().getAllPages();
                
                        DavUtil.sendHTMLResponse( res, DavUtil.getCollectionInHTML( wc, pages ) );
                    }
                    else
                    {
                        if( wc.getPage() != null )
                        {
                            String content = m_engine.getPureText(wc.getPage());
                            
                            res.setContentLength( content.length() );
                            res.setContentType( "text/plain; charset=\"UTF8\"" );
                            res.getWriter().print( content );
                        }
                        else
                        {
                            res.sendError( HttpServletResponse.SC_NOT_FOUND );
                        }
                    }
                }
                else if( "html".equals(dc.m_davcontext) )
                {
                    if( dc.m_page == null )
                    {
                        Collection pages = m_engine.getPageManager().getAllPages();
                        DavUtil.sendHTMLResponse( res, DavUtil.getCollectionInHTML( wc, pages ) );
                    }
                    else
                    {    
                        if( wc.getPage() != null )
                        {
                            String result = m_engine.getHTML( wc, wc.getPage() );
                            
                            System.out.println("RESULT="+result);
                            res.setContentLength( result.length() );
                            res.setContentType( "text/html; charset=\"UTF-8\"" );
                            res.getWriter().print( result );
                        }
                        else
                        {
                            res.sendError( HttpServletResponse.SC_NOT_FOUND );
                        }
                    }
                }
                else
                {
                    res.sendError( HttpServletResponse.SC_NOT_FOUND,
                                   "Could not find "+dc.m_davcontext );
                }
            }
        }
        catch( ProviderException e )
        {
            e.printStackTrace( System.out );
            res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage() );
        }
    }
}
