package com.ecyrd.jspwiki.render;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.xpath.XPath;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.parser.WikiDocument;

/**
 *  A simple renderer that just renders all the text() nodes
 *  from the DOM tree.  This is very useful for cleaning away
 *  all of the XHTML.
 *  
 *  @author Janne Jalkanen
 *  @since  2.4
 */
public class CleanTextRenderer
    extends WikiRenderer
{
    protected static Logger log = Logger.getLogger( CleanTextRenderer.class );
    
    public CleanTextRenderer( WikiContext context, WikiDocument doc )
    {
        super( context, doc );
    }
    
    public String getString()
        throws IOException
    {
        StringBuffer sb = new StringBuffer();
        
        try
        {
            XPath xp = XPath.newInstance("//text()");
        
            List nodes = xp.selectNodes(m_document.getDocument());
            
            for( Iterator i = nodes.iterator(); i.hasNext(); )
            {
                Object el = i.next();
                
                if( el instanceof Text )
                {
                    sb.append( ((Text)el).getValue() );
                }
            }
        }
        catch( JDOMException e )
        {
            log.error("Could not parse XPATH expression");
            throw new IOException( e.getMessage() );
        }
    
        return sb.toString();
    }
}
