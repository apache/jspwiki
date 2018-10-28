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

/**
 *  Thrown in some error situations where a WikiPrincipal object does not exist.
 *  @since 2.3
 */
public final class NoSuchPrincipalException
    extends WikiSecurityException
{
    private static final long serialVersionUID = 3257849895976186169L;

    /**
     * Constructs a new exception object with a supplied message.
     * @param msg the message
     */
    public NoSuchPrincipalException( String msg )
    {
        super(msg);
    }
}
