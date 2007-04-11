package com.ecyrd.jspwiki.ui.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.modules.WikiModuleInfo;

public class AdminBeanManager
{
    private WikiEngine m_engine;
    private ArrayList  m_allBeans;

    public AdminBeanManager( WikiEngine engine )
    {
        m_engine = engine;
    }
    
    public void initialize()
    {
        reload();
    }
    
    private void reload()
    {
        m_allBeans = new ArrayList();
        
        Collection editors = m_engine.getEditorManager().modules();
        
        for( Iterator i = editors.iterator(); i.hasNext(); )
        {
            String abname = ((WikiModuleInfo)i.next()).getAdminBeanClass();
            
            try
            {
                if( abname != null && abname.length() > 0 )
                {
                    Class abclass = Class.forName(abname);
                
                    AdminBean ab = (AdminBean) abclass.newInstance();
                
                    m_allBeans.add( ab );
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
}
