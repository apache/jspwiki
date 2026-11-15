/*
 * Copyright 2025 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wiki.http.filter;

import jakarta.servlet.FilterConfig;
import org.apache.wiki.api.spi.Wiki;

import java.util.Properties;


/**
 * Common {@code jakarta.servlet.Filter} operations. Not expected to be used outside this package.
 */
class FilterOperations {

    /**
     * Retrieves an init value from a filter and, if not present, tries to obtain it from the Wiki properties.
     *
     * @param filterConfig filter configuration.
     * @param initParameter parameter to look into in the {@code filterConfig}
     * @param wikiFilterProp property to ask for on the Wiki properties, which is looked into the {@code jspwiki.http.filters} namespace
     * @return the value of the init param, wiki property, or {@code null} if not present
     */
    static String initValue( final FilterConfig filterConfig, final String initParameter, final String wikiFilterProp ) {
        final Properties props = Wiki.engine().find( filterConfig.getServletContext(), null ).getWikiProperties();
        String configMode = filterConfig.getInitParameter( initParameter );
        if( configMode == null ) {
            configMode = props.getProperty( "jspwiki.http.filters." + wikiFilterProp );
        }
        return configMode;
    }

}
