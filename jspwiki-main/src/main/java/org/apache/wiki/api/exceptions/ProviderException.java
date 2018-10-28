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
 *  This exception represents the superclass of all exceptions that providers may throw.  It is okay to throw
 *  it in case you cannot use any of the specific subclasses, in which case the page loading is considered to be
 *  broken, and the user is notified.
 */
public class ProviderException extends WikiException {

    private static final long serialVersionUID = 0L;

    /**
     *  Creates a ProviderException.
     *
     *  @param msg exception message.
     */
    public ProviderException( String msg ) {
        super( msg );
    }

}
