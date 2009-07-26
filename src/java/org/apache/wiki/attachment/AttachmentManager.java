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
package org.apache.wiki.attachment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.lang.StringUtils;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.content.jcr.JCRWikiPage;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.util.TextUtil;

/**
 *  <p>Provides facilities for handling attachments.  All attachment
 *  handling goes through this class.</p>
 *  <p>The AttachmentManager provides a facade towards the current WikiAttachmentProvider
 *  that is in use.  It is created by the WikiEngine as a singleton object, and
 *  can be requested through the WikiEngine.</p>
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
    public static final String PROP_FORDBIDDENEXTENSIONS = "jspwiki.attachment.forbidden";

    static Logger log = LoggerFactory.getLogger( AttachmentManager.class );
    private WikiEngine             m_engine;

    private CacheManager           m_cachingManager = CacheManager.getInstance();
    private Cache                  m_dynamicAttachments;
    private static final String    CACHE_NAME = "jspwiki.dynamicAttachmentCache";
    
    /**
     *  List of attachment types which are allowed.
     */
    private String[] m_allowedPatterns;

    /**
     *  List of attachment types which are forbidden.
     */
    private String[] m_forbiddenPatterns;
    
    /**
     *  <p>Creates a new AttachmentManager.  Note that creation will never fail,
     *  but it's quite likely that attachments do not function.
     *  </p>
     *  <p><b>DO NOT CREATE</b> an AttachmentManager on your own, unless you really
     *  know what you're doing.  Just use WikiEngine.getAttachmentManager() if
     *  you're making a module for JSPWiki.</p>
     *
     *  @param engine The WikiEngine that owns this attachment manager
     *  @param props  A list of properties from which the AttachmentManager will seek
     *  its configuration.  Typically this is the "jspwiki.properties".
     */
    // FIXME: Perhaps this should fail somehow.
    public AttachmentManager( WikiEngine engine, Properties props )
    {
        m_engine = engine;
        
        m_dynamicAttachments = m_cachingManager.getCache( CACHE_NAME );
        if( m_dynamicAttachments == null )
        {
            m_dynamicAttachments = new Cache( CACHE_NAME, Integer.MAX_VALUE, false, true, 3600, 3600 );
            m_cachingManager.addCache( m_dynamicAttachments );
        }
        
        initFileRestrictions();
    }

    /**
     *  Returns true, if attachments are enabled and running.
     *
     *  @return A boolean value indicating whether attachment functionality is enabled.
     */
    public boolean attachmentsEnabled()
    {
        return true; // ALways enabled in 3.0
    }

    /**
     *  Gets info on a particular attachment, latest version.
     *
     *  @param name A full attachment name
     *  @return the attachment
     *  @throws ProviderException If something goes wrong
     *  @throws PageNotFoundException if no such attachment or version exists
     */
    public Attachment getAttachmentInfo( String name )
        throws ProviderException, PageNotFoundException
    {
        return m_engine.getContentManager().getPage( WikiPath.valueOf( name ) );
    }

    /**
     *  Gets info on a particular attachment with the given version.
     *
     *  @param name A full attachment name
     *  @param version A version number
     *  @return the attachment
     *  @throws ProviderException If something goes wrong
     *  @throws PageNotFoundException if no such attachment or version exists
     */

    public Attachment getAttachmentInfo( String name, int version )
        throws ProviderException, PageNotFoundException
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
     *  @param attachmentname The file name of the attachment
     *  @return the attachment
     *  @throws ProviderException If something goes wrong
     *  @throws PageNotFoundException if no such attachment or version exists
     */

    public Attachment getAttachmentInfo( WikiContext context,
                                         String attachmentname )
        throws ProviderException, PageNotFoundException
    {
        return getAttachmentInfo( context, attachmentname, WikiProvider.LATEST_VERSION );
    }

    /**
     *  Figures out the full attachment name from the context and
     *  attachment name.
     *
     *  @param context The current WikiContext
     *  @param attachmentname The file name of the attachment
     *  @param version A particular version
     *  @return the attachment
     *  @throws ProviderException If something goes wrong
     *  @throws PageNotFoundException if no such attachment or version exists
     */

    public Attachment getAttachmentInfo( WikiContext context,
                                         String attachmentname,
                                         int version )
        throws ProviderException, PageNotFoundException
    {
        WikiPage currentPage = null;

        if( context != null )
        {
            currentPage = context.getPage();
        }
        
        if ( currentPage == null )
        {
            return null;
        }

        WikiPath name = currentPage.getPath().resolve( attachmentname );
        
        Attachment att;

        att = getDynamicAttachment( name );

        if( att == null )
        {
            att = m_engine.getContentManager().getPage( name, version );
            if ( att != null && att.isAttachment() )
            {
                return att;
            }
        }

        return null;
    }

    /**
     * Returns the list of attachments associated with a given wiki page.
     * If there are no attachments, returns an empty List. The order of
     * the list is not defined.
     * 
     * @param wikipage
     *            the wiki page from which you are seeking attachments for
     * @return an unordered List of attachments
     * @throws ProviderException
     *             if there was something wrong in the backend
     */
    public List<WikiPage> listAttachments( WikiPage wikipage )
        throws ProviderException
    {
        return (List<WikiPage>) listAttachmentsImpl( new ArrayList<WikiPage>(), wikipage );
    }

    /**
     * Returns the sorted set of attachments associated with a given wiki page.
     * If there are no attachments, returns an empty Set. The set is in page
     * name order.
     * 
     * @param wikipage
     *            the wiki page from which you are seeking attachments for
     * @return a SortedSet of attachments
     * @throws ProviderException
     *             if there was something wrong in the backend
     */
    public SortedSet<WikiPage> listAttachmentsSorted( WikiPage wikipage )
        throws ProviderException
    {
        return (SortedSet<WikiPage>) listAttachmentsImpl( new TreeSet<WikiPage>(), wikipage );
    }

    /**
     *  Internal routine to find the attachments associated with a given wiki page.
     *  Depends on the passed Collection for ordering and such.
     *
     *  @param result Collection used to hold the resulting attachments
     *  @param wikipage the wiki page from which you are seeking attachments for
     *  @return the passed in Collection, filled with the attachments
     *  @throws ProviderException if there was something wrong in the backend
     */
    private Collection<WikiPage> listAttachmentsImpl( Collection<WikiPage> result, WikiPage wikipage )
        throws ProviderException
    {
        List<WikiPage> children = wikipage.getChildren();
        
        for( WikiPage p : children )
        {
            JCRWikiPage jwp = (JCRWikiPage)p;
            if( jwp.isAttachment() )
                result.add( jwp );
        }
        
        return result;
    }

    /**
     *  Returns true, if the page has any attachments at all.  This is
     *  a convenience method.
     *
     *
     *  @param wikipage The wiki page from which you are seeking attachments for.
     *  @return True, if the page has attachments, else false.
     */
    public boolean hasAttachments( WikiPage wikipage )
    {
        try
        {
            List<WikiPage> children = wikipage.getChildren();
            for( WikiPage p : children )
            {
                JCRWikiPage jwp = (JCRWikiPage)p;
                if( jwp.isAttachment() )
                    return true;
            }
        }
        catch( ProviderException e )
        {
            // Do nothing because there's nothing we can do
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
        throws IOException, ProviderException
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
        if( att instanceof DynamicAttachment )
        {
            return ((DynamicAttachment)att).getProvider().getAttachmentData( ctx, att );
        }

        return att.getContentAsStream();
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
        m_dynamicAttachments.put( new Element( att.getName(), att) );
    }

    /**
     *  Finds a DynamicAttachment.  Normally, you should just use getAttachmentInfo(),
     *  since that will find also DynamicAttachments.
     *
     *  @param name The name of the attachment to look for
     *  @return An Attachment, or null.
     *  @see #getAttachmentInfo(String)
     */

    public DynamicAttachment getDynamicAttachment( WikiPath name )
    {
        Element att = m_dynamicAttachments.get( name.toString() );
        
        if( att != null )
        {
            return (DynamicAttachment) att.getObjectValue();
        }

        return null;
    }

    /**
     *  Stores an attachment that lives in the given file.
     *  If the attachment did not exist previously, this method
     *  will create it.  If it did exist, it stores a new version.
     *
     *  @param att Attachment to store this under.
     *  @param source A file to read from.
     *
     *  @throws IOException If writing the attachment failed.
     *  @throws ProviderException If something else went wrong.
     */
    public void storeAttachment( Attachment att, File source )
        throws IOException,
               ProviderException
    {
        FileInputStream in = null;

        try
        {
            in = new FileInputStream( source );
            storeAttachment( att, in );
        }
        finally
        {
            if( in != null ) in.close();
        }
    }

    /**
     *  Stores an attachment directly from a stream.
     *  If the attachment did not exist previously, this method
     *  will create it.  If it did exist, it stores a new version.
     *
     *  @param att Attachment to store this under.
     *  @param in  InputStream from which the attachment contents will be read.
     *
     *  @throws IOException If writing the attachment failed.
     *  @throws ProviderException If something else went wrong.
     */
    public void storeAttachment( Attachment att, InputStream in )
        throws IOException,
               ProviderException
    {
        att.setContent( in );
        att.save();
    }

    /**
     *  Returns a list of versions of the attachment.
     *
     *  @param attachmentName A fully qualified name of the attachment.
     *
     *  @return A list of Attachments.  May return null, if attachments are
     *          disabled.
     *  @throws ProviderException If the provider fails for some reason.
     *  @throws PageNotFoundException if no such attachment or version exists
     */
    public List<WikiPage> getVersionHistory( String attachmentName )
        throws ProviderException, PageNotFoundException
    {
        return m_engine.getContentManager().getVersionHistory( WikiPath.valueOf(attachmentName) );
    }

    /**
     *  Deletes the given attachment version. If the attachment is
     *  not found in the back-end because it does not exist, this method
     *  completes silently.
     *
     *  @param att The attachment to delete
     *  @throws ProviderException If something goes wrong with the backend.
     */
    public void deleteVersion( WikiPage att ) throws ProviderException
    {
        m_engine.getContentManager().deleteVersion( att );
    }

    /**
     *  Deletes all versions of the given attachment. If the attachment is
     *  not found in the back-end because it does not exist, this method
     *  completes silently.
     *  @param att The Attachment to delete.
     *  @throws ProviderException if something goes wrong with the backend
     */
    public void deleteAttachment( Attachment att ) throws ProviderException
    {
        m_engine.getContentManager().deletePage( att );
    }

    /**
     *  Cleans an attachment filename by trimming and replacing bad characters.
     *  If a file path is supplied, only the trailing file name will be returned.
     *  
     *  @param filename the filename to cleanse
     *  @return A validated name with annoying characters replaced.
     *  @throws WikiException If the filename is not legal (e.g. empty)
     */
    public static String cleanFileName( String filename )
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
            log.info( "Attempt to upload a file with a .jsp/.jspf extension.  In certain cases this" +
                      " can trigger unwanted security side effects, so we're preventing it." );
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

    /**
     * Determines whether a supplied attachment is allowed to be
     * uploaded, based on the file name. The list of files types allowed for uploading
     * are contained in <code>jspwiki.properties</code> property
     * {@link #PROP_ALLOWEDEXTENSIONS}. The list of denied file types are
     * contained in {@link #PROP_FORDBIDDENEXTENSIONS}. If an extension is
     * included in both lists, the forbidden list wins.
     * @param name the proposed file name
     * @return <code>true</code> if a supplied attachment file name is
     * allowed to be uploaded; <code>false</code> otherwise.
     */
    public boolean isFileTypeAllowed( String name )
    {
        if( name == null || name.length() == 0 ) return false;

        name = name.toLowerCase();

        for( int i = 0; i < m_forbiddenPatterns.length; i++ )
        {
            if( name.endsWith(m_forbiddenPatterns[i]) && m_forbiddenPatterns[i].length() > 0 )
                return false;
        }

        for( int i = 0; i < m_allowedPatterns.length; i++ )
        {
            if( name.endsWith(m_allowedPatterns[i]) && m_allowedPatterns[i].length() > 0 )
                return true;
        }

        return m_allowedPatterns.length == 0;
    }
    
    private void initFileRestrictions()
    {
        Properties props = m_engine.getWikiProperties();

        String allowed = TextUtil.getStringProperty( props,
                                                     AttachmentManager.PROP_ALLOWEDEXTENSIONS,
                                                     null );

        if( allowed != null && allowed.length() > 0 )
        {
            m_allowedPatterns = allowed.toLowerCase().split("\\s");
        }
        else
        {
            m_allowedPatterns = new String[0];
        }

        String forbidden = TextUtil.getStringProperty( props,
                                                       AttachmentManager.PROP_FORDBIDDENEXTENSIONS,
                                                       null );

        if( forbidden != null && forbidden.length() > 0 )
        {
            m_forbiddenPatterns = forbidden.toLowerCase().split("\\s");
        }
        else
        {
            m_forbiddenPatterns = new String[0];
        }

        log.debug( "AttachmentManager initialized with allowed/denied file patterns." );
    }
}
