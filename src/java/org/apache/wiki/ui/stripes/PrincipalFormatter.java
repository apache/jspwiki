package org.apache.wiki.ui.stripes;

import java.security.Principal;
import java.util.Locale;

import net.sourceforge.stripes.format.Formatter;

/**
 * Stripes Formatter class that formats a supplied {@link Principal}, with the result
 * being the name of the Principal.
 */
public class PrincipalFormatter implements Formatter<Principal>
{
    /**
     * {@inheritDoc}
     * <p>
     * This implementation produces a formatted String representing the {@link Principal}
     * according to the following rules:
     * </p>
     * <ul>
     * <li>if the Principal is {@code null}, returns the empty string</li>
     * <li>otherwise, the result of {@link Principal#getName()} is returned</li>
     * </ul>
     */
    public String format( Principal input )
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
     * This implementation does nothing; the Principal name is always used.
     * </p>
     */
    public void setFormatPattern( String formatPattern )
    {
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation does nothing; the Principal name is always used.
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
