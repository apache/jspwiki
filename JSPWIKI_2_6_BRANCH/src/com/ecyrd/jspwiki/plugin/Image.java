/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import java.util.*;
import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.attachment.AttachmentManager;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *  Provides an image plugin for better control than is possible with
 *  a simple image inclusion.
 *
 *  @author Janne Jalkanen
 *  @since 2.1.4.
 */
// FIXME: It is not yet possible to do wiki internal links.  In order to
//        do this cleanly, a TranslatorReader revamp is needed.

public class Image
    implements WikiPlugin
{
    public static final String PARAM_SRC      = "src";
    public static final String PARAM_ALIGN    = "align";
    public static final String PARAM_HEIGHT   = "height";
    public static final String PARAM_WIDTH    = "width";
    public static final String PARAM_ALT      = "alt";
    public static final String PARAM_CAPTION  = "caption";
    public static final String PARAM_LINK     = "link";
    public static final String PARAM_TARGET   = "target";
    public static final String PARAM_STYLE    = "style";
    public static final String PARAM_CLASS    = "class";
    //    public static final String PARAM_MAP      = "map";
    public static final String PARAM_BORDER   = "border";

    /**
     *  This method is used to clean away things like quotation marks which
     *  a malicious user could use to stop processing and insert javascript.
     */
    private static final String getCleanParameter( Map params, String paramId )
    {
        return TextUtil.replaceEntities( (String) params.get( paramId ) );
    }

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        WikiEngine engine = context.getEngine();
        String src     = getCleanParameter( params, PARAM_SRC );
        String align   = getCleanParameter( params, PARAM_ALIGN );
        String ht      = getCleanParameter( params, PARAM_HEIGHT );
        String wt      = getCleanParameter( params, PARAM_WIDTH );
        String alt     = getCleanParameter( params, PARAM_ALT );
        String caption = getCleanParameter( params, PARAM_CAPTION );
        String link    = getCleanParameter( params, PARAM_LINK );
        String target  = getCleanParameter( params, PARAM_TARGET );
        String style   = getCleanParameter( params, PARAM_STYLE );
        String cssclass= getCleanParameter( params, PARAM_CLASS );
        // String map     = getCleanParameter( params, PARAM_MAP );
        String border  = getCleanParameter( params, PARAM_BORDER );

        if( src == null )
        {
            throw new PluginException("Parameter 'src' is required for Image plugin");
        }

        if( cssclass == null ) cssclass = "imageplugin";

        if( target != null && !validTargetValue(target) )
        {
            target = null; // not a valid value so ignore
        }

        try
        {
            AttachmentManager mgr = engine.getAttachmentManager();
            Attachment        att = mgr.getAttachmentInfo( context, src );

            if( att != null )
            {
                src = context.getURL( WikiContext.ATTACH, att.getName() );
            }
        }
        catch( ProviderException e )
        {
            throw new PluginException( "Attachment info failed: "+e.getMessage() );
        }

        StringBuffer result = new StringBuffer();

        result.append( "<table border=\"0\" class=\""+cssclass+"\"" );
        //if( align != null ) result.append(" align=\""+align+"\"");
        //if( style != null ) result.append(" style=\""+style+"\"");
        
        //
        //  Do some magic to make sure centering also work on FireFox
        //
        if( style != null ) 
        {
            result.append(" style=\""+style);

            // Make sure that we add a ";" to the end of the style string
            if( result.charAt( result.length()-1 ) != ';' ) result.append(";");
                
            if( align != null && align.equals("center") ) 
            {
                result.append(" margin-left: auto; margin-right: auto;");
            }
                
            result.append("\"");
        }
        else
        {
            if( align != null && align.equals("center") ) result.append(" style=\"margin-left: auto; margin-right: auto;\"");
        }
        
        if( align != null && !(align.equals("center")) ) result.append(" align=\""+align+"\"");
        
        result.append( ">\n" );

        if( caption != null ) 
        {
            result.append("<caption align=bottom>"+TextUtil.replaceEntities(caption)+"</caption>\n");
        }


        result.append( "<tr><td>" );
       
        if( link != null ) 
        {
            result.append("<a href=\""+link+"\"");
            if( target != null )
            {
                result.append(" target=\""+target+"\"");
            }
            result.append(">");
        }

        result.append( "<img src=\""+src+"\"" );
       
        if( ht != null )     result.append(" height=\""+ht+"\"");
        if( wt != null )     result.append(" width=\""+wt+"\"");
        if( alt != null )    result.append(" alt=\""+alt+"\"");
        if( border != null ) result.append(" border=\""+border+"\"");
        // if( map != null )    result.append(" map=\""+map+"\"");

        result.append(" />");
        if( link != null )  result.append("</a>");
        result.append("</td></tr>\n");

        result.append("</table>\n");

        return result.toString();
    }

    private boolean validTargetValue( String s )
    {
        if( s.equals("_blank")
                || s.equals("_self")
                || s.equals("_parent")
                || s.equals("_top") )
        {
            return true;
        } 
        else if( s.length() > 0 ) // check [a-zA-z]
        {
            char c = s.charAt(0);
            return Character.isLowerCase(c) || Character.isUpperCase(c);
        }
        return false;
    }

}
