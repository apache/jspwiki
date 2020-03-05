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
package org.apache.wiki.providers;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.search.QueryItem;
import org.apache.wiki.search.SearchMatcher;
import org.apache.wiki.search.SearchResult;
import org.apache.wiki.search.SearchResultComparator;
import org.apache.wiki.util.FileUtil;
import org.apache.wiki.util.TextUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;


/**
 *  Provides a simple directory based repository for Wiki pages.
 *  <P>
 *  All files have ".txt" appended to make life easier for those who insist on using Windows or other software which makes assumptions
 *  on the files contents based on its name.
 *  <p>
 *  This class functions as a superclass to all file based providers.
 *
 *  @since 2.1.21.
 */
public abstract class AbstractFileProvider implements WikiPageProvider {

    private static final Logger log = Logger.getLogger(AbstractFileProvider.class);
    private String m_pageDirectory = "/tmp/";
    protected String m_encoding;

    protected Engine m_engine;

    public static final String PROP_CUSTOMPROP_MAXLIMIT = "custom.pageproperty.max.allowed";
    public static final String PROP_CUSTOMPROP_MAXKEYLENGTH = "custom.pageproperty.key.length";
    public static final String PROP_CUSTOMPROP_MAXVALUELENGTH = "custom.pageproperty.value.length";

    public static final int DEFAULT_MAX_PROPLIMIT = 200;
    public static final int DEFAULT_MAX_PROPKEYLENGTH = 255;
    public static final int DEFAULT_MAX_PROPVALUELENGTH = 4096;

    /**
     * This parameter limits the number of custom page properties allowed on a page
     */
    public static int MAX_PROPLIMIT = DEFAULT_MAX_PROPLIMIT;

    /**
     * This number limits the length of a custom page property key length. The default value here designed with future JDBC providers in mind.
     */
    public static int MAX_PROPKEYLENGTH = DEFAULT_MAX_PROPKEYLENGTH;

    /**
     * This number limits the length of a custom page property value length. The default value here designed with future JDBC providers in mind.
     */
    public static int MAX_PROPVALUELENGTH = DEFAULT_MAX_PROPVALUELENGTH;

    /**
     *  Name of the property that defines where page directories are.
     */
    public static final String PROP_PAGEDIR = "jspwiki.fileSystemProvider.pageDir";

    /**
     *  All files should have this extension to be recognized as JSPWiki files.
     *  We default to .txt, because that is probably easiest for Windows users,
     *  and guarantees correct handling.
     */
    public static final String FILE_EXT = ".txt";

    /** The default encoding. */
    public static final String DEFAULT_ENCODING = StandardCharsets.ISO_8859_1.toString();

    private boolean m_windowsHackNeeded = false;

    /**
     *  {@inheritDoc}
     *  @throws FileNotFoundException If the specified page directory does not exist.
     *  @throws IOException In case the specified page directory is a file, not a directory.
     */
    @Override
    public void initialize( final Engine engine, final Properties properties ) throws NoRequiredPropertyException, IOException, FileNotFoundException {
        log.debug( "Initing FileSystemProvider" );
        m_pageDirectory = TextUtil.getCanonicalFilePathProperty( properties, PROP_PAGEDIR,
                                                          System.getProperty( "user.home" ) + File.separator + "jspwiki-files" );

        final File f = new File( m_pageDirectory );

        if( !f.exists() ) {
            if( !f.mkdirs() ) {
                throw new IOException(
                        "Failed to create page directory " + f.getAbsolutePath() + " , please check property " + PROP_PAGEDIR );
            }
        } else {
            if( !f.isDirectory() ) {
                throw new IOException( "Page directory is not a directory: " + f.getAbsolutePath() );
            }
            if( !f.canWrite() ) {
                throw new IOException( "Page directory is not writable: " + f.getAbsolutePath() );
            }
        }

        m_engine = engine;
        m_encoding = properties.getProperty( Engine.PROP_ENCODING, DEFAULT_ENCODING );
        final String os = System.getProperty( "os.name" ).toLowerCase();
        if( os.startsWith( "windows" ) || os.equals( "nt" ) ) {
            m_windowsHackNeeded = true;
        }

        MAX_PROPLIMIT = TextUtil.getIntegerProperty( properties, PROP_CUSTOMPROP_MAXLIMIT, DEFAULT_MAX_PROPLIMIT );
        MAX_PROPKEYLENGTH = TextUtil.getIntegerProperty( properties, PROP_CUSTOMPROP_MAXKEYLENGTH, DEFAULT_MAX_PROPKEYLENGTH );
        MAX_PROPVALUELENGTH = TextUtil.getIntegerProperty( properties, PROP_CUSTOMPROP_MAXVALUELENGTH, DEFAULT_MAX_PROPVALUELENGTH );

        log.info( "Wikipages are read from '" + m_pageDirectory + "'" );
    }


    String getPageDirectory()
    {
        return m_pageDirectory;
    }

    private static final String[] WINDOWS_DEVICE_NAMES = {
        "con", "prn", "nul", "aux", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9",
        "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9"
    };

    /**
     *  This makes sure that the queried page name is still readable by the file system.  For example, all XML entities
     *  and slashes are encoded with the percent notation.
     *
     *  @param pagename The name to mangle
     *  @return The mangled name.
     */
    protected String mangleName( String pagename ) {
        pagename = TextUtil.urlEncode( pagename, m_encoding );
        pagename = TextUtil.replaceString( pagename, "/", "%2F" );

        //  Names which start with a dot must be escaped to prevent problems. Since we use URL encoding, this is invisible in our unescaping.
        if( pagename.startsWith( "." ) ) {
            pagename = "%2E" + pagename.substring( 1 );
        }

        if( m_windowsHackNeeded ) {
            final String pn = pagename.toLowerCase();
            for( final String windowsDeviceName : WINDOWS_DEVICE_NAMES ) {
                if( windowsDeviceName.equals( pn ) ) {
                    pagename = "$$$" + pagename;
                }
            }
        }

        return pagename;
    }

    /**
     *  This makes the reverse of mangleName.
     *
     *  @param filename The filename to unmangle
     *  @return The unmangled name.
     */
    protected String unmangleName( String filename ) {
        // The exception should never happen.
        if( m_windowsHackNeeded && filename.startsWith( "$$$" ) && filename.length() > 3 ) {
            filename = filename.substring( 3 );
        }

        return TextUtil.urlDecode( filename, m_encoding );
    }

    /**
     *  Finds a Wiki page from the page repository.
     *
     *  @param page The name of the page.
     *  @return A File to the page.  May be null.
     */
    protected File findPage( final String page )
    {
        return new File( m_pageDirectory, mangleName(page)+FILE_EXT );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public boolean pageExists( final String page ) {
        return findPage( page ).exists();
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public boolean pageExists( final String page, final int version ) {
        return pageExists( page );
    }

    /**
     *  This implementation just returns the current version, as filesystem does not provide versioning information for now.
     *
     *  {@inheritDoc}
     */
    @Override
    public String getPageText( final String page, final int version ) throws ProviderException {
        return getPageText( page );
    }

    /**
     *  Read the text directly from the correct file.
     */
    private String getPageText( final String page ) {
        String result  = null;
        final File pagedata = findPage( page );
        if( pagedata.exists() ) {
            if( pagedata.canRead() ) {
                try( final InputStream in = new FileInputStream( pagedata ) ) {
                    result = FileUtil.readContents( in, m_encoding );
                } catch( final IOException e ) {
                    log.error( "Failed to read", e );
                }
            } else {
                log.warn( "Failed to read page '" + page + "' from '" + pagedata.getAbsolutePath() + "', possibly a permissions problem" );
            }
        } else {
            // This is okay.
            log.info( "New page '" + page + "'" );
        }

        return result;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void putPageText( final WikiPage page, final String text ) throws ProviderException {
        final File file = findPage( page.getName() );
        try( final PrintWriter out = new PrintWriter( new OutputStreamWriter( new FileOutputStream( file ), m_encoding ) ) ) {
            out.print( text );
        } catch( final IOException e ) {
            log.error( "Saving failed", e );
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Collection< WikiPage > getAllPages()  throws ProviderException {
        log.debug("Getting all pages...");
        final ArrayList< WikiPage > set = new ArrayList<>();
        final File wikipagedir = new File( m_pageDirectory );
        final File[] wikipages = wikipagedir.listFiles( new WikiFileFilter() );

        if( wikipages == null ) {
            log.error("Wikipages directory '" + m_pageDirectory + "' does not exist! Please check " + PROP_PAGEDIR + " in jspwiki.properties.");
            throw new ProviderException( "Page directory does not exist" );
        }

        for( final File wikipage : wikipages ) {
            final String wikiname = wikipage.getName();
            final int cutpoint = wikiname.lastIndexOf( FILE_EXT );

            final WikiPage page = getPageInfo( unmangleName( wikiname.substring( 0, cutpoint ) ), WikiPageProvider.LATEST_VERSION );
            if( page == null ) {
                // This should not really happen.
                // FIXME: Should we throw an exception here?
                log.error( "Page " + wikiname + " was found in directory listing, but could not be located individually." );
                continue;
            }

            set.add( page );
        }

        return set;
    }

    /**
     *  Does not work.
     *
     *  @param date {@inheritDoc}
     *  @return {@inheritDoc}
     */
    @Override
    public Collection< WikiPage > getAllChangedSince( final Date date )
    {
        return new ArrayList<>(); // FIXME
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public int getPageCount() {
        final File wikipagedir = new File( m_pageDirectory );
        final File[] wikipages = wikipagedir.listFiles( new WikiFileFilter() );
        return wikipages != null ? wikipages.length : 0;
    }

    /**
     * Iterates through all WikiPages, matches them against the given query, and returns a Collection of SearchResult objects.
     *
     * {@inheritDoc}
     */
    @Override
    public Collection< SearchResult > findPages( final QueryItem[] query ) {
        final File wikipagedir = new File( m_pageDirectory );
        final TreeSet< SearchResult > res = new TreeSet<>( new SearchResultComparator() );
        final SearchMatcher matcher = new SearchMatcher( m_engine, query );
        final File[] wikipages = wikipagedir.listFiles( new WikiFileFilter() );

        if( wikipages != null ) {
            for( final File wikipage : wikipages ) {
                final String filename = wikipage.getName();
                final int cutpoint = filename.lastIndexOf( FILE_EXT );
                final String wikiname = unmangleName( filename.substring( 0, cutpoint ) );
                try( final FileInputStream input = new FileInputStream( wikipage ) ) {
                    final String pagetext = FileUtil.readContents( input, m_encoding );
                    final SearchResult comparison = matcher.matchPageContent( wikiname, pagetext );
                    if( comparison != null ) {
                        res.add( comparison );
                    }
                } catch( final IOException e ) {
                    log.error( "Failed to read " + filename, e );
                }
            }
        }

        return res;
    }

    /**
     *  Always returns the latest version, since FileSystemProvider
     *  does not support versioning.
     *
     *  {@inheritDoc}
     */
    @Override
    public WikiPage getPageInfo( final String page, final int version ) throws ProviderException {
        final File file = findPage( page );
        if( !file.exists() ) {
            return null;
        }

        final WikiPage p = new WikiPage( m_engine, page );
        p.setLastModified( new Date( file.lastModified() ) );

        return p;
    }

    /**
     *  The FileSystemProvider provides only one version.
     *
     *  {@inheritDoc}
     */
    @Override
    public List< WikiPage > getVersionHistory( final String page ) throws ProviderException {
        final ArrayList< WikiPage > list = new ArrayList<>();
        list.add( getPageInfo( page, WikiPageProvider.LATEST_VERSION ) );

        return list;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String getProviderInfo()
    {
        return "";
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void deleteVersion( final String pageName, final int version ) throws ProviderException {
        if( version == WikiProvider.LATEST_VERSION ) {
            final File f = findPage( pageName );
            f.delete();
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void deletePage( final String pageName ) throws ProviderException {
        final File f = findPage( pageName );
        f.delete();
    }

    /**
     * Set the custom properties provided into the given page.
     *
     * @since 2.10.2
     */
    protected void setCustomProperties( final WikiPage page, final Properties properties ) {
        final Enumeration< ? > propertyNames = properties.propertyNames();
    	while( propertyNames.hasMoreElements() ) {
            final String key = ( String )propertyNames.nextElement();
            if( !key.equals( WikiPage.AUTHOR ) && !key.equals( WikiPage.CHANGENOTE ) && !key.equals( WikiPage.VIEWCOUNT ) ) {
                page.setAttribute( key, properties.get( key ) );
            }
    	}
    }

    /**
     * Get custom properties using {@link #addCustomProperties(WikiPage, Properties)}, validate them using {@link #validateCustomPageProperties(Properties)}
     * and add them to default properties provided
     *
     * @since 2.10.2
     */
    protected void getCustomProperties( final WikiPage page, final Properties defaultProperties ) throws IOException {
        final Properties customPageProperties = addCustomProperties( page, defaultProperties );
        validateCustomPageProperties( customPageProperties );
        defaultProperties.putAll( customPageProperties );
    }

    /**
     * By default all page attributes that start with "@" are returned as custom properties.
     * This can be overwritten by custom FileSystemProviders to save additional properties.
     * CustomPageProperties are validated by {@link #validateCustomPageProperties(Properties)}
     *
     * @since 2.10.2
     * @param page the current page
     * @param props the default properties of this page
     * @return default implementation returns empty Properties.
     */
    protected Properties addCustomProperties( final WikiPage page, final Properties props ) {
        final Properties customProperties = new Properties();
        if( page != null ) {
            final Map< String, Object > atts = page.getAttributes();
            for( final String key : atts.keySet() ) {
                final Object value = atts.get( key );
                if( key.startsWith( "@" ) && value != null ) {
                    customProperties.put( key, value.toString() );
                }
            }

        }
        return customProperties;
    }

    /**
     * Default validation, validates that key and value is ASCII <code>StringUtils.isAsciiPrintable()</code> and within lengths set up in jspwiki-custom.properties.
     * This can be overwritten by custom FileSystemProviders to validate additional properties
     * See https://issues.apache.org/jira/browse/JSPWIKI-856
     * @since 2.10.2
     * @param customProperties the custom page properties being added
     */
    protected void validateCustomPageProperties( final Properties customProperties ) throws IOException {
    	// Default validation rules
        if( customProperties != null && !customProperties.isEmpty() ) {
            if( customProperties.size() > MAX_PROPLIMIT ) {
                throw new IOException( "Too many custom properties. You are adding " + customProperties.size() + ", but max limit is " + MAX_PROPLIMIT );
            }
            final Enumeration< ? > propertyNames = customProperties.propertyNames();
            while( propertyNames.hasMoreElements() ) {
                final String key = ( String )propertyNames.nextElement();
                final String value = ( String )customProperties.get( key );
                if( key != null ) {
                    if( key.length() > MAX_PROPKEYLENGTH ) {
                        throw new IOException( "Custom property key " + key + " is too long. Max allowed length is " + MAX_PROPKEYLENGTH );
                    }
                    if( !StringUtils.isAsciiPrintable( key ) ) {
                        throw new IOException( "Custom property key " + key + " is not simple ASCII!" );
                    }
                }
                if( value != null ) {
                    if( value.length() > MAX_PROPVALUELENGTH ) {
                        throw new IOException( "Custom property key " + key + " has value that is too long. Value=" + value + ". Max allowed length is " + MAX_PROPVALUELENGTH );
                    }
                    if( !StringUtils.isAsciiPrintable( value ) ) {
                        throw new IOException( "Custom property key " + key + " has value that is not simple ASCII! Value=" + value );
                    }
                }
            }
        }
    }

    /**
     *  A simple filter which filters only those filenames which correspond to the
     *  file extension used.
     */
    public static class WikiFileFilter implements FilenameFilter {
        /**
         *  {@inheritDoc}
         */
        @Override
        public boolean accept( final File dir, final String name ) {
            return name.endsWith( FILE_EXT );
        }
    }

}
