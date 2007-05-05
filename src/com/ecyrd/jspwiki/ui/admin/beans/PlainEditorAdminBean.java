package com.ecyrd.jspwiki.ui.admin.beans;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.servlet.http.HttpServletRequest;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.ui.admin.AdminBean;
import com.ecyrd.management.SimpleMBean;

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
    
    private boolean m_checked = false;
    
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
