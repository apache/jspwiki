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

/**
 *  Plugin for doing go game diagrams.
 *
 *  @author Janne Jalkanen
 */
public class GoDiagram
    implements WikiPlugin
{
    private static Category log = Category.getInstance( GoDiagram.class );

    public static final int BLACK_FIRST = 1;
    public static final int WHITE_FIRST = 0;

    public void initialize( WikiContext context, Map params )
        throws PluginException
    {
    }

    private String makeImage( WikiContext context, String content )
    {
        //return "<IMG SRC=\"images/diagram/"+content+".gif\">";
        return "<TD><IMG SRC=\""+context.getEngine().getBaseURL()+"images/godiagram/"+content+".png\"></TD>";
    }

    public class DiagramInfo
    {
        boolean topSide = false;
        boolean bottomSide = false;
        boolean leftSide = false;
        boolean rightSide = false;

        int numRows = 0;
        int numCols = 0;

        Object[][] contents;
    }

    protected DiagramInfo getDiagramInfo( String dia )
    {
        DiagramInfo info = new DiagramInfo();
        StringTokenizer tok = new StringTokenizer( dia.trim(), "\n" );

        ArrayList rows = new ArrayList();

        while( tok.hasMoreTokens() )
        {
            String line = tok.nextToken().trim();

            int firstBar = line.indexOf('|');
            int lastBar  = line.lastIndexOf('|');

            if( firstBar == 0 )
                info.leftSide = true;

            if( lastBar == (line.length()-1) )
                info.rightSide = true;

            int firstDash = line.indexOf('-');
            if( firstDash >= 0 )
            {
                // Top or bottom row

                if( info.numRows == 0 )
                    info.topSide = true;
                else
                    info.bottomSide = true;
            }
            else
            {
                // Actual diagram row.
                // Gobble it all in, don't bother to parse.

                ArrayList       currentRow = new ArrayList();
                StringTokenizer st2        = new StringTokenizer( line, " " );

                info.numRows++;

                while( st2.hasMoreTokens() )
                {
                    String mark = st2.nextToken();

                    if( mark.equals("|") ) continue;

                    currentRow.add( mark );
                }

                info.numCols = currentRow.size();
                rows.add( currentRow );
            }                        
        }

        info.contents = new String[info.numRows][info.numCols];

        /*
        System.out.println("\n");
        System.out.println( (info.topSide ? "top " : " ")+
                            (info.bottomSide ? "bottom ": " ")+
                            (info.leftSide ? "left " : " ")+
                            (info.rightSide ? "right " : " " ) );
        */

        for( int i = 0; i < info.numRows; i++ )
        {
            for( int j = 0; j < info.numCols; j++ )
            {
                info.contents[i][j] = (String) ((ArrayList)rows.get(i)).get(j);
                //System.out.print( info.contents[i][j] );
            }
            //System.out.print("\n");
        }
        return info;
    }

    private String insertEmpty( DiagramInfo info, int row, int col )
    {
        if( row == 0 && info.topSide )
        {
            if( col == 0 && info.leftSide )
            {
                return "ULC";
            }
            else if( col == info.numCols-1 && info.rightSide )
            {
                return "URC";
            }

            return "TS";
        }
        else if( row == info.numRows-1 && info.bottomSide )
        {
            if( col == 0 && info.leftSide )
                return "LLC";
            else if( col == info.numCols-1 && info.rightSide )
                return "LRC";

            return "BS";
        }
        else if( col == 0 && info.leftSide )
        {
            return "LS";
        }
        else if( col == info.numCols-1 && info.rightSide )
        {
            return "RS";
        }

        return "empty";
    }

    /**
     *  @param first 'b', if black should have the first move, 'w' otherwise.
     */
    private String parseDiagram( WikiContext context, String dia, int first )
        throws IOException
    {
        DiagramInfo info = getDiagramInfo( dia );

        // System.out.println("dia="+dia);

        StringBuffer res = new StringBuffer();

        //res.append("<DIV CLASS=\"diagram\">\n");
        res.append("<TABLE BORDER=\"0\" CELLSPACING=\"0\" CELLPADDING=\"0\">");
        int type;

        for( int row = 0; row < info.numRows; row++ )
        {
            res.append("<TR>");

            for( int col = 0; col < info.numCols; col++ )
            {
                String item = (String)info.contents[row][col];

                if( Character.isDigit(item.charAt(0)) )
                {
                    int num = Integer.parseInt( item );

                    String which = (num % 2 == first) ? "b" : "w";
                    res.append( makeImage( context, which+Integer.toString(num) ) );
                    continue;
                }

                switch( item.charAt(0) )
                {
                  case '#':
                  case 'X':
                    res.append( makeImage(context,"b") );
                    break;

                  case 'O':
                    res.append( makeImage(context,"w") );
                    break;

                  case '.':
                    res.append( makeImage(context,insertEmpty( info, row, col )) );
                    break;

                  case ',':
                    res.append( makeImage(context,"hoshi") );
                    break;

                  default:
                    res.append( makeImage(context,"lc"+item ) );
                    break;
                }
            } // col

            res.append("</TR>\n");
        } // row


        //res.append("</DIV>\n");
        res.append("</TABLE>");

        return res.toString();
    }

    // FIXME: Parameters should be checked against HTML entities.
    // FIXME: "label" should be run through parser

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        String diagram = (String) params.get( "_body" );
        String label   = (String) params.get( "label" );
        String first   = (String) params.get( "first" );
        String align   = (String) params.get( "align" );

        if( diagram == null || diagram.length() == 0 )
        {
            return "<B>No diagram detected.</B>";
        }

        if( align == null ) align = "left";

        if( first == null || first.length() == 0 || 
            !(first.startsWith("w") || first.startsWith("W")) )
        {
            first = "b";
        }

        try
        {
            StringBuffer sb = new StringBuffer();

            sb.append("<table border=1 align="+align+" cellpadding=5 style=\"margin: 10px;\">");
            sb.append("<tr><td align=center>\n");
            sb.append( parseDiagram( context, 
                                     diagram, 
                                     (first.startsWith("b") ? BLACK_FIRST : WHITE_FIRST )) );
            sb.append("</td></tr>\n");
            if( label != null )
            {
                sb.append( "<tr><td class=\"diagramlabel\"><B>Dia:</B> "+
                           context.getEngine().textToHTML(context,label)+"</td></tr>\n" );
            }
            sb.append("</table>\n");

            return sb.toString();
        }
        catch( IOException e )
        {
            log.error("Something funny in diagram", e );

            throw new PluginException("Error in diagram: "+e.getMessage());
        }
    }
}
