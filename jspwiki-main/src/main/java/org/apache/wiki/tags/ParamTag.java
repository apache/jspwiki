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

import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.Tag;

/**
 * ParamTag submits name-value pairs to the first enclosing 
 * ParamHandler instance. Name and value are strings, and can
 * be given as tag attributes, or alternatively the value can be 
 * given as the body contents of this tag. 
 * <p>
 * The name-value pair is passed to the closest containing 
 * ancestor tag that implements ParamHandler. 
 */
public class ParamTag 
    extends BodyTagSupport
{

    private static final long serialVersionUID = -4671059568218551633L;
    private String m_name;
    private String m_value;
    
    /**
     *  {@inheritDoc}
     */
    @Override
    public void release() 
    {
        m_name = m_value = null;
    }
    
    /**
     *  Set the name of the parameter to transfer.
     *  
     *  @param s The name.
     */
    public void setName( String s ) 
    {
        m_name = s;
    }
    
    /**
     *  Set the value of the parameter to transfer.
     *  
     *  @param s The value.
     */
    public void setValue( String s ) 
    {
        m_value = s;
    }
    
    /**
     *  {@inheritDoc}
     */
    @Override
    public int doEndTag()
    {
        Tag t = null;
        do
        {
            t = getParent();
        } while (t != null && !(t instanceof ParamHandler));

        if( t != null )
        {
            String val = m_value;
            if( val == null )
            {
                BodyContent bc = getBodyContent();
                if( bc != null ) 
                {
                    val = bc.getString();
                }
            }
            if( val != null ) 
            {
                ((ParamHandler)t).setContainedParameter( m_name, val );
            }
        }
        
        
        return EVAL_PAGE;
    }
}
