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
package com.ecyrd.jspwiki.plugin;

import org.apache.log4j.Category;
import com.ecyrd.jspwiki.*;
import java.util.*;

import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;

import java.awt.*;
import java.awt.image.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.ServletContext;
import javax.swing.ImageIcon;

import com.keypoint.PngEncoderB;

/**
 *  Plugin for aggregating go game rankings.  Probably of very little use to anyone,
 *  except perhaps as an example.
 *
 *  @author Janne Jalkanen
 */

// FIXME: Still alive, so very little documentation.

public class GoRankAggregator
    implements WikiPlugin
{
    private static Category log = Category.getInstance( GoRankAggregator.class );

    public void initialize( WikiContext context, Map params )
        throws PluginException
    {
    }

    private Collection getPersonsFromPage( WikiContext context, String pageName )
        throws PluginException,
               IOException
    {
        ArrayList list = new ArrayList();

        String text = context.getEngine().getPureText( pageName, WikiProvider.LATEST_VERSION );

        if( text == null || !context.getEngine().pageExists(pageName) )
        {
            throw new PluginException("No such page called '"+pageName+"' exists.");
        }

        //
        //  A ruler is used as the start marker.
        //
        int start = text.lastIndexOf("----");

        if( start == -1 )
        {
            throw new PluginException("Page '"+pageName+"' contains no ranking data (separator: ---- was missing.)");
        }

        BufferedReader in = new BufferedReader( new StringReader( text.substring(start+4) ) );
        String line;
        String club = "?";

        while( (line = in.readLine() ) != null )
        {
            String lastname  = null;
            String firstname = null;
            String rank      = null;

            line = line.trim();

            if( line.length() == 0 ) continue;

            if( line.startsWith("{{{") ) continue; // Skip this.

            if( line.startsWith("}}}") ) break; // Last line.

            if( line.startsWith("Club:") && line.length() > 5 )
            {
                club = line.substring( 5 ).trim();
                continue;
            }

            StringTokenizer st = new StringTokenizer( line, ", \t" );

            try
            {
                lastname  = st.nextToken();
                firstname = st.nextToken();
                rank      = st.nextToken();
            }
            catch( NoSuchElementException e )
            {
                throw new PluginException( "Page '"+pageName+"' contains invalid data on line '"+line+"'.");
            }

            UserInfo info    = new UserInfo();
            info.m_lastName  = lastname;
            info.m_firstName = firstname;
            info.m_rank      = rank;
            info.m_club      = club;

            list.add( info );
        }

        return list;
    }

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        StringBuffer sb = new StringBuffer();
        StringBuffer errors = new StringBuffer();

        TreeSet list = new TreeSet( new RankSorter() );

        String pages = (String) params.get( "pages" );

        if( pages == null )
        {
            return "ERROR: Parameter 'pages' is required.";
        }

        StringTokenizer st = new StringTokenizer( pages, ", \t" );

        while( st.hasMoreTokens() )
        {
            String pageName = st.nextToken();

            try
            {
                list.addAll( getPersonsFromPage( context, pageName ) );
            }
            catch( PluginException e )
            {
                errors.append( "<li>"+e.getMessage()+"</li>\n" );
            }
            catch( IOException e )
            {
                log.error("I/O error", e);
                errors.append( "<li>I/O Error: "+e.getMessage()+"</li>\n" );
            }
        }

        //
        //  List now contains a sorted set of persons.
        //  Write out the HTML for this.
        //

        int counter = 1;

        sb.append("<table border=1>\n");
        sb.append("<tr><th>Place</th><th>Name</th><th>Club</th><th>Rank</th></tr>");
        for( Iterator i = list.iterator(); i.hasNext(); )
        {            
            sb.append("<tr>\n");

            UserInfo ui = (UserInfo) i.next();
            sb.append("<td>"+counter+"</td>");
            sb.append("<td>"+ui.m_lastName+", "+ui.m_firstName+"</td>");
            sb.append("<td>"+ui.m_club+"</td>");
            sb.append("<td>"+ui.m_rank+"</td>");

            sb.append("</tr>\n");

            ++counter;
        }
        sb.append("</table>\n");

        if( errors.length() > 0 )
        {
            sb.append("<P>Following errors occurred:</P>\n<ul>");
            sb.append( errors );
            sb.append("</ul>\n");
        }

        return sb.toString();
    }

    protected class UserInfo
    {
        public String m_lastName;
        public String m_firstName;
        public String m_club;
        public String m_rank;
    }

    protected class RankSorter
        implements Comparator
    {
        /**
         *  Returns positive values for kyu ranks, negative for dan.
         */
        public int parseRank( String rank )
        {
            if( rank == null || rank.length() == 0 )
            {
                return 50;
            }

            try
            {
                int dan = rank.indexOf('d');
                int kyu = rank.indexOf('k');

                if( dan > 0 )
                {
                    return - Integer.parseInt( rank.substring(0,dan) );
                }
                else if( kyu > 0 )
                {
                    return Integer.parseInt( rank.substring(0,kyu) );
                }
                else
                {
                    return 50;
                }        
            }
            catch( NumberFormatException e )
            {
                return 50;
            }
        }

        public int compare( Object o1, Object o2 )
        {
            UserInfo u1 = (UserInfo)o1;
            UserInfo u2 = (UserInfo)o2;

            return parseRank(u1.m_rank) - parseRank(u2.m_rank);
        }
    }
}
