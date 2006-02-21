package com.ecyrd.jspwiki.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.StringTokenizer;


import com.ecyrd.jspwiki.StringTransmutator;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;

/**
 *   Provides an abstract interface towards the parser instances.
 *   
 *   @author jalkanen
 *
 */
public abstract class MarkupParser
{
    /** Allow this many characters to be pushed back in the stream.  In effect,
    this limits the size of a single heading line.  */
    private static final int              PUSHBACK_BUFFER_SIZE = 10*1024;
    protected PushbackReader                m_in;

    protected WikiEngine     m_engine;
    protected WikiContext    m_context;

    /** Optionally stores internal wikilinks */
    protected ArrayList      m_localLinkMutatorChain    = new ArrayList();
    protected ArrayList      m_externalLinkMutatorChain = new ArrayList();
    protected ArrayList      m_attachmentLinkMutatorChain = new ArrayList();
    protected ArrayList      m_headingListenerChain     = new ArrayList();
    protected ArrayList      m_linkMutators             = new ArrayList();

    protected boolean        m_inlineImages             = true;

    protected boolean        m_parseAccessRules = true;
    /** If set to "true", allows using raw HTML within Wiki text.  Be warned,
        this is a VERY dangerous option to set - never turn this on in a publicly
        allowable Wiki, unless you are absolutely certain of what you're doing. */
    public static final String     PROP_ALLOWHTML        = "jspwiki.translatorReader.allowHTML";
    /** If set to "true", enables plugins during parsing */
    public static final String     PROP_RUNPLUGINS       = "jspwiki.translatorReader.runPlugins";
    
    /** Lists all punctuation characters allowed in WikiMarkup. These
        will not be cleaned away. */
    
    protected static final String           PUNCTUATION_CHARS_ALLOWED = "._/";

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

    public void addHeadingListener( HeadingListener listener )
    {
        if( listener != null )
        {
            m_headingListenerChain.add( listener );
        }
    }

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
     *  @throws IOException
     */
    public abstract WikiDocument parse()
         throws IOException;

    /**
     *  Cleans a Wiki name.
     *  <P>
     *  [ This is a link ] -&gt; ThisIsALink
     *
     *  @param link Link to be cleared. Null is safe, and causes this to return null.
     *  @return A cleaned link.
     *
     *  @since 2.0
     */
    public static String cleanLink( String link )
    {
        StringBuffer clean = new StringBuffer();
    
        if( link == null ) return null;

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
        
        for( int i = 0; i < link.length(); i++ )
        {
            char ch = link.charAt(i);

            if( Character.isLetterOrDigit( ch ) || MarkupParser.PUNCTUATION_CHARS_ALLOWED.indexOf(ch) != -1 )
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

}
