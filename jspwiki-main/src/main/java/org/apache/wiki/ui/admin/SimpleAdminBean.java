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
package org.apache.wiki.ui.admin;

import org.apache.commons.lang3.StringUtils;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.management.SimpleMBean;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.NotCompliantMBeanException;

/**
 *  Provides an easy-to-use interface for JSPWiki AdminBeans, which also
 *  are JMX MBeans.  This class provides a default interface for the doGet()
 *  and doPost() interfaces by using the introspection capabilities of the
 *  SimpleMBean.
 *  
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
        StringBuilder sb = new StringBuilder();
        
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

    /**
     *  By default, this method returns the class name of the bean.  This is
     *  suitable, if you have a singleton bean.
     */
    public String getId()
    {
        return getClass().getName();
    }
}
