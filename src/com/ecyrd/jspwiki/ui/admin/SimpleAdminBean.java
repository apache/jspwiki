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

import javax.management.*;

import org.apache.commons.lang.StringUtils;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.management.SimpleMBean;

/**
 *  Provides an easy-to-use interface for JSPWiki AdminBeans, which also
 *  are JMX MBeans.  This class provides a default interface for the doGet()
 *  and doPost() interfaces by using the introspection capabilities of the
 *  SimpleMBean.
 *  
 *  @author Janne Jalkanen
 *  @since 2.5.52
 */
public abstract class SimpleAdminBean extends SimpleMBean implements AdminBean
{
    /**
     *  Provides access to a WikiEngine instance to which this AdminBean
     *  belongs to.
     */
    protected WikiEngine m_engine;
    
    /**
     *  Constructor reserved for subclasses only.
     *  
     *  @throws NotCompliantMBeanException
     */
    protected SimpleAdminBean() throws NotCompliantMBeanException
    {
        super();
    }
    
    /**
     *  Initialize the AdminBean by setting up a WikiEngine instance internally.
     */
    public void initialize( WikiEngine engine )
    {
        m_engine = engine;
    }
    
    /**
     *  By default, this method creates a blob of HTML, listing
     *  all the attributes which can be read or written to.  If the
     *  attribute is read-only, a readonly input widget is created.
     *  The value is determined by the toString() method of the attribute.
     */
    public String doGet(WikiContext context)
    {
        MBeanInfo info = getMBeanInfo();
        MBeanAttributeInfo[] attributes = info.getAttributes();
        StringBuffer sb = new StringBuffer();
        
        for( int i = 0; i < attributes.length; i++ )
        {
            sb.append("<div class='block'>\n");
            
            sb.append( "<label>"+StringUtils.capitalize( attributes[i].getName() )+"</label>\n");
            
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

    /**
     *  Not implemented yet.
     */
    public String doPost(WikiContext context)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /** FIXME: This method should generate a random id or something... */
    public String getId()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
