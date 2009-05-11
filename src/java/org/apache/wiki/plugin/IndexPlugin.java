/* 
    JSPWiki - a JSP-based WikiWiki clone.

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
package org.apache.wiki.plugin;

import java.util.*;
import org.apache.ecs.Element;
import org.apache.ecs.xhtml.div;
import org.apache.ecs.xhtml.span;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.PluginException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.ContentManager;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;


/**
 *  A plugin that creates an index of pages according to a certain  pattern.
 *  <br>The default is to include all pages.
 *  <p>
 *  This is a complete rewrite of the old IndexPlugin under an Apache license.
 *  <p>Parameters (From AbstractFilteredPlugin) : </p>
 */
public class IndexPlugin  extends AbstractFilteredPlugin implements WikiPlugin
{
    private static Logger log = LoggerFactory.getLogger( IndexPlugin.class );
    
    /**
     *  {@inheritDoc}
     */
    public String execute( WikiContext context, Map<String,Object> params ) throws PluginException
    {
        super.initialize( context, params );
        
        div masterDiv = new div();
        masterDiv.setClass( "index" );
        
        div indexDiv = new div();
        
        masterDiv.addElement( indexDiv );
        indexDiv.setClass( "header" );
        try
        {
            List<WikiPage> pages = context.getEngine().getContentManager().getAllPages( ContentManager.DEFAULT_SPACE  );
            pages = super.filterCollection( pages );
            Collections.sort( pages );
            
            char initialChar = ' ';
            
            div currentDiv = new div();
            
            for( WikiPage page : pages )
            {
                String name = page.getName();
                if( name.charAt( 0 ) != initialChar )
                {
                    if( initialChar != ' ' ) indexDiv.addElement( " - " );
                    initialChar = name.charAt( 0 );
                    
                    masterDiv.addElement( makeHeader(initialChar) );
            
                    currentDiv = new div();
                    currentDiv.setClass("body");
                    masterDiv.addElement( currentDiv );
                    
                    indexDiv.addElement( "<a href='#"+initialChar+"'>"+initialChar+"</a>" );
                }
                else
                {
                    currentDiv.addElement( ", " );
                }
                
                String link = "<a href='"+
                              context.getURL( WikiContext.VIEW, name )+
                              "'>"+name+"</a>";
                
                currentDiv.addElement( link );
            }
        }
        catch( ProviderException e )
        {
            log.warn("Could not load page index",e);
            throw new PluginException( e.getMessage() );
        }
        
        return masterDiv.toString();
    }
    
    /**
     *  Create the DOM for a heading
     * @param initialChar
     * @return A span element.
     */
    private Element makeHeader( char initialChar )
    {
        span s = new span();
        s.setClass( "section" );
        s.addElement( "<a name='"+initialChar+"'>"+initialChar+"</a>" );

        return s;
    }

}
