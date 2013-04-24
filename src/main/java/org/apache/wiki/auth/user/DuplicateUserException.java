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
package org.apache.wiki.auth.user;

import org.apache.wiki.api.exceptions.WikiI18nException;


/**
 * Exception indicating that an identical user already exists in the user database.
 * 
 * @since 2.3
 */
public final class DuplicateUserException extends WikiI18nException
{
    private static final long serialVersionUID = 3258125851953674032L;

    public DuplicateUserException( String message )
    {
        super( message, ( Object[] )null );
    }

    public DuplicateUserException( String message, Object... args )
    {
        super( message, args );
    }

}