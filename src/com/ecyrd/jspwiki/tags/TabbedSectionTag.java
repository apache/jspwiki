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
    private static final long serialVersionUID = 1702437933960026481L;
    private String       m_defaultTabId;
    private String       m_firstTabId;
    private boolean      m_defaultTabFound = false;
  
    private StringBuffer m_buffer = new StringBuffer(BUFFER_SIZE);
    
    private static final int FIND_DEFAULT_TAB = 0;
    private static final int GENERATE_TABMENU = 1;
    private static final int GENERATE_TABBODY = 2;
    
    private static final int BUFFER_SIZE      = 1024;
    
    private              int state            = FIND_DEFAULT_TAB; 

    public void release()
    {
        super.release();
        m_defaultTabId = m_firstTabId = null;
        m_defaultTabFound = false;
        m_buffer = new StringBuffer();
        state = FIND_DEFAULT_TAB;
    }
    
    public void setDefaultTab(String anDefaultTabId)
    {
        m_defaultTabId = anDefaultTabId;
    }
    
    public boolean validateDefaultTab( String aTabId )
    {
        if( m_firstTabId == null ) m_firstTabId = aTabId;
        if( aTabId.equals( m_defaultTabId ) ) m_defaultTabFound = true;
        
        return ( aTabId.equals( m_defaultTabId ) );
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
            if( !m_defaultTabFound ) { m_defaultTabId = m_firstTabId; }
            state = GENERATE_TABMENU; 
            return EVAL_BODY_BUFFERED;
        }
        else if( isStateGenerateTabMenu() )   
        { 
            if( bodyContent != null )
            {
                m_buffer.append( "<div class=\"tabmenu\">" );
                m_buffer.append( bodyContent.getString() );
                bodyContent.clearBody();
                m_buffer.append( "</div>\n" );
            }
            state = GENERATE_TABBODY; 
            return EVAL_BODY_BUFFERED;
        }
        else if( isStateGenerateTabBody() ) 
        { 
            if( bodyContent != null )
            {      
                m_buffer.append( "<div class=\"tabs\">" );
                m_buffer.append( bodyContent.getString() );
                bodyContent.clearBody();
                m_buffer.append( "<div style=\"clear:both; height:0px;\" > </div>\n</div>\n" );
            }
            return SKIP_BODY; 
        }
        return SKIP_BODY; 
    }
    
    public int doEndTag() throws JspTagException
    {
        try
        {
            if( m_buffer.length() > 0 ) 
            {
                getPreviousOut().write( m_buffer.toString() );
            }
        }
        catch(java.io.IOException e)
        {
            throw new JspTagException( "IO Error: " + e.getMessage() );
        }
        
        //now reset some stuff for the next run -- ugh.
        m_buffer    = new StringBuffer(BUFFER_SIZE);
        state = FIND_DEFAULT_TAB; 
        m_defaultTabId    = null;
        m_firstTabId      = null;
        m_defaultTabFound = false;
        return EVAL_PAGE;
    }
    
}
