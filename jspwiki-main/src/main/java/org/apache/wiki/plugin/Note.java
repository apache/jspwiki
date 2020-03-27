/* 
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
package org.apache.wiki.plugin;

import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.ui.TemplateManager;
import org.apache.wiki.util.TextUtil;

import java.util.Map;

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
 *  <p>Parameters : </p>
 *  <ul>
 *  <li><b>_cmdline</b> - the commentText</li>
 *  </ul>
 *  
 */
public class Note implements Plugin {

    /** Property name for setting the image for the note.  Value is <tt>{@value}</tt>. */
    public static final String PROP_NOTE_IMAGE    = "notePlugin.imageName";
    
    /** The default name for the note.  Value is <tt>{@value}</tt>. */
    public static final String DEFAULT_NOTE_IMAGE = "note.png";

    /**
     *  {@inheritDoc}
     */
    @Override
    public String execute( final Context context, final Map<String, String> params) throws PluginException {
        final String commandline = params.get(DefaultPluginManager.PARAM_CMDLINE);
        if (commandline == null || commandline.length() == 0) {
            return "Unable to obtain plugin command line from parameter'" + DefaultPluginManager.PARAM_CMDLINE + "'"; // I18N
        }

        final String commentImage = imageUrl(context);

        final String commentText = clean(commandline);

        return "<img src='" + commentImage + "' alt=\"Comment: " + 
               commentText + "\" title=\"" + commentText + "\"/>";
    }

    private String imageUrl( final Context ctx ) {
        final Engine engine = ctx.getEngine();
        String commentImage = engine.getWikiProperties().getProperty( PROP_NOTE_IMAGE, DEFAULT_NOTE_IMAGE );
        commentImage = "images/" + commentImage;
        
        String resource = engine.getManager( TemplateManager.class ).findResource( ctx, engine.getTemplateDir(), commentImage );
        
        // JSPWIKI-876 Fixed error with Note Plugin. Only one preceding "/" is needed.
        if( resource != null && resource.startsWith( "/" ) ) {
        	resource = resource.substring(1);
        }
        return ctx.getURL( ContextEnum.PAGE_NONE.getRequestContext(), resource );
    }


    /**
     *  Cleans the side.
     * 
     * @param commandline
     */
    private String clean( final String commandline)
    {
        return TextUtil.replaceEntities( commandline );
    }

}

