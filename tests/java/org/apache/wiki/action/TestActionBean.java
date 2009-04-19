package org.apache.wiki.action;

import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.validation.Validate;

import org.apache.wiki.action.AbstractPageActionBean;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.ui.stripes.SpamProtect;

/**
 * Simple test bean with a single event handler method.
 */
public class TestActionBean extends AbstractPageActionBean
{
    @SpamProtect( content = "text" )
    public Resolution test()
    {
        return null;
    }
    
    @Validate( required = false )
    public void setPage( WikiPage page )
    {
        super.setPage( page );
    }
    
    private String text = null;
    
    private String acl = null;

    public String getText()
    {
        return text;
    }

    public void setText( String text )
    {
        this.text = text;
    }

    public String getAcl()
    {
        return acl;
    }

    public void setAcl( String acl )
    {
        this.acl = acl;
    }
    
}