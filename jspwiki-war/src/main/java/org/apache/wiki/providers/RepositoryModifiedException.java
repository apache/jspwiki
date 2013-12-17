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
package org.apache.wiki.providers;

import org.apache.wiki.api.exceptions.ProviderException;

/**
 *  If the provider detects that someone has modified the repository
 *  externally, it should throw this exception.
 *  <p>
 *  Any provider throwing this exception should first clean up any references
 *  to the modified page it has, so that when we call this the next time,
 *  the page is handled as completely, and we don't get the same exception
 *  again.
 *
 *  @since  2.1.25
 */
public class RepositoryModifiedException
    extends ProviderException
{
    private static final long serialVersionUID = 0L;

    protected final String m_page;

    /**
     * Constructs the exception.
     *
     * @param msg The message
     * @param pageName  The name of the page which was modified
     */
    public RepositoryModifiedException( String msg, String pageName )
    {
        super( msg );

        m_page = pageName;
    }

    /**
     *  Return the page name given in the constructor.
     *  
     *  @return The page name.
     */
    public String getPageName()
    {
        return m_page;
    }
}
