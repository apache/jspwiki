/* 
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package org.apache.wiki.tags;

import org.apache.log4j.Logger;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.plugin.PluginManager;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import java.io.IOException;
import java.util.Map;

/**
 *  Inserts any Wiki plugin.  The body of the tag becomes then
 *  the body for the plugin.
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>plugin - name of the plugin you want to insert.
 *    <LI>args   - An argument string for the tag.
 *  </UL>
 *
 *  @since 2.0
 */
public class PluginTag
    extends WikiBodyTag
{
    private static final long serialVersionUID = 0L;
    private static final Logger log = Logger.getLogger( PluginTag.class );
    
    private String m_plugin;
    private String m_args;

    private boolean m_evaluated;

    /**
     *  {@inheritDoc}
     */
    @Override
    public void release()
    {
        super.release();
        m_plugin = m_args = null;
        m_evaluated = false;
    }
    
    /**
     *  Set the name of the plugin to execute.
     *  
     *  @param p Name of the plugin.
     */
    public void setPlugin( final String p )
    {
        m_plugin = p;
    }

    /**
     *  Set the argument string to the plugin.
     *  
     *  @param a Arguments string.
     */
    public void setArgs( final String a )
    {
        m_args = a;
    }
    
    /**
     *  {@inheritDoc}
     */
    @Override
    public int doWikiStartTag() throws JspException, IOException
    {
        m_evaluated = false;
        return EVAL_BODY_BUFFERED;
    }

    private String executePlugin( final String plugin, final String args, final String body ) throws PluginException, IOException {
        final Engine engine = m_wikiContext.getEngine();
        final PluginManager pm  = engine.getManager( PluginManager.class );

        m_evaluated = true;

        final Map<String, String> argmap = pm.parseArgs( args );
        
        if( body != null ) 
        {
            argmap.put( "_body", body );
        }

        return pm.execute( m_wikiContext, plugin, argmap );
    }
    
    /**
     *  {@inheritDoc}
     */
    @Override
    public int doEndTag()
        throws JspException
    {
        if( !m_evaluated )
        {
            try
            {
                pageContext.getOut().write( executePlugin( m_plugin, m_args, null ) );
            }
            catch( final Exception e )
            {
                log.error( "Failed to insert plugin", e );
                throw new JspException( "Tag failed, check logs: "+e.getMessage() );
            }
        }
        return EVAL_PAGE;
    }
    
    /**
     *  {@inheritDoc}
     */
    @Override
    public int doAfterBody()
        throws JspException
    {
        try
        {
            final BodyContent bc = getBodyContent();
            
            getPreviousOut().write( executePlugin( m_plugin, m_args, (bc != null) ? bc.getString() : null) );
        }
        catch( final Exception e )
        {
            log.error( "Failed to insert plugin", e );
            throw new JspException( "Tag failed, check logs: "+e.getMessage() );
        }
        
        return SKIP_BODY;
    }
}
