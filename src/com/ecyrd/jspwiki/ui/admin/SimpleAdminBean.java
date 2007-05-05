package com.ecyrd.jspwiki.ui.admin;

import javax.management.*;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.management.SimpleMBean;

public abstract class SimpleAdminBean extends SimpleMBean implements AdminBean
{
    protected WikiEngine m_engine;
    
    protected SimpleAdminBean() throws NotCompliantMBeanException
    {
        super();
    }
    
    public void initialize( WikiEngine engine )
    {
        m_engine = engine;
    }

    public String doGet(WikiContext context)
    {
        MBeanInfo info = getMBeanInfo();
        MBeanAttributeInfo[] attributes = info.getAttributes();
        StringBuffer sb = new StringBuffer();
        
        for( int i = 0; i < attributes.length; i++ )
        {
            sb.append("<div class='block'>\n");
            
            sb.append( "<label>"+attributes[i].getName()+"</label>\n");
            
            try
            {
                Object value = getAttribute( attributes[i].getName() );
                if( attributes[i].isWritable() )
                {
                    sb.append( "<input type='text' name='question' size='30' value='"+value+"' />\n" );
                }
                else
                {
                    sb.append( "<input type='text' class='readonly' readonly='true' size='30' value='"+value+"' />\n" );
                }
            }
            catch( Exception e )
            {
                sb.append("Exception: "+e.getMessage());
            }

            sb.append( "<div class='description'>"+attributes[i].getDescription()+"</div>\n");
            
            sb.append("</div>\n");
        }
        return sb.toString();
    }

    public String doPost(WikiContext context)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getId()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
