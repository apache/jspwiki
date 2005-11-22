package com.ecyrd.jspwiki.editor;

import java.util.Properties;

import javax.servlet.jsp.PageContext;

import com.ecyrd.jspwiki.NoSuchVariableException;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;

public class EditorManager
{
    public static final String PROP_EDITORTYPE = "jspwiki.editor";
    
    public static final String EDITOR_PLAIN = "plain";
    public static final String EDITOR_FCK   = "FCK";
    public static final String EDITOR_PREVIEW = "preview";
    
    public static final String REQ_EDITEDTEXT = "_editedtext";
    public static final String ATTR_EDITEDTEXT = REQ_EDITEDTEXT;
    
    private           WikiEngine     m_engine;
    
    public void initialize( WikiEngine engine, Properties props )
    {
        m_engine = engine;
    }
    
    
    /**
     *  Returns an editor for the current context.
     *  
     * @param context
     * @return The name of the chosen editor.
     */
    public String getEditorName( WikiContext context )
    {
        if( context.getRequestContext().equals(WikiContext.PREVIEW) )
            return EDITOR_PREVIEW;
        
        try
        {
            String editor = m_engine.getVariableManager().getValue( context, PROP_EDITORTYPE );
        
            if( EDITOR_FCK.equalsIgnoreCase(editor) )
                return EDITOR_FCK;
        }
        catch( NoSuchVariableException e ) {} // This is fine

        return EDITOR_PLAIN;
    }

    /**
     *  Convenience method for getting the path to the editor JSP file.
     *  
     *  @param context
     *  @return e.g. "editors/plain.jsp"
     */
    public String getEditorPath( WikiContext context )
    {
        String editor = getEditorName( context );
        
        return "editors/"+editor+".jsp";
    }
    
    /**
     *  Convinience function which examines the current context and attempts to figure
     *  out whether the edited text is in the HTTP request parameters or somewhere in
     *  the session.
     *  
     *  @param ctx
     *  @return
     */
    public static String getEditedText( PageContext ctx )
    {
        String usertext = ctx.getRequest().getParameter( REQ_EDITEDTEXT );
        
        if( usertext == null )
        {
            usertext = (String)ctx.getAttribute( ATTR_EDITEDTEXT, PageContext.REQUEST_SCOPE );
        }
        
        return usertext;
    }
}
