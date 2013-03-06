package org.apache.wiki.site;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.Release;
import org.apache.wiki.TranslationsCheck;


/**
 * Generates the following site's pages:
 * 
 * <ul>
 *  <li><code>release.mdtext</code>: used at the end of the sidebar; displays JSPWiki version</li>
 *  <li><code>changelog.mdtext</code>: prettifies the Changelog file and turns it into Markdown syntax</li>
 *  <li><code>translations.mdtext</code>: shows the % completed of the different translations' status</li>
 * </ul>
 * 
 * This test file assumes you've checked out both app and site trunk following this structure:
 * <pre>
 *  ./                     [http://svn.apache.org/repos/asf/incubator/jspwiki]
 *  ./trunk                [http://svn.apache.org/repos/asf/incubator/jspwiki/trunk]
 *  ./site/trunk           [http://svn.apache.org/repos/asf/incubator/jspwiki/site/trunk]
 * </pre>
 * 
 * Once you've run this test you should be able to commit the site's changes. Hopefully, 
 * the CI job will publish the site later on (only if the build is successful, that is)
 */
public class SiteGeneratorTest extends TestCase
{

    private static final Logger LOG = Logger.getLogger( SiteGeneratorTest.class );
    
    private static final String BASE_FILE_NAME = "../site/trunk/";
    
    private static final String I18N_BASE = "/etc/i18n/";
    private static final String I18N_CORE = I18N_BASE + "CoreResources.properties";
    private static final String I18N_TEMPLATE = I18N_BASE + "templates/default.properties";
    private static final String I18N_PLUGIN = I18N_BASE + "plugin/PluginResources.properties";
    
    private static final Pattern p = Pattern.compile("JSPWIKI\\-([0-9]+)");

    /**
     * Generates site's pages from source content.
     */
    public void testGenerateSiteFiles() 
    {
        generateReleaseFile();
        generateI18nStatusFile();
        generateChangelogFile();
    }
    
    /**
     * generates <code>release.mdtext</code>; used at the end of the sidebar, displays JSPWiki version.
     */
    void generateReleaseFile() 
    {
        String file = BASE_FILE_NAME + "templates/release.mdtext";
        String content = "JSPWiki v" + Release.VERSTR;
        write( file, content );
    }
    
    /**
     * generates <code>translations.mdtext</code>; shows the % completed of the different translations' status.
     */
    void generateI18nStatusFile()
    {
        String file = BASE_FILE_NAME + "content/jspwiki/development/translations.mdtext";
        StringBuilder sb = new StringBuilder( "## I18n current status\n" );
        
        String[] locales = new String[] { "de", "es", "fi", "fr", "it", "nl", "pt_BR", "ru", "zh_CN" };
        for( String locale : locales ) 
        {
            generateI18nStatusForGivenLocale( sb, locale );
        }
        write( file, sb.toString() );
    }
    
    /**
     * generates the translation status related to a given locale.
     * 
     * @param sb current content.
     * @param i18n given locale.
     */
    void generateI18nStatusForGivenLocale( StringBuilder sb, String i18n )
    {
        sb.append( "\n### **" ).append( i18n ).append( " locale**\n" );
        try
        {
            generateI18nStatusFromFile( sb, i18n, I18N_CORE );
            generateI18nStatusFromFile( sb, i18n, I18N_TEMPLATE );
            generateI18nStatusFromFile( sb, i18n, I18N_PLUGIN );
        }
        catch (IOException e)
        {
            LOG.error( e.getMessage(), e );
        }
    }
    
    /**
     * generates the translation status related to one of the i18n files, for a given locale.
     * 
     * @param sb current content
     * @param i18n given locale
     * @param file i18n file to examine
     */
    void generateI18nStatusFromFile( StringBuilder sb, String i18n, String file ) throws IOException 
    {
        String i18nFile = StringUtils.replace( file, ".properties", "_" + i18n + ".properties" );
        Map< String, Integer > diff = TranslationsCheck.diff( file, i18nFile );
        int dup = TranslationsCheck.detectDuplicates( i18nFile );
        sb.append( "    * " ).append( i18nFile ).append( "\n" )
          .append( "        * Missing: " ).append( diff.get( "missing" ) ).append( "\n" )
          .append( "        * Outdated: " ).append( diff.get( "outdated" ) ).append( "\n" )
          .append( "        * Duplicated: " ).append( dup ).append( "\n\n" );
        TranslationsCheck.clearDuplicates();
    }
    
    /**
     * generates <code>changelog.mdtext</code>; prettifies the ChangeLog file and turns it into Markdown syntax.
     */
    void generateChangelogFile() 
    {
        String file = BASE_FILE_NAME + "content/jspwiki/development/changelog.mdtext";
        StringBuilder sb = new StringBuilder( "## Changelog\n\n" );
        List< String > links = new ArrayList< String >();
        try 
        {
            @SuppressWarnings({ "unchecked", "cast" })
            List< String > lines = ( List< String > )FileUtils.readLines( new File( "./ChangeLog") );
            for( String line : lines ) 
            {
                parseChangeLogLine( line, sb, links );
            }
        }
        catch( IOException e )
        {
            LOG.error( e.getMessage(), e );
        }
        
        append( links, sb );
        write( file, sb.toString() );
    }
    
    /**
     * generates the content for a given line. If the line begins with a date it's assumed to be a 
     * header, if it's not, then is a regular line.
     * 
     * @param line given line.
     * @param sb current content.
     * @param links collection of links, to be able to print JIRA links later on.
     */
    void parseChangeLogLine( String line, StringBuilder sb, List< String > links ) 
    {
        if( lineBeginsWithDate( line ) ) 
        {
            sb.append( "#### " ).append( line ).append( "\n" );
        }
        else 
        {
            parseRegularLine( line, sb, links );
        }
    }
    
    /**
     * generates the content for a <em>regular</em> line. Initial blank space from ChangeLog is 
     * removed and JIRA id's are replaced by regular Markdown links and stored on the collection
     * of links.
     * 
     * @param line given line.
     * @param sb current content.
     * @param links collection of links, to be able to print JIRA links later on.
     */
    void parseRegularLine( String line, StringBuilder sb, List< String > links ) 
    {
        line = StringUtils.replace( line, "       ", StringUtils.EMPTY );
        Matcher m = p.matcher( line );
        while( m.find() ) 
        {
            String replace = StringUtils.replace( m.group( 0 ), "JSPWIKI-", "[JSPWIKI " );
            replace += "][JIRA-" + StringUtils.replace( m.group( 0 ), "JSPWIKI-", StringUtils.EMPTY ) + "]";
            links.add( "JIRA-" + StringUtils.replace( m.group( 0 ), "JSPWIKI-", StringUtils.EMPTY ) );
            line = m.replaceFirst( replace );
            m = p.matcher( line );
        }
        sb.append( line ).append( "\n" );
    }
    
    /**
     * checks if a given line begins with a date or not.
     * 
     * @param line given line.
     * @return {@code true} if it does, {@code false} otherwise. 
     */
    boolean lineBeginsWithDate( String line ) 
    {
        boolean begins = false;
        try
        {
            DateUtils.parseDate( StringUtils.substring( line, 0, 10 ), 
                                 new String[]{ "yyyy-mm-dd", "yyyy/mm/dd" } );
            begins = true;
        }
        catch (ParseException e)
        {
            LOG.info( "Not a date, so it's not a heading" );
        }
        return begins;
    }
    
    /**
     * appends the collection of links to the generated content so far.
     * 
     * @param links collection of links.
     * @param sb generated content so far.
     */
    void append( List< String > links, StringBuilder sb )
    {
        for( String link : links ) 
        {
            sb.append( "  [" ).append( link )
              .append( "]: https://issues.apache.org/jira/browse/" )
              .append( StringUtils.replace( link, "JIRA", "JSPWIKI" ) )
              .append( "\n" );
        }
    }
    
    /**
     * Writes the file with the given content.
     * 
     * @param filename where to write.
     * @param content what to write.
     */
    void write( String filename, String content )  
    {
        BufferedOutputStream bos = null;
        try
        {
            LOG.info( "Attempting to write " + filename );
            File file = new File( filename );
            if( !file.exists() ) 
            {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream( file );
            bos = new BufferedOutputStream( fos );
            bos.write( content.getBytes() );
        }
        catch( IOException e )
        {
            LOG.error( e.getMessage(), e );
        } 
        finally 
        {
            if( bos != null )
            {
                try
                {
                    bos.flush();
                    bos.close();
                }
                catch( IOException e )
                {
                    LOG.error( e.getMessage(), e );
                }
            }
        }
    }
    
}
