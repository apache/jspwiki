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
package org.apache.wiki.attachment;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.providers.WikiAttachmentProvider;
import org.apache.wiki.util.ClassUtil;
import org.apache.wiki.util.TextUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;


/**
 *  Provides facilities for handling attachments.  All attachment handling goes through this class.
 *  <p>
 *  The AttachmentManager provides a facade towards the current WikiAttachmentProvider that is in use.
 *  It is created by the WikiEngine as a singleton object, and can be requested through the WikiEngine.
 *
 *  @since 1.9.28
 */
public class AttachmentManager
{
    /**
     *  The property name for defining the attachment provider class name.
     */
    public static final String  PROP_PROVIDER = "jspwiki.attachmentProvider";

    /**
     *  The maximum size of attachments that can be uploaded.
     */
    public static final String  PROP_MAXSIZE  = "jspwiki.attachment.maxsize";

    /**
     *  A space-separated list of attachment types which can be uploaded
     */
    public static final String PROP_ALLOWEDEXTENSIONS    = "jspwiki.attachment.allowed";

    /**
     *  A space-separated list of attachment types which cannot be uploaded
     */
    public static final String PROP_FORBIDDENEXTENSIONS = "jspwiki.attachment.forbidden";

    /**
     *  A space-separated list of attachment types which never will open in the browser.
     */
    public static final String PROP_FORCEDOWNLOAD = "jspwiki.attachment.forceDownload";

    /** List of attachment types which are forced to be downloaded */
    private String[] m_forceDownloadPatterns;

    static Logger log = Logger.getLogger( AttachmentManager.class );
    private WikiAttachmentProvider m_provider;
    private WikiEngine             m_engine;
    private CacheManager m_cacheManager = CacheManager.getInstance();

    private Cache m_dynamicAttachments;
    /** Name of the page cache. */
    public static final String CACHE_NAME = "jspwiki.dynamicAttachmentCache";

    /** The capacity of the cache, if you want something else, tweak ehcache.xml. */
    public static final int   DEFAULT_CACHECAPACITY   = 1000;

    /**
     *  Creates a new AttachmentManager.  Note that creation will never fail,
     *  but it's quite likely that attachments do not function.
     *  <p>
     *  <b>DO NOT CREATE</b> an AttachmentManager on your own, unless you really
     *  know what you're doing.  Just use WikiEngine.getAttachmentManager() if
     *  you're making a module for JSPWiki.
     *
     *  @param engine The wikiengine that owns this attachment manager.
     *  @param props  A list of properties from which the AttachmentManager will seek
     *  its configuration.  Typically this is the "jspwiki.properties".
     */

    // FIXME: Perhaps this should fail somehow.
    public AttachmentManager( WikiEngine engine, Properties props )
    {
        String classname;

        m_engine = engine;


        //
        //  If user wants to use a cache, then we'll use the CachingProvider.
        //
        boolean useCache = "true".equals(props.getProperty( PageManager.PROP_USECACHE ));

        if( useCache )
        {
            classname = "org.apache.wiki.providers.CachingAttachmentProvider";
        }
        else
        {
            classname = props.getProperty( PROP_PROVIDER );
        }

        //
        //  If no class defined, then will just simply fail.
        //
        if( classname == null )
        {
            log.info( "No attachment provider defined - disabling attachment support." );
            return;
        }

        //
        //  Create and initialize the provider.
        //
        String cacheName = engine.getApplicationName() + "." + CACHE_NAME;
        try {
            if (m_cacheManager.cacheExists(cacheName)) {
                m_dynamicAttachments = m_cacheManager.getCache(cacheName);
            } else {
                log.info("cache with name " + cacheName + " not found in ehcache.xml, creating it with defaults.");
                m_dynamicAttachments = new Cache(cacheName, DEFAULT_CACHECAPACITY, false, false, 0, 0);
                m_cacheManager.addCache(m_dynamicAttachments);
            }

            Class<?> providerclass = ClassUtil.findClass("org.apache.wiki.providers", classname);

            m_provider = (WikiAttachmentProvider) providerclass.newInstance();

            m_provider.initialize(m_engine, props);
        } catch( ClassNotFoundException e )
        {
            log.error( "Attachment provider class not found",e);
        }
        catch( InstantiationException e )
        {
            log.error( "Attachment provider could not be created", e );
        }
        catch( IllegalAccessException e )
        {
            log.error( "You may not access the attachment provider class", e );
        }
        catch( NoRequiredPropertyException e )
        {
            log.error( "Attachment provider did not find a property that it needed: "+e.getMessage(), e );
            m_provider = null; // No, it did not work.
        }
        catch( IOException e )
        {
            log.error( "Attachment provider reports IO error", e );
            m_provider = null;
        }

        String forceDownload = TextUtil.getStringProperty( props, PROP_FORCEDOWNLOAD, null );

        if( forceDownload != null && forceDownload.length() > 0 )
            m_forceDownloadPatterns = forceDownload.toLowerCase().split("\\s");
        else
            m_forceDownloadPatterns = new String[0];


    }

    /**
     *  Returns true, if attachments are enabled and running.
     *
     *  @return A boolean value indicating whether attachment functionality is enabled.
     */
    public boolean attachmentsEnabled()
    {
        return m_provider != null;
    }

    /**
     *  Gets info on a particular attachment, latest version.
     *
     *  @param name A full attachment name.
     *  @return Attachment, or null, if no such attachment exists.
     *  @throws ProviderException If something goes wrong.
     */
    public Attachment getAttachmentInfo( String name )
        throws ProviderException
    {
        return getAttachmentInfo( name, WikiProvider.LATEST_VERSION );
    }

    /**
     *  Gets info on a particular attachment with the given version.
     *
     *  @param name A full attachment name.
     *  @param version A version number.
     *  @return Attachment, or null, if no such attachment or version exists.
     *  @throws ProviderException If something goes wrong.
     */

    public Attachment getAttachmentInfo( String name, int version )
        throws ProviderException
    {
        if( name == null )
        {
            return null;
        }

        return getAttachmentInfo( null, name, version );
    }

    /**
     *  Figures out the full attachment name from the context and
     *  attachment name.
     *
     *  @param context The current WikiContext
     *  @param attachmentname The file name of the attachment.
     *  @return Attachment, or null, if no such attachment exists.
     *  @throws ProviderException If something goes wrong.
     */
    public Attachment getAttachmentInfo( WikiContext context,
                                         String attachmentname )
        throws ProviderException
    {
        return getAttachmentInfo( context, attachmentname, WikiProvider.LATEST_VERSION );
    }

    /**
     *  Figures out the full attachment name from the context and attachment name.
     *
     *  @param context The current WikiContext
     *  @param attachmentname The file name of the attachment.
     *  @return Attachment, or null, if no such attachment exists.
     *  @throws ProviderException If something goes wrong.
     */
    public String getAttachmentInfoName( WikiContext context,
                                         String attachmentname )
    {
        Attachment att = null;

        try
        {
            att = getAttachmentInfo( context, attachmentname );
        }
        catch( ProviderException e )
        {
            log.warn("Finding attachments failed: ",e);
            return null;
        }

        if( att != null )
        {
            return att.getName();
        }
        else if( attachmentname.indexOf('/') != -1 )
        {
            return attachmentname;
        }

        return null;
    }

    /**
     *  Figures out the full attachment name from the context and
     *  attachment name.
     *
     *  @param context The current WikiContext
     *  @param attachmentname The file name of the attachment.
     *  @param version A particular version.
     *  @return Attachment, or null, if no such attachment or version exists.
     *  @throws ProviderException If something goes wrong.
     */

    public Attachment getAttachmentInfo( final WikiContext context,
                                         String attachmentname,
                                         final int version ) throws ProviderException {
        if( m_provider == null ) {
            return null;
        }

        WikiPage currentPage = null;

        if( context != null ) {
            currentPage = context.getPage();
        }

        //
        //  Figure out the parent page of this attachment.  If we can't find it, we'll assume this refers directly to the attachment.
        //
        final int cutpt = attachmentname.lastIndexOf('/');

        if( cutpt != -1 ) {
            String parentPage = attachmentname.substring(0,cutpt);
            parentPage = MarkupParser.cleanLink( parentPage );
            attachmentname = attachmentname.substring(cutpt+1);

            // If we for some reason have an empty parent page name;
            // this can't be an attachment
            if(parentPage.length() == 0) return null;

            currentPage = m_engine.getPageManager().getPage( parentPage );

            //
            // Go check for legacy name
            //
            // FIXME: This should be resolved using CommandResolver,
            //        not this adhoc way.  This also assumes that the
            //        legacy charset is a subset of the full allowed set.
            if( currentPage == null ) {
                currentPage = m_engine.getPageManager().getPage( MarkupParser.wikifyLink( parentPage ) );
            }
        }

        //
        //  If the page cannot be determined, we cannot possibly find the attachments.
        //
        if( currentPage == null || currentPage.getName().length() == 0 ) {
            return null;
        }

        //
        //  Finally, figure out whether this is a real attachment or a generated attachment.
        //
        Attachment att = getDynamicAttachment( currentPage.getName()+"/"+attachmentname );

        if( att == null ) {
            att = m_provider.getAttachmentInfo( currentPage, attachmentname, version );
        }

        return att;
    }

    /**
     *  Returns the list of attachments associated with a given wiki page.
     *  If there are no attachments, returns an empty Collection.
     *
     *  @param wikipage The wiki page from which you are seeking attachments for.
     *  @return a valid collection of attachments.
     *  @throws ProviderException If there was something wrong in the backend.
     */
    public List< Attachment > listAttachments( WikiPage wikipage ) throws ProviderException {
        if( m_provider == null ) {
            return new ArrayList<>();
        }

        List< Attachment >atts = new ArrayList<>( m_provider.listAttachments( wikipage ) );
        Collections.< Attachment >sort( atts, Comparator.comparing( Attachment::getName, m_engine.getPageManager().getPageSorter() ) );

        return atts;
    }

    /**
     *  Returns true, if the page has any attachments at all.  This is
     *  a convinience method.
     *
     *
     *  @param wikipage The wiki page from which you are seeking attachments for.
     *  @return True, if the page has attachments, else false.
     */
    public boolean hasAttachments( WikiPage wikipage )
    {
        try
        {
            return listAttachments( wikipage ).size() > 0;
        }
        catch( Exception e ) {}

        return false;
    }

    /**
     *  Check if attachement link should force a download iso opening the attachment in the browser.
     *
     *  @param name  Name of attachment to be checked
     *  @return true, if the attachment should be downloaded when clicking the link
     *  @since 2.11.0 M4
    */
    public boolean forceDownload( String name )
    {
        if( name == null || name.length() == 0 ) return false;

        name = name.toLowerCase();

        if( name.indexOf('.') == -1) return true;  //force download on attachments without extension or type indication

        for( int i = 0; i < m_forceDownloadPatterns.length; i++ )
        {
            if( name.endsWith(m_forceDownloadPatterns[i]) && m_forceDownloadPatterns[i].length() > 0 )
                return true;
        }

        return false;
    }

    /**
     *  Finds a (real) attachment from the repository as a stream.
     *
     *  @param att Attachment
     *  @return An InputStream to read from.  May return null, if
     *          attachments are disabled.
     *  @throws IOException If the stream cannot be opened
     *  @throws ProviderException If the backend fails due to some other reason.
     */
    public InputStream getAttachmentStream( Attachment att )
        throws IOException,
               ProviderException
    {
        return getAttachmentStream( null, att );
    }

    /**
     *  Returns an attachment stream using the particular WikiContext.  This method
     *  should be used instead of getAttachmentStream(Attachment), since it also allows
     *  the DynamicAttachments to function.
     *
     *  @param ctx The Wiki Context
     *  @param att The Attachment to find
     *  @return An InputStream.  May return null, if attachments are disabled.  You must
     *          take care of closing it.
     *  @throws ProviderException If the backend fails due to some reason
     *  @throws IOException If the stream cannot be opened
     */
    public InputStream getAttachmentStream( WikiContext ctx, Attachment att )
        throws ProviderException, IOException
    {
        if( m_provider == null )
        {
            return null;
        }

        if( att instanceof DynamicAttachment )
        {
            return ((DynamicAttachment)att).getProvider().getAttachmentData( ctx, att );
        }

        return m_provider.getAttachmentData( att );
    }



    /**
     *  Stores a dynamic attachment.  Unlike storeAttachment(), this just stores
     *  the attachment in the memory.
     *
     *  @param ctx A WikiContext
     *  @param att An attachment to store
     */
    public void storeDynamicAttachment( WikiContext ctx, DynamicAttachment att )
    {
        m_dynamicAttachments.put(new Element(att.getName(), att));
    }

    /**
     *  Finds a DynamicAttachment.  Normally, you should just use getAttachmentInfo(),
     *  since that will find also DynamicAttachments.
     *
     *  @param name The name of the attachment to look for
     *  @return An Attachment, or null.
     *  @see #getAttachmentInfo(String)
     */

    public DynamicAttachment getDynamicAttachment(String name) {
        Element element = m_dynamicAttachments.get(name);
        if (element != null) {
            return (DynamicAttachment) element.getObjectValue();
        } else {
            //
            //  Remove from cache, it has expired.
            //
            m_dynamicAttachments.put(new Element(name, null));

            return null;
        }
    }

    /**
     *  Stores an attachment that lives in the given file. If the attachment did not exist previously, this method will create it.
     *  If it did exist, it stores a new version.
     *
     *  @param att Attachment to store this under.
     *  @param source A file to read from.
     *
     *  @throws IOException If writing the attachment failed.
     *  @throws ProviderException If something else went wrong.
     */
    public void storeAttachment( final Attachment att, final File source ) throws IOException, ProviderException {
        try( final FileInputStream in = new FileInputStream( source ) ) {
            storeAttachment( att, in );
        }
    }

    /**
     *  Stores an attachment directly from a stream. If the attachment did not exist previously, this method will create it.
     *  If it did exist, it stores a new version.
     *
     *  @param att Attachment to store this under.
     *  @param in  InputStream from which the attachment contents will be read.
     *
     *  @throws IOException If writing the attachment failed.
     *  @throws ProviderException If something else went wrong.
     */
    public void storeAttachment( final Attachment att, final InputStream in ) throws IOException, ProviderException {
        if( m_provider == null ) {
            return;
        }

        //  Checks if the actual, real page exists without any modifications
        //  or aliases.  We cannot store an attachment to a non-existent page.
        if( !m_engine.getPageManager().pageExists( att.getParentName() ) ) {
            // the caller should catch the exception and use the exception text as an i18n key
            throw new ProviderException( "attach.parent.not.exist" );
        }

        m_provider.putAttachmentData( att, in );
        m_engine.getReferenceManager().updateReferences( att.getName(), new ArrayList<>() );

        final WikiPage parent = new WikiPage( m_engine, att.getParentName() );
        m_engine.getReferenceManager().updateReferences( parent );
        m_engine.getSearchManager().reindexPage( att );
    }

    /**
     *  Returns a list of versions of the attachment.
     *
     *  @param attachmentName A fully qualified name of the attachment.
     *
     *  @return A list of Attachments.  May return null, if attachments are
     *          disabled.
     *  @throws ProviderException If the provider fails for some reason.
     */
    public List< Attachment > getVersionHistory( final String attachmentName ) throws ProviderException {
        if( m_provider == null ) {
            return null;
        }

        final Attachment att = getAttachmentInfo( null, attachmentName );

        if( att != null ) {
            return m_provider.getVersionHistory( att );
        }

        return null;
    }

    /**
     *  Returns a collection of Attachments, containing each and every attachment
     *  that is in this Wiki.
     *
     *  @return A collection of attachments.  If attachments are disabled, will
     *          return an empty collection.
     *  @throws ProviderException If something went wrong with the backend
     */
    public Collection<Attachment> getAllAttachments() throws ProviderException {
        if( attachmentsEnabled() ) {
            return m_provider.listAllChanged( new Date(0L) );
        }

        return new ArrayList<>();
    }

    /**
     *  Returns the current attachment provider.
     *
     *  @return The current provider.  May be null, if attachments are disabled.
     */
    public WikiAttachmentProvider getCurrentProvider()
    {
        return m_provider;
    }

    /**
     *  Deletes the given attachment version.
     *
     *  @param att The attachment to delete
     *  @throws ProviderException If something goes wrong with the backend.
     */
    public void deleteVersion( Attachment att )
        throws ProviderException
    {
        if( m_provider == null ) return;

        m_provider.deleteVersion( att );
    }

    /**
     *  Deletes all versions of the given attachment.
     *  @param att The Attachment to delete.
     *  @throws ProviderException if something goes wrong with the backend.
     */
    // FIXME: Should also use events!
    public void deleteAttachment( Attachment att )
        throws ProviderException
    {
        if( m_provider == null ) return;

        m_provider.deleteAttachment( att );

        m_engine.getSearchManager().pageRemoved( att );

        m_engine.getReferenceManager().clearPageEntries( att.getName() );

    }

    /**
     *  Validates the filename and makes sure it is legal.  It trims and splits
     *  and replaces bad characters.
     *
     *  @param filename
     *  @return A validated name with annoying characters replaced.
     *  @throws WikiException If the filename is not legal (e.g. empty)
     */
    static String validateFileName( String filename )
        throws WikiException
    {
        if( filename == null || filename.trim().length() == 0 )
        {
            log.error("Empty file name given.");

            // the caller should catch the exception and use the exception text as an i18n key
            throw new WikiException(  "attach.empty.file" );
        }

        //
        //  Should help with IE 5.22 on OSX
        //
        filename = filename.trim();

        // If file name ends with .jsp or .jspf, the user is being naughty!
        if( filename.toLowerCase().endsWith( ".jsp" ) || filename.toLowerCase().endsWith(".jspf") )
        {
            log.info( "Attempt to upload a file with a .jsp/.jspf extension.  In certain cases this " +
                      "can trigger unwanted security side effects, so we're preventing it." );
            //
            // the caller should catch the exception and use the exception text as an i18n key
            throw new WikiException(  "attach.unwanted.file"  );
        }

        //
        //  Some browser send the full path info with the filename, so we need
        //  to remove it here by simply splitting along slashes and then taking the path.
        //

        String[] splitpath = filename.split( "[/\\\\]" );
        filename = splitpath[splitpath.length-1];

        //
        //  Remove any characters that might be a problem. Most
        //  importantly - characters that might stop processing
        //  of the URL.
        //
        filename = StringUtils.replaceChars( filename, "#?\"'", "____" );

        return filename;
    }
}
