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
package org.apache.wiki.filters;

import net.sf.akismet.Akismet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.wiki.InternalWikiException;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.exceptions.RedirectException;
import org.apache.wiki.api.filters.BasePageFilter;
import org.apache.wiki.api.providers.WikiProvider;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.ui.EditorManager;
import org.apache.wiki.util.FileUtil;
import org.apache.wiki.util.HttpUtil;
import org.apache.wiki.util.TextUtil;
import org.suigeneris.jrcs.diff.Diff;
import org.suigeneris.jrcs.diff.DifferentiationFailedException;
import org.suigeneris.jrcs.diff.Revision;
import org.suigeneris.jrcs.diff.delta.AddDelta;
import org.suigeneris.jrcs.diff.delta.ChangeDelta;
import org.suigeneris.jrcs.diff.delta.DeleteDelta;
import org.suigeneris.jrcs.diff.delta.Delta;
import org.suigeneris.jrcs.diff.myers.MyersDiff;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.ThreadLocalRandom;


/**
 *  This is Herb, the JSPWiki spamfilter that can also do choke modifications.
 *
 *  Parameters:
 *  <ul>
 *    <li>wordlist - Page name where the spamword regexps are found.  Use [{SET spamwords='regexp list separated with spaces'}] on
 *     that page.  Default is "SpamFilterWordList".
 *    <li>IPlist - Page name where the IP regexps are found.  Use [{SET ips='regexp list separated with spaces'}] on
 *     that page.  Default is "SpamFilterIPList".
 *    <li>maxpagenamelength - Maximum page name length. Default is 100.
 *    <li>blacklist - The name of an attachment containing the list of spam patterns, one per line. Default is
 *        "SpamFilterWordList/blacklist.txt"</li>
 *    <li>errorpage - The page to which the user is redirected.  Has a special variable $msg which states the reason. Default is "RejectedMessage".
 *    <li>pagechangesinminute - How many page changes are allowed/minute.  Default is 5.</li>
 *    <li>similarchanges - How many similar page changes are allowed before the host is banned.  Default is 2.  (since 2.4.72)</li>
 *    <li>bantime - How long an IP address stays on the temporary ban list (default is 60 for 60 minutes).</li>
 *    <li>maxurls - How many URLs can be added to the page before it is considered spam (default is 5)</li>
 *    <li>akismet-apikey - The Akismet API key (see akismet.org)</li>
 *    <li>ignoreauthenticated - If set to "true", all authenticated users are ignored and never caught in SpamFilter</li>
 *    <li>captcha - Sets the captcha technology to use.  Current allowed values are "none" and "asirra".</li>
 *    <li>strategy - Sets the filtering strategy to use.  If set to "eager", will stop at the first probable
 *        match, and won't consider any other tests.  This is the default, as it's considerably lighter. If set to "score", will go through all of the tests
 *        and calculates a score for the spam, which is then compared to a filter level value.
 *  </ul>
 *
 *  <p>Please see the default editors/plain.jsp for examples on how the SpamFilter integrates
 *  with the editor system.</p>
 *  
 *  <p>Changes by admin users are ignored in any case.</p>
 *
 *  @since 2.1.112
 */
public class SpamFilter extends BasePageFilter {
	
    private static final String ATTR_SPAMFILTER_SCORE = "spamfilter.score";
    private static final String REASON_REGEXP = "Regexp";
    private static final String REASON_IP_BANNED_TEMPORARILY = "IPBannedTemporarily";
    private static final String REASON_IP_BANNED_PERMANENTLY = "IPBannedPermanently";
    private static final String REASON_BOT_TRAP = "BotTrap";
    private static final String REASON_AKISMET = "Akismet";
    private static final String REASON_TOO_MANY_URLS = "TooManyUrls";
    private static final String REASON_SIMILAR_MODIFICATIONS = "SimilarModifications";
    private static final String REASON_TOO_MANY_MODIFICATIONS = "TooManyModifications";
    private static final String REASON_PAGENAME_TOO_LONG = "PageNameTooLong";
    private static final String REASON_UTF8_TRAP = "UTF8Trap";

    private static final String LISTVAR = "spamwords";
    private static final String LISTIPVAR = "ips";

    private static final Random RANDOM = ThreadLocalRandom.current();

    /** The filter property name for specifying the page which contains the list of spamwords. Value is <tt>{@value}</tt>. */
    public static final String  PROP_WORDLIST              = "wordlist";

    /** The filter property name for specifying the page which contains the list of IPs to ban. Value is <tt>{@value}</tt>. */
    public static final String  PROP_IPLIST                = "IPlist";

    /** The filter property name for specifying the maximum page name length.  Value is <tt>{@value}</tt>. */
    public static final String  PROP_MAX_PAGENAME_LENGTH   = "maxpagenamelength";

    /** The filter property name for the page to which you are directed if Herb rejects your edit.  Value is <tt>{@value}</tt>. */
    public static final String  PROP_ERRORPAGE             = "errorpage";
    
    /** The filter property name for specifying how many changes is any given IP address
     *  allowed to do per minute.  Value is <tt>{@value}</tt>.
     */
    public static final String  PROP_PAGECHANGES           = "pagechangesinminute";
    
    /** The filter property name for specifying how many similar changes are allowed before a host is banned.  Value is <tt>{@value}</tt>. */
    public static final String  PROP_SIMILARCHANGES        = "similarchanges";
    
    /** The filter property name for specifying how long a host is banned.  Value is <tt>{@value}</tt>.*/
    public static final String  PROP_BANTIME               = "bantime";
    
    /** The filter property name for the attachment containing the blacklist.  Value is <tt>{@value}</tt>.*/
    public static final String  PROP_BLACKLIST             = "blacklist";
    
    /** The filter property name for specifying how many URLs can any given edit contain. Value is <tt>{@value}</tt> */
    public static final String  PROP_MAXURLS               = "maxurls";
    
    /** The filter property name for specifying the Akismet API-key.  Value is <tt>{@value}</tt>. */
    public static final String  PROP_AKISMET_API_KEY       = "akismet-apikey";
    
    /** The filter property name for specifying whether authenticated users should be ignored. Value is <tt>{@value}</tt>. */
    public static final String  PROP_IGNORE_AUTHENTICATED  = "ignoreauthenticated";

    /** The filter property name for specifying groups allowed to bypass the spam filter. Value is <tt>{@value}</tt>. */
    public static final String PROP_ALLOWED_GROUPS = "jspwiki.filters.spamfilter.allowedgroups";
    
    /** The filter property name for specifying which captcha technology should be used. Value is <tt>{@value}</tt>. */
    public static final String  PROP_CAPTCHA               = "captcha";
    
    /** The filter property name for specifying which filter strategy should be used.  Value is <tt>{@value}</tt>. */
    public static final String  PROP_FILTERSTRATEGY        = "strategy";

    /** The string specifying the "eager" strategy. Value is <tt>{@value}</tt>. */
    public static final String  STRATEGY_EAGER             = "eager";
    
    /** The string specifying the "score" strategy. Value is <tt>{@value}</tt>. */
    public static final String  STRATEGY_SCORE             = "score";

    private static final String URL_REGEXP = "(http://|https://|mailto:)([A-Za-z0-9_/\\.\\+\\?\\#\\-\\@=&;]+)";

    private String          m_forbiddenWordsPage = "SpamFilterWordList";
    private String          m_forbiddenIPsPage   = "SpamFilterIPList";
    private String          m_pageNameMaxLength  = "100";
    private String          m_errorPage          = "RejectedMessage";
    private String          m_blacklist          = "SpamFilterWordList/blacklist.txt";

    private final PatternMatcher  m_matcher = new Perl5Matcher();
    private final PatternCompiler m_compiler = new Perl5Compiler();

    private Collection<Pattern> m_spamPatterns;
    private Collection<Pattern> m_IPPatterns;

    private Date m_lastRebuild = new Date( 0L );

    private static final Logger c_spamlog = LogManager.getLogger( "SpamLog" );
    private static final Logger log = LogManager.getLogger( SpamFilter.class );

    private final Vector<Host>    m_temporaryBanList = new Vector<>();

    private int             m_banTime = 60; // minutes

    private final Vector<Host>    m_lastModifications = new Vector<>();

    /** How many times a single IP address can change a page per minute? */
    private int             m_limitSinglePageChanges = 5;

    /** How many times can you add the exact same string to a page? */
    private int             m_limitSimilarChanges = 2;

    /** How many URLs can be added at maximum. */
    private int             m_maxUrls = 10;

    private Pattern         m_urlPattern;
    private Akismet         m_akismet;

    private String          m_akismetAPIKey;

    private boolean         m_useCaptcha;

    /** The limit at which we consider something to be spam. */
    private final int             m_scoreLimit = 1;

    /** If set to true, will ignore anyone who is in Authenticated role. */
    private boolean         m_ignoreAuthenticated;

    /** Groups allowed to bypass the filter */
    private String[]         m_allowedGroups;

    private boolean         m_stopAtFirstMatch = true;

    private static String   c_hashName;
    private static long     c_lastUpdate;

    /** The HASH_DELAY value is a maximum amount of time that an user can keep
     *  a session open, because after the value has expired, we will invent a new
     *  hash field name.  By default this is {@value} hours, which should be ample
     *  time for someone.
     */
    private static final long HASH_DELAY = 24;


    /**
     *  {@inheritDoc}
     */
    @Override
    public void initialize( final Engine engine, final Properties properties ) {
        m_forbiddenWordsPage = properties.getProperty( PROP_WORDLIST, m_forbiddenWordsPage );
        m_forbiddenIPsPage = properties.getProperty( PROP_IPLIST, m_forbiddenIPsPage);
        m_pageNameMaxLength = properties.getProperty( PROP_MAX_PAGENAME_LENGTH, m_pageNameMaxLength);
        m_errorPage = properties.getProperty( PROP_ERRORPAGE, m_errorPage );
        m_limitSinglePageChanges = TextUtil.getIntegerProperty( properties, PROP_PAGECHANGES, m_limitSinglePageChanges );
        
        m_limitSimilarChanges = TextUtil.getIntegerProperty( properties, PROP_SIMILARCHANGES, m_limitSimilarChanges );

        m_maxUrls = TextUtil.getIntegerProperty( properties, PROP_MAXURLS, m_maxUrls );
        m_banTime = TextUtil.getIntegerProperty( properties, PROP_BANTIME, m_banTime );
        m_blacklist = properties.getProperty( PROP_BLACKLIST, m_blacklist );

        m_ignoreAuthenticated = TextUtil.getBooleanProperty( properties, PROP_IGNORE_AUTHENTICATED, m_ignoreAuthenticated );
        m_allowedGroups = StringUtils.split( StringUtils.defaultString( properties.getProperty( PROP_ALLOWED_GROUPS, m_blacklist ) ), ',' );

        m_useCaptcha = properties.getProperty( PROP_CAPTCHA, "" ).equals("asirra");

        try {
            m_urlPattern = m_compiler.compile( URL_REGEXP );
        } catch( final MalformedPatternException e ) {
            log.fatal( "Internal error: Someone put in a faulty pattern.", e );
            throw new InternalWikiException( "Faulty pattern." , e);
        }

        m_akismetAPIKey = TextUtil.getStringProperty( properties, PROP_AKISMET_API_KEY, m_akismetAPIKey );
        m_stopAtFirstMatch = TextUtil.getStringProperty( properties, PROP_FILTERSTRATEGY, STRATEGY_EAGER ).equals( STRATEGY_EAGER );

        log.info( "# Spam filter initialized.  Temporary ban time " + m_banTime +
                  " mins, max page changes/minute: " + m_limitSinglePageChanges );
    }

    private static final int REJECT = 0;
    private static final int ACCEPT = 1;
    private static final int NOTE   = 2;

    private static String log( final Context ctx, final int type, final String source, String message ) {
        message = TextUtil.replaceString( message, "\r\n", "\\r\\n" );
        message = TextUtil.replaceString( message, "\"", "\\\"" );

        final String uid = getUniqueID();
        final String page   = ctx.getPage().getName();
        final String addr   = ctx.getHttpRequest() != null ? HttpUtil.getRemoteAddress( ctx.getHttpRequest() ) : "-";
        final String reason;
        switch( type ) {
            case REJECT: reason = "REJECTED";
                break;
            case ACCEPT: reason = "ACCEPTED";
                break;
            case NOTE: reason = "NOTE";
                break;
            default: throw new InternalWikiException( "Illegal type " + type );
        }
        c_spamlog.info( reason + " " + source + " " + uid + " " + addr + " \"" + page + "\" " + message );

        return uid;
    }

    /** {@inheritDoc} */
    @Override
    public String preSave( final Context context, final String content ) throws RedirectException {
        cleanBanList();
        refreshBlacklists( context );
        final Change change = getChange( context, content );

        if( !ignoreThisUser( context ) ) {
            checkBanList( context, change );
            checkSinglePageChange( context, content, change );
            checkIPList( context );
            checkPatternList( context, content, change );
            checkPageName( context, content, change);
        }

        if( !m_stopAtFirstMatch ) {
            final Integer score = context.getVariable( ATTR_SPAMFILTER_SCORE );

            if( score != null && score >= m_scoreLimit ) {
                throw new RedirectException( "Herb says you got too many points", getRedirectPage( context ) );
            }
        }

        log( context, ACCEPT, "-", change.toString() );
        return content;
    }

    private void checkPageName( final Context context, final String content, final Change change ) throws RedirectException {
        final Page page = context.getPage();
        final String pageName = page.getName();
        final int maxlength = Integer.parseInt(m_pageNameMaxLength);
        if ( pageName.length() > maxlength) {
            //
            //  Spam filter has a match.
            //

            final String uid = log( context, REJECT, REASON_PAGENAME_TOO_LONG + "(" + m_pageNameMaxLength + ")" , pageName);

            log.info("SPAM:PageNameTooLong (" + uid + "). The length of the page name is too large (" + pageName.length() + " , limit is " + m_pageNameMaxLength + ")");
            checkStrategy( context, REASON_PAGENAME_TOO_LONG, "Herb says '" + pageName + "' is a bad pageName and I trust Herb! (Incident code " + uid + ")" );

        }
    }

    private void checkStrategy( final Context context, final String error, final String message ) throws RedirectException {
        if( m_stopAtFirstMatch ) {
            throw new RedirectException( message, getRedirectPage( context ) );
        }

        Integer score = context.getVariable( ATTR_SPAMFILTER_SCORE );
        if( score != null ) {
            score = score + 1;
        } else {
            score = 1;
        }

        context.setVariable( ATTR_SPAMFILTER_SCORE, score );
    }
    
    /**
     *  Parses a list of patterns and returns a Collection of compiled Pattern objects.
     *
     * @param source page containing the list of patterns.
     * @param list list of patterns.
     * @return A Collection of the Patterns that were found from the lists.
     */
    private Collection< Pattern > parseWordList( final Page source, final String list ) {
        final ArrayList< Pattern > compiledpatterns = new ArrayList<>();

        if( list != null ) {
            final StringTokenizer tok = new StringTokenizer( list, " \t\n" );

            while( tok.hasMoreTokens() ) {
                final String pattern = tok.nextToken();

                try {
                    compiledpatterns.add( m_compiler.compile( pattern ) );
                } catch( final MalformedPatternException e ) {
                    log.debug( "Malformed spam filter pattern " + pattern );
                    source.setAttribute("error", "Malformed spam filter pattern " + pattern);
                }
            }
        }

        return compiledpatterns;
    }

    /**
     *  Takes a MT-Blacklist -formatted blacklist and returns a list of compiled Pattern objects.
     *
     *  @param list list of patterns.
     *  @return The parsed blacklist patterns.
     */
    private Collection< Pattern > parseBlacklist( final String list ) {
        final ArrayList< Pattern > compiledpatterns = new ArrayList<>();

        if( list != null ) {
            try {
                final BufferedReader in = new BufferedReader( new StringReader(list) );
                String line;
                while( (line = in.readLine() ) != null ) {
                    line = line.trim();
                    if( line.isEmpty() ) continue; // Empty line
                    if( line.startsWith("#") ) continue; // It's a comment

                    int ws = line.indexOf( ' ' );
                    if( ws == -1 ) ws = line.indexOf( '\t' );
                    if( ws != -1 ) line = line.substring( 0, ws );

                    try {
                        compiledpatterns.add( m_compiler.compile( line ) );
                    } catch( final MalformedPatternException e ) {
                        log.debug( "Malformed spam filter pattern " + line );
                    }
                }
            } catch( final IOException e ) {
                log.info( "Could not read patterns; returning what I got" , e );
            }
        }

        return compiledpatterns;
    }

    /**
     * Takes a single page change and performs a load of tests on the content change. An admin can modify anything.
     *
     * @param context page Context
     * @param content page content
     * @param change page change
     * @throws RedirectException spam filter rejects the page change.
     */
    private synchronized void checkSinglePageChange( final Context context, final String content, final Change change )
    		throws RedirectException {
        final HttpServletRequest req = context.getHttpRequest();

        if( req != null ) {
            final String addr = HttpUtil.getRemoteAddress( req );
            int hostCounter = 0;
            int changeCounter = 0;

            log.debug( "Change is " + change.m_change );

            final long time = System.currentTimeMillis() - 60*1000L; // 1 minute

            for( final Iterator< Host > i = m_lastModifications.iterator(); i.hasNext(); ) {
                final Host host = i.next();

                //  Check if this item is invalid
                if( host.getAddedTime() < time ) {
                    log.debug( "Removed host " + host.getAddress() + " from modification queue (expired)" );
                    i.remove();
                    continue;
                }

                // Check if this IP address has been seen before
                if( host.getAddress().equals( addr ) ) {
                    hostCounter++;
                }

                //  Check, if this change has been seen before
                if( host.getChange() != null && host.getChange().equals( change ) ) {
                    changeCounter++;
                }
            }

            //  Now, let's check against the limits.
            if( hostCounter >= m_limitSinglePageChanges ) {
                final Host host = new Host( addr, null );
                m_temporaryBanList.add( host );

                final String uid = log( context, REJECT, REASON_TOO_MANY_MODIFICATIONS, change.m_change );
                log.info( "SPAM:TooManyModifications (" + uid + "). Added host " + addr + " to temporary ban list for doing too many modifications/minute" );
                checkStrategy( context, REASON_TOO_MANY_MODIFICATIONS, "Herb says you look like a spammer, and I trust Herb! (Incident code " + uid + ")" );
            }

            if( changeCounter >= m_limitSimilarChanges ) {
                final Host host = new Host( addr, null );
                m_temporaryBanList.add( host );

                final String uid = log( context, REJECT, REASON_SIMILAR_MODIFICATIONS, change.m_change );
                log.info( "SPAM:SimilarModifications (" + uid + "). Added host " + addr + " to temporary ban list for doing too many similar modifications" );
                checkStrategy( context, REASON_SIMILAR_MODIFICATIONS, "Herb says you look like a spammer, and I trust Herb! (Incident code "+uid+")");
            }

            //  Calculate the number of links in the addition.
            String tstChange  = change.toString();
            int urlCounter = 0;
            while( m_matcher.contains( tstChange,m_urlPattern ) ) {
                final MatchResult m = m_matcher.getMatch();
                tstChange = tstChange.substring( m.endOffset(0) );
                urlCounter++;
            }

            if( urlCounter > m_maxUrls ) {
                final Host host = new Host( addr, null );
                m_temporaryBanList.add( host );

                final String uid = log( context, REJECT, REASON_TOO_MANY_URLS, change.toString() );
                log.info( "SPAM:TooManyUrls (" + uid + "). Added host " + addr + " to temporary ban list for adding too many URLs" );
                checkStrategy( context, REASON_TOO_MANY_URLS, "Herb says you look like a spammer, and I trust Herb! (Incident code " + uid + ")" );
            }

            //  Check bot trap
            checkBotTrap( context, change );

            //  Check UTF-8 mangling
            checkUTF8( context, change );

            //  Do Akismet check.  This is good to be the last, because this is the most expensive operation.
            checkAkismet( context, change );

            m_lastModifications.add( new Host( addr, change ) );
        }
    }


    /**
     *  Checks against the akismet system.
     *
     * @param context page Context
     * @throws RedirectException spam filter rejects the page change.
     */
    private void checkAkismet( final Context context, final Change change ) throws RedirectException {
        if( m_akismetAPIKey != null ) {
            if( m_akismet == null ) {
                log.info( "Initializing Akismet spam protection." );
                m_akismet = new Akismet( m_akismetAPIKey, context.getEngine().getBaseURL() );

                if( !m_akismet.verifyAPIKey() ) {
                    log.error( "Akismet API key cannot be verified.  Please check your config." );
                    m_akismetAPIKey = null;
                    m_akismet = null;
                }
            }

            final HttpServletRequest req = context.getHttpRequest();

            //  Akismet will mark all empty statements as spam, so we'll just ignore them.
            if( change.m_adds == 0 && change.m_removals > 0 ) {
                return;
            }
            
            if( req != null && m_akismet != null ) {
                log.debug( "Calling Akismet to check for spam..." );

                final StopWatch sw = new StopWatch();
                sw.start();

                final String ipAddress     = HttpUtil.getRemoteAddress( req );
                final String userAgent     = req.getHeader( "User-Agent" );
                final String referrer      = req.getHeader( "Referer");
                final String permalink     = context.getViewURL( context.getPage().getName() );
                final String commentType   = context.getRequestContext().equals( ContextEnum.PAGE_COMMENT.getRequestContext() ) ? "comment" : "edit";
                final String commentAuthor = context.getCurrentUser().getName();
                final String commentAuthorEmail = null;
                final String commentAuthorURL   = null;

                final boolean isSpam = m_akismet.commentCheck( ipAddress,
                                                               userAgent,
                                                               referrer,
                                                               permalink,
                                                               commentType,
                                                               commentAuthor,
                                                               commentAuthorEmail,
                                                               commentAuthorURL,
                                                               change.toString(),
                                                               null );

                sw.stop();
                log.debug( "Akismet request done in: " + sw );

                if( isSpam ) {
                    // Host host = new Host( ipAddress, null );
                    // m_temporaryBanList.add( host );

                    final String uid = log( context, REJECT, REASON_AKISMET, change.toString() );
                    log.info( "SPAM:Akismet (" + uid + "). Akismet thinks this change is spam; added host to temporary ban list." );
                    checkStrategy( context, REASON_AKISMET, "Akismet tells Herb you're a spammer, Herb trusts Akismet, and I trust Herb! (Incident code " + uid + ")" );
                }
            }
        }
    }

    /**
     * Returns a static string which can be used to detect spambots which just wildly fill in all the fields.
     *
     * @return A string
     */
    public static String getBotFieldName() {
        return "submit_auth";
    }

    /**
     * This checks whether an invisible field is available in the request, and whether it's contents are suspected spam.
     *
     * @param context page Context
     * @param change page change
     * @throws RedirectException spam filter rejects the page change.
     */
    private void checkBotTrap( final Context context, final Change change ) throws RedirectException {
        final HttpServletRequest request = context.getHttpRequest();
        if( request != null ) {
            final String unspam = request.getParameter( getBotFieldName() );
            if( unspam != null && !unspam.isEmpty() ) {
                final String uid = log( context, REJECT, REASON_BOT_TRAP, change.toString() );

                log.info( "SPAM:BotTrap (" + uid + ").  Wildly behaving bot detected." );
                checkStrategy( context, REASON_BOT_TRAP, "Spamming attempt detected. (Incident code " + uid + ")" );
            }
        }
    }

    private void checkUTF8( final Context context, final Change change ) throws RedirectException {
        final HttpServletRequest request = context.getHttpRequest();
        if( request != null ) {
            final String utf8field = request.getParameter( "encodingcheck" );
            if( utf8field != null && !utf8field.equals( "\u3041" ) ) {
                final String uid = log( context, REJECT, REASON_UTF8_TRAP, change.toString() );

                log.info( "SPAM:UTF8Trap (" + uid + ").  Wildly posting dumb bot detected." );
                checkStrategy( context, REASON_UTF8_TRAP, "Spamming attempt detected. (Incident code " + uid + ")" );
            }
        }
    }

    /** Goes through the ban list and cleans away any host which has expired from it. */
    private synchronized void cleanBanList() {
        final long now = System.currentTimeMillis();
        for( final Iterator< Host > i = m_temporaryBanList.iterator(); i.hasNext(); ) {
            final Host host = i.next();

            if( host.getReleaseTime() < now ) {
                log.debug( "Removed host " + host.getAddress() + " from temporary ban list (expired)" );
                i.remove();
            }
        }
    }

    /**
     *  Checks the ban list if the IP address of the changer is already on it.
     *
     *  @param context page context
     *  @throws RedirectException spam filter rejects the page change.
     */
    private void checkBanList( final Context context, final Change change ) throws RedirectException {
        final HttpServletRequest req = context.getHttpRequest();

        if( req != null ) {
            final String remote = HttpUtil.getRemoteAddress(req);
            final long now = System.currentTimeMillis();

            for( final Host host : m_temporaryBanList ) {
                if( host.getAddress().equals( remote ) ) {
                    final long timeleft = ( host.getReleaseTime() - now ) / 1000L;

                    log( context, REJECT, REASON_IP_BANNED_TEMPORARILY, change.m_change );
                    checkStrategy( context, REASON_IP_BANNED_TEMPORARILY,
                            "You have been temporarily banned from modifying this wiki. (" + timeleft + " seconds of ban left)" );
                }
            }
        }
    }

    /**
     *  If the spam filter notices changes in the black list page, it will refresh them automatically.
     *
     *  @param context associated WikiContext
     */
    private void refreshBlacklists( final Context context ) {
        try {
            boolean rebuild = false;

            //  Rebuild, if the spam words page, the attachment or the IP ban page has changed since.
            final Page sourceSpam = context.getEngine().getManager( PageManager.class ).getPage( m_forbiddenWordsPage );
            if( sourceSpam != null ) {
                if( m_spamPatterns == null || m_spamPatterns.isEmpty() || sourceSpam.getLastModified().after( m_lastRebuild ) ) {
                    rebuild = true;
                }
            }

            final Attachment att = context.getEngine().getManager( AttachmentManager.class ).getAttachmentInfo( context, m_blacklist );
            if( att != null ) {
                if( m_spamPatterns == null || m_spamPatterns.isEmpty() || att.getLastModified().after( m_lastRebuild ) ) {
                    rebuild = true;
                }
            }

            final Page sourceIPs = context.getEngine().getManager( PageManager.class ).getPage( m_forbiddenIPsPage );
            if( sourceIPs != null ) {
                if( m_IPPatterns == null || m_IPPatterns.isEmpty() || sourceIPs.getLastModified().after( m_lastRebuild ) ) {
                    rebuild = true;
                }
            }

            //  Do the actual rebuilding.  For simplicity's sake, we always rebuild the complete filter list regardless of what changed.
            if( rebuild ) {
                m_lastRebuild = new Date();
                m_spamPatterns = parseWordList( sourceSpam, ( sourceSpam != null ) ? sourceSpam.getAttribute( LISTVAR ) : null );

                log.info( "Spam filter reloaded - recognizing " + m_spamPatterns.size() + " patterns from page " + m_forbiddenWordsPage );

                m_IPPatterns = parseWordList( sourceIPs,  ( sourceIPs != null ) ? sourceIPs.getAttribute( LISTIPVAR ) : null );
                log.info( "IP filter reloaded - recognizing " + m_IPPatterns.size() + " patterns from page " + m_forbiddenIPsPage );

                if( att != null ) {
                    final InputStream in = context.getEngine().getManager( AttachmentManager.class ).getAttachmentStream(att);
                    final StringWriter out = new StringWriter();
                    FileUtil.copyContents( new InputStreamReader( in, StandardCharsets.UTF_8 ), out );
                    final Collection< Pattern > blackList = parseBlacklist( out.toString() );
                    log.info( "...recognizing additional " + blackList.size() + " patterns from blacklist " + m_blacklist );
                    m_spamPatterns.addAll( blackList );
                }
            }
        } catch( final IOException ex ) {
            log.info( "Unable to read attachment data, continuing...", ex );
        } catch( final ProviderException ex ) {
            log.info( "Failed to read spam filter attachment, continuing...", ex );
        }
    }

    /**
     * Does a check against a known pattern list.
     *
     * @param context page Context
     * @param content page content
     * @param change page change
     * @throws RedirectException spam filter rejects the page change.
     */
    private void checkPatternList( final Context context, final String content, final Change change ) throws RedirectException {
        // If we have no spam patterns defined, or we're trying to save the page containing the patterns, just return.
        if( m_spamPatterns == null || context.getPage().getName().equals( m_forbiddenWordsPage ) ) {
            return;
        }

        String ch = change.toString();
        if( context.getHttpRequest() != null ) {
            ch += HttpUtil.getRemoteAddress( context.getHttpRequest() );
        }

        for( final Pattern p : m_spamPatterns ) {
            // log.debug("Attempting to match page contents with "+p.getPattern());

            if( m_matcher.contains( ch, p ) ) {
                //  Spam filter has a match.
                final String uid = log( context, REJECT, REASON_REGEXP + "(" + p.getPattern() + ")", ch );

                log.info( "SPAM:Regexp (" + uid + "). Content matches the spam filter '" + p.getPattern() + "'" );
                checkStrategy( context, REASON_REGEXP, "Herb says '" + p.getPattern() + "' is a bad spam word and I trust Herb! (Incident code " + uid + ")" );
            }
        }
    }


    /**
     *  Does a check against a pattern list of IPs.
     *
     *  @param context page context
     *  @throws RedirectException spam filter rejects the page change.
     */
    private void checkIPList( final Context context ) throws RedirectException {
        //  If we have no IP patterns defined, or we're trying to save the page containing the IP patterns, just return.
        if( m_IPPatterns == null || context.getPage().getName().equals( m_forbiddenIPsPage ) ) {
            return;
        }

        final String remoteIP = HttpUtil.getRemoteAddress( context.getHttpRequest() );
        log.info("Attempting to match remoteIP " + remoteIP + " against " + m_IPPatterns.size() + " patterns");

        for( final Pattern p : m_IPPatterns ) {
             log.debug("Attempting to match remoteIP with " + p.getPattern());

            if( m_matcher.contains( remoteIP, p ) ) {

                //  IP filter has a match.
                //
                final String uid = log( context, REJECT, REASON_IP_BANNED_PERMANENTLY + "(" + p.getPattern() + ")", remoteIP );

                log.info( "SPAM:IPBanList (" + uid + "). remoteIP matches the IP filter '" + p.getPattern() + "'" );
                checkStrategy( context, REASON_IP_BANNED_PERMANENTLY, "Herb says '" + p.getPattern() + "' is a banned IP and I trust Herb! (Incident code " + uid + ")" );
            }
        }
    }

    private void checkPatternList( final Context context, final String content, final String change ) throws RedirectException {
        final Change c = new Change();
        c.m_change = change;
        checkPatternList( context, content, c );
    }
 
    /**
     *  Creates a simple text string describing the added content.
     *
     *  @param context page context
     *  @param newText added content
     *  @return Empty string, if there is no change.
     */
    private static Change getChange( final Context context, final String newText ) {
        final Page page = context.getPage();
        final StringBuffer change = new StringBuffer();
        final Engine engine = context.getEngine();
        // Get current page version

        final Change ch = new Change();
        
        try {
            final String oldText = engine.getManager( PageManager.class ).getPureText( page.getName(), WikiProvider.LATEST_VERSION );
            final String[] first  = Diff.stringToArray( oldText );
            final String[] second = Diff.stringToArray( newText );
            final Revision rev = Diff.diff( first, second, new MyersDiff() );

            if( rev == null || rev.size() == 0 ) {
                return ch;
            }
            
            for( int i = 0; i < rev.size(); i++ ) {
                final Delta d = rev.getDelta( i );

                if( d instanceof AddDelta ) {
                    d.getRevised().toString( change, "", "\r\n" );
                    ch.m_adds++;
                    
                } else if( d instanceof ChangeDelta ) {
                    d.getRevised().toString( change, "", "\r\n" );
                    ch.m_adds++;
                    
                } else if( d instanceof DeleteDelta ) {
                    ch.m_removals++;
                }
            }
        } catch( final DifferentiationFailedException e ) {
            log.error( "Diff failed", e );
        }

        //  Don't forget to include the change note, too
        final String changeNote = page.getAttribute( Page.CHANGENOTE );
        if( changeNote != null ) {
            change.append( "\r\n" );
            change.append( changeNote );
        }

        //  And author as well
        if( page.getAuthor() != null ) {
            change.append( "\r\n" ).append( page.getAuthor() );
        }

        ch.m_change = change.toString();
        return ch;
    }

    /**
     * Returns true, if this user should be ignored.  For example, admin users.
     *
     * @param context page context
     * @return True, if this user should be ignored.
     */
    private boolean ignoreThisUser( final Context context ) {
        if( context.hasAdminPermissions() ) {
            return true;
        }

        final List< String > groups = Arrays.asList( m_allowedGroups );
        if( Arrays.stream( context.getWikiSession().getRoles() ).anyMatch( role -> groups.contains( role.getName() ) ) ) {
            return true;
        }

        if( m_ignoreAuthenticated && context.getWikiSession().isAuthenticated() ) {
            return true;
        }

        return context.getVariable("captcha") != null;
    }

    /**
     *  Returns a random string of six uppercase characters.
     *
     *  @return A random string
     */
    private static String getUniqueID() {
        final StringBuilder sb = new StringBuilder();
        for( int i = 0; i < 6; i++ ) {
            final char x = ( char )( 'A' + RANDOM.nextInt( 26 ) );
            sb.append( x );
        }

        return sb.toString();
    }

    /**
     *  Returns a page to which we shall redirect, based on the current value of the "captcha" parameter.
     *
     *  @param ctx WikiContext
     *  @return An URL to redirect to
     */
    private String getRedirectPage( final Context ctx ) {
        if( m_useCaptcha ) {
            return ctx.getURL( ContextEnum.PAGE_NONE.getRequestContext(), "Captcha.jsp", "page= " +ctx.getEngine().encodeName( ctx.getPage().getName() ) );
        }

        return ctx.getURL( ContextEnum.PAGE_VIEW.getRequestContext(), m_errorPage );
    }

    /**
     *  Checks whether the UserProfile matches certain checks.
     *
     *  @param profile The profile to check
     *  @param context The WikiContext
     *  @return False, if this userprofile is suspect and should not be allowed to be added.
     *  @since 2.6.1
     */
    public boolean isValidUserProfile( final Context context, final UserProfile profile ) {
        try {
            checkPatternList( context, profile.getEmail(), profile.getEmail() );
            checkPatternList( context, profile.getFullname(), profile.getFullname() );
            checkPatternList( context, profile.getLoginName(), profile.getLoginName() );
        } catch( final RedirectException e ) {
            log.info("Detected attempt to create a spammer user account (see above for rejection reason)");
            return false;
        }

        return true;
    }

    /**
     *  This method is used to calculate an unique code when submitting the page to detect edit conflicts.  
     *  It currently incorporates the last-modified date of the page, and the IP address of the submitter.
     *
     *  @param page The WikiPage under edit
     *  @param request The HTTP Request
     *  @since 2.6
     *  @return A hash value for this page and session
     */
    public static String getSpamHash( final Page page, final HttpServletRequest request ) {
        long lastModified = 0;

        if( page.getLastModified() != null ) {
            lastModified = page.getLastModified().getTime();
        }
        final long remote = HttpUtil.getRemoteAddress( request ).hashCode();

        return Long.toString( lastModified ^ remote );
    }

    /**
     *  Returns the name of the hash field to be used in this request. The value is unique per session, and once 
     *  the session has expired, you cannot edit anymore.
     *
     *  @param request The page request
     *  @return The name to be used in the hash field
     *  @since  2.6
     */
    public static String getHashFieldName( final HttpServletRequest request ) {
        String hash = null;

        if( request.getSession() != null ) {
            hash = ( String )request.getSession().getAttribute( "_hash" );

            if( hash == null ) {
                hash = c_hashName;
                request.getSession().setAttribute( "_hash", hash );
            }
        }

        if( c_hashName == null || c_lastUpdate < ( System.currentTimeMillis() - HASH_DELAY * 60 * 60 * 1000 ) ) {
            c_hashName = getUniqueID().toLowerCase();
            c_lastUpdate = System.currentTimeMillis();
        }

        return hash != null ? hash : c_hashName;
    }


    /**
     *  This method checks if the hash value is still valid, i.e. if it exists at all. This can occur in two cases: 
     *  either this is a spam bot which is not adaptive, or it is someone who has been editing one page for too long, 
     *  and their session has expired.
     *  <p>
     *  This method puts a redirect to the http response field to page "SessionExpired" and logs the incident in 
     *  the spam log (it may or may not be spam, but it's rather likely that it is).
     *
     *  @param context The WikiContext
     *  @param pageContext The JSP PageContext.
     *  @return True, if hash is okay.  False, if hash is not okay, and you need to redirect.
     *  @throws IOException If redirection fails
     *  @since 2.6
     */
    public static boolean checkHash( final Context context, final PageContext pageContext ) throws IOException {
        final String hashName = getHashFieldName( (HttpServletRequest)pageContext.getRequest() );
        if( pageContext.getRequest().getParameter(hashName) == null ) {
            if( pageContext.getAttribute( hashName ) == null ) {
                final Change change = getChange( context, EditorManager.getEditedText( pageContext ) );
                log( context, REJECT, "MissingHash", change.m_change );

                final String redirect = context.getURL( ContextEnum.PAGE_VIEW.getRequestContext(),"SessionExpired" );
                ( ( HttpServletResponse )pageContext.getResponse() ).sendRedirect( redirect );
                return false;
            }
        }

        return true;
    }

    /**
     * This helper method adds all the input fields to your editor that the SpamFilter requires
     * to check for spam.  This <i>must</i> be in your editor form if you intend to use the SpamFilter.
     *  
     * @param pageContext The PageContext
     * @return A HTML string which contains input fields for the SpamFilter.
     */
    public static String insertInputFields( final PageContext pageContext ) {
        final Context ctx = Context.findContext( pageContext );
        final Engine engine = ctx.getEngine();
        final StringBuilder sb = new StringBuilder();
        if( engine.getContentEncoding().equals( StandardCharsets.UTF_8 ) ) {
            sb.append( "<input name='encodingcheck' type='hidden' value='\u3041' />\n" );
        }

        return sb.toString();
    }
    
    /**
     *  A local class for storing host information.
     */
    private class Host {

        private final long m_addedTime = System.currentTimeMillis();
        private final long m_releaseTime;
        private final String m_address;
        private final Change m_change;

        public String getAddress() {
            return m_address;
        }

        public long getReleaseTime() {
            return m_releaseTime;
        }

        public long getAddedTime() {
            return m_addedTime;
        }

        public Change getChange() {
            return m_change;
        }

        public Host( final String ipaddress, final Change change ) {
            m_address = ipaddress;
            m_change = change;
            m_releaseTime = System.currentTimeMillis() + m_banTime * 60 * 1000L;
        }
        
    }
    
    private static class Change {
    	
        public String m_change;
        public int    m_adds;
        public int    m_removals;

        @Override
        public String toString() {
            return m_change;
        }

        @Override
        public boolean equals( final Object o ) {
            if( o instanceof Change ) {
                return m_change.equals( ( ( Change )o ).m_change );
            }
            return false;
        }

        @Override
        public int hashCode() {
            return m_change.hashCode() + 17;
        }
        
    }

}
