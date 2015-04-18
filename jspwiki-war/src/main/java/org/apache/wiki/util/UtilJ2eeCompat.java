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
package org.apache.wiki.util;

import org.apache.log4j.Logger;

/**
 * Misc J2EE Compatibility Utility Functions
 */
public class UtilJ2eeCompat
{
    private static Logger log = Logger.getLogger( UtilJ2eeCompat.class.getName() );

    public static final String TOMCAT = "apache tomcat";

    public static final String ORION = "orion";

    public static final String RESIN = "resin";

    public static final String REX_IP = "tradecity";

    public static final String OC4J = "oracle";

    public static final String JRUN = "jrun";

    public static final String JETTY = "jetty";

    public static final String WEBSPHERE = "websphere";

    public static final String WEBSPHERE_LIBERTY = "SMF WebContainer";

    public static final String WEBLOGIC = "weblogic";

    public static final String GLASSFISH_1 = "sun java system application server";

    public static final String GLASSFISH_2 = "glassfish server";

    public static final String JBOSS = "jboss";
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
        if( serverInfo.toLowerCase().indexOf( RESIN ) >= 0 )
        {
            log.info( RESIN + " detected" );
            // use response.getOutputStream instead of response.getWriter
            useStream = true;
        }
        else if( serverInfo.toLowerCase().indexOf( REX_IP ) >= 0 )
        {
            log.info( REX_IP + " detected" );
        }
        else if( serverInfo.toLowerCase().indexOf( TOMCAT ) >= 0 )
        {
            log.info( TOMCAT + " detected" );
            // use response.getOutputStream instead of response.getWriter
            useStream = true;
        }
        else if( serverInfo.toLowerCase().indexOf( JRUN ) >= 0 )
        {
            log.info( JRUN + " detected" );
        }
        else if( serverInfo.toLowerCase().indexOf( JETTY ) >= 0 )
        {
            log.info( JETTY + " detected" );
        }
        else if( serverInfo.toLowerCase().indexOf( ORION ) >= 0 )
        {
            log.info( ORION + " detected" );
        }
        else if( serverInfo.toLowerCase().indexOf( WEBSPHERE ) >= 0 )
        {
            log.info( WEBSPHERE + " detected" );
        }
        else if( serverInfo.toLowerCase().indexOf( WEBSPHERE_LIBERTY ) >= 0 )
        {
            log.info( WEBSPHERE_LIBERTY + " detected" );
        }
        else if( serverInfo.toLowerCase().indexOf( WEBLOGIC ) >= 0 )
        {
            log.info( WEBLOGIC + " detected" );
            // use response.getOutputStream instead of response.getWriter
            useStream = true;
        }
        else if( serverInfo.toLowerCase().indexOf( GLASSFISH_1 ) >= 0 )
        {
            log.info( GLASSFISH_1 + " detected" );
        }
        else if( serverInfo.toLowerCase().indexOf( GLASSFISH_2 ) >= 0 )
        {
            log.info( GLASSFISH_2 + " detected" );
        }
        else if( serverInfo.toLowerCase().indexOf( OC4J ) >= 0 )
        {
            log.info( "Oracle Container for JEE detected" );
            // use response.getOutputStream instead of response.getWriter
            useStream = true;
        }
        else if( serverInfo.toLowerCase().indexOf( JBOSS ) >= 0 )
        {
            log.info( JBOSS + " detected" );
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
