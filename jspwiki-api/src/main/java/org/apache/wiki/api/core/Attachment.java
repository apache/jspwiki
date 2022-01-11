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
package org.apache.wiki.api.core;

/**
 *  Describes an attachment.  Attachments are actually derivatives of a WikiPage, since they do actually have a WikiName as well.
 */
public interface Attachment extends Page {

    /**
     *  Returns the file name of the attachment.
     *  
     *  @return A String with the file name.
     */
    String getFileName();

    /**
     *  Sets the file name of this attachment. 
     *  
     *  @param name The name of the attachment.  Must be a legal file name without the path.
     */
    void setFileName( String name );

    /**
     *  Returns the name of the parent of this Attachment, i.e. the page which contains this attachment.
     *  
     *  @return String depicting the parent of the attachment.
     */
    String getParentName();

    /**
     *  Returns true, if this attachment can be cached by the user agent.  By default attachments are cacheable.
     *  
     *  @return False, if the attachment should not be cached by the user agent.
     *  @since 2.5.34
     */
    boolean isCacheable();

    /**
     *  Sets this attachment to be cacheable or not.  This mostly concerns things like DynamicAttachments, but it may be useful for
     *  certain AttachmentProviders as well.
     *  
     *  @param value True or false, depending on whether you want this attachment to be cacheable or not.
     *  @since 2.5.34
     */
    void setCacheable( boolean value );
}
