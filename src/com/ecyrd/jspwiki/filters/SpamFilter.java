/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.filters;

import java.util.*;

import javax.servlet.http.HttpServletRequest;

import com.ecyrd.jspwiki.*;

import org.apache.oro.text.regex.*;

import org.apache.log4j.Logger;

/**
 *  A regular expression-based spamfilter that can also do choke modifications.
 *
 *  Parameters:
 *  <ul>
 *    <li>wordlist - Page name where the regexps are found.  Use [{SET spamwords='regexp list separated with spaces'}] on
 *     that page.  Default is "SpamFilterWordList".
 *    <li>errorpage - The page to which the user is redirected.  Has a special variable $msg which states the reason. Default is "RejectedMessage".
 *    <li>pagechangesinminute - How many page changes are allowed/minute.  Default is 5.
 *    <li>bantime - How long an IP address stays on the temporary ban list (default is 60 for 60 minutes).
 *  </ul>
 *  @since 2.1.112
 *  @author Janne Jalkanen
 */
public class SpamFilter
    extends BasicPageFilter
{
    private String m_forbiddenWordsPage = "SpamFilterWordList";
    private String m_errorPage          = "RejectedMessage";

    private static final String LISTVAR = "spamwords";
    private PatternMatcher m_matcher = new Perl5Matcher();
    private PatternCompiler m_compiler = new Perl5Compiler();

    private Collection m_spamPatterns = null;

    private Date m_lastRebuild = new Date( 0L );

    static Logger log = Logger.getLogger( SpamFilter.class );

    public static final String PROP_WORDLIST  = "wordlist";
    public static final String PROP_ERRORPAGE = "errorpage";
    public static final String PROP_PAGECHANGES = "pagechangesinminute";
    public static final String PROP_BANTIME   = "bantime";
    
    private Vector m_temporaryBanList = new Vector();
    
    private int m_banTime = 60; // minutes
    
    private Vector m_lastModifications = new Vector();
    
    /**
     *  How many times a single IP address can change a page per minute?
     */
    private int m_limitSinglePageChanges = 5;
    
    public void initialize( Properties properties )
    {
        m_forbiddenWordsPage = properties.getProperty( PROP_WORDLIST, 
                                                       m_forbiddenWordsPage );
        m_errorPage = properties.getProperty( PROP_ERRORPAGE, 
                                              m_errorPage );

        m_limitSinglePageChanges = TextUtil.getIntegerProperty( properties,
                                                                PROP_PAGECHANGES,
                                                                m_limitSinglePageChanges );
        
        m_banTime = TextUtil.getIntegerProperty( properties,
                                                 PROP_BANTIME,
                                                 m_banTime );
        
        log.info("Spam filter initialized.  Temporary ban time "+m_banTime+
                 " mins, max page changes/minute: "+m_limitSinglePageChanges );
    }

    private Collection parseWordList( WikiPage source, String list )
    {
        ArrayList compiledpatterns = new ArrayList();

        if( list != null )
        {
            StringTokenizer tok = new StringTokenizer( list, " \t\n" );

            while( tok.hasMoreTokens() )
            {
                String pattern = tok.nextToken();

                try
                {
                    compiledpatterns.add( m_compiler.compile( pattern ) );
                }
                catch( MalformedPatternException e )
                {
                    log.debug( "Malformed spam filter pattern "+pattern );
                
                    source.setAttribute("error", "Malformed spam filter pattern "+pattern);
                }
            }
        }

        return compiledpatterns;
    }

    private synchronized void checkSinglePageChange( WikiContext context )
        throws RedirectException
    {
        HttpServletRequest req = context.getHttpRequest();
        
        if( req != null )
        {
            String addr = req.getRemoteAddr();
            int counter = 0;
                
            long time = System.currentTimeMillis()-60*1000L; // 1 minute
            
            for( Iterator i = m_lastModifications.iterator(); i.hasNext(); )
            {
                Host host = (Host)i.next();
                
                //
                //  Check if this item is invalid
                //
                if( host.getAddedTime() < time )
                {
                    log.debug("Removed host "+host.getAddress()+" from modification queue (expired)");
                    i.remove();
                    continue;
                }
                
                if( host.getAddress().equals(addr) )
                {
                    counter++;
                }
            }
            
            if( counter > m_limitSinglePageChanges )
            {
                Host host = new Host( addr );
                
                m_temporaryBanList.add( host );
                
                log.info("Added host "+addr+" to temporary ban list for doing too many modifications/minute" );
                throw new RedirectException( "Too many modifications/minute",
                                             context.getViewURL( m_errorPage ) );
            }
            
            m_lastModifications.add( new Host( addr ) );
        }
    }

    private synchronized void cleanBanList()
    {
        long now = System.currentTimeMillis();
        
        for( Iterator i = m_temporaryBanList.iterator(); i.hasNext(); )
        {
            Host host = (Host)i.next();
            
            if( host.getReleaseTime() < now )
            {
                log.debug("Removed host "+host.getAddress()+" from temporary ban list (expired)");
                i.remove();
            }
        }
    }
    
    
    private void checkBanList( WikiContext context )
        throws RedirectException
    {
        HttpServletRequest req = context.getHttpRequest();
        
        if( req != null )
        {
            String remote = req.getRemoteAddr();
            
            long now = System.currentTimeMillis();
            
            for( Iterator i = m_temporaryBanList.iterator(); i.hasNext(); )
            {
                Host host = (Host)i.next();
                
                if( host.getAddress().equals(remote) )
                {
                    long timeleft = (host.getReleaseTime() - now) / 1000L;
                    throw new RedirectException( "You have been temporarily banned from modifying this wiki. ("+timeleft+" seconds of ban left)",
                                                 context.getViewURL( m_errorPage ) );
                }
            }
        }
        
    }
    public String preSave( WikiContext context, String content )
        throws RedirectException
    {
        cleanBanList();
        checkBanList( context );
        checkSinglePageChange( context );
        
        WikiPage source = context.getEngine().getPage( m_forbiddenWordsPage );

        if( source != null )
        {
            if( m_spamPatterns == null || m_spamPatterns.isEmpty() || source.getLastModified().after(m_lastRebuild) )
            {
                m_lastRebuild = source.getLastModified();

                m_spamPatterns = parseWordList( source, 
                                                (String)source.getAttribute( LISTVAR ) );

                log.info("Spam filter reloaded - recognizing "+m_spamPatterns.size()+" patterns from page "+m_forbiddenWordsPage);
            }
        }

        //
        //  If we have no spam patterns defined, or we're trying to save
        //  the page containing the patterns, just return.
        //
        if( m_spamPatterns == null || context.getPage().getName().equals( m_forbiddenWordsPage ) )
        {
            return content;
        }

        for( Iterator i = m_spamPatterns.iterator(); i.hasNext(); )
        {
            Pattern p = (Pattern) i.next();

            log.debug("Attempting to match page contents with "+p.getPattern());

            if( m_matcher.contains( content, p ) )
            {
                //
                //  Spam filter has a match.
                //

                throw new RedirectException( "Content matches the spam filter '"+p.getPattern()+"'", 
                                             context.getURL(WikiContext.VIEW,m_errorPage) );
            }
        }

        return content;
    }
    
    /**
     *  A local class for storing host information.
     * 
     *  @author jalkanen
     *
     *  @since
     */
    public class Host
    {
        private  long m_addedTime = System.currentTimeMillis();
        private  long m_releaseTime;
        private  String m_address;
        
        public String getAddress()
        {
            return m_address;
        }
        
        public long getReleaseTime()
        {
            return m_releaseTime;
        }
        
        public long getAddedTime()
        {
            return m_addedTime;
        }
        
        public Host( String ipaddress )
        {
            m_address = ipaddress;
            
            m_releaseTime = System.currentTimeMillis() + m_banTime * 60 * 1000L;
        }
    }
}
