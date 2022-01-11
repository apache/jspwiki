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
package org.apache.wiki.api.engine;

import org.apache.wiki.api.core.Context;


/**
 * <p>Rendering routines that all JSPWiki public API implementations should provide.</p>
 *
 * <p>A {@code RenderApi} should be obtained from {@code Engine#getManager( RenderApi.class )}.</p>
 */
public interface RenderApi {

    /**
     *  Converts raw page data to HTML.
     *
     *  @param pagedata Raw page data to convert to HTML
     *  @param context  The WikiContext in which the page is to be rendered
     *  @return Rendered page text
     */
    String textToHTML( Context context, String pagedata );

}
