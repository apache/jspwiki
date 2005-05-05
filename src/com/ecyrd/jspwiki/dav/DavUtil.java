/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import org.apache.ecs.XhtmlDocument;
import org.apache.ecs.xhtml.li;
import org.apache.ecs.xhtml.ul;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiPage;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public class DavUtil
{
    public static String getCollectionInHTML( WikiContext context, Collection coll )
    {
        XhtmlDocument doc = new XhtmlDocument("UTF-8");
        
        ul content = new ul();
        
        for( Iterator i = coll.iterator(); i.hasNext(); )
        {
            Object o = i.next();
            
            if( o instanceof WikiPage )
            {
                WikiPage p = (WikiPage)o;
                content.addElement( new li().addElement( p.getName() ) );
            }
            else if( o instanceof String )
            {
                content.addElement( new li().addElement( o.toString() ));
            }
        }

        doc.appendBody( content );
        
        return doc.toString();
    }
    
    public static void sendHTMLResponse( HttpServletResponse res, String txt )
    throws IOException
    {
        res.setContentType( "text/html; charset=UTF-8" );
        res.setContentLength( txt.length() );
        
        res.getWriter().print( txt );
    }
    
}
