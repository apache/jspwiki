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

import org.apache.wiki.*;
import org.apache.wiki.api.PluginException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.ContentManager;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.content.ReferenceManager;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.util.TextUtil;



/**
 *  Displays the pages referring to the current page.
 *
 *  <p>Parameters</p>
 *  <ul>
 *    <li><b>name</b> - Name of the root page. Default name of calling page
 *    <li><b>depth</b> - How many levels of pages to be parsed.
 *    <li><b>include</b> - Include only these pages. (eg. include='UC.*|BP.*' )
 *    <li><b>exclude</b> - Exclude with this pattern. (eg. exclude='LeftMenu' )
 *    <li><b>format</b> -  full|compact, FULL now expands all levels correctly
 *  </ul>
 *
 *  @author Dirk Frederickx
 */
public class ReferredPagesPlugin extends AbstractFilteredPlugin implements WikiPlugin
{
    private static Logger log = LoggerFactory.getLogger( ReferredPagesPlugin.class );
    private WikiEngine     m_engine;
    private int            m_depth;
    private HashSet<WikiPath> m_exists  = new HashSet<WikiPath>();
    private StringBuilder   m_result  = new StringBuilder(1024);
    private boolean m_formatCompact  = true;
    private boolean m_formatSort     = false;

    /** The parameter name for the root page to start from.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_ROOT    = "page";

    /** The parameter name for the depth.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_DEPTH   = "depth";
    
    /** The parameter name for the format.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_FORMAT  = "format";
    
    /** The minimum depth. Value is <tt>{@value}</tt>. */
    public static final int    MIN_DEPTH = 1;
    
    /** The maximum depth. Value is <tt>{@value}</tt>. */
    public static final int    MAX_DEPTH = 8;

    /**
     *  {@inheritDoc}
     */
    public String execute( WikiContext context, Map<String,Object> params )
        throws PluginException
    {
        m_engine = context.getEngine();
        super.initialize( context, params );

        WikiPage         page   = context.getPage();
        if( page == null ) return "";

        // parse parameters
        String rootname = (String)params.get( PARAM_ROOT );
        if( rootname == null ) rootname = page.getName() ;

        String format = (String)params.get( PARAM_FORMAT );
        if( format == null) format = "";
        if( format.indexOf( "full" ) >=0 ) m_formatCompact = false ;
        if( format.indexOf( "sort" ) >=0 ) m_formatSort    = true  ;

        m_depth = TextUtil.parseIntParameter( (String)params.get( PARAM_DEPTH ), MIN_DEPTH );
        if( m_depth > MAX_DEPTH )  m_depth = MAX_DEPTH;

        String includePattern = filterString( m_include );
        String excludePattern = filterString( m_exclude );

        log.debug( "Fetching referred pages for "+ rootname +
                   " with a depth of "+ m_depth +
                   " with include pattern of "+ includePattern +
                   " with exclude pattern of "+ excludePattern );

        //
        // do the actual work
        //
        String href  = context.getViewURL(rootname);
        String title = "ReferredPagesPlugin: depth["+m_depth+
                       "] include["+includePattern+"] exclude["+excludePattern+
                       "] format["+(m_formatCompact ? "compact" : "full") +
                       (m_formatSort ? " sort" : "") + "]";

        WikiPath root = WikiPath.valueOf( rootname );
        String rootString = ContentManager.DEFAULT_SPACE.equals( root.getSpace() ) ? root.getPath() : root.toString();
        m_result.append("<div class=\"ReferredPagesPlugin\">\n");
        m_result.append("<a class=\"wikipage\" href=\""+ href +
                        "\" title=\"" + title +
                        "\">" + rootString + "</a>\n");
        m_exists.add(WikiPath.valueOf( rootname ) );

        // pre compile all needed patterns
        // glob compiler :  * is 0..n instance of any char  -- more convenient as input
        // perl5 compiler : .* is 0..n instances of any char -- more powerful
        //PatternCompiler g_compiler = new GlobCompiler();

        // go get all referred links
        try
        {
            getReferredPages( context, WikiPath.valueOf( rootname ), 0);
        }
        catch(Exception e)
        {
            throw new PluginException("Failed to get referred pages "+e.getMessage());
        }

        // close and finish
        m_result.append ("</div>\n" ) ;

        return m_result.toString() ;
    }

    private String filterString( Pattern[] patterns )
    {
        StringBuilder s = new StringBuilder();
        for ( int i = 0; i < patterns.length; i++ )
        {
            s.append( patterns[i].pattern() );
            if ( i < patterns.length - 1 )
            {
                s.append( ',' );
            }
        }
        return s.toString();
    }

    /**
     * Retrieves a list of all referred pages. Is called recursively
     * depending on the depth parameter
     * @throws PageNotFoundException 
     * @throws ProviderException 
     */
    private void getReferredPages( WikiContext context, WikiPath path, int depth ) throws ProviderException, PageNotFoundException
    {
        if( depth >= m_depth ) return;  // end of recursion
        if( path == null ) return;
        
        if( !m_engine.pageExists( path.toString() ) ) return;

        ReferenceManager mgr = m_engine.getReferenceManager();

        List<WikiPath> pages = mgr.getRefersTo( path );
        if( pages != null && pages.size() > 0 )
        {
            pages = super.filterCollection( pages );
        }

        handleLinks( context, pages, ++depth, path );
    }

    private void handleLinks(WikiContext context, List<WikiPath> links, int depth, WikiPath path ) throws ProviderException, PageNotFoundException
    {
        boolean isUL = false;
        links.add( path );

        if( m_formatSort ) Collections.sort(links);

        for( WikiPath link : links )
        {
            if( !m_engine.pageExists( link.toString() ) ) continue; // hide links to non existing pages

            if( m_exists.contains( link ) )
            {
                if( !m_formatCompact )
                {
                    if( !isUL )
                    {
                        isUL = true; m_result.append("<ul>\n");
                    }

                    m_result.append("<li> " + link + " </li>\n");

                    getReferredPages( context, link, depth );  // recursive
                }
            }
            else
            {
                if( !isUL )
                {
                    isUL = true; m_result.append("<ul>\n");
                }

                String href = context.getURL(WikiContext.VIEW,link.toString());
                String linkString = ContentManager.DEFAULT_SPACE.equals( link.getSpace() ) ? link.getPath() : link.toString();
                m_result.append("<li><a class=\"wikipage\" href=\""+ href +"\">"+linkString+"</a></li>\n" );
                m_exists.add( link );

                getReferredPages( context, link, depth );
            }
        }

        if( isUL ) m_result.append("</ul>\n");
    }
}

