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
package com.ecyrd.jspwiki.parser;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

import javax.xml.transform.Result;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.oro.text.GlobCompiler;
import org.apache.oro.text.regex.*;
import org.jdom.Content;
import org.jdom.Element;
import org.jdom.IllegalDataException;
import org.jdom.ProcessingInstruction;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.attachment.AttachmentManager;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.auth.acl.Acl;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.plugin.PluginManager;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.render.CleanTextRenderer;

/**
 *  Parses JSPWiki-style markup into a WikiDocument DOM tree.  This class is the
 *  heart and soul of JSPWiki : make sure you test properly anything that is added,
 *  or else it breaks down horribly.
 *
 *  @author Janne Jalkanen
 *  @since  2.4
 */
public class JSPWikiMarkupParser
    extends MarkupParser
{
    private static final int              READ          = 0;
    private static final int              EDIT          = 1;
    private static final int              EMPTY         = 2;  // Empty message
    private static final int              LOCAL         = 3;
    private static final int              LOCALREF      = 4;
    private static final int              IMAGE         = 5;
    private static final int              EXTERNAL      = 6;
    private static final int              INTERWIKI     = 7;
    private static final int              IMAGELINK     = 8;
    private static final int              IMAGEWIKILINK = 9;
    private static final int              ATTACHMENT    = 10;
    // private static final int              ATTACHMENTIMAGE = 11;

    private static Logger log = Logger.getLogger( JSPWikiMarkupParser.class );

    //private boolean        m_iscode       = false;
    private boolean        m_isbold       = false;
    private boolean        m_isitalic     = false;
    private boolean        m_istable      = false;
    private boolean        m_isPre        = false;
    private boolean        m_isEscaping   = false;
    private boolean        m_isdefinition = false;
    private boolean        m_isPreBlock   = false;

    /** Contains style information, in multiple forms. */
    private Stack          m_styleStack   = new Stack();
    
     // general list handling
    private int            m_genlistlevel = 0;
    private StringBuffer   m_genlistBulletBuffer = new StringBuffer(10);  // stores the # and * pattern
    private boolean        m_allowPHPWikiStyleLists = true;


    private boolean        m_isOpenParagraph = false;

    /** Keeps image regexp Patterns */
    private ArrayList      m_inlineImagePatterns;

    private PatternMatcher m_inlineMatcher = new Perl5Matcher();

    /** Keeps track of any plain text that gets put in the Text nodes */
    private StringBuffer   m_plainTextBuf = new StringBuffer(20);
    
    private Element        m_currentElement;
    
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

    /** If set to "true", all external links are tagged with 'rel="nofollow"' */
    public static final String     PROP_USERELNOFOLLOW   = "jspwiki.translatorReader.useRelNofollow";

    /** If true, then considers CamelCase links as well. */
    private boolean                m_camelCaseLinks      = false;

    /** If true, consider URIs that have no brackets as well. */
    // FIXME: Currently reserved, but not used.
    private boolean                m_plainUris           = false;

    /** If true, all outward links use a small link image. */
    private boolean                m_useOutlinkImage     = true;

    /** If true, allows raw HTML. */
    private boolean                m_allowHTML           = false;

    private boolean                m_useRelNofollow      = false;

    private PatternCompiler        m_compiler = new Perl5Compiler();

    static final String WIKIWORD_REGEX = "(^|[[:^alnum:]]+)([[:upper:]]+[[:lower:]]+[[:upper:]]+[[:alnum:]]*|(http://|https://|mailto:)([A-Za-z0-9_/\\.\\+\\?\\#\\-\\@]+))";
    
    private PatternMatcher         m_camelCaseMatcher = new Perl5Matcher();
    private Pattern                m_camelCasePattern;
    
    private int                    m_rowNum              = 1;
    

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
     *  This Comparator is used to find an external link from c_externalLinks.  It
     *  checks if the link starts with the other arraythingie.
     */
    private static Comparator c_startingComparator = new StartingComparator();

    static
    {
        Arrays.sort( c_externalLinks );
    }
    
    /**
     *  Creates a markup parser.
     */
    public JSPWikiMarkupParser( WikiContext context, Reader in )
    {
        super( context, in );
        initialize();
    }

    /**
     *  @param engine The WikiEngine this reader is attached to.  Is
     * used to figure out of a page exits.
     */

    // FIXME: parsers should be pooled for better performance.
    private void initialize()
    {
        PatternCompiler compiler         = new GlobCompiler();
        ArrayList       compiledpatterns = new ArrayList();

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
            m_camelCasePattern = m_compiler.compile( WIKIWORD_REGEX );
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
                                                             MarkupParser.PROP_ALLOWHTML, 
                                                             m_allowHTML );

        m_useRelNofollow      = TextUtil.getBooleanProperty( props,
                                                             PROP_USERELNOFOLLOW,
                                                             m_useRelNofollow );
    
        if( m_engine.getUserDatabase() == null || m_engine.getAuthorizationManager() == null )
        {
            disableAccessRules();
        }   
        
        m_context.getPage().setHasMetadata();
    }


    /**
     *  Figure out which image suffixes should be inlined.
     *  @return Collection of Strings with patterns.
     */

    // FIXME: Does not belong here; should be elsewhere
    public static Collection getImagePatterns( WikiEngine engine )
    {
        Properties props    = engine.getWikiProperties();
        ArrayList  ptrnlist = new ArrayList();

        for( Enumeration e = props.propertyNames(); e.hasMoreElements(); )
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

    private Element makeLink( int type, String link, String text, String section )
    {
        Element el = null;
        
        if( text == null ) text = link;

        text = callMutatorChain( m_linkMutators, text );

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
                el = new Element("a").setAttribute("class", "wikipage");
                el.setAttribute("href",m_context.getURL(WikiContext.VIEW, link)+section);
                el.addContent(text);
                break;

            case EDIT:
                el = new Element("a").setAttribute("class", "editpage");
                el.setAttribute("title","Create '"+link+"'");
                el.setAttribute("href", m_context.getURL(WikiContext.EDIT,link));
                el.addContent(text);
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
                el = new Element("a").setAttribute("class","footnoteref");
                el.setAttribute("href","#ref-"+m_context.getPage().getName()+"-"+link);
                el.addContent("["+text+"]");
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
                el = new Element("a").setAttribute("href",text).addContent(el);
                break;

            case IMAGEWIKILINK:
                String pagelink = m_context.getURL(WikiContext.VIEW,text);
                el = new Element("img").setAttribute("class","inline");
                el.setAttribute("src",link);
                el.setAttribute("alt",text);
                el = new Element("a").setAttribute("class","wikipage").setAttribute("href",pagelink).addContent(el);
                break;

            case EXTERNAL:
                el = new Element("a").setAttribute("class","external");
                if( m_useRelNofollow ) el.setAttribute("rel","nofollow");
                el.setAttribute("href",link+section);
                el.addContent(text);
                break;
                
            case INTERWIKI:
                el = new Element("a").setAttribute("class","interwiki");
                el.setAttribute("href",link+section);
                el.addContent(text);
                break;

            case ATTACHMENT:
                String attlink = m_context.getURL( WikiContext.ATTACH,
                                                   link );

                String infolink = m_context.getURL( WikiContext.INFO,
                                                    link );

                String imglink = m_context.getURL( WikiContext.NONE,
                                                   "images/attachment_small.png" );

                el = new Element("a").setAttribute("class","attachment");
                el.setAttribute("href",attlink);
                el.addContent(text);
                
                pushElement(el);
                popElement(el.getName());
                
                el = new Element("img").setAttribute("src",imglink);
                el.setAttribute("border","0");
                el.setAttribute("alt","(info)");
                
                el = new Element("a").setAttribute("href",infolink).addContent(el);
                break;

            default:
                break;
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
     *  @since 2.4
     */

    public static boolean isExternalLink( String link )
    {
        int idx = Arrays.binarySearch( c_externalLinks, link, 
                                       c_startingComparator );

        //
        //  We need to check here once again; otherwise we might
        //  get a match for something like "h".
        //
        if( idx >= 0 && link.startsWith(c_externalLinks[idx]) ) return true;
        
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
    

    /**
     *  Writes HTML for error message.
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
                buf = escapeHTMLEntities(m_plainTextBuf);
            }
            else
            {
                buf = m_plainTextBuf.toString();
            }
            //
            //  We must first empty the buffer because the side effect of 
            //  calling makeCamelCaseLink() is to call this routine.
            //

            m_plainTextBuf = new StringBuffer(20);
              
            //
            //  This is the heaviest part of parsing, and therefore we can
            //  do some optimization here.
            //
            //  1) Only when the length of the buffer is big enough, we try to do the match
            //

            if( m_camelCaseLinks && !m_isEscaping && buf.length() > 3 )
            {            
                // System.out.println("Buffer="+buf);

                while( m_camelCaseMatcher.contains( buf, m_camelCasePattern ) )
                {
                    MatchResult result = m_camelCaseMatcher.getMatch();

                    String firstPart = buf.substring(0,result.beginOffset(0)); 
                    String prefix = result.group(1);
                    
                    if( prefix == null ) prefix = "";
                    
                    String camelCase = result.group(2);
                    String protocol  = result.group(3);
                    String uri       = protocol+result.group(4);
                    buf              = buf.substring(result.endOffset(0));
                    
                    m_currentElement.addContent( firstPart );
                    
                    //
                    //  Check if the user does not wish to do URL or WikiWord expansion
                    //
                    if( prefix.endsWith("~") || prefix.indexOf('[') != -1 )
                    {
                        if( prefix.endsWith("~") ) prefix = prefix.substring(0,prefix.length()-1);
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
                        // System.out.println("URI match "+uri);
                        m_currentElement.addContent( prefix );
                        makeDirectURILink( uri );
                    }
                    else
                    {
                        // System.out.println("Matched: '"+camelCase+"'");
                        // System.out.println("Split to '"+firstPart+"', and '"+buf+"'");
                        // System.out.println("prefix="+prefix);
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
        
        return numChars;
    }

    /**
     *  Escapes XML entities in a HTML-compatible way (i.e. does not escape
     *  entities that are already escaped).
     *  
     *  @param buf
     *  @return
     */
    private String escapeHTMLEntities(StringBuffer buf)
    {
        StringBuffer tmpBuf = new StringBuffer( buf.length() + 20 );
        
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
            else if( ch == '&' )
            {
                for( int j = (i < buf.length()-1 ) ? i+1 : i; j < buf.length(); j++ )
                {
                    int ch2 = buf.charAt(j);
                    if( ch2 == ';' ) 
                    { 
                        tmpBuf.append(ch); 
                        break; 
                    }
                    if( ch2 != '#' && !Character.isLetterOrDigit( (char)ch2) ) 
                    { 
                        tmpBuf.append("&amp;"); break; 
                    }
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
        
        return m_currentElement;
    }


    /**
     *  Reads the stream until it meets one of the specified
     *  ending characters, or stream end.  The ending character will be left
     *  in the stream.
     */
    private String readUntil( String endChars )
        throws IOException
    {
        StringBuffer sb = new StringBuffer( 80 );
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
        StringBuffer sb = new StringBuffer( 80 );
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
            WikiContext dummyContext = new WikiContext( m_engine, 
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
     */
    private String makeHeadingAnchor( String baseName, String title, Heading hd )
    {
        hd.m_titleText = title;
        title = MarkupParser.cleanLink( title );
        hd.m_titleSection = m_engine.encodeName(title);
        hd.m_titleAnchor = "section-"+m_engine.encodeName(baseName)+
                           "-"+hd.m_titleSection;
        
        hd.m_titleAnchor = hd.m_titleAnchor.replace( '%', '_' );
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
            log.fatal("CleanTranslator not working", e);
            throw new InternalWikiException("CleanTranslator not working as expected, when cleaning title"+ e.getMessage() );
        }

        return outTitle;
    }
    
    /**
     *  Returns XHTML for the start of the heading.  Also sets the
     *  line-end emitter.
     *  @param level 
     *  @param headings A List to which heading should be added.
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
            makeLink( READ, matchedLink, wikiname, null );
        }
        else
        {
            makeLink( EDIT, wikiname, wikiname, null );
        }

        return m_currentElement;
    }

    private Element outlinkImage()
    {
        Element el = null;
        
        if( m_useOutlinkImage )
        {
            el = new Element("img").setAttribute("class", "outlink");
            el.setAttribute( "src", m_context.getURL( WikiContext.NONE,"images/out.png" ) );
            el.setAttribute("alt","");
        }

        return el;
    }

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
            result = handleImageLink( url, url, false );
        }
        else
        {
            result = makeLink( EXTERNAL, url, url,null );
            addElement( outlinkImage() );
        }

        if( last != null )
            m_plainTextBuf.append(last);
        
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
        String matchedLink;

        if( isExternalLink( link ) && hasLinkText )
        {
            return makeLink( IMAGELINK, reallink, link, null );
        }
        else if( (matchedLink = linkExists( possiblePage )) != null &&
                 hasLinkText )
        {
            // System.out.println("Orig="+link+", Matched: "+matchedLink);
            callMutatorChain( m_localLinkMutatorChain, possiblePage );
            
            return makeLink( IMAGEWIKILINK, reallink, link, null );
        }
        else
        {
            return makeLink( IMAGE, reallink, link, null );
        }
    }

    private Element handleAccessRule( String ruleLine )
    {
        if( !m_parseAccessRules ) return m_currentElement;
        Acl acl;
        WikiPage          page = m_context.getPage();
        // UserDatabase      db = m_context.getEngine().getUserDatabase();

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
            return makeError( wse.getMessage() );
        }

        return m_currentElement;
    }

    /**
     *  Handles metadata setting [{SET foo=bar}]
     */
    private Element handleMetadata( String link )
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
            return makeError(" Invalid SET found: "+link);
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
     *  Emits a processing instruction that will enable markup escaping.
     *
     */
    private void enableOutputEscaping()
    {
        addElement( new ProcessingInstruction(Result.PI_ENABLE_OUTPUT_ESCAPING, "") );
    }
    
    /**
     *  Gobbles up all hyperlinks that are encased in square brackets.
     */
    private Element handleHyperlinks( String link )
    {
        StringBuffer sb        = new StringBuffer(link.length()+80);
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
            try
            {
                Content pluginContent = m_engine.getPluginManager().parsePluginLine( m_context, link );

                addElement( pluginContent );
            }
            catch( PluginException e )
            {
                log.info( "Failed to insert plugin", e );
                log.info( "Root cause:",e.getRootThrowable() );
                return addElement( makeError("Plugin insertion failed: "+e.getMessage()) );
            }
            
            return m_currentElement;
        }

        // link = TextUtil.replaceEntities( link );

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
            Content el = new VariableContent(link);

            addElement( el );
        }
        else if( isExternalLink( reallink ) )
        {
            // It's an external link, out of this Wiki

            callMutatorChain( m_externalLinkMutatorChain, reallink );

            if( isImageLink( reallink ) )
            {
                handleImageLink( reallink, link, (cutpoint != -1) );
            }
            else
            {
                makeLink( EXTERNAL, reallink, link, null );
                addElement( outlinkImage() );
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

                if( isImageLink(urlReference) )
                {
                    handleImageLink( urlReference, link, cutpoint != -1 );
                }
                else
                {
                    makeLink( INTERWIKI, urlReference, link, null );
                }
                
                if( isExternalLink(urlReference) )
                {
                    addElement( outlinkImage() );
                }
            }
            else
            {
                addElement( makeError("No InterWiki reference defined in properties for Wiki called '"+extWiki+"'!)") );
            }
        }
        else if( reallink.startsWith("#") )
        {
            // It defines a local footnote
            makeLink( LOCAL, reallink, link, null );
        }
        else if( TextUtil.isNumber( reallink ) )
        {
            // It defines a reference to a local footnote
            makeLink( LOCALREF, reallink, link, null );
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
                    makeLink( ATTACHMENT, attachment, link, null );
                }
            }
            else if( (hashMark = reallink.indexOf('#')) != -1 )
            {
                // It's an internal Wiki link, but to a named section

                String namedSection = reallink.substring( hashMark+1 );
                reallink = reallink.substring( 0, hashMark );

                reallink     = MarkupParser.cleanLink( reallink );

                callMutatorChain( m_localLinkMutatorChain, reallink );

                String matchedLink;
                if( (matchedLink = linkExists( reallink )) != null )
                {
                    String sectref = "section-"+m_engine.encodeName(matchedLink)+"-"+namedSection;
                    sectref = sectref.replace('%', '_');
                    makeLink( READ, matchedLink, link, sectref );
                }
                else
                {
                    makeLink( EDIT, reallink, link, null );
                }
            }
            else
            {
                // It's an internal Wiki link
                reallink = MarkupParser.cleanLink( reallink );

                callMutatorChain( m_localLinkMutatorChain, reallink );

                String matchedLink = linkExists( reallink );
                
                if( matchedLink != null )
                {
                    makeLink( READ, matchedLink, link, null );
                }
                else
                {
                    makeLink( EDIT, reallink, link, null );
                }
            }
        }

        return m_currentElement;
    }

    private String findAttachment( String link )
    {
        AttachmentManager mgr = m_engine.getAttachmentManager();
        Attachment att = null;

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

                return pushElement( new Element("span").setAttribute("style","font-family:monospace; whitespace:pre;") );
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

        if( el != null ) pushElement(el);
        
        return el;
    }

    /**
     *  Reads the stream until the next EOL or EOF.  Note that it will also read the
     *  EOL from the stream.
     */
    private StringBuffer readUntilEOL()
        throws IOException
    {
        int ch;
        StringBuffer buf = new StringBuffer( 256 );

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
                 // buf.append( m_renderer.openList(strBullets.charAt(m_genlistlevel++)) );

                 for( ; m_genlistlevel < numBullets; m_genlistlevel++ )
                 {
                     // bullets are growing, get from new bullet list
                     pushElement( new Element("li") );
                     // buf.append( m_renderer.openListItem() );
                     pushElement( new Element( getListType(strBullets.charAt(m_genlistlevel)) ));
                     // buf.append( m_renderer.openList(strBullets.charAt(m_genlistlevel)) );
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
                     // buf.append( m_renderer.closeList(m_genlistBulletBuffer.charAt(m_genlistlevel - 1)) );
                     
                     popElement( getListType(m_genlistBulletBuffer.charAt(m_genlistlevel-1)) );
                     if( m_genlistlevel > 0 ) 
                     {
                         // buf.append( m_renderer.closeListItem() );
                         popElement( "li" );
                     }

                 }
             }
             else
             {
                 if( m_genlistlevel > 0 ) 
                 {
                     popElement( "li" );
                     // buf.append( m_renderer.closeListItem() );
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
                 // buf.append( m_renderer.closeList( m_genlistBulletBuffer.charAt(m_genlistlevel - 1) ) );
                 if( m_genlistlevel > 0 ) 
                 {
                     //buf.append( m_renderer.closeListItem() );
                     popElement("li");
                 }
             }

             //rewind
             // buf.append( m_renderer.openList( strBullets.charAt(numEqualBullets++) ) );
             pushElement( new Element(getListType( strBullets.charAt(numEqualBullets++) ) ) );
             for(int i = numEqualBullets; i < numBullets; i++)
             {
                 pushElement( new Element("li") );
                 pushElement( new Element( getListType( strBullets.charAt(i) ) ) );
                 // buf.append( m_renderer.openListItem() );
                 // buf.append( m_renderer.openList( strBullets.charAt(i) ) );
             }
             m_genlistlevel = numBullets;
         }
         //buf.append( m_renderer.openListItem() );
         pushElement( new Element("li") );
         
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
        StringBuffer sb = new StringBuffer(40);
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

        return handleHyperlinks( sb.toString() );
    }

    /**
     *  Reads the stream until the current brace is closed or stream end. 
     */
    private String readBraceContent( char opening, char closing )
        throws IOException
    {
        StringBuffer sb = new StringBuffer(40);
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
     * @return
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
                    Boolean isSpan = (Boolean)m_styleStack.pop();
                
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
            style = StringEscapeUtils.unescapeHtml(style);
            if( style != null && style.indexOf("javascript:") != -1 )
            {
                log.debug("Attempt to output javascript within CSS:"+style);
                return addElement( makeError("Attempt to output javascript!") );
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

    private Element handleBar( boolean newLine )
        throws IOException
    {
        Element el = null;
        
        if( !m_istable && !newLine )
        {
            return null;
        }

        if( newLine )
        {
            if( !m_istable )
            {
                startBlockLevel();
                el = pushElement( new Element("table").setAttribute("class","wikitable") );
                m_istable = true;
                m_rowNum = 0;
            }

            m_rowNum++;
            Element tr = ( m_rowNum % 2 == 1 ) 
                       ? new Element("tr").setAttribute("class", "odd")
                       : new Element("tr");
            el = pushElement( tr );
            // m_closeTag = m_renderer.closeTableItem()+m_renderer.closeTableRow();
        }
        
        int ch = nextToken();

        if( ch == '|' )
        {
            if( !newLine ) 
            {
                el = popElement("th");
            }
            el = pushElement( new Element("th") );
        }
        else
        {
            if( !newLine ) 
            {
                el = popElement("td");
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

        if( ch == '|' || ch == '~' || ch == '\\' || ch == '*' || ch == '#' || 
            ch == '-' || ch == '!' || ch == '\'' || ch == '_' || ch == '[' ||
            ch == '{' || ch == ']' || ch == '}' || ch == '%' )
        {
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
        boolean newLine     = true; // FIXME: not true if reading starts in middle of buffer

        disableOutputEscaping();
        
        while(!quitReading)
        {
            int ch = nextToken();
            String s = null;
            Element el = null;
            
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
                else 
                {
                    m_plainTextBuf.append( (char) ch );
                }

                continue;
            }

            //
            //  An empty line stops a list
            //
            if( newLine && ch != '*' && ch != '#' && ch != ' ' && m_genlistlevel > 0 )
            {
                m_plainTextBuf.append(unwindGeneralList());
            }

            if( newLine && ch != '|' && m_istable )
            {
                el = popElement("table");
                m_istable = false;
            }

            //
            //  Now, check the incoming token.
            //
            switch( ch )
            {
              case '\r':
                // DOS linefeeds we forget
                continue;

              case '\n':
                //
                //  Close things like headings, etc.
                //

                // FIXME: This is not really very fast
                popElement("dl"); // Close definition lists.
                popElement("h2");
                popElement("h3");
                popElement("h4");
                if( m_istable ) 
                { 
                    popElement("tr");
                }
                
                m_isdefinition = false;

                if( newLine )
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
                    newLine = true;
                }
                continue;
                

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
                el = handleOpenbrace( newLine );
                break;

              case '}':
                el = handleClosebrace();
                break;

              case '-':
                if( newLine )
                    el = handleDash();

                break;

              case '!':
                if( newLine )
                {
                    el = handleHeading();
                }
                break;

              case ';':
                if( newLine )
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
                if( newLine )
                {
                    pushBack('*');
                    el = handleGeneralList();
                }
                break;

              case '#':
                if( newLine )
                {
                    pushBack('#');
                    el = handleGeneralList();
                }
                break;

              case '|':
                el = handleBar( newLine );
                break;

              case '~':
                el = handleTilde();
                break;

              case '%':
                el = handleDiv( newLine );
                break;

              case -1:
                quitReading = true;
                continue;
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
            if( el != null )
            {
                newLine = false;
            }
            else
            {
                m_plainTextBuf.append( (char) ch );
                newLine = false;
            }
            
            if( s != null )
            {
                m_plainTextBuf.append( s );
                newLine = false;
            }
        }
        
        popElement("domroot");
    }

    public WikiDocument parse()
        throws IOException
    {
        WikiDocument d = new WikiDocument( m_context.getPage() );
        Element rootElement = new Element("domroot");
        
        d.setRootElement( rootElement );
        try
        {
            fillBuffer( rootElement );
        }
        catch( IllegalDataException e )
        {
            log.error("Page "+m_context.getPage().getName()+" contained something that cannot be added in the DOM tree",e);
            throw new IOException("Illegal page data: "+e.getMessage());
        }
        
        return d;
    }
    

    /**
     *  Compares two Strings, and if one starts with the other, then
     *  returns null.  Otherwise just like the normal Comparator
     *  for strings.
     *  
     *  @author jalkanen
     *
     *  @since
     */
    private static class StartingComparator implements Comparator
    {
        public int compare( Object arg0, Object arg1 )
        {
            String s1 = (String)arg0;
            String s2 = (String)arg1;
            
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

