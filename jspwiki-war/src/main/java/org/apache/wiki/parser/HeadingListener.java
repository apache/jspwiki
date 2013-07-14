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
package org.apache.wiki.parser;

import org.apache.wiki.WikiContext;

/**
 *  Provides a listener interface for headings.  This is used in parsing,
 *  and e.g. the TableOfContents is built using this listener.
 */
public interface HeadingListener
{
    /**
     *  Is called whenever a heading is encountered in the stream.
     *  
     *  @param context The WikiContext
     *  @param hd The heading which was just encountered.
     */
    void headingAdded( WikiContext context, Heading hd );
}
