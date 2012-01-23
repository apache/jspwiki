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
package org.apache.wiki.tags;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.wiki.ui.admin.AdminBean;
import org.apache.wiki.ui.admin.AdminBeanManager;

/**
 * Iterator tag for all AdminBeans of a given type.
 */
public class AdminBeanIteratorTag extends IteratorTag<AdminBean>
{
    private static final long serialVersionUID = 1L;

    private int m_type;

    /**
     * Set the type of the bean to iterate over.
     * 
     * @param type type of AdminBean to seek
     */
    public void setType( String type )
    {
        m_type = AdminBeanManager.getTypeFromString( type );
    }

    /**
     * Returns the default list of AdminBeans returned by
     * {@link AdminBeanManager#getAllBeans()}. Only AdminBeans that match the
     * type set by {@link #setType(String)} will be returned.
     * 
     * @return the admin beans
     */
    @Override
    protected Collection<AdminBean> initItems()
    {
        AdminBeanManager mgr = m_wikiContext.getEngine().getAdminBeanManager();

        // Retrieve the list of beans for the specified type
        Collection<AdminBean> beans = mgr.getAllBeans();
        ArrayList<AdminBean> typedBeans = new ArrayList<AdminBean>();
        for( AdminBean ab : beans )
        {
            if( ab.getType() == m_type )
            {
                typedBeans.add( ab );
            }
        }

        return typedBeans;
    }
}
