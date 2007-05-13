/*
 * Tab tag 
 * Dirk Frederickx, Jan 06
 */


package com.ecyrd.jspwiki.tags;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

/**
 *  Generates single tabbed page layout.
 *  Works together with the tabbedSection javacript.
 *
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>id - ID for this tab. (mandatory)
 *    <LI>title - Title of this tab. (mandatory)
 *    <LI>accesskey - Single char usable as quick accesskey (alt- or ctrl-) (optional)
 *  </UL>
 *
 *  @author Dirk Frederickx
 *  @since v2.3.63
 */

public class TabTag extends TagSupport
{
    private static final long serialVersionUID = -8534125226484616489L;
    private String m_accesskey;
    private String m_tabID;
    private String m_tabTitle;

    public void release()
    {
        super.release();
        m_accesskey = m_tabID = m_tabTitle = null;
    }

    public void setId(String aTabID)
    {
        m_tabID = aTabID;
    }

    public void setTitle(String aTabTitle)
    {
        m_tabTitle = aTabTitle;
    }

    public void setAccesskey(String anAccesskey)
    {
        m_accesskey = anAccesskey; //take only the first char
    }

    // insert <u> ..accesskey.. </u> in title
    private boolean handleAccesskey()
    {
        if( (m_tabTitle == null) || (m_accesskey == null) ) return( false );

        int pos = m_tabTitle.toLowerCase().indexOf( m_accesskey.toLowerCase() );
        if( pos > -1 )
        {
            m_tabTitle = m_tabTitle.substring( 0, pos ) + "<u>" 
                       + m_tabTitle.charAt( pos ) + "</u>" + m_tabTitle.substring( pos+1 );
        }
        return( true );
    }

    public int doStartTag() throws JspTagException
    {
        TabbedSectionTag parent=(TabbedSectionTag)findAncestorWithClass( this, TabbedSectionTag.class );

        if( m_tabID == null )
        {
            throw new JspTagException("Tab Tag without \"id\" attribute");
        }
        if( m_tabTitle == null )
        {
            throw new JspTagException("Tab Tag without \"tabTitle\" attribute");
        }
        if( parent == null )
        {
            throw new JspTagException("Tab Tag without parent \"TabbedSection\" Tag");
        }

        if( !parent.isStateGenerateTabBody() ) return SKIP_BODY;
    
        StringBuffer sb = new StringBuffer();

        sb.append( "<div id=\""+ m_tabID + "\"" );

        if( !parent.validateDefaultTab( m_tabID) )
        {
            sb.append( " style=\"display:none;\"" );
        }
        sb.append( " >\n" );

        try
        {
            pageContext.getOut().write( sb.toString() );
        }
        catch( java.io.IOException e )
        {
            throw new JspTagException( "IO Error: " + e.getMessage() );
        }

        return EVAL_BODY_INCLUDE;
    }

    public int doEndTag() throws javax.servlet.jsp.JspTagException
    {
        TabbedSectionTag parent=(TabbedSectionTag)findAncestorWithClass( this, TabbedSectionTag.class );

        StringBuffer sb = new StringBuffer();

        if( parent.isStateFindDefaultTab() )
        {
            //inform the parent of each tab
            parent.validateDefaultTab( m_tabID ); 
        }
        else if( parent.isStateGenerateTabBody() )
        {
            sb.append( "</div>\n" );
        }
        else if( parent.isStateGenerateTabMenu() )
        {
            sb.append( "<span><a" );

            if( parent.validateDefaultTab( m_tabID ) )
            {
                sb.append( " class=\"activetab\"" );
            }

            sb.append( " id=\"menu-" + m_tabID + "\"" );
            sb.append( " onclick=\"TabbedSection.onclick(\'" + m_tabID + "\')\"" );

            if( handleAccesskey() )
            {
                sb.append( " accesskey=\"" + m_accesskey + "\"" );
            }

            sb.append( " >" );
            sb.append( m_tabTitle );
            sb.append( "</a></span>" );
        }
 
        try
        {
            pageContext.getOut().write( sb.toString() );
        }
        catch( java.io.IOException e )
        {
            throw new JspTagException( "IO Error: " + e.getMessage() );
        }
 
        return EVAL_PAGE;
    }
}
