package com.ecyrd.jspwiki.attachment;

import java.io.IOException;
import java.io.InputStream;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *  Provides the data for an attachment.  Please note that there will
 *  be a strong reference retained for the provider for each Attachment
 *  it provides, so do try to keep the object light.  Also, reuse objects
 *  if possible.
 *  <p>
 *  The Provider needs to be thread-safe.
 *  
 *  @author Janne Jalkanen
 *  @since  2.5.34
 */
public interface DynamicAttachmentProvider
{
    /**
     *  Returns a stream of data for this attachment.  The stream will be
     *  closed by AttachmentServlet.
     *  
     *  @param context A Wiki Context
     *  @param att The Attachment for which the data should be received.
     *  @return InputStream for the data.
     *  @throws ProviderException If something goes wrong internally
     *  @throws IOException If something goes wrong when reading the data
     */
    public InputStream getAttachmentData( WikiContext context, Attachment att ) 
        throws ProviderException, IOException;
}
