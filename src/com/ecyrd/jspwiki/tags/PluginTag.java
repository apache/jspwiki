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
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.plugin.PluginManager;

/**
 *  Inserts any Wiki plugin.  The body of the tag becomes then
 *  the body for the plugin.
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>plugin - name of the plugin you want to insert.
 *    <LI>args   - An argument string for the tag.
 *  </UL>
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class PluginTag
    extends WikiBodyTag
{
    private String m_plugin;
    private String m_args;

    private boolean m_evaluated = false;
    
    public void setPlugin( String p )
    {
        m_plugin = p;
    }

    public void setArgs( String a )
    {
        m_args = a;
    }
    
    public int doWikiStartTag() throws JspException, IOException
    {
        m_evaluated = false;
        return EVAL_BODY_BUFFERED;
    }

    private String executePlugin( String plugin, String args, String body )
        throws PluginException, IOException
    {
        WikiEngine engine = m_wikiContext.getEngine();
        PluginManager pm  = engine.getPluginManager();

        m_evaluated = true;

        Map argmap = pm.parseArgs( args );
        
        if( body != null ) 
        {
            argmap.put( "_body", body );
        }

        String result = pm.execute( m_wikiContext, plugin, argmap );

        return result;        
    }
    
    public int doEndTag()
        throws JspException
    {
        if( !m_evaluated )
        {
            try
            {
                pageContext.getOut().write( executePlugin( m_plugin, m_args, null ) );
            }
            catch( Exception e )
            {
                log.error( "Failed to insert plugin", e );
                throw new JspException( "Tag failed, check logs: "+e.getMessage() );
            }
        }
        return EVAL_PAGE;
    }
    
    public int doAfterBody()
        throws JspException
    {
        try
        {
            BodyContent bc = getBodyContent();
            
            getPreviousOut().write( executePlugin( m_plugin, m_args, ((bc != null) ? bc.getString() : null) ) );
        }
        catch( Exception e )
        {
            log.error( "Failed to insert plugin", e );
            throw new JspException( "Tag failed, check logs: "+e.getMessage() );
        }
        
        return SKIP_BODY;
    }
}
