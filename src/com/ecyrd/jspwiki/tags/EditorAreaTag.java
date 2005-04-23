/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.tags;

import org.apache.ecs.ConcreteElement;
import org.apache.ecs.xhtml.textarea;

import com.ecyrd.jspwiki.WikiContext;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public class EditorAreaTag extends WikiTagBase
{
    public int doWikiStartTag() throws Exception
    {
        pageContext.getOut().print( getEditorArea( m_wikiContext ).toString() );
        
        return SKIP_BODY;
    }
    
    public static ConcreteElement getEditorArea( WikiContext context )
    {
        textarea area = new textarea();

        area.setClass("editor");
        area.setWrap("virtual");
        area.setName("text");
        area.setRows( 25 );
        area.setCols( 80 );
        area.setStyle( "width:100%;" );
       
        if( context.getRequestContext().equals(WikiContext.EDIT) )
        {
            String usertext = context.getHttpParameter("text");
            if( usertext == null )
            {
                usertext = context.getEngine().getText( context, context.getPage() );
            }
            
            area.addElement( usertext );
        }
        else if( context.getRequestContext().equals(WikiContext.COMMENT) )
        {
            String usertext = context.getHttpParameter("text");
            
            if( usertext != null ) area.addElement( usertext );
        }
        
        return area;
    }
}
