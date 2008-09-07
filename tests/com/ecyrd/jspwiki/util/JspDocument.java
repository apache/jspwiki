package com.ecyrd.jspwiki.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Tree structure that represents a JSP document.
 */
class JspDocument
{
    private static final int POSITION_NOT_SET = -1;

    public static class Attribute
    {
        Attribute()
        {
            super();
        }

        public int getStart()
        {
            return start;
        }

        public int getEnd()
        {
            return end;
        }

        public String getName()
        {
            return name;
        }

        public Node getValue()
        {
            return value;
        }

        /**
         * Returns the quote character used to delimit the attribute value; by
         * definition, either ' or ".
         * 
         * @return the quote character
         */
        public char getQuoteChar()
        {
            return quote;
        }

        private String name = null;

        private Node value = new JspDocument.Node( Node.Type.UNDEFINED );

        private int start = POSITION_NOT_SET;

        private int end = POSITION_NOT_SET;

        private char quote = QUOTE_NOT_SET;
    }

    /**
     * Element that has been parsed.
     */
    public static class Node
    {
        public enum Type
        {
            /** Root node */
            ROOT("ROOT", null, null),
            /** Text node */
            TEXT("TEXT", "", ""),
            /** HTML start tag */
            HTML_START_TAG("HTML_START_TAG", "<", ">"),
            /** HTML end tag */
            HTML_END_TAG("HTML_END_TAG", "</", ">"),
            /** HTML end tag */
            HTML_COMBINED_TAG("HTML_COMBINED_TAG", "<", "/>"),
            /** HTML tag, but not sure whether it's a start, end or combined tag. */
            UNDEFINED_HTML_TAG("UNDEFINED_HTML_TAG", "<", null),
            /** JSP comments, e.g., &lt;%-- comment --%&gt; */
            JSP_COMMENT("JSP_COMMENT", "<%--", "--%>"),
            /**
             * JSP declaration, e.g., &lt;%! declaration; [ declaration; ]+ ...
             * %&gt;
             */
            JSP_DECLARATION("JSP_DECLARATION", "<%!", "%>"),
            /** JSP expression, e.g., &lt;%= expression %&gt; */
            JSP_EXPRESSION("JSP_EXPRESSION", "<%=", "%>"),
            /**
             * JSP scriptlet, e.g., &lt;% code fragment %&gt;. Note the
             * whitespace after the %.
             */
            SCRIPTLET("SCRIPTLET", "<%", "%>"),
            /**
             * JSP page, import or taglib directive, e.g., &lt;%@ include...
             * %&gt; &lt;%@ page... %&gt; &lt;%@ taglib... %&gt;
             */
            JSP_DIRECTIVE("JSP_DIRECTIVE", "<%@", "%>"),
            /** JSP tag, but not sure what kind.. */
            UNDEFINED_JSP_TAG("UNEFINED_JSP_TAG", null, null),
            /** Parser has seen &lt;, but hasn't figured out what it is yet. */
            UNDEFINED("UNDEFINED", null, null);
            Type( String description, String tagStart, String tagEnd )
            {
                this.description = description;
                this.tagStart = tagStart;
                this.tagEnd = tagEnd;
            }

            private final String description;

            private final String tagStart;

            private final String tagEnd;
        }

        private Node.Type type;

        private String text = null;

        private List<JspDocument.Attribute> attributes = new ArrayList<JspDocument.Attribute>();

        private int level = 0;

        private int start = POSITION_NOT_SET;

        private int end = POSITION_NOT_SET;

        private int line = POSITION_NOT_SET;

        private int col = POSITION_NOT_SET;

        private Node parent = null;

        private String name = null;

        private List<Node> children = new ArrayList<Node>();

        /**
         * Returns the value of the node, which will be equal to the text of the
         * node, minus the start and end delimiters specified for the node type.
         * Thus, a node whose text is set to <code>&lt;/endtag&gt;</code> and
         * whose type is {@link Node.Type#HTML_END_TAG} would return the value
         * <code>endtag</code>.
         * 
         * @return the value
         */
        public String getValue()
        {
            if( text == null )
            {
                return null;
            }
            return text.substring( type.tagStart.length(), text.length() - type.tagEnd.length() );
        }

        /**
         * Returns the name of the tag, if the node is an HTML start, end, or
         * combined tag, or the directive name if a JSP directive; <code>null</code> otherwise.
         * 
         * @return the tag name
         */
        public String getName()
        {
            return name;
        }

        /**
         * Returns the line of the source text the node starts on.
         * 
         * @return the line
         */
        public int getLine()
        {
            return line;
        }

        /**
         * Returns the starting character position of the node, inclusive,
         * relative to the beginning of the source text.
         * 
         * @return the start position
         */
        public int getStartChar()
        {
            return start;
        }

        public List<JspDocument.Attribute> getAttributes()
        {
            return attributes;
        }

        /**
         * Returns the exnding character position of the node, exclusive,
         * relative to the beginning of the source text.
         * 
         * @return the end position
         */
        public int getEndChar()
        {
            return end;
        }

        /**
         * Returns the type of the node.
         * 
         * @return the type
         */
        public Node.Type getType()
        {
            return type;
        }

        /**
         * Returns the column the node starts on, relative to the beginning of
         * the line in the source text.
         * 
         * @return the column
         */
        public int getColumn()
        {
            return col;
        }

        /**
         * Returns the "level" of the tag; that is, how far from the top-level
         * nodes (which are level 1).
         * 
         * @return
         */
        public int getLevel()
        {
            return level;
        }

        /**
         * Returns the "child" nodes relative to the current one.
         * 
         * @return the children
         */
        public List<Node> getChildren()
        {
            return children;
        }

        /**
         * Returns the siblings of this node.
         * 
         * @return the siblings
         */
        public List<Node> getSiblings()
        {
            List<Node> siblings = new ArrayList<Node>();
            for( Node sibling : parent.children )
            {
                if( !this.equals( sibling ) )
                {
                    siblings.add( sibling );
                }
            }
            return siblings;
        }

        /**
         * Returns the parent of this node, which may be null if a top-level
         * node.
         * 
         * @return the parent
         */
        public Node getParent()
        {
            return parent.type == Node.Type.ROOT ? null : parent;
        }

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

        /**
         * Returns <code>true</code> if the node can contain attributes,
         * <code>false</code> otherwise.
         * 
         * @return the result
         */
        public boolean isTag()
        {
            return type == Type.HTML_START_TAG || type == Type.HTML_COMBINED_TAG || type == Type.UNDEFINED_HTML_TAG;
        }

        public String toString()
        {
            return "[" + type.description + "(pos=" + line + ":" + col + ",chars=" + start + ":" + end + ",L" + level + "):\""
                   + text + "\"]";
        }
    }

    private static final char QUOTE_NOT_SET = 0;

    private final Stack<JspDocument.Node> nodeStack = new Stack<JspDocument.Node>();

    private final List<JspDocument.Node> allNodes = new ArrayList<JspDocument.Node>();

    private final List<Integer> lineBreaks = new ArrayList<Integer>();

    private Node root = null;

    /**
     * Constructs a new JspDocument.
     */
    public JspDocument()
    {
        super();
    }

    /**
     * Returns the list of nodes parsed by {@link #parse(String)}, in the order
     * parsed.
     * 
     * @return the list of nodes
     */
    public List<JspDocument.Node> getNodes()
    {
        return allNodes;
    }

    /**
     * Returns the list of nodes of a specified type as parsed by
     * {@link #parse(String)}, in the order parsed.
     * 
     * @return the list of nodes
     */
    public List<JspDocument.Node> getNodes( JspDocument.Node.Type type )
    {
        List<JspDocument.Node> typeNodes = new ArrayList<JspDocument.Node>();
        for( JspDocument.Node node : allNodes )
        {
            if( node.type == type )
            {
                typeNodes.add( node );
            }
        }
        return typeNodes;
    }

    /**
     * Factory method that constructs and returns a new Node. This node is
     * appended to the internal list of Nodes. When constructed, the node's
     * start position, line number, column number and level are set
     * automatically based on JspDocument's internal cache of line-breaks and
     * nodes.
     * 
     * @param type the node type
     * @param pos the start position for the node
     * @return the new Node
     */
    protected JspDocument.Node createNode( JspDocument.Node.Type type, int pos )
    {
        // If last node added has no length (i.e., is null), get rid of it
        Node parent = nodeStack.peek();
        if( allNodes.size() > 0 )
        {
            JspDocument.Node lastNode = allNodes.get( allNodes.size() - 1 );
            if( lastNode.start == lastNode.end )
            {
                allNodes.remove( lastNode );
                parent.children.remove( lastNode );
            }
        }

        // Create new Node and set parent/child relationships
        JspDocument.Node node = new JspDocument.Node( type );
        node.parent = parent;
        parent.children.add( node );
        allNodes.add( node );

        // Set the start, end, linebreak and level
        node.start = pos;
        int lastLineBreakPos = lineBreaks.size() == 0 ? POSITION_NOT_SET : lineBreaks.get( lineBreaks.size() - 1 );
        node.line = lineBreaks.size() + 1;
        node.col = pos - lastLineBreakPos;
        node.level = nodeStack.size();

        return node;
    }

    /**
     * Parses a JSP file, supplied as a String, into Nodes.
     * 
     * @param source the JSP file contents
     */
    public void parse( String source )
    {
        // First, break the raw text into Nodes
        parseNodes( source );

        // Now, visit each Node and extract attributes (for HTML tags) or the directive name (for JSP directives)
        for( Node node : allNodes )
        {
            if( node.isTag() || node.getType() == Node.Type.JSP_DIRECTIVE )
            {
                String nodeValue = node.getValue().substring( node.getValue().indexOf( node.name ) + node.name.length() );
                parseAttributes( node, nodeValue );
            }
        }
    }
    
    /**
     * Extracts the attributes for a supplied Node.
     * @param source the text to parse
     */
    protected void parseAttributes( Node node, String source )
    {
        // Get the node text starting just after the tag name.
        if ( source.length() == 0 )
        {
            return;
        }

        // Parse the node's value string, character by character
        int pos = 0;
        JspDocument.Attribute attribute = null;
        for( char ch : source.toCharArray() )
        {
            switch( ch )
            {
                case ('<'): {
                    // If attribute already set, the left bracket will always
                    // signal a nested JSP expression
                    if( attribute != null && attribute.value.start == POSITION_NOT_SET )
                    {
                        attribute.value.start = pos;
                        attribute.value.type = JspDocument.Node.Type.JSP_EXPRESSION;
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
                    // If no current attribute, whitespace marks the
                    // start
                    if( attribute == null )
                    {
                        attribute = new Attribute();
                    }
                    break;
                }

                    // If single/double quote and HTML tag, start/stop/continue
                    // an attribute
                case ('\''):
                case ('\"'): {
                    if( attribute != null )
                    {
                        // If the same as the quote char used to start the
                        // attribute, we're done; extract the attribute value
                        if( attribute.quote == ch )
                        {
                            attribute.value.end = pos;
                            attribute.value.text = source.substring( attribute.value.start, attribute.value.end );
                            node.attributes.add( attribute );
                            attribute = null;
                        }
                        else if( attribute.quote == QUOTE_NOT_SET )
                        {
                            attribute.quote = ch;
                        }
                        break;
                    }
                }

                case ('='):
                {
                    if( attribute != null && attribute.name == null )
                    {
                        attribute.name = source.substring( attribute.start, pos );
                    }
                    break;
                }

                default: {
                    // If inside an attribute, set the start position if not set
                    if( attribute != null )
                    {
                        if( attribute.start == POSITION_NOT_SET )
                        {
                            attribute.start = pos;
                        }
                        else if( attribute.quote != QUOTE_NOT_SET && attribute.value.start == POSITION_NOT_SET )
                        {
                            attribute.value.start = pos;
                            attribute.value.type = JspDocument.Node.Type.TEXT;
                        }
                    }
                }
            }
            pos++;
        }
    }

    /**
     * Parses a supplied String into its component Nodes. For simplicity's sake,
     * this method does <em>not</em> parse each Node's attributes.
     * 
     * @param source the string to parse
     */
    protected void parseNodes( String source )
    {
        // Clear out the node cache and stack
        nodeStack.clear();
        allNodes.clear();

        // Create a root node that contains all top-level siblings
        root = new JspDocument.Node( Node.Type.ROOT );
        nodeStack.push( root );

        // Reset the character counter
        char lastCh = ' ';
        int pos = 0;
        int directiveNameStart = POSITION_NOT_SET;
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

                    // If the last node added was a blank/empty text node,
                    // remove it
                    if( nodeStack.size() > 1 )
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

                        // If inside JSP directive and name not set, set it
                        if ( node.type == JspDocument.Node.Type.JSP_DIRECTIVE )
                        {
                            if ( node.name == null &&  directiveNameStart != POSITION_NOT_SET )
                            {
                                node.name = source.substring( directiveNameStart, pos );
                            }
                        }
                        
                        // If inside HTML tag, whitespace marks the end of the
                        // tag name
                        if( node.isTag() )
                        {
                            // Set the tag name if still empty
                            if( node.name == null )
                            {
                                node.name = source.substring( node.start + node.type.tagStart.length(), pos );
                            }
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
                        directiveNameStart = POSITION_NOT_SET;
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

                    // Equals after starting <% means JSP expression; inside
                    // attribute means end of name
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
                    // If node is still undefined, any other character is by
                    // definition part of a text block
                    if( node.type == JspDocument.Node.Type.UNDEFINED )
                    {
                        node.type = JspDocument.Node.Type.UNDEFINED_HTML_TAG;
                    }
                    
                    // If in JSP directive and name start not set, start it here
                    else if ( node.type == JspDocument.Node.Type.JSP_DIRECTIVE && directiveNameStart == POSITION_NOT_SET )
                    {
                        directiveNameStart = pos;
                    }
                }
            }

            // Increment the character position and line metrics
            lastCh = ch;
            pos++;
        }

        // Set the end point for the last node
        if( node != null )
        {
            node.end = pos;
            if( node.start == node.end )
            {
                allNodes.remove( node );
                node.parent.children.remove( node );
            }
            else
            {
                node.text = source.substring( node.start, node.end );
            }
        }
    }

}
