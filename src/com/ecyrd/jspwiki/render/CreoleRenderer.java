package com.ecyrd.jspwiki.render;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Content;
import org.jdom.Element;
import org.jdom.Text;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.parser.PluginContent;
import com.ecyrd.jspwiki.parser.WikiDocument;

/**
 *  Implements DOM-to-Creole rendering.
 *  
 *  @author Janne Jalkanen
 */
public class CreoleRenderer extends WikiRenderer
{
    private static final String LI = "li";
    private static final String UL = "ul";
    private static final String OL = "ol";
    private static final String P  = "p";
    private static final String A  = "a";
    private static final String PRE = "pre";
    
    /**
     * Contains element, start markup, end markup
     */
    private static final String[] ELEMENTS = {
       "i" , "//"    , "//",
       "b" , "**"    , "**",
       "h2", "== "   , " ==",
       "h3", "=== "  , " ===",
       "h4", "==== " , " ====",
       "hr", "----"  , "",
       "tt", "<<{{>>", "<<}}>>"
    };
    
    private int m_listCount = 0;
    private char m_listChar = 'x';

    private List m_plugins = new ArrayList();

    public CreoleRenderer( WikiContext ctx, WikiDocument doc )
    {
        super( ctx, doc );
    }
    
    /**
     * Renders an element into the StringBuffer given
     * @param ce
     * @param sb
     */
    private void renderElement( Element ce, StringBuffer sb )
    {
        String endEl = "";
        for( int i = 0; i < ELEMENTS.length; i+=3 )
        {
            if( ELEMENTS[i].equals(ce.getName()) )
            {
                sb.append( ELEMENTS[i+1] );
                endEl = ELEMENTS[i+2];
            }
        }
        
        if( UL.equals(ce.getName()) )
        {
            m_listCount++;
            m_listChar = '*';
        }
        else if( OL.equals(ce.getName()) )
        {
            m_listCount++;
            m_listChar = '#';
        }
        else if( LI.equals(ce.getName()) )
        {
            for(int i = 0; i < m_listCount; i++ ) sb.append( m_listChar );
            sb.append(" ");
        }
        else if( A.equals(ce.getName()) )
        {
            String href = ce.getAttributeValue("href");
            String text = ce.getText();
            
            if( href.equals(text) )
            {
                sb.append("[["+href+"]]");
            }
            else
            {
                sb.append("[["+href+"|"+text+"]]");
            }
            // Do not render anything else 
            return;
        }
        else if( PRE.equals(ce.getName()) )
        {
            sb.append("{{{");
            sb.append( ce.getText() );
            sb.append("}}}");
            
            return;
        }
        
        //
        //  Go through the children
        //
        for( Iterator i = ce.getContent().iterator(); i.hasNext(); )
        {
            Content c = (Content)i.next();
            
            if( c instanceof PluginContent )
            {
                PluginContent pc = (PluginContent)c;
                
                if( pc.getPluginName().equals("Image") )
                {
                    sb.append("{{"+pc.getParameter("src")+"}}");
                }
                else
                {
                    m_plugins.add(pc);
                    sb.append( "<<"+pc.getPluginName()+" "+m_plugins.size()+">>" );
                }
            }
            else if( c instanceof Text )
            {
                sb.append( ((Text)c).getText() );
            }
            else if( c instanceof Element )
            {
                renderElement( (Element)c, sb );
            }
        }

        if( UL.equals(ce.getName()) || OL.equals(ce.getName()) )
        {
            m_listCount--;
        }
        else if( P.equals(ce.getName()) )
        {
            sb.append("\n");
        }
        
        sb.append(endEl);
    }
    
    public String getString() throws IOException
    {
        StringBuffer sb = new StringBuffer(1000);
        
        Element ce = m_document.getRootElement();
        
        //
        //  Traverse through the entire tree of everything.
        //
        
        renderElement( ce, sb );
        
        return sb.toString();
    }

}
