/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import org.apache.log4j.Category;
import org.apache.oro.text.*;
import org.apache.oro.text.regex.*;

import com.ecyrd.jspwiki.plugin.PluginManager;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.attachment.AttachmentManager;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.acl.AccessControlList;
import com.ecyrd.jspwiki.acl.AclImpl;
import com.ecyrd.jspwiki.acl.AclEntryImpl;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;
import com.ecyrd.jspwiki.auth.UserProfile;
import com.ecyrd.jspwiki.auth.UserManager;
import java.security.acl.AclEntry;
import java.security.acl.NotOwnerException;
import java.security.Principal;

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

    /** Allow this many characters to be pushed back in the stream.  In effect,
        this limits the size of a single heading line.  */
    private static final int              PUSHBACK_BUFFER_SIZE = 512;
    private PushbackReader m_in;

    private StringReader   m_data = new StringReader("");

    private static Category log = Category.getInstance( TranslatorReader.class );

    //private boolean        m_iscode       = false;
    private boolean        m_isbold       = false;
    private boolean        m_isitalic     = false;
    private boolean        m_isTypedText  = false;
    private boolean        m_istable      = false;
    private boolean        m_isPre        = false;
    private boolean        m_isPreSpan    = false;
    private boolean        m_isEscaping   = false;
    private boolean        m_isdefinition = false;
    private int            m_listlevel    = 0;
    private int            m_numlistlevel = 0;
    private boolean        m_isOpenParagraph = false;

    /** Tag that gets closed at EOL. */
    private String         m_closeTag     = null; 

    private WikiEngine     m_engine;
    private WikiContext    m_context;
    
    /** Optionally stores internal wikilinks */
    private ArrayList      m_localLinkMutatorChain    = new ArrayList();
    private ArrayList      m_externalLinkMutatorChain = new ArrayList();
    private ArrayList      m_attachmentLinkMutatorChain = new ArrayList();

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

    /** If true, then considers CamelCase links as well. */
    private boolean                m_camelCaseLinks      = false;

    /** If true, consider URIs that have no brackets as well. */
    // FIXME: Currently reserved, but not used.
    private boolean                m_plainUris           = false;

    /** If true, all outward links use a small link image. */
    private boolean                m_useOutlinkImage     = true;

    /** If true, allows raw HTML. */
    private boolean                m_allowHTML           = false;

    private PatternMatcher         m_matcher  = new Perl5Matcher();
    private PatternCompiler        m_compiler = new Perl5Compiler();
    private Pattern                m_camelCasePtrn;

    private HTMLRenderer           m_renderer = new HTMLRenderer();

    /**
     *  The default inlining pattern.  Currently "*.png"
     */
    public static final String     DEFAULT_INLINEPATTERN = "*.png";

    /**
     *  These characters constitute word separators when trying
     *  to find CamelCase links.
     */
    private static final String    WORD_SEPARATORS = ",.|;+=&()";

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

    private boolean m_parseAccessRules = true;

    public void disableAccessRules()
    {
        m_parseAccessRules = false;
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
        for( Iterator i = m_inlineImagePatterns.iterator(); i.hasNext(); )
        {
            if( m_inlineMatcher.matches( link, (Pattern) i.next() ) )
                return true;
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
            result = makeLink( EXTERNAL, url, url ) + outlinkImage();
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

    /**
     *  If outlink images are turned on, returns a link to the outward
     *  linking image.
     */
    private final String outlinkImage()
    {
        if( m_useOutlinkImage )
        {
            return "<img class=\"outlink\" src=\""+m_engine.getBaseURL()+"images/out.png\" alt=\"\" />";
        }

        return "";
    }

    private String handleAccessRule( String ruleLine )
    {
        if( !m_parseAccessRules ) return "";
        WikiPage          page = m_context.getPage();
        AccessControlList acl  = page.getAcl();
        UserManager       mgr  = m_context.getEngine().getUserManager();

        if( acl == null )
        {            
            acl = new AclImpl();            
        }

        if( ruleLine.startsWith( "{" ) )
            ruleLine = ruleLine.substring( 1 );
        if( ruleLine.endsWith( "}" ) )
            ruleLine = ruleLine.substring( 0, ruleLine.length() - 1 );

        log.debug("page="+page.getName()+", ACL = "+ruleLine);

        try
        {
            StringTokenizer fieldToks = new StringTokenizer( ruleLine );
            String policy  = fieldToks.nextToken();
            String chain   = fieldToks.nextToken();            

            while( fieldToks.hasMoreTokens() )
            {
                String roleOrPerm = fieldToks.nextToken( "," ).trim();
                boolean isNegative = true;

                Principal principal = mgr.getPrincipal( roleOrPerm );

                if( policy.equals("ALLOW") ) isNegative = false;

                AclEntry oldEntry = acl.getEntry( principal, isNegative );

                if( oldEntry != null )
                {
                    log.debug("Adding to old acl list: "+principal+", "+chain);
                    oldEntry.addPermission( WikiPermission.newInstance( chain ) );
                }
                else
                {
                    log.debug("Adding new acl entry for "+chain);
                    AclEntry entry = new AclEntryImpl();
            
                    entry.setPrincipal( principal );
                    if( isNegative ) entry.setNegativePermissions();
                    entry.addPermission( WikiPermission.newInstance( chain ) );
                    
                    acl.addEntry( principal, entry );
                }
            }

            page.setAcl( acl );

            log.debug( acl.toString() );
        }
        catch( NoSuchElementException nsee )
        {
            log.warn( "Invalid access rule: " + ruleLine + " - defaults will be used." );
            return m_renderer.makeError("Invalid access rule: "+ruleLine);
        }
        catch( NotOwnerException noe )
        {
            throw new InternalWikiException("Someone has implemented access control on access control lists without telling me.");
        }
        catch( IllegalArgumentException iae )
        {
            return m_renderer.makeError("Invalid permission type: "+ruleLine);
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
            String included;
            try
            {
                included = m_engine.getPluginManager().execute( m_context, link );
            }
            catch( PluginException e )
            {
                log.error( "Failed to insert plugin", e );
                log.error( "Root cause:",e.getRootThrowable() );
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
                sb.append( outlinkImage() );
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
                    sb.append( outlinkImage() );
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
                    attachment = m_engine.getAttachmentURL(attachment);
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

                namedSection = cleanLink( namedSection );
                reallink     = cleanLink( reallink );

                callMutatorChain( m_localLinkMutatorChain, reallink );

                String matchedLink;
                if( (matchedLink = linkExists( reallink )) != null )
                {
                    matchedLink += "#section-"+matchedLink+"-"+namedSection;
                    sb.append( makeLink( READ, matchedLink, link ) );
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
            buf.append("</b>");
            m_isbold = false;
        }

        if( m_isitalic )
        {
            buf.append("</i>");
            m_isitalic = false;
        }

        if( m_isTypedText )
        {
            buf.append("</tt>");
            m_isTypedText = false;
        }

        for( ; m_listlevel > 0; m_listlevel-- )
        {
            buf.append( "</ul>\n" );
        }

        for( ; m_numlistlevel > 0; m_numlistlevel-- )
        {
            buf.append( "</ol>\n" );
        }

        if( m_isPre ) 
        {
            buf.append("</pre>\n");
	    m_isEscaping   = false;
            m_isPre = false;
        }

        if( m_isPreSpan ) 
        {
            buf.append("</span>");
	    m_isEscaping   = false;
            m_isPreSpan = false;
        }

        if( m_istable )
        {
            buf.append( "</table>" );
            m_istable = false;
        }

	if( m_isOpenParagraph )
	{
	    buf.append("</p>");
	    m_isOpenParagraph = false;
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
                return "<br clear=\"all\" />";
            }
           
            pushBack( ch2 );

            return "<br />";
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
            res      = m_isbold ? "</b>" : "<b>";
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
            res        = m_isitalic ? "</i>" : "<i>";
            m_isitalic = !m_isitalic;
        }
        else
        {
            m_in.unread( ch );
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
                if( isBlock )
                {
                    res = "<pre>";
                    m_isPre = true;
                }
                else
                {
                    res = "<span style=\"font-family:monospace; whitespace:pre;\">";
                    m_isPreSpan = true;
                }
		m_isEscaping = true;
            }
            else
            {
                pushBack( ch2 );
                
                res = "<tt>";
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
                    res = "</pre>";
                }
		else if( m_isPreSpan )
                {
                    m_isPreSpan = false;
		    m_isEscaping = false;
                    res = "</span>";
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
                    res = "</tt>";
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
                    return m_renderer.makeRuler();
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

        if( ch == '!' )
        {
            int ch2 = nextToken();

            if( ch2 == '!' )
            {
                String title = cleanLink( peekAheadLine() );
                
                buf.append( m_renderer.makeHeading( HTMLRenderer.HEADING_LARGE, title ) );
            }
            else
            {
                pushBack( ch2 );
                String title = cleanLink( peekAheadLine() );
                buf.append( m_renderer.makeHeading( HTMLRenderer.HEADING_MEDIUM, title ) );
            }
        }
        else
        {
            pushBack( ch );
            String title = cleanLink( peekAheadLine() );
            buf.append( m_renderer.makeHeading( HTMLRenderer.HEADING_SMALL, title ) );
        }
        
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
            buf.append("</li>\n");
        }

        int numBullets = countChars( m_in, '*' ) + 1;        

        if( numBullets > m_listlevel )
        {
            for( ; m_listlevel < numBullets; m_listlevel++ )
                buf.append("<ul>\n");
        }
        else if( numBullets < m_listlevel )
        {
            for( ; m_listlevel > numBullets; m_listlevel-- )
                buf.append("</ul>\n");
        }
                
        buf.append("<li>");

        return buf.toString();
    }

    private String handleOrderedList()
        throws IOException
    {
        StringBuffer buf = new StringBuffer();

        if( m_numlistlevel > 0 )
        {
            buf.append("</li>\n");
        }

        int numBullets = countChars( m_in, '#' ) + 1;
                
        if( numBullets > m_numlistlevel )
        {
            for( ; m_numlistlevel < numBullets; m_numlistlevel++ )
                buf.append("<ol>\n");
        }
        else if( numBullets < m_numlistlevel )
        {
            for( ; m_numlistlevel > numBullets; m_numlistlevel-- )
                buf.append("</ol>\n");
        }
                
        buf.append("<li>");

        return buf.toString();

    }

    private String handleDefinitionList()
        throws IOException
    {
        if( !m_isdefinition )
        {
            m_isdefinition = true;

            m_closeTag = "</dd>\n</dl>";

            return "<dl>\n<dt>";
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
            log.info("Warning: unterminated link detected!");
            return sb.toString();
        }

        return handleHyperlinks( sb.toString() );
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

            //
            //  Style or class?
            //
            if( ch == '(' )
            {                
                style = readUntil( ")" );
                nextToken(); // Pop the ) from the list, too.
            }
            else if( Character.isLetter( (char) ch ) )
            {
                pushBack( ch );
                clazz = readUntil( " \t\n\r" );
            }
            else
            {
                //
                // Anything else stops.
                //
                /*
                if( m_isOpenParagraph ) 
                { 
                    sb.append("</p>\n"); 
                    m_isOpenParagraph=false; 
                }
                */
                sb.append( "\n</div>\n" );

                return sb.toString();
            }

            // sb.append( newLine ? "<div" : "<span" );

            sb.append( "<div" );
            sb.append( style != null ? " style=\""+style+"\"" : "" );
            sb.append( clazz != null ? " class=\""+clazz+"\"" : "" );
            sb.append( ">" );

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
                sb.append("<table class=\"wikitable\" border=\"1\">\n");
                m_istable = true;
            }

            sb.append("<tr>");
            m_closeTag = "</td></tr>";
        }
        
        int ch = nextToken();

        if( ch == '|' )
        {
            if( !newLine ) 
            {
                sb.append("</th>");
            }
            sb.append("<th>");
            m_closeTag = "</th></tr>";
        }
        else
        {
            if( !newLine ) 
            {
                sb.append("</td>");
            }
            sb.append("<td>");
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

        if( ch == '~' ) return "~"; // Escapes itself.

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
                else if( ch == '<' )
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
                else if( ch == -1 )
                {
                    quitReading = true;
                }
                else 
                {
                    buf.append( (char)ch );
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
                                start = buf.toString().lastIndexOf( potentialLink );

                                String link = readUntil(" \t()[]{}!\"'\n|");

                                link = potentialLink + (char)ch + link; // Do not forget the start.

                                buf.replace( start,
                                             start + potentialLink.length(),
                                             makeDirectURILink( link ) );

                                // System.out.println("Resulting with "+buf);

                                ch = nextToken();
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
            //  Check if any lists need closing down.
            //

            if( newLine && ch != '*' && ch != ' ' && m_listlevel > 0 )
            {
                buf.append("</li>\n");
                for( ; m_listlevel > 0; m_listlevel-- )
                {
                    buf.append("</ul>\n");
                }
            }

            if( newLine && ch != '#' && ch != ' ' && m_numlistlevel > 0 )
            {
                buf.append("</li>\n");
                for( ; m_numlistlevel > 0; m_numlistlevel-- )
                {
                    buf.append("</ol>\n");
                }
            }

            if( newLine && ch != '|' && m_istable )
            {
                buf.append("</table>\n");
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
		    if( m_isOpenParagraph )
			buf.append("</p>");

                    buf.append("<p>\n");
		    m_isOpenParagraph = true;
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
                    s = "</dt><dd>";
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
     *  All HTML output stuff is here.
     */

    // FIXME: Not everything is yet, and in the future this class will be spawned
    //        out to be its own class.
    private class HTMLRenderer
    {
        public static final int HEADING_SMALL  = 1;
        public static final int HEADING_MEDIUM = 2;
        public static final int HEADING_LARGE  = 3;
        /**
         *  Write a HTMLized link depending on its type.
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

            switch(type)
            {
              case READ:
                result = "<a class=\"wikipage\" href=\""+m_engine.getViewURL(link)+"\">"+text+"</a>";
                break;

              case EDIT:
                result = "<u>"+text+"</u><a href=\""+m_engine.getEditURL(link)+"\">?</a>";
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
                result = "<a href=\""+text+"\"><img class=\"inline\" src=\""+link+"\" /></a>";
                break;

              case IMAGEWIKILINK:
                String pagelink = m_engine.getViewURL(text);
                result = "<a class=\"wikipage\" href=\""+pagelink+"\"><img class=\"inline\" src=\""+link+"\" alt=\""+text+"\" /></a>";
                break;

              case EXTERNAL:
                result = "<a class=\"external\" href=\""+link+"\">"+text+"</a>";
                break;
                
              case INTERWIKI:
                result = "<a class=\"interwiki\" href=\""+link+"\">"+text+"</a>";
                break;

              case ATTACHMENT:
                String attlink = m_engine.getAttachmentURL( link );
                result = "<a class=\"attachment\" href=\""+attlink+"\">"+text+"</a>"+
                         "<a href=\""+m_engine.getBaseURL()+"PageInfo.jsp?page="+encodedlink+
                         "\"><img src=\""+m_engine.getBaseURL()+"images/attachment_small.png\" border=\"0\" /></a>";
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

        private String makeHeadingAnchor( String baseName, String title )
        {
            return "<a name=\"section-"+baseName+"-"+m_engine.encodeName(title)+"\">";
        }

        /**
         *  Returns XHTML for the start of the heading.  Also sets the
         *  line-end emitter.
         *  @param level 
         */ 
        public String makeHeading( int level, String title )
        {
            String res = "";

            String pageName = m_context.getPage().getName();

            switch( level )
            {
              case HEADING_SMALL:
                res = "<h4>"+makeHeadingAnchor( pageName, title );
                m_closeTag = "</a></h4>";
                break;

              case HEADING_MEDIUM:
                res = "<h3>"+makeHeadingAnchor( pageName, title );
                m_closeTag = "</a></h3>";
                break;

              case HEADING_LARGE:
                res = "<h2>"+makeHeadingAnchor( pageName, title );
                m_closeTag = "</a></h2>";
                break;
            }

            return res;
        }

    } // HTMLRenderer
}
