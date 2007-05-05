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
import com.ecyrd.jspwiki.modules.WikiModuleInfo;
import com.ecyrd.jspwiki.ui.admin.beans.CoreBean;

public class AdminBeanManager
{
    private WikiEngine m_engine;
    private ArrayList  m_allBeans;

    private MBeanServer m_mbeanServer;
    
    private static Logger log = Logger.getLogger(AdminBeanManager.class);
    
    public AdminBeanManager( WikiEngine engine )
    {
        if( SystemUtils.isJavaVersionAtLeast(1.5f) )
        {
            log.info("Using JDK 1.5 Platform MBeanServer");
            m_mbeanServer = MBeanServerFactory15.getServer();
        }
        else
        {
            log.info("Finding a JDK 1.4 -compatible MBeanServer.");
            m_mbeanServer = MBeanServerFactory14.getServer();
        }
        m_engine = engine;
            
        log.info( m_mbeanServer.getClass().getName() );
        log.info( m_mbeanServer.getDefaultDomain() );
        
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
            case AdminBean.UNKNOWN:
            default:
                return "Unknown";
                
            case AdminBean.CORE:
                return "Core";

            case AdminBean.EDITOR:
                return "Editors";
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
            if( ab instanceof DynamicMBean )
            {
                String component = getJMXTitleString( ab.getType() );
                String title     = ab.getTitle();
            
                ObjectName name = new ObjectName( Release.APPNAME + ":component="+component+",name="+title );
                m_mbeanServer.registerMBean( ab, name );
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
     *  Lists all administration beans.
     *  
     *  @return all AdminBeans known to the manager
     */
    public List getAllBeans()
    {
        if( m_allBeans == null ) reload();
        
        return m_allBeans;
    }
    
    /**
     *  A JDK 1.4 version of something which gets us the MBeanServer.  It
     *  binds to the first server it can find.
     *  
     *  @author jalkanen
     *
     */
    private static class MBeanServerFactory14
    {
        public static MBeanServer getServer()
        {
            MBeanServer server;
            ArrayList servers = MBeanServerFactory.findMBeanServer(null);
            
            if( servers == null || servers.size() == 0 )
            {
                log.info("Creating a new MBeanServer...");
                server = MBeanServerFactory.createMBeanServer(Release.APPNAME);
            }
            else
            {
                server = (MBeanServer)servers.get(0);
            }
  
            return server;
        }
    }

    /**
     *  A JDK 1.5 version of something which gets use the MBeanServer.
     *  
     *  @author jalkanen
     *
     */
    private static class MBeanServerFactory15
    {
        public static MBeanServer getServer()
        {
            return ManagementFactory.getPlatformMBeanServer();
        }
    }

    public static int getTypeFromString(String type)
    {
        if( type.equals("core") )
            return AdminBean.CORE;
        else if( type.equals("editors") )
            return AdminBean.EDITOR;

        return AdminBean.UNKNOWN;
    }
}
