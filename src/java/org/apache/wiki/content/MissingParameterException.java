/* 
    JSPWiki - a JSP-based WikiWiki clone.

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
package org.apache.wiki.content;

import org.apache.wiki.api.WikiException;

/**
 * Exception indicating that a required request parameter was not supplied, for example
 * to SpamFilter.
 */
public class MissingParameterException extends WikiException
{
    private static final long serialVersionUID = 8665543487480429651L;

    /**
     * Constructs a new MissingParameterException
     * @param message the exception message
     * @param cause the cause of the exception, which may be <code>null</code>
     */
    public MissingParameterException( String message, Throwable cause )
    {
        super( message, cause );
    }
    
}
