package com.ecyrd.jspwiki.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import org.apache.log4j.Category;

import com.ecyrd.jspwiki.WikiContext;

/**
 *  Base class for JSPWiki tags.  You do not necessarily have
 *  to derive from this class, since this does some initialization.
 *  <P>
 *  This tag is only useful if you're having an "empty" tag, with
 *  no body content.
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public abstract class WikiTagBase
    extends TagSupport
{
    public static final String ATTR_CONTEXT = "jspwiki.context";

    static    Category    log = Category.getInstance( WikiTagBase.class );

    protected WikiContext m_wikiContext;

    public int doStartTag()
        throws JspException
    {
        try
        {
            m_wikiContext = (WikiContext) pageContext.getAttribute( ATTR_CONTEXT );

            return doWikiStartTag();
        }
        catch( Exception e )
        {
            log.error( "Tag failed", e );
            throw new JspException( "Tag failed, check logs: "+e.getMessage() );
        }
    }

    /**
     *  This method is allowed to do pretty much whatever he wants.
     *  We then catch all mistakes.
     */
    public abstract int doWikiStartTag() throws Exception;

    public int doEndTag()
    {
        return EVAL_PAGE;
    }
}
