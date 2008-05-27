/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
