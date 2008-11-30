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

import java.util.Formatter;

import org.slf4j.Logger;

/**
 * An adapter class for slf4j Logging.
 * 
 * @author Harry Metske
 * @since 3.0
 */
public class LoggerImpl implements com.ecyrd.jspwiki.log.Logger
{
    Logger m_slf4jLogger = null;
    
    /**
     * @param loggerName The name of the SFL4J Logger to find
     */
    protected LoggerImpl( String loggerName )
    {
        m_slf4jLogger = org.slf4j.LoggerFactory.getLogger( loggerName );
    }

    /** {@inheritDoc} */
    
    public void error( String string, Object... params )
    {
        if( isErrorEnabled() )
        {
            if( params.length > 0 )
                string = String.format( string, params );
        
            m_slf4jLogger.error( string );
        }
    }

    /** {@inheritDoc} */
    public void warn( String string, Object... params )
    {
        if( isWarnEnabled() )
        {
            if( params.length > 0 )
                string = String.format( string, params );
        
            m_slf4jLogger.warn( string );
        }
    }

    /** {@inheritDoc} */
    public void info( String string, Object... params )
    {
        if( isInfoEnabled() )
        {
            if( params.length > 0 )
                string = String.format( string, params );
        
            m_slf4jLogger.info( string );
        }
    }

    /** {@inheritDoc} */
    public void debug( String string, Object... params )
    {
        if( isDebugEnabled() )
        {
            if( params.length > 0 )
                string = String.format( string, params );
        
            m_slf4jLogger.debug( string );
        }
    }

    /** {@inheritDoc} */
    public void error( String string, Throwable t, Object... params )
    {
        if( isErrorEnabled() )
        {
            if( params.length > 0 )
                string = String.format( string, params );
        
            m_slf4jLogger.error( string, t );
        }
    }

    /** {@inheritDoc} */
    public void warn( String string, Throwable t, Object... params )
    {
        if( isWarnEnabled() )
        {
            if( params.length > 0 )
                string = String.format( string, params );
        
            m_slf4jLogger.warn( string, t );
        }
    }

    /** {@inheritDoc} */
    public void info( String string, Throwable t, Object... params )
    {
        if( isInfoEnabled() )
        {
            if( params.length > 0 )
                string = String.format( string, params );

            m_slf4jLogger.info( string, t );
        }
    }

    /** {@inheritDoc} */
    public void debug( String string, Throwable t, Object... params )
    {
        if( isDebugEnabled() )
        {
            if( params.length > 0 )
                string = String.format( string, params );
        
            m_slf4jLogger.debug( string, t );
        }
    }

    /** {@inheritDoc} */
    public boolean isErrorEnabled()
    {
        return m_slf4jLogger.isErrorEnabled();
    }

    /** {@inheritDoc} */
    public boolean isWarnEnabled()
    {
        return m_slf4jLogger.isWarnEnabled();
    }

    /** {@inheritDoc} */
    public boolean isInfoEnabled()
    {
        return m_slf4jLogger.isInfoEnabled();
    }

    /** {@inheritDoc} */
    public boolean isDebugEnabled()
    {
        return m_slf4jLogger.isDebugEnabled();
    }
}
