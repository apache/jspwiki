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
package org.apache.wiki.plugin;

import java.util.*;

import org.apache.log4j.Logger;
import org.apache.oro.text.regex.*;

import org.apache.wiki.*;


/**
 *  Displays the pages referring to the current page.
 *
 *  <p>Parameters</p>
 *  <ul>
 *    <li><b>name</b> - Name of the root page. Default name of calling page
 *    <li><b>type</b> - local|externalattachment
 *    <li><b>depth</b> - How many levels of pages to be parsed.
 *    <li><b>include</b> - Include only these pages. (eg. include='UC.*|BP.*' )
 *    <li><b>exclude</b> - Exclude with this pattern. (eg. exclude='LeftMenu' )
 *    <li><b>format</b> -  full|compact, FULL now expands all levels correctly
 *  </ul>
 *
 */
public class ReferredPagesPlugin implements WikiPlugin
{
    private static Logger log = Logger.getLogger( ReferredPagesPlugin.class );
    private WikiEngine     m_engine;
    private int            m_depth;
    private HashSet<String> m_exists  = new HashSet<String>();
    private StringBuffer   m_result  = new StringBuffer(1024);
    private PatternMatcher m_matcher = new Perl5Matcher();
    private Pattern        m_includePattern;
    private Pattern        m_excludePattern;
    private boolean m_formatCompact  = true;
    private boolean m_formatSort     = false;

    /** The parameter name for the root page to start from.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_ROOT    = "page";

    /** The parameter name for the depth.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_DEPTH   = "depth";

    /** The parameter name for the type of the references.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_TYPE    = "type";
    
    /** The parameter name for the included pages.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_INCLUDE = "include";
    
    /** The parameter name for the excluded pages.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_EXCLUDE = "exclude";
    
    /** The parameter name for the format.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_FORMAT  = "format";
    
    /** The minimum depth. Value is <tt>{@value}</tt>. */
    public static final int    MIN_DEPTH = 1;
    
    /** The maximum depth. Value is <tt>{@value}</tt>. */
    public static final int    MAX_DEPTH = 8;

    /**
     *  {@inheritDoc}
     */
    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        m_engine = context.getEngine();

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

        String includePattern = (String) params.get(PARAM_INCLUDE);
        if( includePattern == null ) includePattern = ".*";

        String excludePattern = (String) params.get(PARAM_EXCLUDE);
        if( excludePattern == null ) excludePattern = "^$";

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

        m_result.append("<div class=\"ReferredPagesPlugin\">\n");
        m_result.append("<a class=\"wikipage\" href=\""+ href +
                        "\" title=\"" + title +
                        "\">" + rootname + "</a>\n");
        m_exists.add(rootname);

        // pre compile all needed patterns
        // glob compiler :  * is 0..n instance of any char  -- more convenient as input
        // perl5 compiler : .* is 0..n instances of any char -- more powerful
        //PatternCompiler g_compiler = new GlobCompiler();
        PatternCompiler compiler = new Perl5Compiler();

        try
        {
            m_includePattern = compiler.compile(includePattern);

            m_excludePattern = compiler.compile(excludePattern);
        }
        catch( MalformedPatternException e )
        {
            if (m_includePattern == null )
            {
                throw new PluginException("Illegal include pattern detected.");
            }
            else if (m_excludePattern == null )
            {
                throw new PluginException("Illegal exclude pattern detected.");
            }
            else
            {
                throw new PluginException("Illegal internal pattern detected.");
            }
        }

        // go get all referred links
        getReferredPages(context,rootname, 0);

        // close and finish
        m_result.append ("</div>\n" ) ;

        return m_result.toString() ;
    }


    /**
     * Retrieves a list of all referred pages. Is called recursively
     * depending on the depth parameter
     */
    @SuppressWarnings("unchecked")
    private void getReferredPages( WikiContext context, String pagename, int depth )
    {
        if( depth >= m_depth ) return;  // end of recursion
        if( pagename == null ) return;
        if( !m_engine.pageExists(pagename) ) return;

        ReferenceManager mgr = m_engine.getReferenceManager();

        Collection<String> allPages = mgr.findRefersTo( pagename );

        handleLinks( context, allPages, ++depth, pagename );
    }

    private void handleLinks(WikiContext context,Collection<String> links, int depth, String pagename)
    {
        boolean isUL = false;
        HashSet<String> localLinkSet = new HashSet<String>();  // needed to skip multiple
        // links to the same page
        localLinkSet.add(pagename);

        ArrayList<String> allLinks = new ArrayList<String>();

        if( links != null )
            allLinks.addAll( links );

        if( m_formatSort ) context.getEngine().getPageSorter().sort( allLinks );

        for( Iterator i = allLinks.iterator(); i.hasNext(); )
        {
            String link = (String) i.next() ;

            if( localLinkSet.contains( link ) ) continue; // skip multiple
                                                          // links to the same
                                                          // page
            localLinkSet.add( link );

            if( !m_engine.pageExists( link ) ) continue; // hide links to non
                                                         // existing pages

            if(  m_matcher.matches( link , m_excludePattern ) ) continue;
            if( !m_matcher.matches( link , m_includePattern ) ) continue;

            if( m_exists.contains( link ) )
            {
                if( !m_formatCompact )
                {
                    if( !isUL )
                    {
                        isUL = true; m_result.append("<ul>\n");
                    }

                    m_result.append("<li> " + link + " </li>\n");

                    getReferredPages( context, link, depth );  // added recursive
                                                      // call - on general
                                                      // request
                }
            }
            else
            {
                if( !isUL )
                {
                    isUL = true; m_result.append("<ul>\n");
                }

                String href = context.getURL(WikiContext.VIEW,link);
                m_result.append("<li><a class=\"wikipage\" href=\""+ href +"\">"+link+"</a></li>\n" );

                m_exists.add( link );

                getReferredPages( context, link, depth );
            }
        }

        if( isUL ) m_result.append("</ul>\n");
    }
}

