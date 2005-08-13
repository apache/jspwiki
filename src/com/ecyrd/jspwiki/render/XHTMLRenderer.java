package com.ecyrd.jspwiki.render;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.transform.Result;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.ProcessingInstruction;
import org.jdom.Text;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.parser.JSPWikiMarkupParser;
import com.ecyrd.jspwiki.parser.WikiDocument;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.plugin.PluginManager;

public class XHTMLRenderer
    extends WikiRenderer 
{
    private static Logger log = Logger.getLogger( XHTMLRenderer.class );
    
    public XHTMLRenderer( WikiContext context, WikiDocument doc )
    {
        super( context, doc );
    }
    
    /**
     *   Evaluates plugins into the DOM tree.
     *
     */
    private void executePlugins()
    {
        if( !m_enablePlugins ) return;
        
        WikiEngine engine = m_context.getEngine();
        
        try
        {
            XPath xpath = XPath.newInstance("//"+PluginManager.DOM_PLUGIN);
            
            List plugins = xpath.selectNodes( m_document );
            
            for( Iterator i = plugins.iterator(); i.hasNext(); )
            {
                Element el = (Element) i.next();
                
                try
                {
                    String name = el.getAttributeValue("class");
                    
                    System.out.println("Executing plugin "+name);
                    Map params = new TreeMap();

                    for( Iterator p = el.getChildren("param").iterator(); p.hasNext(); )
                    {
                        Element parelm = (Element)p.next();
                        
                        String key = parelm.getChildText("name");
                        String val = parelm.getChildText("value");
                        
                        params.put( key, val );
                    }
                    
                    String result = engine.getPluginManager().execute( m_context,
                                                                       name,
                                                                       params );
                    
                    Element parent = el.getParentElement();
                    int idx = parent.indexOf(el);
                    parent.removeContent( idx );
                    
                    //
                    // Turn off HTML escaping for plugins
                    //
                    parent.addContent( idx++, new ProcessingInstruction(Result.PI_DISABLE_OUTPUT_ESCAPING, "") );
                    parent.addContent( idx++, new Text(result) );
                    parent.addContent( idx++, new ProcessingInstruction(Result.PI_ENABLE_OUTPUT_ESCAPING, "") );

                }
                catch( PluginException e )
                {
                    log.info( "Failed to insert plugin", e );
                    log.info( "Root cause:",e.getRootThrowable() );
                    el.addContent( JSPWikiMarkupParser.makeError("Plugin insertion failed: "+e.getMessage()) );
                }
            }
        }
        catch( Exception e )
        {
            log.error("What?",e);
        }
        
    }
    
    public String getString()
        throws IOException
    {
        executePlugins();
        
        XMLOutputter output = new XMLOutputter();
        
        StringWriter out = new StringWriter();
        
        Format fmt = Format.getRawFormat();
        fmt.setExpandEmptyElements( false );
        fmt.setLineSeparator("\n");
        output.setFormat( fmt );
        output.outputElementContent( m_document.getDocument().getRootElement(), out );
        
        return out.toString();
    }
}
