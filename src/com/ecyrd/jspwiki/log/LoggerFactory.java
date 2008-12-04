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

import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.bridge.SLF4JBridgeHandler;

import com.ecyrd.jspwiki.Release;

/**
 * <p>
 * Factory class for {@link com.ecyrd.jspwiki.log.Logger} objects
 * </p>
 * JSPWiki uses the slf4j facade for logging. See the <a
 * href='http://www.slf4j.org'>slf4j website<a/> for details.
 * <p>
 * You can decide at runtime which logging framework to use by placing one of
 * the slf4j wrapped implementations in WEB-INF/lib. By default, JSPWiki ships
 * with a log4j implementation. The static initializer calls the
 * SLF4JBridgeHandler.install() which is a jul handler, that routes all incoming
 * jul records to the slf4j API.
 * </p>
 * 
 * @author Harry Metske
 * @since 3.0
 */
public final class LoggerFactory
{
    private static boolean c_isLog4jPresent = true;
    
    //     it appears that after shutting down JSPWiki, we get new requests for Loggers, to prevent this we "close the door" :
    private static boolean c_isLoggerFactoryClosed= false;

    private static final String SLF4J_LOG4J_ADAPTER_CLASS = "org.slf4j.impl.Log4jLoggerAdapter";

    private static final String LOG4J_LOGGER_CLASS = "org.apache.log4j.Logger";

    private static HashMap<String, LoggerImpl> c_registeredLoggers = new HashMap<String, LoggerImpl>( 200 );

    /**
     *   @TODO  We need something here to make the Logger MBeans unique across the JVM, this will not work if you
     *                     run multiple wiki's in the same JVM, same is true for other MBeans. 
     */
    private static final String OBJECTNAME_PREFIX = Release.APPNAME + ":component=Loggers,name=";

    static
    {
        SLF4JBridgeHandler.install();
    }

    /**
     *  Private constructor prevents instantiation.
     */
    private LoggerFactory()
    {}


    /**
     *  Utility method for locating a Logger based on a Class.
     *  
     *  @param clazz The Class to find a Logger for.
     *  @return A Logger instance.
     */
    public static final Logger getLogger( Class clazz )
    {
        return getLogger( clazz.getName() );
    }

    
    /**
     * Returns a Logger instance, and also, if it is a Log4J logger, registers the Logging MBean.
     * 
     * @param loggerName Logger to find.
     * @return A Logger instance. 
     */
    public static final synchronized Logger getLogger( String loggerName )
    {
        if( c_isLoggerFactoryClosed )
        {
            return null;
        }
        
        if( c_registeredLoggers.get( loggerName ) == null )
        {
            LoggerImpl logger = new LoggerImpl( loggerName );
            c_registeredLoggers.put( loggerName, logger );
            if( c_isLog4jPresent )
            {
                registerLoggerMBean( loggerName );
            }
            return logger;
        }
        return c_registeredLoggers.get( loggerName );
    }

    /**
     * Registers the Logger instance the the Platform MBeanServer. This gives
     * the option of dynamically setting the loglevel for each individual logger
     * using java's jconsole. It is only available if log4j is used as the
     * logging implementation.
     * 
     * @TODO It would be nice if we also had access to this from the Admin.jsp
     * @param loggerName
     */
    private static void registerLoggerMBean( String loggerName )
    {
        // This is a real kludge, slf4j offers no programmatic access to the
        // loglevels and no DynamicMBeans. log4j has them, so if the log4j adapter is on the
        // classpath we use the log4j DynamicMBeans.
        // At compile time we don't have log4j, so we use reflection to do the job.
        //
        try
        {
            // first instantiate the logger: Logger.getLogger(loggerName)
            //
            @SuppressWarnings("unused")
            Object slf4jLog4jImpl = Class.forName( SLF4J_LOG4J_ADAPTER_CLASS);
            //
            Object log4jLogger = Class.forName(LOG4J_LOGGER_CLASS );
            Class loggerClass = Class.forName(LOG4J_LOGGER_CLASS  );
            Class[] parms = new Class[1];
            parms[0] = new String().getClass();
            Method getLoggerMethod = loggerClass.getMethod( "getLogger", parms );
            log4jLogger = getLoggerMethod.invoke( log4jLogger, loggerName );
            //
            // then register the logger to the mbeanServer :
            //
            // mbeanServer.registerMBean( new LoggerDynamicMBean( logger ), mbeanName );
            //
            Object[] arglist = new Object[1];
            arglist[0] = log4jLogger;
            Class mbeanClass = Class.forName( "org.apache.log4j.jmx.LoggerDynamicMBean" );
            Constructor constr = mbeanClass.getConstructor( loggerClass );
            Object dynMBean = constr.newInstance( arglist );
            ObjectName mbeanName = new ObjectName( Release.APPNAME + ":component=Loggers,name=" + loggerName );
            MBeanServer mbeanServer =  ManagementFactory.getPlatformMBeanServer();
            if( !mbeanServer.isRegistered( mbeanName ) )
            {
                mbeanServer.registerMBean( dynMBean, mbeanName );
            }
        }
        catch( ClassNotFoundException cnfe )
        {
            // apparently we cannot find the slf4j log4j adapter, so we assume there is no log4j
            // available, so there is no use in registering MBeans
            c_isLog4jPresent = false;
            System.err.println( "Could not find class " +SLF4J_LOG4J_ADAPTER_CLASS + ", so no dynamic log configuration here :-(" );
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }

    
    /**
     * UnRegisters all Logger MBeans from the Platform MBeanServer.
     */
    public static void unRegisterAllLoggerMBeans()
    {
        c_isLoggerFactoryClosed = true;

        Set<String> loggerNames = c_registeredLoggers.keySet();
        for( String loggerName : loggerNames )
        {
            try
            {
                ObjectName mbeanName = new ObjectName( OBJECTNAME_PREFIX + loggerName );
                ManagementFactory.getPlatformMBeanServer().unregisterMBean( mbeanName );
            }
            catch( Exception e )
            {
                // ignore this, we can't do anything about it
            }
        }
    }
}
