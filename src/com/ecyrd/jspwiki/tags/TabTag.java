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
    private String accesskey;
    private String tabID;
    private String tabTitle;

    public void setId(String aTabID)
    {
        tabID = aTabID;
    }

    public void setTitle(String aTabTitle)
    {
        tabTitle = aTabTitle;
    }

    public void setAccesskey(String anAccesskey)
    {
        accesskey = anAccesskey; //take only the first char
    }

    // insert <u> ..accesskey.. </u> in title
    private boolean handleAccesskey()
    {
        if( (tabTitle == null) || (accesskey == null) ) return( false );

        int pos = tabTitle.toLowerCase().indexOf( accesskey.toLowerCase() );
        if( pos > -1 )
        {
            tabTitle = tabTitle.substring( 0, pos ) + "<u>" 
                       + tabTitle.charAt( pos ) + "</u>" + tabTitle.substring( pos+1 );
        }
        return( true );
    }

    public int doStartTag() throws JspTagException
    {
        TabbedSectionTag parent=(TabbedSectionTag)findAncestorWithClass( this, TabbedSectionTag.class );

        if( tabID == null )
        {
            throw new JspTagException("Tab Tag without \"id\" attribute");
        }
        if( tabTitle == null )
        {
            throw new JspTagException("Tab Tag without \"tabTitle\" attribute");
        }
        if( parent == null )
        {
            throw new JspTagException("Tab Tag without parent \"TabbedSection\" Tag");
        }

        if( !parent.isStateGenerateTabBody() ) return SKIP_BODY;
    
        StringBuffer sb = new StringBuffer();

        sb.append( "<div id=\""+ tabID + "\"" );

        if( !parent.validateDefaultTab( tabID) )
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
            parent.validateDefaultTab( tabID ); 
        }
        else if( parent.isStateGenerateTabBody() )
        {
            sb.append( "</div>\n" );
        }
        else if( parent.isStateGenerateTabMenu() )
        {
            sb.append( "<span><a" );

            if( parent.validateDefaultTab( tabID ) )
            {
                sb.append( " class=\"activetab\"" );
            }

            sb.append( " id=\"menu-" + tabID + "\"" );
            sb.append( " onclick=\"TabbedSection.onclick(\'" + tabID + "\')\"" );

            if( handleAccesskey() )
            {
                sb.append( " accesskey=\"" + accesskey + "\"" );
            }

            sb.append( " >" );
            sb.append( tabTitle );
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