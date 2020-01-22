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
package org.apache.wiki.url;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;

import javax.servlet.http.HttpServletRequest;
import java.util.Properties;

/**
 *  A specific URL constructor that returns easy-to-grok URLs for VIEW and ATTACH contexts, but goes through JSP pages otherwise.
 * 
 *  @since 2.2
 */
public class ShortViewURLConstructor extends ShortURLConstructor {

    /**
     *  {@inheritDoc}
     */
    public void initialize( final WikiEngine engine, final Properties properties ) {
        super.initialize( engine, properties );
    }
    
    private String makeURL( final String context, final String name ) {
        final String viewurl = "%p" + m_urlPrefix + "%n";
        if( context.equals( WikiContext.VIEW ) ) {
            if( name == null ) {
                return doReplacement("%u","" );
            }
            return doReplacement( viewurl, name );
        }

        return doReplacement( DefaultURLConstructor.getURLPattern( context, name ), name );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String makeURL( final String context, final String name, String parameters ) {
        if( parameters != null && parameters.length() > 0 ) {
            if( context.equals( WikiContext.ATTACH ) || context.equals( WikiContext.VIEW ) || name == null ) {
                parameters = "?" + parameters;
            } else if( context.equals(WikiContext.NONE) ) {
                parameters = (name.indexOf('?') != -1 ) ? "&amp;" : "?" + parameters;
            } else {
                parameters = "&amp;" + parameters;
            }
        } else {
            parameters = "";
        }
        return makeURL( context, name ) + parameters;
    }
    
    /**
     *   Since we're only called from WikiServlet, where we get the VIEW requests, we can safely return this.
     *   
     * @param request The HTTP Request that was used to end up in this page.
     * @return always returns "Wiki.jsp"
     */
    @Override
    public String getForwardPage( final HttpServletRequest request )
    {        
        return "Wiki.jsp";
    }

}
