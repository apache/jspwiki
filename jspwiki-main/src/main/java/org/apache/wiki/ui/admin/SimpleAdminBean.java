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
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
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
public abstract class SimpleAdminBean extends SimpleMBean implements AdminBean {

    /** Provides access to a Engine instance to which this AdminBean belongs to. */
    protected Engine m_engine;
    
    /**
     *  Constructor reserved for subclasses only.
     *  
     *  @throws NotCompliantMBeanException
     */
    protected SimpleAdminBean() throws NotCompliantMBeanException {
        super();
    }
    
    /**
     *  Initialize the AdminBean by setting up a Engine instance internally.
     */
    @Override
    public void initialize( final Engine engine )
    {
        m_engine = engine;
    }
    
    /**
     *  By default, this method creates a blob of HTML, listing
     *  all the attributes which can be read or written to.  If the
     *  attribute is read-only, a readonly input widget is created.
     *  The value is determined by the toString() method of the attribute.
     */
    @Override
    public String doGet( final Context context ) {
        final MBeanInfo info = getMBeanInfo();
        final MBeanAttributeInfo[] attributes = info.getAttributes();
        final StringBuilder sb = new StringBuilder();

        for( int i = 0; i < attributes.length; i++ ) {
            sb.append( "<div class='block'>\n" );

            sb.append( "<label>" ).append( StringUtils.capitalize( attributes[i].getName() ) ).append( "</label>\n" );

            try {
                final Object value = getAttribute( attributes[ i ].getName() );
                if( attributes[ i ].isWritable() ) {
                    sb.append( "<input type='text' name='question' size='30' value='" ).append( value ).append( "' />\n" );
                } else {
                    sb.append( "<input type='text' class='readonly' readonly='true' size='30' value='" ).append( value ).append( "' />\n" );
                }
            } catch( final Exception e ) {
                sb.append( "Exception: " ).append( e.getMessage() );
            }

            sb.append( "<div class='description'>" ).append( attributes[i].getDescription() ).append( "</div>\n" );

            sb.append( "</div>\n" );
        }
        return sb.toString();
    }

    /**
     *  Not implemented yet.
     */
    @Override
    public String doPost( final Context context) {
        return null;
    }

    /**
     *  By default, this method returns the class name of the bean.  This is suitable, if you have a singleton bean.
     */
    @Override
    public String getId()
    {
        return getClass().getName();
    }

}
