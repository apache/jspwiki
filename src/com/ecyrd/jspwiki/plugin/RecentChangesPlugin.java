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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.ecs.xhtml.*;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.attachment.Attachment;

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
    private static final String PARAM_FORMAT = "format";
    private static final String PARAM_TIME_FORMAT = "timeFormat";
    private static final String PARAM_DATE_FORMAT = "dateFormat";

    /** How many days we show by default. */
    private static final int    DEFAULT_DAYS = 100*365;

    private static Logger log = Logger.getLogger( RecentChangesPlugin.class );

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
        boolean  showChangenote = true;
        int      tablewidth = 4;
        
        WikiEngine engine = context.getEngine();

        //
        //  Which format we want to see?
        //
        if( "compact".equals( params.get(PARAM_FORMAT) ) )
        {
            spacing  = 0;
            showAuthor = false;
            showChangenote = false;
            tablewidth = 2;
        }

        Calendar sincedate = new GregorianCalendar();
        sincedate.add( Calendar.DAY_OF_MONTH, -since );

        log.debug("Calculating recent changes from "+sincedate.getTime());

        // FIXME: Should really have a since date on the getRecentChanges
        // method.
        Collection   changes = engine.getRecentChanges();

        if( changes != null )
        {
            Date olddate   = new Date(0);

            DateFormat fmt = getDateFormat(params);
            DateFormat tfmt = getTimeFormat(params);

            table rt = new table();
            rt.setBorder(0).setCellPadding(spacing).setClass("recentchanges");

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
                    tr row = new tr();
                    td col = new td();
                    
                    col.setColSpan(tablewidth).setClass("date"); 
                    col.addElement( new b().addElement(fmt.format(lastmod)) );

                    rt.addElement(row);
                    row.addElement(col);                    
                    olddate = lastmod;
                }

                String link = context.getURL( ((pageref instanceof Attachment) ? WikiContext.ATTACH : WikiContext.VIEW), 
                                              pageref.getName() ) ;
                
                a linkel = new a(link,engine.beautifyTitle(pageref.getName()));
                
                tr row = new tr();
                
                td col = new td().setWidth("30%").addElement(linkel);

                //
                //  Add the direct link to the attachment info.
                //
                if( pageref instanceof Attachment )
                {
                    linkel = new a().setHref(context.getURL(WikiContext.INFO,pageref.getName()));
                    linkel.addElement( new img().setBorder(0).setSrc(context.getURL(WikiContext.NONE, "images/attachment_small.png")));

                    col.addElement( linkel );
                }

                
                row.addElement(col);
                rt.addElement(row);
                
                if( pageref instanceof Attachment )
                {
                    row.addElement( new td(tfmt.format(lastmod)).setClass("lastchange") );
                }
                else
                {
                    td infocol = (td) new td().setClass("lastchange");
                    infocol.addElement( new a(context.getURL(WikiContext.DIFF, pageref.getName(), "r1=-1"),tfmt.format(lastmod)) );
                    row.addElement(infocol);
                }

                //
                //  Display author information.
                //

                if( showAuthor )
                {
                    String author = pageref.getAuthor();

                    td authorinfo = new td();
                    authorinfo.setClass("author");
                    
                    if( author != null )
                    {
                        if( engine.pageExists(author) )
                        {
                            authorinfo.addElement( new a(context.getURL(WikiContext.VIEW, author),author) );
                        }
                        else
                        {
                            authorinfo.addElement(author);
                        }
                    }
                    else
                    {
                        authorinfo.addElement("unknown");
                    }

                    row.addElement( authorinfo );
                }

                // Change note
                if( showChangenote )
                {
                    String changenote = (String)pageref.getAttribute(WikiPage.CHANGENOTE);
                    
                    row.addElement( new td((changenote != null ? TextUtil.replaceEntities(changenote) : "")).setClass("changenote") );
                }
                
                //  Revert note
/*                
                if( context.hasAdminPermissions() )
                {
                    row.addElement( new td("Revert") );
                }
 */
            }

            rt.setPrettyPrint(true);
            return rt.toString();
        }
        
        return "";
    }

    

    // TODO: Ideally the default behavior should be to return the default format for the default
    // locale, but that is at odds with the 1st version of this plugin. We seek to preserve the
    // behaviour of that first version, so to get the default format, the user must explicitly do
    // something like: dateFormat='' timeformat='' which is a odd, but probably okay.
    private DateFormat getTimeFormat(Map params)
    {
        String formatString = get(params, "HH:mm:ss", PARAM_TIME_FORMAT);

        if ("".equals(formatString.trim()))
            return SimpleDateFormat.getTimeInstance();

        return new SimpleDateFormat(formatString);
    }



    private DateFormat getDateFormat(Map params)
    {
        String formatString = get(params, "dd.MM.yyyy", PARAM_DATE_FORMAT);

        if ("".equals(formatString.trim()))
            return SimpleDateFormat.getDateInstance();

        return new SimpleDateFormat(formatString);

    }



    private String get(Map params, String defaultValue, String paramName)
    {
        String value = (String) params.get(paramName);
        return (null == value ? defaultValue : value);
    }

    
}


