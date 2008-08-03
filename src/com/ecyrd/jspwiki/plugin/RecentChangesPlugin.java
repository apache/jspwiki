/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
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
import com.ecyrd.jspwiki.i18n.InternationalizationManager;
import com.ecyrd.jspwiki.preferences.Preferences;
import com.ecyrd.jspwiki.preferences.Preferences.TimeFormat;

/**
 *  Returns the Recent Changes.
 *
 *  Parameters: since=number of days,
 *              format=(compact|full)
 *
 */
public class RecentChangesPlugin
    implements WikiPlugin
{
    public static final String PARAM_FORMAT = "format";
    public static final String PARAM_TIME_FORMAT = "timeFormat";
    public static final String PARAM_DATE_FORMAT = "dateFormat";

    /** How many days we show by default. */
    private static final int    DEFAULT_DAYS = 100*365;

    private static Logger log = Logger.getLogger( RecentChangesPlugin.class );

    private boolean isSameDay( Date a, Date b )
    {
        Calendar aa = Calendar.getInstance(); aa.setTime(a);
        Calendar bb = Calendar.getInstance(); bb.setTime(b);

        return aa.get( Calendar.YEAR ) == bb.get( Calendar.YEAR ) &&
                aa.get( Calendar.DAY_OF_YEAR ) == bb.get( Calendar.DAY_OF_YEAR );
    }

    /**
     * {@inheritDoc}
     */
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

            DateFormat fmt = getDateFormat( context, params );
            DateFormat tfmt = getTimeFormat( context, params );

            table rt = new table();
            rt.setCellPadding(spacing).setClass("recentchanges");

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

                String link = context.getURL( pageref instanceof Attachment ? WikiContext.ATTACH : WikiContext.VIEW, 
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
                    linkel.setClass("infolink");
                    linkel.addElement( new img().setSrc(context.getURL(WikiContext.NONE, "images/attachment_small.png")));

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
                        authorinfo.addElement( context.getBundle(InternationalizationManager.CORE_BUNDLE).getString( "common.unknownauthor" ) );
                    }

                    row.addElement( authorinfo );
                }

                // Change note
                if( showChangenote )
                {
                    String changenote = (String)pageref.getAttribute(WikiPage.CHANGENOTE);
                    
                    row.addElement( new td(changenote != null ? TextUtil.replaceEntities(changenote) : "").setClass("changenote") );
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
    private DateFormat getTimeFormat( WikiContext context, Map params )
    {
        String formatString = get(params, "HH:mm:ss", PARAM_TIME_FORMAT);

        if ("".equals(formatString.trim()))
            return Preferences.getDateFormat( context, TimeFormat.TIME );

        return new SimpleDateFormat(formatString);
    }



    private DateFormat getDateFormat( WikiContext context, Map params )
    {
        String formatString = get(params, "dd.MM.yyyy", PARAM_DATE_FORMAT);

        if ("".equals(formatString.trim()))
            return Preferences.getDateFormat( context, TimeFormat.DATE );

        return new SimpleDateFormat(formatString);

    }



    private String get(Map params, String defaultValue, String paramName)
    {
        String value = (String) params.get(paramName);
        return null == value ? defaultValue : value;
    }

    
}


