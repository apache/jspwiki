package com.ecyrd.jspwiki.tags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.ecyrd.jspwiki.ui.admin.AdminBean;
import com.ecyrd.jspwiki.ui.admin.AdminBeanManager;

public class AdminBeanIteratorTag extends IteratorTag
{
    private static final long serialVersionUID = 1L;

    private int m_type;
    
    public void setType(String type)
    {
        if( type.equalsIgnoreCase("editors") )
            m_type = AdminBean.EDITOR;
        else
            m_type = AdminBean.UNKNOWN;
    }
    
    public void resetIterator()
    {
        AdminBeanManager mgr = m_wikiContext.getEngine().getAdminBeanManager();
        
        Collection beans = mgr.getAllBeans();
        
        ArrayList typedBeans = new ArrayList();
        
        for( Iterator i = beans.iterator(); i.hasNext(); )
        {
            AdminBean ab = (AdminBean)i.next();
            
            if( ab.getType() == m_type )
            {
                typedBeans.add( ab );
            }
        }
        
        setList( typedBeans );
    }
}
