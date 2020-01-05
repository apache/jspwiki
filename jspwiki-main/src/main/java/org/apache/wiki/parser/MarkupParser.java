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

import org.apache.log4j.Logger;
import org.apache.oro.text.GlobCompiler;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.wiki.StringTransmutator;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.jdom2.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *   Provides an abstract class for the parser instances.
 *
 *   @since  2.4
 */
public abstract class MarkupParser
{
    /** Allow this many characters to be pushed back in the stream.  In effect,
        this limits the size of a single line.  */
    protected static final int              PUSHBACK_BUFFER_SIZE = 10*1024;
    protected PushbackReader                m_in;
    private int              m_pos = -1; // current position in reader stream

    protected WikiEngine     m_engine;
    protected WikiContext    m_context;

    /** Optionally stores internal wikilinks */
    protected ArrayList<StringTransmutator>      m_localLinkMutatorChain    = new ArrayList<>();
    protected ArrayList<StringTransmutator>      m_externalLinkMutatorChain = new ArrayList<>();
    protected ArrayList<StringTransmutator>      m_attachmentLinkMutatorChain = new ArrayList<>();
    protected ArrayList<HeadingListener>         m_headingListenerChain     = new ArrayList<>();
    protected ArrayList<StringTransmutator>      m_linkMutators             = new ArrayList<>();

    protected boolean        m_inlineImages     = true;
    protected boolean        m_parseAccessRules = true;
    /** Keeps image regexp Patterns */
    protected List< Pattern > m_inlineImagePatterns = null;
    protected LinkParsingOperations m_linkParsingOperations;

    private static Logger log = Logger.getLogger( MarkupParser.class );

    /** If set to "true", allows using raw HTML within Wiki text.  Be warned,
        this is a VERY dangerous option to set - never turn this on in a publicly
        allowable Wiki, unless you are absolutely certain of what you're doing. */
    public static final String     PROP_ALLOWHTML        = "jspwiki.translatorReader.allowHTML";
    /** If set to "true", enables plugins during parsing */
    public static final String     PROP_RUNPLUGINS       = "jspwiki.translatorReader.runPlugins";

    /** Lists all punctuation characters allowed in WikiMarkup. These
        will not be cleaned away. This is for compatibility for older versions
        of JSPWiki. */
    protected static final String LEGACY_CHARS_ALLOWED      = "._";

    /** Lists all punctuation characters allowed in page names. */
    public    static final String PUNCTUATION_CHARS_ALLOWED = " ()&+,-=._$";

    public    static final String HASHLINK = "hashlink";

    /** Name of the outlink image; relative path to the JSPWiki directory. */
    public    static final String OUTLINK_IMAGE = "images/out.png";
    /** Outlink css class. */
    public    static final String OUTLINK = "outlink";

    /** If true, all outward links (external links) have a small link image appended. */
    public    static final String PROP_USEOUTLINKIMAGE  = "jspwiki.translatorReader.useOutlinkImage";

    private   static final String INLINE_IMAGE_PATTERNS = "JSPWikiMarkupParser.inlineImagePatterns";

    /** If set to "true", all external links are tagged with 'rel="nofollow"' */
    public static final String     PROP_USERELNOFOLLOW   = "jspwiki.translatorReader.useRelNofollow";

    /** The value for anchor element <tt>class</tt> attributes when used
     * for wiki page (normal) links. The value is "wikipage". */
   public static final String CLASS_WIKIPAGE = "wikipage";

   /** The value for anchor element <tt>class</tt> attributes when used
     * for edit page links. The value is "createpage". */
   public static final String CLASS_EDITPAGE = "createpage";

   /** The value for anchor element <tt>class</tt> attributes when used
     * for interwiki page links. The value is "interwiki". */
   public static final String CLASS_INTERWIKI = "interwiki";

   /** The value for anchor element <tt>class</tt> attributes when used
     * for footnote links. The value is "footnote". */
   public static final String CLASS_FOOTNOTE = "footnote";

   /** The value for anchor element <tt>class</tt> attributes when used
     * for footnote links. The value is "footnote". */
   public static final String CLASS_FOOTNOTE_REF = "footnoteref";

   /** The value for anchor element <tt>class</tt> attributes when used
     * for external links. The value is "external". */
   public static final String CLASS_EXTERNAL = "external";

   /** The value for anchor element <tt>class</tt> attributes when used
     * for attachments. The value is "attachment". */
   public static final String CLASS_ATTACHMENT = "attachment";

   public static final String[] CLASS_TYPES =
   {
      CLASS_WIKIPAGE,
      CLASS_EDITPAGE,
      "",
      CLASS_FOOTNOTE,
      CLASS_FOOTNOTE_REF,
      "",
      CLASS_EXTERNAL,
      CLASS_INTERWIKI,
      CLASS_EXTERNAL,
      CLASS_WIKIPAGE,
      CLASS_ATTACHMENT
   };

    /**
     *  Constructs a MarkupParser.  The subclass must call this constructor
     *  to set up the necessary bits and pieces.
     *
     *  @param context The WikiContext.
     *  @param in The reader from which we are reading the bytes from.
     */
    protected MarkupParser( WikiContext context, Reader in )
    {
        m_engine = context.getEngine();
        m_context = context;
        m_linkParsingOperations = new LinkParsingOperations( m_context );
        setInputReader( in );
    }

    /**
     *  Replaces the current input character stream with a new one.
     *  @param in New source for input.  If null, this method does nothing.
     *  @return the old stream
     */
    public Reader setInputReader( Reader in )
    {
        Reader old = m_in;

        if( in != null )
        {
            m_in = new PushbackReader( new BufferedReader( in ),
                                       PUSHBACK_BUFFER_SIZE );
        }

        return old;
    }

    /**
     *  Adds a hook for processing link texts.  This hook is called
     *  when the link text is written into the output stream, and
     *  you may use it to modify the text.  It does not affect the
     *  actual link, only the user-visible text.
     *
     *  @param mutator The hook to call.  Null is safe.
     */
    public void addLinkTransmutator( StringTransmutator mutator )
    {
        if( mutator != null )
        {
            m_linkMutators.add( mutator );
        }
    }

    /**
     *  Adds a hook for processing local links.  The engine
     *  transforms both non-existing and existing page links.
     *
     *  @param mutator The hook to call.  Null is safe.
     */
    public void addLocalLinkHook( StringTransmutator mutator )
    {
        if( mutator != null )
        {
            m_localLinkMutatorChain.add( mutator );
        }
    }

    /**
     *  Adds a hook for processing external links.  This includes
     *  all http:// ftp://, etc. links, including inlined images.
     *
     *  @param mutator The hook to call.  Null is safe.
     */
    public void addExternalLinkHook( StringTransmutator mutator )
    {
        if( mutator != null )
        {
            m_externalLinkMutatorChain.add( mutator );
        }
    }

    /**
     *  Adds a hook for processing attachment links.
     *
     *  @param mutator The hook to call.  Null is safe.
     */
    public void addAttachmentLinkHook( StringTransmutator mutator )
    {
        if( mutator != null )
        {
            m_attachmentLinkMutatorChain.add( mutator );
        }
    }

    /**
     *  Adds a HeadingListener to the parser chain.  It will be called whenever
     *  a parsed header is found.
     *
     *  @param listener The listener to add.
     */
    public void addHeadingListener( HeadingListener listener )
    {
        if( listener != null )
        {
            m_headingListenerChain.add( listener );
        }
    }

    /**
     *  Disables access rule parsing.
     */
    public void disableAccessRules()
    {
        m_parseAccessRules = false;
    }

    public boolean isParseAccessRules()
    {
        return m_parseAccessRules;
    }

    /**
     *  Use this to turn on or off image inlining.
     *  @param toggle If true, images are inlined (as per set in jspwiki.properties)
     *                If false, then images won't be inlined; instead, they will be
     *                treated as standard hyperlinks.
     *  @since 2.2.9
     */
    public void enableImageInlining( boolean toggle )
    {
        m_inlineImages = toggle;
    }

    public boolean isImageInlining() {
        return m_inlineImages;
    }

    @SuppressWarnings( "unchecked" )
    protected final void initInlineImagePatterns() {
		PatternCompiler compiler = new GlobCompiler();
        //
        //  We cache compiled patterns in the engine, since their creation is really expensive
        //
        List< Pattern > compiledpatterns = m_engine.getAttribute( INLINE_IMAGE_PATTERNS );

        if( compiledpatterns == null ) {
            compiledpatterns = new ArrayList< >( 20 );
            Collection< String > ptrns = m_engine.getAllInlinedImagePatterns();

            //
            //  Make them into Regexp Patterns.  Unknown patterns are ignored.
            //
            for( Iterator< String > i = ptrns.iterator(); i.hasNext(); ) {
            	String pattern = i.next();
                try {
                    compiledpatterns.add( compiler.compile( pattern,
                                                            GlobCompiler.DEFAULT_MASK | GlobCompiler.READ_ONLY_MASK ) );
                } catch( MalformedPatternException e ) {
                    log.error( "Malformed pattern [" + pattern + "] in properties: ", e );
                }
            }

            m_engine.setAttribute( INLINE_IMAGE_PATTERNS, compiledpatterns );
        }

        m_inlineImagePatterns = Collections.unmodifiableList( compiledpatterns );
	}

    public List< Pattern > getInlineImagePatterns() {
    	if( m_inlineImagePatterns == null ) {
    		initInlineImagePatterns();
    	}
    	return m_inlineImagePatterns;
    }

    /**
     *  Parses the document.
     *  @return the parsed document, as a WikiDocument
     *  @throws IOException If something goes wrong.
     */
    public abstract WikiDocument parse()
         throws IOException;

    /**
     *  Return the current position in the reader stream.
     *  The value will be -1 prior to reading.
     * @return the reader position as an int.
     */
    public int getPosition()
    {
        return m_pos;
    }

    /**
     * Returns the next token in the stream.  This is the most called method
     * in the entire parser, so it needs to be lean and mean.
     *
     * @return The next token in the stream; or, if the stream is ended, -1.
     * @throws IOException If something bad happens
     * @throws NullPointerException If you have not yet created an input document.
     */
    protected final int nextToken()
        throws IOException, NullPointerException
    {
        // if( m_in == null ) return -1;
        m_pos++;
        return m_in.read();
    }

    /**
     *  Push back any character to the current input.  Does not
     *  push back a read EOF, though.
     *
     *  @param c Character to push back.
     *  @throws IOException In case the character cannot be pushed back.
     */
    protected void pushBack( int c )
        throws IOException
    {
        if( c != -1 && m_in != null )
        {
            m_pos--;
            m_in.unread( c );
        }
    }

    /**
     *  Writes HTML for error message.  Does not add it to the document, you
     *  have to do it yourself.
     *
     *  @param error The error string.
     *  @return An Element containing the error.
     */

    public static Element makeError( String error )
    {
        return new Element("span").setAttribute("class","error").addContent(error);
    }

    /**
     *  Cleans a Wiki name.  The functionality of this method was changed in 2.6
     *  so that the list of allowed characters is much larger.  Use wikifyLink()
     *  to get the legacy behaviour.
     *  <P>
     *  [ This is a link ] -&gt; This is a link
     *
     *  @param link Link to be cleared. Null is safe, and causes this to return null.
     *  @return A cleaned link.
     *
     *  @since 2.0
     */
    public static String cleanLink( String link )
    {
        return cleanLink(link, PUNCTUATION_CHARS_ALLOWED);
    }

    /**
     *  Cleans a Wiki name based on a list of characters.  Also, any multiple
     *  whitespace is collapsed into a single space, and any leading or trailing
     *  space is removed.
     *
     *  @param link Link to be cleared. Null is safe, and causes this to return null.
     *  @param allowedChars Characters which are allowed in the string.
     *  @return A cleaned link.
     *
     *  @since 2.6
     */
    public static String cleanLink( String link, String allowedChars )
    {
        if( link == null ) return null;

        link = link.trim();
        StringBuilder clean = new StringBuilder(link.length());

        //
        //  Remove non-alphanumeric characters that should not
        //  be put inside WikiNames.  Note that all valid
        //  Unicode letters are considered okay for WikiNames.
        //  It is the problem of the WikiPageProvider to take
        //  care of actually storing that information.
        //
        //  Also capitalize things, if necessary.
        //

        boolean isWord = true;  // If true, we've just crossed a word boundary
        boolean wasSpace = false;

        for( int i = 0; i < link.length(); i++ )
        {
            char ch = link.charAt(i);

            //
            //  Cleans away repetitive whitespace and only uses the first one.
            //
            if( Character.isWhitespace(ch) )
            {
                if( wasSpace )
                    continue;

                wasSpace = true;
            }
            else
            {
                wasSpace = false;
            }

            //
            //  Check if it is allowed to use this char, and capitalize, if necessary.
            //
            if( Character.isLetterOrDigit( ch ) || allowedChars.indexOf(ch) != -1 )
            {
                // Is a letter

                if( isWord ) ch = Character.toUpperCase( ch );
                clean.append( ch );
                isWord = false;
            }
            else
            {
                isWord = true;
            }
        }

        return clean.toString();
    }

    /**
     *  Cleans away extra legacy characters.  This method functions exactly
     *  like pre-2.6 cleanLink()
     *  <P>
     *  [ This is a link ] -&gt; ThisIsALink
     *
     *  @param link Link to be cleared. Null is safe, and causes this to return null.
     *  @return A cleaned link.
     *  @since 2.6
     */
    public static String wikifyLink(String link)
    {
        return MarkupParser.cleanLink(link, MarkupParser.LEGACY_CHARS_ALLOWED);
    }

}
