package com.ecyrd.jspwiki.ui.stripes;

import java.util.*;

/**
 * Parser that reads JSP document and constructs a {@link JspDocument} with the
 * results.
 */
public class JspParser
{
    private static final Parser TEXT_PARSER = new TextParser();

    private static final Parser TAG_PARSER = new TagParser();

    private static final AttributeParser ATTRIBUTE_PARSER = new AttributeParser();
    
    private static final DynamicAttributeParser DYNAMIC_ATTRIBUTE_PARSER = new DynamicAttributeParser();

    /**
     * Parses dynamic attributes in a Tag declaration.
     */
    public static class DynamicAttributeParser implements Parser
    {
        public void beginStage()
        {
        }

        public void endStage()
        {
        }

        public void handle( char ch )
        {
            ParseContext ctx = ParseContext.currentContext();
            int leftAngleBrackets = 1;
            boolean increment = false;
            do
            {
                // Get character at current position
                // Increment position
                if ( increment )
                {
                    ctx.incrementPosition();
                }
                ch = ctx.getSource().charAt( ctx.position() );
               
               switch (ch) {
                   
                   case ('<'):
                   {
                       leftAngleBrackets++;
                       break;
                   }
                   case ('>'):
                   {
                       leftAngleBrackets--;
                       break;
                   }
                   default:
                   {
                       // Ignore any other character
                   }
                   increment = true;
               }
                
            } while ( leftAngleBrackets != 0 );
            
            // Set the end position, name and value.
            Attribute attribute = (Attribute)ctx.getNode();
            ctx.setEndPosition( attribute );
            attribute.setType( NodeType.DYNAMIC_ATTRIBUTE );
            attribute.setValue( ctx.getSource().substring( ctx.getMarkerForStage( Stage.CODE_OR_COMMENT), attribute.getEnd() ) );
            
            // Add to parent
            Tag parent = (Tag)ctx.getParentContext().getNode();
            parent.addAttribute( attribute );
            
            // Pop the ParseContext and exit
            ParseContext.pop();
        }
    }

    public static class AttributeParser implements Parser
    {
        public void beginStage()
        {
            ParseContext ctx = ParseContext.currentContext();
            Node attribute = new Attribute( ctx.getDocument() );
            ctx.setNode( attribute );
        }

        public void endStage()
        {
            ParseContext ctx = ParseContext.currentContext();
            Node attribute = ctx.getNode();
            attribute.setEnd( ctx.position() );
        }

        /**
         * {@link Stage#NAME}.
         */
        public void handle( char ch )
        {
            ParseContext ctx = ParseContext.currentContext();
            int pos = ctx.position();

            switch( ctx.getStage() )
            {
                // Characters that name an attribute, after whitespace but
                // before equals (=).
                case NAME: {
                    if( ch == '=' )
                    {
                        // Set the attribute name
                        Attribute attribute = (Attribute) ctx.getNode();
                        attribute.setName( ctx.getSource().substring( attribute.getStart(), pos ) );

                        // The next character will be a single/double quote
                        ctx.incrementPosition();
                        char delimiter = ctx.getSource().charAt( ctx.position() );
                        attribute.setAttributeDelimiter( delimiter );

                        // Start a new ParseContext, and Text node in position
                        // after quote
                        ctx = ParseContext.push();
                        ctx.setStartPosition( ctx.getNode(), ctx.position() + 1 );
                    }
                    break;
                }

                case ATTRIBUTE_END: {
                    // Add the finished attribute to the parent
                    Attribute attribute = (Attribute) ctx.getNode();
                    Tag parent = (Tag) attribute.getParent();
                    parent.addAttribute( attribute );

                    // Retrieve the Tag ParseContext & fire the handler method
                    ctx = ParseContext.pop();
                    Parser parser = ctx.getParser();
                    parser.handle( ch );
                    break;
                }
            }
        }

    }

    public static class TagParser implements Parser
    {
        /**
         * Factory method that initializes a supplied Node. When initialized,
         * the node's start position, line number, column number and level are
         * set automatically based on JspDocument's internal cache of
         * line-breaks and nodes. Note that the new node is not actually added
         * to the internal node tree until the method
         * {@link #finalizeNode(Node, int)} is called.
         * 
         * @param type the node type
         * @param the stage to set at the end of the initialization
         */
        private void initNode( Node node, Stage stage )
        {
            ParseContext ctx = ParseContext.currentContext();
            ctx.setNode( node );

            // Skip ahead if tag start > 1 char long
            int increment = node.getType().getTagStart().length() - 1;
            for( int i = 0; i < increment; i++ )
            {
                ctx.incrementPosition();
            }

            // Set the new stage and mark it at the next character
            ctx.setStage( stage, 1 );
        }

        public void beginStage()
        {
            ParseContext ctx = ParseContext.currentContext();

            // Figure out what this tag is
            String lookahead = ctx.lookahead( 4 );
            String lookahead9 = ctx.lookahead( 9 );
            JspDocument doc = ctx.getDocument();

            // <%- means hidden JSP comment
            if( lookahead.startsWith( NodeType.JSP_COMMENT.getTagStart() ) )
            {
                initNode( new Text( doc, NodeType.JSP_COMMENT ), Stage.CODE_OR_COMMENT );
            }

            // <%! means JSP declaration
            else if( lookahead.startsWith( NodeType.JSP_DECLARATION.getTagStart() ) )
            {
                initNode( new Text( doc, NodeType.JSP_DECLARATION ), Stage.CODE_OR_COMMENT );
            }

            // <%= means JSP expression
            else if( lookahead.startsWith( NodeType.JSP_EXPRESSION.getTagStart() ) )
            {
                initNode( new Text( doc, NodeType.JSP_EXPRESSION ), Stage.CODE_OR_COMMENT );
            }

            // <%@ + space means JSP directive
            else if( lookahead.startsWith( NodeType.JSP_DIRECTIVE.getTagStart() ) )
            {
                initNode( new Tag( doc, NodeType.JSP_DIRECTIVE ), Stage.NAME );
            }

            // <!-- means HTML comment
            else if( lookahead.startsWith( NodeType.HTML_COMMENT.getTagStart() ) )
            {
                initNode( new Text( doc, NodeType.HTML_COMMENT ), Stage.CODE_OR_COMMENT );
            }

            // Whitespace after <% means
            // scriptlet
            else if( lookahead.startsWith( NodeType.SCRIPTLET.getTagStart() ) )
            {
                if( lookahead.length() >= 3 && Character.isWhitespace( lookahead.charAt( 2 ) ) )
                {
                    initNode( new Text( doc, NodeType.SCRIPTLET ), Stage.CODE_OR_COMMENT );
                }
            }
            
            // <![CDATA[ means CDATA
            else if( lookahead9.startsWith( NodeType.CDATA.getTagStart() ) )
            {
                initNode( new Text( doc, NodeType.CDATA ), Stage.CODE_OR_COMMENT );
            }

            // If </, it's an HTML end tag
            else if( lookahead.startsWith( NodeType.HTML_END_TAG.getTagStart() ) )
            {
                initNode( new Tag( doc, NodeType.HTML_END_TAG ), Stage.NAME );
            }

            // Any other char means its HTML start tag
            // or combined tag
            else
            {
                initNode( new Tag( doc, NodeType.UNRESOLVED_HTML_TAG ), Stage.NAME );
            }
        }

        public void endStage()
        {
            ParseContext ctx = ParseContext.currentContext();
            Node node = ctx.getNode();

            if( node != null )
            {
                // Set the end position
                ctx.setEndPosition( node );

                // If node length is > 0, add it to the parent
                if( node.getEnd() > node.getStart() )
                {
                    node.getParent().addChild( node );
                }
            }
        }

        /**
         * Handles {@link Stage#NAME}.
         */
        public void handle( char ch )
        {
            ParseContext ctx = ParseContext.currentContext();
            Node node = ctx.getNode();

            switch( ctx.getStage() )
            {
                // Characters that supply the tag name; immediately after <
                case NAME: {
                    switch( ch )
                    {
                        // Whitespace == end of the name
                        case (' '): {
                            int nameStart = ctx.getMarkerForStage( Stage.NAME );
                            int nameEnd = ctx.position();
                            node.setName( ctx.getSource().substring( nameStart, nameEnd ) );
                            ctx.setStage( Stage.WHITESPACE, 0 );
                            break;
                        }

                            // Right angle bracket == end of the node
                        case ('>'): {
                            handleTagEnd();
                            break;
                        }
                    }
                    break;
                }

                    // Whitespace between <%! and name, or between attributes.
                case WHITESPACE: {
                    switch( ch )
                    {
                        case ('/'):
                        case (' '):
                        case ('%'): {
                            break;
                        }
                        
                        // It's the start of a new dynamic attribute
                        case ('<'): {
                            ctx = ParseContext.push();
                            ctx.setParser( DYNAMIC_ATTRIBUTE_PARSER, Stage.CODE_OR_COMMENT, 0 );
                            ctx.setNode( new Attribute( ctx.getDocument() ) );
                            break;
                        }

                        case ('>'): {
                            handleTagEnd();
                            break;
                        }

                        default: {
                            // It's the start of a new attribute
                            ctx = ParseContext.push();
                            ctx.setParser( ATTRIBUTE_PARSER, Stage.NAME, 0 );
                        }
                    }
                    break;
                }

                case CODE_OR_COMMENT: {
                    switch( ch )
                    {
                        // Terminating %> or --> means the end of the comment
                        case ('>'): {
                            String tagEnd = node.getType().getTagEnd();
                            String lookbehind = ctx.lookbehind( tagEnd.length() );
                            if( lookbehind.equals( tagEnd ) )
                            {
                                // Set the end position
                                node.setEnd( ctx.position() + 1 );

                                // Set the value
                                NodeType type = node.getType();
                                node.setValue( ctx.getSource().substring( node.getStart() + type.getTagStart().length(),
                                                                          node.getEnd() - type.getTagEnd().length() ) );

                                ctx.setParser( TEXT_PARSER, Stage.TEXT, 1 );
                            }
                            break;
                        }
                    }
                    break;
                }
            }
        }

        /**
         * <p>
         * Finishes up an HTML or JSP tag when &gt; is detected by the handler
         * method {@link #handle(char)}, but before {@link #endStage()}
         * executes. Because HTML parsing can be messy, this method resolves
         * several edge cases:
         * </p>
         * <ul>
         * <li>If the tag's type has not been determined (that is, its current
         * type is {@link NodeType#UNRESOLVED_HTML_TAG}, its type is resolved
         * to either {@link NodeType#HTML_COMBINED_TAG} or
         * {@link NodeType#HTML_START_TAG}, depending on whether the last
         * character was /.
         * <li>
         * <li>If the tag's name has not been set, because the parser has not
         * encountered whitespace that delimits attributes, its name is set.</li>
         * <li>If the tag is of type {@link NodeType#HTML_START_TAG} or
         * {@link NodeType#HTML_START_TAG}, the current ParseContext is pushed
         * on, or popped off, of the stack. In addition, if the tag is an end
         * tag, its parent is re-wired to the same parent as the start tag. This
         * makes the start and end tag logical peers, as they should be.</li>
         * </ul>
         * <p>
         * After these cases are resolved, the current node is re-set to a Text
         * node, in the character position starting after the right angle
         * bracket (&gt;).
         * </p>
         */
        private void handleTagEnd()
        {
            ParseContext ctx = ParseContext.currentContext();
            Node node = ctx.getNode();

            // Resolve tag type if not set
            if( node.getType() == NodeType.UNRESOLVED_HTML_TAG )
            {
                String lookbehind = ctx.lookbehind( 2 );
                if( NodeType.HTML_COMBINED_TAG.getTagEnd().equals( lookbehind ) )
                {
                    node.setType( NodeType.HTML_COMBINED_TAG );
                }
                else
                {
                    node.setType( NodeType.HTML_START_TAG );
                }
            }

            // Set the name if not set
            if( node.getName() == null )
            {
                int nameStart = ctx.getMarkerForStage( Stage.NAME );
                int nameEnd = ctx.position() + 1 - node.getType().getTagEnd().length();
                node.setName( ctx.getSource().substring( nameStart, nameEnd ) );
            }

            // If start or end tag, push/pop as needed;
            // otherwise start new Text
            switch( node.getType() )
            {
                case HTML_START_TAG: {
                    // Add the start tag to parent, and push it onto stack
                    endStage();
                    ctx = ParseContext.push();
                    break;
                }
                case HTML_END_TAG: {
                    // Make end tag the peer of the start tag
                    Node startTag = node.getParent();
                    node.setParent( startTag.getParent() );

                    // Get rid of the current node in the parent context
                    ctx = ParseContext.pop();
                    ctx.setNode( null );
                    break;
                }
            }

            // Start new Text node after the >
            ctx.setParser( TEXT_PARSER, Stage.TEXT, 1 );
        }
    }

    public interface Parser
    {
        public abstract void beginStage();

        public abstract void endStage();

        public abstract void handle( char ch );
    }

    public static class TextParser implements Parser
    {

        public void beginStage()
        {
            // Create Text node and set start at next character
            ParseContext ctx = ParseContext.currentContext();
            Text text = new Text( ctx.getDocument() );
            ctx.setNode( text );
            ctx.setStartPosition( ctx.getNode(), ctx.position() + 1 );
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
         * Handles {@link Stage#TEXT}.
         */
        public void handle( char ch )
        {
            ParseContext ctx = ParseContext.currentContext();
            switch( ctx.getStage() )
            {
                case TEXT: {
                    switch( ch )
                    {
                        // If we see a quote, check to see if it's a part of a
                        // parent attribute
                        case ('\''):
                        case ('"'): {
                            Node node = ctx.getNode();
                            Node parent = node.getParent();
                            if( parent instanceof Attribute )
                            {
                                Attribute attribute = (Attribute) parent;
                                if( ch == attribute.getAttributeDelimiter() )
                                {
                                    ctx = ParseContext.pop();
                                    ctx.setStage( Stage.ATTRIBUTE_END, 0 );
                                }
                            }
                            break;
                        }
                        case ('<'): {
                            // Valid XML or JSP tag start?
                            String lookahead = ctx.lookahead( 2 );
                            if( lookahead.length() == 2 )
                            {
                                char nextChar = lookahead.charAt( 1 );
                                if( "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ!/%_:".indexOf( nextChar ) != -1 )
                                {
                                    ctx.setParser( TAG_PARSER, Stage.TAG_START, 0 );
                                }
                            }
                            break;
                        }
                    }
                    break;
                }
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

    private enum Stage
    {
        /**
         * Parsing the left-angle bracket that starts the node (&lt;).
         */
        TAG_START,
        /** Parsing any text outside of a tag or element. */
        TEXT,
        /**
         * Characters that name a node. For tags, the node name begins after the
         * opening left bracket (&lt;) and ends before the first whitespace
         * character. For attributes, the name begins after any whitespace and
         * ends before the equals (=) character.
         */
        NAME,
        /**
         * Whitespace between the node or directive name and the first
         * attribute, and between attributes.
         */
        WHITESPACE,
        /** Any text inside of a scriptlet, JSP comment, or JSP declaration. */
        CODE_OR_COMMENT,
        /** Finishing an attribute. */
        ATTRIBUTE_END
    }

    /**
     * Encapsulates the current state of document parsing.
     */
    private static class ParseContext
    {
        private static final Stack<ParseContext> CONTEXT_STACK = new Stack<ParseContext>();

        private static ParseContext CURRENT_CONTEXT;

        public static ParseContext currentContext()
        {
            return CURRENT_CONTEXT;
        }

        public static ParseContext initialContext( JspDocument doc, String source )
        {
            CONTEXT_STACK.clear();
            ParseContext context = new ParseContext( doc, source, new Counter() );
            CURRENT_CONTEXT = context;

            // Set the default stage to TEXT
            Text text = new Text( doc );
            context.setNode( text );
            context.setStartPosition( text, 0 );
            context.m_stage = Stage.TEXT;
            context.m_parser = TEXT_PARSER;

            // Return the init'ed context
            return context;
        }

        private Parser m_parser = null;

        private final List<Integer> lineBreaks = new ArrayList<Integer>();

        private Node m_node = null;

        private final Counter m_counter;

        private final String m_source;

        private Stage m_stage = Stage.TEXT;

        private JspDocument m_doc;

        private Map<Stage, Integer> m_markers = new HashMap<Stage, Integer>();

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
        public int getMarkerForStage( Stage stage )
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
         * {@link Parser#beginStage()} or {@link Parser#endStage()} methods.
         * 
         * @param parser the parser to set
         */
        public void setParser( Parser parser )
        {
            m_parser = parser;
        }

        public ParseContext getParentContext()
        {
            return CONTEXT_STACK.size() == 0 ? null : CONTEXT_STACK.peek();
        }

        public Parser getParser()
        {
            return m_parser;
        }

        public String getSource()
        {
            return m_source;
        }

        public Stage getStage()
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
         * Pops the topmost ParseContext from the stack and replaces the
         * JspParser's current ParseContext with it. Before popping the
         * ParseContext, the current stage's {@link Parser#endStage()} method is
         * executed.
         */
        public static ParseContext pop()
        {
            // Run the endStage method for the current stage
            ParseContext ctx = ParseContext.currentContext();
            ctx.getParser().endStage();

            ctx = CONTEXT_STACK.pop();
            CURRENT_CONTEXT = ctx;
            return ctx;
        }

        public int position()
        {
            return m_counter.position();
        }

        /**
         * Pushes this ParseContext onto the stack and replaces the JspParser's
         * current ParseContext with a new one. This method does <em>not</em>
         * call the current Parser's {@link Parser#endStage()} method.
         * 
         * @return the new ParseContext
         */
        public static ParseContext push()
        {
            ParseContext oldCtx = currentContext();
            CONTEXT_STACK.push( oldCtx );
            ParseContext ctx = new ParseContext( oldCtx.m_doc, oldCtx.m_source, oldCtx.m_counter );
            CURRENT_CONTEXT = ctx;

            // Set the default stage to TEXT
            Node text = new Text( ctx.m_doc );
            ctx.setNode( text );
            ctx.m_stage = Stage.TEXT;
            ctx.m_parser = TEXT_PARSER;
            return ctx;
        }

        /**
         * Sets the current Node to a supplied node. If the start position is
         * not set, it set to the current position. If not already set, the
         * parent is set to the parent ParseContext's node, or else the
         * JspDocument's root node.
         * 
         * @param node the node to set. If <code>null</code>, the current
         *            node is removed
         */
        public void setNode( Node node )
        {
            m_node = node;
            if( node == null )
            {
                return;
            }

            // If start not set, set it now
            if( node.getStart() == Node.POSITION_NOT_SET )
            {
                setStartPosition( node, position() );
            }

            // Set the parent relationship
            if( node.getParent() == null )
            {
                if( getParentContext() == null )
                {
                    node.setParent( m_doc.getRoot() );
                }
                else
                {
                    node.setParent( getParentContext().getNode() );
                }
            }
        }

        /**
         * Sets the current lifecycle stage and mark its position, without
         * resetting the current parser. The "marker" is set relative to the
         * current position (as reported by {@link #position()}. The marker can
         * be retrieved later via
         * {@link #getMarkerForStage(com.ecyrd.jspwiki.ui.stripes.JspParser.Stage)}.
         * 
         * @param stage
         * @param increment the number of characters ahead of the current
         *            position to set the marker
         */
        public void setStage( Stage stage, int increment )
        {
            // Set the new stage.
            m_stage = stage;
            m_markers.put( getStage(), position() + increment );
        }

        /**
         * <p>
         * Ends the current {@link Stage} and starts another, and sets a marker
         * for the next character position. When this method is called, the
         * active {@link Parser} is finalized for the previous stage by calling
         * its {@link Parser#endStage()} method. Then, the current text parser
         * is replaced with the one that corresponds to the correct one for the
         * new stage, and a marker is set at the next position. Finally, the new
         * stage is initialized by calling the new parser's
         * {@link Parser #beginStage()} method.
         * </p>
         * 
         * @param parser the parser to set
         * @param stage the stage to set
         * @increment the number of characters ahead to set the marker for the
         *            new stage. Must be zero or higher.
         */
        public void setParser( Parser parser, Stage stage, int increment )
        {
            // Finish the parser's current stage
            if( parser != null && m_parser != null )
            {
                m_parser.endStage();
            }

            // Set the new stage and set a marker at the next position
            m_stage = stage;
            m_markers.put( stage, position() + increment );

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
            Parser parser = ctx.getParser();
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
            ctx.getParser().endStage();
        }

        return doc;
    }

}
