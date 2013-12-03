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

import java.io.IOException;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.JspException;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.NoSuchVariableException;
import org.apache.wiki.util.TextUtil;

/**
 *  Returns the value of an Wiki variable.
 *
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>var - Name of the variable.  Required.
 *    <LI>default - Revert to this value, if the value of "var" is null.
 *                  If left out, this tag will produce a concise error message
 *                  if the named variable is not found. Set to empty (default="")
 *                  to hide the message.
 *  </UL>
 *
 *  <P>A default value implies <I>failmode='quiet'</I>.
 *
 *  @since 2.0
 */
public class VariableTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 0L;

    private String m_var      = null;
    private String m_default  = null;

    public void initTag()
    {
        super.initTag();
        m_var = m_default = null;
    }

    public String getVar()
    {
        return m_var;
    }

    public void setVar( String arg )
    {
        m_var = arg;
    }

    public void setDefault( String arg )
    {
        m_default = arg;
    }

    public final int doWikiStartTag()
        throws JspException,
               IOException
    {
        WikiEngine engine   = m_wikiContext.getEngine();
        JspWriter out = pageContext.getOut();
        String msg = null;
        String value = null;

        try
        {
            value = engine.getVariableManager().getValue( m_wikiContext,
                                                          getVar() );
        }
        catch( NoSuchVariableException e )
        {
            msg = "No such variable: "+e.getMessage();
        }
        catch( IllegalArgumentException e )
        {
            msg = "Incorrect variable name: "+e.getMessage();
        }

        if( value == null )
        {
            value = m_default;
        }

        if( value == null )
        {
            value = msg;
        }
        out.write( TextUtil.replaceEntities(value) );
        return SKIP_BODY;
    }
}
