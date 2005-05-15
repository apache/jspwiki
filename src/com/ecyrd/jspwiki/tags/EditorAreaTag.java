/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.tags;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.ecs.ConcreteElement;
import org.apache.ecs.xhtml.br;
import org.apache.ecs.xhtml.div;
import org.apache.ecs.xhtml.h3;
import org.apache.ecs.xhtml.noscript;
import org.apache.ecs.xhtml.script;
import org.apache.ecs.xhtml.textarea;

import com.ecyrd.jspwiki.NoSuchVariableException;
import com.ecyrd.jspwiki.TranslatorReader;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public class EditorAreaTag extends WikiTagBase
{
    public static final String PROP_EDITORTYPE = "jspwiki.editor";
    
    public static final String EDITOR_PLAIN = "Plain";
    public static final String EDITOR_FCK   = "FCK";
    
    public static final String AREA_NAME    = "text"; // TODO: Change
    
    public int doWikiStartTag() throws Exception
    {
        pageContext.getOut().print( getEditorArea( m_wikiContext ).toString() );
        
        return SKIP_BODY;
    }
    
    private static ConcreteElement getFCKEditorArea( WikiContext context )
    {
        WikiEngine engine = context.getEngine();
        
        // FIXME: Should this change the properties?
        context.setVariable( WikiEngine.PROP_RUNFILTERS, "false" );
        context.setVariable( TranslatorReader.PROP_RUNPLUGINS, "false" );
        String pageAsHtml = StringEscapeUtils.escapeJavaScript( engine.textToHTML( context, getText(context) ) );

        div container = new div();
        script area = new script();
        
        area.setType( "text/javascript" );
        
        area.addElement( "var oFCKeditor = new FCKeditor( 'htmlPageText' );");
        area.addElement( "oFCKeditor.BasePath = 'scripts/fckeditor/';" );
        area.addElement( "oFCKeditor.Value = '"+pageAsHtml+"' ;" );
        area.addElement( "oFCKeditor.Width  = '100%';" );
        area.addElement( "oFCKeditor.Height = '500';" );
        area.addElement( "oFCKeditor.ToolbarSet = 'JSPWiki';" );
        area.addElement( "oFCKeditor.Config['CustomConfigurationsPath'] = '"+
                         context.getEngine().getURL(WikiContext.NONE, "scripts/fckconfig.js",null,true)+"';" );
        area.addElement( "oFCKeditor.Create() ;" );
        
        noscript noscriptarea = new noscript();
        
        noscriptarea.addElement( new br() );
        noscriptarea.addElement( new h3().addElement("You need to enable Javascript in your browser to use the WYSIWYG editor").setStyle("previewnote"));
        noscriptarea.addElement( new br() );
        
        container.addElement( area );
        container.addElement( noscriptarea );
        
        area.setPrettyPrint( true );
        container.setPrettyPrint( true );
        return container;
    }

    private static String getText( WikiContext context )
    {
        String usertext = null;
        
        if( context.getRequestContext().equals(WikiContext.EDIT) )
        {
            usertext = context.getHttpParameter( AREA_NAME );
            if( usertext == null )
            {
                usertext = context.getEngine().getText( context, context.getPage() );
            }            
        }
        else if( context.getRequestContext().equals(WikiContext.COMMENT) )
        {
            usertext = context.getHttpParameter( AREA_NAME );
        }
        
        return usertext;
    }

    /**
     *  Returns an element for constructing an editor.
     * 
     * @param context Current WikiContext
     * @return
     */
    public static ConcreteElement getEditorArea( WikiContext context )
    {
        try
        {
            String editor = context.getEngine().getVariableManager().getValue( context, PROP_EDITORTYPE );
        
            if( EDITOR_FCK.equals(editor) )
                return getFCKEditorArea( context );
        }
        catch( NoSuchVariableException e ) {} // This is fine
        
        return getPlainEditorArea( context );
    }
    
    private static ConcreteElement getPlainEditorArea( WikiContext context )
    {
        textarea area = new textarea();

        area.setClass("editor");
        area.setWrap("virtual");
        area.setName( AREA_NAME );
        area.setRows( 25 );
        area.setCols( 80 );
        area.setStyle( "width:100%;" );
        area.setID( "editorarea" );  // FIXME: Should really be settable in case there are several
        
        String text = getText( context );
        if( text != null ) area.addElement( text );
        
        return area;
    }
}
