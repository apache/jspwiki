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

import java.util.Map;



/**
 * Abstract implementation of JspTransformer that contains utility methods for subclasses, such as logging.
 */
public abstract class AbstractJspTransformer implements JspTransformer
{

    public abstract void transform( Map<String, Object> sharedState, JspDocument doc );

    /**
     * Prints a standard message for a node, prefixed by the line, position and character range.
     * @param node the node the message pertains to
     * @param message the message, which will be printed after the prefix
     */
    protected void message( Node node, String message )
    {
        String nodename;
        if ( node.getType() == NodeType.ATTRIBUTE )
        {
            nodename = "<" + node.getParent().getName() + "> \"" + node.getName() + "\" attribute";
        }
        else
        {
            nodename = "<" + node.getName() + ">";
        }
        System.out.println( "(line " + node.getLine() + "," + node.getColumn() + " chars " + node.getStart() + ":"
                            + node.getEnd() + ") " + nodename + " - " + message );
    }

}
