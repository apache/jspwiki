/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki;

import java.io.*;
import java.util.*;
import java.text.*;

import org.apache.log4j.Category;
import org.apache.oro.text.*;
import org.apache.oro.text.regex.*;

import com.ecyrd.jspwiki.plugin.PluginManager;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.auth.AccessRuleSet;
import com.ecyrd.jspwiki.attachment.AttachmentManager;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.providers.ProviderException;

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
    private static final int              ATTACHMENT    = 10;
    private static final int              ATTACHMENTIMAGE = 11;

    /** Allow this many characters to be pushed back in the stream. */
    private static final int              PUSHBACK_BUFFER_SIZE = 8;
    private PushbackReader m_in;

    private StringReader   m_data = new StringReader("");

    private static Category log = Category.getInstance( TranslatorReader.class );

    //private boolean        m_iscode       = false;
    private boolean        m_isbold       = false;
    private boolean        m_isitalic     = false;
    private boolean        m_isTypedText  = false;
    private boolean        m_istable      = false;
    private boolean        m_isPre        = false;
    private boolean        m_isdefinition = false;
    private int            m_listlevel    = 0;
    private int            m_numlistlevel = 0;

    /** Tag that gets closed at EOL. */
    private String         m_closeTag     = null; 

    private WikiEngine     m_engine;
    private WikiContext    m_context;
    
    /** Optionally stores internal wikilinks */
    private ArrayList      m_localLinkMutatorChain    = new ArrayList();
    private ArrayList      m_externalLinkMutatorChain = new ArrayList();

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

    /** If true, then considers CamelCase links as well. */
    private boolean                m_camelCaseLinks      = false;

    /** If true, consider URIs that have no brackets as well. */
    // FIXME: Currently reserved, but not used.
    private boolean                m_plainUris           = false;

    /** If true, all outward links use a small link image. */
    private boolean                m_useOutlinkImage     = false;

    private PatternMatcher         m_matcher  = new Perl5Matcher();
    private PatternCompiler        m_compiler = new Perl5Compiler();
    private Pattern                m_camelCasePtrn;

    /** The default inlining pattern.  Currently "*.png" */
    public static final String     DEFAULT_INLINEPATTERN = "*.png";

    /** Container for AccessRule collection. */
    private AccessRuleSet m_accessRules;

    /** Flag to disable plugin expansion. */
    private boolean m_pluginsEnabled = true;

    /**
     *  These characters constitute word separators when trying
     *  to find CamelCase links.
     */
    private static final String    WORD_SEPARATORS = ",.|:;+=&";

    /**
     *  @param engine The WikiEngine this reader is attached to.  Is
     * used to figure out of a page exits.
     */

    // FIXME: TranslatorReaders should be pooled for better performance.
    public TranslatorReader( WikiContext context, Reader in )
    {
        PatternCompiler compiler         = new GlobCompiler();
        ArrayList       compiledpatterns = new ArrayList();

        m_in     = new PushbackReader( new BufferedReader( in ),
                                       PUSHBACK_BUFFER_SIZE );
        m_engine = context.getEngine();
        m_context = context;
        m_accessRules = new AccessRuleSet();

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

        m_camelCaseLinks      = "true".equals( props.getProperty( PROP_CAMELCASELINKS, "false" ) );
        m_plainUris           = "true".equals( props.getProperty( PROP_PLAINURIS, "false" ) );
        m_useOutlinkImage     = "true".equals( props.getProperty( PROP_USEOUTLINKIMAGE, "false" ) );
    }

    /**
     * Enables or disables plugin expansion. By default, plugins are enabled.
     */
    public void enablePlugins( boolean expand )
    {
        m_pluginsEnabled = expand;
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
     * Returns the access rules that have been collected when the 
     * wiki text was parsed (read()).
     */
    public AccessRuleSet getAccessRules()
    {
        return( m_accessRules );
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
        return m_engine.getFinalPageName( page );
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
        String result;

        if( text == null ) text = link;

        // Make sure we make a link name that can be accepted
        // as a valid URL.

        String encodedlink = m_engine.encodeName( link );

        if( encodedlink.length() == 0 )
        {
            type = EMPTY;
        }

        text = callMutatorChain( m_linkMutators, text );

        switch(type)
        {
          case READ:
            result = "<A CLASS=\"wikipage\" HREF=\""+m_engine.getBaseURL()+"Wiki.jsp?page="+encodedlink+"\">"+text+"</A>";
            break;

          case EDIT:
            result = "<U>"+text+"</U><A HREF=\""+m_engine.getBaseURL()+"Edit.jsp?page="+encodedlink+"\">?</A>";
            break;

          case EMPTY:
            result = "<U>"+text+"</U>";
            break;

            //
            //  These two are for local references - footnotes and 
            //  references to footnotes.
            //  We embed the page name (or whatever WikiContext gives us)
            //  to make sure the links are unique across Wiki.
            //
          case LOCALREF:
            result = "<A CLASS=\"footnoteref\" HREF=\"#ref-"+
                m_context.getPage().getName()+"-"+
                link+"\">["+text+"]</A>";
            break;

          case LOCAL:
            result = "<A CLASS=\"footnote\" NAME=\"ref-"+
                m_context.getPage().getName()+"-"+
                link.substring(1)+"\">["+text+"]</A>";
            break;

            //
            //  With the image, external and interwiki types we need to
            //  make sure nobody can put in Javascript or something else
            //  annoying into the links themselves.  We do this by preventing
            //  a haxor from stopping the link name short with quotes in 
            //  fillBuffer().
            //
          case IMAGE:
            result = "<IMG CLASS=\"inline\" SRC=\""+link+"\" ALT=\""+text+"\" />";
            break;

          case IMAGELINK:
            result = "<A HREF=\""+text+"\"><IMG CLASS=\"inline\" SRC=\""+link+"\" /></A>";
            break;

          case IMAGEWIKILINK:
            String pagelink = m_engine.getBaseURL()+"Wiki.jsp?page="+text;
            result = "<A CLASS=\"wikipage\" HREF=\""+pagelink+"\"><IMG CLASS=\"inline\" SRC=\""+link+"\" ALT=\""+text+"\" /></A>";
            break;

          case EXTERNAL:
            result = "<A CLASS=\"external\" HREF=\""+link+"\">"+text+"</A>";
            break;

          case INTERWIKI:
            result = "<A CLASS=\"interwiki\" HREF=\""+link+"\">"+text+"</A>";
            break;

          case ATTACHMENT:
            result = "<a class=\"attachment\" href=\""+m_engine.getBaseURL()+
                     "attach?page="+link+"\">"+text+"</a>"+
                     "<a href=\""+m_engine.getBaseURL()+"PageInfo.jsp?page="+link+
                     "\"><img src=\"images/attachment_small.png\" border=\"0\" /></a>";
            break;

          default:
            result = "";
            break;
        }

        return result;
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
            if( !(Character.isLetterOrDigit(clean.charAt(i)) ||
                  clean.charAt(i) == '_' ||
                  clean.charAt(i) == '.') )
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
    private boolean isExternalLink( String link )
    {
        return link.startsWith("http:") || link.startsWith("ftp:") ||
            link.startsWith("https:") || link.startsWith("mailto:") ||
            link.startsWith("news:") || link.startsWith("file:");
    }

    /**
     *  Matches the given link to the list of image name patterns
     *  to determine whether it should be treated as an inline image
     *  or not.
     */
    private boolean isImageLink( String link )
    {
        for( Iterator i = m_inlineImagePatterns.iterator(); i.hasNext(); )
        {
            if( m_inlineMatcher.matches( link, (Pattern) i.next() ) )
                return true;
        }

        return false;
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
            String matchedLink;

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


    /**
     *  Gobbles up all hyperlinks that are encased in square brackets.
     */
    private String handleHyperlinks( String link )
    {
        StringBuffer sb        = new StringBuffer();
        String       reallink;
        int          cutpoint;

        // Start with access rules, to get them out of the way of plugins.
        if( AccessRuleSet.isAccessRule( link ) )
        {
            m_accessRules.addRule( link );
            return( null );
        }

        // Continue with plugin links. (Anything starting with [{ defaults to a plugin.)
        if( PluginManager.isPluginLink( link ) )
        {
            if( m_pluginsEnabled == false )
                return( null );

            String included;
            try
            {
                included = m_engine.getPluginManager().execute( m_context, link );
            }
            catch( PluginException e )
            {
                log.error( "Failed to insert plugin", e );
                log.error( "Root cause:",e.getRootThrowable() );
                included = "<FONT COLOR=\"#FF0000\">Plugin insertion failed: "+e.getMessage()+"</FONT>";
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
                value = "<FONT COLOR=\"#FF0000\">"+e.getMessage()+"</FONT>";
            }
            catch( IllegalArgumentException e )
            {
                value = "<FONT COLOR=\"#FF0000\">"+e.getMessage()+"</FONT>";
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

                if( m_useOutlinkImage )
                {
                    sb.append( "<img class=\"outlink\" src=\""+m_engine.getBaseURL()+"images/out.png\" alt=\"\" />" );
                }
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
            }
            else
            {
                sb.append( link+" <FONT COLOR=\"#FF0000\">(No InterWiki reference defined in properties for Wiki called '"+extWiki+"'!)</FONT>");
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
            //
            //  Internal wiki link, but is it an attachment link?
            //
            String attachment = findAttachment( reallink );
            if( attachment != null )
            {
                if( isImageLink( reallink ) )
                {
                    attachment = m_engine.getBaseURL()+"attach?page="+attachment;
                    sb.append( handleImageLink( attachment, link, (cutpoint != -1) ) );
                }
                else
                {
                    sb.append( makeLink( ATTACHMENT, attachment, link ) );
                }
            }
            else
            {
                // It's an internal Wiki link
                reallink = cleanLink( reallink );

                callMutatorChain( m_localLinkMutatorChain, reallink );

                String matchedLink;
                if( (matchedLink = linkExists( reallink )) != null )
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
            buf.append("</B>");
            m_isbold = false;
        }

        if( m_isitalic )
        {
            buf.append("</I>");
            m_isitalic = false;
        }

        if( m_isTypedText )
        {
            buf.append("</TT>");
            m_isTypedText = false;
        }

        for( ; m_listlevel > 0; m_listlevel-- )
        {
            buf.append( "</UL>\n" );
        }

        for( ; m_numlistlevel > 0; m_numlistlevel-- )
        {
            buf.append( "</OL>\n" );
        }

        if( m_isPre ) 
        {
            buf.append("</PRE>\n");
            m_isPre = false;
        }

        if( m_istable )
        {
            buf.append( "</TABLE>" );
            m_istable = false;
        }

        return buf.toString();
    }

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
        return m_in.read();
    }

    /**
     *  Push back any character to the current input.  Does not
     *  push back a read EOF, though.
     */
    private void pushBack( int c )
        throws IOException
    {        
        if( c != -1 )
        {
            m_in.unread( c );
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
                return "<BR clear=\"all\" />";
            }
           
            pushBack( ch2 );

            return "<BR />";
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
            res      = m_isbold ? "</B>" : "<B>";
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
            res        = m_isitalic ? "</I>" : "<I>";
            m_isitalic = !m_isitalic;
        }
        else
        {
            m_in.unread( ch );
        }

        return res;
    }

    private String handleOpenbrace()
        throws IOException
    {
        int ch = nextToken();
        String res = "{";

        if( ch == '{' )
        {
            int ch2 = nextToken();

            if( ch2 == '{' )
            {
                res = "<PRE>";
                m_isPre = true;
            }
            else
            {
                pushBack( ch2 );
                
                res = "<TT>";
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
                    res = "</PRE>";
                }
                else
                {
                    res = "}}}";
                }
            }
            else
            {
                pushBack( ch3 );

                if( !m_isPre )
                {
                    res = "</TT>";
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
                    return "<HR />";
                }
        
                pushBack( ch3 );
            }
            pushBack( ch2 );
        }

        pushBack( ch );

        return "-";
    }

    private String handleHeading()
        throws IOException
    {
        StringBuffer buf = new StringBuffer();

        int ch  = nextToken();

        if( ch == '!' )
        {
            int ch2 = nextToken();

            if( ch2 == '!' )
            {
                buf.append("<H2>");
                m_closeTag = "</H2>";
            }
            else
            {
                buf.append( "<H3>" );
                m_closeTag = "</H3>";
                pushBack( ch2 );
            }
        }
        else
        {
            buf.append( "<H4>" );
            m_closeTag = "</H4>";
            pushBack( ch );
        }
        
        return buf.toString();
    }

    /**
     *  Reads the stream until the next EOL or EOF.
     */
    private StringBuffer readUntilEOL()
        throws IOException
    {
        int ch;
        StringBuffer buf = new StringBuffer();

        while( true )
        {
            ch = nextToken();

            if( ch == -1 || ch == '\n' )
                break;

            buf.append( (char) ch );
        }

        return buf;
    }

    private int countChars( PushbackReader in, char c )
        throws IOException
    {
        int count = 0;
        int ch; 

        while( (ch = in.read()) != -1 )
        {
            if( (char)ch == c )
            {
                count++;
            }
            else
            {
                in.unread( ch );
                break;
            }
        }

        return count;
    }

    private String handleUnorderedList()
        throws IOException
    {
        StringBuffer buf = new StringBuffer();

        if( m_listlevel > 0 )
        {
            buf.append("</LI>\n");
        }

        int numBullets = countChars( m_in, '*' ) + 1;        

        if( numBullets > m_listlevel )
        {
            for( ; m_listlevel < numBullets; m_listlevel++ )
                buf.append("<UL>\n");
        }
        else if( numBullets < m_listlevel )
        {
            for( ; m_listlevel > numBullets; m_listlevel-- )
                buf.append("</UL>\n");
        }
                
        buf.append("<LI>");

        return buf.toString();
    }

    private String handleOrderedList()
        throws IOException
    {
        StringBuffer buf = new StringBuffer();

        if( m_numlistlevel > 0 )
        {
            buf.append("</LI>\n");
        }

        int numBullets = countChars( m_in, '#' ) + 1;
                
        if( numBullets > m_numlistlevel )
        {
            for( ; m_numlistlevel < numBullets; m_numlistlevel++ )
                buf.append("<OL>\n");
        }
        else if( numBullets < m_numlistlevel )
        {
            for( ; m_numlistlevel > numBullets; m_numlistlevel-- )
                buf.append("</OL>\n");
        }
                
        buf.append("<LI>");

        return buf.toString();

    }

    private String handleDefinitionList()
        throws IOException
    {
        if( !m_isdefinition )
        {
            m_isdefinition = true;

            m_closeTag = "</DD>\n</DL>";

            return "<DL>\n<DT>";
        }

        return ";";
    }

    private String handleOpenbracket()
        throws IOException
    {
        StringBuffer sb = new StringBuffer();
        int ch;

        while( (ch = nextToken()) == '[' )
        {
            sb.append( (char)ch );
        }

        pushBack( ch );

        if( sb.length() > 0 )
        {
            return sb.toString();
        }

        while( true )
        {
            ch = nextToken();

            if( ch == -1 || ch == ']' )
                break;

            sb.append( (char) ch );
        }

        if( ch == -1 )
        {
            log.info("Warning: unterminated link detected!");
            return sb.toString();
        }

        return handleHyperlinks( sb.toString() );
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
                sb.append("<TABLE CLASS=\"wikitable\" BORDER=\"1\">\n");
                m_istable = true;
            }

            sb.append("<TR>");
            m_closeTag = "</TD></TR>";
        }
        
        int ch = nextToken();

        if( ch == '|' )
        {
            if( !newLine ) 
            {
                sb.append("</TH>");
            }
            sb.append("<TH>");
            m_closeTag = "</TH></TR>";
        }
        else
        {
            if( !newLine ) 
            {
                sb.append("</TD>");
            }
            sb.append("<TD>");
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

        if( ch == '|' )
            return "|";

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
            //  Check if we're actually ending the preformatted mode.
            //  We still must do an entity transformation here.
            //
            if( m_isPre )
            {
                if( ch == '}' )
                {
                    buf.append( handleClosebrace() );
                }
                else if (ch == '<') 
                {
                    buf.append("&lt;");
                }
                else if (ch == '>') 
                {
                    buf.append("&gt;");
                }
                else 
                {
                    buf.append( (char)ch );
                }

                continue;
            }

            //
            //  Check if any lists need closing down.
            //

            if( newLine && ch != '*' && ch != ' ' && m_listlevel > 0 )
            {
                buf.append("</LI>\n");
                for( ; m_listlevel > 0; m_listlevel-- )
                {
                    buf.append("</UL>\n");
                }
            }

            if( newLine && ch != '#' && ch != ' ' && m_numlistlevel > 0 )
            {
                buf.append("</LI>\n");
                for( ; m_numlistlevel > 0; m_numlistlevel-- )
                {
                    buf.append("</OL>\n");
                }
            }

            if( newLine && ch != '|' && m_istable )
            {
                buf.append("</TABLE>\n");
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

                    buf.append("<P>\n");
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
                s = handleOpenbrace();
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
                    s = "</DT><DD>";
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
                    s = handleUnorderedList();
                }
                else
                {
                    s = "*";
                }
                break;

              case '#':
                if( newLine )
                {
                    s = handleOrderedList();
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
                s = "&lt;";
                break;

              case '>':
                s = "&gt;";
                break;

              case '\"':
                s = "&quot;";
                break;
                /*
              case '&':
                s = "&amp;";
                break;
                */
              case '~':
                s = handleTilde();
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
}
