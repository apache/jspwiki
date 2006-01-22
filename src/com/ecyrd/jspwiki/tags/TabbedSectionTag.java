/*
 * TabbedSection tag
 * Dirk Frederickx, Jan 06
 */

package com.ecyrd.jspwiki.tags;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

/**
 *  Generates tabbed page section: container for the Tab tag.
 *  Works together with the tabbedSection javacript.
 *
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>defaultTab - Page name to refer to.  Default is the current page.
 *  </UL>
 *
 *  @author Dirk Frederickx
 *  @since v2.3.63
 */

public class TabbedSectionTag extends BodyTagSupport
{
    private String       defaultTabId;
    private String       firstTabId;
    private boolean      defaultTabFound = false;
    private StringBuffer sb = new StringBuffer();
    
    private static final int FIND_DEFAULT_TAB = 0;
    private static final int GENERATE_TABMENU = 1;
    private static final int GENERATE_TABBODY = 2;
    private              int state            = FIND_DEFAULT_TAB; 
    
    public void setDefaultTab(String anDefaultTabId)
    {
        defaultTabId = anDefaultTabId;
    }
    
    public boolean validateDefaultTab( String aTabId )
    {
        if( firstTabId == null ) firstTabId = aTabId;
        if( aTabId.equals( defaultTabId ) ) defaultTabFound = true;
        
        return ( aTabId.equals( defaultTabId ) );
    }
    
    public int doStartTag() throws JspTagException
    {
        return EVAL_BODY_BUFFERED; /* always look inside */
    }
    
    public boolean isStateFindDefaultTab()  { return state == FIND_DEFAULT_TAB; }
    public boolean isStateGenerateTabMenu() { return state == GENERATE_TABMENU; }
    public boolean isStateGenerateTabBody() { return state == GENERATE_TABBODY; }
    
    
    /* The tabbed section iterates 3 time through the underlying Tab tags
     * - first it identifies the default tab (displayed by default)
     * - second it generates the tabmenu markup (displays all tab-titles)
     * - finally it generates the content of each tab.
     */
    public int doAfterBody() throws JspTagException
    {
        if( isStateFindDefaultTab() )         
        { 
            if( !defaultTabFound ) { defaultTabId = firstTabId; }
            state = GENERATE_TABMENU; 
            return EVAL_BODY_BUFFERED;
        }
        else if( isStateGenerateTabMenu() )   
        { 
            if( bodyContent != null )
            {
                sb.append( "<div class=\"tabmenu\">" );
                sb.append( bodyContent.getString() );
                bodyContent.clearBody();
                sb.append( "</div>\n" );
            }
            state = GENERATE_TABBODY; 
            return EVAL_BODY_BUFFERED;
        }
        else if( isStateGenerateTabBody() ) 
        { 
            if( bodyContent != null )
            {      
                sb.append( "<div class=\"tabs\">" );
                sb.append( bodyContent.getString() );
                bodyContent.clearBody();
                sb.append( "<div style=\"clear:both; height:0px;\" > </div>\n</div>\n" );
            }
            return SKIP_BODY; 
        }
        return SKIP_BODY; 
    }
    
    public int doEndTag() throws JspTagException
    {
        try
        {
            if( sb.length() > 0 ) 
            {
                getPreviousOut().write( sb.toString() );
            }
        }
        catch(java.io.IOException e)
        {
            throw new JspTagException( "IO Error: " + e.getMessage() );
        }
        
        //now reset some stuff for the next run -- ugh.
        sb    = new StringBuffer();
        state = FIND_DEFAULT_TAB; 
        defaultTabId    = null;
        firstTabId      = null;
        defaultTabFound = false;
        return EVAL_PAGE;
    }
    
}