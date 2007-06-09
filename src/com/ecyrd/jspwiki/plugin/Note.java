/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2007 JSPWiki Developer Group

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

import java.util.Map;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;

/**
 * Outputs an image with the supplied text as the <tt>title</tt> which is shown as a tooltip by
 * most browsers. This is intended for short one line comments.
 * <p>
 * See http://www.456bereastreet.com/archive/200412/the_alt_and_title_attributes/ for discussion on
 * alt and title attributes.
 * <p>
 * Adaption of the CommentPlugin written by Scott Hulbert, cleaned up and generalized, but basically
 * his concept.
 * <p>
 * 
 * @author John Volkar
 * @author Scott Hulbert
 */
public class Note implements WikiPlugin
{
    public static final String PROP_NOTE_IMAGE    = "notePlugin.imageName";
    public static final String DEFAULT_NOTE_IMAGE = "note.png";

    public String execute(WikiContext context, Map params) throws PluginException
    {
        String commandline = (String) params.get(PluginManager.PARAM_CMDLINE);
        if (commandline == null || commandline.length() == 0)
        {
            return "Unable to obtain plugin command line from parameter'" + PluginManager.PARAM_CMDLINE + "'"; // I18N
        }

        String commentImage = imageUrl(context);

        String commentText = clean(commandline);

        return "<img src='" + commentImage + "' alt=\"Comment: " + 
               commentText + "\" title=\"" + commentText + "\"/>";
    }

    private String imageUrl( WikiContext ctx )
    {
        WikiEngine engine = ctx.getEngine();
        String commentImage = engine.getWikiProperties().getProperty(PROP_NOTE_IMAGE,
                                                                     DEFAULT_NOTE_IMAGE);

        commentImage = "images/"+commentImage;
        
        String resource = engine.getTemplateManager().findResource( ctx, 
                                                                    engine.getTemplateDir(), 
                                                                    commentImage );

        return ctx.getURL( WikiContext.NONE, resource );
    }


    /**
     *  Cleans the side.
     * 
     * @param commandline
     * @return
     */
    private String clean(String commandline)
    {
        return TextUtil.replaceEntities( commandline );
    }

}

