package com.ecyrd.jspwiki.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * Tree structure that represents a JSP document.
 */
class JspDocument
{
    /**
     * Element that has been parsed.
     */
    public static class Node
    {
        public enum Type
        {
            /** Root node */
            ROOT("ROOT"),
            /** Text node */
            TEXT("TEXT"),
            /** HTML start tag */
            HTML_START_TAG("HTML_START_TAG"),
            /** HTML end tag */
            HTML_END_TAG("HTML_END_TAG"),
            /** HTML end tag */
            HTML_COMBINED_TAG("HTML_COMBINED_TAG"),
            /** HTML tag, but not sure whether it's a start, end or combined tag. */
            UNDEFINED_HTML_TAG("UNDEFINED_HTML_TAG"),
            /** JSP comments, e.g., &lt;%-- comment --%&gt; */
            JSP_COMMENT("JSP_COMMENT"),
            /**
             * JSP declaration, e.g., &lt;%! declaration; [ declaration; ]+ ...
             * %&gt;
             */
            JSP_DECLARATION("JSP_DECLARATION"),
            /** JSP expression, e.g., &lt;%= expression %&gt; */
            JSP_EXPRESSION("JSP_EXPRESSION"),
            /**
             * JSP scriptlet, e.g., &lt;% code fragment %&gt;. Note the
             * whitespace after the %.
             */
            SCRIPTLET("SCRIPTLET"),
            /**
             * JSP page, import or taglib directive, e.g., &lt;%@ include...
             * %&gt; &lt;%@ page... %&gt; &lt;%@ taglib... %&gt;
             */
            JSP_DIRECTIVE("JSP_DIRECTIVE"),
            /** JSP tag, but not sure what kind.. */
            UNDEFINED_JSP_TAG("UNEFINED_JSP_TAG"),
            /** Parser has seen &lt;, but hasn't figured out what it is yet. */
            UNDEFINED("UNDEFINED");
            Type( String description )
            {
                this.description = description;
            }
    
            private String description;
        }
    
        private Node.Type type;
    
        private String text = null;
        
        private String revisedText = null;
    
        private int level = 0;
        
        private int start = -1;
    
        private int end = -1;
    
        private int line = -1;
    
        private int col = -1;
        
        private Node parent = null;
        
        private List<Node> children = new LinkedList<Node>();
    
        private List<Node> siblings = new LinkedList<Node>();
        
        /**
         * Constructs a new Node. Callers should use the
         * {@link JspDocument#createNode(com.ecyrd.jspwiki.util.JspDocument.Node.Type, int)}
         * method instead
         * 
         * @param type the node type
         */
        private Node( Node.Type type )
        {
            this.type = type;
        }
    
        public boolean isJspNode()
        {
            return type == Type.JSP_COMMENT || type == Type.JSP_DECLARATION || type == Type.JSP_EXPRESSION
                   || type == Type.SCRIPTLET || type == Type.JSP_DIRECTIVE || type == Type.UNDEFINED_JSP_TAG;
        }
    
        public String toString()
        {
            return "[" + type.description + "(pos=" + line + ":" + col + ",chars=" + start + ":" + end + ",L" + level + "):\"" + text + "\"]";
        }
    }

    private final Stack<JspDocument.Node> nodeStack = new Stack<JspDocument.Node>();

    private final List<JspDocument.Node> allNodes = new LinkedList<JspDocument.Node>();
    
    private final List<Integer> lineBreaks = new LinkedList<Integer>();
    
    private Node root = null;
    
    /**
     * Constructs a new JspDocument.
     */
    public JspDocument()
    {
        super();
        
        // Add a fake root node, then erase it from the list of "all nodes"
        createNode( Node.Type.ROOT, 0 );
        nodeStack.push( root );
        allNodes.clear();
    }
    
    /**
     * Returns the list of nodes parsed by {@link #parse(String)}, in the order parsed. 
     * @return the list of nodes
     */
    public List<JspDocument.Node> getNodes()
    {
        return allNodes;
    }
    
    /**
     * Returns the list of nodes of a specified type as parsed by {@link #parse(String)}, in the order parsed. 
     * @return the list of nodes
     */
    public List<JspDocument.Node> getNodes( JspDocument.Node.Type type )
    {
        List<JspDocument.Node> typeNodes = new LinkedList<JspDocument.Node>();
        for ( JspDocument.Node node : allNodes )
        {
            if ( node.type == type )
            {
                typeNodes.add( node );
            }
        }
        return typeNodes;
    }

    /**
     * Factory method that constructs and returns a new Node. This node is
     * appended to the internal list of Nodes. When constructed, the node's
     * start position, line number, column number and level are set automatically
     * based on JspDocument's internal cache of line-breaks and nodes.
     * 
     * @param type the node type
     * @param pos the start position for the node
     * @return the new Node
     */
    public JspDocument.Node createNode( JspDocument.Node.Type type, int pos )
    {
        JspDocument.Node node = new JspDocument.Node( type );
        node.start = pos;
        int lastLineBreakPos =lineBreaks.size() == 0 ? -1 : lineBreaks.get( lineBreaks.size() - 1 ); 
        node.line = lineBreaks.size() + 1;
        node.col = pos - lastLineBreakPos;
        node.level = nodeStack.size();
        
        // If last node added has no length (i.e., is null), get rid of it
        if ( allNodes.size() > 0 )
        {
            JspDocument.Node lastNode = allNodes.get( allNodes.size() - 1 );
            if ( lastNode.start == lastNode.end )
            {
                allNodes.remove( allNodes.size() - 1 );
            }
        }
        
        // Add the new node
        allNodes.add( node );
        
        // Set parent/child relationship, if Node is on stack
        if (nodeStack.size() > 0 )
        {
            Node parent = nodeStack.peek();
            node.parent = parent;
            parent.children.add( node );
        }
        
        // If no root yet, set it
        if ( root == null )
        {
            root = node;
        }
        else
        {
            root.end = node.end;
        }
        return node;
    }

    /**
     * Parses a JSP file, supplied as a String, into Nodes.
     * 
     * @param source the JSP file contents
     */
    public void parse( String source )
    {
        nodeStack.clear();
        allNodes.clear();
        char lastCh = ' ';
        int pos = 0;
        JspDocument.Node node = createNode( JspDocument.Node.Type.TEXT, pos );
        
        // Parse the file, character by character
        for( char ch : source.toCharArray() )
        {
            switch( ch )
            {
                // The left angle bracket ALWAYS starts a new node
                case ('<'): {
                    node.end = pos;
                    if( node.type == JspDocument.Node.Type.TEXT )
                    {
                        if( pos > 0 )
                        {
                            if( node.end > node.start )
                            {
                                node.text = source.substring( node.start, node.end );
                            }
                        }
                    }
                    else
                    {
                        nodeStack.push( node );
                    }
                    node = createNode( JspDocument.Node.Type.UNDEFINED, pos );
                    break;
                }

                    // Slash following left angle bracket </ means HTML
                    // end-tag
                    // if
                    // we're not inside JSP item
                case ('/'): {
                    if( lastCh == '<' && !node.isJspNode() )
                    {
                        node.type = JspDocument.Node.Type.HTML_END_TAG;
                    }
                    break;
                }

                    // Right angle bracket always means end of a tag
                case ('>'): {

                    node.end = pos + 1;
                    node.text = source.substring( node.start, node.end );
                    if( node.type == JspDocument.Node.Type.UNDEFINED_HTML_TAG )
                    {
                        if( lastCh == '/' )
                        {
                            node.type = JspDocument.Node.Type.HTML_COMBINED_TAG;
                        }
                        else
                        {
                            node.type = JspDocument.Node.Type.HTML_START_TAG;
                        }
                    }

                    // Cleanup
                    if( nodeStack.size() > 0 )
                    {
                        node = nodeStack.pop();
                    }
                    else
                    {
                        node = createNode( JspDocument.Node.Type.TEXT, pos + 1 );
                    }
                    break;
                }

                case (' '):
                case ('\t'):
                case ('\r'):
                case ('\n'):
                case ('\u000B'):
                case ('\u000C'):
                case ('\u001C'):
                case ('\u001D'):
                case ('\u001E'):
                case ('\u001F'): {
                    if( node.type != JspDocument.Node.Type.TEXT )
                    {
                        // If inside the start of JSP tag, space following
                        // percent means scriptlet
                        if( node.type == JspDocument.Node.Type.UNDEFINED_JSP_TAG )
                        {
                            if( lastCh == '%' )
                            {
                                node.type = JspDocument.Node.Type.SCRIPTLET;
                            }
                        }
                        else if( node.type == JspDocument.Node.Type.UNDEFINED )
                        {
                            node.type = JspDocument.Node.Type.UNDEFINED_HTML_TAG;
                        }
                    }
                    
                    // Reset the line/column counters if we encounter linebreaks
                    if( ch == '\r' || ch == '\n' )
                    {
                        lineBreaks.add( pos );
                    }
                    break;
                }

                    // Percent after starting < means JSP tag of some kind
                case ('%'): {
                    if( node.type == JspDocument.Node.Type.UNDEFINED )
                    {
                        node.type = JspDocument.Node.Type.UNDEFINED_JSP_TAG;
                    }
                    break;
                }

                    // Dash after starting <% means hidden JSP comment
                case ('-'): {
                    if( node.type == JspDocument.Node.Type.UNDEFINED_JSP_TAG && lastCh == '%' )
                    {
                        node.type = JspDocument.Node.Type.JSP_COMMENT;
                    }
                    break;
                }

                    // Bang after starting <% means JSP declaration
                case ('!'): {
                    if( node.type == JspDocument.Node.Type.UNDEFINED_JSP_TAG && lastCh == '%' )
                    {
                        node.type = JspDocument.Node.Type.JSP_DECLARATION;
                    }
                    break;
                }

                    // Equals after starting <% means JSP expression
                case ('='): {
                    if( node.type == JspDocument.Node.Type.UNDEFINED_JSP_TAG && lastCh == '%' )
                    {
                        node.type = JspDocument.Node.Type.JSP_EXPRESSION;
                    }
                    break;
                }

                    // At-sign after starting <% means JSP directive
                case ('@'): {
                    if( node.type == JspDocument.Node.Type.UNDEFINED_JSP_TAG && lastCh == '%' )
                    {
                        node.type = JspDocument.Node.Type.JSP_DIRECTIVE;
                    }
                    break;
                }

                default: {
                    if( node.type == JspDocument.Node.Type.UNDEFINED )
                    {
                        node.type = JspDocument.Node.Type.UNDEFINED_HTML_TAG;
                    }
                }
            }

            // Increment the character position and line metrics
            lastCh = ch;
            pos++;
        }
        
        // Set the end point for the last node
        if ( node != null )
        {
            node.end = pos;
            if (node.start == node.end)
            {
                allNodes.remove( allNodes.size() -1 );
            }
        }
    }

}