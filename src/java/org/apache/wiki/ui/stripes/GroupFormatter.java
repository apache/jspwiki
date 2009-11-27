package org.apache.wiki.ui.stripes;

import java.util.Locale;

import net.sourceforge.stripes.format.Formatter;

import org.apache.wiki.auth.authorize.Group;

/**
 * Stripes Formatter class that formats a supplied wiki {@link Group}, with the result
 * being the name of the group.
 */
public class GroupFormatter implements Formatter<Group>
{
    /**
     * {@inheritDoc}
     * <p>
     * This implementation produces a formatted String representing the {@link Group}
     * according to the following rules:
     * </p>
     * <ul>
     * <li>if the Group is {@code null}, returns the empty string</li>
     * <li>otherwise, the Group name is returned</li>
     * </ul>
     */
    public String format( Group input )
    {
        return input == null ? "" : input.getName();
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
     * This implementation does nothing; the group name is always used.
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
