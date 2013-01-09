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
package org.apache.wiki.ui.admin.beans;

import javax.management.NotCompliantMBeanException;
import javax.servlet.http.HttpServletRequest;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.ui.admin.AdminBean;
import org.apache.wiki.management.SimpleMBean;
import org.apache.wiki.util.TextUtil;

/**
 *  This class is still experimental.
 *  
 *
 */
public class PlainEditorAdminBean
    extends SimpleMBean
    implements AdminBean
{
    private static final String TEMPLATE = 
        "<div>"+
        "<input type='checkbox' id='ajax' %checked/>Use AJAX?<br />"+
        "<input type='submit' value='Submit'/>"+
        "%messages"+
        "</div>"
        ;
    
    private boolean m_checked;
    
    private static final String[] ATTRIBUTES = {"title","checked"};
    private static final String[] METHODS    = {};
    
    public PlainEditorAdminBean() throws NotCompliantMBeanException
    {
    }
    
    public String doGet(WikiContext context)
    {
        HttpServletRequest req = context.getHttpRequest();
        
        if( req != null && req.getMethod().equals("POST") && getTitle().equals( req.getParameter("form") ) )
        {
            return doPost( context );
        }
        String base = TEMPLATE;
        
        base = TextUtil.replaceString( base, "%checked", "checked='checked'" );
        base = TextUtil.replaceString( base, "%messages", "" );
        
        return base;
    }

    public String doPost( WikiContext context )
    {
        HttpServletRequest req = context.getHttpRequest();
        
        boolean checked = "checked".equals( req.getParameter( "id" ) );
        
        // Make changes
        
        String base = TEMPLATE;
        
        base = TextUtil.replaceString( base, "%checked", checked ? "checked='checked'" : "" );
        base = TextUtil.replaceString( base, "%messages", "<br /><font color='red'>Your settings have been saved</font>" );
        
        return base;
    }
    
    public String getTitle()
    {
        return "Plain editor";
    }

    public int getType()
    {
        return EDITOR;
    }

    public boolean isEnabled()
    {
        return true;
    }

    public String getId()
    {
        return "editor.plain";
    }
    
    public boolean getChecked()
    {
        return m_checked;
    }
    
    public String[] getAttributeNames()
    {
        return ATTRIBUTES;
    }

    public String[] getMethodNames()
    {
        return METHODS;
    }

    public void initialize(WikiEngine engine)
    {
        // TODO Auto-generated method stub
        
    }
}
