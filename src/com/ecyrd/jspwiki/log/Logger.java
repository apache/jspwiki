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
package com.ecyrd.jspwiki.log;

/**
 * This is just a plain wrapper around the slf4j logging interface.
 * 
 * @author Harry Metske
 * @since 3.0
 */
public interface Logger
{

    void error( String string );

    void warn( String string );

    void info( String string );

    void debug( String string );

    void error( String string, Throwable t );

    void warn( String string, Throwable t );

    void info( String string, Throwable t );

    void debug( String string, Throwable t );

    boolean isErrorEnabled();

    boolean isWarnEnabled();

    boolean isInfoEnabled();

    boolean isDebugEnabled();

}
