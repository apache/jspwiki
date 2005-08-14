package com.ecyrd.jspwiki.render;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.transform.Result;

import org.apache.log4j.Logger;
import org.jdom.Content;
import org.jdom.Element;
import org.jdom.ProcessingInstruction;
import org.jdom.Text;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import com.ecyrd.jspwiki.NoSuchVariableException;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.parser.JSPWikiMarkupParser;
import com.ecyrd.jspwiki.parser.WikiDocument;
import com.ecyrd.jspwiki.plugin.PluginException;

public class XHTMLRenderer
    extends WikiRenderer 
{
    private static Logger log = Logger.getLogger( XHTMLRenderer.class );
    
    public XHTMLRenderer( WikiContext context, WikiDocument doc )
    {
        super( context, doc );
    }

    private Content executeSinglePlugin( Element el )
    {
        String result;
        
        try
        {
            String name = el.getAttributeValue("class");
        
            Map params = new TreeMap();

            for( Iterator p = el.getChildren("param").iterator(); p.hasNext(); )
            {
                Element parelm = (Element)p.next();
            
                String key = parelm.getChildText("name");
                String val = parelm.getChildText("value");
            
                params.put( key, val );
            }
        
            WikiEngine engine = m_context.getEngine();
            result = engine.getPluginManager().execute( m_context,
                                                        name,
                                                        params );
        }
        catch( Exception e )
        {
            log.info("Failed to execute plugin",e);
            return JSPWikiMarkupParser.makeError("Plugin insertion failed: "+e.getMessage());
        }
        return new Text( result );
    }
    
    private Content executeVariable( Element el )
    {
        String result;
        
        try
        {
            String varName = el.getAttributeValue( "name" );
            result = m_context.getEngine().getVariableManager().parseAndGetValue( m_context, varName );
        }
        catch( NoSuchVariableException e )
        {
            log.info("Failed to find variable",e);
            return JSPWikiMarkupParser.makeError("No such variable: "+e.getMessage());            
        }
        return new Text( result );
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
            XPath xpath = XPath.newInstance("//plugin | //variable");
            
            List plugins = xpath.selectNodes( m_document.getDocument() );
            
            for( Iterator i = plugins.iterator(); i.hasNext(); )
            {
                Element el = (Element) i.next();
                
                Content res = null;
                    
                if( el.getName().equals("plugin") )
                {
                    res = executeSinglePlugin( el );
                }
                else if( el.getName().equals("variable") )
                {
                    res = executeVariable( el );
                }
                    
                Element parent = el.getParentElement();
                int idx = parent.indexOf(el);
                parent.removeContent( idx );
                    
                //
                // Turn off HTML escaping for plugins
                //
                parent.addContent( idx++, new ProcessingInstruction(Result.PI_DISABLE_OUTPUT_ESCAPING, "") );
                parent.addContent( idx++, res );
                parent.addContent( idx++, new ProcessingInstruction(Result.PI_ENABLE_OUTPUT_ESCAPING, "") );
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
