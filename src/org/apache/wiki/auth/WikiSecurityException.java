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
package org.apache.wiki.auth;

import org.apache.wiki.api.exceptions.WikiException;

/**
 *  Indicates an authentication or authorization
 *  error or exception.
 *
 *  @since 2.0
 */
public class WikiSecurityException
    extends WikiException
{
    private static final long serialVersionUID = 3617293441285764405L;

    /**
     *  Constructs an exception.
     *  @param msg the message to supply to the exception
     */
    public WikiSecurityException( String msg )
    {
        super( msg );
    }
    
    /**
     *  Constructs an exception with a supplied cause.
     *  @param msg the message to supply to the exception
     *  @param cause the cause
     */
    public WikiSecurityException( String msg, Throwable cause )
    {
        super( msg, cause );
    }
}
