package com.ecyrd.jspwiki.ui.stripes;

import java.util.List;
import java.util.Map;

/**
 * Transforms a JspDocument from standard JSP markup to Stripes markup.
 */
public class JSPWikiJspTransformer extends AbstractJspTransformer
{
    /**
     * {@inheritDoc}
     */
    public void initialize( Map<String, Object> sharedState, JspDocument doc )
    {
    }

    /**
     * {@inheritDoc}
     */
    public void transform( Map<String, Object> sharedState, JspDocument doc )
    {
        List<Node> nodes = doc.getNodes();

        for( Node node : nodes )
        {
            // For all HTML tags...
            if( node.isHtmlNode() )
            {
                Tag tag = (Tag) node;

                // Check any form or stripes:form elements
                if( "form".equals( tag.getName() ) || "stripes:form".equals( tag.getName() ) )
                {
                    processFormTag( tag );
                }
                else if( "fmt:setBundle".equals( tag.getName() ) )
                {
                    removeSetBundle( tag );
                }

                // Advise user about <input type="hidden"> or <stripes:hidden> tags
                boolean isTypeHidden = false;
                if ( tag.getType() != NodeType.END_TAG )
                {
                    isTypeHidden = "stripes:hidden".equals( tag.getName() );
                    if( "input".equals( tag.getName() ) )
                    {
                        Attribute attribute = tag.getAttribute( "type" );
                        isTypeHidden = "hidden".equals( attribute.getValue() );
                    }
                    if( isTypeHidden )
                    {
                        String paramName = tag.hasAttribute( "name" ) ? tag.getAttribute( "name" ).getValue() : null;
                        String paramValue = tag.hasAttribute( "value" ) ? tag.getAttribute( "value" ).getValue() : null;
                        if ( paramName != null && paramValue != null )
                        {
                            message( tag, "NOTE: hidden form input sets parameter " + paramName
                                     + "=\"" + paramValue + "\". This should probably correspond to a Stripes ActionBean getter/settter. Refactor?" );
                        }
                    }
                }

                // Tell user about <wiki:Messages> tags.
                if( "wiki:Messages".equals( tag.getName() ) )
                {
                    message( tag,
                             "Consider using <stripes:errors> tags instead of <wiki:Messages> for displaying validation errors." );
                }
            }
        }
    }

    /**
     * Removes the &lt;fmt:setBundle&gt; tag and advises the user.
     * 
     * @param tag the tag to remove
     */
    private void removeSetBundle( Tag tag )
    {
        Node parent = tag.getParent();
        parent.removeChild( tag );
        message( tag, "Removed <fmt:setBundle> tag because it is automatically set in web.xml." );
    }

    /**
     * For &lt;form&gt; and &lt;stripes:form&gt; tags, changes
     * <code>accept-charset</code> or <code>acceptcharset</code> attribute
     * value to "UTF-8", and removes any <code>onsubmit</code> function calls.
     * 
     * @param tag the form tag
     */
    private void processFormTag( Tag tag )
    {
        // Change "accept-charset" or "acceptcharset" values to UTF-8
        Attribute attribute = tag.getAttribute( "accept-charset" );
        if( attribute == null )
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

}
