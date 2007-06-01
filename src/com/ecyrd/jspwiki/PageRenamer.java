/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2006 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki;

import java.util.*;

import org.apache.log4j.Logger;
import org.apache.oro.text.regex.*;

import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.parser.JSPWikiMarkupParser;
import com.ecyrd.jspwiki.parser.MarkupParser;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.providers.WikiAttachmentProvider;
import com.ecyrd.jspwiki.providers.WikiPageProvider;



/**
 *  Do all the nitty-gritty work of renaming pages.
 *
 *  @since 2.4
 */
public class PageRenamer
{
    private static final Logger log = Logger.getLogger(PageRenamer.class);

    private final WikiEngine m_wikiEngine;

    private static final PatternMatcher MATCHER = new Perl5Matcher();

    private boolean m_camelCaseLink;
    private boolean m_matchEnglishPlurals;

    private static final String LONG_LINK_PATTERN = "\\[([\\w\\s]+\\|)?([\\w\\s\\+-/\\?&;@:=%\\#<>$\\.,\\(\\)'\\*]+)?\\]";
    private static final String CAMELCASE_LINK_PATTERN = "([[:upper:]]+[[:lower:]]+[[:upper:]]+[[:alnum:]]*)";

    private Pattern m_longLinkPattern = null;
    private Pattern m_camelCaseLinkPattern = null;

    /** Extension to be appended onto directories to denote they contain attachments. */
    public static final String DIR_EXTENSION   = "-att";
    
    /** Property key denoting storage directory for attachments. */
    public static final String PROP_STORAGEDIR = "jspwiki.basicAttachmentProvider.storageDir";


    /**
     * Constructor, ties this renamer instance to a WikiEngine.
     * @param engine the wiki engine
     * @param props the properties used to initialize the wiki engine
     */
    public PageRenamer( WikiEngine engine, Properties props )
    {
        m_wikiEngine = engine;

        // Retrieve relevant options
        m_matchEnglishPlurals = TextUtil.getBooleanProperty( props,
                                                             WikiEngine.PROP_MATCHPLURALS,
                                                             false );

        m_camelCaseLink = TextUtil.getBooleanProperty( props,
                                                       JSPWikiMarkupParser.PROP_CAMELCASELINKS,
                                                       false );

        // Compile regular expression patterns
        PatternCompiler compiler = new Perl5Compiler();

        try
        {
            m_longLinkPattern = compiler.compile( LONG_LINK_PATTERN );
            m_camelCaseLinkPattern = compiler.compile( CAMELCASE_LINK_PATTERN );
        }
        catch (MalformedPatternException mpe)
        {
            log.error( "Error compiling regexp patterns.", mpe );
        }
    }

    /**
     * Renames, or moves, a wiki page. Can also alter referring wiki
     * links to point to the renamed page.
     * @param context TODO
     * @param oldName           Name of the source page.
     * @param newName           Name of the destination page.
     * @param changeReferrers   If true, then changes any referring links
     *                          to point to the renamed page.
     *
     * @return The name of the page that the source was renamed to.
     *
     * @throws WikiException    In the case of an error, such as the destination
     *                          page already existing.
     */
    public String renamePage(WikiContext context, String oldName, String newName, boolean changeReferrers)
        throws WikiException
    {
        // Work out the clean version of the new name of the page
        newName = MarkupParser.cleanLink( newName.trim() );

        // Get the collection of pages that the refered to the old name (the From name)...
        Collection referrers = getReferrersCollection( oldName );

        log.debug( "Rename request for page '"+ oldName +"' to '" + newName + "'" );

        // Check if we're attempting to rename to a pagename that already exists
        if( m_wikiEngine.pageExists( newName ) )
        {
            log.debug("Rename request failed because target page '"+newName+"' exists");

            throw new WikiException( "Page exists" );
        }

        // Tell the providers to actually move the data around...
        movePageData( oldName, newName );
        moveAttachmentData( oldName, newName );

        m_wikiEngine.getReferenceManager().clearPageEntries(oldName);

        // If there were pages refering to the old name, update them to point to the new name...
        if( referrers != null )
        {
            updateReferrersOnRename( context, oldName, newName, changeReferrers, referrers );
        }
        else
        {
            // Now we need to go and update.
            WikiPage p = m_wikiEngine.getPage(newName);
            String pagedata = m_wikiEngine.getPureText(p);
            Collection refs = m_wikiEngine.scanWikiLinks(p,pagedata);
            m_wikiEngine.getReferenceManager().updateReferences(newName,refs);
        }

        return newName;
    }


    // Go gather and return a collection of page names that refer to the old name...
    private Collection getReferrersCollection( String oldName )
    {
        TreeSet list = new TreeSet();

        WikiPage p = m_wikiEngine.getPage(oldName);

        if( p != null )
        {
            Collection c = m_wikiEngine.getReferenceManager().findReferrers(oldName);

            if( c != null ) list.addAll(c);

            try
            {
                Collection attachments = m_wikiEngine.getAttachmentManager().listAttachments(p);

                for( Iterator i = attachments.iterator(); i.hasNext(); )
                {
                    Attachment att = (Attachment) i.next();

                    c = m_wikiEngine.getReferenceManager().findReferrers(att.getName());

                    if( c != null ) list.addAll(c);
                }
            }
            catch( ProviderException e )
            {
                log.error("Cannot list attachments",e);
            }

        }

        return list;
    }


    // Loop the collection, calling update for each, tickle the reference manager when done.
    private void updateReferrersOnRename( WikiContext context,
                                          String oldName,
                                          String newName,
                                          boolean changeReferrers,
                                          Collection referrers)
    {
        // Make a new list out of this, otherwise there is a ConcurrentModificationException
        // when the referrer is modifed at the end of this loop when it no longer refers to
        // the original page.
        List referrersList = new ArrayList( referrers );
        Iterator referrersIterator = referrersList.iterator();
        while ( referrersIterator.hasNext() )
        {
            String referrerName = (String)referrersIterator.next();
            updateReferrerOnRename( context, oldName, newName, changeReferrers, referrerName );
        }

        m_wikiEngine.getReferenceManager().clearPageEntries( oldName );

        String text = m_wikiEngine.getText( newName );

        Collection updatedReferrers = m_wikiEngine.scanWikiLinks( m_wikiEngine.getPage(newName),text );
        m_wikiEngine.getReferenceManager().updateReferences( newName, updatedReferrers );
    }


    // Update the referer, changing text if indicated.
    private void updateReferrerOnRename(WikiContext context, String oldName, String newName, boolean changeReferrer, String referrerName)
    {
        log.debug("oldName = "+oldName);
        log.debug("newName = "+newName);
        log.debug("referrerName = "+referrerName);

        String text = m_wikiEngine.getPureText(referrerName,WikiProvider.LATEST_VERSION);

        if (changeReferrer)
        {
            text = changeReferrerText( oldName, newName, referrerName, text );
        }

        try
        {

            WikiContext tempCtx = new WikiContext( m_wikiEngine, m_wikiEngine.getPage(referrerName) );

            if (context.getPage() != null)
            {
                PageLock lock = m_wikiEngine.getPageManager().getCurrentLock( m_wikiEngine.getPage(referrerName) );
                m_wikiEngine.getPageManager().unlockPage( lock );

                tempCtx.getPage().setAuthor( context.getCurrentUser().getName() );
                m_wikiEngine.saveText( tempCtx, text );

                Collection updatedReferrers = m_wikiEngine.scanWikiLinks( m_wikiEngine.getPage(referrerName),text );

                m_wikiEngine.getReferenceManager().updateReferences( referrerName, updatedReferrers );
             }
        }
        catch( WikiException e )
        {
            log.error("Unable to update referer on rename!",e);
        }

    }

    /**
     * Change the text of each referer to reflect the renamed page.  There are seven starting cases
     * and two differnting ending scenarios depending on if the new name is camel or long.
     * <pre>
     * "Start"                               "A"                                "B"
     * 1) OldCleanLink                   --> NewCleanLink                   --> [New Long Link]
     * 2) [OldCleanLink]                 --> [NewCleanLink]                 --> [New Long Link]
     * 3) [old long text|OldCleanLink]   --> [old long text|NewCleanLink]   --> [old long text|New Long Link]
     * 4) [Old Long Link]                --> [NewCleanLink]                 --> [New Long Link]
     * 5) [old long text|Old Long Link]  --> [old long text|NewCleanLink]   --> [old long text|New Long Link]
     * 6) OldLongLink                    --> NewCleanLink                   --> NewLongLink
     * 7) [OldLongLink]                  --> [NewCleanLink]                 --> [NewLongLink]
     * </pre>
     * It's important to note that case 6 and 7 can exist, but are not expected since they are
     * counter intuitive.
     * <br/>
     * When doing any of the above renames these should not get touched...
     * A) OtherOldCleanLink
     * B) ~OldCleanLink     <-Um maybe we _should_ rename this one?
     * C) [[OldCleanLink]   <-Maybe rename this too?
     */
    private String changeReferrerText(String oldName, String newName, String referrerName, String referrerText)
    {
        // The text we are replacing old links with
        // String replacementLink = null;

        // Work out whether the new page name is CamelCase or not
        // TODO: Check if the pattern can be replaced with the compiled version
        /*
        if( m_camelCaseLink == false || !m_perlUtil.match( "/" + m_camelCaseLinkPatternString + "/", newName ) )
        {
            replacementLink = "["+newName+"]";
        }
        else
        {
            replacementLink = newName;
        }
        */
        // replacementLink = "["+newName+"]";
        // Replace long format links
        referrerText = replaceLongLinks( referrerText, oldName, newName );

        // Replace CamelCase links
        if( m_camelCaseLink == true )
        {
            referrerText = replaceCamelCaseLinks( referrerText, oldName, newName );
        }

        return referrerText;
    }

    /**
     *  Replace long format links in a piece of text
     */
    private String replaceLongLinks( String text, String oldName, String replacementLink )
    {
        int lastMatchEnd = 0;

        PatternMatcherInput input = new PatternMatcherInput( text );

        StringBuffer ret = new StringBuffer();

        while( MATCHER.contains( input, m_longLinkPattern ) )
        {
            MatchResult matchResult = MATCHER.getMatch();

            ret.append( input.substring( lastMatchEnd, matchResult.beginOffset( 0 ) ) );

            String linkText = matchResult.group( 1 );
            String link = matchResult.group( 2 );

            String anchor = "";
            String subpage = "";

            int hash;
            if( (hash = link.indexOf('#')) != -1 )
            {
                anchor = link.substring(hash);
                link   = link.substring(0,hash);
            }

            int slash;
            if( (slash = link.indexOf('/')) != -1 )
            {
                subpage = link.substring(slash);
                link    = link.substring(0,slash);
            }

            String linkDestinationPage = checkPluralPageName( MarkupParser.cleanLink( link ) );

            if( linkDestinationPage.equals( oldName ) )
            {
                String properReplacement;

                if( linkText != null )
                {
                    properReplacement = '[' + linkText + replacementLink+subpage+anchor + ']';
                }
                else
                {
                    properReplacement = '['+replacementLink+subpage+anchor+']';
                }

                ret.append( properReplacement );
            }
            else
            {
                ret.append( input.substring( matchResult.beginOffset( 0 ), matchResult.endOffset( 0 ) ) );
            }

            lastMatchEnd = matchResult.endOffset(0);
        }

        ret.append( input.substring( lastMatchEnd ) );

        return ret.toString();
    }

    // Replace CamelCase format links in a piece of text
    private String replaceCamelCaseLinks( String text, String oldName, String replacementLink )
    {
        int lastMatchEnd = 0;

        PatternMatcherInput input = new PatternMatcherInput( text );

        StringBuffer ret = new StringBuffer();

        while( MATCHER.contains( input, m_camelCaseLinkPattern ) )
        {
            MatchResult matchResult = MATCHER.getMatch();

            ret.append( input.substring( lastMatchEnd, matchResult.beginOffset( 0 ) ) );

            // Check if there's the tilde to stop this being a camel case link
            int matchOffset = matchResult.beginOffset( 0 );

            char charBefore = 0;

            if( matchOffset != 0 )
            {
                charBefore = input.charAt( matchOffset - 1 );
            }

            // Check if the CamelCase link has been escaped
            if ( charBefore != '~' )
            {
                // Check if this link maps to our page
                String page = checkPluralPageName( matchResult.group( 0 ) );

                if( page.equals( oldName ) )
                {
                    ret.append( replacementLink );
                }
                else
                {
                    ret.append( input.substring( matchResult.beginOffset( 0 ), matchResult.endOffset( 0 ) ) );
                }
            }
            else
            {
                ret.append( input.substring( matchResult.beginOffset( 0 ), matchResult.endOffset( 0 ) ) );
            }

            lastMatchEnd = matchResult.endOffset(0);
        }

        ret.append( input.substring( lastMatchEnd ) );

        return ret.toString();
    }

    /**
     * Checks if a name is plural, and if so, checks if the page with plural
     * exists, otherwise it returns the singular version of the name.
     * @param pageName the name of the page
     * @return the corrected page name
     */
    public String checkPluralPageName( String pageName )
    {
        if( pageName == null )
        {
            return null;
        }

        if( m_matchEnglishPlurals )
        {
            try
            {
                if( pageName.endsWith( "s" ) && !m_wikiEngine.getPageManager().pageExists( pageName ) )
                {
                    pageName = pageName.substring( 0, pageName.length() - 1 );
                }
            }
            catch( ProviderException e )
            {
                log.error("Unable to check Plural Pagename!",e);
            }
        }

        return pageName;
    }

    //Move the page data from the old name to the new name.
    private void movePageData(String oldName, String newName) throws WikiException
    {
        WikiPageProvider pageProvider = m_wikiEngine.getPageManager().getProvider();

        try
        {
            pageProvider.movePage(oldName, newName);
        }
        catch (ProviderException pe)
        {
            log.debug("Failed in .movePageData()", pe);
            throw new WikiException(pe.getMessage());
        }
    }


    //Move the attachment data from the old name to the new name.
    private void moveAttachmentData(String oldName, String newName) throws WikiException
    {
        WikiAttachmentProvider attachmentProvider = m_wikiEngine.getAttachmentManager().getCurrentProvider();

        log.debug("Trying to move all attachments from old page name "+oldName+" to new page name "+newName);

        try
        {
            attachmentProvider.moveAttachmentsForPage(oldName, newName);
            //moveAttachmentsForPage(oldName, newName);
        }
        catch (ProviderException pe)
        {
            log.debug("Failed in .moveAttachmentData()", pe);
            throw new WikiException(pe.getMessage());
        }
    }
}
