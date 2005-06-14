/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav.methods;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ecyrd.jspwiki.FileUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.dav.DavContext;
import com.ecyrd.jspwiki.dav.DavPath;
import com.ecyrd.jspwiki.dav.DavProvider;
import com.ecyrd.jspwiki.dav.DavUtil;
import com.ecyrd.jspwiki.dav.items.DavItem;
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
    public GetMethod( DavProvider provider )
    {
        super( provider );
    }
    
    public void execute( HttpServletRequest req, HttpServletResponse res, DavPath dp )
        throws IOException
    {        
        DavItem di = m_provider.getItem( dp );
        
        if( di != null )
        {
            String mime = di.getContentType();
            res.setContentType( mime );
        
            long length = di.getLength();
        
            if( length >= 0 )
            {
                res.setContentLength( (int)di.getLength() );
            }
        
            InputStream in = di.getInputStream();
            
            if( in != null )
            {
                FileUtil.copyContents( in, res.getOutputStream() );
                
                in.close();
            }
            else
            {
                res.sendError( HttpServletResponse.SC_NO_CONTENT ); // FIXME: probably not correct
            }
            
        }
        else
        {
            res.sendError( HttpServletResponse.SC_NOT_FOUND );
        }
    }
}
