/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.plugin;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.providers.ProviderException;
import org.apache.log4j.Category;
import org.apache.oro.text.*;
import org.apache.oro.text.regex.*;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *  Builds an index of all pages.
 *  <P>Parameters</P>
 *  <UL>
 *    <LI>itemsPerLine: How many items should be allowed per line before break.
 *    <LI>include: Include only these pages.
 *    <LI>exclude: Exclude with this pattern.
 *  </UL>
 *
 *  @author Alain Ravet
 *  @author Janne Jalkanen
 *  @since 1.9.9
 */
public class IndexPlugin implements WikiPlugin
{
    protected static Category   log = Category.getInstance(IndexPlugin.class);

    public  static final String INITIALS_COLOR                  = "red" ;
    private static final int    DEFAULT_ITEMS_PER_LINE          = 4     ;

    private static final String PARAM_ITEMS_PER_LINE            = "itemsPerLine";
    private static final String PARAM_INCLUDE                   = "include";
    private static final String PARAM_EXCLUDE                   = "exclude";
    
    private int                 m_currentNofPagesOnLine         = 0;
    private int                 m_itemsPerLine;
    protected String            m_previousPageFirstLetter       = "";
    protected StringWriter      m_bodyPart      =   new StringWriter();
    protected StringWriter      m_headerPart    =   new StringWriter();
    private Pattern             m_includePattern;
    private Pattern             m_excludePattern;
    

    public String execute( WikiContext i_context , Map i_params )
        throws PluginException
    {
        //
        //  Parse arguments and create patterns.
        //
        PatternCompiler compiler = new GlobCompiler();
        m_itemsPerLine = TextUtil.parseIntParameter( (String) i_params.get(PARAM_ITEMS_PER_LINE),
                                                     DEFAULT_ITEMS_PER_LINE );
        try
        {
            String ptrn = (String) i_params.get(PARAM_INCLUDE);
            if( ptrn == null ) ptrn = "*";
            m_includePattern = compiler.compile(ptrn);

            ptrn = (String) i_params.get(PARAM_EXCLUDE);
            if( ptrn == null ) ptrn = "";
            m_excludePattern = compiler.compile(ptrn);
        }
        catch( MalformedPatternException e )
        {
            throw new PluginException("Illegal pattern detected."); // FIXME, make a proper error.
        }
        
        //
        //  Get pages, then sort.
        //
        
        final Collection        allPages      = getAllPagesSortedByName( i_context );
        final TranslatorReader  linkProcessor = new TranslatorReader( i_context, 
                                                                      new java.io.StringReader ( "" ) );

        //
        //  Build the page.
        //
        buildIndexPageHeaderAndBody( allPages, linkProcessor );

        return  m_headerPart.toString()
                +   "<br>"
                +   m_bodyPart.toString();
    }


    private void buildIndexPageHeaderAndBody ( final Collection i_allPages , 
                                               final TranslatorReader i_linkProcessor )
    {
        PatternMatcher matcher = new Perl5Matcher();
        
        for( Iterator i = i_allPages.iterator (); i.hasNext ();)
        {
            WikiPage curPage = (WikiPage) i.next();

            if( matcher.matches( curPage.getName(), m_includePattern ) )
            {
                if( !matcher.matches( curPage.getName(), m_excludePattern ) )
                {
                    ++m_currentNofPagesOnLine;
            
                    String    pageNameFirstLetter           = curPage.getName().substring(0,1).toUpperCase();
                    boolean   sameFirstLetterAsPreviousPage = m_previousPageFirstLetter.equals(pageNameFirstLetter);

                    if( !sameFirstLetterAsPreviousPage ) 
                    {
                        addLetterToIndexHeader( pageNameFirstLetter );
                        addLetterHeaderWithLine( pageNameFirstLetter );

                        m_currentNofPagesOnLine   = 1;
                        m_previousPageFirstLetter = pageNameFirstLetter;
                    }

                    addPageToIndex( curPage, i_linkProcessor );
                    breakLineIfTooLong();
                }
            }
        } // for
    }


    /**
     *  Gets all pages, then sorts them.
     */
    static Collection getAllPagesSortedByName( WikiContext i_context )
    {
        final WikiEngine engine = i_context.getEngine();

        final PageManager pageManager = engine.getPageManager();
        if( pageManager == null )
            return null;

        Collection result = new TreeSet( new Comparator() {
            public int compare( Object o1, Object o2 )
            {
                if( o1 == null || o2 == null ) { return 0; }

                WikiPage page1 = (WikiPage)o1,
                         page2 = (WikiPage)o2;

                return page1.getName().compareTo( page2.getName() );
            }
        });

        try 
        {
            Collection allPages = pageManager.getAllPages();
            result.addAll( allPages );
        }
        catch( ProviderException e ) 
        {
            log.fatal("PageProvider is unable to list pages: ", e);
        }

        return result;
    }


    private void addLetterToIndexHeader( final String i_firstLetter )
    {
        final boolean noLetterYetInTheIndex = ! "".equals(m_previousPageFirstLetter);

        if( noLetterYetInTheIndex ) 
        {
            m_headerPart.write(" - " );
        }

        m_headerPart.write("<A href=\"#"  + i_firstLetter + "\">" + i_firstLetter + "</A>" );
    }


    // FIXME: Should really use <SPAN> instead of <FONT>.
    private void addLetterHeaderWithLine( final String i_firstLetter )
    {
        m_bodyPart.write("<br /><br />" +
                         "<A name=\"" + i_firstLetter + "\">" +
                         "<font color="+INITIALS_COLOR+">"+i_firstLetter+"</A></font>" +
                         "<hr>" );
    }

    protected void addPageToIndex( WikiPage i_curPage, final TranslatorReader i_linkProcessor )
    {
        final boolean notFirstPageOnLine = 2 <= m_currentNofPagesOnLine;

        if( notFirstPageOnLine ) 
        {
            m_bodyPart.write(",&nbsp;&nbsp;"); 
        }
        m_bodyPart.write( i_linkProcessor.makeLink( TranslatorReader.READ, 
                                                    i_curPage.getName(), 
                                                    i_curPage.getName () ));
    }

    protected void breakLineIfTooLong()
    {
        final boolean limitReached = (m_itemsPerLine == m_currentNofPagesOnLine);

        if( limitReached ) 
        {
            m_bodyPart.write( "<br />" );
            m_currentNofPagesOnLine = 0;
        }
    }

}
