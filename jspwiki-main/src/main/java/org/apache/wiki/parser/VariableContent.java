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
package org.apache.wiki.parser;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.NoSuchVariableException;
import org.jdom2.Text;

/**
 *  Stores the contents of a WikiVariable in a WikiDocument DOM tree.
 *  <p>
 *  When the WikiDocument is rendered, if the {@link WikiContext#VAR_WYSIWYG_EDITOR_MODE} is set to {@link Boolean#TRUE}, the
 *  variable declaration is rendered instead of the variable value.
 *
 *  @since  2.4
 */
public class VariableContent extends Text {

    private static final long serialVersionUID = 1L;

    private String m_varName;
    
    /**
     *  Create a VariableContent for the given variable.
     *  
     *  @param varName The name of the variable.
     */
    public VariableContent( final String varName )
    {
        m_varName = varName;
    }
    
    /**
     *   Evaluates the variable and returns the contents. 
     *   
     *   @return The rendered value of the variable.
     */
    public String getValue() {
        String result;
        final WikiDocument root = (WikiDocument) getDocument();

        if( root == null ) {
            // See similar note in PluginContent
            return m_varName;
        }
        
        final WikiContext context = root.getContext();
        if( context == null ) {
            return "No WikiContext available: INTERNAL ERROR";
        }
    
        final Boolean wysiwygEditorMode = ( Boolean )context.getVariable( WikiContext.VAR_WYSIWYG_EDITOR_MODE );
        if( wysiwygEditorMode != null && wysiwygEditorMode ) {
            result = "[" + m_varName + "]";
        } else {
            try {
                result = context.getEngine().getVariableManager().parseAndGetValue( context, m_varName );
            } catch( final NoSuchVariableException e ) {
                result = MarkupParser.makeError( "No such variable: " + e.getMessage() ).getText(); 
            }
        }

        return StringEscapeUtils.escapeXml11( result );
    }
    
    /**
     *  Returns exactly getValue().
     *  @return Whatever getValue() returns.
     */
    public String getText() {
        return getValue();
    }

    /**
     *  Returns a debug-suitable string.
     *  @return Debug string
     */
    public String toString() {
        return "VariableElement[\"" + m_varName + "\"]";
    }

}
