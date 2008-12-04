package com.ecyrd.jspwiki.ui.migrator;

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
