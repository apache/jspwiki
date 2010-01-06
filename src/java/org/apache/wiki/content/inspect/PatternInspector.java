package org.apache.wiki.content.inspect;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.wiki.WikiContext;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.util.FileUtil;

/**
 * {@link Inspector} implementation that determines if a change is spam because
 * it contains words on a blacklist.
 */
public class PatternInspector implements Inspector
{
    private String m_forbiddenWordsPage = "SpamFilterWordList";

    private String m_blacklist = "SpamFilterWordList/blacklist.txt";

    private static final Logger log = LoggerFactory.getLogger( PatternInspector.class );

    /**
     * The filter property name for specifying the page which contains the list
     * of spam words. Value is <tt>{@value}</tt>.
     */
    public static final String PROP_WORDLIST = "wordlist";

    /**
     * The filter property name for the attachment containing the blacklist.
     * Value is <tt>{@value}</tt>.
     */
    public static final String PROP_BLACKLIST = "blacklist";

    private Collection<Pattern> m_spamPatterns = null;

    private Date m_lastRebuild = new Date( 0L );

    private static final String LISTVAR = "spamwords";

    /**
     * Always returns {@link Inspector.Scope#FIELD}.
     */
    public Scope getScope()
    {
        return Scope.FIELD;
    }

    public void initialize( InspectionPlan config )
    {
        Properties properties = config.getProperties();
        m_forbiddenWordsPage = properties.getProperty( PROP_WORDLIST, m_forbiddenWordsPage );
        m_blacklist = properties.getProperty( PROP_BLACKLIST, m_blacklist );
    }

    /**
     * Returns {@link Finding.Result#FAILED} if any contents are contained on
     * the banned-word pattern blacklist; {@code null} otherwise.
     * @param inspection the current Inspection
     * @param change the current contents, plus content that represents the added or
     *            deleted text since the last change
     * @return {@link Finding.Result#FAILED} if the test fails; {@code null} otherwise
     */
    public Finding[] inspect( Inspection inspection, Change change )
    {
        WikiContext context = inspection.getContext();
        refreshBlacklists( context );
        //
        // If we have no spam patterns defined, or we're trying to save
        // the page containing the patterns, just return.
        //
        if( m_spamPatterns == null || context.getPage().getName().equals( m_forbiddenWordsPage ) )
        {
            return null;
        }

        String ch = change.toString();

        if( context.getHttpRequest() != null )
            ch += context.getHttpRequest().getRemoteAddr();

        for( Pattern p : m_spamPatterns )
        {
            // log.debug("Attempting to match page contents with
            // "+p.getPattern());

            Matcher matcher = p.matcher( ch );
            if( matcher.find() )
            {
                return new Finding[] { new Finding( Topic.SPAM, Finding.Result.FAILED, "'" + p.pattern()
                                                                                       + "' is spam word. (Incident code "
                                                                                       + inspection.getUid() + ")" ) };
            }
        }
        return null;
    }

    /**
     * Takes a MT-Blacklist -formatted blacklist and returns a list of compiled
     * Pattern objects.
     * 
     * @param list
     * @return The parsed blacklist patterns.
     */
    private Collection<Pattern> parseBlacklist( String list )
    {
        ArrayList<Pattern> compiledpatterns = new ArrayList<Pattern>();

        if( list != null )
        {
            try
            {
                BufferedReader in = new BufferedReader( new StringReader( list ) );

                String line;

                while ( (line = in.readLine()) != null )
                {
                    line = line.trim();
                    if( line.length() == 0 )
                        continue; // Empty line
                    if( line.startsWith( "#" ) )
                        continue; // It's a comment

                    int ws = line.indexOf( ' ' );

                    if( ws == -1 )
                        ws = line.indexOf( '\t' );

                    if( ws != -1 )
                        line = line.substring( 0, ws );

                    try
                    {
                        compiledpatterns.add( Pattern.compile( line ) );
                    }
                    catch( PatternSyntaxException e )
                    {
                        log.debug( "Malformed spam filter pattern " + line );
                    }
                }
            }
            catch( IOException e )
            {
                log.info( "Could not read patterns; returning what I got", e );
            }
        }

        return compiledpatterns;
    }

    /**
     * Parses a list of patterns and returns a Collection of compiled Pattern
     * objects.
     * 
     * @param source
     * @param list
     * @return A Collection of the Patterns that were found from the lists.
     */
    private Collection<Pattern> parseWordList( WikiPage source, String list )
    {
        ArrayList<Pattern> compiledpatterns = new ArrayList<Pattern>();

        if( list != null )
        {
            StringTokenizer tok = new StringTokenizer( list, " \t\n" );

            while ( tok.hasMoreTokens() )
            {
                String pattern = tok.nextToken();

                try
                {
                    compiledpatterns.add( Pattern.compile( pattern ) );
                }
                catch( PatternSyntaxException e )
                {
                    log.debug( "Malformed spam filter pattern " + pattern );

                    source.setAttribute( "error", "Malformed spam filter pattern " + pattern );
                }
            }
        }

        return compiledpatterns;
    }

    /**
     * If the spam filter notices changes in the black list page, it will
     * refresh them automatically.
     * 
     * @param context
     */
    private void refreshBlacklists( WikiContext context )
    {
        try
        {
            WikiPage source = null;
            Attachment att = null;
            try
            {
                source = context.getEngine().getPage( m_forbiddenWordsPage );
                att = context.getEngine().getAttachmentManager().getAttachmentInfo( context, m_blacklist );
            }
            catch( PageNotFoundException e )
            {
                // No worries
            }

            boolean rebuild = false;

            //
            // Rebuild, if the page or the attachment has changed since.
            //
            if( source != null )
            {
                if( m_spamPatterns == null || m_spamPatterns.isEmpty() || source.getLastModified().after( m_lastRebuild ) )
                {
                    rebuild = true;
                }
            }

            if( att != null )
            {
                if( m_spamPatterns == null || m_spamPatterns.isEmpty() || att.getLastModified().after( m_lastRebuild ) )
                {
                    rebuild = true;
                }
            }

            //
            // Do the actual rebuilding. For simplicity's sake, we always
            // rebuild the complete
            // filter list regardless of what changed.
            //

            if( rebuild )
            {
                m_lastRebuild = new Date();

                m_spamPatterns = parseWordList( source, (source != null) ? (String) source.getAttribute( LISTVAR ) : null );

                log.info( "Spam filter reloaded - recognizing " + m_spamPatterns.size() + " patterns from page "
                          + m_forbiddenWordsPage );

                if( att != null )
                {
                    InputStream in = context.getEngine().getAttachmentManager().getAttachmentStream( att );

                    StringWriter out = new StringWriter();

                    FileUtil.copyContents( new InputStreamReader( in, "UTF-8" ), out );

                    Collection<Pattern> blackList = parseBlacklist( out.toString() );

                    log.info( "...recognizing additional " + blackList.size() + " patterns from blacklist " + m_blacklist );

                    m_spamPatterns.addAll( blackList );
                }
            }
        }
        catch( IOException ex )
        {
            log.info( "Unable to read attachment data, continuing...", ex );
        }
        catch( ProviderException ex )
        {
            log.info( "Failed to read spam filter attachment, continuing...", ex );
        }

    }
}
