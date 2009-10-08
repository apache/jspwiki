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
package org.apache.wiki.filters;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.jrcs.diff.DifferentiationFailedException;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.inspect.*;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;

/**
 * Spam filter that can also do choke modifications. Parameters:
 * <ul>
 * <li>wordlist - Page name where the regexps are found. Use [{SET
 * spamwords='regexp list separated with spaces'}] on that page. Default is
 * "SpamFilterWordList".
 * <li>blacklist - The name of an attachment containing the list of spam
 * patterns, one per line. Default is "SpamFilterWordList/blacklist.txt"</li>
 * <li>errorpage - The page to which the user is redirected. Has a special
 * variable $msg which states the reason. Default is "RejectedMessage".
 * <li>pagechangesinminute - How many page changes are allowed/minute. Default
 * is 5.</li>
 * <li>similarchanges - How many similar page changes are allowed before the
 * host is banned. Default is 2. (since 2.4.72)</li>
 * <li>bantime - How long an IP address stays on the temporary ban list (default
 * is 60 for 60 minutes).</li>
 * <li>maxurls - How many URLs can be added to the page before it is considered
 * spam (default is 5)</li>
 * <li>akismet-apikey - The Akismet API key (see akismet.org)</li>
 * <li>ignoreauthenticated - If set to "true", all authenticated users are
 * ignored and never caught in SpamFilter</li>
 * <li>captcha - Sets the captcha technology to use. Current allowed values are
 * "none" and "asirra".</li>
 * <li>strategy - Sets the filtering strategy to use. If set to "eager", will
 * stop at the first probable match, and won't consider any other tests. This is
 * the default, as it's considerably lighter. If set to "score", will go through
 * all of the tests and until a threshold score is reached. Setting the strategy
 * to "eager" is equivalent to setting the scoring theshold to 1.
 * </ul>
 * <p>
 * Please see the default editors/plain.jsp for examples on how the SpamFilter
 * integrates with the editor system.
 * </p>
 * <p>
 * Changes by admin users are ignored in any case.
 * </p>
 * 
 * @since 2.1.112
 */
public class SpamFilter extends BasicPageFilter
{
    /**
     * The filter property name for the page to which you are directed if Herb
     * rejects your edit. Value is <tt>{@value}</tt>.
     */
    public static final String PROP_ERRORPAGE = "errorpage";

    /**
     * The filter property name for specifying which captcha technology should
     * be used. Value is <tt>{@value}</tt>.
     */
    public static final String PROP_CAPTCHA = "captcha";

    /**
     * This method is used to calculate an unique code when submitting the page
     * to detect edit conflicts. It currently incorporates the last-modified
     * date of the page, and the IP address of the submitter.
     * 
     * @param page The WikiPage under edit
     * @param request The HTTP Request
     * @since 2.6
     * @return A hash value for this page and session
     */
    public static final String getSpamHash( WikiPage page, HttpServletRequest request )
    {
        long lastModified = 0;

        if( page.getLastModified() != null )
            lastModified = page.getLastModified().getTime();

        long remote = request.getRemoteAddr().hashCode();

        return Long.toString( lastModified ^ remote );
    }

    private String m_errorPage = "RejectedMessage";

    private static final String ATTR_SPAMFILTER_SCORE = "spamfilter.score";

    private InspectionPlan m_plan;

    private boolean m_useCaptcha = false;

    private static Logger log = LoggerFactory.getLogger( SpamFilter.class );

    public InspectionPlan getInspectionContext()
    {
        return m_plan;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize( WikiEngine engine, Properties properties )
    {
        m_errorPage = properties.getProperty( PROP_ERRORPAGE, m_errorPage );
        m_useCaptcha = properties.getProperty( PROP_CAPTCHA, "" ).equals( "asirra" );
        m_plan = SpamInspectionFactory.getInspectionPlan( engine, properties );
    }

    /** {@inheritDoc} */
    public String preSave( WikiContext context, String content ) throws RedirectException
    {
        Change change;
        try
        {
            change = Change.getPageChange( context, content );
        }
        catch( DifferentiationFailedException e )
        {
            throw new RedirectException( "Could not diff page.", getRedirectPage( context ) );
        }

        // Run the Inspection
        Inspection inspection = new Inspection( context, m_plan );
        float spamScoreLimit = SpamInspectionFactory.defaultSpamLimit( m_engine );
        SpamInspectionFactory.setSpamLimit( inspection, spamScoreLimit );
        inspection.inspect( content, change );
        float spamScore = inspection.getScore( Topic.SPAM );
        context.setVariable( ATTR_SPAMFILTER_SCORE, spamScore );

        // Redirect user if score too low
        if( spamScore <= spamScoreLimit )
        {
            StringBuilder s = new StringBuilder();
            for( Finding finding : inspection.getFindings( Topic.SPAM ) )
            {
                s.append( finding.getMessage() );
                s.append( ' ' );
            }
            throw new RedirectException( s.toString(), getRedirectPage( context ) );
        }

        // Log successful change
        else
        {
            log.debug( "Not spam: " + change.toString() );
        }
        return content;
    }

    /**
     * Returns a page to which we shall redirect, based on the current value of
     * the "captcha" parameter.
     * 
     * @param ctx WikiContext
     * @return An URL to redirect to
     */
    private String getRedirectPage( WikiContext ctx )
    {
        if( m_useCaptcha )
            return ctx.getURL( WikiContext.NONE, "Captcha.jsp", "page=" + ctx.getEngine().encodeName( ctx.getPage().getName() ) );

        return ctx.getURL( WikiContext.VIEW, m_errorPage );
    }
}
