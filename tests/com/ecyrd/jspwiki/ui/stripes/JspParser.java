package com.ecyrd.jspwiki.ui.stripes;

import java.util.*;

/**
 * Parser that reads JSP document and constructs a {@link JspDocument} with the
 * results.
 */
class JspParser
{
    private static final class Counter
    {
        private int m_pos;

        Counter()
        {
            m_pos = 0;
        }

        public void increment()
        {
            m_pos++;
        }

        public int position()
        {
            return m_pos;
        }
    }

    private enum NodeLifecycle
    {
        TAG_RESOLUTION,
        /**
         * Characters after the directive tag start (&lt;) and whitespace, but
         * before whitespace that delimit the attributes.
         */
        /**
         * Characters after the opening left bracket (&lt;), but before
         * whitespace that delimit the attributes.
         */
        TAG_NAME,
        /**
         * After &lt;@ that determines that it's a JSP element, but before we
         * know for sure what it is.
         */
        JSP_DIRECTIVE_NAME,
        /**
         * Whitespace between the node name and the first attribute, and between
         * attributes.
         */
        BETWEEN_ATTRIBUTES,
        /**
         * Characters that name an attribute, after whitespace but before the
         * equals (=) character.
         */
        ATTRIBUTE_NAME,
        /**
         * When the current character is the equals (=) character that separates
         * the attribute name and value.
         */
        ATTRIBUTE_EQUALS,
        /**
         * The opening quote, closing quote, and all characters in between, that
         * denote an attribute's value.
         */
        ATTRIBUTE_VALUE,
        /** Any outside of a tag or element (i.e., part of a text node). */
        TEXT_NODE,
        /** Any text inside of a scriptlet, JSP comment, or JSP declaration. */
        CODE_OR_COMMENT,
    }

    /**
     * Encapsulates the current state of document parsing.
     */
    private static class ParseContext
    {
        private Node m_node = null;

        private Attribute m_attribute = null;

        private Counter m_counter = null;;

        private NodeLifecycle m_stage = NodeLifecycle.TEXT_NODE;

        public ParseContext( Counter counter )
        {
            m_counter = counter;
        }
        
        private Map<NodeLifecycle,Integer> m_markers = new HashMap<NodeLifecycle,Integer>();

        /**
         * Sets a "marker" for the current stage (as reported by
         * {@link #getStage()} at the current position (as reported by
         * {@link #position()}. The marker can be retrieved later via
         * {@link #getMarker(com.ecyrd.jspwiki.ui.stripes.JspParser.NodeLifecycle)}.
         * Callers may place only one marker per lifecycle stage. Generally,
         * markers are used to set character positions that are important to
         * retrieve later, for example the position of the left angle-bracket
         * during the {@link NodeLifecycle#TAG_RESOLUTION} stage.
         */
        public void mark()
        {
            m_markers.put( getStage(), position() );
        }

        /**
         * Retrieves the position of the marker set for the current stage (as set by {@link #mark()}).
         * If no marker was set, this method returns {@link Node#POSITION_NOT_SET}.
         * @param stage the stage for which the marker position is desired
         * @return the position of the marker.
         */
        public int getMarker( NodeLifecycle stage )
        {
            Integer mark = m_markers.get( stage );
            return mark == null ? Node.POSITION_NOT_SET : mark.intValue();
        }

        public Attribute getAttribute()
        {
            return m_attribute;
        }

        public Counter getCounter()
        {
            return m_counter;
        }

        public Node getNode()
        {
            return m_node;
        }

        public NodeLifecycle getStage()
        {
            return m_stage;
        }

        public void incrementPosition()
        {
            m_counter.increment();
        }

        public int position()
        {
            return m_counter.position();
        }

        public void setAttribute( Attribute attribute )
        {
            m_attribute = attribute;
        }

        public void setNode( Node node )
        {
            m_node = node;
        }

        public void setStage( NodeLifecycle stage )
        {
            m_stage = stage;
        }
    }

    private final List<Integer> lineBreaks = new ArrayList<Integer>();

    private JspDocument doc = null;

    protected Stack<ParseContext> contextStack = new Stack<ParseContext>();

    private Stack<Node> nodeStack = new Stack<Node>();

    private String m_source;

    /** The current parsing context. */
    private ParseContext context;

    /**
     * Constructs a new JspDocument.
     */
    public JspParser()
    {
        super();
    }

    /**
     * Parses a JSP file, supplied as a String, into Nodes.
     * 
     * @param m_source the JSP file contents
     */
    public JspDocument parse( String source )
    {
        // Initialize the cached document, m_source, and stack variables
        this.doc = new JspDocument();
        m_source = source;
        contextStack.clear();
        nodeStack.clear();
        nodeStack.push( doc.getRoot() );

        // Create new parse context and put it on the stack
        context = new ParseContext( new Counter() );
        initText( context.position() );

        // Initialize parser delegates
        ParserDelegate textParser = new TextParser();

        // Parse the file, character by character
        for( char currentChar : source.toCharArray() )
        {
            // Is the current character whitespace?
            boolean isWhitespace = Character.isWhitespace( currentChar );
            char ch = isWhitespace ? ' ' : currentChar; // For case statements
            int pos = context.position();

            switch( context.getStage() )
            {
                // Part of a text node.
                case TEXT_NODE: {
                    textParser.handle( ch, context );
                    switch( ch )
                    {
                        // If we see a quote, check to see if it's a part of a
                        // parent attribute
                        case ('\''):
                        case ('"'): {
                            if( contextStack.size() > 0 )
                            {
                                Attribute parentAttribute = contextStack.peek().getAttribute();
                                if( parentAttribute != null && ch == parentAttribute.getAttributeDelimiter() )
                                {
                                    // Finish the current text node and attach
                                    // it to the parent attribute
                                    finalizeText();

                                    // Restore the parent ParseContext and Node
                                    context = contextStack.pop();
                                    nodeStack.pop();

                                    // Finish the parent attribute
                                    finalizeAttribute();
                                }
                            }
                            break;
                        }
                        case ('<'): {
                            // Finalize current node and start a new
                            // (unresolved) one
                            finalizeText();
                            initTag( NodeType.UNRESOLVED, context.position() );
                            context.setStage( NodeLifecycle.TAG_RESOLUTION );
                            break;
                        }
                    }
                    break;
                }

                case TAG_RESOLUTION: {
                    Node node = context.getNode();
                    switch( node.getType() )
                    {

                        case UNRESOLVED: {
                            switch( ch )
                            {
                                // If <%, it's a JSP element
                                case ('%'): {
                                    // Re-initialize the node as a JSPMarkup
                                    // node
                                    initJspMarkup();
                                    break;
                                }

                                    // If </, it's an HTML end tag
                                case ('/'): {
                                    // Re-initialize the node as an end tag
                                    initTag( NodeType.HTML_END_TAG, node.getStart() );
                                    context.setStage( NodeLifecycle.TAG_NAME );
                                    break;
                                }

                                    // If < plus space, it's just ordinary
                                    // (albeit sloppy) markup
                                case (' '): {
                                    // Re-initialize the node as a text node
                                    initText( node.getStart() );
                                    context.setStage( NodeLifecycle.TEXT_NODE );
                                    break;
                                }

                                    // Any other char means its HTML start tag
                                    // or combined tag
                                default: {
                                    node.setType( NodeType.UNRESOLVED_HTML_TAG );
                                    context.setStage( NodeLifecycle.TAG_NAME );
                                }
                            }
                            break;
                        }

                            // If JSP element, next character narrows it down
                        case UNRESOLVED_JSP_TAG: {
                            switch( ch )
                            {
                                // Dash after <% means hidden JSP
                                // comment
                                case ('-'): {
                                    node.setType( NodeType.JSP_COMMENT );
                                    context.setStage( NodeLifecycle.CODE_OR_COMMENT );
                                    break;
                                }

                                    // Bang after <% means JSP
                                    // declaration
                                case ('!'): {
                                    node.setType( NodeType.JSP_DECLARATION );
                                    context.setStage( NodeLifecycle.CODE_OR_COMMENT );
                                    break;
                                }

                                    // Equals after <% means JSP
                                    // expression
                                case ('='): {
                                    node.setType( NodeType.JSP_EXPRESSION );
                                    context.setStage( NodeLifecycle.CODE_OR_COMMENT );
                                    break;
                                }

                                    // At-sign after <% means JSP
                                    // directive
                                case ('@'): {
                                    // Re-initialize the node as a JspDirective
                                    initJspDirective();
                                    context.setStage( NodeLifecycle.BETWEEN_ATTRIBUTES );
                                    break;
                                }

                                    // Whitespace after <% means
                                    // scriptlet
                                case (' '): {
                                    node.setType( NodeType.SCRIPTLET );
                                    context.setStage( NodeLifecycle.CODE_OR_COMMENT );
                                    break;
                                }
                            }
                            break;
                        }
                    }
                    break;
                }

                case CODE_OR_COMMENT: {
                    switch( ch )
                    {
                        // Terminating %> means the end of the scriptlet
                        case ('>'): {
                            if( source.charAt( pos - 1 ) == '%' )
                            {
                                finalizeJspMarkup();
                                initText( pos + 1 );
                            }
                            break;
                        }
                    }
                    break;
                }

                    // Characters that supply the JSP directive name.
                case JSP_DIRECTIVE_NAME: {
                    if( isWhitespace )
                    {
                        Node node = context.getNode();
                        Attribute directive = context.getAttribute();
                        node.setName( m_source.substring( directive.getStart(), context.position() ) );
                        context.setAttribute( null );
                        context.setStage( NodeLifecycle.BETWEEN_ATTRIBUTES );
                    }
                    break;
                }

                    // After < but before whitespace that delimit attributes.
                case TAG_NAME: {
                    switch( ch )
                    {
                        // If current character is whitespace, set the name and
                        // move
                        // to attributes stage
                        case (' '): {
                            finalizeTagName();
                            context.setStage( NodeLifecycle.BETWEEN_ATTRIBUTES );
                            break;
                        }

                            // Right angle bracket == end of the node
                        case ('>'): {
                            finalizeTagName();
                            finalizeNode( pos + 1 );
                            initText( pos + 1 );
                            break;
                        }
                    }
                    break;
                }

                    // Whitespace between node name and first attribute, or
                    // between attributes.
                case BETWEEN_ATTRIBUTES: {
                    switch( ch )
                    {
                        case (' '): {
                            break;
                        }
                        case ('/'): {
                            // Ignore the / because we might be in HTML
                            // combined tag
                            break;
                        }
                        case ('%'): {
                            // Ignore the % because we might be in a JSP
                            // directive/element
                            break;
                        }
                        case ('>'): {
                            finalizeNode( pos + 1 );
                            initText( pos + 1 );
                            break;
                        }
                        default:
                            initAttribute();
                    }
                    break;
                }

                    // Characters that name an attribute, after whitespace but
                    // before equals (=).
                case ATTRIBUTE_NAME: {
                    if( ch == '=' )
                    {
                        Attribute attribute = context.getAttribute();
                        attribute.setName( m_source.substring( attribute.getStart(), pos ) );
                        context.setStage( NodeLifecycle.ATTRIBUTE_EQUALS );
                    }
                    break;
                }

                    // The equals (=) that separates the attribute name and
                    // value.
                case ATTRIBUTE_EQUALS: {
                    if( ch == '\'' || ch == '\"' )
                    {
                        // Save the quote delimiter for later
                        Attribute attribute = context.getAttribute();
                        attribute.setAttributeDelimiter( ch );

                        // Push current ParseContext and Node onto stack
                        contextStack.push( context );
                        nodeStack.push( attribute );

                        // Create new context, with text node as child of
                        // attribute
                        context = new ParseContext( context.getCounter() );
                        initText( context.position() + 1 );
                        context.getNode().setParent( attribute );
                    }
                    break;
                }

            }

            // Reset the line/column counters if we encounter linebreaks
            if( currentChar == '\r' || currentChar == '\n' )
            {
                lineBreaks.add( pos );
            }

            // Increment the character position
            context.incrementPosition();
        }

        // Finalize the last node and return the parsed JSP
        finalizeNode( context.position() );
        return doc;
    }

    private void finalizeJspMarkup()
    {
        Node node = context.getNode();
        NodeType type = node.getType();

        // Set the end position
        node.setEnd( context.position() + 1 );

        node.setValue( m_source
            .substring( node.getStart() + type.getTagStart().length(), node.getEnd() - type.getTagEnd().length() ) );

        // If node length is > 0, add it to the parent
        if( node.getEnd() > node.getStart() )
        {
            node.getParent().addChild( node );
        }
    }

    private void finalizeAttribute()
    {
        Attribute attribute = context.getAttribute();
        Node node = attribute.getParent();
        attribute.setEnd( context.position() + 1 );
        if( node.isHtmlNode() )
        {
            ((Tag) node).addAttribute( attribute );
        }
        else if( node.getType() == NodeType.JSP_DIRECTIVE )
        {
            ((JspDirective) node).addAttribute( attribute );
        }
        context.setAttribute( null );
        context.setStage( NodeLifecycle.BETWEEN_ATTRIBUTES );
    }

    private void finalizeText()
    {
        Node node = context.getNode();

        // Set the end position
        node.setEnd( context.position() );

        node.setValue( m_source.substring( node.getStart(), node.getEnd() ) );

        // If node length is > 0, add it to the parent
        if( node.getEnd() > node.getStart() )
        {
            node.getParent().addChild( node );
        }
    }

    /**
     * Finalizes the current Tag (returned by {@link ParseContext#getNode()}),
     * adds it as a child to the parent node, and initializes a new {@link Text}
     * node that will begin at a specified character position. The parent node
     * is determined by taking the Node currently at the top of the internal
     * node stack.
     * 
     * @param pos the desired start position for the new text node
     */
    private void finalizeNode( int pos )
    {
        Node node = context.getNode();
        NodeType type = node.getType();

        // Set the end position
        node.setEnd( pos );

        // Finalize the node type if it is still undefined
        if( type == NodeType.UNRESOLVED_HTML_TAG )
        {
            char lastCh = m_source.charAt( pos - 2 );
            if( lastCh == '/' )
            {
                node.setType( NodeType.HTML_COMBINED_TAG );
            }
            else
            {
                // If no /, it's an HTML start tag, and new nodes should be
                // children of it
                node.setType( NodeType.HTML_START_TAG );
                nodeStack.push( node );
            }
        }
        else if( type == NodeType.TEXT )
        {
            node.setValue( m_source.substring( node.getStart(), node.getEnd() ) );
        }
        else if( node.isJspNode() )
        {
            node.setValue( m_source.substring( node.getStart() + type.getTagStart().length(), node.getEnd()
                                                                                              - type.getTagEnd().length() ) );
        }

        // If node length is > 0, add it to the parent
        if( node.getEnd() > node.getStart() )
        {
            node.getParent().addChild( node );
        }
    }

    private void finalizeTagName()
    {
        Node node = context.getNode();
        int nameStart = node.getStart() + node.getType().getTagStart().length();
        int pos = context.position();
        if( pos - nameStart > 0 )
        {
            node.setName( m_source.substring( nameStart, pos ) );
        }
    }

    private void initAttribute()
    {
        Attribute attribute = new Attribute( doc );
        attribute.setParent( context.getNode() );
        context.setAttribute( attribute );

        // Set the start, end, linebreak
        updatePosition( attribute, context.position() );

        // Set the correct lifecycle stage
        Node node = context.getNode();
        if( node.getType() == NodeType.JSP_DIRECTIVE && node.getName() == null )
        {
            context.setStage( NodeLifecycle.JSP_DIRECTIVE_NAME );
        }
        else
        {
            context.setStage( NodeLifecycle.ATTRIBUTE_NAME );
        }
    }

    /**
     * Sets the start, end and line/column positions for a supplied node, based
     * on the position in the ParseContext.
     * 
     * @param node the node to set
     */
    private void updatePosition( Node node, int pos )
    {
        // Set the start, end, linebreak
        node.setStart( pos );
        int lastLineBreakPos = lineBreaks.size() == 0 ? Node.POSITION_NOT_SET : lineBreaks.get( lineBreaks.size() - 1 );
        node.setLine( lineBreaks.size() + 1 );
        node.setColumn( pos - lastLineBreakPos );
    }

    /**
     * Factory method that returns a new JspDirective node.
     */
    private void initJspDirective()
    {
        // Create new JspDirective
        JspDirective node = new JspDirective( doc );

        // Set parent relationship
        node.setParent( nodeStack.peek() );
        context.setNode( node );
        context.setAttribute( null );

        // Set the start, end, linebreak
        updatePosition( node, context.position() - 2 );
    }

    /**
     * Factory method that returns a new JspMarkup node.
     */
    private void initJspMarkup()
    {
        // Create new JspMarkup
        JspMarkup node = new JspMarkup( doc, NodeType.UNRESOLVED_JSP_TAG );

        // Set parent relationship
        node.setParent( nodeStack.peek() );
        context.setNode( node );
        context.setAttribute( null );

        // Set the start, end, linebreak
        updatePosition( node, context.position() - 1 );
    }

    /**
     * Factory method that constructs and returns a new Tag. When constructed,
     * the node's start position, line number, column number and level are set
     * automatically based on JspDocument's internal cache of line-breaks and
     * nodes. Note that the new node is not actually added to the internal node
     * tree until the method {@link #finalizeNode(Node, int)} is called.
     * 
     * @param type the node type
     * @param pos the start position for the tag
     */
    private void initTag( NodeType type, int pos )
    {
        // Create new Tag
        Tag tag = new Tag( doc, type );

        // If HTML end tag, pop the node stack first
        if( tag.getType() == NodeType.HTML_END_TAG )
        {
            nodeStack.pop();
        }

        // Set parent relationship
        tag.setParent( nodeStack.peek() );
        context.setNode( tag );
        context.setAttribute( null );

        // Set the start, end, linebreak
        updatePosition( tag, pos );
    }

    private void initText( int pos )
    {
        // Create new Text
        Text text = new Text( doc );

        // Set parent relationship
        text.setParent( nodeStack.peek() );
        context.setNode( text );
        context.setAttribute( null );

        // Set the start, end, linebreak
        updatePosition( text, pos );
        context.setStage( NodeLifecycle.TEXT_NODE );
    }

    public static abstract class ParserDelegate
    {
        public abstract void handle( char ch, ParseContext context );
    }

    public static class TextParser extends ParserDelegate
    {
        public void handle( char ch, ParseContext context )
        {
        }

    }

}
