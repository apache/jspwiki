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
package org.apache.wiki.parser;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Result;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wiki.InternalWikiException;
import org.apache.wiki.VariableManager;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.PluginException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.acl.Acl;
import org.apache.wiki.content.ContentManager;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.plugin.PluginManager;
import org.apache.wiki.plugin.WikiPlugin;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.render.CleanTextRenderer;
import org.apache.wiki.render.RenderingManager;
import org.apache.wiki.util.RegExpUtil;
import org.apache.wiki.util.StringTransmutator;
import org.apache.wiki.util.TextUtil;
import org.jdom.*;


/**
 *  Parses JSPWiki-style markup into a WikiDocument DOM tree.  This class is the
 *  heart and soul of JSPWiki : make sure you test properly anything that is added,
 *  or else it breaks down horribly.
 *
 *  @since  2.4
 */
public class JSPWikiMarkupParser
    extends MarkupParser
{
    /** Name of the outlink image; relative path to the JSPWiki directory. */
    private static final String OUTLINK_IMAGE = "images/out.png";

    /** The value for anchor element <tt>class</tt> attributes when used
      * for wiki page (normal) links. The value is "wikipage". */
    public static final String CLASS_WIKIPAGE = "wikipage";

    /** The value for anchor element <tt>class</tt> attributes when used
      * for edit page links. The value is "createpage". */
    public static final String CLASS_EDITPAGE = "createpage";

    /** The value for anchor element <tt>class</tt> attributes when used
      * for interwiki page links. The value is "interwiki". */
    public static final String CLASS_INTERWIKI = "interwiki";

    protected static final int              READ          = 0;
    protected static final int              EDIT          = 1;
    protected static final int              EMPTY         = 2;  // Empty message
    protected static final int              LOCAL         = 3;
    protected static final int              LOCALREF      = 4;
    protected static final int              IMAGE         = 5;
    protected static final int              EXTERNAL      = 6;
    protected static final int              INTERWIKI     = 7;
    protected static final int              IMAGELINK     = 8;
    protected static final int              IMAGEWIKILINK = 9;
    protected static final int              ATTACHMENT    = 10;

    private static Logger log = LoggerFactory.getLogger( JSPWikiMarkupParser.class );

    private boolean        m_isbold       = false;
    private boolean        m_isitalic     = false;
    private boolean        m_istable      = false;
    private boolean        m_isPre        = false;
    private boolean        m_isEscaping   = false;
    private boolean        m_isdefinition = false;
    private boolean        m_isPreBlock   = false;

    /** Contains style information, in multiple forms. */
    private Stack<Boolean> m_styleStack   = new Stack<Boolean>();

     // general list handling
    private int            m_genlistlevel = 0;
    private StringBuilder  m_genlistBulletBuffer = new StringBuilder(10);  // stores the # and * pattern
    private boolean        m_allowPHPWikiStyleLists = true;


    private boolean        m_isOpenParagraph = false;

    /** Keeps image regexp Patterns */
    private List<Pattern>  m_inlineImagePatterns;

    /** Parser for extended link functionality. */
    private LinkParser     m_linkParser = new LinkParser();

    /** Keeps track of any plain text that gets put in the Text nodes */
    private StringBuilder  m_plainTextBuf = new StringBuilder(20);

    private Element        m_currentElement;

    /** Keep track of duplicate header names.  */
    private Map<String, Integer>   m_titleSectionCounter = new HashMap<String, Integer>();

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

    /** If true, all outward attachment info links have a small link image appended. */
    public static final String     PROP_USEATTACHMENTIMAGE = "jspwiki.translatorReader.useAttachmentImage";

    /** If set to "true", all external links are tagged with 'rel="nofollow"' */
    public static final String     PROP_USERELNOFOLLOW   = "jspwiki.translatorReader.useRelNofollow";

    /** If true, then considers CamelCase links as well. */
    private boolean                m_camelCaseLinks      = false;

    /** If true, then generate special output for wysiwyg editing in certain cases */
    private boolean                m_wysiwygEditorMode     = false;

    /** If true, consider URIs that have no brackets as well. */
    // FIXME: Currently reserved, but not used.
    private boolean                m_plainUris           = false;

    /** If true, all outward links use a small link image. */
    private boolean                m_useOutlinkImage     = true;

    private boolean                m_useAttachmentImage  = true;

    /** If true, allows raw HTML. */
    private boolean                m_allowHTML           = false;

    private boolean                m_useRelNofollow      = false;

//    static final String WIKIWORD_REGEX = "(^|[[:^alnum:]]+)([[:upper:]]+[[:lower:]]+[[:upper:]]+[[:alnum:]]*|(http://|https://|mailto:)([A-Za-z0-9_/\\.\\+\\?\\#\\-\\@=&;~%]+))";
    private static final String    ALNUM = "\\p{javaLetterOrDigit}";
    private static final String    NOT_ALNUM = "\\P{javaLetterOrDigit}";
    private static final String    UPPER = "\\p{Lu}";
    private static final String    LOWER = "\\p{Ll}";
    
    static final String WIKIWORD_REGEX = "(^|["+NOT_ALNUM+"]+)("+
                                         UPPER+"+"
                                         +LOWER+"+"
                                         +UPPER+"+"
                                         +ALNUM+"*"
                                         +"|(http://|https://|mailto:)([A-Za-z0-9_/\\.\\+\\?\\#\\-\\@=&;~%]+))";

    private Pattern                m_camelCasePattern;

    private int                    m_rowNum              = 1;

    private Heading                m_lastHeading         = null;
    
    /**
     *  The default inlining pattern.  Currently "*.png"
     */
    public static final String     DEFAULT_INLINEPATTERN = "*.png";

    /**
     *  This list contains all IANA registered URI protocol
     *  types as of September 2004 + a few well-known extra types.
     *
     *  JSPWiki recognises all of them as external links.
     *
     *  This array is sorted during class load, so you can just dump
     *  here whatever you want in whatever order you want.
     */
    static final String[] EXTERNAL_LINKS = {
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

    private static final String INLINE_IMAGE_PATTERNS = "JSPWikiMarkupParser.inlineImagePatterns";

    private static final String CAMELCASE_PATTERN     = "JSPWikiMarkupParser.camelCasePattern";

    private static final String[] CLASS_TYPES =
    {
       CLASS_WIKIPAGE,
       CLASS_EDITPAGE,
       "",
       "footnote",
       "footnoteref",
       "",
       "external",
       CLASS_INTERWIKI,
       "external",
       CLASS_WIKIPAGE,
       "attachment"
    };


    /**
     *  This Comparator is used to find an external link from c_externalLinks.  It
     *  checks if the link starts with the other arraythingie.
     */
    private static Comparator<String> c_startingComparator = new StartingComparator();

    static
    {
        Arrays.sort( EXTERNAL_LINKS );
    }

    /**
     *  Creates a markup parser.
     *  
     *  @param context The WikiContext which controls the parsing
     *  @param in Where the data is read from.
     */
    public JSPWikiMarkupParser( WikiContext context, Reader in )
    {
        super( context, in );
        initialize();
    }

    /**
     *  @param m_engine The WikiEngine this reader is attached to.  Is
     * used to figure out of a page exits.
     */

    // FIXME: parsers should be pooled for better performance.
    @SuppressWarnings("unchecked")
    private void initialize()
    {
        List<Pattern>   compiledpatterns;

        //
        //  We cache compiled patterns in the engine, since their creation is
        //  really expensive
        //
        compiledpatterns = (List<Pattern>)m_engine.getAttribute( INLINE_IMAGE_PATTERNS );

        if( compiledpatterns == null )
        {
            compiledpatterns = new ArrayList<Pattern>(20);
            Collection ptrns = getImagePatterns( m_engine );

            //
            //  Make them into Regexp Patterns.  Unknown patterns
            //  are ignored.
            //
            for( Iterator i = ptrns.iterator(); i.hasNext(); )
            {
                try
                {
                    compiledpatterns.add( Pattern.compile( RegExpUtil
                        .globToPerl5( ((String) i.next()).toCharArray(), RegExpUtil.DEFAULT_MASK|RegExpUtil.READ_ONLY_MASK ) ) );
                }
                catch( PatternSyntaxException e )
                {
                    log.error("Malformed pattern in properties: ", e );
                }
            }

            m_engine.setAttribute( INLINE_IMAGE_PATTERNS, compiledpatterns );
        }

        m_inlineImagePatterns = Collections.unmodifiableList(compiledpatterns);

        m_camelCasePattern = (Pattern) m_engine.getAttribute( CAMELCASE_PATTERN );
        if( m_camelCasePattern == null )
        {
            try
            {
                m_camelCasePattern = Pattern.compile( WIKIWORD_REGEX, RegExpUtil.DEFAULT_MASK | RegExpUtil.READ_ONLY_MASK );
            }
            catch( PatternSyntaxException e )
            {
                log.error("Internal error: Someone put in a faulty pattern.",e);
                throw new InternalWikiException("Faulty camelcasepattern in TranslatorReader");
            }
            m_engine.setAttribute( CAMELCASE_PATTERN, m_camelCasePattern );
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



        Boolean wysiwygVariable = (Boolean)m_context.getVariable( RenderingManager.WYSIWYG_EDITOR_MODE );
        if( wysiwygVariable != null )
        {
            m_wysiwygEditorMode = wysiwygVariable.booleanValue();
        }

        m_plainUris           = getLocalBooleanProperty( m_context,
                                                         props,
                                                         PROP_PLAINURIS,
                                                         m_plainUris );
        m_useOutlinkImage     = getLocalBooleanProperty( m_context,
                                                         props,
                                                         PROP_USEOUTLINKIMAGE,
                                                         m_useOutlinkImage );
        m_useAttachmentImage  = getLocalBooleanProperty( m_context,
                                                         props,
                                                         PROP_USEATTACHMENTIMAGE,
                                                         m_useAttachmentImage );
        m_allowHTML           = getLocalBooleanProperty( m_context,
                                                         props,
                                                         MarkupParser.PROP_ALLOWHTML,
                                                         m_allowHTML );

        m_useRelNofollow      = getLocalBooleanProperty( m_context,
                                                         props,
                                                         PROP_USERELNOFOLLOW,
                                                         m_useRelNofollow );

        if( m_engine.getUserManager().getUserDatabase() == null || m_engine.getAuthorizationManager() == null )
        {
            disableAccessRules();
        }
    }

    /**
     *  This is just a simple helper method which will first check the context
     *  if there is already an override in place, and if there is not,
     *  it will then check the given properties.
     *
     *  @param context WikiContext to check first
     *  @param props   Properties to check next
     *  @param key     What key are we searching for?
     *  @param defValue Default value for the boolean
     *  @return True or false
     */
    private static boolean getLocalBooleanProperty( WikiContext context,
                                                    Properties  props,
                                                    String      key,
                                                    boolean     defValue )
    {
        Object bool = context.getVariable(key);

        if( bool != null )
        {
            return TextUtil.isPositive( (String) bool );
        }

        return TextUtil.getBooleanProperty( props, key, defValue );
    }

    /**
     *  Figure out which image suffixes should be inlined.
     *  @return Collection of Strings with patterns.
     *  
     *  @param engine The WikiEngine from which the patterns are read.
     */

    // FIXME: Does not belong here; should be elsewhere
    public static Collection<String> getImagePatterns( WikiEngine engine )
    {
        Properties props    = engine.getWikiProperties();
        ArrayList<String>  ptrnlist = new ArrayList<String>();

        for( Enumeration<?> e = props.propertyNames(); e.hasMoreElements(); )
        {
            String name = (String) e.nextElement();

            if( name.startsWith( PROP_INLINEIMAGEPTRN ) )
            {
                String ptrn = TextUtil.getStringProperty( props, name, null );

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

            WikiPath path = WikiPath.valueOf( page );
            WikiPath finalPath = m_engine.getFinalPageName( path );
            path = finalPath != null ? finalPath : path;
            if ( m_engine.pageExists( path.toString() ) )
            {
                String pageLink = ContentManager.DEFAULT_SPACE.equals( path.getSpace() ) ? path.getPath() : path.toString();
                return pageLink;
            }
            return null;
        }
        catch( Exception e )
        {
            // FIXME: This should be completely different!
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

    protected String callMutatorChain( Collection<StringTransmutator> list, String text )
    {
        if( list == null || list.size() == 0 )
        {
            return text;
        }

        for( StringTransmutator m : list )
        {
            text = m.mutate( m_context, text );
        }

        return text;
    }

    /**
     * Calls the heading listeners.
     *
     * @param param A Heading object.
     */
    protected void callHeadingListenerChain( Heading param )
    {
        List<HeadingListener> list = m_headingListenerChain;
        for( HeadingListener h : list )
        {
            h.headingAdded( m_context, param );
        }
    }

    /**
     *  Creates a JDOM anchor element.  Can be overridden to change the URL creation,
     *  if you really know what you are doing.
     *
     *  @param type One of the types above
     *  @param link URL to which to link to
     *  @param text Link text
     *  @param section If a particular section identifier is required.
     *  @return An A element.
     *  @since 2.4.78
     */
    protected Element createAnchor(int type, String link, String text, String section)
    {
        text = escapeHTMLEntities( text );
        section = escapeHTMLEntities( section );
        Element el = new Element("a");
        el.setAttribute("class",CLASS_TYPES[type]);
        el.setAttribute("href",link+section);
        el.addContent(text);
        return el;
    }

    private Element makeLink( int type, String link, String text, String section, Iterator<Attribute> attributes )
    {
        Element el = null;

        if( text == null ) text = link;

        text = callMutatorChain( m_linkMutators, text );

        section = (section != null) ? ("#"+section) : "";

        // Make sure we make a link name that can be accepted
        // as a valid URL.

        if( link.length() == 0 )
        {
            type = EMPTY;
        }
        ResourceBundle rb = m_context.getBundle(InternationalizationManager.CORE_BUNDLE);
        Object[] args = { link };

        switch(type)
        {
            case READ:
                el = createAnchor( READ, m_context.getURL(WikiContext.VIEW, link), text, section );
                break;

            case EDIT:
                el = createAnchor( EDIT, m_context.getURL(WikiContext.EDIT,link), text, "" );
                el.setAttribute("title", MessageFormat.format( rb.getString( "markupparser.link.create" ), args ) );
                break;

            case EMPTY:
                el = new Element("u").addContent(text);
                break;

                //
                //  These two are for local references - footnotes and
                //  references to footnotes.
                //  We embed the page name (or whatever WikiContext gives us)
                //  to make sure the links are unique across Wiki.
                //
            case LOCALREF:
                el = createAnchor( LOCALREF, "#ref-"+m_context.getPage().getName()+"-"+link, "["+text+"]", "" );
                break;

            case LOCAL:
                el = new Element("a").setAttribute("class","footnote");
                el.setAttribute("name", "ref-"+m_context.getPage().getName()+"-"+link.substring(1));
                el.addContent("["+text+"]");
                break;

                //
                //  With the image, external and interwiki types we need to
                //  make sure nobody can put in Javascript or something else
                //  annoying into the links themselves.  We do this by preventing
                //  a haxor from stopping the link name short with quotes in
                //  fillBuffer().
                //
            case IMAGE:
                el = new Element("img").setAttribute("class","inline");
                el.setAttribute("src",link);
                el.setAttribute("alt",text);
                break;

            case IMAGELINK:
                el = new Element("img").setAttribute("class","inline");
                el.setAttribute("src",link);
                el.setAttribute("alt",text);
                el = createAnchor(IMAGELINK,text,"","").addContent(el);
                break;

            case IMAGEWIKILINK:
                String pagelink = m_context.getURL(WikiContext.VIEW,text);
                el = new Element("img").setAttribute("class","inline");
                el.setAttribute("src",link);
                el.setAttribute("alt",text);
                el = createAnchor(IMAGEWIKILINK,pagelink,"","").addContent(el);
                break;

            case EXTERNAL:
                el = createAnchor( EXTERNAL, link, text, section );
                if( m_useRelNofollow ) el.setAttribute("rel","nofollow");
                break;

            case INTERWIKI:
                el = createAnchor( INTERWIKI, link, text, section );
                break;

            case ATTACHMENT:
                String attlink = m_context.getURL( WikiContext.ATTACH,
                                                   link );

                String infolink = m_context.getURL( WikiContext.INFO,
                                                    link );

                String imglink = m_context.getURL( WikiContext.NONE,
                                                   "images/attachment_small.png" );

                el = createAnchor( ATTACHMENT, attlink, text, "" );

                pushElement(el);
                popElement(el.getName());

                if( m_useAttachmentImage )
                {
                    el = new Element("img").setAttribute("src",imglink);
                    el.setAttribute("border","0");
                    el.setAttribute("alt","(info)");

                    el = new Element("a").setAttribute("href",infolink).addContent(el);
                    el.setAttribute("class","infolink");
                }
                else
                {
                    el = null;
                }
                break;

            default:
                break;
        }

        if( el != null && attributes != null )
        {
            while( attributes.hasNext() )
            {
                Attribute attr = attributes.next();
                if( attr != null )
                {
                    el.setAttribute(attr);
                }
            }
        }

        if( el != null )
        {
            flushPlainText();
            m_currentElement.addContent( el );
        }
        return el;
    }


    /**
     *  Figures out if a link is an off-site link.  This recognizes
     *  the most common protocols by checking how it starts.
     *
     *  @param link The link to check.
     *  @return true, if this is a link outside of this wiki.
     *  @since 2.4
     */

    public static boolean isExternalLink( String link )
    {
        int idx = Arrays.binarySearch( EXTERNAL_LINKS, link,
                                       c_startingComparator );

        //
        //  We need to check here once again; otherwise we might
        //  get a match for something like "h".
        //
        if( idx >= 0 && link.startsWith(EXTERNAL_LINKS[idx]) ) return true;

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
     * Returns <code>true</code> if the link passed contains
     * a colon, and the contents to the left of the colon matches
     * one of the configured wiki spaces as returned by
     * {@link WikiEngine#getSpaces()}.
     * @param link the link to examine
     * @return the result
     */
    private boolean isSpaceLink( String link )
    {
        int colonPos = link.indexOf( ':' );
        if ( colonPos != -1 )
        {
            String possibleSpace = link.substring( 0, colonPos );
            for ( String space : m_engine.getSpaces() )
            {
                if ( space.equals( possibleSpace ) )
                {
                    return true;
                }
            }
        }
        return false;
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
            link = link.toLowerCase();

            for( Pattern pattern : m_inlineImagePatterns )
            {
                Matcher matcher = pattern.matcher( link );
                if( matcher.matches() )
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
     *  These are all of the HTML 4.01 block-level elements.
     */
    private static final String[] BLOCK_ELEMENTS = {
        "address", "blockquote", "div", "dl", "fieldset", "form",
        "h1", "h2", "h3", "h4", "h5", "h6",
        "hr", "noscript", "ol", "p", "pre", "table", "ul"
    };

    private static final boolean isBlockLevel( String name )
    {
        return Arrays.binarySearch( BLOCK_ELEMENTS, name ) >= 0;
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

        if( s.length() > PUSHBACK_BUFFER_SIZE )
        {
            log.warn("Line is longer than maximum allowed size ("+PUSHBACK_BUFFER_SIZE+" characters.  Attempting to recover...");
            pushBack( s.substring(0,PUSHBACK_BUFFER_SIZE-1) );
        }
        else
        {
            try
            {
                pushBack( s );
            }
            catch( IOException e )
            {
                log.warn("Pushback failed: the line is probably too long.  Attempting to recover.");
            }
        }
        return s;
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

    private int flushPlainText()
    {
        int numChars = m_plainTextBuf.length();

        if( numChars > 0 )
        {
            String buf;

            if( !m_allowHTML )
            {
                buf = escapeHTMLEntities(m_plainTextBuf.toString());
            }
            else
            {
                buf = m_plainTextBuf.toString();
            }
            //
            //  We must first empty the buffer because the side effect of
            //  calling makeCamelCaseLink() is to call this routine.
            //

            m_plainTextBuf = new StringBuilder(20);

            try
            {
                //
                //  This is the heaviest part of parsing, and therefore we can
                //  do some optimization here.
                //
                //  1) Only when the length of the buffer is big enough, we try to do the match
                //

                if( m_camelCaseLinks && !m_isEscaping && buf.length() > 3 )
                {
                    Matcher camelCaseMatcher = null;
                    while (( camelCaseMatcher =  m_camelCasePattern.matcher( buf )).find())
                    {
                        MatchResult result = camelCaseMatcher.toMatchResult();
                        
                        String firstPart = buf.substring( 0, result.start() );
                        String prefix = result.group( 1 );

                        if( prefix == null )
                            prefix = "";

                        String camelCase = result.group( 2 );
                        String protocol = result.group( 3 );
                        String uri = protocol + result.group( 4 );
                        buf = buf.substring( result.end() );

                        m_currentElement.addContent( firstPart );
                        result.group( 2 );
                        //
                        //  Check if the user does not wish to do URL or WikiWord expansion
                        //
                        if( prefix.endsWith("~") || prefix.indexOf('[') != -1 )
                        {
                            if( prefix.endsWith("~") )
                            {
                                if( m_wysiwygEditorMode )
                                {
                                    m_currentElement.addContent( "~" );
                                }
                                prefix = prefix.substring(0,prefix.length()-1);
                            }
                            if( camelCase != null )
                            {
                                m_currentElement.addContent( prefix+camelCase );
                            }
                            else if( protocol != null )
                            {
                                m_currentElement.addContent( prefix+uri );
                            }
                            continue;
                        }

                        //
                        //  Fine, then let's check what kind of a link this was
                        //  and emit the proper elements
                        //
                        if( protocol != null )
                        {
                            char c = uri.charAt(uri.length()-1);
                            if( c == '.' || c == ',' )
                            {
                                uri = uri.substring(0,uri.length()-1);
                                buf = c + buf;
                            }
                            m_currentElement.addContent( prefix );
                            makeDirectURILink( uri );
                        }
                        else
                        {
                            m_currentElement.addContent( prefix );

                            makeCamelCaseLink( camelCase );
                        }
                    }

                    m_currentElement.addContent( buf );
                }
                else
                {
                    //
                    //  No camelcase asked for, just add the elements
                    //
                    m_currentElement.addContent( buf );
                }
            }
            catch( IllegalDataException e )
            {
                //
                // Sometimes it's possible that illegal XML chars is added to the data.
                // Here we make sure it does not stop parsing.
                //
                m_currentElement.addContent( makeError(cleanupSuspectData( e.getMessage() )) );
            }
        }

        return numChars;
    }

    /**
     *  Escapes XML entities in a HTML-compatible way (i.e. does not escape
     *  entities that are already escaped).
     *
     *  @param buf
     *  @return An escaped string.
     */
    private String escapeHTMLEntities(String buf)
    {
        StringBuilder tmpBuf = new StringBuilder( buf.length() + 20 );

        for( int i = 0; i < buf.length(); i++ )
        {
            char ch = buf.charAt(i);

            if( ch == '<' )
            {
                tmpBuf.append("&lt;");
            }
            else if( ch == '>' )
            {
                tmpBuf.append("&gt;");
            }
            else if( ch == '\"' )
            {
                tmpBuf.append("&quot;");
            }
            else if( ch == '&' )
            {
                //
                //  If the following is an XML entity reference (&#.*;) we'll
                //  leave it as it is; otherwise we'll replace it with an &amp;
                //
                
                boolean isEntity = false;
                StringBuilder entityBuf = new StringBuilder();
                
                if( i < buf.length() -1 )
                {
                    for( int j = i; j < buf.length(); j++ )
                    {
                        char ch2 = buf.charAt(j);
                        
                        if( Character.isLetterOrDigit( ch2 ) || (ch2 == '#' && j == i+1) || ch2 == ';' || ch2 == '&' )
                        {
                            entityBuf.append(ch2);
                            
                            if( ch2 == ';' )
                            {
                                isEntity = true;
                                break;
                            }
                        }
                        else
                        {
                            break;
                        }
                    }
                }
                
                if( isEntity ) 
                {
                    tmpBuf.append( entityBuf );
                    i = i + entityBuf.length() - 1;
                }
                else 
                {
                    tmpBuf.append("&amp;");
                }
                
            }
            else
            {
                tmpBuf.append( ch );
            }
        }

        return tmpBuf.toString();
    }

    private Element pushElement( Element e )
    {
        flushPlainText();
        m_currentElement.addContent( e );
        m_currentElement = e;

        return e;
    }

    private Element addElement( Content e )
    {
        if( e != null )
        {
            flushPlainText();
            m_currentElement.addContent( e );
        }
        return m_currentElement;
    }

    /**
     *  All elements that can be empty by the HTML DTD.
     */
    //  Keep sorted.
    private static final String[] EMPTY_ELEMENTS = {
        "area", "base", "br", "col", "hr", "img", "input", "link", "meta", "p", "param"
    };

    /**
     *  Goes through the current element stack and pops all elements until this
     *  element is found - this essentially "closes" and element.
     *
     *  @param s
     *  @return The new current element, or null, if there was no such element in the entire stack.
     */
    private Element popElement( String s )
    {
        int flushedBytes = flushPlainText();

        Element currEl = m_currentElement;

        while( currEl.getParentElement() != null )
        {
            if( currEl.getName().equals(s) && !currEl.isRootElement() )
            {
                m_currentElement = currEl.getParentElement();

                //
                //  Check if it's okay for this element to be empty.  Then we will
                //  trick the JDOM generator into not generating an empty element,
                //  by putting an empty string between the tags.  Yes, it's a kludge
                //  but what'cha gonna do about it. :-)
                //

                if( flushedBytes == 0 && Arrays.binarySearch( EMPTY_ELEMENTS, s ) < 0 )
                {
                    currEl.addContent("");
                }

                return m_currentElement;
            }

            currEl = currEl.getParentElement();
        }

        return null;
    }


    /**
     *  Reads the stream until it meets one of the specified
     *  ending characters, or stream end.  The ending character will be left
     *  in the stream.
     */
    private String readUntil( String endChars )
        throws IOException
    {
        StringBuilder sb = new StringBuilder( 80 );
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
        StringBuilder sb = new StringBuilder( 80 );
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

    private JSPWikiMarkupParser m_cleanTranslator;

    /**
     *  Does a lazy init.  Otherwise, we would get into a situation
     *  where HTMLRenderer would try and boot a TranslatorReader before
     *  the TranslatorReader it is contained by is up.
     */
    private JSPWikiMarkupParser getCleanTranslator()
    {
        if( m_cleanTranslator == null )
        {
            WikiContext dummyContext = m_engine.getWikiContextFactory().newViewContext(
                                                           m_context.getHttpRequest(),
                                                           (HttpServletResponse)null,
                                                           m_context.getPage() );            
            m_cleanTranslator = new JSPWikiMarkupParser( dummyContext, null );

            m_cleanTranslator.m_allowHTML = true;
        }

        return m_cleanTranslator;
    }
    /**
     *  Modifies the "hd" parameter to contain proper values.  Because
     *  an "id" tag may only contain [a-zA-Z0-9:_-], we'll replace the
     *  % after url encoding with '_'.
     *  <p>
     *  Counts also duplicate headings (= headings with similar name), and
     *  attaches a counter.
     */
    private String makeHeadingAnchor( String baseName, String title, Heading hd )
    {
        hd.m_titleText = title;
        title = MarkupParser.wikifyLink( title );

        hd.m_titleSection = m_engine.encodeName(title);
        
        if( m_titleSectionCounter.containsKey( hd.m_titleSection ) )
        {
            Integer count = m_titleSectionCounter.get( hd.m_titleSection );
            count = count + 1;
            m_titleSectionCounter.put( hd.m_titleSection, count );
            hd.m_titleSection += "-" + count;
        }
        else
        {
            m_titleSectionCounter.put( hd.m_titleSection, 1 );
        }

        hd.m_titleAnchor = "section-"+m_engine.encodeName(baseName)+
                           "-"+hd.m_titleSection;
        hd.m_titleAnchor = hd.m_titleAnchor.replace( '%', '_' );
        hd.m_titleAnchor = hd.m_titleAnchor.replace( '/', '_' );
        
        return hd.m_titleAnchor;
    }

    private String makeSectionTitle( String title )
    {
        title = title.trim();
        String outTitle;

        try
        {
            JSPWikiMarkupParser dtr = getCleanTranslator();
            dtr.setInputReader( new StringReader(title) );

            CleanTextRenderer ctt = new CleanTextRenderer(m_context, dtr.parse());

            outTitle = ctt.getString();
        }
        catch( IOException e )
        {
            log.error("CleanTranslator not working", e);
            throw new InternalWikiException("CleanTranslator not working as expected, when cleaning title"+ e.getMessage() );
        }

        return outTitle;
    }

    /**
     *  Returns XHTML for the heading. 
     *  
     *  @param level The level of the heading.  @see Heading
     *  @param title the title for the heading
     *  @param hd a List to which heading should be added
     *  @return An Element containing the heading
     */
    public Element makeHeading( int level, String title, Heading hd )
    {
        Element el = null;

        String pageName = m_context.getPage().getName();

        String outTitle = makeSectionTitle( title );

        hd.m_level = level;

        switch( level )
        {
          case Heading.HEADING_SMALL:
            el = new Element("h4").setAttribute("id",makeHeadingAnchor( pageName, outTitle, hd ));
            break;

          case Heading.HEADING_MEDIUM:
            el = new Element("h3").setAttribute("id",makeHeadingAnchor( pageName, outTitle, hd ));
            break;

          case Heading.HEADING_LARGE:
            el = new Element("h2").setAttribute("id",makeHeadingAnchor( pageName, outTitle, hd ));
            break;

          default:
            throw new InternalWikiException("Illegal heading type "+level);
        }

        
        return el;
    }

    /**
     *  When given a link to a WikiName, we just return
     *  a proper HTML link for it.  The local link mutator
     *  chain is also called.
     */
    private Element makeCamelCaseLink( String wikiname )
    {
        String matchedLink;

        callMutatorChain( m_localLinkMutatorChain, wikiname );

        if( (matchedLink = linkExists( wikiname )) != null )
        {
            makeLink( READ, matchedLink, wikiname, null, null );
        }
        else
        {
            makeLink( EDIT, wikiname, wikiname, null, null );
        }

        return m_currentElement;
    }

    /** Holds the image URL for the duration of this parser */
    private String m_outlinkImageURL = null;

    /**
     *  Returns an element for the external link image (out.png).  However,
     *  this method caches the URL for the lifetime of this MarkupParser,
     *  because it's commonly used, and we'll end up with possibly hundreds
     *  our thousands of references to it...  It's a lot faster, too.
     *
     *  @return  An element containing the HTML for the outlink image.
     */
    private Element outlinkImage()
    {
        Element el = null;

        if( m_useOutlinkImage )
        {
            if( m_outlinkImageURL == null )
            {
                m_outlinkImageURL = m_context.getURL( WikiContext.NONE, OUTLINK_IMAGE );
            }

            el = new Element("img").setAttribute("class", "outlink");
            el.setAttribute( "src", m_outlinkImageURL );
            el.setAttribute("alt","");
        }

        return el;
    }

    /**
     *  Takes an URL and turns it into a regular wiki link.  Unfortunately,
     *  because of the way that flushPlainText() works, it already encodes
     *  all of the XML entities.  But so does WikiContext.getURL(), so we
     *  have to do a reverse-replace here, so that it can again be replaced in makeLink.
     *  <p>
     *  What a crappy problem.
     *
     * @param url
     * @return An anchor Element containing the link.
     */
    private Element makeDirectURILink( String url )
    {
        Element result;
        String last = null;

        if( url.endsWith(",") || url.endsWith(".") )
        {
            last = url.substring( url.length()-1 );
            url  = url.substring( 0, url.length()-1 );
        }

        callMutatorChain( m_externalLinkMutatorChain, url );

        if( isImageLink( url ) )
        {
            result = handleImageLink( StringUtils.replace(url,"&amp;","&"), url, false );
        }
        else
        {
            result = makeLink( EXTERNAL, StringUtils.replace(url,"&amp;","&"), url, null, null );
            addElement( outlinkImage() );
        }

        if( last != null )
        {
            m_plainTextBuf.append(last);
        }

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

    // FIXME: isExternalLink() is called twice.
    private Element handleImageLink( String reallink, String link, boolean hasLinkText )
    {
        String possiblePage = MarkupParser.cleanLink( link );

        if( isExternalLink( link ) && hasLinkText )
        {
            return makeLink( IMAGELINK, reallink, link, null, null );
        }
        else if( ( linkExists( possiblePage ) ) != null &&
                 hasLinkText )
        {
            callMutatorChain( m_localLinkMutatorChain, possiblePage );

            return makeLink( IMAGEWIKILINK, reallink, link, null, null );
        }
        else
        {
            return makeLink( IMAGE, reallink, link, null, null );
        }
    }

    private Element handleAccessRule( String ruleLine )
    {
        if( m_wysiwygEditorMode )
        {
            m_currentElement.addContent( "[" + ruleLine + "]" );
        }

        if( !m_parseAccessRules ) return m_currentElement;
        Acl acl;
        WikiPage          page = m_context.getRealPage();
        // UserDatabase      db = m_context.getEngine().getUserDatabase();

        if( ruleLine.startsWith( "{" ) )
            ruleLine = ruleLine.substring( 1 );
        if( ruleLine.endsWith( "}" ) )
            ruleLine = ruleLine.substring( 0, ruleLine.length() - 1 );

        if( log.isDebugEnabled() ) log.debug("page="+page.getName()+", ACL = "+ruleLine);

        try
        {
            acl = m_engine.getAclManager().parseAcl( page, ruleLine );
            page.save();
            if( log.isDebugEnabled() ) log.debug( acl.toString() );
        }
        catch( WikiSecurityException wse )
        {
            return makeError( wse.getMessage() );
        }
        catch( ProviderException pe )
        {
            return makeError( "Could not save WikiPage after extracting ACL: " + pe.getMessage() );
        }

        return m_currentElement;
    }

    /**
     *  Handles metadata setting [{SET foo=bar}]
     */
    private Element handleMetadata( String link )
    {
        if( m_wysiwygEditorMode )
        {
            m_currentElement.addContent( "[" + link + "]" );
        }

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
            ResourceBundle rb = m_context.getBundle(InternationalizationManager.CORE_BUNDLE);
            Object[] args = { link };
            return makeError( MessageFormat.format( rb.getString( "markupparser.error.invalidset" ), args ) );
        }

        return m_currentElement;
    }

    /**
     *  Emits a processing instruction that will disable markup escaping. This is
     *  very useful if you want to emit HTML directly into the stream.
     *
     */
    private void disableOutputEscaping()
    {
        addElement( new ProcessingInstruction(Result.PI_DISABLE_OUTPUT_ESCAPING, "") );
    }

    /**
     *  Gobbles up all hyperlinks that are encased in square brackets.
     */
    private Element handleHyperlinks( String linktext, int pos )
    {
        ResourceBundle rb = m_context.getBundle(InternationalizationManager.CORE_BUNDLE);
        
        StringBuilder sb = new StringBuilder(linktext.length()+80);

        if( isAccessRule( linktext ) )
        {
            return handleAccessRule( linktext );
        }

        if( isMetadata( linktext ) )
        {
            return handleMetadata( linktext );
        }

        if( PluginManager.isPluginLink( linktext ) )
        {
            try
            {
                PluginContent pluginContent = m_engine.getPluginManager().parsePluginLine( m_context,
                                                                                           linktext,
                                                                                           pos );
                //
                //  This might sometimes fail, especially if there is something which looks
                //  like a plugin invocation but is really not.
                //
                if( pluginContent != null )
                {
                    addElement( pluginContent );

                    pluginContent.executeParse( m_context );
                }
            }
            catch( PluginException e )
            {
                log.info( "Failed to insert plugin: "+e.getMessage() );
                //log.info( "Root cause:",e.getRootThrowable() );
                if( !m_wysiwygEditorMode )
                {
                    ResourceBundle rbPlugin = m_context.getBundle(WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE);
                    Object[] args = { e.getMessage() };
                    return addElement( makeError( MessageFormat.format( rbPlugin.getString( "plugin.error.insertionfailed" ), args ) ) );
                }
            }

            return m_currentElement;
        }

        try
        {
            LinkParser.Link link = m_linkParser.parse(linktext);
            linktext       = link.getText();
            String linkref = link.getReference();

            //
            //  Yes, we now have the components separated.
            //  linktext = the text the link should have
            //  linkref  = the url or page name.
            //
            //  In many cases these are the same.  [linktext|linkref].
            //
            if( VariableManager.isVariableLink( linktext ) )
            {
                Content el = new VariableContent(linktext);

                addElement( el );
            }
            else if( isExternalLink( linkref ) )
            {
                // It's an external link, out of this Wiki

                callMutatorChain( m_externalLinkMutatorChain, linkref );

                if( isImageLink( linkref ) )
                {
                    handleImageLink( linkref, linktext, link.hasReference() );
                }
                else
                {
                    makeLink( EXTERNAL, linkref, linktext, null, link.getAttributes() );
                    addElement( outlinkImage() );
                }
            }
            else if( link.isInterwikiLink() && !isSpaceLink( linktext ) )
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

                String extWiki  = link.getExternalWiki();
                String wikiPage = link.getExternalWikiPage();

                if( m_wysiwygEditorMode )
                {
                    makeLink( INTERWIKI, extWiki + ":" + wikiPage, linktext, null, link.getAttributes() );
                }
                else
                {
                    String urlReference = m_engine.getInterWikiURL( extWiki );

                    if( urlReference != null )
                    {
                        urlReference = TextUtil.replaceString( urlReference, "%s", wikiPage );
                        urlReference = callMutatorChain( m_externalLinkMutatorChain, urlReference );

                        if( isImageLink(urlReference) )
                        {
                            handleImageLink( urlReference, linktext, link.hasReference() );
                        }
                        else
                        {
                            makeLink( INTERWIKI, urlReference, linktext, null, link.getAttributes() );
                        }

                        if( isExternalLink(urlReference) )
                        {
                            addElement( outlinkImage() );
                        }
                    }
                    else
                    {
                        Object[] args = { extWiki };
                        addElement( makeError( MessageFormat.format( rb.getString( "markupparser.error.nointerwikiref" ), args ) ) );
                    }
                }
            }
            else if( linkref.startsWith("#") )
            {
                // It defines a local footnote
                makeLink( LOCAL, linkref, linktext, null, link.getAttributes() );
            }
            else if( TextUtil.isNumber( linkref ) )
            {
                // It defines a reference to a local footnote
                makeLink( LOCALREF, linkref, linktext, null, link.getAttributes() );
            }
            else
            {
                int hashMark = -1;

                //
                //  Internal wiki link, but is it an attachment link?
                //
                String attachment = findAttachment( linkref );
                if( attachment != null )
                {
                    callMutatorChain( m_attachmentLinkMutatorChain, attachment );

                    if( isImageLink( linkref ) )
                    {
                        attachment = m_context.getURL( WikiContext.ATTACH, attachment );
                        sb.append( handleImageLink( attachment, linktext, link.hasReference() ) );
                    }
                    else
                    {
                        makeLink( ATTACHMENT, attachment, linktext, null, link.getAttributes() );
                    }
                }
                else if( (hashMark = linkref.indexOf('#')) != -1 )
                {
                    // It's an internal Wiki link, but to a named section

                    String namedSection = linkref.substring( hashMark+1 );
                    linkref = linkref.substring( 0, hashMark );

                    linkref = MarkupParser.cleanLink( linkref );

                    callMutatorChain( m_localLinkMutatorChain, linkref );

                    String matchedLink;
                    if( (matchedLink = linkExists( linkref )) != null )
                    {
                        String sectref = "section-"+m_engine.encodeName(matchedLink)+"-"+wikifyLink(namedSection);
                        sectref = sectref.replace('%', '_');
                        makeLink( READ, matchedLink, linktext, sectref, link.getAttributes() );
                    }
                    else
                    {
                        makeLink( EDIT, linkref, linktext, null, link.getAttributes() );
                    }
                }
                else
                {
                    // It's an internal Wiki link
                    linkref = MarkupParser.cleanLink( linkref );

                    callMutatorChain( m_localLinkMutatorChain, linkref );

                    String matchedLink = linkExists( linkref );

                    if( matchedLink != null )
                    {
                        makeLink( READ, matchedLink, linktext, null, link.getAttributes() );
                    }
                    else
                    {
                        makeLink( EDIT, linkref, linktext, null, link.getAttributes() );
                    }
                }
            }
        }
        catch( ParseException e )
        {
            log.info("Parser failure: ",e);
            Object[] args = { e.getMessage() };
            addElement( makeError( MessageFormat.format( rb.getString( "markupparser.error.parserfailure" ), args ) ) );
        }

        return m_currentElement;
    }

    private String findAttachment( String linktext )
    {
        AttachmentManager mgr = m_engine.getAttachmentManager();
        WikiPage att = null;

        try
        {
            att = mgr.getAttachmentInfo( m_context, linktext );
            return att == null ? null : att.getName();
        }
        catch( PageNotFoundException e )
        {}
        catch( Exception e )
        {
            log.warn("Finding attachments failed: ",e);
            return null;
        }
        
        if( linktext.indexOf('/') != -1 )
        {
            return linktext;
        }

        return null;
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

    private Element handleBackslash()
        throws IOException
    {
        int ch = nextToken();

        if( ch == '\\' )
        {
            int ch2 = nextToken();

            if( ch2 == '\\' )
            {
                pushElement( new Element("br").setAttribute("clear","all"));
                return popElement("br");
            }

            pushBack( ch2 );

            pushElement( new Element("br") );
            return popElement("br");
        }

        pushBack( ch );

        return null;
    }

    private Element handleUnderscore()
        throws IOException
    {
        int ch = nextToken();
        Element el = null;

        if( ch == '_' )
        {
            if( m_isbold )
            {
                el = popElement("b");
            }
            else
            {
                el = pushElement( new Element("b") );
            }
            m_isbold = !m_isbold;
        }
        else
        {
            pushBack( ch );
        }

        return el;
    }


    /**
     *  For example: italics.
     */
    private Element handleApostrophe()
        throws IOException
    {
        int ch = nextToken();
        Element el = null;

        if( ch == '\'' )
        {
            if( m_isitalic )
            {
                el = popElement("i");
            }
            else
            {
                el = pushElement( new Element("i") );
            }
            m_isitalic = !m_isitalic;
        }
        else
        {
            pushBack( ch );
        }

        return el;
    }

    private Element handleOpenbrace( boolean isBlock )
        throws IOException
    {
        int ch = nextToken();

        if( ch == '{' )
        {
            int ch2 = nextToken();

            if( ch2 == '{' )
            {
                m_isPre = true;
                m_isEscaping = true;
                m_isPreBlock = isBlock;

                if( isBlock )
                {
                    startBlockLevel();
                    return pushElement( new Element("pre") );
                }

                return pushElement( new Element("span").setAttribute("style","font-family:monospace; white-space:pre;") );
            }

            pushBack( ch2 );

            return pushElement( new Element("tt") );
        }

        pushBack( ch );

        return null;
    }

    /**
     *  Handles both }} and }}}
     */
    private Element handleClosebrace()
        throws IOException
    {
        int ch2 = nextToken();

        if( ch2 == '}' )
        {
            int ch3 = nextToken();

            if( ch3 == '}' )
            {
                if( m_isPre )
                {
                    if( m_isPreBlock )
                    {
                        popElement( "pre" );
                    }
                    else
                    {
                        popElement( "span" );
                    }

                    m_isPre = false;
                    m_isEscaping = false;
                    return m_currentElement;
                }

                m_plainTextBuf.append("}}}");
                return m_currentElement;
            }

            pushBack( ch3 );

            if( !m_isEscaping )
            {
                return popElement("tt");
            }
        }

        pushBack( ch2 );

        return null;
    }

    private Element handleDash()
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
                    startBlockLevel();
                    pushElement( new Element("hr") );
                    return popElement( "hr" );
                }

                pushBack( ch3 );
            }
            pushBack( ch2 );
        }

        pushBack( ch );

        return null;
    }

    private Element handleHeading()
        throws IOException
    {
        Element el = null;

        int ch  = nextToken();

        Heading hd = new Heading();

        if( ch == '!' )
        {
            int ch2 = nextToken();

            if( ch2 == '!' )
            {
                String title = peekAheadLine();

                el = makeHeading( Heading.HEADING_LARGE, title, hd);
            }
            else
            {
                pushBack( ch2 );
                String title = peekAheadLine();
                el = makeHeading( Heading.HEADING_MEDIUM, title, hd );
            }
        }
        else
        {
            pushBack( ch );
            String title = peekAheadLine();
            el = makeHeading( Heading.HEADING_SMALL, title, hd );
        }

        callHeadingListenerChain( hd );

        m_lastHeading = hd;
        
        if( el != null ) pushElement(el);

        return el;
    }

    /**
     *  Reads the stream until the next EOL or EOF.  Note that it will also read the
     *  EOL from the stream.
     */
    private StringBuilder readUntilEOL()
        throws IOException
    {
        int ch;
        StringBuilder buf = new StringBuilder( 256 );

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

    /** Controls whether italic is restarted after a paragraph shift */

    private boolean m_restartitalic = false;
    private boolean m_restartbold   = false;

    private boolean m_newLine;

    /**
     *  Starts a block level element, therefore closing
     *  a potential open paragraph tag.
     */
    private void startBlockLevel()
    {
        // These may not continue over block level limits in XHTML

        popElement("i");
        popElement("b");
        popElement("tt");

        if( m_isOpenParagraph )
        {
            m_isOpenParagraph = false;
            popElement("p");
            m_plainTextBuf.append("\n"); // Just small beautification
        }

        m_restartitalic = m_isitalic;
        m_restartbold   = m_isbold;

        m_isitalic = false;
        m_isbold   = false;
    }

    private static String getListType( char c )
    {
        if( c == '*' )
        {
            return "ul";
        }
        else if( c == '#' )
        {
            return "ol";
        }
        throw new InternalWikiException("Parser got faulty list type: "+c);
    }
    /**
     *  Like original handleOrderedList() and handleUnorderedList()
     *  however handles both ordered ('#') and unordered ('*') mixed together.
     */

    // FIXME: Refactor this; it's a bit messy.

    private Element handleGeneralList()
        throws IOException
    {
         startBlockLevel();

         String strBullets = readWhile( "*#" );
         // String strBulletsRaw = strBullets;      // to know what was original before phpwiki style substitution
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
             if( numBullets > m_genlistlevel )
             {
                 pushElement( new Element( getListType(strBullets.charAt(m_genlistlevel++) ) ) );

                 for( ; m_genlistlevel < numBullets; m_genlistlevel++ )
                 {
                     // bullets are growing, get from new bullet list
                     pushElement( new Element("li") );
                     pushElement( new Element( getListType(strBullets.charAt(m_genlistlevel)) ));
                 }
             }
             else if( numBullets < m_genlistlevel )
             {
                 //  Close the previous list item.
                 // buf.append( m_renderer.closeListItem() );
                 popElement( "li" );

                 for( ; m_genlistlevel > numBullets; m_genlistlevel-- )
                 {
                     // bullets are shrinking, get from old bullet list

                     popElement( getListType(m_genlistBulletBuffer.charAt(m_genlistlevel-1)) );
                     if( m_genlistlevel > 0 )
                     {
                         popElement( "li" );
                     }

                 }
             }
             else
             {
                 if( m_genlistlevel > 0 )
                 {
                     popElement( "li" );
                 }
             }
         }
         else
         {
             //
             //  The pattern has changed, unwind and restart
             //
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
                 popElement( getListType( m_genlistBulletBuffer.charAt(m_genlistlevel-1) ) );
                 if( m_genlistlevel > numBullets )
                 {
                     popElement("li");
                 }
             }

             //rewind

             pushElement( new Element(getListType( strBullets.charAt(numEqualBullets++) ) ) );
             for(int i = numEqualBullets; i < numBullets; i++)
             {
                 pushElement( new Element("li") );
                 pushElement( new Element( getListType( strBullets.charAt(i) ) ) );
             }
             m_genlistlevel = numBullets;
         }

         //
         //  Push a new list item, and eat away any extra whitespace
         //
         pushElement( new Element("li") );
         readWhile(" ");

         // work done, remember the new bullet list (in place of old one)
         m_genlistBulletBuffer.setLength(0);
         m_genlistBulletBuffer.append(strBullets);

         return m_currentElement;
    }

    private Element unwindGeneralList()
    {
        //unwind
        for( ; m_genlistlevel > 0; m_genlistlevel-- )
        {
            popElement( "li" );
            popElement( getListType(m_genlistBulletBuffer.charAt(m_genlistlevel-1)) );
        }

        m_genlistBulletBuffer.setLength(0);

        return null;
    }


    private Element handleDefinitionList()
        throws IOException
    {
        if( !m_isdefinition )
        {
            m_isdefinition = true;

            startBlockLevel();

            pushElement( new Element("dl") );
            return pushElement( new Element("dt") );
        }

        return null;
    }

    private Element handleOpenbracket()
        throws IOException
    {
        StringBuilder sb = new StringBuilder(40);
        int pos = getPosition();
        int ch = nextToken();
        boolean isPlugin = false;

        if( ch == '[' )
        {
            if( m_wysiwygEditorMode )
            {
                sb.append( '[' );
            }

            sb.append( (char)ch );

            while( (ch = nextToken()) == '[' )
            {
                sb.append( (char)ch );
            }
        }


        if( ch == '{' )
        {
            isPlugin = true;
        }

        pushBack( ch );

        if( sb.length() > 0 )
        {
            m_plainTextBuf.append( sb );
            return m_currentElement;
        }

        //
        //  Find end of hyperlink
        //

        ch = nextToken();
        int nesting = 1;    // Check for nested plugins

        while( ch != -1 )
        {
            int ch2 = nextToken(); pushBack(ch2);

            if( isPlugin )
            {
                if( ch == '[' && ch2 == '{' )
                {
                    nesting++;
                }
                else if( nesting == 0 && ch == ']' && sb.charAt(sb.length()-1) == '}' )
                {
                    break;
                }
                else if( ch == '}' && ch2 == ']' )
                {
                    // NB: This will be decremented once at the end
                    nesting--;
                }
            }
            else
            {
                if( ch == ']' )
                {
                    break;
                }
            }

            sb.append( (char) ch );

            ch = nextToken();
        }

        //
        //  If the link is never finished, do some tricks to display the rest of the line
        //  unchanged.
        //
        if( ch == -1 )
        {
            log.debug("Warning: unterminated link detected!");
            m_isEscaping = true;
            m_plainTextBuf.append( sb );
            flushPlainText();
            m_isEscaping = false;
            return m_currentElement;
        }

        return handleHyperlinks( sb.toString(), pos );
    }

    /**
     *  Reads the stream until the current brace is closed or stream end.
     */
    private String readBraceContent( char opening, char closing )
        throws IOException
    {
        StringBuilder sb = new StringBuilder(40);
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
                if (braceLevel==0)
                {
                  break;
                }
            }
            sb.append( (char)ch );
        }
        return sb.toString();
    }


    /**
     *  Handles constructs of type %%(style) and %%class
     * @param newLine
     * @return An Element containing the div or span, depending on the situation.
     * @throws IOException
     */
    private Element handleDiv( boolean newLine )
        throws IOException
    {
        int ch = nextToken();
        Element el = null;

        if( ch == '%' )
        {
            String style = null;
            String clazz = null;

            ch = nextToken();

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
                    Boolean isSpan = m_styleStack.pop();

                    if( isSpan == null )
                    {
                        // Fail quietly
                    }
                    else if( isSpan.booleanValue() )
                    {
                        el = popElement( "span" );
                    }
                    else
                    {
                        el = popElement( "div" );
                    }
                }
                catch( EmptyStackException e )
                {
                    log.debug("Page '"+m_context.getPage().getName()+"' closes a %%-block that has not been opened.");
                    return m_currentElement;
                }

                return el;
            }

            //
            //  Check if there is an attempt to do something nasty
            //
            
            try
            {
                style = StringEscapeUtils.unescapeHtml(style);
                if( style != null && style.indexOf("javascript:") != -1 )
                {
                    log.debug("Attempt to output javascript within CSS:"+style);
                    ResourceBundle rb = m_context.getBundle(InternationalizationManager.CORE_BUNDLE);
                    return addElement( makeError( rb.getString( "markupparser.error.javascriptattempt" ) ) );
                }
            }
            catch( NumberFormatException e )
            {
                //
                //  If there are unknown entities, we don't want the parser to stop.
                //
                ResourceBundle rb = m_context.getBundle(InternationalizationManager.CORE_BUNDLE);
                Object[] args = { e.getMessage() };
                String msg = MessageFormat.format( rb.getString( "markupparser.error.parserfailure"), args );
                return addElement( makeError( msg ) );
            }

            //
            //  Decide if we should open a div or a span?
            //
            String eol = peekAheadLine();

            if( eol.trim().length() > 0 )
            {
                // There is stuff after the class

                el = new Element("span");

                m_styleStack.push( Boolean.TRUE );
            }
            else
            {
                startBlockLevel();
                el = new Element("div");
                m_styleStack.push( Boolean.FALSE );
            }

            if( style != null ) el.setAttribute("style", style);
            if( clazz != null ) el.setAttribute("class", clazz );
            el = pushElement( el );

            return el;
        }

        pushBack(ch);

        return el;
    }

    private Element handleSlash( boolean newLine )
        throws IOException
    {
        int ch = nextToken();

        pushBack(ch);
        if( ch == '%' && !m_styleStack.isEmpty() )
        {
            return handleDiv( newLine );
        }

        return null;
    }

    private Element handleBar( boolean newLine )
        throws IOException
    {
        Element el = null;

        if( !m_istable && !newLine )
        {
            return null;
        }

        //
        //  If the bar is in the first column, we will either start
        //  a new table or continue the old one.
        //

        if( newLine )
        {
            if( !m_istable )
            {
                startBlockLevel();
                el = pushElement( new Element("table").setAttribute("class","wikitable").setAttribute("border","1") );
                m_istable = true;
                m_rowNum = 0;
            }

            m_rowNum++;
            Element tr = ( m_rowNum % 2 != 0 )
                       ? new Element("tr").setAttribute("class", "odd")
                       : new Element("tr");
            el = pushElement( tr );
        }

        //
        //  Check out which table cell element to start;
        //  a header element (th) or a regular element (td).
        //
        int ch = nextToken();

        if( ch == '|' )
        {
            if( !newLine )
            {
                el = popElement("th");
                if( el == null ) popElement("td");
            }
            el = pushElement( new Element("th") );
        }
        else
        {
            if( !newLine )
            {
                el = popElement("td");
                if( el == null ) popElement("th");
            }

            el = pushElement( new Element("td") );

            pushBack( ch );
        }

        return el;
    }

    /**
     *  Generic escape of next character or entity.
     */
    private Element handleTilde()
        throws IOException
    {
        int ch = nextToken();

        if( ch == ' ' )
        {
            if( m_wysiwygEditorMode )
            {
                m_plainTextBuf.append( "~ " );
            }
            return m_currentElement;
        }

        if( ch == '|' || ch == '~' || ch == '\\' || ch == '*' || ch == '#' ||
            ch == '-' || ch == '!' || ch == '\'' || ch == '_' || ch == '[' ||
            ch == '{' || ch == ']' || ch == '}' || ch == '%' )
        {
            if( m_wysiwygEditorMode )
            {
                m_plainTextBuf.append( '~' );
            }

            m_plainTextBuf.append( (char)ch );
            m_plainTextBuf.append(readWhile( ""+(char)ch ));
            return m_currentElement;
        }

        // No escape.
        pushBack( ch );

        return null;
    }

    private void fillBuffer( Element startElement )
        throws IOException
    {
        m_currentElement = startElement;

        boolean quitReading = false;
        m_newLine = true;
        disableOutputEscaping();

        while(!quitReading)
        {
            int ch = nextToken();

            if( ch == -1 ) break;

            //
            //  Check if we're actually ending the preformatted mode.
            //  We still must do an entity transformation here.
            //
            if( m_isEscaping )
            {
                if( ch == '}' )
                {
                    if( handleClosebrace() == null ) m_plainTextBuf.append( (char) ch );
                }
                else if( ch == -1 )
                {
                    quitReading = true;
                }
                else if( ch == '\r' )
                {
                    // DOS line feeds we ignore.
                }
                else if( ch == '<' )
                {
                    m_plainTextBuf.append( "&lt;" );
                }
                else if( ch == '>' )
                {
                    m_plainTextBuf.append( "&gt;" );
                }
                else if( ch == '&' )
                {
                    m_plainTextBuf.append( "&amp;" );
                }
                else if( ch == '~' )
                {
                    String braces = readWhile("}");
                    if( braces.length() >= 3 )
                    {
                        m_plainTextBuf.append("}}}");

                        braces = braces.substring(3);
                    }
                    else
                    {
                        m_plainTextBuf.append( (char) ch );
                    }

                    for( int i = braces.length()-1; i >= 0; i-- )
                    {
                        pushBack(braces.charAt(i));
                    }
                }
                else
                {
                    m_plainTextBuf.append( (char) ch );
                }

                continue;
            }

            //
            //  An empty line stops a list
            //
            if( m_newLine && ch != '*' && ch != '#' && ch != ' ' && m_genlistlevel > 0 )
            {
                m_plainTextBuf.append(unwindGeneralList());
            }

            if( m_newLine && ch != '|' && m_istable )
            {
                popElement("table");
                m_istable = false;
            }

            int skip = IGNORE;
            
            //
            //  Do the actual parsing and catch any errors.
            //
            try
            {
                skip = parseToken( ch );
            }
            catch( IllegalDataException e )
            {
                log.info("Page "+m_context.getPage().getName()+" contains data which cannot be added to DOM tree: "+e.getMessage());
                
                makeError("Error: "+cleanupSuspectData(e.getMessage()) );
            }
            
            //
            //   The idea is as follows:  If the handler method returns
            //   an element (el != null), it is assumed that it has been
            //   added in the stack.  Otherwise the character is added
            //   as is to the plaintext buffer.
            //
            //   For the transition phase, if s != null, it also gets
            //   added in the plaintext buffer.
            //

            switch( skip )
            {
                case ELEMENT:
                    m_newLine = false;
                    break;

                case CHARACTER:
                    m_plainTextBuf.append( (char) ch );
                    m_newLine = false;
                    break;

                case IGNORE:
                default:
                    break;
            }
        }

        closeHeadings();
        popElement("domroot");
    }

    private String cleanupSuspectData( String s )
    {
        StringBuilder sb = new StringBuilder( s.length() );
        
        for( int i = 0; i < s.length(); i++ )
        {
            char c = s.charAt(i);
            
            if( Verifier.isXMLCharacter( c ) ) sb.append( c );
            else sb.append( "0x"+Integer.toString(c,16).toUpperCase() );
        }
        
        return sb.toString();
    }
    
    /** The token is a plain character. */
    protected static final int CHARACTER = 0;
    
    /** The token is a wikimarkup element. */
    protected static final int ELEMENT   = 1;
    
    /** The token is to be ignored. */
    protected static final int IGNORE    = 2;

    /**
     *  Return CHARACTER, if you think this was a plain character; ELEMENT, if
     *  you think this was a wiki markup element, and IGNORE, if you think
     *  we should ignore this altogether.
     *  <p>
     *  To add your own MarkupParser, you can override this method, but it
     *  is recommended that you call super.parseToken() as well to gain advantage
     *  of JSPWiki's own markup.  You can call it at the start of your own
     *  parseToken() or end - it does not matter.
     *
     * @param ch The character under investigation
     * @return {@link #ELEMENT}, {@link #CHARACTER} or {@link #IGNORE}.
     * @throws IOException If parsing fails.
     */
    protected int parseToken( int ch )
        throws IOException
    {
        Element el = null;

        //
        //  Now, check the incoming token.
        //
        switch( ch )
        {
          case '\r':
            // DOS linefeeds we forget
            return IGNORE;

          case '\n':
            //
            //  Close things like headings, etc.
            //

            // FIXME: This is not really very fast
            
            closeHeadings();
              
            popElement("dl"); // Close definition lists.
            if( m_istable )
            {
                popElement("tr");
            }

            m_isdefinition = false;

            if( m_newLine )
            {
                // Paragraph change.
                startBlockLevel();

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
                    pushElement( new Element("p") );
                    m_isOpenParagraph = true;

                    if( m_restartitalic )
                    {
                        pushElement( new Element("i") );
                        m_isitalic = true;
                        m_restartitalic = false;
                    }
                    if( m_restartbold )
                    {
                        pushElement( new Element("b") );
                        m_isbold = true;
                        m_restartbold = false;
                    }
                }
            }
            else
            {
                m_plainTextBuf.append("\n");
                m_newLine = true;
            }
            return IGNORE;


          case '\\':
            el = handleBackslash();
            break;

          case '_':
            el = handleUnderscore();
            break;

          case '\'':
            el = handleApostrophe();
            break;

          case '{':
            el = handleOpenbrace( m_newLine );
            break;

          case '}':
            el = handleClosebrace();
            break;

          case '-':
            if( m_newLine )
                el = handleDash();

            break;

          case '!':
            if( m_newLine )
            {
                el = handleHeading();
            }
            break;

          case ';':
            if( m_newLine )
            {
                el = handleDefinitionList();
            }
            break;

          case ':':
            if( m_isdefinition )
            {
                popElement("dt");
                el = pushElement( new Element("dd") );
                m_isdefinition = false;
            }
            break;

          case '[':
            el = handleOpenbracket();
            break;

          case '*':
            if( m_newLine )
            {
                pushBack('*');
                el = handleGeneralList();
            }
            break;

          case '#':
            if( m_newLine )
            {
                pushBack('#');
                el = handleGeneralList();
            }
            break;

          case '|':
            el = handleBar( m_newLine );
            break;

          case '~':
            el = handleTilde();
            break;

          case '%':
            el = handleDiv( m_newLine );
            break;

          case '/':
            el = handleSlash( m_newLine );
            break;

          default:
            break;
        }

        return el != null ? ELEMENT : CHARACTER;
    }

    private void closeHeadings()
    {
        if( m_lastHeading != null && !m_wysiwygEditorMode )
        {
            // Add the hash anchor element at the end of the heading
            addElement( new Element("a").setAttribute( "class","hashlink" ).setAttribute( "href","#"+m_lastHeading.m_titleAnchor ).setText( "#" ) );
            m_lastHeading = null;
        }
        popElement("h2");
        popElement("h3");
        popElement("h4");
    }

    /**
     *  Parses the entire document from the Reader given in the constructor or
     *  set by {@link #setInputReader(Reader)}.
     *  
     *  @return A WikiDocument, ready to be passed to the renderer.
     *  @throws IOException If parsing cannot be accomplished.
     */
    public WikiDocument parse()
        throws IOException
    {
        WikiDocument d = new WikiDocument( m_context.getPage() );
        d.setContext( m_context );

        Element rootElement = new Element("domroot");

        d.setRootElement( rootElement );

        fillBuffer( rootElement );

        paragraphify(rootElement);

        return d;
    }

    /**
     *  Checks out that the first paragraph is correctly installed.
     *
     *  @param rootElement
     */
    private void paragraphify(Element rootElement)
    {
        //
        //  Add the paragraph tag to the first paragraph
        //
        List<?> kids = rootElement.getContent();

        if( rootElement.getChild("p") != null )
        {
            ArrayList<Content> ls = new ArrayList<Content>();
            int idxOfFirstContent = 0;
            int count = 0;

            for( Iterator<?> i = kids.iterator(); i.hasNext(); count++ )
            {
                Content c = (Content) i.next();
                if( c instanceof Element )
                {
                    String name = ((Element)c).getName();
                    if( isBlockLevel(name) ) break;
                }

                if( !(c instanceof ProcessingInstruction) )
                {
                    ls.add( c );
                    if( idxOfFirstContent == 0 ) idxOfFirstContent = count;
                }
            }

            //
            //  If there were any elements, then add a new <p> (unless it would
            //  be an empty one)
            //
            if( ls.size() > 0 )
            {
                Element newel = new Element("p");

                for( Content c : ls )
                {
                    c.detach();
                    newel.addContent(c);
                }

                //
                // Make sure there are no empty <p/> tags added.
                //
                if( newel.getTextTrim().length() > 0 || !newel.getChildren().isEmpty() )
                    rootElement.addContent(idxOfFirstContent, newel);
            }
        }
    }


    /**
     *  Compares two Strings, and if one starts with the other, then
     *  returns null.  Otherwise just like the normal Comparator
     *  for strings.
     *
     *  @since
     */
    private static class StartingComparator implements Comparator<String>
    {
        public int compare( String s1, String s2 )
        {
            if( s1.length() > s2.length() )
            {
                if( s1.startsWith(s2) && s2.length() > 1 ) return 0;
            }
            else
            {
                if( s2.startsWith(s1) && s1.length() > 1 ) return 0;
            }

            return s1.compareTo( s2 );
        }

    }

}
