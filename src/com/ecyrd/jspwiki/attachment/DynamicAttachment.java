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

import com.ecyrd.jspwiki.WikiEngine;

/**
 *  A DynamicAttachment is an attachment which does not really exist, but is
 *  created dynamically by a JSPWiki component.
 *  <p>
 *  Note that a DynamicAttachment might not be available before it is actually
 *  created by a component (e.g. plugin), and therefore trying to access it
 *  before that component has been invoked, might result in a surprising 404.
 *  <p>
 *  DynamicAttachments are not listed among regular attachments in the current
 *  version.
 *  <p>
 *  Usage:
 *
 *  <pre>
 *
 *  class MyDynamicComponent implements DynamicAttachmentProvider
 *  {
 *  ...
 *
 *     DynamicAttachment destatt = mgr.getDynamicAttachment( destattname );
 *
 *     if( destatt == null )
 *     {
 *         destatt = new DynamicAttachment( context.getEngine(),
 *                                          context.getPage().getName(),
 *                                          destfilename,
 *                                          this );
 *         destatt.setCacheable( false );
 *     }
 *
 *     // This is used to check whether the attachment is modified or not
 *     // so don't forget to update this if your attachment source changes!
 *     // Else JSPWiki will be serving 304s to anyone who asks...
 *
 *     destatt.setLastModified( context.getPage().getLastModified() );
 *     mgr.storeDynamicAttachment( context,  destatt );
 *  ...
 *
 *      public InputStream getAttachmentData( WikiContext context, Attachment att )
 *          throws IOException
 *      {
 *          byte[] bytes = "This is a test".getBytes();
 *
 *          return new ByteArrayInputStream( bytes );
 *      }
 *  </pre>
 *
 *  @author Janne Jalkanen
 *  @since 2.5.34
 */
public class DynamicAttachment extends Attachment
{
    private DynamicAttachmentProvider m_provider  = null;

    /**
     *  Creates a DynamicAttachment.
     *
     *  @param engine
     *  @param parentPage
     *  @param fileName
     *  @param provider The provider which will be used to generate the attachment.
     */
    public DynamicAttachment(WikiEngine engine,
                             String parentPage,
                             String fileName,
                             DynamicAttachmentProvider provider)
    {
        super(engine, parentPage, fileName);
        m_provider = provider;
    }

    /**
     *  Returns the provider which is used to generate this attachment.
     *
     *  @return A Provider component for this attachment.
     */
    public DynamicAttachmentProvider getProvider()
    {
        return m_provider;
    }
}
