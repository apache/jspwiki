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
package org.apache.wiki.log;

import java.util.Formatter;

/**
 *  <p>This is just a plain wrapper around the slf4j logging interface with additional
 *  varargs interfaces.  The varargs interfaces are useful because they are faster
 *  to construct than the usual string concatenation, so you won't lose as much time
 *  if you're not logging.  In addition, they allow for easy locale-specific numbers
 *  and dates.</p>
 *  <p>For example, these two logging statements are equivalent, but the latter does
 *  not construct the string <i>every</i> time the log sentence is created, and
 *  also is in general easier to read.</p>
 *  <pre>
 *     log.debug("Found "+list.size()+" elements of type '"+elementType+"'");
 *     log.debug("Found %d elements of type '%s'", list.size, elementType );
 *  </pre>
 * 
 *  @since 3.0
 *  @see java.util.Formatter
 */
public interface Logger
{
    /**
     *  Log an error.
     *  
     *  @param string The string to log.  May contain {@link Formatter} -specific
     *  formatting codes.
     *  @param params An array of parameters.
     */
    void error( String string, Object... params );

    /**
     *  Log an error.
     *  
     *  @param string The string to log.  
     *  @param t , the Throwable to log, including the stacktrace
     */
    void error( String string, Throwable t);

    /**
     *  Log a warning.
     *  
     *  @param string The string to log.  May contain {@link Formatter} -specific
     *  formatting codes.
     *  @param params An array of parameters.
     */
    void warn( String string, Object... params );

    /**
     *  Log an informative message.
     *  
     *  @param string The string to log.  May contain {@link Formatter} -specific
     *  formatting codes.
     *  @param params An array of parameters.
     */
    void info( String string, Object... params );

    /**
     *  Log a debug message.
     *  
     *  @param string The string to log.  May contain {@link Formatter} -specific
     *  formatting codes.
     *  @param params An array of parameters.
     */
    void debug( String string, Object... params );

    /**
     *  Log an error with an exception.
     *  
     *  @param string The string to log.  May contain {@link Formatter} -specific
     *  formatting codes.
     *  @param t The exception to log. 
     *  @param params An array of parameters.
     */
    void error( String string, Throwable t, Object... params );

    /**
     *  Log a warning with an exception.
     *  
     *  @param string The string to log.  May contain {@link Formatter} -specific
     *  formatting codes.
     *  @param t The exception to log. 
     *  @param params An array of parameters.
     */
    void warn( String string, Throwable t, Object... params );

    /**
     *  Log an informative message with an exception.
     *  
     *  @param string The string to log.  May contain {@link Formatter} -specific
     *  formatting codes.
     *  @param t The exception to log. 
     *  @param params An array of parameters.
     */
    void info( String string, Throwable t, Object... params );

    /**
     *  Log a debug message with an exception.
     *  
     *  @param string The string to log.  May contain {@link Formatter} -specific
     *  formatting codes.
     *  @param t The exception to log. 
     *  @param params An array of parameters.
     */
    void debug( String string, Throwable t, Object... params );

    /**
     *  Checks if ERROR messages are enabled for this Logger.
     *  
     *  @return True, if ERROR messages are enabled.
     */

    boolean isErrorEnabled();

    /**
     *  Checks if WARN messages are enabled for this Logger.
     *  
     *  @return True, if WARN messages are enabled.
     */

    boolean isWarnEnabled();

    /**
     *  Checks if INFO messages are enabled for this Logger.
     *  
     *  @return True, if INFO messages are enabled.
     */
    boolean isInfoEnabled();

    /**
     *  Checks if DEBUG messages are enabled for this Logger.
     *  
     *  @return True, if DEBUG messages are enabled.
     */
    boolean isDebugEnabled();

}
