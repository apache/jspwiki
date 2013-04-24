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

import org.apache.wiki.WikiEngine;

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
 *  @since 2.5.34
 */
public class DynamicAttachment extends Attachment
{
    private DynamicAttachmentProvider m_provider  = null;

    /**
     *  Creates a DynamicAttachment.
     *
     *  @param engine  The engine which owns this attachment
     *  @param parentPage The page which owns this attachment
     *  @param fileName The filename of the attachment
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
