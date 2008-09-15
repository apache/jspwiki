package com.ecyrd.jspwiki.ui.stripes;

import java.util.List;
import java.util.Map;

/**
 * Transforms a JspDocument from standard JSP markup to Stripes markup.
 */
public class JSPWikiJspTransformer extends AbstractJspTransformer
{

    public void transform( Map<String, Object> sharedState, JspDocument doc )
    {
        List<Node> nodes = doc.getNodes();

        for( Node node : nodes )
        {
            // For all HTML tags...
            if( node.isHtmlNode() )
            {
                Tag tag = (Tag)node;
                
                // Check any form or stripes:form elements
                if( "form".equals( tag.getName() ) || "stripes:form".equals( tag.getName() ) )
                {
                    // Change "accept-charset" or "acceptcharset" values to UTF-8
                    Attribute attribute = tag.getAttribute( "accept-charset" );
                    if ( attribute == null )
                    {
                        attribute = tag.getAttribute( "acceptcharset" );
                    }
                    if( attribute != null )
                    {
                        message( attribute, "Changed value to \"UTF-8\"." );
                        attribute.setValue( "UTF-8" );
                    }
                    
                    // Remove onsubmit() attribute and warn the user
                    attribute = tag.getAttribute( "onsubmit" );
                    if( attribute != null )
                    {
                        String value = attribute.getValue();
                        message( attribute, "Removed JavaScript call \"" + value + "\". REASON: it probably does not work with Stripes." );
                        tag.removeAttribute( attribute );
                    }
                }
                
                // Advise user about <input type="hidden"> tags
                boolean isTypeHidden = false;
                isTypeHidden = "stripes:form".equals( tag.getName() );
                if( "input".equals( tag.getName() ) )
                {
                    Attribute attribute = tag.getAttribute( "type" );
                    isTypeHidden = "hidden".equals( attribute.getValue() );
                }
                if( isTypeHidden )
                {
                    Attribute hidden = tag.getAttribute( "name" );
                    message( hidden, "NOTE: hidden form input \"" + hidden.getValue() +"\" should probably correspond to a Stripes ActionBean getter/settter. Refactor?"  );
                }
                
                // Tell user about <wiki:Messages> tags.
                if ( "wiki:Messages".equals( tag.getName() ) )
                {
                    message( tag, "Consider using <stripes:errors> tags instead of <wiki:Messages> for displaying validation errors." );
                }
            }
        }
    }

}
