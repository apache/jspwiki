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

    public void initialize( WikiContext context, Map params )
        throws PluginException
    {
    }

    private String makeImage( String content )
    {
        return "<IMG SRC=\"images/diagram/"+content+".gif\">";
        //return "<TD>"+content+"</TD>";
    }

    private String parseDiagram( String dia )
        throws IOException
    {
        StringTokenizer tok = new StringTokenizer( dia.trim(), " \t\n-", true );

        // System.out.println("dia="+dia);

        StringBuffer res = new StringBuffer();

        res.append("<DIV CLASS=\"diagram\">\n");

        int type;

        int row = 0;
        int col = -1;

        while( tok.hasMoreTokens() )
        {
            col++;
            String item = tok.nextToken();

            if( Character.isDigit(item.charAt(0)) )
            {
                int num = Integer.parseInt( item );

                String which = (num % 2 == 1) ? "b" : "w";
                res.append( makeImage( which+Integer.toString(num) ) );
                continue;
            }

            switch( item.charAt(0) )
            {
              case '#':
              case 'X':
                res.append( makeImage("b") );
                break;

              case 'O':
                res.append( makeImage("w") );
                break;

              case '.':
                res.append( makeImage("empty") );
                break;

              case ',':
                res.append( makeImage("hoshi") );
                break;

              case '|':
                res.append( makeImage( (col == 0) ? "LS" : "RS" ) );
                break;

              case '+':
                res.append( makeImage( (row == 0 ? "U" : "L") + 
                                       (col == 0 ? "L" : "R") +
                                       "C" ) );
                break;

              case '-':                
                res.append( makeImage( (row == 0) ? "TS" : "BS" ) );
                break;

              case '\n':
                res.append("<BR>\n");
                col = -1;
                row++;
                break;

              case ' ':
              case '\t':
                break;

              default:
                res.append( makeImage( "lc"+item ) );
                break;
            }
        }

        res.append("</DIV>\n");

        return res.toString();
    }

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        String diagram = (String) params.get( "_body" );

        if( diagram == null || diagram.length() == 0 )
        {
            return "<B>No diagram detected.</B>";
        }

        try
        {
            return parseDiagram( diagram );
        }
        catch( IOException e )
        {
            log.error("Something funny in diagram", e );

            throw new PluginException("Error in diagram: "+e.getMessage());
        }
    }
}
