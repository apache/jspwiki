/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.dav.methods;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ecyrd.jspwiki.FileUtil;
import com.ecyrd.jspwiki.dav.DavPath;
import com.ecyrd.jspwiki.dav.DavProvider;
import com.ecyrd.jspwiki.dav.items.DavItem;

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
