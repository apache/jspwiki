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
package org.apache.wiki.api.spi;

import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;


/**
 * SPI used to locate and provide {@link Page} and {@link Attachment} instances.
 */
public interface ContentsSPI {

    /**
     * Creates a new {@link Attachment}. The final name of the attachment will be a synthesis of the parent page name and the file name.
     *
     * @param engine     The Engine which is hosting this attachment.
     * @param parentPage The page which will contain this attachment.
     * @param fileName   The file name for the attachment.
     * @return new {@link Attachment} instance.
     */
    Attachment attachment( Engine engine, String parentPage, String fileName );

    /**
     * Creates a {@link Page} instance.
     *
     * @param engine The Engine that owns this page.
     * @param name   The name of the page.
     * @return new {@link Page} instance.
     */
    Page page( Engine engine, String name );

}
