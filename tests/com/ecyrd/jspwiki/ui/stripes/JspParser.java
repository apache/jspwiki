package com.ecyrd.jspwiki.ui.stripes;

import java.util.*;

/**
 * Parser that reads JSP document and constructs a {@link JspDocument} with the
 * results.
 */
public class JspParser
{
    private static final ParserDelegate TEXT_PARSER = new TextParser();

    private static final JspTagParser JSP_TAG_PARSER = new JspTagParser();

    private static final ParserDelegate HTML_TAG_PARSER = new HtmlTagParser();

    private static final AttributeParser ATTRIBUTE_PARSER = new AttributeParser();

    public static class AttributeParser extends ParserDelegate
    {
        public void beginStage()
        {
        }

        public void endStage()
        {
        }

        /**
         * Handles {@link NodeLifecycle#TAG_WHITESPACE},
         * {@link NodeLifecycle#ATTRIBUTE_EQUALS},
         * {@link NodeLifecycle#ATTRIBUTE_VALUE}.
         */
        public void handle( char ch )
        {
            ParseContext ctx = ParseContext.currentContext();
            int pos = ctx.position();

            switch( ctx.getStage() )
            {
                // Whitespace between node name and first attribute, or
                // between attributes.
                case TAG_WHITESPACE: {
                    switch( ch )
                    {
                        case (' '): {
                            break;
                        }
                        case ('/'): {
                            // If we see /, switch back to HTML tag parser
                            ctx.setParser( HTML_TAG_PARSER, NodeLifecycle.TAG_WHITESPACE );
                            break;
                        }
                        case ('%'): {
                            // Ignore the % because we might be in a JSP
                            // directive/element
                            break;
                        }
                        case ('>'): {
                            // Switch back to HTML tag parser so its endStage()
                            // method is called
                            ctx.setParser( HTML_TAG_PARSER );
                            ctx.setParser( TEXT_PARSER, NodeLifecycle.PARSING_TEXT );
                            break;
                        }
                        default: {
                            // Create new attribute
                            Attribute attribute = new Attribute( ctx.getDocument() );
                            attribute.setParent( ctx.getNode() );
                            ctx.setAttribute( attribute );
                            ctx.setStage( NodeLifecycle.ATTRIBUTE_NAME );
                        }
                    }
                    break;
                }

                    // Characters that name an attribute, after whitespace but
                    // before equals (=).
                case ATTRIBUTE_NAME: {
                    if( ch == '=' )
                    {
                        // Set the attribute name
                        Attribute attribute = ctx.getAttribute();
                        attribute.setName( ctx.getSource().substring( attribute.getStart(), pos ) );
                        ctx.setStage( NodeLifecycle.ATTRIBUTE_EQUALS );
                    }
                    break;
                }

                    // The equals (=) that separates the attribute name and
                    // value.
                case ATTRIBUTE_EQUALS: {
                    if( ch == '\'' || ch == '\"' )
                    {
                        // Save the quote delimiter for later
                        Attribute attribute = ctx.getAttribute();
                        attribute.setAttributeDelimiter( ch );

                        // Push current ParseContext and Node onto stack
                        ctx = ctx.push();
                        ctx.pushNode( attribute );

                        // Assign new text node to ParseContext, set as child of
                        // attribute
                        Text text = new Text( ctx.getDocument() );
                        text.setParent( ctx.getParentNode() );
                        ctx.setNode( text );
                        ctx.setStartPosition( text, ctx.position() );
                        ctx.setParser( TEXT_PARSER, NodeLifecycle.PARSING_TEXT );
                    }
                    break;
                }
            }
        }

    }

    public static class HtmlTagParser extends ParserDelegate
    {
        public void beginStage()
        {
        }

        public void endStage()
        {
            ParseContext ctx = ParseContext.currentContext();
            Node node = ctx.getNode();

            // Finalize the node type if it is still undefined
            if( node.getType() == NodeType.UNRESOLVED_HTML_TAG )
            {
                char lastCh = ctx.getSource().charAt( ctx.position() - 1 );
                if( lastCh == '/' )
                {
                    node.setType( NodeType.HTML_COMBINED_TAG );
                }
                else
                {
                    // If no /, it's an HTML start tag, and new nodes should be
                    // children of it
                    node.setType( NodeType.HTML_START_TAG );
                    ctx.pushNode( node );
                }
            }

            // Set the end position
            ctx.setEndPosition( node );

            // If node length is > 0, add it to the parent
            if( node.getEnd() > node.getStart() )
            {
                node.getParent().addChild( node );
            }
        }

        /**
         * Handles {@link NodeLifecycle#TAG_NAME}.
         */
        public void handle( char ch )
        {
            ParseContext ctx = ParseContext.currentContext();

            switch( ctx.getStage() )
            {
                // After < but before whitespace that delimit attributes.
                case TAG_NAME: {
                    switch( ch )
                    {
                        // If current character is whitespace, set the name and
                        // move
                        // to attributes stage
                        case (' '): {
                            finalizeTagName();
                            ctx.setParser( ATTRIBUTE_PARSER );
                            ctx.setStage( NodeLifecycle.TAG_WHITESPACE );
                            break;
                        }

                            // Right angle bracket == end of the node
                        case ('>'): {
                            finalizeTagName();
                            ctx.setParser( TEXT_PARSER, NodeLifecycle.PARSING_TEXT );
                            break;
                        }
                    }
                    break;
                }

                    // Whitespace immediately before >
                case TAG_WHITESPACE: {
                    switch( ch )
                    {
                        case ('>'): {
                            ctx.setParser( TEXT_PARSER, NodeLifecycle.PARSING_TEXT );
                            break;
                        }
                    }
                    break;
                }

                case CODE_OR_COMMENT: {
                    switch( ch )
                    {
                        // Terminating %> means the end of the comment
                        case ('>'): {
                            Node node = ctx.getNode();
                            String lookbehind = ctx.lookbehind( 3 );
                            if( lookbehind.equals( NodeType.HTML_COMMENT.getTagEnd() ) )
                            {
                                // Set the end position
                                node.setEnd( ctx.position() + 1 );

                                // Set the value
                                NodeType type = node.getType();
                                node.setValue( ctx.getSource().substring( node.getStart() + type.getTagStart().length(),
                                                                          node.getEnd() - type.getTagEnd().length() ) );

                                ctx.setParser( TEXT_PARSER, NodeLifecycle.PARSING_TEXT );
                            }
                            break;
                        }
                    }
                    break;
                }
            }
        }

        private void finalizeTagName()
        {
            ParseContext ctx = ParseContext.currentContext();
            Node node = ctx.getNode();
            int nameStart = node.getStart() + node.getType().getTagStart().length();
            int nameEnd = ctx.position();
            if( nameEnd - nameStart > 0 )
            {
                if( ctx.getSource().charAt( nameEnd - 1 ) == '/' )
                {
                    nameEnd--;
                }
                node.setName( ctx.getSource().substring( nameStart, nameEnd ) );
            }
        }
    }

    public static class JspTagParser extends ParserDelegate
    {
        public void beginStage()
        {
        }

        public void endStage()
        {
        }

        /**
         * Handles {@link NodeLifecycle#TAG_WHITESPACE},
         * {@link NodeLifecycle#TAG_NAME},
         * {@link NodeLifecycle#CODE_OR_COMMENT}.
         */
        public void handle( char ch )
        {
            ParseContext ctx = ParseContext.currentContext();
            Node node = ctx.getNode();

            switch( ctx.getStage() )
            {
                // Whitespace between node name and first attribute, or
                // between attributes.
                case TAG_WHITESPACE: {
                    switch( ch )
                    {
                        case (' '): {
                            break;
                        }
                        default: {
                            // If directive name not set, start new ParseContext (and set marker)
                            if ( node.getName() == null )
                            {
                                ctx = ctx.push();
                                ctx.setParser( JSP_TAG_PARSER );
                                ctx.setStage( NodeLifecycle.TAG_NAME );     // Sets the marker too
                            }
                            
                            // Otherwise, it's the start of a new attribute
                            else
                            {
                            }
                        }
                    }
                    break;
                }
                    // Characters that supply the JSP directive name.
                case TAG_NAME: {
                    if( ch == ' ' )
                    {
                        int nameStart = ctx.getMarker( NodeLifecycle.TAG_NAME );    // Retrieve the marker
                        ctx = ctx.pop();
                        Node directive = ctx.getNode();
                        directive.setName( ctx.getSource().substring( nameStart, ctx.position() ) );
                        ctx.setParser( ATTRIBUTE_PARSER );
                        ctx.setStage( NodeLifecycle.TAG_WHITESPACE );
                    }
                    break;
                }

                case CODE_OR_COMMENT: {
                    switch( ch )
                    {
                        // Terminating %> means the end of the scriptlet
                        case ('>'): {
                            String lookbehind = ctx.lookbehind( 2 );
                            if( lookbehind.equals( NodeType.SCRIPTLET.getTagEnd() ) )
                            {
                                // Set the end position
                                node.setEnd( ctx.position() + 1 );

                                // Set the value
                                NodeType type = node.getType();
                                node.setValue( ctx.getSource().substring( node.getStart() + type.getTagStart().length(),
                                                                          node.getEnd() - type.getTagEnd().length() ) );

                                // If node length is > 0, add it to the parent
                                if( node.getEnd() > node.getStart() )
                                {
                                    node.getParent().addChild( node );
                                }
                                ctx.setParser( TEXT_PARSER, NodeLifecycle.PARSING_TEXT );
                            }
                            break;
                        }
                    }
                    break;
                }
            }

        }

    }

    public static abstract class ParserDelegate
    {
        public abstract void beginStage();

        public abstract void endStage();

        public abstract void handle( char ch );
    }

    public static class TextParser extends ParserDelegate
    {

        public void beginStage()
        {
            ParseContext ctx = ParseContext.currentContext();
            int pos = ctx.position() + 1;

            // Create new Text
            Text text = new Text( ctx.getDocument() );

            // Set parent relationship
            text.setParent( ctx.getParentNode() );
            ctx.setNode( text );
            ctx.setAttribute( null );

            // Set the start, end, linebreak
            ctx.setStartPosition( text, pos );
        }

        public void endStage()
        {
            // Finalize current node
            ParseContext ctx = ParseContext.currentContext();
            if( ctx.position() > 0 )
            {
                Node node = ctx.getNode();

                // Set the end position
                node.setEnd( ctx.position() );

                // Set the node value
                node.setValue( ctx.getSource().substring( node.getStart(), node.getEnd() ) );

                // If node length is > 0, add it to the parent
                if( node.getEnd() > node.getStart() )
                {
                    node.getParent().addChild( node );
                }
            }
        }

        /**
         * Handles {@link NodeLifecycle#PARSING_TEXT}.
         */
        public void handle( char ch )
        {
            ParseContext ctx = ParseContext.currentContext();
            switch( ctx.getStage() )
            {
                case PARSING_TEXT: {
                    switch( ch )
                    {
                        // If we see a quote, check to see if it's a part of a
                        // parent attribute
                        case ('\''):
                        case ('"'): {
                            if( ctx.hasParentContext() )
                            {
                                Attribute attribute = ctx.getParentContext().getAttribute();
                                if( attribute != null && ch == attribute.getAttributeDelimiter() )
                                {
                                    // Pop the ParseContext (and run its
                                    // endStage method) and Node
                                    ctx = ctx.pop();
                                    ctx.popNode();

                                    // Finish the parent attribute
                                    Node node = attribute.getParent();
                                    attribute.setEnd( ctx.position() + 1 );
                                    if( node.isHtmlNode() )
                                    {
                                        ((Tag) node).addAttribute( attribute );
                                    }
                                    else if( node.getType() == NodeType.JSP_DIRECTIVE )
                                    {
                                        ((JspDirective) node).addAttribute( attribute );
                                    }
                                    ctx.setAttribute( null );
                                    ctx.setParser( ATTRIBUTE_PARSER, NodeLifecycle.TAG_WHITESPACE );
                                }
                            }
                            break;
                        }
                        case ('<'): {

                            // Figure out what this tag is
                            String lookahead = ctx.lookahead( 4 );
                            JspDocument doc = ctx.getDocument();

                            // <%- means hidden JSP comment
                            if( lookahead.startsWith( NodeType.JSP_COMMENT.getTagStart() ) )
                            {
                                ctx.setParser( JSP_TAG_PARSER, NodeLifecycle.CODE_OR_COMMENT );
                                initNode( new Markup( doc, NodeType.JSP_COMMENT ) );
                            }

                            // <%! means JSP declaration
                            else if( lookahead.startsWith( NodeType.JSP_DECLARATION.getTagStart() ) )
                            {
                                ctx.setParser( JSP_TAG_PARSER, NodeLifecycle.CODE_OR_COMMENT );
                                initNode( new Markup( doc, NodeType.JSP_DECLARATION ) );
                            }

                            // <%= means JSP expression
                            else if( lookahead.startsWith( NodeType.JSP_EXPRESSION.getTagStart() ) )
                            {
                                ctx.setParser( JSP_TAG_PARSER, NodeLifecycle.CODE_OR_COMMENT );
                                initNode( new Markup( doc, NodeType.JSP_EXPRESSION ) );
                            }

                            // <%@ means JSP directive
                            else if( lookahead.startsWith( NodeType.JSP_DIRECTIVE.getTagStart() ) )
                            {
                                ctx.setParser( JSP_TAG_PARSER, NodeLifecycle.TAG_WHITESPACE );
                                initNode( new JspDirective( doc ) );
                            }

                            // <!-- means HTML comment
                            else if( lookahead.startsWith( NodeType.HTML_COMMENT.getTagStart() ) )
                            {
                                ctx.setParser( HTML_TAG_PARSER, NodeLifecycle.CODE_OR_COMMENT );
                                initNode( new Markup( doc, NodeType.HTML_COMMENT ) );
                            }

                            // Whitespace after <% means
                            // scriptlet
                            else if( lookahead.startsWith( NodeType.SCRIPTLET.getTagStart() ) )
                            {
                                if( lookahead.length() >= 3 && Character.isWhitespace( lookahead.charAt( 2 ) ) )
                                {
                                    ctx.setParser( JSP_TAG_PARSER, NodeLifecycle.CODE_OR_COMMENT );
                                    initNode( new Markup( doc, NodeType.SCRIPTLET ) );
                                }
                            }

                            // If </, it's an HTML end tag
                            else if( lookahead.startsWith( NodeType.HTML_END_TAG.getTagStart() ) )
                            {
                                ctx.setParser( HTML_TAG_PARSER, NodeLifecycle.TAG_NAME );
                                initNode( new Tag( doc, NodeType.HTML_END_TAG ) );
                            }

                            // If < plus space, it's just ordinary
                            // (albeit sloppy) markup
                            else if( "< ".equals( lookahead.subSequence( 0, 2 ) ) )
                            {
                                ctx.setStage( NodeLifecycle.PARSING_TEXT );
                                initNode( new Text( doc ) );
                            }

                            // Any other char means its HTML start tag
                            // or combined tag
                            else
                            {
                                ctx.setParser( HTML_TAG_PARSER, NodeLifecycle.TAG_NAME );
                                initNode( new Tag( doc, NodeType.UNRESOLVED_HTML_TAG ) );
                            }

                            break;
                        }
                    }
                    break;
                }
            }

        }

        /**
         * Factory method that initializes a supplied Node. When initialized,
         * the node's start position, line number, column number and level are
         * set automatically based on JspDocument's internal cache of
         * line-breaks and nodes. Note that the new node is not actually added
         * to the internal node tree until the method
         * {@link #finalizeNode(Node, int)} is called.
         * 
         * @param type the node type
         */
        private void initNode( Node node )
        {
            ParseContext ctx = ParseContext.currentContext();

            // If HTML end tag, pop the node stack first
            if( node.getType() == NodeType.HTML_END_TAG )
            {
                ctx.popNode();
            }

            // Set parent relationship
            node.setParent( ctx.getParentNode() );
            ctx.setNode( node );
            ctx.setAttribute( null );

            // Set the start, end, linebreak
            ctx.setStartPosition( node, ctx.position() );

            // Skip ahead if tag start > 1 char long
            int increment = node.getType().getTagStart().length() - 1;
            for( int i = 0; i < increment; i++ )
            {
                ctx.incrementPosition();
            }
        }

    }

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
        /** Parsing any text outside of a tag or element. */
        PARSING_TEXT,
        /**
         * Characters after the opening left bracket (&lt;), but before
         * whitespace that delimit the attributes.
         */
        TAG_NAME,
        /**
         * Whitespace between the node or directive name and the first
         * attribute, and between attributes.
         */
        TAG_WHITESPACE,
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
        /** Any text inside of a scriptlet, JSP comment, or JSP declaration. */
        CODE_OR_COMMENT,
    }

    /**
     * Encapsulates the current state of document parsing.
     */
    private static class ParseContext
    {
        private static final Stack<Node> NODE_STACK = new Stack<Node>();

        private static final Stack<ParseContext> CONTEXT_STACK = new Stack<ParseContext>();

        private static ParseContext CURRENT_CONTEXT;

        public static ParseContext currentContext()
        {
            return CURRENT_CONTEXT;
        }

        public static ParseContext initialContext( JspDocument doc, String source )
        {
            NODE_STACK.clear();
            CONTEXT_STACK.clear();
            ParseContext context = new ParseContext( doc, source, new Counter() );
            context.pushNode( doc.getRoot() );
            CURRENT_CONTEXT = context;

            // Set the default stage to TEXT
            Text text = new Text( doc );
            text.setParent( doc.getRoot() );
            context.setNode( text );
            context.setAttribute( null );
            context.setStartPosition( text, 0 );
            context.m_stage = NodeLifecycle.PARSING_TEXT;
            context.m_parser = TEXT_PARSER;

            // Return the init'ed context
            return context;
        }

        private ParserDelegate m_parser = null;

        private final List<Integer> lineBreaks = new ArrayList<Integer>();

        private Node m_node = null;

        private Attribute m_attribute = null;

        private final Counter m_counter;

        private final String m_source;

        private NodeLifecycle m_stage = NodeLifecycle.PARSING_TEXT;

        private JspDocument m_doc;

        private Map<NodeLifecycle, Integer> m_markers = new HashMap<NodeLifecycle, Integer>();

        private ParseContext( JspDocument doc, String source, Counter counter )
        {
            super();
            m_doc = doc;
            m_source = source;
            m_counter = counter;
        }

        /**
         * Returns an arbitrary number of characters ahead of the current
         * position, starting with the current character. For example, if the
         * current position returned by {@link #position()} is 70,
         * <code>lookahead(3)</code> returns the characters at positions 70,
         * 71 and 72. If the supplied length causes the lookahead position to
         * exceed the length of the source string, the substring is truncated
         * appropriately.
         * 
         * @param length the number of characters to return
         * @return the substring
         */
        public String lookahead( int length )
        {
            int startPos = position();
            int endPos = startPos + length > m_source.length() ? endPos = m_source.length() : startPos + length;
            return m_source.substring( startPos, endPos );
        }

        /**
         * Returns an arbitrary number of characters behind the current
         * position, starting with the current character. For example, if the
         * current position returned by {@link #position()} is 70,
         * <code>lookbehind(3)</code> returns the characters at positions 68,
         * 69 and 70. If the supplied length causes the lookbehind position to
         * exceed the length of the source string, the substring is truncated
         * appropriately.
         * 
         * @param length the number of characters to return
         * @return the substring
         */
        public String lookbehind( int length )
        {
            int endPos = position() + 1;
            int startPos = endPos - length < 0 ? 0 : endPos - length;
            return m_source.substring( startPos, endPos );
        }

        public Node getParentNode()
        {
            return NODE_STACK.peek();
        }

        public Attribute getAttribute()
        {
            return m_attribute;
        }

        public Counter getCounter()
        {
            return m_counter;
        }

        public JspDocument getDocument()
        {
            return m_doc;
        }

        /**
         * Retrieves the position of the marker set for the current stage (as
         * set by {@link #mark()}). If no marker was set, this method returns
         * {@link Node#POSITION_NOT_SET}.
         * 
         * @param stage the stage for which the marker position is desired
         * @return the position of the marker.
         */
        public int getMarker( NodeLifecycle stage )
        {
            Integer mark = m_markers.get( stage );
            return mark == null ? Node.POSITION_NOT_SET : mark.intValue();
        }

        public Node getNode()
        {
            return m_node;
        }

        /**
         * Sets the active Parser without changing the stage or incrementing the
         * position. Calling this method does <em>not</em> execute either the
         * {@link ParserDelegate#beginStage()} or
         * {@link ParserDelegate#endStage()} methods.
         * 
         * @param parser the parser to set
         */
        public void setParser( ParserDelegate parser )
        {
            m_parser = parser;
        }

        public ParseContext getParentContext()
        {
            return CONTEXT_STACK.peek();
        }

        public ParserDelegate getParser()
        {
            return m_parser;
        }

        public String getSource()
        {
            return m_source;
        }

        public NodeLifecycle getStage()
        {
            return m_stage;
        }

        /**
         * Returns <code>true</code> if one or more ParseContexts have been
         * pushed onto the stack (via {@link #push()}); <code>false</code>
         * otherwise.
         * 
         * @return the result
         */
        public boolean hasParentContext()
        {
            return CONTEXT_STACK.size() > 0;
        }

        public void incrementPosition()
        {
            // Reset the line/column counters if we encounter linebreaks
            char currentChar = m_source.charAt( position() );
            if( currentChar == '\r' || currentChar == '\n' )
            {
                lineBreaks.add( position() );
            }

            m_counter.increment();
        }

        /**
         * Sets a "marker" for the current stage (as reported by
         * {@link #getStage()} at the current position (as reported by
         * {@link #position()}. The marker can be retrieved later via
         * {@link #getMarker(com.ecyrd.jspwiki.ui.stripes.JspParser.NodeLifecycle)}.
         * Callers may place only one marker per lifecycle stage. Generally,
         * markers are used to set character positions that are important to
         * retrieve later.
         */
        public void mark()
        {
            m_markers.put( getStage(), position() );
        }

        /**
         * Pops the topmost ParseContext from the stack and replaces the
         * JspParser's current ParseContext with it. Before popping the
         * ParseContext, the current stage's {@link ParserDelegate#endStage()}
         * method is executed.
         */
        public ParseContext pop()
        {
            // Run the endStage method for the current stage
            ParseContext ctx = ParseContext.currentContext();
            ctx.getParser().endStage();

            ctx = CONTEXT_STACK.pop();
            CURRENT_CONTEXT = ctx;
            return ctx;
        }

        public void popNode()
        {
            NODE_STACK.pop();
        }

        public int position()
        {
            return m_counter.position();
        }

        /**
         * Pushes this ParseContext onto the stack and replaces the JspParser's
         * current ParseContext with a new one. This method does <em>not</em>
         * call the current ParserDelegate's {@link ParserDelegate#endStage()}
         * method.
         * 
         * @return the new ParseContext
         */
        public ParseContext push()
        {
            CONTEXT_STACK.push( this );
            ParseContext context = new ParseContext( m_doc, m_source, m_counter );
            CURRENT_CONTEXT = context;

            // Set the default stage to TEXT
            context.setParser( TEXT_PARSER, NodeLifecycle.PARSING_TEXT );

            return context;
        }

        public void pushNode( Node node )
        {
            NODE_STACK.push( node );
        }

        /**
         * Sets the ParseContext's current attribute, and sets it start position
         * if not null.
         * 
         * @param attribute
         */
        public void setAttribute( Attribute attribute )
        {
            m_attribute = attribute;
            if( attribute != null )
            {
                setStartPosition( attribute, position() );
            }
        }

        public void setNode( Node node )
        {
            m_node = node;
        }

        /**
         * Sets the current lifecycle stage, without resetting the current
         * parser.
         * 
         * @param stage
         */
        public void setStage( NodeLifecycle stage )
        {
            // Set the new stage and set a marker at the current position
            m_stage = stage;
            mark();
        }

        /**
         * <p>
         * Ends the current {@link NodeLifecycle} and starts another, and sets a
         * marker for the current position. When this method is called, the
         * active {@link ParserDelegate} is finalized for the previous stage by
         * calling its {@link ParserDelegate#endStage()} method. Then, the
         * current text parser is replaced with the one that corresponds to the
         * correct one for the new stage, and a marker is set. Finally, the new
         * stage is initialized by calling the new parser's
         * {@link ParserDelegate #beginStage()} method.
         * </p>
         * 
         * @param stage
         */
        public void setParser( ParserDelegate parser, NodeLifecycle stage )
        {
            // Finish the parser's current stage
            if( parser != null && m_parser != null )
            {
                m_parser.endStage();
            }

            // Set the new stage and set a marker at the current position
            m_stage = stage;
            mark();

            // Replace the parser and start it up
            if( parser != null )
            {
                m_parser = parser;
                m_parser.beginStage();
            }
        }

        /**
         * Sets the start line/column positions for a supplied node, based on
         * the position in the ParseContext.
         * 
         * @param node the node to set
         * @param the position to set
         */
        private void setStartPosition( Node node, int pos )
        {
            // Set the start, end, linebreak
            node.setStart( pos );
            int lastLineBreakPos = lineBreaks.size() == 0 ? Node.POSITION_NOT_SET : lineBreaks.get( lineBreaks.size() - 1 );
            node.setLine( lineBreaks.size() + 1 );
            node.setColumn( pos - lastLineBreakPos );
        }

        /**
         * Sets the end position for a supplied node, based on the current
         * position in the ParseContext plus one.
         * 
         * @param node to set
         */
        private void setEndPosition( Node node )
        {
            ParseContext ctx = ParseContext.currentContext();
            int pos = ctx.position() + 1;
            if( pos > ctx.getSource().length() )
            {
                pos = ctx.getSource().length();
            }
            node.setEnd( pos );
        }
    }

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
        JspDocument doc = new JspDocument();

        // Create new parse context and put it on the stack
        ParseContext ctx = ParseContext.initialContext( doc, source );

        // Parse the file, character by character
        int pos = ctx.position();
        while ( pos < source.length() )
        {
            ctx = ParseContext.currentContext();
            char currentChar = source.charAt( pos );

            // Is the current character whitespace?
            boolean isWhitespace = Character.isWhitespace( currentChar );
            char ch = isWhitespace ? ' ' : currentChar; // For case statements

            // Handle the current character
            ParserDelegate parser = ctx.getParser();
            parser.handle( ch );

            // Increment the character position
            ctx.incrementPosition();
            pos = ctx.position();
        }

        // Finalize the last node and return the parsed JSP
        Node node = ctx.getNode();
        node.setEnd( ctx.position() );
        if( node.getType() == NodeType.TEXT )
        {
            node.setValue( ctx.getSource().substring( node.getStart(), node.getEnd() ) );
            if( node.getEnd() > node.getStart() )
            {
                node.getParent().addChild( node );
            }
        }

        return doc;
    }

}
