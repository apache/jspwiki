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
package org.apache.wiki;

/**
 *  Denotes something really serious going on inside Wiki.
 *  It is a runtime exception so that the API does not need
 *  to be changed, but it's helluva lot better than NullPointerException =).
 *
 *  @since 1.6.9
 */
public class InternalWikiException extends RuntimeException
{
    private static final long          serialVersionUID = 1L;

    /**
     *  Create a new InternalWikiException.
     *
     *  @param msg The Exception message.
     */
    public InternalWikiException( String msg )
    {
        super( msg );
    }

	public InternalWikiException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public InternalWikiException(Throwable cause)
	{
		super(cause);
	}
    
    
}
