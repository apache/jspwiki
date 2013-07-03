/* 
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package org.apache.wiki.render;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Text;

import org.apache.wiki.WikiContext;
import org.apache.wiki.parser.PluginContent;
import org.apache.wiki.parser.WikiDocument;

/**
 *  Implements DOM-to-Creole rendering.
 *  <p>
 *  FIXME: This class is not yet completely done.
 *  
 */
public class CreoleRenderer extends WikiRenderer
{
    private static final String IMG_START = "{{";
    private static final String IMG_END = "}}";
    private static final String PLUGIN_START = "<<";
    private static final String PLUGIN_END = ">>";
    private static final String HREF_START = "[[";
    private static final String HREF_DELIMITER = "|";
    private static final String HREF_END = "]]";
    private static final String PRE_START = "{{{";
    private static final String PRE_END = "}}}";
    private static final String PLUGIN_IMAGE = "Image";
    private static final String PARAM_SRC = "src";
    private static final String HREF_ATTRIBUTE = "href";
    private static final String ONE_SPACE = " ";
    private static final String EMPTY_STRING = "";
    private static final String LINEBREAK = "\n";
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
       "hr", "----"  , EMPTY_STRING,
       "tt", "<<{{>>", "<<}}>>"
    };
    
    private int m_listCount = 0;
    private char m_listChar = 'x';

    private List<PluginContent> m_plugins = new ArrayList<PluginContent>();

    /**
     *  Creates a new Creole Renderer.
     *  
     */
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
        String endEl = EMPTY_STRING;
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
            sb.append( ONE_SPACE );
        }
        else if( A.equals(ce.getName()) )
        {
            String href = ce.getAttributeValue( HREF_ATTRIBUTE );
            String text = ce.getText();
            
            if( href.equals(text) )
            {
                sb.append( HREF_START + href + HREF_END );
            }
            else
            {
                sb.append( HREF_START + href+ HREF_DELIMITER + text +HREF_END);
            }
            // Do not render anything else 
            return;
        }
        else if( PRE.equals(ce.getName()) )
        {
            sb.append( PRE_START );
            sb.append( ce.getText() );
            sb.append( PRE_END );
            
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
                
                if( pc.getPluginName().equals( PLUGIN_IMAGE ) )
                {
                    sb.append( IMG_START + pc.getParameter( PARAM_SRC ) + IMG_END );
                }
                else
                {
                    m_plugins.add(pc);
                    sb.append( PLUGIN_START + pc.getPluginName() + ONE_SPACE + m_plugins.size() + PLUGIN_END );
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

        if( UL.equals( ce.getName() ) || OL.equals( ce.getName() ) )
        {
            m_listCount--;
        }
        else if( P.equals( ce.getName() ) )
        {
            sb.append( LINEBREAK );
        }
        
        sb.append(endEl);
    }
    
    /**
     *  {@inheritDoc}
     */
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
