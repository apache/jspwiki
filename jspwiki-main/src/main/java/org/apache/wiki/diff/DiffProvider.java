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
package org.apache.wiki.diff;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;

import java.io.IOException;
import java.util.Properties;


/**
 *  Provides an SPI for creating a diff between two page versions.
 */
public interface DiffProvider extends WikiProvider {
    /**
     * The return string is to be XHTML compliant ready to display html.  No further
     * processing of this text will be done by the wiki engine.
     * 
     * @return An XHTML diff.
     * @param context The Wiki Context
     * @param oldWikiText the old text
     * @param newWikiText the new text
     */
    String makeDiffHtml( WikiContext context, String oldWikiText, String newWikiText );
    
    /**
     *  If there is no diff provider set, this provider will work instead.
     */
    class NullDiffProvider implements DiffProvider {
        /**
         *  {@inheritDoc}
         */
        public String makeDiffHtml( final WikiContext ctx, final String oldWikiText, final String newWikiText ) {
            return "You are using the NullDiffProvider, check your properties file.";
        }

        /**
         *  {@inheritDoc}
         */
        public void initialize( final WikiEngine engine, final Properties properties ) throws NoRequiredPropertyException, IOException {
        }

        /**
         *  {@inheritDoc}
         */
        public String getProviderInfo()
        {
            return "NullDiffProvider";
        }
    }
    
}
