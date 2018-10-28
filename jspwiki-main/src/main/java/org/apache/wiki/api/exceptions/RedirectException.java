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
package org.apache.wiki.api.exceptions;


/**
 *  This exception may be thrown if a filter wants to reject something and redirect the user elsewhere.
 *
 *  @since 2.1.112
 */
public class RedirectException extends FilterException {
	
    private static final long serialVersionUID = 6626993875873941172L;
    
    private final String m_where;

    /**
     *  Constructs a new RedirectException.
     *  
     *  @param msg The message for the exception
     *  @param redirect The redirect URI.
     */
    public RedirectException( String msg, String redirect ) {
        super( msg );
        m_where = redirect;
    }

    /**
     *  Get the URI for redirection.
     *  
     *  @return The URI given in the constructor.
     */
    public String getRedirect() {
        return m_where;
    }
    
}
