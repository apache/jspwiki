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
package com.ecyrd.jspwiki.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;


import com.ecyrd.jspwiki.StringTransmutator;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;

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
    protected ArrayList<StringTransmutator>      m_localLinkMutatorChain    = new ArrayList<StringTransmutator>();
    protected ArrayList<StringTransmutator>      m_externalLinkMutatorChain = new ArrayList<StringTransmutator>();
    protected ArrayList<StringTransmutator>      m_attachmentLinkMutatorChain = new ArrayList<StringTransmutator>();
    protected ArrayList<HeadingListener>         m_headingListenerChain     = new ArrayList<HeadingListener>();
    protected ArrayList<StringTransmutator>      m_linkMutators             = new ArrayList<StringTransmutator>();

    protected boolean        m_inlineImages             = true;

    protected boolean        m_parseAccessRules = true;
    /** If set to "true", allows using raw HTML within Wiki text.  Be warned,
        this is a VERY dangerous option to set - never turn this on in a publicly
        allowable Wiki, unless you are absolutely certain of what you're doing. */
    public static final String     PROP_ALLOWHTML        = "jspwiki.translatorReader.allowHTML";
    /** If set to "true", enables plugins during parsing */
    public static final String     PROP_RUNPLUGINS       = "jspwiki.translatorReader.runPlugins";

    /** Lists all punctuation characters allowed in WikiMarkup. These
        will not be cleaned away. This is for compatibility for older versions
        of JSPWiki. */

    protected static final String           LEGACY_CHARS_ALLOWED      = "._";

    /** Lists all punctuation characters allowed in page names. */
    public    static final String           PUNCTUATION_CHARS_ALLOWED = " ()&+,-=._$";

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
        StringBuffer clean = new StringBuffer(link.length());

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
