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
package org.apache.wiki.variables;

import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.NoSuchVariableException;

/**
 *  Manages variables.  Variables are case-insensitive.  A list of all available variables is on a Wiki page called "WikiVariables".
 *
 *  @since 1.9.20.
 */
public interface VariableManager {

    // FIXME: These are probably obsolete.
    String VAR_ERROR = "error";
    String VAR_MSG   = "msg";

    /** If this variable is set to false, all filters are disabled when translating. */
    String VAR_RUNFILTERS   = "jspwiki.runFilters";

    /**
     *  Parses the link and finds a value.  This is essentially used once
     *  {@link org.apache.wiki.parser.LinkParsingOperations#isVariableLink(String) LinkParsingOperations#isVariableLink(String)}
     *  has found that the link text actually contains a variable.  For example, you could pass in "{$username}" and get back
     *  "JanneJalkanen".
     *
     *  @param  context The WikiContext
     *  @param  link    The link text containing the variable name.
     *  @return The variable value.
     *  @throws IllegalArgumentException If the format is not valid (does not start with "{$", is zero length, etc.)
     *  @throws NoSuchVariableException If a variable is not known.
     */
    String parseAndGetValue( WikiContext context, String link ) throws IllegalArgumentException, NoSuchVariableException;

    /**
     *  This method does in-place expansion of any variables.  However, the expansion is not done twice, that is,
     *  a variable containing text $variable will not be expanded.
     *  <P>
     *  The variables should be in the same format ({$variablename} as in the web pages.
     *
     *  @param context The WikiContext of the current page.
     *  @param source  The source string.
     *  @return The source string with variables expanded.
     */
    String expandVariables( WikiContext context, String source );

    /**
     *  Returns the value of a named variable.  See {@link #getValue(WikiContext, String)}. The only difference is that
     *  this method does not throw an exception, but it returns the given default value instead.
     *
     *  @param context WikiContext
     *  @param varName The name of the variable
     *  @param defValue A default value.
     *  @return The variable value, or if not found, the default value.
     */
    String getValue( WikiContext context, String varName, String defValue );

    /**
     *  Shortcut to getValue(). However, this method does not throw a NoSuchVariableException, but returns null
     *  in case the variable does not exist.
     *
     *  @param context WikiContext to look the variable in
     *  @param name Name of the variable to look for
     *  @return Variable value, or null, if there is no such variable.
     *  @since 2.2 on WikiEngine, moved to VariableManager on 2.11.0
     */
    String getVariable( WikiContext context, String name );

    /**
     *  Returns a value of the named variable.  The resolving order is
     *  <ol>
     *    <li>Known "constant" name, such as "pagename", etc.  This is so
     *        that pages could not override certain constants.
     *    <li>WikiContext local variable.  This allows a programmer to
     *        set a parameter which cannot be overridden by user.
     *    <li>HTTP Session
     *    <li>HTTP Request parameters
     *    <li>WikiPage variable.  As set by the user with the SET directive.
     *    <li>jspwiki.properties
     *  </ol>
     *
     *  Use this method only whenever you really need to have a parameter that
     *  can be overridden by anyone using the wiki.
     *
     *  @param context The WikiContext
     *  @param varName Name of the variable.
     *
     *  @return The variable value.
     *
     *  @throws IllegalArgumentException If the name is somehow broken.
     *  @throws NoSuchVariableException If a variable is not known.
     */
    String getValue( WikiContext context, String varName ) throws IllegalArgumentException, NoSuchVariableException;

}
