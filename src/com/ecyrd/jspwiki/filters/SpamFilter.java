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

import java.io.*;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

import net.sf.akismet.Akismet;

import org.apache.commons.jrcs.diff.*;
import org.apache.commons.jrcs.diff.myers.MyersDiff;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.apache.oro.text.regex.*;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *  This is Herb, the JSPWiki spamfilter that can also do choke modifications.
 *
 *  Parameters:
 *  <ul>
 *    <li>wordlist - Page name where the regexps are found.  Use [{SET spamwords='regexp list separated with spaces'}] on
 *     that page.  Default is "SpamFilterWordList".
 *    <li>blacklist - The name of an attachment containing the list of spam patterns, one per line. Default is
 *        "SpamFilterWordList/blacklist.txt"</li>
 *    <li>errorpage - The page to which the user is redirected.  Has a special variable $msg which states the reason. Default is "RejectedMessage".
 *    <li>pagechangesinminute - How many page changes are allowed/minute.  Default is 5.</li>
 *    <li>similarchanges - How many similar page changes are allowed before the host is banned.  Default is 2.  (since 2.4.72)</li>
 *    <li>bantime - How long an IP address stays on the temporary ban list (default is 60 for 60 minutes).</li>
 *    <li>maxurls - How many URLs can be added to the page before it is considered spam (default is 5)</li>
 *  </ul>
 *  
 *  <p>Changes by admin users are ignored.</p>
 *  
 *  @since 2.1.112
 *  @author Janne Jalkanen
 */
public class SpamFilter
    extends BasicPageFilter
{
    private static final String LISTVAR = "spamwords";
    public static final String  PROP_WORDLIST  = "wordlist";
    public static final String  PROP_ERRORPAGE = "errorpage";
    public static final String  PROP_PAGECHANGES = "pagechangesinminute";
    public static final String  PROP_SIMILARCHANGES = "similarchanges";
    public static final String  PROP_BANTIME   = "bantime";
    public static final String  PROP_BLACKLIST = "blacklist";
    public static final String  PROP_MAXURLS   = "maxurls";
    public static final String  PROP_AKISMET_API_KEY = "akismet-apikey";
    public static final String  PROP_IGNORE_AUTHENTICATED = "ignoreauthenticated";
    
    private String          URL_REGEXP = "(http://|https://|mailto:)([A-Za-z0-9_/\\.\\+\\?\\#\\-\\@=&;]+)";
    
    private String          m_forbiddenWordsPage = "SpamFilterWordList";
    private String          m_errorPage          = "RejectedMessage";
    private String          m_blacklist          = "SpamFilterWordList/blacklist.txt";
    
    private PatternMatcher  m_matcher = new Perl5Matcher();
    private PatternCompiler m_compiler = new Perl5Compiler();

    private Collection      m_spamPatterns = null;

    private Date            m_lastRebuild = new Date( 0L );

    static  Logger          log = Logger.getLogger( SpamFilter.class );

    
    private Vector          m_temporaryBanList = new Vector();
    
    private int             m_banTime = 60; // minutes
    
    private Vector          m_lastModifications = new Vector();
    
    /**
     *  How many times a single IP address can change a page per minute?
     */
    private int             m_limitSinglePageChanges = 5;
    
    /**
     *  How many times can you add the exact same string to a page?
     */
    private int             m_limitSimilarChanges = 2;
    
    /**
     *  How many URLs can be added at maximum.
     */
    private int             m_maxUrls = 10;
    
    private Pattern         m_UrlPattern;
    private Akismet         m_akismet;

    private String          m_akismetAPIKey = null;
    
    /**
     * If set to true, will ignore anyone who is in Authenticated role.
     */
    private boolean         m_ignoreAuthenticated = false;
    
    public void initialize( WikiEngine engine, Properties properties )
    {
        m_forbiddenWordsPage = properties.getProperty( PROP_WORDLIST, 
                                                       m_forbiddenWordsPage );
        m_errorPage = properties.getProperty( PROP_ERRORPAGE, 
                                              m_errorPage );

        m_limitSinglePageChanges = TextUtil.getIntegerProperty( properties,
                                                                PROP_PAGECHANGES,
                                                                m_limitSinglePageChanges );

        m_limitSimilarChanges = TextUtil.getIntegerProperty( properties,
                                                             PROP_SIMILARCHANGES,
                                                             m_limitSimilarChanges );

        m_maxUrls = TextUtil.getIntegerProperty( properties,
                                                 PROP_MAXURLS,
                                                 m_maxUrls );

        m_banTime = TextUtil.getIntegerProperty( properties,
                                                 PROP_BANTIME,
                                                 m_banTime );
    
        m_blacklist = properties.getProperty( PROP_BLACKLIST, m_blacklist );
        
        m_ignoreAuthenticated = TextUtil.getBooleanProperty( properties,
                                                             PROP_IGNORE_AUTHENTICATED,
                                                             m_ignoreAuthenticated );
        try
        {
            m_UrlPattern = m_compiler.compile( URL_REGEXP );
        }
        catch( MalformedPatternException e )
        {
            log.fatal("Internal error: Someone put in a faulty pattern.",e);
            throw new InternalWikiException("Faulty pattern.");
        }

        m_akismetAPIKey = TextUtil.getStringProperty( properties,
                                                      PROP_AKISMET_API_KEY,
                                                      m_akismetAPIKey );
        
        log.info("Spam filter initialized.  Temporary ban time "+m_banTime+
                 " mins, max page changes/minute: "+m_limitSinglePageChanges );
        
        
    }

    /**
     *  Parses a list of patterns and returns a Collection of compiled Pattern
     *  objects.
     *  
     * @param source
     * @param list
     * @return
     */
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

    /**
     *  Takes a MT-Blacklist -formatted blacklist and returns a list of compiled
     *  Pattern objects.
     *  
     *  @param list
     *  @return
     */
    private Collection parseBlacklist( String list )
    {
        ArrayList compiledpatterns = new ArrayList();
        
        if( list != null )
        {
            try
            {
                BufferedReader in = new BufferedReader( new StringReader(list) );
            
                String line;
            
                while( (line = in.readLine()) != null )
                {
                    line = line.trim();
                    if( line.length() == 0 ) continue; // Empty line
                    if( line.startsWith("#") ) continue; // It's a comment
                    
                    int ws = line.indexOf(' ');
                    
                    if( ws == -1 ) ws = line.indexOf('\t');
                    
                    if( ws != -1 ) line = line.substring(0,ws);
                    
                    try
                    {
                        compiledpatterns.add( m_compiler.compile( line ) );
                    }
                    catch( MalformedPatternException e )
                    {
                        log.debug( "Malformed spam filter pattern "+line );
                    }                    
                }
            }
            catch( IOException e )
            {
                log.info("Could not read patterns; returning what I got",e);
            }
        }
        
        return compiledpatterns;
    }
    
    private String getUniqueID()
    {
        StringBuffer sb = new StringBuffer();
        Random rand = new Random();
        
        for( int i = 0; i < 6; i++ )
        {
            char x = (char)('A'+rand.nextInt(26));
            
            sb.append(x);
        }
        
        return sb.toString();
    }
        
    /**
     *  Takes a single page change and performs a load of tests on the content change.
     *  An admin can modify anything.
     *  
     *  @param context
     *  @param content
     *  @throws RedirectException
     */
    private synchronized void checkSinglePageChange( WikiContext context, String content )
        throws RedirectException
    {
        HttpServletRequest req = context.getHttpRequest();

        if( req != null )
        {
            String addr = req.getRemoteAddr();
            int hostCounter = 0;
            int changeCounter = 0;

            String change = getChange( context, content );

            log.debug("Change is "+change);

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
         
                //
                // Check if this IP address has been seen before
                //
                
                if( host.getAddress().equals(addr) )
                {
                    hostCounter++;
                }
                
                //
                //  Check, if this change has been seen before
                //
                
                if( host.getChange() != null && host.getChange().equals(change) )
                {
                    changeCounter++;
                }
            }
            
            //
            //  Now, let's check against the limits.
            //
            if( hostCounter >= m_limitSinglePageChanges )
            {
                Host host = new Host( addr, null );

                
                m_temporaryBanList.add( host );
                
                String uid = getUniqueID();
                
                log.info("SPAM:TooManyModifications ("+uid+"). Added host "+addr+" to temporary ban list for doing too many modifications/minute" );
                throw new RedirectException( "Herb says you look like a spammer, and I trust Herb! (Incident code "+uid+")",
                                             context.getViewURL( m_errorPage ) );
            }
            
            if( changeCounter >= m_limitSimilarChanges )
            {
                Host host = new Host( addr, null );
                
                m_temporaryBanList.add( host );

                String uid = getUniqueID();
                
                log.info("SPAM:SimilarModifications ("+uid+"). Added host "+addr+" to temporary ban list for doing too many similar modifications" );
                throw new RedirectException( "Herb says you look like a spammer, and I trust Herb! (Incident code "+uid+")",
                                             context.getViewURL( m_errorPage ) );                
            }
            
            //
            //  Calculate the number of links in the addition.
            //
            
            String tstChange = change;
            int    urlCounter = 0;
            
            while( m_matcher.contains(tstChange,m_UrlPattern) )
            {
                MatchResult m = m_matcher.getMatch();
                
                tstChange = tstChange.substring( m.endOffset(0) );
                
                urlCounter++;
            }
            
            if( urlCounter > m_maxUrls )
            {
                Host host = new Host( addr, null );
                
                m_temporaryBanList.add( host );
                
                String uid = getUniqueID();

                log.info("SPAM:TooManyUrls ("+uid+"). Added host "+addr+" to temporary ban list for adding too many URLs" );
                throw new RedirectException( "Herb says you look like a spammer, and I trust Herb! (Incident code "+uid+")",
                                             context.getViewURL( m_errorPage ) );                
            }
            
            //
            //  Do Akismet check
            //
            
            checkAkismet( context, change );
            
            m_lastModifications.add( new Host( addr, change ) );
        }
    }

    private boolean ignoreThisUser(WikiContext context)
    {
        if( context.hasAdminPermissions() )
        {
            return true;
        }

        if( m_ignoreAuthenticated && context.getWikiSession().isAuthenticated() )
        {
            return true;
        }
        
        return false;
    }

    /**
     *  Checks against the akismet system.
     *  
     * @param context
     * @param change
     * @throws RedirectException
     */
    private void checkAkismet( WikiContext context, String change )
        throws RedirectException
    {
        if( m_akismetAPIKey != null )
        {
            if( m_akismet == null )
            {
                log.info("Initializing Akismet spam protection.");
                
                m_akismet = new Akismet( m_akismetAPIKey, context.getEngine().getBaseURL() );

                if( !m_akismet.verifyAPIKey() )
                {
                    log.error("Akismet API key cannot be verified.  Please check your config.");
                    m_akismetAPIKey = null;
                    m_akismet = null;
                }
            }
            
            HttpServletRequest req = context.getHttpRequest();
                
            if( req != null && m_akismet != null )
            {
                log.debug("Calling Akismet to check for spam...");
                
                StopWatch sw = new StopWatch();
                sw.start();
                
                String ipAddress     = req.getRemoteAddr();
                String userAgent     = req.getHeader("User-Agent");
                String referrer      = req.getHeader( "Referer");
                String permalink     = context.getViewURL( context.getPage().getName() );
                String commentType   = (context.getRequestContext().equals(WikiContext.COMMENT) ? "comment" : "edit" );
                String commentAuthor = context.getCurrentUser().getName();
                String commentAuthorEmail = null;
                String commentAuthorURL   = null;
                    
                boolean isSpam = m_akismet.commentCheck( ipAddress,
                                                         userAgent,
                                                         referrer,
                                                         permalink,
                                                         commentType,
                                                         commentAuthor,
                                                         commentAuthorEmail,
                                                         commentAuthorURL,
                                                         change,
                                                         null );
                
                sw.stop();
                
                log.debug("Akismet request done in: "+sw);
                
                if( isSpam )
                {
                    Host host = new Host( ipAddress, null );
                    
                    m_temporaryBanList.add( host );

                    String uid = getUniqueID();

                    log.info("SPAM:Akismet ("+uid+"). Akismet thinks this change is spam; added host to temporary ban list.");
                    
                    throw new RedirectException("Akismet tells Herb you're a spammer, Herb trusts Akismet, and I trust Herb! (Incident code "+uid+")",
                                                context.getViewURL( m_errorPage ) );                
                }
            }
        }
    }
    
    /**
     *  Goes through the ban list and cleans away any host which has expired from it.
     */
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
    
    /**
     *  Checks the ban list if the IP address of the changer is already on it.
     *  
     *  @param context
     *  @throws RedirectException
     */
    
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
    
    /**
     *  If the spam filter notices changes in the black list page, it will refresh
     *  them automatically.
     *  
     *  @param context
     */
    private void refreshBlacklists( WikiContext context )
    {
        try
        {
            WikiPage source = context.getEngine().getPage( m_forbiddenWordsPage );
            Attachment att = context.getEngine().getAttachmentManager().getAttachmentInfo( context, m_blacklist );
        
            boolean rebuild = false;
        
            //
            //  Rebuild, if the page or the attachment has changed since.
            //
            if( source != null )
            {
                if( m_spamPatterns == null || m_spamPatterns.isEmpty() || source.getLastModified().after(m_lastRebuild) )
                {
                    rebuild = true;
                }
            }

            if( att != null )
            {
                if( m_spamPatterns == null || m_spamPatterns.isEmpty() || att.getLastModified().after(m_lastRebuild) )
                {
                    rebuild = true;
                }
            }
  
            
            //
            //  Do the actual rebuilding.  For simplicity's sake, we always rebuild the complete
            //  filter list regardless of what changed.
            //
            
            if( rebuild )
            {
                m_lastRebuild = new Date();

                m_spamPatterns = parseWordList( source, 
                                                (String)source.getAttribute( LISTVAR ) );

                log.info("Spam filter reloaded - recognizing "+m_spamPatterns.size()+" patterns from page "+m_forbiddenWordsPage);
            
                if( att != null )
                {
                    InputStream in = context.getEngine().getAttachmentManager().getAttachmentStream(att);
            
                    StringWriter out = new StringWriter();
            
                    FileUtil.copyContents( new InputStreamReader(in,"UTF-8"), out );
            
                    Collection blackList = parseBlacklist( out.toString() );

                    log.info("...recognizing additional "+blackList.size()+" patterns from blacklist "+m_blacklist);
            
                    m_spamPatterns.addAll( blackList );
                }
            }
        }
        catch( IOException ex )
        {
            log.info("Unable to read attachment data, continuing...",ex);
        }
        catch( ProviderException ex )
        {
            log.info("Failed to read spam filter attachment, continuing...",ex);
        }

    }
    
    public String preSave( WikiContext context, String content )
        throws RedirectException
    {
        cleanBanList();
        refreshBlacklists(context);        
        
        if(!ignoreThisUser(context))
        {
            checkBanList( context );
            checkSinglePageChange( context, content );
            checkPatternList(context, content);
        }
        
        return content;
    }

    private void checkPatternList(WikiContext context, String content) throws RedirectException
    {
        String changeNote = (String)context.getPage().getAttribute( WikiPage.CHANGENOTE );
        
        //
        //  If we have no spam patterns defined, or we're trying to save
        //  the page containing the patterns, just return.
        //
        if( m_spamPatterns == null || context.getPage().getName().equals( m_forbiddenWordsPage ) )
        {
            return;
        }

        for( Iterator i = m_spamPatterns.iterator(); i.hasNext(); )
        {
            Pattern p = (Pattern) i.next();

            // log.debug("Attempting to match page contents with "+p.getPattern());

            if( m_matcher.contains( content, p ) )
            {
                //
                //  Spam filter has a match.
                //
                String uid = getUniqueID();

                log.info("SPAM:Regexp ("+uid+"). Content matches the spam filter '"+p.getPattern()+"'");
                
                throw new RedirectException( "Herb says '"+p.getPattern()+"' is a bad spam word and I trust Herb! (Incident code "+uid+")", 
                                             context.getURL(WikiContext.VIEW,m_errorPage) );
            }
            
            if( changeNote != null && m_matcher.contains( changeNote, p ) )
            {
                String uid = getUniqueID();

                log.info("SPAM:Regexp ("+uid+"). Content matches the spam filter '"+p.getPattern()+"'");

                throw new RedirectException( "Herb says '"+p.getPattern()+"' is a bad spam word and I trust Herb! (Incident code "+uid+")", 
                                             context.getURL(WikiContext.VIEW,m_errorPage) );                
            }
        }
    }
    
    /**
     *  Creates a simple text string describing the added content.
     *  
     *  @param context
     *  @param newText
     *  @return Empty string, if there is no change.
     */
    private String getChange( WikiContext context, String newText )
    {
        WikiPage page = context.getPage();
        StringBuffer change = new StringBuffer();
        WikiEngine engine = context.getEngine();
        // Get current page version
        
        try
        {
            String oldText = engine.getPureText(page.getName(), WikiProvider.LATEST_VERSION);
        
            String[] first  = Diff.stringToArray(oldText);
            String[] second = Diff.stringToArray(newText);
            Revision rev = Diff.diff(first, second, new MyersDiff());

            if( rev == null || rev.size() == 0 )
            {
                return "";
            }

            
            for( int i = 0; i < rev.size(); i++ )
            {
                Delta d = rev.getDelta(i);
                
                if( d instanceof AddDelta )
                {
                    change.append( d.getRevised().toString() );
                }
            }
        }
        catch (DifferentiationFailedException e)
        {
            log.error( "Diff failed", e );
        }

        //
        //  Don't forget to include the change note, too
        //
        String changeNote = (String)page.getAttribute(WikiPage.CHANGENOTE);
        
        if( changeNote != null )
        {
            change.append("\r\n");
            change.append(changeNote);
        }
        
        return change.toString();
    }
    
    /**
     *  A local class for storing host information.
     * 
     *  @author jalkanen
     *
     *  @since
     */
    private class Host
    {
        private  long m_addedTime = System.currentTimeMillis();
        private  long m_releaseTime;
        private  String m_address;
        private  String m_change;
        
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
        
        public String getChange()
        {
            return m_change;
        }
        
        public Host( String ipaddress, String change )
        {
            m_address = ipaddress;
            m_change  = change;
            
            m_releaseTime = System.currentTimeMillis() + m_banTime * 60 * 1000L;
        }
    }
}
