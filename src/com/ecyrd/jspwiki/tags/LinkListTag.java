/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.tags;

import java.io.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import java.util.*;
import com.ecyrd.jspwiki.*;
import org.apache.log4j.*;


/**
 *
 * Utility tag for 
 * <UL>
 * <LI> Loading special reference information on WikiPages 
 * <LI> Producing HTML links from the results
 * </UL>
 *
 * @author  ebu@iki.fi
 * @version $Version:$
 */
public class LinkListTag extends TagSupport 
{

    /** Possible 'action' parameter values */
    public static final String ACTION_GETREFPAGES     = "getref";
    public static final String ACTION_GETUNREFPAGES   = "getunref";
    public static final String ACTION_GETMISSINGPAGES = "getmissing";

    //    private static final Category   log = Category.getInstance(LinkListTag.class);

    /** What action to take: getref, getunref, getmissing */
    private String action;

    /** Name of page (for getref action) */
    private String page;

    /** Separate list entries with HTML line break? */
    private String useSeparator;

    /** How many entries to return in the list. Zero means show all. */
    private int show = 0;

    /**

     */
    public void setAction( String act ) 
    {
        action = act;
    }

    /**

     */
    public void setPage( String p ) 
    {
        page = p;
    }

    /**

     */
    public void setLinesep( String p ) 
    {
        useSeparator = p;
    }

    /**

     */
    public void setShow( String p ) 
    {
        try
        {
            int num = Integer.parseInt( p );
            show = num;
        }
        catch( NumberFormatException e )
        {
            //log.error( "Invalid number for parameter "show" in LinkListTag." );
            show = 0;
        }
    }


    /**
     * Make sure that the tag has been initialized properly.
     */
    public int doStartTag() throws JspTagException 
    {
        // No default action, param 'action' is required.
        return SKIP_BODY;
    }

    /**
     * Write the parameter value to the page.
     */
    public int doEndTag() throws JspTagException 
    {
        try
        {
            JspWriter out = pageContext.getOut();
            
            // Get WikiEngine and its referenceManager 
            WikiEngine we = WikiEngine.getInstance( pageContext.getServletConfig() );
            ReferenceManager ref = we.getReferenceManager();
            
            if( ref == null )
            {
                //                    log.debug( "Empty or uninitialized reference manager received!" );
                out.println( "<BR>(no reference manager)" );
                return EVAL_PAGE;
            }

            // All ReferenceManager utilities return a Collection of Strings which are
            // WikiNames. Just get the right one and output.
            Collection links = null;
            if( ACTION_GETREFPAGES.equals( action ) )
                links = ref.findReferrers( page );
            else if( ACTION_GETUNREFPAGES.equals( action ) )
                links = ref.findUnreferenced();
            else if ( ACTION_GETMISSINGPAGES.equals( action ) )
                links = ref.findUncreated();

            boolean sep = false;
            if( useSeparator != null )
                sep = true;
                
            String result = wikitizeCollection( links, sep );
            String converted = we.textToHTML( new WikiContext( we, ""), result ); //FIXME
            out.println( converted );


        }
        catch( IOException e )
        {
            //            log.debug("LinkListTag.doEndTag() failed: " + e.getMessage());
        }

        return EVAL_PAGE;
    }

    /**
       Converts a Collection of Strings into WikiLinks, separated by line breaks.
       Returns a String. If the links were null or empty, returns an empty string.
    */
    private String wikitizeCollection( Collection links, boolean separate )
    {
        if(links == null || links.isEmpty() )
            return( "" );

        String sep = "";
        if( separate )
            sep = "\\\\";   // This is Wiki markup, see TranslatorReader.
        StringBuffer output = new StringBuffer();
        
        Iterator it = links.iterator();
        int count = 0;
        while( it.hasNext() && ( (count < show) || ( show == 0 ) ) )
        {
            String value = (String)it.next();
            // Make a Wiki markup link. See TranslatorReader.
            output.append( "[" + value + "]\n" + sep );
            count++;
        }

        return( output.toString() );
    }


} // EOF
