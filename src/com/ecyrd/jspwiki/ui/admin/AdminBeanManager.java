/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 JSPWiki development group

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.ui.admin;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.management.*;

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.Release;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.event.WikiEngineEvent;
import com.ecyrd.jspwiki.event.WikiEvent;
import com.ecyrd.jspwiki.event.WikiEventListener;
import com.ecyrd.jspwiki.modules.WikiModuleInfo;
import com.ecyrd.jspwiki.ui.admin.beans.CoreBean;
import com.ecyrd.jspwiki.ui.admin.beans.PluginBean;
import com.ecyrd.jspwiki.ui.admin.beans.SearchManagerBean;
import com.ecyrd.jspwiki.ui.admin.beans.UserBean;

/**
 *  Provides a manager class for all AdminBeans within JSPWiki.  This class
 *  also manages registration for any AdminBean which is also a JMX bean.
 *
 *  @author Janne Jalkanen
 *  @since  2.5.52
 */
public class AdminBeanManager implements WikiEventListener
{
    private WikiEngine m_engine;
    private ArrayList  m_allBeans;

    private MBeanServer m_mbeanServer = null;

    private static Logger log = Logger.getLogger(AdminBeanManager.class);

    public AdminBeanManager( WikiEngine engine )
    {
        log.info("Using JDK 1.5 Platform MBeanServer");
        m_mbeanServer = MBeanServerFactory15.getServer();

        m_engine = engine;

        if( m_mbeanServer != null )
        {
            log.info( m_mbeanServer.getClass().getName() );
            log.info( m_mbeanServer.getDefaultDomain() );
        }

        m_engine.addWikiEventListener( this );
        initialize();
    }

    public void initialize()
    {
        reload();
    }

    private String getJMXTitleString( int title )
    {
        switch( title )
        {
            case AdminBean.CORE:
                return "Core";

            case AdminBean.EDITOR:
                return "Editors";

            case AdminBean.UNKNOWN:
            default:
                return "Unknown";
        }
    }


    /**
     *  Register an AdminBean.  If the AdminBean is also a JMX MBean, it
     *  also gets registered to the MBeanServer we've found.
     *
     *  @param ab AdminBean to register.
     */
    private void registerAdminBean( AdminBean ab )
    {
        try
        {
            if( ab instanceof DynamicMBean && m_mbeanServer != null )
            {
                ObjectName name = getObjectName(ab);

                if( !m_mbeanServer.isRegistered(name))
                {
                    m_mbeanServer.registerMBean( ab, name );
                }
            }

            m_allBeans.add( ab );

            log.info("Registered new admin bean "+ab.getTitle());
        }
        catch( InstanceAlreadyExistsException e )
        {
            log.error("Admin bean already registered to JMX",e);
        }
        catch( MBeanRegistrationException e )
        {
            log.error("Admin bean cannot be registered to JMX",e);
        }
        catch( NotCompliantMBeanException e )
        {
            log.error("Your admin bean is not very good",e);
        }
        catch( MalformedObjectNameException e )
        {
            log.error("Your admin bean name is not very good",e);
        }
        catch( NullPointerException e )
        {
            log.error("Evil NPE occurred",e);
        }
    }

    private ObjectName getObjectName(AdminBean ab) throws MalformedObjectNameException
    {
        String component = getJMXTitleString( ab.getType() );
        String title     = ab.getTitle();

        ObjectName name = new ObjectName( Release.APPNAME + ":component="+component+",name="+title );
        return name;
    }

    /**
     *  Registers all the beans from a collection of WikiModuleInfos.  If some of the beans
     *  fail, logs the message and keeps going to the next bean.
     *
     *  @param c Collection of WikiModuleInfo instances
     */
    private void registerBeans( Collection c )
    {
        for( Iterator i = c.iterator(); i.hasNext(); )
        {
            String abname = ((WikiModuleInfo)i.next()).getAdminBeanClass();

            try
            {
                if( abname != null && abname.length() > 0 )
                {
                    Class abclass = Class.forName(abname);

                    AdminBean ab = (AdminBean) abclass.newInstance();

                    registerAdminBean( ab );
                }
            }
            catch (ClassNotFoundException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (InstantiationException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    // FIXME: Should unload the beans first.
    private void reload()
    {
        m_allBeans = new ArrayList();

        try
        {
            registerAdminBean( new CoreBean(m_engine) );
            registerAdminBean( new UserBean(m_engine) );
            registerAdminBean( new SearchManagerBean(m_engine) );
            registerAdminBean( new PluginBean(m_engine) );
        }
        catch (NotCompliantMBeanException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        registerBeans( m_engine.getEditorManager().modules() );
        registerBeans( m_engine.getPluginManager().modules() );
    }

    /**
     *  Lists all administration beans which are currently known
     *  and instantiated.
     *
     *  @return all AdminBeans known to the manager
     */
    public List getAllBeans()
    {
        if( m_allBeans == null ) reload();

        return m_allBeans;
    }

    /**
     *  Locates a bean based on the AdminBean.getId() out of all
     *  the registered beans.
     *
     *  @param id ID
     *  @return An AdminBean, or null, if no such bean is found.
     */
    public AdminBean findBean( String id )
    {
        for( Iterator i = m_allBeans.iterator(); i.hasNext(); )
        {
            AdminBean ab = (AdminBean) i.next();

            if( ab.getId().equals(id) )
                return ab;
        }

        return null;
    }

    /**
     *  Provides a JDK 1.5-compliant version of the MBeanServerFactory. This
     *  will simply bind to the platform MBeanServer.
     *
     *  @author Janne Jalkanen
     *
     */
    private static final class MBeanServerFactory15
    {
        private MBeanServerFactory15()
        {}

        public static MBeanServer getServer()
        {
            return ManagementFactory.getPlatformMBeanServer();
        }
    }

    /**
     *  Returns the type identifier for a string type.
     *
     *  @param type A type string.
     *  @return A type value.
     */
    public static int getTypeFromString(String type)
    {
        if( type.equals("core") )
            return AdminBean.CORE;
        else if( type.equals("editors") )
            return AdminBean.EDITOR;

        return AdminBean.UNKNOWN;
    }

    /**
     *  Unregisters AdminBeans upon SHUTDOWN event.
     *
     *  @param event {@inheritDoc}
     */
    public void actionPerformed(WikiEvent event)
    {
        if( event instanceof WikiEngineEvent )
        {
            if( ((WikiEngineEvent)event).getType() == WikiEngineEvent.SHUTDOWN )
            {
                for( Iterator i = m_allBeans.iterator(); i.hasNext(); )
                {
                    try
                    {
                        AdminBean ab = (AdminBean) i.next();
                        ObjectName on = getObjectName( ab );
                        if( m_mbeanServer.isRegistered( on ) )
                        {
                            m_mbeanServer.unregisterMBean(on);
                            log.info("Unregistered AdminBean "+ab.getTitle());
                        }
                    }
                    catch( MalformedObjectNameException e )
                    {
                        log.error("Malformed object name when unregistering",e);
                    }
                    catch (InstanceNotFoundException e)
                    {
                        log.error("Object was registered; yet claims that it's not there",e);
                    }
                    catch (MBeanRegistrationException e)
                    {
                        log.error("Registration exception while unregistering",e);
                    }
                }
            }
        }
    }
}
