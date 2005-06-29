/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.oro.text.*;
import org.apache.oro.text.regex.*;

import com.ecyrd.jspwiki.plugin.PluginManager;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.attachment.AttachmentManager;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.auth.acl.Acl;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.auth.user.UserDatabase;

/**
 *  Handles conversion from Wiki format into fully featured HTML.
 *  This is where all the magic happens.  It is CRITICAL that this
 *  class is tested, or all Wikis might die horribly.
 *  <P>
 *  The output of the HTML has not yet been validated against
 *  the HTML DTD.  However, it is very simple.
 *
 *  @author Janne Jalkanen
 */

public class TranslatorReader extends Reader
{
    public  static final int              READ          = 0;
    public  static final int              EDIT          = 1;
    private static final int              EMPTY         = 2;  // Empty message
    private static final int              LOCAL         = 3;
    private static final int              LOCALREF      = 4;
    private static final int              IMAGE         = 5;
    private static final int              EXTERNAL      = 6;
    private static final int              INTERWIKI     = 7;
    private static final int              IMAGELINK     = 8;
    private static final int              IMAGEWIKILINK = 9;
    public  static final int              ATTACHMENT    = 10;
    private static final int              ATTACHMENTIMAGE = 11;

    /** Lists all punctuation characters allowed in WikiMarkup. These
        will not be cleaned away. */

    private static final String           PUNCTUATION_CHARS_ALLOWED = "._";

    /** Allow this many characters to be pushed back in the stream.  In effect,
        this limits the size of a single heading line.  */
    private static final int              PUSHBACK_BUFFER_SIZE = 10*1024;
    private PushbackReader m_in;

    private StringReader   m_data = new StringReader("");

    private static Logger log = Logger.getLogger( TranslatorReader.class );

    //private boolean        m_iscode       = false;
    private boolean        m_isbold       = false;
    private boolean        m_isitalic     = false;
    private boolean        m_isTypedText  = false;
    private boolean        m_istable      = false;
    private boolean        m_isPre        = false;
    private boolean        m_isEscaping   = false;
    private boolean        m_isdefinition = false;

    /** Contains style information, in multiple forms. */
    private Stack          m_styleStack   = new Stack();
    
     // general list handling
    private int            m_genlistlevel = 0;
    private StringBuffer   m_genlistBulletBuffer = new StringBuffer();  // stores the # and * pattern
    private boolean        m_allowPHPWikiStyleLists = true;


    private boolean        m_isOpenParagraph = false;

    /** Tag that gets closed at EOL. */
    private String         m_closeTag     = null; 

    private WikiEngine     m_engine;
    private WikiContext    m_context;
    
    /** Optionally stores internal wikilinks */
    private ArrayList      m_localLinkMutatorChain    = new ArrayList();
    private ArrayList      m_externalLinkMutatorChain = new ArrayList();
    private ArrayList      m_attachmentLinkMutatorChain = new ArrayList();
    private ArrayList      m_headingListenerChain     = new ArrayList();

    /** Keeps image regexp Patterns */
    private ArrayList      m_inlineImagePatterns;

    private PatternMatcher m_inlineMatcher = new Perl5Matcher();

    private ArrayList      m_linkMutators = new ArrayList();

    /**
     *  This property defines the inline image pattern.  It's current value
     *  is jspwiki.translatorReader.inlinePattern
     */
    public static final String     PROP_INLINEIMAGEPTRN  = "jspwiki.translatorReader.inlinePattern";

    /** If true, consider CamelCase hyperlinks as well. */
    public static final String     PROP_CAMELCASELINKS   = "jspwiki.translatorReader.camelCaseLinks";

    /** If true, all hyperlinks are translated as well, regardless whether they
        are surrounded by brackets. */
    public static final String     PROP_PLAINURIS        = "jspwiki.translatorReader.plainUris";

    /** If true, all outward links (external links) have a small link image appended. */
    public static final String     PROP_USEOUTLINKIMAGE  = "jspwiki.translatorReader.useOutlinkImage";

    /** If set to "true", allows using raw HTML within Wiki text.  Be warned,
        this is a VERY dangerous option to set - never turn this on in a publicly
        allowable Wiki, unless you are absolutely certain of what you're doing. */
    public static final String     PROP_ALLOWHTML        = "jspwiki.translatorReader.allowHTML";

    /** If set to "true", all external links are tagged with 'rel="nofollow"' */
    public static final String     PROP_USERELNOFOLLOW   = "jspwiki.translatorReader.useRelNofollow";

    /** If set to "true", enables plugins during parsing */
    public static final String     PROP_RUNPLUGINS       = "jspwiki.translatorReader.runPlugins";
    
    /** If true, then considers CamelCase links as well. */
    private boolean                m_camelCaseLinks      = false;

    /** If true, consider URIs that have no brackets as well. */
    // FIXME: Currently reserved, but not used.
    private boolean                m_plainUris           = false;

    /** If true, all outward links use a small link image. */
    private boolean                m_useOutlinkImage     = true;

    /** If true, allows raw HTML. */
    private boolean                m_allowHTML           = false;

    /** If true, executes plugins; otherwise ignores them. */
    private boolean                m_enablePlugins       = true;

    private boolean                m_useRelNofollow      = false;

    private boolean                m_inlineImages        = true;
    
    private PatternMatcher         m_matcher  = new Perl5Matcher();
    private PatternCompiler        m_compiler = new Perl5Compiler();
    private Pattern                m_camelCasePtrn;

    private TextRenderer           m_renderer;

    /**
     *  The default inlining pattern.  Currently "*.png"
     */
    public static final String     DEFAULT_INLINEPATTERN = "*.png";

    /**
     *  These characters constitute word separators when trying
     *  to find CamelCase links.
     */
    private static final String    WORD_SEPARATORS = ",.|;+=&()";

    protected static final int BOLD           = 0;
    protected static final int ITALIC         = 1;
    protected static final int TYPED          = 2;
    
    /**
     *  This list contains all IANA registered URI protocol
     *  types as of September 2004 + a few well-known extra types.
     *
     *  JSPWiki recognises all of them as external links.
     */
    static final String[] c_externalLinks = {
        "http:", "ftp:", "https:", "mailto:",
        "news:", "file:", "rtsp:", "mms:", "ldap:",
        "gopher:", "nntp:", "telnet:", "wais:",
        "prospero:", "z39.50s", "z39.50r", "vemmi:",
        "imap:", "nfs:", "acap:", "tip:", "pop:",
        "dav:", "opaquelocktoken:", "sip:", "sips:",
        "tel:", "fax:", "modem:", "soap.beep:", "soap.beeps",
        "xmlrpc.beep", "xmlrpc.beeps", "urn:", "go:",
        "h323:", "ipp:", "tftp:", "mupdate:", "pres:",
        "im:", "mtqp", "smb:" };


    /**
     *  Creates a TranslatorReader using the default HTML renderer.
     */
    public TranslatorReader( WikiContext context, Reader in )
    {
        initialize( context, in, new HTMLRenderer() );
    }

    public TranslatorReader( WikiContext context, Reader in, TextRenderer renderer )
    {
        initialize( context, in, renderer );
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
     *  @param engine The WikiEngine this reader is attached to.  Is
     * used to figure out of a page exits.
     */

    // FIXME: TranslatorReaders should be pooled for better performance.
    private void initialize( WikiContext context, 
                             Reader in, 
                             TextRenderer renderer )
    {
        PatternCompiler compiler         = new GlobCompiler();
        ArrayList       compiledpatterns = new ArrayList();

        m_engine = context.getEngine();
        m_context = context;

        m_renderer = renderer;

        setInputReader( in );

        Collection ptrns = getImagePatterns( m_engine );

        //
        //  Make them into Regexp Patterns.  Unknown patterns
        //  are ignored.
        //
        for( Iterator i = ptrns.iterator(); i.hasNext(); )
        {
            try
            {       
                compiledpatterns.add( compiler.compile( (String)i.next() ) );
            }
            catch( MalformedPatternException e )
            {
                log.error("Malformed pattern in properties: ", e );
            }
        }

        m_inlineImagePatterns = compiledpatterns;

        try
        {
            m_camelCasePtrn = m_compiler.compile( "^([[:^alnum:]]*|\\~)([[:upper:]]+[[:lower:]]+[[:upper:]]+[[:alnum:]]*)[[:^alnum:]]*$" );
        }
        catch( MalformedPatternException e )
        {
            log.fatal("Internal error: Someone put in a faulty pattern.",e);
            throw new InternalWikiException("Faulty camelcasepattern in TranslatorReader");
        }

        //
        //  Set the properties.
        //
        Properties props      = m_engine.getWikiProperties();

        String cclinks = (String)m_context.getPage().getAttribute( PROP_CAMELCASELINKS );

        if( cclinks != null )
        {
            m_camelCaseLinks = TextUtil.isPositive( cclinks );
        }
        else
        {
            m_camelCaseLinks  = TextUtil.getBooleanProperty( props,
                                                             PROP_CAMELCASELINKS, 
                                                             m_camelCaseLinks );
        }

        m_plainUris           = TextUtil.getBooleanProperty( props,
                                                             PROP_PLAINURIS,
                                                             m_plainUris );
        m_useOutlinkImage     = TextUtil.getBooleanProperty( props,
                                                             PROP_USEOUTLINKIMAGE, 
                                                             m_useOutlinkImage );
        m_allowHTML           = TextUtil.getBooleanProperty( props,
                                                             PROP_ALLOWHTML, 
                                                             m_allowHTML );

        m_useRelNofollow      = TextUtil.getBooleanProperty( props,
                                                             PROP_USERELNOFOLLOW,
                                                             m_useRelNofollow );
    
        String runplugins = m_engine.getVariable( m_context, PROP_RUNPLUGINS );
        if( runplugins != null ) enablePlugins( TextUtil.isPositive(runplugins));
        
        if( m_engine.getUserDatabase() == null || m_engine.getAuthorizationManager() == null )
        {
            disableAccessRules();
        }   
        
        m_context.getPage().setHasMetadata();
    }

    /**
     *  Sets the currently used renderer.  This method is protected because
     *  we only want to use it internally for now.  The renderer interface
     *  is not yet set to stone, so it's not expected that third parties
     *  would use this.
     */
    protected void setRenderer( TextRenderer renderer )
    {
        m_renderer = renderer;
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

    private boolean m_parseAccessRules = true;

    public void disableAccessRules()
    {
        m_parseAccessRules = false;
    }

    /**
     *  Can be used to turn on plugin execution on a translator-reader basis
     */
    public void enablePlugins( boolean toggle )
    {
        m_enablePlugins = toggle;
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
     *  Figure out which image suffixes should be inlined.
     *  @return Collection of Strings with patterns.
     */

    protected static Collection getImagePatterns( WikiEngine engine )
    {
        Properties props    = engine.getWikiProperties();
        ArrayList  ptrnlist = new ArrayList();

        for( Enumeration e = props.propertyNames(); e.hasMoreElements(); )
        {
            String name = (String) e.nextElement();

            if( name.startsWith( PROP_INLINEIMAGEPTRN ) )
            {
                String ptrn = props.getProperty( name );

                ptrnlist.add( ptrn );
            }
        }

        if( ptrnlist.size() == 0 )
        {
            ptrnlist.add( DEFAULT_INLINEPATTERN );
        }

        return ptrnlist;
    }

    /**
     *  Returns link name, if it exists; otherwise it returns null.
     */
    private String linkExists( String page )
    {
        try
		{
			if( page == null || page.length() == 0 ) return null;
			
            return m_engine.getFinalPageName( page );
        }
        catch( ProviderException e )
        {
            log.warn("TranslatorReader got a faulty page name!",e);

            return page;  // FIXME: What would be the correct way to go back?
        }
    }

    /**
     *  Calls a transmutator chain.
     *
     *  @param list Chain to call
     *  @param text Text that should be passed to the mutate() method
     *              of each of the mutators in the chain.
     *  @return The result of the mutation.
     */

    private String callMutatorChain( Collection list, String text )
    {
        if( list == null || list.size() == 0 )
        {
            return text;
        }

        for( Iterator i = list.iterator(); i.hasNext(); )
        {
            StringTransmutator m = (StringTransmutator) i.next();

            text = m.mutate( m_context, text );
        }

        return text;
    }

    private void callHeadingListenerChain( Heading param )
    {
        List list = m_headingListenerChain;

        for( Iterator i = list.iterator(); i.hasNext(); )
        {
            HeadingListener h = (HeadingListener) i.next();
            
            h.headingAdded( m_context, param );
        }
    }

    /**
     *  Write a HTMLized link depending on its type.
     *  The link mutator chain is processed.
     *
     *  @param type Type of the link.
     *  @param link The actual link.
     *  @param text The user-visible text for the link.
     */
    public String makeLink( int type, String link, String text )
    {
        if( text == null ) text = link;

        text = callMutatorChain( m_linkMutators, text );

        return m_renderer.makeLink( type, link, text );
    }
    
    /**
     *  Just like makeLink, but also adds the section reference (#sect...)
     */
    private String makeLink( int type, String link, String text, String sectref )
    {
        if( text == null ) text = link;

        text = callMutatorChain( m_linkMutators, text );

        return m_renderer.makeLink( type, link, text, sectref );
    }


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
        //  Compress away all whitespace and capitalize
        //  all words in between.
        //

        StringTokenizer st = new StringTokenizer( link, " -" );

        while( st.hasMoreTokens() )
        {
            StringBuffer component = new StringBuffer(st.nextToken());

            component.setCharAt(0, Character.toUpperCase( component.charAt(0) ) );

            //
            //  We must do this, because otherwise compiling on JDK 1.4 causes
            //  a downwards incompatibility to JDK 1.3.
            //
            clean.append( component.toString() );
        }

        //
        //  Remove non-alphanumeric characters that should not
        //  be put inside WikiNames.  Note that all valid
        //  Unicode letters are considered okay for WikiNames.
        //  It is the problem of the WikiPageProvider to take
        //  care of actually storing that information.
        //

        for( int i = 0; i < clean.length(); i++ )
        {
            char ch = clean.charAt(i);

            if( !(Character.isLetterOrDigit(ch) ||
                  PUNCTUATION_CHARS_ALLOWED.indexOf(ch) != -1 ))
            {
                clean.deleteCharAt(i);
                --i; // We just shortened this buffer.
            }
        }

        return clean.toString();
    }

    /**
     *  Figures out if a link is an off-site link.  This recognizes
     *  the most common protocols by checking how it starts.
     */

    // FIXME: Should really put the external link types to a sorted set,
    //        then searching for them would be faster.
    private boolean isExternalLink( String link )
    {
        for( int i = 0; i < c_externalLinks.length; i++ )
        {
            if( link.startsWith( c_externalLinks[i] ) ) return true;
        }

        return false;
    }

    /**
     *  Returns true, if the link in question is an access
     *  rule.
     */
    private static boolean isAccessRule( String link )
    {
        return link.startsWith("{ALLOW") || link.startsWith("{DENY");
    }

    /**
     *  Matches the given link to the list of image name patterns
     *  to determine whether it should be treated as an inline image
     *  or not.
     */
    private boolean isImageLink( String link )
    {
        if( m_inlineImages )
        {
            for( Iterator i = m_inlineImagePatterns.iterator(); i.hasNext(); )
            {
                if( m_inlineMatcher.matches( link, (Pattern) i.next() ) )
                    return true;
            }
        }

        return false;
    }

    private static boolean isMetadata( String link )
    {
        return link.startsWith("{SET");
    }

    /**
     *  Returns true, if the argument contains a number, otherwise false.
     *  In a quick test this is roughly the same speed as Integer.parseInt()
     *  if the argument is a number, and roughly ten times the speed, if
     *  the argument is NOT a number.
     */

    private boolean isNumber( String s )
    {
        if( s == null ) return false;

        if( s.length() > 1 && s.charAt(0) == '-' )
            s = s.substring(1);

        for( int i = 0; i < s.length(); i++ )
        {
            if( !Character.isDigit(s.charAt(i)) )
                return false;
        }

        return true;
    }

    /**
     *  Checks for the existence of a traditional style CamelCase link.
     *  <P>
     *  We separate all white-space -separated words, and feed it to this
     *  routine to find if there are any possible camelcase links.
     *  For example, if "word" is "__HyperLink__" we return "HyperLink".
     *
     *  @param word A phrase to search in.
     *  @return The match within the phrase.  Returns null, if no CamelCase
     *          hyperlink exists within this phrase.
     */
    private String checkForCamelCaseLink( String word )
    {
        PatternMatcherInput input;

        input = new PatternMatcherInput( word );

        if( m_matcher.contains( input, m_camelCasePtrn ) )
        {
            MatchResult res = m_matcher.getMatch();
  
            int start = res.beginOffset(2);
            int end   = res.endOffset(2);

            String link = res.group(2);

            if( res.group(1) != null )
            {
                if( res.group(1).equals("~") ||
                    res.group(1).indexOf('[') != -1 )
                {
                    // Delete the (~) from beginning.
                    // We'll make '~' the generic kill-processing-character from
                    // now on.
                    return null;
                }
            }

            return link;
        } // if match

        return null;
    }

    /**
     *  When given a link to a WikiName, we just return
     *  a proper HTML link for it.  The local link mutator
     *  chain is also called.
     */
    private String makeCamelCaseLink( String wikiname )
    {
        String matchedLink;
        String link;

        callMutatorChain( m_localLinkMutatorChain, wikiname );

        if( (matchedLink = linkExists( wikiname )) != null )
        {
            link = makeLink( READ, matchedLink, wikiname );
        }
        else
        {
            link = makeLink( EDIT, wikiname, wikiname );
        }

        return link;
    }

    private String makeDirectURILink( String url )
    {
        String last = "";
        String result;

        if( url.endsWith(",") || url.endsWith(".") )
        {
            last = url.substring( url.length()-1 );
            url  = url.substring( 0, url.length()-1 );
        }

        callMutatorChain( m_externalLinkMutatorChain, url );

        if( isImageLink( url ) )
        {
            result = handleImageLink( url, url, false );
        }
        else
        {
            result = makeLink( EXTERNAL, url, url ) + m_renderer.outlinkImage();
        }

        result += last;

        return result;
    }

    /**
     *  Image links are handled differently:
     *  1. If the text is a WikiName of an existing page,
     *     it gets linked.
     *  2. If the text is an external link, then it is inlined.  
     *  3. Otherwise it becomes an ALT text.
     *
     *  @param reallink The link to the image.
     *  @param link     Link text portion, may be a link to somewhere else.
     *  @param hasLinkText If true, then the defined link had a link text available.
     *                  This means that the link text may be a link to a wiki page,
     *                  or an external resource.
     */
    
    private String handleImageLink( String reallink, String link, boolean hasLinkText )
    {
        String possiblePage = cleanLink( link );
        String matchedLink;
        String res = "";

        if( isExternalLink( link ) && hasLinkText )
        {
            res = makeLink( IMAGELINK, reallink, link );
        }
        else if( (matchedLink = linkExists( possiblePage )) != null &&
                 hasLinkText )
        {
            // System.out.println("Orig="+link+", Matched: "+matchedLink);
            callMutatorChain( m_localLinkMutatorChain, possiblePage );
            
            res = makeLink( IMAGEWIKILINK, reallink, link );
        }
        else
        {
            res = makeLink( IMAGE, reallink, link );
        }

        return res;
    }

    private String handleAccessRule( String ruleLine )
    {
        if( !m_parseAccessRules ) return "";
        Acl acl;
        WikiPage          page = m_context.getPage();
        UserDatabase      db = m_context.getEngine().getUserDatabase();

        if( ruleLine.startsWith( "{" ) )
            ruleLine = ruleLine.substring( 1 );
        if( ruleLine.endsWith( "}" ) )
            ruleLine = ruleLine.substring( 0, ruleLine.length() - 1 );

        log.debug("page="+page.getName()+", ACL = "+ruleLine);
        
        try
        {
            acl = m_engine.getAclManager().parseAcl( page, ruleLine );

            page.setAcl( acl );

            log.debug( acl.toString() );
        }
        catch( WikiSecurityException wse )
        {
            return m_renderer.makeError( wse.getMessage() );
        }

        return "";
    }

    /**
     *  Handles metadata setting [{SET foo=bar}]
     */
    private String handleMetadata( String link )
    {
        try
        {
            String args = link.substring( link.indexOf(' '), link.length()-1 );
            
            String name = args.substring( 0, args.indexOf('=') );
            String val  = args.substring( args.indexOf('=')+1, args.length() );

            name = name.trim();
            val  = val.trim();

            if( val.startsWith("'") ) val = val.substring( 1 );
            if( val.endsWith("'") )   val = val.substring( 0, val.length()-1 );

            // log.debug("SET name='"+name+"', value='"+val+"'.");

            if( name.length() > 0 && val.length() > 0 )
            {
                val = m_engine.getVariableManager().expandVariables( m_context,
                                                                     val );
            
                m_context.getPage().setAttribute( name, val );
            }
        }
        catch( Exception e )
        {
            m_renderer.makeError(" Invalid SET found: "+link);
        }

        return "";
    }

    /**
     *  Gobbles up all hyperlinks that are encased in square brackets.
     */
    private String handleHyperlinks( String link )
    {
        StringBuffer sb        = new StringBuffer();
        String       reallink;
        int          cutpoint;

        if( isAccessRule( link ) )
        {
            return handleAccessRule( link );
        }

        if( isMetadata( link ) )
        {
            return handleMetadata( link );
        }

        if( PluginManager.isPluginLink( link ) )
        {
            String included = "";
            try
            {
                if( m_enablePlugins )
                {
                    included = m_engine.getPluginManager().execute( m_context, link );
                }
            }
            catch( PluginException e )
            {
                log.info( "Failed to insert plugin", e );
                log.info( "Root cause:",e.getRootThrowable() );
                included = m_renderer.makeError("Plugin insertion failed: "+e.getMessage());
            }
                            
            sb.append( included );

            return sb.toString();
        }

        link = TextUtil.replaceEntities( link );

        if( (cutpoint = link.indexOf('|')) != -1 )
        {                    
            reallink = link.substring( cutpoint+1 ).trim();
            link = link.substring( 0, cutpoint );
        }
        else
        {
            reallink = link.trim();
        }

        int interwikipoint = -1;

        //
        //  Yes, we now have the components separated.
        //  link     = the text the link should have
        //  reallink = the url or page name.
        //
        //  In many cases these are the same.  [link|reallink].
        //  
        if( VariableManager.isVariableLink( link ) )
        {
            String value;
            
            try
            {
                value = m_engine.getVariableManager().parseAndGetValue( m_context, link );
            }
            catch( NoSuchVariableException e )
            {
                value = m_renderer.makeError(e.getMessage());
            }
            catch( IllegalArgumentException e )
            {
                value = m_renderer.makeError(e.getMessage());
            }

            sb.append( value );
        }
        else if( isExternalLink( reallink ) )
        {
            // It's an external link, out of this Wiki

            callMutatorChain( m_externalLinkMutatorChain, reallink );

            if( isImageLink( reallink ) )
            {
                sb.append( handleImageLink( reallink, link, (cutpoint != -1) ) );
            }
            else
            {
                sb.append( makeLink( EXTERNAL, reallink, link ) );
                sb.append( m_renderer.outlinkImage() );
            }
        }
        else if( (interwikipoint = reallink.indexOf(":")) != -1 )
        {
            // It's an interwiki link
            // InterWiki links also get added to external link chain
            // after the links have been resolved.
            
            // FIXME: There is an interesting issue here:  We probably should
            //        URLEncode the wikiPage, but we can't since some of the
            //        Wikis use slashes (/), which won't survive URLEncoding.
            //        Besides, we don't know which character set the other Wiki
            //        is using, so you'll have to write the entire name as it appears
            //        in the URL.  Bugger.
            
            String extWiki = reallink.substring( 0, interwikipoint );
            String wikiPage = reallink.substring( interwikipoint+1 );

            String urlReference = m_engine.getInterWikiURL( extWiki );

            if( urlReference != null )
            {
                urlReference = TextUtil.replaceString( urlReference, "%s", wikiPage );
                callMutatorChain( m_externalLinkMutatorChain, urlReference );

                sb.append( makeLink( INTERWIKI, urlReference, link ) );

                if( isExternalLink(urlReference) )
                {
                    sb.append( m_renderer.outlinkImage() );
                }
            }
            else
            {
                sb.append( link+" "+m_renderer.makeError("No InterWiki reference defined in properties for Wiki called '"+extWiki+"'!)") );
            }
        }
        else if( reallink.startsWith("#") )
        {
            // It defines a local footnote
            sb.append( makeLink( LOCAL, reallink, link ) );
        }
        else if( isNumber( reallink ) )
        {
            // It defines a reference to a local footnote
            sb.append( makeLink( LOCALREF, reallink, link ) );
        }
        else
        {
            int hashMark = -1;

            //
            //  Internal wiki link, but is it an attachment link?
            //
            String attachment = findAttachment( reallink );
            if( attachment != null )
            {
                callMutatorChain( m_attachmentLinkMutatorChain, attachment );

                if( isImageLink( reallink ) )
                {
                    attachment = m_context.getURL( WikiContext.ATTACH, attachment );
                    sb.append( handleImageLink( attachment, link, (cutpoint != -1) ) );
                }
                else
                {
                    sb.append( makeLink( ATTACHMENT, attachment, link ) );
                }
            }
            else if( (hashMark = reallink.indexOf('#')) != -1 )
            {
                // It's an internal Wiki link, but to a named section

                String namedSection = reallink.substring( hashMark+1 );
                reallink = reallink.substring( 0, hashMark );

                reallink     = cleanLink( reallink );

                callMutatorChain( m_localLinkMutatorChain, reallink );

                String matchedLink;
                if( (matchedLink = linkExists( reallink )) != null )
                {
                    String sectref = "section-"+m_engine.encodeName(matchedLink)+"-"+namedSection;
                    sectref = sectref.replace('%', '_');
                    sb.append( makeLink( READ, matchedLink, link, sectref ) );
                }
                else
                {
                    sb.append( makeLink( EDIT, reallink, link ) );
                }
            }
            else
            {
                // It's an internal Wiki link
                reallink = cleanLink( reallink );

                callMutatorChain( m_localLinkMutatorChain, reallink );

                String matchedLink = linkExists( reallink );
				
                if( matchedLink != null )
				{
                    sb.append( makeLink( READ, matchedLink, link ) );
                }
                else
                {
                    sb.append( makeLink( EDIT, reallink, link ) );
                }
            }
        }

        return sb.toString();
    }

    private String findAttachment( String link )
    {
        AttachmentManager mgr = m_engine.getAttachmentManager();
        WikiPage currentPage = m_context.getPage();
        Attachment att = null;

        /*
        System.out.println("Finding attachment of page "+currentPage.getName());
        System.out.println("With name "+link);
        */

        try
        {
            att = mgr.getAttachmentInfo( m_context, link );
        }
        catch( ProviderException e )
        {
            log.warn("Finding attachments failed: ",e);
            return null;
        }

        if( att != null )
        {
            return att.getName();
        }
        else if( link.indexOf('/') != -1 )
        {
            return link;
        }

        return null;
    }

    /**
     *  Closes all annoying lists and things that the user might've
     *  left open.
     */
    private String closeAll()
    {
        StringBuffer buf = new StringBuffer();

        if( m_isbold )
        {
            buf.append(m_renderer.closeTextEffect(BOLD));
            m_isbold = false;
        }

        if( m_isitalic )
        {
            buf.append(m_renderer.closeTextEffect(ITALIC));
            m_isitalic = false;
        }

        if( m_isTypedText )
        {
            buf.append(m_renderer.closeTextEffect(TYPED));
            m_isTypedText = false;
        }

        /*
        for( ; m_listlevel > 0; m_listlevel-- )
        {
            buf.append( "</ul>\n" );
        }

        for( ; m_numlistlevel > 0; m_numlistlevel-- )
        {
            buf.append( "</ol>\n" );
        }
        */
        // cleanup OL and UL lists
        buf.append(unwindGeneralList());

        if( m_isPre ) 
        {
            buf.append(m_renderer.closePreformatted());
	    m_isEscaping   = false;
            m_isPre = false;
        }

        if( m_istable )
        {
            buf.append( m_renderer.closeTable() );
            m_istable = false;
        }

	if( m_isOpenParagraph )
	{
	    buf.append( m_renderer.closeParagraph() );
	    m_isOpenParagraph = false;
	}

        return buf.toString();
    }

    // FIXME: should remove countChar() as it is unused
    /**
     *  Counts how many consecutive characters of a certain type exists on the line.
     *  @param line String of chars to check.
     *  @param startPos Position to start reading from.
     *  @param char Character to check for.
     */
    private int countChar( String line, int startPos, char c )
    {
        int count;

        for( count = 0; (startPos+count < line.length()) && (line.charAt(count+startPos) == c); count++ );

        return count;
    }

    /**
     *  Returns a new String that has char c n times.
     */
    private String repeatChar( char c, int n )
    {
        StringBuffer sb = new StringBuffer();
        for( int i = 0; i < n; i++ ) sb.append(c);

        return sb.toString();
    }

    private int nextToken()
        throws IOException
    {
        if( m_in == null ) return -1;
        return m_in.read();
    }

    /**
     *  Push back any character to the current input.  Does not
     *  push back a read EOF, though.
     */
    private void pushBack( int c )
        throws IOException
    {        
        if( c != -1 && m_in != null )
        {
            m_in.unread( c );
        }
    }

    /**
     *  Pushes back any string that has been read.  It will obviously
     *  be pushed back in a reverse order.
     *
     *  @since 2.1.77
     */
    private void pushBack( String s )
        throws IOException
    {
        for( int i = s.length()-1; i >= 0; i-- )
        {
            pushBack( s.charAt(i) );
        }
    }

    private String handleBackslash()
        throws IOException
    {
        int ch = nextToken();

        if( ch == '\\' )
        {
            int ch2 = nextToken();

            if( ch2 == '\\' )
            {
                return m_renderer.lineBreak(true);
            }
           
            pushBack( ch2 );

            return m_renderer.lineBreak(false);
        }

        pushBack( ch );

        return "\\";
    }

    private String handleUnderscore()
        throws IOException
    {
        int ch = nextToken();
        String res = "_";

        if( ch == '_' )
        {
            res      = m_isbold ? m_renderer.closeTextEffect(BOLD) : m_renderer.openTextEffect(BOLD);
            m_isbold = !m_isbold;
        }
        else
        {
            pushBack( ch );
        }

        return res;
    }

    /**
     *  For example: italics.
     */
    private String handleApostrophe()
        throws IOException
    {
        int ch = nextToken();
        String res = "'";

        if( ch == '\'' )
        {
            res        = m_isitalic ? m_renderer.closeTextEffect(ITALIC) : m_renderer.openTextEffect(ITALIC);
            m_isitalic = !m_isitalic;
        }
        else
        {
            pushBack( ch );
        }

        return res;
    }

    private String handleOpenbrace( boolean isBlock )
        throws IOException
    {
        int ch = nextToken();
        String res = "{";

        if( ch == '{' )
        {
            int ch2 = nextToken();

            if( ch2 == '{' )
            {
                res = startBlockLevel()+m_renderer.openPreformatted( isBlock );
                m_isPre = true;
                m_isEscaping = true;
            }
            else
            {
                pushBack( ch2 );
                
                res = m_renderer.openTextEffect(TYPED);
                m_isTypedText = true;
           }
        }
        else
        {
            pushBack( ch );
        }

        return res;
    }

    /**
     *  Handles both }} and }}}
     */
    private String handleClosebrace()
        throws IOException
    {
        String res = "}";

        int ch2 = nextToken();

        if( ch2 == '}' )
        {
            int ch3 = nextToken();

            if( ch3 == '}' )
            {
                if( m_isPre )
                {
                    m_isPre = false;
		    m_isEscaping = false;
                    res = m_renderer.closePreformatted();
                }
                else
                {
                    res = "}}}";
                }
            }
            else
            {
                pushBack( ch3 );

                if( !m_isEscaping )
                {
                    res = m_renderer.closeTextEffect(TYPED);
                    m_isTypedText = false;
                }
                else
                {
                    pushBack( ch2 );
                }
            }
        }
        else
        {
            pushBack( ch2 );
        }

        return res;
    }

    private String handleDash()
        throws IOException
    {
        int ch = nextToken();

        if( ch == '-' )
        {
            int ch2 = nextToken();

            if( ch2 == '-' )
            {
                int ch3 = nextToken();

                if( ch3 == '-' ) 
                {
                    // Empty away all the rest of the dashes.
                    // Do not forget to return the first non-match back.
                    while( (ch = nextToken()) == '-' );
                    
                    pushBack(ch);
                    return startBlockLevel()+m_renderer.makeRuler();
                }
        
                pushBack( ch3 );
            }
            pushBack( ch2 );
        }

        pushBack( ch );

        return "-";
    }

    /**
     *  This method peeks ahead in the stream until EOL and returns the result.
     *  It will keep the buffers untouched.
     *
     *  @return The string from the current position to the end of line.
     */

    // FIXME: Always returns an empty line, even if the stream is full.
    private String peekAheadLine()
        throws IOException
    {
        String s = readUntilEOL().toString();
        pushBack( s );

        return s;
    }

    private String handleHeading()
        throws IOException
    {
        StringBuffer buf = new StringBuffer();

        int ch  = nextToken();

        Heading hd = new Heading();

        if( ch == '!' )
        {
            int ch2 = nextToken();

            if( ch2 == '!' )
            {
                String title = peekAheadLine();
                
                buf.append( m_renderer.makeHeading( Heading.HEADING_LARGE, title, hd) );
            }
            else
            {
                pushBack( ch2 );
                String title = peekAheadLine();
                buf.append( m_renderer.makeHeading( Heading.HEADING_MEDIUM, title, hd ) );
            }
        }
        else
        {
            pushBack( ch );
            String title = peekAheadLine();
            buf.append( m_renderer.makeHeading( Heading.HEADING_SMALL, title, hd ) );
        }

        callHeadingListenerChain( hd );

        return buf.toString();
    }

    /**
     *  Reads the stream until the next EOL or EOF.  Note that it will also read the
     *  EOL from the stream.
     */
    private StringBuffer readUntilEOL()
        throws IOException
    {
        int ch;
        StringBuffer buf = new StringBuffer();

        while( true )
        {
            ch = nextToken();

            if( ch == -1 )
                break;

            buf.append( (char) ch );

            if( ch == '\n' ) 
                break;
        }

        return buf;
    }

    /**
     *  Starts a block level element, therefore closing the
     *  a potential open paragraph tag.
     */
    private String startBlockLevel()
    {
        if( m_isOpenParagraph )
        {
            m_isOpenParagraph = false;
            return m_renderer.closeParagraph();
        }

        return "";
    }

    /**
     *  Like original handleOrderedList() and handleUnorderedList()
     *  however handles both ordered ('#') and unordered ('*') mixed together.
     */

    // FIXME: Refactor this; it's a bit messy.

    private String handleGeneralList()
        throws IOException
    {
         String cStrShortName = "handleGeneralList()";   //put log messages in some context

         StringBuffer buf = new StringBuffer();

         buf.append( startBlockLevel() );

         String strBullets = readWhile( "*#" );
         String strBulletsRaw = strBullets;      // to know what was original before phpwiki style substitution
         int numBullets = strBullets.length();

         // override the beginning portion of bullet pattern to be like the previous
         // to simulate PHPWiki style lists

         if(m_allowPHPWikiStyleLists)
         {
             // only substitute if different
             if(!( strBullets.substring(0,Math.min(numBullets,m_genlistlevel)).equals
                   (m_genlistBulletBuffer.substring(0,Math.min(numBullets,m_genlistlevel)) ) ) )
             {
                 if(numBullets <= m_genlistlevel)
                 {
                     // Substitute all but the last character (keep the expressed bullet preference)
                     strBullets  = (numBullets > 1 ? m_genlistBulletBuffer.substring(0, numBullets-1) : "")
                                   + strBullets.substring(numBullets-1, numBullets);
                 }
                 else
                 {
                     strBullets = m_genlistBulletBuffer + strBullets.substring(m_genlistlevel, numBullets);
                 }
             }
         }

         //
         //  Check if this is still of the same type
         //
         if( strBullets.substring(0,Math.min(numBullets,m_genlistlevel)).equals
            (m_genlistBulletBuffer.substring(0,Math.min(numBullets,m_genlistlevel)) ) )
         {
             int chBullet;

             if( numBullets > m_genlistlevel )
             {
                 buf.append( m_renderer.openList(strBullets.charAt(m_genlistlevel++)) );

                 for( ; m_genlistlevel < numBullets; m_genlistlevel++ )
                 {
                     // bullets are growing, get from new bullet list
                     buf.append( m_renderer.openListItem() );
                     buf.append( m_renderer.openList(strBullets.charAt(m_genlistlevel)) );
                 }
             }
             else if( numBullets < m_genlistlevel )
             {
                 //  Close the previous list item.
                 buf.append( m_renderer.closeListItem() );

                 for( ; m_genlistlevel > numBullets; m_genlistlevel-- )
                 {
                     // bullets are shrinking, get from old bullet list
                     buf.append( m_renderer.closeList(m_genlistBulletBuffer.charAt(m_genlistlevel - 1)) );
                     if( m_genlistlevel > 0 ) buf.append( m_renderer.closeListItem() );

                 }
             }
             else
             {
                 if( m_genlistlevel > 0 ) buf.append( m_renderer.closeListItem() );
             }
         }
         else
         {
             //
             //  The pattern has changed, unwind and restart
             //
             char chBullet;
             int  numEqualBullets;
             int  numCheckBullets;

             // find out how much is the same
             numEqualBullets = 0;
             numCheckBullets = Math.min(numBullets,m_genlistlevel);

             while( numEqualBullets < numCheckBullets )
             {
                 // if the bullets are equal so far, keep going
                 if( strBullets.charAt(numEqualBullets) == m_genlistBulletBuffer.charAt(numEqualBullets))
                     numEqualBullets++;
                 // otherwise giveup, we have found how many are equal
                 else
                     break;
             }

             //unwind
             for( ; m_genlistlevel > numEqualBullets; m_genlistlevel-- )
             {
                 buf.append( m_renderer.closeList( m_genlistBulletBuffer.charAt(m_genlistlevel - 1) ) );
                 if( m_genlistlevel > 0 ) buf.append( m_renderer.closeListItem() );
             }

             //rewind
             buf.append( m_renderer.openList( strBullets.charAt(numEqualBullets++) ) );
             for(int i = numEqualBullets; i < numBullets; i++)
             {
                 buf.append( m_renderer.openListItem() );
                 buf.append( m_renderer.openList( strBullets.charAt(i) ) );
             }
             m_genlistlevel = numBullets;
         }
         buf.append( m_renderer.openListItem() );

         // work done, remember the new bullet list (in place of old one)
         m_genlistBulletBuffer.setLength(0);
         m_genlistBulletBuffer.append(strBullets);

         return buf.toString();
    }

    private String unwindGeneralList()
    {
        // String cStrShortName = "unwindGeneralList()";

        StringBuffer buf = new StringBuffer();
        int bulletCh;

        //unwind
        for( ; m_genlistlevel > 0; m_genlistlevel-- )
        {
            buf.append(m_renderer.closeListItem());
            buf.append( m_renderer.closeList( m_genlistBulletBuffer.charAt(m_genlistlevel - 1) ) );
        }

        m_genlistBulletBuffer.setLength(0);

        return buf.toString();
    }


    private String handleDefinitionList()
        throws IOException
    {
        if( !m_isdefinition )
        {
            m_isdefinition = true;

            m_closeTag = m_renderer.closeDefinitionItem()+m_renderer.closeDefinitionList();

            return startBlockLevel()+m_renderer.openDefinitionList()+m_renderer.openDefinitionTitle();
        }

        return ";";
    }

    private String handleOpenbracket()
        throws IOException
    {
        StringBuffer sb = new StringBuffer();
        int ch;
        boolean isPlugin = false;

        while( (ch = nextToken()) == '[' )
        {
            sb.append( (char)ch );
        }

        if( ch == '{' )
        {
            isPlugin = true;
        }

        pushBack( ch );

        if( sb.length() > 0 )
        {
            return sb.toString();
        }

        //
        //  Find end of hyperlink
        //

        ch = nextToken();

        while( ch != -1 )
        {
            if( ch == ']' && (!isPlugin || sb.charAt( sb.length()-1 ) == '}' ) )
            {
                break;
            }

            sb.append( (char) ch );

            ch = nextToken();
        }

        if( ch == -1 )
        {
            log.debug("Warning: unterminated link detected!");
            return sb.toString();
        }

        return handleHyperlinks( sb.toString() );
    }

    /**
     *  Reads the stream until the current brace is closed or stream end. 
     */
    private String readBraceContent( char opening, char closing )
        throws IOException
    {
        StringBuffer sb = new StringBuffer();
        int braceLevel = 1;    
        int ch;        
        while(( ch = nextToken() ) != -1 )
        {
            if( ch == '\\' ) 
            {
                continue;
            }
            else if ( ch == opening ) 
            {
                braceLevel++;
            }
            else if ( ch == closing ) 
            {
                braceLevel--;
                if (braceLevel==0) {
                  break;
                }
            }
            sb.append( (char)ch );
        }    
        return sb.toString();
    }

    /**
     *  Reads the stream until it meets one of the specified
     *  ending characters, or stream end.  The ending character will be left
     *  in the stream.
     */
    private String readUntil( String endChars )
        throws IOException
    {
        StringBuffer sb = new StringBuffer();
        int ch = nextToken();

        while( ch != -1 )
        {
            if( ch == '\\' ) 
            {
                ch = nextToken(); 
                if( ch == -1 ) 
                {
                    break;
                }
            }
            else
            {
                if( endChars.indexOf((char)ch) != -1 )
                {
                    pushBack( ch );
                    break;
                }
            }
            sb.append( (char) ch );
            ch = nextToken();
        }

        return sb.toString();
    }

    /**
     *  Reads the stream while the characters that have been specified are
     *  in the stream, returning then the result as a String.
     */
    private String readWhile( String endChars )
        throws IOException
    {
        StringBuffer sb = new StringBuffer();
        int ch = nextToken();

        while( ch != -1 )
        {
            if( endChars.indexOf((char)ch) == -1 )
            {
                pushBack( ch );
                break;
            }
            
            sb.append( (char) ch );
            ch = nextToken();
        }

        return sb.toString();
    }

    
    /**
     *  Handles constructs of type %%(style) and %%class
     * @param newLine
     * @return
     * @throws IOException
     */
    private String handleDiv( boolean newLine )
        throws IOException
    {
        int ch = nextToken();

        if( ch == '%' )
        {
            StringBuffer sb = new StringBuffer();

            String style = null;
            String clazz = null;

            ch = nextToken();

            boolean isspan = false;
            
            //
            //  Style or class?
            //
            if( ch == '(' )
            {                
                style = readBraceContent('(',')');
            }
            else if( Character.isLetter( (char) ch ) )
            {
                pushBack( ch );
                clazz = readUntil( " \t\n\r" );
                ch = nextToken();
                
                //
                //  Pop out only spaces, so that the upcoming EOL check does not check the
                //  next line.
                //
                if( ch == '\n' || ch == '\r' )
                {
                    pushBack(ch);
                }
            }
            else
            {
                //
                // Anything else stops.
                //

                pushBack(ch);
                
                try
                {
                    Boolean isSpan = (Boolean)m_styleStack.pop();
                
                    if( isSpan == null )
                    {
                        // Fail quietly
                    }
                    else if( isSpan.booleanValue() )
                    {
                        sb.append( m_renderer.closeSpan() );
                    }
                    else
                    {
                        sb.append( m_renderer.closeDiv() );
                    }
                }
                catch( EmptyStackException e )
                {
                    log.debug("Page '"+m_context.getPage().getName()+"' closes a %%-block that has not been opened.");
                }
                
                return sb.toString();
            }

            //
            //  Decide if we should open a div or a span?
            //
            String eol = peekAheadLine();
            
            if( eol.trim().length() > 0 )
            {
                // There is stuff after the class
                
                sb.append( m_renderer.openSpan( style, clazz ) );
                
                m_styleStack.push( Boolean.TRUE );
            }
            else
            {
                sb.append( startBlockLevel() );
                sb.append( m_renderer.openDiv( style, clazz ) );
                m_styleStack.push( Boolean.FALSE );
            }
            
            return sb.toString();
        }

        pushBack(ch);

        return "%";
    }

    private String handleBar( boolean newLine )
        throws IOException
    {
        StringBuffer sb = new StringBuffer();

        if( !m_istable && !newLine )
        {
            return "|";
        }

        if( newLine )
        {
            if( !m_istable )
            {
                sb.append( startBlockLevel() );
                sb.append( m_renderer.openTable() );
                m_istable = true;
            }

            sb.append( m_renderer.openTableRow() );
            m_closeTag = m_renderer.closeTableItem()+m_renderer.closeTableRow();
        }
        
        int ch = nextToken();

        if( ch == '|' )
        {
            if( !newLine ) 
            {
                sb.append( m_renderer.closeTableHeading() );
            }
            sb.append( m_renderer.openTableHeading() );
            m_closeTag = m_renderer.closeTableHeading()+m_renderer.closeTableRow();
        }
        else
        {
            if( !newLine ) 
            {
                sb.append( m_renderer.closeTableItem() );
            }
            sb.append( m_renderer.openTableItem() );
            pushBack( ch );
        }

        return sb.toString();
    }

    /**
     *  Generic escape of next character or entity.
     */
    private String handleTilde()
        throws IOException
    {
        int ch = nextToken();

        if( ch == '|' || ch == '~' || ch == '\\' || ch == '*' || ch == '#' || 
            ch == '-' || ch == '!' || ch == '\'' || ch == '_' || ch == '[' ||
            ch == '{' || ch == ']' || ch == '}' )
        {
            StringBuffer sb = new StringBuffer();
            sb.append( (char)ch );
            sb.append(readWhile( ""+(char)ch ));
            return sb.toString();
        }
        
        if( Character.isUpperCase( (char) ch ) )
        {
            return String.valueOf( (char)ch );
        }

        // No escape.
        pushBack( ch );

        return "~";
    }

    private void fillBuffer()
        throws IOException
    {
        StringBuffer buf = new StringBuffer();
        StringBuffer word = null;
        int previousCh = -2;
        int start = 0;
        
        boolean quitReading = false;
        boolean newLine     = true; // FIXME: not true if reading starts in middle of buffer

        while(!quitReading)
        {
            int ch = nextToken();
            String s = null;

            //
            //  Check if we're actually ending the preformatted mode.
            //  We still must do an entity transformation here.
            //
            if( m_isEscaping )
            {
                if( ch == '}' )
                {
                    buf.append( handleClosebrace() );
                }                
                else if( ch == -1 )
                {
                    quitReading = true;
                }
                else 
                {
                    m_renderer.doChar( buf, (char)ch );
                }

                continue;
            }

            //
            //  CamelCase detection, a non-trivial endeavour.
            //  We keep track of all white-space separated entities, which we
            //  hereby refer to as "words".  We then check for an existence
            //  of a CamelCase format text string inside the "word", and
            //  if one exists, we replace it with a proper link.
            //
            
            if( m_camelCaseLinks )
            {
                // Quick parse of start of a word boundary.

                if( word == null &&                    
                    (Character.isWhitespace( (char)previousCh ) ||
                     WORD_SEPARATORS.indexOf( (char)previousCh ) != -1 ||
                     newLine ) &&
                    !Character.isWhitespace( (char) ch ) )
                {
                    word = new StringBuffer();
                }

                // Are we currently tracking a word?
                if( word != null )
                {
                    //
                    //  Check for the end of the word.
                    //

                    if( Character.isWhitespace( (char)ch ) || 
                        ch == -1 ||
                        WORD_SEPARATORS.indexOf( (char) ch ) != -1 )
                    {
                        String potentialLink = word.toString();

                        String camelCase = checkForCamelCaseLink(potentialLink);

                        if( camelCase != null )
                        {
                            // System.out.println("Buffer is "+buf);

                            // System.out.println("  Replacing "+camelCase+" with proper link.");
                            start = buf.toString().lastIndexOf( camelCase );
                            buf.replace(start,
                                        start+camelCase.length(),
                                        makeCamelCaseLink(camelCase) );

                            // System.out.println("  Resulting with "+buf);
                        }
                        else
                        {
                            // System.out.println("Checking for potential URI: "+potentialLink);
                            if( isExternalLink( potentialLink ) )
                            {
                                // System.out.println("buf="+buf);
                                start = buf.toString().lastIndexOf( potentialLink );

                                if( start >= 0 )
                                {
                                    String link = readUntil(" \t()[]{}!\"'\n|");

                                    link = potentialLink + (char)ch + link; // Do not forget the start.

                                    // System.out.println("start="+start+", pl="+potentialLink);

                                    buf.replace( start,
                                                 start + potentialLink.length(),
                                                 makeDirectURILink( link ) );

                                    // System.out.println("Resulting with "+buf);

                                    ch = nextToken();
                                }
                            }
                        }

                        // We've ended a word boundary, so time to reset.
                        word = null;
                    }
                    else
                    {
                        // This should only be appending letters and digits.
                        word.append( (char)ch );
                    } // if end of word
                } // if word's not null

                // Always set the previous character to test for word starts.
                previousCh = ch;
		 
            } // if m_camelCaseLinks

            //
            //  An empty line stops a list
            //
            if( newLine && ch != '*' && ch != '#' && ch != ' ' && m_genlistlevel > 0 )
            {
                buf.append(unwindGeneralList());
            }

            if( newLine && ch != '|' && m_istable )
            {
                buf.append( m_renderer.closeTable() );
                m_istable = false;
                m_closeTag = null;
            }

            //
            //  Now, check the incoming token.
            //
            switch( ch )
            {
              case '\r':
                // DOS linefeeds we forget
                s = null;
                break;

              case '\n':
                //
                //  Close things like headings, etc.
                //
                if( m_closeTag != null ) 
                {
                    buf.append( m_closeTag );
                    m_closeTag = null;
                }

                m_isdefinition = false;

                if( newLine )
                {
                    // Paragraph change.
                    buf.append( startBlockLevel() );

                    //
                    //  Figure out which elements cannot be enclosed inside
                    //  a <p></p> pair according to XHTML rules.
                    //
                    String nextLine = peekAheadLine();
                    if( nextLine.length() == 0 || 
                        (nextLine.length() > 0 &&
                         !nextLine.startsWith("{{{") &&
                         !nextLine.startsWith("----") &&
                         !nextLine.startsWith("%%") &&
                         "*#!;".indexOf( nextLine.charAt(0) ) == -1) )
                    {
                        buf.append( m_renderer.openParagraph() );
                        m_isOpenParagraph = true;
                    }
                }
                else
                {
                    buf.append("\n");
                    newLine = true;
                }

                break;

              case '\\':
                s = handleBackslash();
                break;

              case '_':
                s = handleUnderscore();
                break;
                
              case '\'':
                s = handleApostrophe();
                break;

              case '{':
                s = handleOpenbrace( newLine );
                break;

              case '}':
                s = handleClosebrace();
                break;

              case '-':
                s = handleDash();
                break;

              case '!':
                if( newLine )
                {
                    s = handleHeading();
                }
                else
                {
                    s = "!";
                }
                break;

              case ';':
                if( newLine )
                {
                    s = handleDefinitionList();
                }
                else
                {
                    s = ";";
                }
                break;

              case ':':
                if( m_isdefinition )
                {
                    s = m_renderer.closeDefinitionTitle()+m_renderer.openDefinitionItem();
                    m_isdefinition = false;
                }
                else
                {
                    s = ":";
                }
                break;

              case '[':
                s = handleOpenbracket();
                break;

              case '*':
                if( newLine )
                {
                    pushBack('*');
                    s = handleGeneralList();
                }
                else
                {
                    s = "*";
                }
                break;

              case '#':
                if( newLine )
                {
                    pushBack('#');
                    s = handleGeneralList();
                }
                else
                {
                    s = "#";
                }
                break;

              case '|':
                s = handleBar( newLine );
                break;

              case '<':
                s = m_allowHTML ? "<" : "&lt;";
                break;

              case '>':
                s = m_allowHTML ? ">" : "&gt;";
                break;

              case '\"':
                s = m_allowHTML ? "\"" : "&quot;";
                break;

                /*
              case '&':
                s = "&amp;";
                break;
                */
              case '~':
                s = handleTilde();
                break;

              case '%':
                s = handleDiv( newLine );
                break;

              case -1:
                if( m_closeTag != null )
                {
                    buf.append( m_closeTag );
                    m_closeTag = null;
                }
                quitReading = true;
                break;

              default:
                buf.append( (char)ch );
                newLine = false;
                break;
            }

            if( s != null )
            {
                buf.append( s );
                newLine = false;
            }

         }

        m_data = new StringReader( buf.toString() );
    }


    public int read()
        throws IOException
    {
        int val = m_data.read();

        if( val == -1 )
        {
            fillBuffer();
            val = m_data.read();

            if( val == -1 )
            {
                m_data = new StringReader( closeAll() );

                val = m_data.read();
            }
        }

        return val;
    }

    public int read( char[] buf, int off, int len )
        throws IOException
    {
        return m_data.read( buf, off, len );
    }

    public boolean ready()
        throws IOException
    {
        log.debug("ready ? "+m_data.ready() );
        if(!m_data.ready())
        {
            fillBuffer();
        }

        return m_data.ready();
    }

    public void close()
    {
    }

    /**
     *  All HTML output stuff is here.  This class is a helper class, and will
     *  be spawned later on with a proper API of its own so that we can have
     *  different kinds of renderers.
     */

    // FIXME: Not everything is yet, and in the future this class will be spawned
    //        out to be its own class.
    private class HTMLRenderer
        extends TextRenderer
    {
        private boolean m_isPreBlock = false;
        private TranslatorReader m_cleanTranslator;

        /*
           FIXME: It's relatively slow to create two TranslatorReaders each time.
        */
        public HTMLRenderer()
        {
        }

        /**
         *  Does a lazy init.  Otherwise, we would get into a situation
         *  where HTMLRenderer would try and boot a TranslatorReader before
         *  the TranslatorReader it is contained by is up.
         */
        private TranslatorReader getCleanTranslator()
        {
            if( m_cleanTranslator == null )
            {
                WikiContext dummyContext = new WikiContext( m_engine, 
                                                            m_context.getPage() );
                m_cleanTranslator = new TranslatorReader( dummyContext, 
                                                          null,
                                                          new TextRenderer() );
                m_cleanTranslator.m_allowHTML = true;
            }

            return m_cleanTranslator;
        }

        public void doChar( StringBuffer buf, char ch )
        {
            if( ch == '<' )
            {
                buf.append("&lt;");
            }
            else if( ch == '>' )
            {
                buf.append("&gt;");
            }
            else if( ch == '&' )
            {
                buf.append("&amp;");
            }
            else
            {
                buf.append( ch );
            }
        }

        public String openDiv( String style, String clazz )
        {
            StringBuffer sb = new StringBuffer();

            sb.append( "<div" );
            sb.append( style != null ? " style=\""+style+"\"" : "" );
            sb.append( clazz != null ? " class=\""+clazz+"\"" : "" );
            sb.append( ">" );

            return sb.toString();
        }

        public String openSpan( String style, String clazz )
        {
            StringBuffer sb = new StringBuffer();

            sb.append( "<span" );
            sb.append( style != null ? " style=\""+style+"\"" : "" );
            sb.append( clazz != null ? " class=\""+clazz+"\"" : "" );
            sb.append( ">" );

            return sb.toString();
        }

        public String closeDiv()
        {
            return "</div>";
        }

        public String closeSpan()
        {
            return "</span>";
        }
        
        public String openParagraph()
        {
            return "<p>";
        }

        public String closeParagraph()
        {
            return "</p>\n";
        }

        /**
         *  Writes out a text effect
         */
        public String openTextEffect( int effect )
        {
            switch( effect )
            {
              case BOLD:
                return "<b>";
              case ITALIC:
                return "<i>";
              case TYPED:
                return "<tt>";
            }

            return "";
        }

        public String closeTextEffect( int effect )
        {
            switch( effect )
            {
              case BOLD:
                return "</b>";
              case ITALIC:
                return "</i>";
              case TYPED:
                return "</tt>";
            }

            return "";
        }

        public String openDefinitionItem()
        {
            return "<dd>";
        }

        public String closeDefinitionItem()
        {
            return "</dd>\n";
        }

        public String openDefinitionTitle()
        {
            return "<dt>";
        }
        public String closeDefinitionTitle()
        {
            return "</dt>";
        }

        public String openDefinitionList()
        {
            return "<dl>\n";
        }

        public String closeDefinitionList()
        {
            return "</dl>";
        }
        

        /**
         *  Write a HTMLized link depending on its type.
         *
         *  <p>This jsut calls makeLink() with "section" set to null.
         */
        public String makeLink( int type, String link, String text )
        {
            return makeLink( type, link, text, null );
        }

        private final String getURL( String context, String link )
        {
            return m_context.getURL( context,
                                     link,
                                     null );
        }

        /**
         *  Write a HTMLized link depending on its type.
         *
         *  @param type Type of the link.
         *  @param link The actual link.
         *  @param text The user-visible text for the link.
         *  @param section Which named anchor to point to.  This may not have any
         *         effect on certain link types.  If null, will ignore it.
         */
        public String makeLink( int type, String link, String text, String section )
        {
            String result;

            if( text == null ) text = link;

            section = (section != null) ? ("#"+section) : "";

            // Make sure we make a link name that can be accepted
            // as a valid URL.

            String encodedlink = m_engine.encodeName( link );

            if( encodedlink.length() == 0 )
            {
                type = EMPTY;
            }

            switch(type)
            {
              case READ:
                result = "<a class=\"wikipage\" href=\""+getURL(WikiContext.VIEW,
                                                                link)+section+"\">"+text+"</a>";
                break;

              case EDIT:
                result = "<a class=\"editpage\" title=\"Create '"+link+"'\" href=\""+
                		  getURL(WikiContext.EDIT, link)+"\">"+
                		  text+"</a>";
                break;

              case EMPTY:
                result = "<u>"+text+"</u>";
                break;

                //
                //  These two are for local references - footnotes and 
                //  references to footnotes.
                //  We embed the page name (or whatever WikiContext gives us)
                //  to make sure the links are unique across Wiki.
                //
              case LOCALREF:
                result = "<a class=\"footnoteref\" href=\"#ref-"+
                m_context.getPage().getName()+"-"+
                link+"\">["+text+"]</a>";
                break;

              case LOCAL:
                result = "<a class=\"footnote\" name=\"ref-"+
                m_context.getPage().getName()+"-"+
                link.substring(1)+"\">["+text+"]</a>";
                break;

                //
                //  With the image, external and interwiki types we need to
                //  make sure nobody can put in Javascript or something else
                //  annoying into the links themselves.  We do this by preventing
                //  a haxor from stopping the link name short with quotes in 
                //  fillBuffer().
                //
              case IMAGE:
                result = "<img class=\"inline\" src=\""+link+"\" alt=\""+text+"\" />";
                break;

              case IMAGELINK:
                result = "<a href=\""+text+"\"><img class=\"inline\" src=\""+link+"\" alt=\""+text+"\"/></a>";
                break;

              case IMAGEWIKILINK:
                String pagelink = getURL(WikiContext.VIEW,text);
                result = "<a class=\"wikipage\" href=\""+pagelink+"\"><img class=\"inline\" src=\""+link+"\" alt=\""+text+"\" /></a>";
                break;

              case EXTERNAL:
                result = "<a class=\"external\" "+
                         (m_useRelNofollow ? "rel=\"nofollow\" " : "")+
                         "href=\""+link+section+"\">"+text+"</a>";
                break;
                
              case INTERWIKI:
                result = "<a class=\"interwiki\" href=\""+link+section+"\">"+text+"</a>";
                break;

              case ATTACHMENT:
                String attlink = getURL( WikiContext.ATTACH,
                                         link );

                String infolink = getURL( WikiContext.INFO,
                                          link );

                String imglink = getURL( WikiContext.NONE,
                                         "images/attachment_small.png" );

                result = "<a class=\"attachment\" href=\""+attlink+"\">"+text+"</a>"+
                         "<a href=\""+infolink+
                         "\"><img src=\""+imglink+"\" border=\"0\" alt=\"(info)\"/></a>";
                break;

              default:
                result = "";
                break;
            }

            return result;
        }

        /**
         *  Writes HTML for error message.
         */

        public String makeError( String error )
        {
            return "<span class=\"error\">"+error+"</span>";
        }

        /**
         *  Emits a vertical line.
         */

        public String makeRuler()
        {
            return "<hr />";
        }

        /**
         *  Modifies the "hd" parameter to contain proper values.  Because
         *  an "id" tag may only contain [a-zA-Z0-9:_-], we'll replace the
         *  % after url encoding with '_'.
         */
        private String makeHeadingAnchor( String baseName, String title, Heading hd )
        {
            hd.m_titleText = title;
            title = cleanLink( title );
            hd.m_titleSection = m_engine.encodeName(title);
            hd.m_titleAnchor = "section-"+m_engine.encodeName(baseName)+
                               "-"+hd.m_titleSection;
            
            hd.m_titleAnchor = hd.m_titleAnchor.replace( '%', '_' );
            return hd.m_titleAnchor;
        }

        private String makeSectionTitle( String title )
        {
            title = title.trim();

            StringWriter outTitle = new StringWriter();

            try
            {
                TranslatorReader read = getCleanTranslator();
                read.setInputReader( new StringReader(title) );
                FileUtil.copyContents( read, outTitle );
            }
            catch( IOException e )
            {
                log.fatal("CleanTranslator not working", e);
                throw new InternalWikiException("CleanTranslator not working as expected, when cleaning title"+ e.getMessage() );
            }

            return outTitle.toString();
        }
        
        /**
         *  Returns XHTML for the start of the heading.  Also sets the
         *  line-end emitter.
         *  @param level 
         *  @param headings A List to which heading should be added.
         */ 
        public String makeHeading( int level, String title, Heading hd )
        {
            String res = "";

            String pageName = m_context.getPage().getName();

            String outTitle = makeSectionTitle( title );

            hd.m_level = level;

            switch( level )
            {
              case Heading.HEADING_SMALL:
                res = "<h4 id='"+makeHeadingAnchor( pageName, outTitle, hd )+"'>";
                m_closeTag = "</h4>";
                break;

              case Heading.HEADING_MEDIUM:
                res = "<h3 id='"+makeHeadingAnchor( pageName, outTitle, hd )+"'>";
                m_closeTag = "</h3>";
                break;

              case Heading.HEADING_LARGE:
                res = "<h2 id='"+makeHeadingAnchor( pageName, outTitle, hd )+"'>";
                m_closeTag = "</h2>";
                break;
            }

            return res;
        }

        /**
         *  @param bullet A character detailing which kind of a list
         *  we are dealing with here.  Options are '#' and '*'.
         */
        public String openList( char bullet )
        {
            String res = "";

            if( bullet == '#' )
                res = "<ol>\n";
            else if( bullet == '*' )
                res = "<ul>\n";
            else
                log.info("Warning: unknown bullet character '" + bullet + "' at (+)" );

            return res;
        }

        public String openListItem()
        {
            return "<li>";
        }

        public String closeListItem()
        {
            return "</li>\n";
        }

        /**
         *  @param bullet A character detailing which kind of a list
         *  we are dealing with here.  Options are '#' and '*'.
         */
        public String closeList( char bullet )
        {
            String res = "";

            if( bullet == '#' )
            {
                res = "</ol>\n";
            }
            else if( bullet == '*' )
            {
                res = "</ul>\n";
            }
            else
            {
                //FIXME unknown character -> error
                log.info("Warning: unknown character in unwind '" + bullet + "'" );
            }

            return res;
        }

        public String openTable()
        {
            return "<table class=\"wikitable\" border=\"1\">\n";
        }

        public String closeTable()
        {
            return "</table>\n";
        }

        public String openTableRow()
        {
            return "<tr>";
        }

        public String closeTableRow()
        {
            return "</tr>";
        }

        public String openTableItem()
        {
            return "<td>";
        }

        public String closeTableItem()
        {
            return "</td>";
        }

        public String openTableHeading()
        {
            return "<th>";
        }

        public String closeTableHeading()
        {
            return "</th>";
        }

        public String openPreformatted( boolean isBlock )
        {
            m_isPreBlock = isBlock;

            if( isBlock )
            {
                return "<pre>";
            }
            
            return "<span style=\"font-family:monospace; whitespace:pre;\">";
        }

        public String closePreformatted()
        {
            if( m_isPreBlock )
                return "</pre>\n";
            
            return "</span>";
        }

        /**
         *  If outlink images are turned on, returns a link to the outward
         *  linking image.
         */
        public String outlinkImage()
        {
            if( m_useOutlinkImage )
            {
                return "<img class=\"outlink\" src=\""+
                       getURL( WikiContext.NONE,"images/out.png" )+"\" alt=\"\" />";
            }

            return "";
        }

        /**
         *  @param clear If true, then flushes all thingies.
         */
        public String lineBreak( boolean clear )
        {
            if( clear )
                return "<br clear=\"all\" />";
            
            return "<br />";
        }

    } // HTMLRenderer

    /**
     *  A very simple class for outputting plain text with no
     *  formatting.
     */
    private class TextRenderer
    {
        public void doChar( StringBuffer buf, char ch )
        {
            buf.append( ch );
        }

        public String openDiv( String style, String clazz )
        {
            return "";
        }

        public String closeDiv()
        {
            return "";
        }

        public String openSpan( String style, String clazz )
        {
            return "";
        }

        public String closeSpan()
        {
            return "";
        }

        public String openParagraph()
        {
            return "";
        }

        public String closeParagraph()
        {
            return "\n\n";
        }

        /**
         *  Writes out a text effect
         */
        public String openTextEffect( int effect )
        {
            return "";
        }

        public String closeTextEffect( int effect )
        {
            return "";
        }

        public String openDefinitionItem()
        {
            return " : ";
        }

        public String closeDefinitionItem()
        {
            return "\n";
        }

        public String openDefinitionTitle()
        {
            return "";
        }

        public String closeDefinitionTitle()
        {
            return "";
        }

        public String openDefinitionList()
        {
            return "";
        }

        public String closeDefinitionList()
        {
            return "\n";
        }
        

        /**
         *  Write a HTMLized link depending on its type.
         *
         *  <p>This jsut calls makeLink() with "section" set to null.
         */
        public String makeLink( int type, String link, String text )
        {
            return text;
        }

        public String makeLink( int type, String link, String text, String section )
        {
            return text;
        }

        /**
         *  Writes HTML for error message.
         */

        public String makeError( String error )
        {
            return "ERROR: "+error;
        }

        /**
         *  Emits a vertical line.
         */

        public String makeRuler()
        {
            return "----------------------------------";
        }

        /**
         *  Returns XHTML for the start of the heading.  Also sets the
         *  line-end emitter.
         *  @param level 
         */ 
        public String makeHeading( int level, String title, Heading hd )
        {
            String res = "";

            String pageName = m_context.getPage().getName();

            title = title.trim();

            hd.m_level = level;
            hd.m_titleText = title;
            hd.m_titleSection = "";
            hd.m_titleAnchor = "";

            switch( level )
            {
              case Heading.HEADING_SMALL:
                res = title;
                m_closeTag = "\n\n";
                break;

              case Heading.HEADING_MEDIUM:
                res = title;                
                m_closeTag = "\n"+TextUtil.repeatString("-",title.length())+"\n\n";
                break;

              case Heading.HEADING_LARGE:
                res = title.toUpperCase();
                m_closeTag= "\n"+TextUtil.repeatString("=",title.length())+"\n\n";
                break;
            }

            return res;
        }

        /**
         *  @param bullet A character detailing which kind of a list
         *  we are dealing with here.  Options are '#' and '*'.
         */
        // FIXME: Should really start a different kind of list depending
        //        on the bullet type
        public String openList( char bullet )
        {
            return "\n";
        }

        public String openListItem()
        {
            return "- "; 
        }

        public String closeListItem()
        {
            return "\n";
        }

        /**
         *  @param bullet A character detailing which kind of a list
         *  we are dealing with here.  Options are '#' and '*'.
         */
        public String closeList( char bullet )
        {
            return "\n\n";
        }

        public String openTable()
        {
            return "\n";
        }

        public String closeTable()
        {
            return "\n";
        }

        public String openTableRow()
        {
            return "";
        }

        public String closeTableRow()
        {
            return "\n";
        }

        public String openTableItem()
        {
            return "\t";
        }

        public String closeTableItem()
        {
            return "";
        }

        public String openTableHeading()
        {
            return "\t";
        }

        public String closeTableHeading()
        {
            return "";
        }

        public String openPreformatted( boolean isBlock )
        {
            return "";
        }

        public String closePreformatted()
        {
            return "\n";
        }

        /**
         *  If outlink images are turned on, returns a link to the outward
         *  linking image.
         */
        public String outlinkImage()
        {
            return "";
        }

        /**
         *  @param clear If true, then flushes all thingies.
         */
        public String lineBreak( boolean clear )
        {
            return "\n";
        }

    } // TextRenderer

    /**
     *  This class is used to store the headings in a manner which
     *  allow the building of a Table Of Contents.
     */
    public class Heading
    {
        public static final int HEADING_SMALL  = 1;
        public static final int HEADING_MEDIUM = 2;
        public static final int HEADING_LARGE  = 3;

        public int    m_level;
        public String m_titleText;
        public String m_titleAnchor;
        public String m_titleSection;
    }
}
