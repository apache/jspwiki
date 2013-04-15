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
package org.apache.wiki.util;

import org.apache.wiki.*;


/**
 *  Contains useful utilities for JSPWiki blogging functionality.
 *
 *  @since 2.2.
 */
public final class BlogUtil
{
    /**
     * Private constructor to prevent direct instantiation.
     */
    private BlogUtil()
    {
    }
    
    /** Wiki variable storing the blog's name. */
    public static final String VAR_BLOGNAME = "blogname";

    /**
     * Figure out a site name for a feed.
     * @param context the wiki context
     * @return the site name
     */
    public static String getSiteName( WikiContext context )
    {
        WikiEngine engine = context.getEngine();

        String blogname = null;

        try
        {
            blogname = engine.getVariableManager().getValue( context, VAR_BLOGNAME );
        }
        catch( NoSuchVariableException e ) {}

        if( blogname == null )
        {
            blogname = engine.getApplicationName()+": "+context.getPage().getName();
        }

        return blogname;
    }
}
