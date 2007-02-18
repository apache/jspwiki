package com.ecyrd.jspwiki.ui.admin;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.parser.WikiDocument;
import com.ecyrd.jspwiki.render.RenderingManager;

public abstract class WikiFormAdminBean
    implements AdminBean
{
    public abstract String getForm( WikiContext context );
    
    public abstract void handleResponse( WikiContext context, Map params );

    public String getHTML(WikiContext context)
    {
        String result = "";
        
        String wikiMarkup = getForm(context);
        
        RenderingManager mgr = context.getEngine().getRenderingManager();
        
        WikiDocument doc;
        try
        {
            doc = mgr.getParser( context, wikiMarkup ).parse();
            result = mgr.getHTML(context, doc);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return result;
    }

    public String handlePost(WikiContext context, HttpServletRequest req, HttpServletResponse resp)
    {
        return null;
        // FIXME: Not yet implemented
    }
}
