/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2004 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.tags;

import java.io.IOException;
import java.io.StringReader;
import javax.servlet.jsp.PageContext;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;

import org.apache.ecs.xhtml.*;
import org.apache.ecs.GenericElement;
import org.apache.ecs.ConcreteElement;

/**
 *  Creates an editor component with all the necessary parts
 *  to get it working.
 *  <p>
 *  In the future, this component should be expanded to provide
 *  a customized version of the editor according to user preferences.
 *
 *  @author Janne Jalkanen
 *  @since 2.2
 */
public class EditorTag
    extends WikiTagBase
{
    private String m_submit  = "Save";
    private String m_preview = "Preview";
    private String m_cancel  = "Cancel";

    public void setSubmit( String s )
    {
        m_submit = s;
    }

    public void setPreview( String s )
    {
        m_preview = s;
    }

    public void setCancel( String s )
    {
        m_cancel = s;
    }

    private GenericElement createInput( String name, String value )
    {
        input in = new input();
        in.setType("hidden");
        in.setName( name );
        in.setValue( value );

        return in;
    }

    private GenericElement createSubmit( String name, String value )
    {
        input in = new input();
        in.setType("submit");
        in.setName( name );
        in.setValue( value );

        return in;
    }

    private ConcreteElement createSimpleEditor()
    {
        WikiEngine engine = m_wikiContext.getEngine();
        WikiPage   page   = m_wikiContext.getPage();

        div d = new div();
        d.setClass("editor");

        form f = new form();
        f.setName("editForm");
        f.setAction( m_wikiContext.getURL( WikiContext.EDIT, page.getName() ) );
        f.setMethod( "POST" );
        f.setAcceptCharset( engine.getContentEncoding() );

        d.addElement( f );

        p para1 = new p();

        f.addElement(para1);

        para1.addElement( createInput("page", page.getName() ) );
        para1.addElement( createInput("action", "save" ) );
        para1.addElement( createInput("edittime",
                                      (String)pageContext.getAttribute("lastchange",
                                                                       PageContext.REQUEST_SCOPE ) ) );
       
        if( m_wikiContext.getRequestContext().equals("comment") )
        {
            para1.addElement( createInput("comment","true") );
        }

        textarea area = new textarea();

        area.setClass("editor");
        area.setWrap("virtual");
        area.setName("text");
        area.setRows( 25 );
        area.setCols( 80 );
        area.setStyle( "width:100%;" );
       
        if( m_wikiContext.getRequestContext().equals("edit") )
        {
            area.addElement( engine.getText( m_wikiContext, page ) );
        }

        para1.addElement( area );

        //
        //  The edit buttons block
        //

        p para2 = new p();

        f.addElement( para2 );
        para2.addElement( createSubmit( "ok",      m_submit ) );
        para2.addElement( "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" );
        para2.addElement( createSubmit( "preview", m_preview ) );
        para2.addElement( "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" );
        para2.addElement( createSubmit( "cancel",  m_cancel ) );

        return d;
    }

    public final int doWikiStartTag()
        throws IOException
    {
        ConcreteElement editor = createSimpleEditor();

        pageContext.getOut().print( editor.toString() );

        return SKIP_BODY;
    }
}
