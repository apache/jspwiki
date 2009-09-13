package org.apache.wiki.ui.stripes;

import java.util.Locale;

import net.sourceforge.stripes.format.Formatter;

import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.ContentManager;
import org.apache.wiki.content.WikiPath;

/**
 * Stripes Formatter class that formats a supplied WikiPage, with the result
 * being the name of the page.
 */
public class WikiPageFormatter implements Formatter<WikiPage>
{
    /**
     * {@inheritDoc}
     * <p>
     * This implementation produces a formatted String representing the WikiPage
     * according to the following rules:
     * </p>
     * <ul>
     * <li>if the WikiPage is null, returns the empty string</li>
     * <li>if the WikiPage belongs to the default space, the path name without
     * space is returned</li>
     * <li>otherwise, the full path name, including the space, is returned</li>
     * </ul>
     */
    public String format( WikiPage input )
    {
        WikiPath path = input.getPath();
        if ( ContentManager.DEFAULT_SPACE.equals( path.getSpace() ) )
        {
            return path.getPath();
        }
        return path.toString();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation does nothing.
     * </p>
     */
    public void init()
    {
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation does nothing; the page name is always used.
     * </p>
     */
    public void setFormatPattern( String formatPattern )
    {
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation does nothing; the page name is always used.
     * </p>
     */
    public void setFormatType( String formatType )
    {
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation does nothing, because the Locale has no effect on the
     * output.
     * </p>
     */
    public void setLocale( Locale locale )
    {
    }

}
