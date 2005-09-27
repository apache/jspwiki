/* 
        ReferredPagesPlugin
        Dirk Frederickx Aug 2004, Jan 2005
        Janne Jalkanen 2005
        
        This program is free software; you can redistribute it and/or modify
        it under the terms of the GNU Lesser General Public License as published by
        the Free Software Foundation; either version 2.1 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU Lesser General Public License for more details.

        You should have received a copy of the GNU Lesser General Public License
        along with this program; if not, write to the Free Software
        Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
     */
package com.ecyrd.jspwiki.plugin;

/**
 *  Displays the pages referring to the current page.
 *
 *  <P>Parameters</P>
 *  <UL>
 *    <LI>name:    Name of the root page. Default name of calling page
 *    <LI>type:    local|externalattachment
 *    <LI>depth:   How many levels of pages to be parsed.
 *    <LI>include: Include only these pages. (eg. include='UC.*|BP.*' )
 *    <LI>exclude: Exclude with this pattern. (eg. exclude='LeftMenu' )
 *    <LI>format:  full|compact
 *        <br>FULL now expands all levels correctly
 *  </UL>
 *
 *  @author Dirk Frederickx
 */

import java.util.*;

import org.apache.log4j.Logger;
import org.apache.oro.text.regex.*;
import org.apache.oro.text.regex.Util;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.parser.JSPWikiMarkupParser;

public class ReferredPagesPlugin implements WikiPlugin
{
    private static Logger log = Logger.getLogger( ReferredPagesPlugin.class );
    private WikiEngine     m_engine;
    private int            m_depth;
    private HashSet        m_exists  = new HashSet();
    private StringBuffer   m_result  = new StringBuffer();
    private PatternMatcher m_matcher = new Perl5Matcher();
    private Pattern        m_includePattern;
    private Pattern        m_excludePattern;
    private boolean m_formatCompact  = true;
    private boolean m_formatSort     = false;
    
    public static final String PARAM_ROOT    = "page";
    public static final String PARAM_DEPTH   = "depth";
    public static final String PARAM_TYPE    = "type";
    public static final String PARAM_INCLUDE = "include";
    public static final String PARAM_EXCLUDE = "exclude";
    public static final String PARAM_FORMAT  = "format";
    public static final int    MIN_DEPTH = 1;
    public static final int    MAX_DEPTH = 8;
    
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
        String href  = m_engine.getViewURL(rootname);  
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
        getReferredPages(rootname, 0);

        // close and finish
        m_result.append ("</div>\n" ) ;
              
        return m_result.toString() ;
    }
        
        
    /**
     * Retrieves a list of all referred pages. Is called recursively
     * depending on the depth parameter
     */
    protected void getReferredPages(String pagename, int depth )
    {
        if( depth >= m_depth ) return;  // end of recursion
        if( pagename == null ) return;
        if( !m_engine.pageExists(pagename) ) return;

        ReferenceManager mgr = m_engine.getReferenceManager();
          
        Collection allPages = mgr.findRefersTo( pagename );
        
        handleLinks( allPages, ++depth, pagename );
    }
       
    protected void handleLinks(Collection links, int depth, String pagename)
    {
        boolean UL = false;
        HashSet localLinkSet = new HashSet();  // needed to skip multiple
        // links to the same page
        localLinkSet.add(pagename);
          
        ArrayList allLinks = new ArrayList();
        
        if( links != null )
            allLinks.addAll( links );
        
        if( m_formatSort ) Collections.sort(allLinks);
          
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
                    if( !UL ) { UL = true; m_result.append("<ul>\n");  }

                    m_result.append("<li> " + link + " </li>\n");          

                    getReferredPages( link, depth );  // added recursive
                                                      // call - on general
                                                      // request
                }
            }
            else
            {
                if( !UL ) { UL = true; m_result.append("<ul>\n");  }

                String href = m_engine.getViewURL(link);  
                m_result.append("<li><a class=\"wikipage\" href=\""+ href +"\">"+link+"</a></li>\n" );

                m_exists.add( link );
       
                getReferredPages( link, depth );
            }
        }

        if( UL ) m_result.append("</ul>\n");
    }
}

