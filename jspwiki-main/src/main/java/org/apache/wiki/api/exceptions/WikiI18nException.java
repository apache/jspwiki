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
 *  A generic Wiki exception, meant to carry key & args to construct i18n messages to show meaningful messages to 
 *  end-user. Classes and/or JSPs on the web tier are responsible of constructing the appropiate message.
 *
 *  @since 2.9.1
 */
public class WikiI18nException extends WikiException {
	
    private static final long serialVersionUID = -1412916016409728838L;
    
    /** arguments needed to construct the i18n message associated with the exception. */
    protected final Object[] args;

    /**
     *  Constructs an exception.
     *  
     *  @param key the key corresponding to the i18n message in the exception.
     *  @param args arguments needed to construct the i18n message associated with the exception.
     */
    public WikiI18nException( String key, Object... args ) {
        super( key );
        this.args = args;
    }
    
    /**
     * getter. 
     * 
     * @return arguments needed to construct the i18n message associated with the exception.
     */
    public Object[] getArgs() {
        return args;
    }

}
