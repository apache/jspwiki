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

import java.io.StringReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;

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
    
    // FIXME:  This is now SHARED across all Wiki instances.  This is a HACK!
    // FIXME:  Make a engine-specific thing.
    private static List c_egfList = null;
    private static long c_lastEGFUpdate = -1;

    public static final long EGF_UPDATE_INTERVAL = 24*60*60*1000L;

    public void initialize( WikiContext context, Map params )
        throws PluginException
    {
    }

    /**
     *  Give the input as HTML, and this will parse it, returning a List
     *  of UserInfo nodes.
     */
    List parseEGFRatingsList( WikiContext context, String ratingsList )
        throws IOException
    {
        ArrayList list = new ArrayList();

        BufferedReader in = new BufferedReader( new StringReader(ratingsList) );

        String line;

        while( (line = in.readLine() ) != null )
        {
            if( line.trim().length() == 0 ) continue; // Skip empty lines.

            StringTokenizer tok = new StringTokenizer( line, " \t" );

            try
            {
                int    id    = Integer.parseInt(tok.nextToken().trim());
                String last  = tok.nextToken();
                String first = tok.nextToken();
                String club  = tok.nextToken();
                String grade = tok.nextToken();
                int    gor   = Integer.parseInt(tok.nextToken().trim());

                UserInfo info    = new UserInfo();
                info.m_lastName  = last;
                info.m_firstName = first;
                info.m_rank      = parseRank( grade );
                info.m_egfRank   = gor;

                list.add( info );
            }
            catch( NoSuchElementException e )
            {
                // Skip quietly.
            }
            catch( NumberFormatException e )
            {
                // Skip quietly, too.  This line is probably some HTML crap.
            }
        }

        return list;
    }

    List getPersonsFromPage( WikiContext context, String pageName )
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
            int    rank      = 50;

            line = line.trim();

            if( line.length() == 0 ) continue;

            if( line.startsWith("{{{") ) continue; // Skip this.

            if( line.startsWith("}}}") ) break; // Last line.

            if( line.startsWith("Club:") && line.length() > 5 )
            {
                club = line.substring( 5 ).trim();
                continue;
            }

            StringTokenizer st = new StringTokenizer( line, "," );

            try
            {
                lastname  = st.nextToken();
                firstname = st.nextToken();
                rank      = parseRank(st.nextToken());
            }
            catch( NoSuchElementException e )
            {
                throw new PluginException( "Page '"+pageName+"' contains invalid data on line '"+line+"'.");
            }
            catch( IllegalArgumentException e )
            {
                log.debug("Bad rank data: ",e);
                throw new PluginException( "Page '"+pageName+"' has bad rank data on line '"+line+"': "+e.getMessage());
            }

            UserInfo info    = new UserInfo();
            info.m_lastName  = lastname.trim();
            info.m_firstName = firstname.trim();
            info.m_rank      = rank;
            info.m_club      = club;

            list.add( info );
        }

        return list;
    }

    String printRank( int rank )
    {
        if( rank < 0 )
        {
            return -rank+" dan";
        }
        else
        {
            return rank+" kyu";
        }
    }

    /**
     *  Returns positive values for kyu ranks, negative for dan.
     */
    int parseRank( String rank )
        throws IllegalArgumentException
    {
        if( rank == null || rank.length() == 0 )
        {
            throw new IllegalArgumentException("Null name");
        }

        rank = rank.toLowerCase();

        try
        {
            int dan = rank.indexOf('d');
            int kyu = rank.indexOf('k');

            if( dan > 0 )
            {
                return - Integer.parseInt( rank.substring(0,dan).trim() );
            }
            else if( kyu > 0 )
            {
                return Integer.parseInt( rank.substring(0,kyu).trim() );
            }
            else
            {
                throw new IllegalArgumentException("Rank qualifier (kyu or dan) is missing: "+rank);
            }        
        }
        catch( NumberFormatException e )
        {
            throw new IllegalArgumentException("Bad number format: "+rank);
        }
    }

    private void fetchNewEGFList( WikiContext context, String url )
        throws MalformedURLException,
               IOException
    {
        synchronized( c_egfList )
        {
            log.info("Fetching new EGF ratings list from "+url);
            URL source = new URL( url );

            URLConnection conn      = source.openConnection();

            String        encoding  = conn.getContentEncoding();
            InputStream   in        = conn.getInputStream();
            StringWriter  out       = new StringWriter();

            if( encoding == null ) encoding = "ISO-8859-1";

            log.debug("Data channel opened, now reading "+conn.getContentLength()+" bytes.");
            FileUtil.copyContents( new InputStreamReader(in, encoding), out );

            c_egfList = parseEGFRatingsList( context, out.toString() );

            // FIXME: Potential problem here: If one fetch fails, then will
            // reattempt on every new fetch.

            c_lastEGFUpdate = System.currentTimeMillis();

            log.debug("EGF ranking update done.");
        }
    }

    private void updateEGF( WikiContext context, String url )
        throws MalformedURLException,
               IOException
    {
        if( c_egfList == null )
        {
            c_egfList = new ArrayList();
            fetchNewEGFList( context, url );
        }
        else if( System.currentTimeMillis() - c_lastEGFUpdate > EGF_UPDATE_INTERVAL )
        {
            fetchNewEGFList( context, url );
        }
    }

    private void mergeEGFRatings( Collection originals, Collection egf )
    {
        for( Iterator i = originals.iterator(); i.hasNext(); )
        {
            UserInfo info = (UserInfo) i.next();

            for( Iterator it = egf.iterator(); it.hasNext(); )
            {
                UserInfo egfinfo = (UserInfo) it.next();

                if( egfinfo.m_lastName.equalsIgnoreCase(info.m_lastName) &&
                    egfinfo.m_firstName.equalsIgnoreCase(info.m_firstName) )
                {
                    // Same guy

                    info.m_egfRank = egfinfo.m_egfRank;
                    break;
                }
            }
        }
    }

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        StringBuffer sb = new StringBuffer();
        StringBuffer errors = new StringBuffer();

        String pages = (String) params.get( "pages" );
        String egfurl = (String) params.get( "egfurl" );

        if( pages == null )
        {
            return "ERROR: Parameter 'pages' is required.";
        }

        StringTokenizer st = new StringTokenizer( pages, ", \t" );

        ArrayList personList = new ArrayList();

        while( st.hasMoreTokens() )
        {
            String pageName = st.nextToken();

            try
            {
                personList.addAll( getPersonsFromPage( context, pageName ) );
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
        //  Now, let's do EGF ratings update.
        //

        try
        {
            if( egfurl != null )
            {
                updateEGF( context, egfurl );
                
                mergeEGFRatings( personList, c_egfList );
            }
        }
        catch( MalformedURLException e )
        {
            errors.append( "<li>EGF URL seems to be faulty.</li>\n");
        }
        catch( IOException e )
        {
            errors.append( "<li>Could not read EGF rankings data: "+e.getMessage()+"</li>\n");
        }

        TreeSet list = new TreeSet( new RankSorter() );

        list.addAll( personList );

        //
        //  List now contains a sorted set of persons.
        //  Write out the HTML for this.
        //

        int counter = 1;

        sb.append("<table border=\"1\">\n");
        sb.append("<tr><th>Place</th><th>Name</th><th>Club</th><th>Rank</th><th>EGF GoR</th></tr>");
        for( Iterator i = list.iterator(); i.hasNext(); )
        {            
            sb.append("<tr>\n");

            UserInfo ui = (UserInfo) i.next();
            sb.append("<td align=center>"+counter+"</td>");
            sb.append("<td>"+ui.m_lastName+", "+ui.m_firstName+"</td>");
            sb.append("<td>"+ui.m_club+"</td>");
            sb.append("<td align=center>"+printRank(ui.m_rank)+"</td>");
            sb.append("<td align=center>"+((ui.m_egfRank > 0) ? ""+ui.m_egfRank : "?")+"</td>");

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
        public int    m_rank;
        public int    m_egfRank = 0;

        public String toString()
        {
            return m_lastName+","+m_firstName+": "+m_rank;
        }
    }

    /**
     *  Sorts stuff according to rank, then EGF points, then last name.
     */
    protected class RankSorter
        implements Comparator
    {

        public int compare( Object o1, Object o2 )
        {
            UserInfo u1 = (UserInfo)o1;
            UserInfo u2 = (UserInfo)o2;

            int res = u1.m_rank - u2.m_rank;

            if( res == 0 )
            {
                res = u2.m_egfRank - u1.m_egfRank;

                if( res == 0 )
                {
                    res = u1.m_lastName.compareTo( u2.m_lastName );

                    if( res == 0 )
                    {
                        res = u2.m_firstName.compareTo( u2.m_lastName );

                        if( res == 0 )
                        {
                            res = 1; // Default, someone's gotta win.
                        }
                    }
                }
            }

            return res;
        }
    }
}
