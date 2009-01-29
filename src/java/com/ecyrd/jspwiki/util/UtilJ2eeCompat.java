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
package com.ecyrd.jspwiki.util;

import com.ecyrd.jspwiki.log.Logger;
import com.ecyrd.jspwiki.log.LoggerFactory;

/**
 * Misc J2EE Compatibility Utility Functions
 */
public class UtilJ2eeCompat
{
    private static Logger log = LoggerFactory.getLogger( UtilJ2eeCompat.class.getName() );

    public static final String TOMCAT = "Apache Tomcat";

    public static final String ORION = "Orion";

    public static final String RESIN = "Resin";

    public static final String REX_IP = "TradeCity";

    public static final String OC4J = "Oracle";

    public static final String JRUN = "JRun";

    public static final String JETTY = "Jetty";

    public static final String WEBSPHERE = "Websphere";

    public static final String WEBLOGIC = "WebLogic";

    public static final String GLASSFISH = "Sun Java System Application Server";

    /**
     * 
     */
    protected static Boolean useOutputStreamValue = null;

    private static String m_serverInfo;

    /**
     * Determines the response wrapper for the servlet filters
     * 
     * @param serverInfo The string returned from context.getServerInfo()
     * @return <code>true</code> if standard response wrapper does not work
     *         properly; <code>false</code> default, otherwise
     */
    public static boolean useOutputStream( String serverInfo )
    {
        if( useOutputStreamValue == null )
        {
            initCompatibilityOptions( serverInfo );
        }
        return useOutputStreamValue.booleanValue();
    }

    /**
     * For testing only
     * 
     * @param serverInfo The string returned from context.getServerInfo()
     * @param boolInitialize True, if you want to force initialization again
     * @return <code>true</code> if standard response wrapper does not work
     *         properly; <code>false</code> default, otherwise
     */
    public static boolean useOutputStream( String serverInfo, Boolean boolInitialize )
    {
        if( (useOutputStreamValue == null) | (boolInitialize) )
        {
            initCompatibilityOptions( serverInfo );
        }
        return useOutputStreamValue.booleanValue();
    }

    /**
     * Simple check of the servlet container
     * 
     * @param serverInfo The string returned from context.getServerInfo()
     */
    protected static void initCompatibilityOptions( String serverInfo )
    {
        log.info( "serverInfo: " + serverInfo );
        m_serverInfo = serverInfo;
        // response.getWriter is the default
        boolean useStream = false;
        if( serverInfo.indexOf( RESIN ) >= 0 )
        {
            log.info( RESIN + " detected" );
        }
        else if( serverInfo.indexOf( REX_IP ) >= 0 )
        {
            log.info( REX_IP + " detected" );
        }
        else if( serverInfo.indexOf( TOMCAT ) >= 0 )
        {
            log.info( TOMCAT + " detected" );
        }
        else if( serverInfo.indexOf( JRUN ) >= 0 )
        {
            log.info( JRUN + " detected" );
        }
        else if( serverInfo.indexOf( JETTY ) >= 0 )
        {
            log.info( JETTY + " detected" );
        }
        else if( serverInfo.indexOf( ORION ) >= 0 )
        {
            log.info( ORION + " detected" );
        }
        else if( serverInfo.indexOf( WEBSPHERE ) >= 0 )
        {
            log.info( WEBSPHERE + " detected" );
        }
        else if( serverInfo.indexOf( WEBLOGIC ) >= 0 )
        {
            log.info( WEBLOGIC + " detected" );
        }
        else if( serverInfo.indexOf( GLASSFISH ) >= 0 )
        {
            log.info( GLASSFISH + " detected" );
        }
        else if( serverInfo.indexOf( OC4J ) >= 0 )
        {
            log.info( "Oracle Container for JEE detected" );
            // use response.getOutputStream instead of response.getWriter
            useStream = true;
        }
        useOutputStreamValue = new Boolean( useStream );
    }

    /**
     * @return the Container type that has been detected
     */
    public static String getServerInfo()
    {
        return m_serverInfo;
    }

}
