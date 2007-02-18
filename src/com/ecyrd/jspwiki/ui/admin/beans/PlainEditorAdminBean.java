package com.ecyrd.jspwiki.ui.admin.beans;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.ui.admin.AdminBean;

public class PlainEditorAdminBean
    implements AdminBean
{
    private static final String TEMPLATE = 
        "<div>"+
        "<input type='checkbox' id='ajax' %checked/>Use AJAX?<br />"+
        "<input type='submit' value='Submit'/>"+
        "</div>"
        ;
    public String getHTML(WikiContext context)
    {
        String base = TEMPLATE;
        
        base = TextUtil.replaceString( base, "%checked", "checked='checked'" );
        
        return base;
    }

    public String handlePost(WikiContext context, HttpServletRequest req, HttpServletResponse resp)
    {
        return null;
        // TODO Auto-generated method stub
        
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
    
}
