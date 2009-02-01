/* 
    JSPWiki - a JSP-based WikiWiki clone.

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

package org.apache.wiki.ui.migrator;

/**
 * Represents markup in a JSP that is not a {@link Tag} or {@link Attribute},
 * such as text, an HTML comment, a JSP comment, a JSP declaration, scriptlet or
 * a JSP expression.
 */
public class Text extends AbstractNode
{

    private String m_value = null;

    public Text( JspDocument doc )
    {
        super( doc, NodeType.TEXT );
    }

    public Text( JspDocument doc, NodeType type )
    {
        super( doc, type );
    }

    /**
     * Always returns \"(TEXT)\".
     */
    @Override
    public String getName()
    {
        return "(TEXT)";
    }

    public String getValue()
    {
        return m_value;
    }

    public void setName( String name )
    {
        throw new UnsupportedOperationException( "Text nodes cannot have names." );
    }

    public void setValue( String value )
    {
        m_value = value;
    }

    /**
     * Returns the string that represents the Tag, including the name and
     * attributes, but not any child nodes.
     */
    public String toString()
    {
        return m_type.getTagStart() + m_value + m_type.getTagEnd();
    }
}
