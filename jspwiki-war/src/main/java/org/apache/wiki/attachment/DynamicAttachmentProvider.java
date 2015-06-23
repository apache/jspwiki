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

import java.io.IOException;
import java.io.InputStream;

import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.ProviderException;

/**
 *  Provides the data for an attachment.  Please note that there will
 *  be a strong reference retained for the provider for each Attachment
 *  it provides, so do try to keep the object light.  Also, reuse objects
 *  if possible.
 *  <p>
 *  The Provider needs to be thread-safe.
 *
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
    InputStream getAttachmentData( WikiContext context, Attachment att )
        throws ProviderException, IOException;
}
