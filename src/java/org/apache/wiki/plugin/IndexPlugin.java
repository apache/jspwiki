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
import java.util.regex.Pattern;

import org.apache.ecs.Element;
import org.apache.ecs.xhtml.div;
import org.apache.ecs.xhtml.span;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.PluginException;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;


/**
 *  A plugin that creates an index of pages according to a certain  pattern.
 *  <br>The default is to include all pages.
 *  <p>
 *  This is a complete rewrite of the old IndexPlugin under an Apache license.
 *  <p>Parameters (From AbstractReferralPlugin) : </p>
 *  <ul>
 *    <li><b>include</b> - A regexp pattern for marking which pages should be included.</li>
 *    <li><b>exclude</b> - A regexp pattern for marking which pages should be excluded.</li>
 *    <li><b>showAttachments</b> - Indicates if attachments should also be shown, the default is true.</li>
 *  </ul>
 */
public class IndexPlugin  extends AbstractReferralPlugin implements WikiPlugin
{
    private static Logger log = LoggerFactory.getLogger( IndexPlugin.class );
    
    /** The parameter name for setting the showAttachment.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_SHOW_ATTACHMENTS = "showAttachments";
    
    /**
     *  {@inheritDoc}
     */
    public String execute( WikiContext context, Map params ) throws PluginException
    {
        String include = (String)params.get( PARAM_INCLUDE );
        String exclude = (String)params.get( PARAM_EXCLUDE );
        String showAttachmentsString = (String) params.get( PARAM_SHOW_ATTACHMENTS );
        boolean showAttachments = true;
        if( "false".equals( showAttachmentsString ) )
        {
            showAttachments = false;
        }
        
        List<String> pages;
        div masterDiv = new div();
        masterDiv.setClass( "index" );
        
        div indexDiv = new div();
        
        masterDiv.addElement( indexDiv );
        indexDiv.setClass( "header" );
        try
        {
            pages = listPages( context, include, exclude, showAttachments );
            Collections.sort( pages );
            
            char initialChar = ' ';
            
            div currentDiv = new div();
            
            for( String name : pages )
            {
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

    /**
     *  Grabs a list of all pages and filters them according to the include/exclude patterns.
     *  
     * @param context
     * @param include
     * @param exclude
     * @return A list containing page names which matched the filters.
     * @throws ProviderException
     */
    private List<String> listPages( WikiContext context, String include, String exclude, boolean showAttachments )
        throws ProviderException
    {
        Pattern includePtrn = include != null ? Pattern.compile( include ) : Pattern.compile(".*");
        Pattern excludePtrn = exclude != null ? Pattern.compile( exclude ) : Pattern.compile("\\p{Cntrl}"); // There are no control characters in page names
        
        ArrayList<String> result = new ArrayList<String>();
        
        Collection pages = context.getEngine().getReferenceManager().findCreated();
        
        for( Iterator i = pages.iterator(); i.hasNext(); )
        {
            String pageName = (String) i.next();

            if( excludePtrn.matcher( pageName ).matches() ) continue;
            if( includePtrn.matcher( pageName ).matches() )
            {
                if( showAttachments )
                {
                    result.add( pageName );
                }
                else
                {
                    if( !pageName.contains( "/" ) )
                    {
                        result.add( pageName );
                    }
                }
            }
        }
        
        return result;
    }

}
