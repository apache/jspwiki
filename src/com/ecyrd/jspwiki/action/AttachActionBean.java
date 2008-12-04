package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.ui.stripes.HandlerPermission;
import com.ecyrd.jspwiki.ui.stripes.WikiRequestContext;

@UrlBinding("/attach/{page}/{attachment}")
public class AttachActionBean extends AbstractActionBean
{
    private Attachment m_attachment;
    
    @DefaultHandler
    @HandlesEvent("upload")
    @HandlerPermission(permissionClass=PagePermission.class,target="${page.name}",actions=PagePermission.UPLOAD_ACTION)
    @WikiRequestContext("att")
    public Resolution upload()
    {
        return null;
    }

    public Attachment getAttachment()
    {
        return m_attachment;
    }

    public void setAttachment( Attachment attachment )
    {
        this.m_attachment = attachment;
    }
}
