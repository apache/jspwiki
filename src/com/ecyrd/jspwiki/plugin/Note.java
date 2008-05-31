/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2007 JSPWiki Developer Group

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
    /**
     *  Property name for setting the image for the note.  Value is <tt>{@value}</tt>.
     */
    public static final String PROP_NOTE_IMAGE    = "notePlugin.imageName";
    
    /**
     *  The default name for the note.  Value is <tt>{@value}</tt>.
     */
    public static final String DEFAULT_NOTE_IMAGE = "note.png";

    /**
     *  {@inheritDoc}
     */
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

