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

import org.slf4j.Logger;

/**
 * An adapter class for slf4j Logging.
 * 
 * @author Harry Metske
 * @since 3.0
 */
public class LoggerImpl implements com.ecyrd.jspwiki.log.Logger
{
    Logger slf4jLogger = null;

    /**
     * @param loggerName
     */
    protected LoggerImpl( String loggerName )
    {
        slf4jLogger = org.slf4j.LoggerFactory.getLogger( loggerName );
    }

    public void error( String string )
    {
        slf4jLogger.error( string );
    }

    public void warn( String string )
    {
        slf4jLogger.warn( string );
    }

    public void info( String string )
    {
        slf4jLogger.info( string );
    }

    public void debug( String arg0 )
    {
        slf4jLogger.debug( arg0 );
    }

    public void error( String string, Throwable t )
    {
        slf4jLogger.error( string, t );
    }

    public void warn( String string, Throwable t )
    {
        slf4jLogger.warn( string, t );
    }

    public void info( String string, Throwable t )
    {
        slf4jLogger.info( string, t );
    }

    public void debug( String string, Throwable t )
    {
        slf4jLogger.debug( string, t );
    }

    public boolean isErrorEnabled()
    {
        return slf4jLogger.isErrorEnabled();
    }

    public boolean isWarnEnabled()
    {
        return slf4jLogger.isWarnEnabled();
    }

    public boolean isInfoEnabled()
    {
        return slf4jLogger.isInfoEnabled();
    }

    public boolean isDebugEnabled()
    {
        return slf4jLogger.isDebugEnabled();
    }
}
