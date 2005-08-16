package com.ecyrd.jspwiki.render;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.parser.WikiDocument;

public class XHTMLRenderer
    extends WikiRenderer 
{
    private static Logger log = Logger.getLogger( XHTMLRenderer.class );
    
    public XHTMLRenderer( WikiContext context, WikiDocument doc )
    {
        super( context, doc );
    }
    
    public String getString()
        throws IOException
    {
        m_document.setContext( m_context );

        XMLOutputter output = new XMLOutputter();
        
        StringWriter out = new StringWriter();
        
        Format fmt = Format.getRawFormat();
        fmt.setExpandEmptyElements( false );
        fmt.setLineSeparator("\n");
        output.setFormat( fmt );
        output.outputElementContent( m_document.getRootElement(), out );
        
        return out.toString();
    }
}
