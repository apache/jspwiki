/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.plugin;

import org.apache.log4j.Category;
import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.attachment.Attachment;
import java.util.*;
import java.io.StringWriter;
import java.text.SimpleDateFormat;

/**
 *  Returns the Recent Changes.
 *
 *  Parameters: since=number of days,
 *              format=(compact|full)
 *
 *  @author Janne Jalkanen
 */
public class RecentChangesPlugin
    implements WikiPlugin
{
    /** How many days we show by default. */
    private static final int    DEFAULT_DAYS = 100*365;

    private static Category log = Category.getInstance( RecentChangesPlugin.class );

    private boolean isSameDay( Date a, Date b )
    {
        Calendar aa = Calendar.getInstance(); aa.setTime(a);
        Calendar bb = Calendar.getInstance(); bb.setTime(b);

        return( aa.get( Calendar.YEAR ) == bb.get( Calendar.YEAR ) &&
                aa.get( Calendar.DAY_OF_YEAR ) == bb.get( Calendar.DAY_OF_YEAR ) );
    }

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        int      since    = TextUtil.parseIntParameter( (String) params.get("since"),
                                                        DEFAULT_DAYS );
        int      spacing  = 4;
        boolean  showAuthor = true;
        WikiEngine engine = context.getEngine();

        //
        //  Which format we want to see?
        //
        String format = (String)params.get("format");
        if( "compact".equals( params.get("format") ) )
        {
            spacing  = 0;
            showAuthor = false;
        }

        Calendar sincedate = new GregorianCalendar();
        sincedate.add( Calendar.DAY_OF_MONTH, -since );

        log.debug("Calculating recent changes from "+sincedate.getTime());

        // FIXME: Should really have a since date on the getRecentChanges
        // method.
        Collection   changes = engine.getRecentChanges();
        StringWriter out     = new StringWriter();

        //
        //  This linkProcessor is used to transform links.
        //
        TranslatorReader linkProcessor = new TranslatorReader( context, new java.io.StringReader("") );

        if( changes != null )
        {
            Date olddate   = new Date(0);

            SimpleDateFormat fmt  = new SimpleDateFormat( "dd.MM.yyyy" );
            SimpleDateFormat tfmt = new SimpleDateFormat( "HH:mm:ss" );

            out.write("<table border=\"0\" cellpadding=\""+spacing+"\">\n");

            for( Iterator i = changes.iterator(); i.hasNext(); )
            {
                WikiPage pageref = (WikiPage) i.next();

                Date lastmod = pageref.getLastModified();

                if( lastmod.before( sincedate.getTime() ) )
                {
                    break;
                }
                
                if( !isSameDay( lastmod, olddate ) )
                {
                    out.write("<tr>\n");
                    out.write("  <td colspan=\"2\"><b>"+
                              fmt.format(lastmod)+
                              "</b></td>\n");
                    out.write("</tr>\n");
                    olddate = lastmod;
                }

                String pageurl = engine.encodeName(pageref.getName());

                String link = linkProcessor.makeLink( (pageref instanceof Attachment) ? 
                                                      TranslatorReader.ATTACHMENT : TranslatorReader.READ,
                                                      pageref.getName(),
                                                      pageref.getName() );
                                                      
                out.write("<tr>\n");

                out.write("<td width=\"30%\">"+
                          link+
                          "</td>\n");

                if( pageref instanceof Attachment )
                {
                    out.write("<td>"+tfmt.format(lastmod)+"</td>");
                }
                else
                {
                    out.write("<td><a href=\""+engine.getBaseURL()+"Diff.jsp?page="+pageurl+"&amp;r1=-1\">"+
                              tfmt.format(lastmod)+
                              "</a></td>\n");
                }

                //
                //  Display author information.
                //

                if( showAuthor )
                {
                    String author = pageref.getAuthor();

                    if( author != null )
                    {
                        if( engine.pageExists(author) )
                        {
                            author = linkProcessor.makeLink( TranslatorReader.READ, author, author );
                        }
                    }
                    else
                    {
                        author = "unknown";
                    }

                    out.write("<td>"+author+"</td>");
                }

                out.write("</tr>\n");
            }

            out.write("</table>\n");
        }
        
        return out.toString();
    }

}
